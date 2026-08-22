package com.example.z_editor.datapack.smf

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.Inflater

/**
 * SMF unpacker — extracts every RSGP subgroup's files from an RSB container
 * (or a standalone RSGP) into a plain output directory using java.io.File.
 *
 * 1:1 port of scripts/pvz2_tool/smf_unpacker.py.  Reads go through SAF
 * (contentResolver), writes go to a real public folder via java.io.File.
 * The caller must hold MANAGE_EXTERNAL_STORAGE before writing.
 *
 * Deliberately shares SmfPacker's parseRsgpFileList / magic constants and
 * does NOT reuse any patching logic.  Data/image section decompression uses
 * a strict inflater (fails loudly instead of silently returning garbage).
 */
object SmfUnpacker {
    private const val TAG = "SmfUnpacker"
    private const val MAX_DECOMP_SIZE = 512_000_000

    data class UnpackOptions(
        /** Only extract entries whose internal path ends with .rton (case-insensitive). */
        val onlyRton: Boolean = false,
        /** Skip image entries entirely (still counted in progress). */
        val skipImages: Boolean = false,
        /** Only process subgroups whose name starts with this prefix. */
        val subgroupFilterPrefix: String? = null
    )

    data class UnpackResult(
        val fileCount: Int,
        val bytesWritten: Long,
        /** Image entries skipped due to the skipImages option. */
        val skippedImages: Int,
        /** Entries with size == 0. */
        val skippedZeroLength: Int,
        /** Entries whose offset+size fell outside the section. */
        val skippedOob: Int,
        /** Entries rejected by the path sanitizer (traversal / reserved name / etc). */
        val skippedUnsafePaths: Int,
        /** Entries skipped because the required data/image section was unavailable or corrupt. */
        val skippedInvalid: Int,
        /** Entries written under a sanitized path different from the internal path. */
        val sanitizedCount: Int,
        /** Number of subgroups actually processed (after filter + bounds checks). */
        val subgroupsProcessed: Int,
        val outputDir: File
    )

    /**
     * Blocking unpack.  Call from a background dispatcher (Dispatchers.IO);
     * onProgress runs on the calling thread (usually the same IO dispatcher).
     *
     * @param outputRootDir target directory (already created; e.g. /storage/emulated/0/Z_editor/<name>)
     */
    fun unpackSmf(
        context: Context,
        inputUri: Uri,
        outputRootDir: File,
        options: UnpackOptions = UnpackOptions(),
        onProgress: (done: Int, total: Int, name: String?) -> Unit
    ): Result<UnpackResult> {
        return try {
            if (!outputRootDir.mkdirs() && !outputRootDir.isDirectory) {
                return Result.failure(Exception("无法创建输出目录（可能缺少存储权限）"))
            }

            var rawBytes = context.contentResolver.openInputStream(inputUri)?.use { it.readBytes() }
                ?: return Result.failure(Exception("无法读取输入文件"))

            // ---- RSLB outer compression (new format) ----
            if (RslbDecompressor.isRslb(rawBytes)) {
                Log.i(TAG, "Detected RSLB outer compression")
                rawBytes = try {
                    RslbDecompressor.decompress(rawBytes)
                } catch (e: Exception) {
                    return Result.failure(Exception("RSLB 外层压缩解压失败: ${e.message}", e))
                }
                Log.i(TAG, "RSLB decompressed: ${rawBytes.size} bytes")
            }

            // ---- Outer PopCap Zlib compression (0xDEADFED4) ----
            val rawData: ByteArray
            if (isPopcapMagic(rawBytes)) {
                rawData = decompressOuter(rawBytes)
                    ?: return Result.failure(Exception("外层 PopCap 压缩解压失败"))
            } else {
                rawData = rawBytes
            }

            if (rawData.size < 4) {
                return Result.failure(Exception("文件太小，无法识别格式"))
            }
            val magic = rawData.copyOfRange(0, 4)

            val state = State(options, onProgress)
            when {
                magic.contentEquals(SmfPacker.RSB_MAGIC) -> unpackRsb(rawData, outputRootDir, state)
                magic.contentEquals(SmfPacker.RSGP_MAGIC) -> unpackRsgp(
                    rawData,
                    outputRootDir,
                    state
                )

                else -> {
                    val hex = magic.joinToString("") { "%02X".format(it) }
                    return Result.failure(Exception("未知文件格式: magic=$hex"))
                }
            }

            Result.success(
                UnpackResult(
                    fileCount = state.fileCount,
                    bytesWritten = state.bytesWritten,
                    skippedImages = state.skippedImages,
                    skippedZeroLength = state.skippedZeroLength,
                    skippedOob = state.skippedOob,
                    skippedUnsafePaths = state.skippedUnsafePaths,
                    skippedInvalid = state.skippedInvalid,
                    sanitizedCount = state.sanitizedCount,
                    subgroupsProcessed = state.subgroupsProcessed,
                    outputDir = outputRootDir
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "unpack failed", e)
            Result.failure(e)
        }
    }

    // ---- RSB container ----

    private fun unpackRsb(rawData: ByteArray, outputRootDir: File, state: State) {
        // RSB header: SUBGROUP_INFO_ENTRIES/OFFSET/ENTRY_SIZE at 40/44/48.
        val sgInfoEntries = rawData.readU32LE(40)
        val sgInfoOffset = rawData.readU32LE(44)
        val sgInfoEntrySize = rawData.readU32LE(48)
        val stride = sgInfoEntrySize.coerceIn(1, 65536)

        val subgroups = mutableListOf<Subgroup>()

        // ---- Pre-pass: read subgroup info + file lists, count progress total ----
        var pos = sgInfoOffset
        var i = 0
        while (i < sgInfoEntries && pos + stride <= rawData.size) {
            val name = readFixedCString(rawData, pos, 128)
            val rsgOffset = rawData.readU32LE(pos + 128)
            // Subgroup size on disk = image_data_offset + compressed_image_size.
            val rsgSize = rawData.readU32LE(pos + 164).toLong() + rawData.readU32LE(pos + 168)
            val infoStart = pos
            pos += stride
            i++

            val filter = state.options.subgroupFilterPrefix
            if (filter != null && !name.startsWith(filter, ignoreCase = true)) continue

            // Subgroup's own RSGP header must fit.
            if (rsgOffset < 0 || rsgOffset + 80 > rawData.size) continue

            // Authoritative RSGP fields come from the info table (offsets 140..172).
            val compFlags = rawData.readU32LE(infoStart + 140)
            val dataOffset = rawData.readU32LE(infoStart + 148)
            val compDataSize = rawData.readU32LE(infoStart + 152)
            val decompDataSize = rawData.readU32LE(infoStart + 156)
            val imageOffset = rawData.readU32LE(infoStart + 164)
            val compImageSize = rawData.readU32LE(infoStart + 168)
            val decompImageSize = rawData.readU32LE(infoStart + 172)

            // INFO_SIZE/OFFSET are read from the subgroup's own RSGP header (72/76).
            val infoSize = rawData.readU32LE(rsgOffset + 72)
            val infoOffset = rawData.readU32LE(rsgOffset + 76)
            if (infoOffset < 0 || infoSize < 0) continue
            if (rsgOffset.toLong() + infoOffset > rawData.size) continue

            val entries = SmfPacker.parseRsgpFileList(rawData, rsgOffset + infoOffset, infoSize)
            subgroups += Subgroup(
                rsgOffset, rsgSize, compFlags,
                dataOffset, compDataSize, decompDataSize,
                imageOffset, compImageSize, decompImageSize,
                entries
            )
            state.subgroupsProcessed++
        }

        countTotal(state, subgroups)
        for (sg in subgroups) processSubgroup(rawData, sg, outputRootDir, state)
    }

    // ---- Standalone RSGP ----

    private fun unpackRsgp(rawData: ByteArray, outputRootDir: File, state: State) {
        val compFlags = rawData.readU32LE(16)
        val dataOffset = rawData.readU32LE(24)
        val compDataSize = rawData.readU32LE(28)
        val decompDataSize = rawData.readU32LE(32)
        val imageOffset = rawData.readU32LE(40)
        val compImageSize = rawData.readU32LE(44)
        val decompImageSize = rawData.readU32LE(48)
        val infoSize = rawData.readU32LE(72)
        val infoOffset = rawData.readU32LE(76)
        if (infoOffset < 0 || infoSize < 0) {
            throw IllegalStateException("RSGP 文件列表偏移非法")
        }

        val entries = SmfPacker.parseRsgpFileList(rawData, infoOffset, infoSize)
        val sg = Subgroup(
            0, rawData.size.toLong(), compFlags,
            dataOffset, compDataSize, decompDataSize,
            imageOffset, compImageSize, decompImageSize,
            entries
        )
        state.subgroupsProcessed = 1

        countTotal(state, listOf(sg))
        processSubgroup(rawData, sg, outputRootDir, state)
    }

    // ---- Shared processing ----

    private fun countTotal(state: State, subgroups: List<Subgroup>) {
        val options = state.options
        for (sg in subgroups) {
            for (e in sg.entries) {
                if (e.name.isEmpty()) continue
                if (options.onlyRton && !e.name.endsWith(".rton", ignoreCase = true)) continue
                state.total++
            }
        }
    }

    private fun processSubgroup(
        rawData: ByteArray,
        sg: Subgroup,
        outputRootDir: File,
        state: State
    ) {
        val options = state.options

        // ---- Data section ----
        var data: ByteArray? = null
        if (sg.compFlags and 2 == 0) {
            data = sliceOrNull(rawData, sg.rsgOffset + sg.dataOffset, sg.compDataSize)
        } else if (sg.compDataSize != 0) {
            if (sg.decompDataSize in 1..MAX_DECOMP_SIZE) {
                val comp = sliceOrNull(rawData, sg.rsgOffset + sg.dataOffset, sg.compDataSize)
                if (comp != null) {
                    try {
                        data = inflate(comp, 0, nowrap = false)
                    } catch (e: Exception) {
                        Log.w(TAG, "data section decompress failed", e)
                        data = null
                    }
                }
            }
        } // else: no data section → data stays null

        // ---- Image section ----
        var image: ByteArray? = null
        if (sg.decompImageSize != 0) {
            if (sg.compFlags and 1 == 0) {
                image = sliceOrNull(rawData, sg.rsgOffset + sg.imageOffset, sg.compImageSize)
            } else if (sg.compImageSize != 0 && sg.decompImageSize in 1..MAX_DECOMP_SIZE) {
                val comp = sliceOrNull(rawData, sg.rsgOffset + sg.imageOffset, sg.compImageSize)
                if (comp != null) {
                    try {
                        image = inflate(comp, 0, nowrap = false)
                    } catch (e: Exception) {
                        Log.w(TAG, "image section decompress failed", e)
                        image = null
                    }
                }
            }
        }

        // ---- Extract files ----
        for (e in sg.entries) {
            if (e.name.isEmpty()) continue
            if (options.onlyRton && !e.name.endsWith(".rton", ignoreCase = true)) continue

            state.done++
            state.onProgress(state.done, state.total, e.name)

            if (options.skipImages && e.isImage) {
                state.skippedImages++
                continue
            }
            val section = if (e.isImage) image else data
            if (section == null) {
                state.skippedInvalid++
                continue
            }
            if (e.size == 0) {
                state.skippedZeroLength++
                continue
            }
            if (e.offset < 0 || e.size < 0 || e.offset.toLong() + e.size > section.size) {
                state.skippedOob++
                continue
            }
            val safeRel = sanitizePath(e.name)
            if (safeRel == null) {
                state.skippedUnsafePaths++
                continue
            }
            val target = File(outputRootDir, safeRel)
            target.parentFile?.mkdirs()
            try {
                FileOutputStream(target).use { it.write(section, e.offset, e.size) }
                state.fileCount++
                state.bytesWritten += e.size.toLong()
                if (safeRel != e.name) state.sanitizedCount++
            } catch (e2: Exception) {
                Log.w(TAG, "write failed: $safeRel", e2)
                state.skippedInvalid++
            }
        }
    }

    // ---- Outer PopCap decompression ----

    private fun isPopcapMagic(data: ByteArray): Boolean =
        data.size >= 4 && data[0] == 0xD4.toByte() && data[1] == 0xFE.toByte() &&
                data[2] == 0xAD.toByte() && data[3] == 0xDE.toByte()

    /**
     * Decompress the outer PopCap layer.  Real-world files use a zlib stream
     * starting at byte 8 (Python smf_unpacker.py: zlib.decompress(raw[8:]));
     * SmfPacker's own compress/decompress helpers assume raw deflate from
     * byte 8 / byte 12.  Try each candidate layout and validate the result
     * starts with a known container magic, so a correct parse wins regardless
     * of which convention produced the file.
     */
    private fun decompressOuter(rawBytes: ByteArray): ByteArray? {
        // (streamOffset, nowrap) candidates: byte-8 zlib header, byte-8 raw, byte-12 raw.
        val candidates = listOf(8 to false, 8 to true, 12 to false)
        for ((offset, nowrap) in candidates) {
            val out = try {
                inflate(rawBytes, offset, nowrap)
            } catch (e: Exception) {
                continue
            }
            if (out.size >= 4) {
                val m = out.copyOfRange(0, 4)
                if (m.contentEquals(SmfPacker.RSB_MAGIC) || m.contentEquals(SmfPacker.RSGP_MAGIC)) {
                    return out
                }
            }
        }
        return null
    }

    /** Strict bounded inflate. Throws on corrupt/truncated/oversized input. */
    private fun inflate(data: ByteArray, offset: Int, nowrap: Boolean): ByteArray {
        if (offset >= data.size) throw IllegalStateException("empty stream")
        val inflater = Inflater(nowrap)
        val out = ByteArrayOutputStream(8192)
        try {
            inflater.setInput(data, offset, data.size - offset)
            val buf = ByteArray(65536)
            var total = 0
            while (!inflater.finished()) {
                val n = inflater.inflate(buf)
                if (n > 0) {
                    out.write(buf, 0, n)
                    total += n
                    if (total > MAX_DECOMP_SIZE) throw IllegalStateException("解压数据过大")
                } else if (inflater.needsInput()) {
                    throw IllegalStateException("解压数据不完整")
                } else if (n == 0) {
                    throw IllegalStateException("解压停滞")
                }
            }
            return out.toByteArray()
        } finally {
            inflater.end()
        }
    }

    // ---- Helpers ----

    private fun sliceOrNull(data: ByteArray, offset: Int, size: Int): ByteArray? {
        if (offset < 0 || size < 0) return null
        if (offset.toLong() + size > data.size) return null
        return data.copyOfRange(offset, offset + size)
    }

    private fun ByteArray.readU32LE(offset: Int): Int {
        return (this[offset].toInt() and 0xFF) or
                ((this[offset + 1].toInt() and 0xFF) shl 8) or
                ((this[offset + 2].toInt() and 0xFF) shl 16) or
                ((this[offset + 3].toInt() and 0xFF) shl 24)
    }

    /** Fixed-width NUL-terminated string, matching Python's decode+rstrip(b'\x00'). */
    private fun readFixedCString(data: ByteArray, offset: Int, maxLen: Int): String {
        val len = minOf(maxLen, data.size - offset)
        if (len <= 0) return ""
        val s = String(data, offset, len, Charsets.UTF_8)
        return s.trimEnd('\u0000')
    }

    // ---- Path sanitizer ----

    private val RESERVED = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )
    private const val MAX_COMPONENT = 200
    private const val MAX_TOTAL_PATH = 3800

    /**
     * Sanitize an internal package path (already '/' separated) into a safe
     * relative path under the output root.  Returns null to skip the entry:
     *   - empty / NUL bytes
     *   - '..' traversal segments
     *   - Windows reserved device names (CON, PRN, COM1.., incl. "CON.txt")
     * Rewrites (returns a different path, counts as sanitized):
     *   - trailing dots/spaces (FAT/sdcardfs illegality)
     *   - illegal chars : * ? " < > |  → '_'
     *   - leading '.' → prefix '_' so files stay visible
     *   - segment > 200 chars → truncate
     *   - whole relative path > 3800 chars → flatten to a single component
     */
    private fun sanitizePath(internalPath: String): String? {
        if (internalPath.isEmpty()) return null
        if (internalPath.contains('\u0000')) return null

        val cleaned = mutableListOf<String>()
        for (raw in internalPath.split('/')) {
            if (raw == "..") return null
            if (raw.isEmpty() || raw == ".") continue
            var seg = raw.trimEnd(' ', '.')
            if (seg.isEmpty()) continue
            val base = seg.substringBefore('.').uppercase()
            if (base in RESERVED) return null
            seg = cleanSegment(seg)
            if (seg.startsWith(".")) seg = "_$seg"
            if (seg.length > MAX_COMPONENT) seg = seg.take(MAX_COMPONENT)
            cleaned.add(seg)
        }
        if (cleaned.isEmpty()) return null

        val joined = cleaned.joinToString("/")
        if (joined.length > MAX_TOTAL_PATH) {
            // Pathological: flatten everything into a single filename.
            var flat = cleanSegment(internalPath.replace('/', '_'))
            flat = flat.trimEnd(' ', '.')
            if (flat.isEmpty()) return null
            if (flat.length > MAX_TOTAL_PATH) flat = flat.take(MAX_TOTAL_PATH)
            return flat
        }
        return joined
    }

    private fun cleanSegment(seg: String): String = seg.map { c ->
        when (c) {
            ':', '*', '?', '"', '<', '>', '|', '\\' -> '_'
            else -> c
        }
    }.joinToString("")

    // ---- Internal state (mutable, per unpack call) ----

    private data class Subgroup(
        val rsgOffset: Int,
        val rsgSize: Long,
        val compFlags: Int,
        val dataOffset: Int,
        val compDataSize: Int,
        val decompDataSize: Int,
        val imageOffset: Int,
        val compImageSize: Int,
        val decompImageSize: Int,
        val entries: List<SmfPacker.RsgpFileEntry>
    )

    private class State(
        val options: UnpackOptions,
        val onProgress: (done: Int, total: Int, name: String?) -> Unit
    ) {
        var total = 0
        var done = 0
        var fileCount = 0
        var bytesWritten = 0L
        var skippedImages = 0
        var skippedZeroLength = 0
        var skippedOob = 0
        var skippedUnsafePaths = 0
        var skippedInvalid = 0
        var sanitizedCount = 0
        var subgroupsProcessed = 0
    }
}
