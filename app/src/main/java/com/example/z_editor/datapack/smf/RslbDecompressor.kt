package com.example.z_editor.datapack.smf

import org.tukaani.xz.LZMAInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

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
 */
object RslbDecompressor {

    private const val MAGIC = 0x424C5352 // "RSLB" little-endian
    private const val HEADER_SIZE = 0x20
    private const val BLOCK_ENTRY_SIZE = 0x24
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

    /**
     * Decompress an RSLB container into its inner SMF/RSB/RSGP payload.
     *
     * @throws IllegalArgumentException malformed header / magic mismatch
     * @throws IllegalStateException corrupt structure, truncated data, LZMA failure
     */
    fun decompress(data: ByteArray): ByteArray {
        if (data.size < HEADER_SIZE) throw IllegalArgumentException("RSLB 文件过小")
        val magic = data.readU32LE(0)
        if (magic != MAGIC) {
            throw IllegalArgumentException("不是 RSLB 格式 (magic=0x%08X)".format(magic))
        }

        val totalUncomp = data.readU64LE(0x08)
        val blockCount = data.readU32LE(0x1C)
        val headerEnd = HEADER_SIZE + blockCount.toLong() * BLOCK_ENTRY_SIZE
        if (headerEnd > data.size) throw IllegalStateException("RSLB 块表越界")

        if (blockCount < 1 || blockCount > 1_000_000) {
            throw IllegalStateException("RSLB 块数非法: $blockCount")
        }
        if (totalUncomp < 0 || totalUncomp > MAX_TOTAL_SIZE) {
            throw IllegalStateException("RSLB 解压尺寸非法: $totalUncomp")
        }

        val out = ByteArrayOutputStream(totalUncomp.toInt())
        var produced = 0L
        for (i in 0 until blockCount) {
            val entry = HEADER_SIZE + i * BLOCK_ENTRY_SIZE
            val cumOff = data.readU64LE(entry + 0x00)
            val uSize = data.readU32LE(entry + 0x08)
            val dataOff = data.readU64LE(entry + 0x10)
            val cSize = data.readU32LE(entry + 0x18)

            if (cumOff != produced) {
                throw IllegalStateException("块 $i 累计偏移不连续 ($cumOff != $produced)")
            }
            if (uSize < 0 || uSize > MAX_BLOCK_SIZE) {
                throw IllegalStateException("块 $i 解压尺寸非法: $uSize")
            }
            if (cSize < LZMA_PROPS_SIZE) {
                throw IllegalStateException("块 $i 压缩尺寸非法: $cSize")
            }
            if (dataOff + cSize > data.size) {
                throw IllegalStateException("块 $i 数据越界")
            }

            val dataStart = dataOff.toInt()
            val props = data.copyOfRange(dataStart, dataStart + LZMA_PROPS_SIZE)
            val src = data.copyOfRange(dataStart + LZMA_PROPS_SIZE, dataStart + cSize)

            out.write(decodeLzma(src, props, uSize))
            produced += uSize
        }

        val result = out.toByteArray()
        if (result.size.toLong() != totalUncomp) {
            throw IllegalStateException("RSLB 解压总大小 ${result.size} != 期望 $totalUncomp")
        }
        return result
    }

    private fun decodeLzma(compressed: ByteArray, props: ByteArray, expectedSize: Int): ByteArray {
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

        val out = ByteArrayOutputStream(expectedSize)
        try {
            LZMAInputStream(ByteArrayInputStream(compressed), expectedSize.toLong(), propsByte, dictSize).use { lzma ->
                val buf = ByteArray(65536)
                var total = 0
                while (total < expectedSize) {
                    val n = lzma.read(buf)
                    if (n < 0) throw IllegalStateException("LZMA 流提前结束")
                    if (n > 0) {
                        out.write(buf, 0, n)
                        total += n
                    }
                }
            }
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException("LZMA 解压失败: ${e.message}", e)
        }

        val result = out.toByteArray()
        if (result.size != expectedSize) {
            throw IllegalStateException("LZMA 解压大小不符")
        }
        return result
    }

    private fun ByteArray.readU32LE(offset: Int): Int =
        (this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8) or
            ((this[offset + 2].toInt() and 0xFF) shl 16) or
            ((this[offset + 3].toInt() and 0xFF) shl 24)

    private fun ByteArray.readU64LE(offset: Int): Long =
        (this[offset].toLong() and 0xFF) or
            ((this[offset + 1].toLong() and 0xFF) shl 8) or
            ((this[offset + 2].toLong() and 0xFF) shl 16) or
            ((this[offset + 3].toLong() and 0xFF) shl 24) or
            ((this[offset + 4].toLong() and 0xFF) shl 32) or
            ((this[offset + 5].toLong() and 0xFF) shl 40) or
            ((this[offset + 6].toLong() and 0xFF) shl 48) or
            ((this[offset + 7].toLong() and 0xFF) shl 56)
}
