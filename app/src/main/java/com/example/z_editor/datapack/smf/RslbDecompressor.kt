package com.example.z_editor.datapack.smf

import org.tukaani.xz.LZMAInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * RSLB outer-layer decompressor — pure Kotlin (JVM LZMA via org.tukaani:xz).
 *
 * Newer PvZ2 datapacks wrap the classic SMF/RSB/RSGP payload in an "RSLB"
 * LZMA block container.  Format (reverse-engineered in main/ by the reference
 * C tool):
 *
 *   0x00 u32 magic = "RSLB"
 *   0x04 u32 version = 1
 *   0x08 u64 total_uncompressed_size
 *   0x10 u64 packed_after_header (file_size - 0x20)
 *   0x18 u32 block_size (0x02000000 = 32 MiB)
 *   0x1C u32 block_count
 *   0x20 block table (block_count x 36 bytes):
 *       +0x00 u64 cum_uncompressed_offset
 *       +0x08 u32 uncompressed_size
 *       +0x0C u32 reserved (0)
 *       +0x10 u64 data_offset  -> this block's 5B LZMA props
 *       +0x18 u32 compressed_size (incl. the 5B props)
 *       +0x1C u8  lzma_props_byte
 *       +0x1D u32 dict_size
 *       +0x21 3B  padding (0)
 *   block data: per block = 5B props + LZMA1 stream (no end marker)
 *
 * Blocks decode independently with a KNOWN uncompressed size (no EOS marker
 * in the stream), so LZMAInputStream's known-size path is used.
 *
 * Decompression only — the app does not re-encode RSLB.
 *
 * **流式**：逐块解压直接写进调用方的 [OutputStream]，峰值内存是"一个块"
 * （≤ [MAX_BLOCK_SIZE]）而不是整个解压结果。原先的
 * `ByteArrayOutputStream(totalUncomp.toInt())` 本身就是一次几百 MB 的分配，
 * 是 500M 级数据包闪退的一环。[decompress] 那个 ByteArray 入口只是薄封装，
 * 仅供 packer 与单测使用。
 */
object RslbDecompressor {

    private const val MAGIC = 0x424C5352 // "RSLB" little-endian
    private const val HEADER_SIZE = 0x20L
    private const val BLOCK_ENTRY_SIZE = 0x24L
    private const val LZMA_PROPS_SIZE = 5

    /** Whole-file sanity bound (matches SmfUnpacker's MAX_DECOMP_SIZE). */
    private const val MAX_TOTAL_SIZE = 512_000_000L
    /** Per-block bound: 32 MiB nominal + slack, also guards LZMA dict allocation. */
    private const val MAX_BLOCK_SIZE = 64_000_000

    /** True if [data] starts with the RSLB magic ("RSLB" ascii bytes). */
    fun isRslb(data: ByteArray): Boolean =
        data.size >= 4 &&
            data[0] == 'R'.code.toByte() && data[1] == 'S'.code.toByte() &&
            data[2] == 'L'.code.toByte() && data[3] == 'B'.code.toByte()

    /** 同上，但只看前 4 字节，不把文件读进内存。 */
    internal fun isRslb(src: ByteSource): Boolean =
        src.size >= 4 && src.u32LE(0) == MAGIC

    /**
     * Decompress an RSLB container into its inner SMF/RSB/RSGP payload.
     *
     * @throws IllegalArgumentException malformed header / magic mismatch
     * @throws IllegalStateException corrupt structure, truncated data, LZMA failure
     */
    fun decompress(data: ByteArray): ByteArray {
        val src = data.asByteSource()
        // 预分配（与老实现一致）；越界的声明尺寸留给 decompressTo 里的检查去拒
        val declared = src.u64LE(0x08)
        val out = if (declared in 1..Int.MAX_VALUE.toLong()) {
            ByteArrayOutputStream(declared.toInt())
        } else {
            ByteArrayOutputStream()
        }
        decompressTo(src, out)
        return out.toByteArray()
    }

    /**
     * 流式解压进 [sink]。同一套校验，只是不再把结果攒在内存里。
     *
     * @throws IllegalArgumentException header 不合法 / magic 不符
     * @throws IllegalStateException 结构损坏、数据截断、LZMA 失败
     */
    internal fun decompressTo(src: ByteSource, sink: OutputStream) {
        if (src.size < HEADER_SIZE) throw IllegalArgumentException("RSLB 文件过小")
        val magic = src.u32LE(0)
        if (magic != MAGIC) {
            throw IllegalArgumentException("不是 RSLB 格式 (magic=0x%08X)".format(magic))
        }

        val totalUncomp = src.u64LE(0x08)
        val blockCount = src.u32LE(0x1C)
        val headerEnd = HEADER_SIZE + blockCount.toLong() * BLOCK_ENTRY_SIZE
        if (headerEnd > src.size) throw IllegalStateException("RSLB 块表越界")

        if (blockCount < 1 || blockCount > 1_000_000) {
            throw IllegalStateException("RSLB 块数非法: $blockCount")
        }
        if (totalUncomp < 0 || totalUncomp > MAX_TOTAL_SIZE) {
            throw IllegalStateException("RSLB 解压尺寸非法: $totalUncomp")
        }

        var produced = 0L
        for (i in 0 until blockCount) {
            val entry = HEADER_SIZE + i.toLong() * BLOCK_ENTRY_SIZE
            val cumOff = src.u64LE(entry)
            val uSize = src.u32LE(entry + 0x08)
            val dataOff = src.u64LE(entry + 0x10)
            val cSize = src.u32LE(entry + 0x18)

            if (cumOff != produced) {
                throw IllegalStateException("块 $i 累计偏移不连续 ($cumOff != $produced)")
            }
            if (uSize < 0 || uSize > MAX_BLOCK_SIZE) {
                throw IllegalStateException("块 $i 解压尺寸非法: $uSize")
            }
            if (cSize < LZMA_PROPS_SIZE) {
                throw IllegalStateException("块 $i 压缩尺寸非法: $cSize")
            }
            if (dataOff + cSize > src.size) {
                throw IllegalStateException("块 $i 数据越界")
            }

            val props = src.readFully(dataOff, LZMA_PROPS_SIZE)
                ?: throw IllegalStateException("块 $i LZMA props 越界")
            decodeLzmaTo(
                src = src,
                compStart = dataOff + LZMA_PROPS_SIZE,
                compLen = (cSize - LZMA_PROPS_SIZE).toLong(),
                props = props,
                expectedSize = uSize,
                sink = sink
            )
            produced += uSize
        }

        if (produced != totalUncomp) {
            throw IllegalStateException("RSLB 解压总大小 $produced != 期望 $totalUncomp")
        }
    }

    private fun decodeLzmaTo(
        src: ByteSource,
        compStart: Long,
        compLen: Long,
        props: ByteArray,
        expectedSize: Int,
        sink: OutputStream
    ) {
        // 5-byte props layout: [0] = LZMA props byte (lc/lp/pb), [1..4] = dict_size (LE).
        // xz 1.10's LZMAInputStream has no (InputStream, long, byte[]) ctor — the
        // props must be decomposed into (propsByte, dictSize). Validate dictSize
        // before constructing so a corrupt file can't trigger a huge allocation.
        val propsByte = props[0]
        val dictSize = (props[1].toInt() and 0xFF) or
            ((props[2].toInt() and 0xFF) shl 8) or
            ((props[3].toInt() and 0xFF) shl 16) or
            ((props[4].toInt() and 0xFF) shl 24)
        if (dictSize < 0 || dictSize > MAX_BLOCK_SIZE) {
            throw IllegalStateException("LZMA dict_size 非法: $dictSize")
        }
        if (compLen <= 0) throw IllegalStateException("LZMA 压缩数据为空")

        var total = 0L
        try {
            LZMAInputStream(
                src.window(compStart, compLen), expectedSize.toLong(), propsByte, dictSize
            ).use { lzma ->
                val buf = ByteArray(65536)
                while (total < expectedSize) {
                    val n = lzma.read(buf)
                    if (n < 0) throw IllegalStateException("LZMA 流提前结束")
                    if (n > 0) {
                        sink.write(buf, 0, n)
                        total += n
                    }
                }
            }
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException("LZMA 解压失败: ${e.message}", e)
        }

        if (total != expectedSize.toLong()) {
            throw IllegalStateException("LZMA 解压大小不符")
        }
    }
}
