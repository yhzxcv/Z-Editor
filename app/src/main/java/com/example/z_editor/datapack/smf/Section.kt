package com.example.z_editor.datapack.smf

import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * RSGP 的一个"段"（data 段或 image 段）的只读视图。
 *
 * 存在的理由和 [ByteSource] 一样：段的解压结果是几百 MB 级的东西，原先
 * `rawData.copyOfRange(...)` 一上来就整段进内存。现在按段的来源分三种：
 *
 * - [SourceSlice] —— 段**没压缩**，就是源文件里的一段区间，零拷贝；
 * - [MemorySection] —— 段解压后不大（[SmfUnpacker] 里设了阈值），直接放内存；
 * - [FileSection] —— 段解压后很大，落临时文件，写完再按需读。
 *
 * 条目提取统一走 [copyTo]（流式拷进目标文件）或 [readInto]（小条目取回字节，
 * 只给 PTX 解码用）。
 */
internal interface Section : AutoCloseable {

    /** 段的有效字节数。 */
    val size: Long

    /** 把 `[offset, offset+length)` 流式写进 [out]。 */
    fun copyTo(out: OutputStream, offset: Long, length: Long)

    /** 取回 `[offset, offset+length)`；越界返回 null。只用于小数据（如一张 PTX）。 */
    fun readInto(offset: Long, length: Int): ByteArray?
}

/** 源文件里的一段区间：不解压、不拷贝。 */
internal class SourceSlice(
    private val src: ByteSource,
    private val start: Long,
    override val size: Long
) : Section {

    override fun copyTo(out: OutputStream, offset: Long, length: Long) {
        if (offset < 0 || length <= 0 || offset + length > size) return
        val buf = ByteArray(COPY_BUFFER)
        var done = 0L
        while (done < length) {
            val want = minOf(buf.size.toLong(), length - done).toInt()
            val n = src.readInto(start + offset + done, buf, 0, want)
            if (n <= 0) return
            out.write(buf, 0, n)
            done += n
        }
    }

    override fun readInto(offset: Long, length: Int): ByteArray? {
        if (offset < 0 || length < 0 || offset + length > size) return null
        return src.readFully(start + offset, length)
    }

    override fun close() = Unit   // 源由调用方持有

    private companion object {
        const val COPY_BUFFER = 64 * 1024
    }
}

/** 已经在内存里的一段字节。 */
internal class MemorySection(private val bytes: ByteArray) : Section {

    override val size: Long get() = bytes.size.toLong()

    override fun copyTo(out: OutputStream, offset: Long, length: Long) {
        if (offset < 0 || length <= 0 || offset + length > bytes.size) return
        out.write(bytes, offset.toInt(), length.toInt())
    }

    override fun readInto(offset: Long, length: Int): ByteArray? {
        if (offset < 0 || length < 0 || offset + length > bytes.size) return null
        return bytes.copyOfRange(offset.toInt(), offset.toInt() + length)
    }

    override fun close() = Unit
}

/**
 * 解压结果落在临时文件上的段。
 *
 * [close] 会**顺手删掉临时文件**，所以调用方必须保证条目已经全部提取完
 * （[SmfUnpacker.processSubgroup] 用 try/finally 保证）。
 */
internal class FileSection private constructor(
    private val file: File,
    private val raf: RandomAccessFile,
    override val size: Long
) : Section {

    private val channel: FileChannel = raf.channel

    override fun copyTo(out: OutputStream, offset: Long, length: Long) {
        if (offset < 0 || length <= 0 || offset + length > size) return
        val buf = ByteArray(64 * 1024)
        val bb = ByteBuffer.wrap(buf)
        var done = 0L
        while (done < length) {
            bb.clear()
            bb.limit(minOf(buf.size.toLong(), length - done).toInt())
            val n = channel.read(bb, offset + done)
            if (n <= 0) return
            out.write(buf, 0, n)
            done += n
        }
    }

    override fun readInto(offset: Long, length: Int): ByteArray? {
        if (offset < 0 || length < 0 || offset + length > size) return null
        val out = ByteArray(length)
        val bb = ByteBuffer.wrap(out)
        var pos = offset
        while (bb.hasRemaining()) {
            val n = channel.read(bb, pos)
            if (n <= 0) return null
            pos += n
        }
        return out
    }

    override fun close() {
        runCatching { channel.close() }
        runCatching { raf.close() }
        runCatching { file.delete() }
    }

    companion object {
        /** 打开已写好的临时文件；失败返回 null（调用方负责删除文件）。 */
        fun open(file: File): FileSection? = try {
            val raf = RandomAccessFile(file, "r")
            FileSection(file, raf, raf.length())
        } catch (e: Exception) {
            null
        }
    }
}
