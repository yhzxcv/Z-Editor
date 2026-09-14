package com.example.z_editor.datapack.smf

import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * 只读随机访问字节源。
 *
 * **为什么需要它**：SMF 数据包动辄几百 MB，原先的实现在
 * `inputStream.readBytes()` 那一步就把整个文件读成 ByteArray —— 500MB 的文件
 * 光读取（扩容 + `toByteArray()` 拷贝）峰值就 ~1GB，直接 `OutOfMemoryError`。
 * 而 `OutOfMemoryError` 是 `Error` 不是 `Exception`，会穿透
 * `catch (e: Exception)` 让应用闪退，连错误文案都来不及给。
 *
 * 改成"按需定位读取"后，解包链路的常驻内存与文件大小无关。
 *
 * **契约：越界读取一律返回 0 / null / 空串，绝不抛异常。**
 * 这条是从 [RsbTextureIndex] 一路延续下来的——坏容器应该被跳过（并计入
 * `skippedOob` / `skippedInvalid`），而不是让整次解包崩掉。真正的越界判定
 * 由调用方（条目 offset+size 与段大小的比较）负责。
 *
 * 读取方法**不是线程安全的**（[ChannelByteSource] 复用了内部暂存缓冲）。
 * 解包链路全程单线程，够用。
 */
internal interface ByteSource : Closeable {

    /** 总字节数。 */
    val size: Long

    /** 小端 u32；越界返回 0。 */
    fun u32LE(offset: Long): Int

    /** 小端 u64；越界返回 0。 */
    fun u64LE(offset: Long): Long

    /** 读取 [len] 字节；越界返回 null。 */
    fun readFully(offset: Long, len: Int): ByteArray?

    /** 读取最多 [len] 字节到 [dest]；返回实际读到的字节数，越界/失败返回 0。 */
    fun readInto(offset: Long, dest: ByteArray, destOff: Int, len: Int): Int

    /** 定长字段里的 NUL 结尾字符串（在第一个 NUL 处截断）。 */
    fun fixedCString(offset: Long, maxLen: Int): String

    /** [start, start+length) 这段区间的只读流（LZMA 等需要 InputStream 的地方用）。 */
    fun window(start: Long, length: Long): InputStream = ByteSourceWindow(this, start, length)
}

/** 无符号解释：u32 读进来是 Int，转 Long 时按无符号处理（>2GB 的偏移不会变负数）。 */
internal fun Int.toU32(): Long = toLong() and 0xFFFFFFFFL

/** u32 字段读成"非负 Long"，越界返回 0。 */
internal fun ByteSource.u32At(offset: Long): Long = u32LE(offset).toU32()

// ---- 内存实现 ----

/** 把已经在内存里的 ByteArray 包成 ByteSource（单测、小文件回退路径用）。 */
internal class ArrayByteSource(private val data: ByteArray) : ByteSource {

    override val size: Long get() = data.size.toLong()

    override fun u32LE(offset: Long): Int {
        if (offset < 0 || offset + 4 > data.size) return 0
        val i = offset.toInt()
        return (data[i].toInt() and 0xFF) or
                ((data[i + 1].toInt() and 0xFF) shl 8) or
                ((data[i + 2].toInt() and 0xFF) shl 16) or
                ((data[i + 3].toInt() and 0xFF) shl 24)
    }

    override fun u64LE(offset: Long): Long = composeLong(offset)

    override fun readFully(offset: Long, len: Int): ByteArray? {
        if (offset < 0 || len < 0 || offset + len > data.size) return null
        return data.copyOfRange(offset.toInt(), offset.toInt() + len)
    }

    override fun readInto(offset: Long, dest: ByteArray, destOff: Int, len: Int): Int {
        if (offset < 0 || len <= 0 || offset >= data.size) return 0
        if (destOff < 0 || destOff + len > dest.size) return 0
        val n = minOf(len.toLong(), data.size - offset).toInt()
        System.arraycopy(data, offset.toInt(), dest, destOff, n)
        return n
    }

    override fun fixedCString(offset: Long, maxLen: Int): String {
        if (offset < 0 || offset >= data.size) return ""
        val len = minOf(maxLen.toLong(), data.size - offset).toInt()
        if (len <= 0) return ""
        return cutAtNul(String(data, offset.toInt(), len, Charsets.UTF_8))
    }

    private fun composeLong(offset: Long): Long {
        if (offset < 0 || offset + 8 > data.size) return 0
        var v = 0L
        for (i in 0 until 8) v = v or ((data[offset.toInt() + i].toLong() and 0xFF) shl (i * 8))
        return v
    }

    override fun close() = Unit   // 内存里的数组没什么可关的
}

internal fun ByteArray.asByteSource(): ByteSource = ArrayByteSource(this)

// ---- 文件实现 ----

/**
 * [FileChannel] 支持的随机访问源。
 *
 * 既能包装 app 自己开的临时文件，也能直接包装 SAF 给的
 * `ParcelFileDescriptor` 的 fd —— 后者意味着**大包不必先复制一份到临时目录**。
 */
internal class ChannelByteSource(
    private val channel: FileChannel,
    override val size: Long,
    private val onClose: () -> Unit = {}
) : ByteSource {

    /** u32/u64 的暂存区；单线程复用，避免每次读头都分配。 */
    private val scratch = ByteArray(8)

    override fun u32LE(offset: Long): Int {
        if (offset < 0 || offset + 4 > size) return 0
        if (readInto(offset, scratch, 0, 4) != 4) return 0
        return (scratch[0].toInt() and 0xFF) or
                ((scratch[1].toInt() and 0xFF) shl 8) or
                ((scratch[2].toInt() and 0xFF) shl 16) or
                ((scratch[3].toInt() and 0xFF) shl 24)
    }

    override fun u64LE(offset: Long): Long {
        if (offset < 0 || offset + 8 > size) return 0
        if (readInto(offset, scratch, 0, 8) != 8) return 0
        var v = 0L
        for (i in 0 until 8) v = v or ((scratch[i].toLong() and 0xFF) shl (i * 8))
        return v
    }

    override fun readFully(offset: Long, len: Int): ByteArray? {
        if (offset < 0 || len < 0 || offset + len > size) return null
        if (len == 0) return ByteArray(0)
        val out = ByteArray(len)
        return if (readInto(offset, out, 0, len) == len) out else null
    }

    override fun readInto(offset: Long, dest: ByteArray, destOff: Int, len: Int): Int {
        if (offset < 0 || len <= 0) return 0
        if (destOff < 0 || destOff + len > dest.size) return 0
        if (offset >= size) return 0
        val want = minOf(len.toLong(), size - offset).toInt()
        if (want <= 0) return 0
        val buf = ByteBuffer.wrap(dest, destOff, want)
        var pos = offset
        var read = 0
        try {
            while (buf.hasRemaining()) {
                // 定位读（pread）：不依赖也不改变通道自身的 position
                val n = channel.read(buf, pos)
                if (n <= 0) break
                pos += n
                read += n
            }
        } catch (e: Exception) {
            return read
        }
        return read
    }

    override fun fixedCString(offset: Long, maxLen: Int): String {
        if (offset < 0 || offset >= size) return ""
        val len = minOf(maxLen.toLong(), size - offset).toInt()
        if (len <= 0) return ""
        val raw = readFully(offset, len) ?: return ""
        return cutAtNul(String(raw, Charsets.UTF_8))
    }

    override fun close() {
        runCatching { channel.close() }
        onClose()
    }
}

/** 打开一个本地文件作随机访问源；失败返回 null。 */
internal fun openFileSource(file: File): ByteSource? = try {
    val raf = RandomAccessFile(file, "r")
    ChannelByteSource(raf.channel, raf.length()) { runCatching { raf.close() } }
} catch (e: Exception) {
    null
}

// ---- 区间流 ----

private class ByteSourceWindow(
    private val src: ByteSource,
    private val start: Long,
    private val length: Long
) : InputStream() {

    private var pos = 0L
    private val one = ByteArray(1)

    override fun read(): Int {
        if (pos >= length) return -1
        val n = src.readInto(start + pos, one, 0, 1)
        if (n <= 0) return -1
        pos++
        return one[0].toInt() and 0xFF
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        if (pos >= length) return -1
        val want = minOf(len.toLong(), length - pos).toInt()
        if (want <= 0) return -1
        val n = src.readInto(start + pos, b, off, want)
        if (n <= 0) return -1
        pos += n
        return n
    }

    override fun available(): Int =
        (length - pos).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
}

private fun cutAtNul(s: String): String {
    val nul = s.indexOf('\u0000')
    return if (nul >= 0) s.substring(0, nul) else s
}
