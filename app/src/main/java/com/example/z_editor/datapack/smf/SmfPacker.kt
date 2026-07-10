package com.example.z_editor.datapack.smf

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * SMF/RSB Packer — injects patched files into RSB containers.
 *
 * 1:1 port of scripts/pvz2_tool/smf_packer.py
 *
 * The packer uses Python's "in-place slice assignment" pattern throughout:
 * patching modifies a mutable byte array with automatic offset cascading.
 *
 * Usage:
 *   SmfPacker.packSmf(context, templateUri, patchDirUri, outputUri, outputName)
 *
 * Patch matching: files in [patchDir] are matched by basename against
 * entries inside the RSB subgroups. Only modified files need to be placed.
 */
object SmfPacker {

    private const val TAG = "SmfPacker"
    private const val POPCAP_ZLIB_MAGIC = 0xDEADFED4.toInt()

    private val RSB_MAGIC = byteArrayOf('1'.code.toByte(), 'b'.code.toByte(), 's'.code.toByte(), 'r'.code.toByte())
    private val RSGP_MAGIC = byteArrayOf('p'.code.toByte(), 'g'.code.toByte(), 's'.code.toByte(), 'r'.code.toByte())

    // ---- Public data classes ----

    data class PackResult(
        val outputName: String,
        val patchesApplied: Int,
        val subgroupsModified: Int,
        val outputSize: Long
    )

    data class ConflictError(
        val basename: String,
        val subgroups: List<String>
    )

    // ---- Internal data classes ----

    /**
     * Parsed entry from RSGP info section file list.
     * Matches the dict returned by smf_packer.py:_parse_rsgp_file_list.
     *
     * @param infoPos byte position immediately AFTER the size field in subdata.
     *   Equivalent to Python's `entry_pos = file.tell()` after reading fsize.
     *   Used by the legacy patcher for `subdata[file_info - 4:file_info]` etc.
     */
    private data class RsgpFileEntry(
        val name: String,
        val isImage: Boolean,
        val offset: Int,
        val size: Int,
        val sizePos: Int,
        val offsetPos: Int
    ) {
        /** Position after the size field (= sizePos + 4). Python `entry_pos`. */
        val infoPos: Int get() = sizePos + 4
    }

    /** Entry in data_dict / image_dict for the legacy look-back-one patcher. */
    private data class FileDictEntry(
        val fileInfo: Int,
        val fileOffset: Int
    )

    // =========================================================================
    // PatchIndex — pre-indexed patch files for O(1) basename lookup
    // =========================================================================

    /**
     * Pre-indexed patch files for O(1) basename lookup.
     *
     * Replaces repeated [DocumentFile.findFile] calls (SAF IPC + directory scan)
     * with a single upfront scan and in-memory [HashMap] lookups.
     *
     * Lookup is **case-insensitive** because PvZ2 RSGP internal paths may differ
     * in case from the actual filesystem names (e.g. "PLANT.rton" vs "plant.rton"),
     * and SAF [DocumentFile.findFile] delegates to the filesystem which is often
     * case-insensitive (FAT32/exFAT).
     */
    private class PatchIndex(
        private val bytesMap: Map<String, ByteArray>,
        private val lowerMap: Map<String, String>,
        val fileCount: Int
    ) {
        fun contains(basename: String): Boolean {
            if (bytesMap.containsKey(basename)) return true
            return lowerMap.containsKey(basename.lowercase())
        }

        fun getBytes(basename: String): ByteArray? {
            bytesMap[basename]?.let { return it }
            val lowerKey = lowerMap[basename.lowercase()] ?: return null
            return bytesMap[lowerKey]
        }
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Pack modified files into an RSB container.
     *
     * 1:1 port of smf_packer.py:pack_smf (lines 398-500).
     *
     * @param templateUri SAF URI of the template .rsb/.smf file
     * @param patchDirUri SAF tree URI of the patches directory
     * @param outputUri   SAF tree URI of the output directory
     * @param outputName  Desired output filename (e.g. "levels_patched.smf")
     */
    fun packSmf(
        context: Context,
        templateUri: Uri,
        patchDirUri: Uri,
        outputUri: Uri,
        outputName: String
    ): Result<PackResult> {
        return try {
            // Build patch index — O(N) scan once, O(1) lookups thereafter
            val patchIndex = buildPatchIndex(context, patchDirUri)
                ?: return Result.failure(Exception("无法访问补丁目录"))

            Log.d(TAG, "Pack: ${patchIndex.fileCount} patches indexed")

            // Read template
            val rawBytes = context.contentResolver.openInputStream(templateUri)?.use { it.readBytes() }
                ?: return Result.failure(Exception("无法读取模板文件"))

            // ---- Step 1: Handle outer PopCap Zlib compression (0xDEADFED4) ----
            val outerCompressed = rawBytes.size >= 4 &&
                rawBytes[0] == 0xD4.toByte() && rawBytes[1] == 0xFE.toByte() &&
                rawBytes[2] == 0xAD.toByte() && rawBytes[3] == 0xDE.toByte()

            var rawData: ByteArray
            if (outerCompressed) {
                Log.d(TAG, "Detected PopCap Zlib outer compression")
                rawData = popcapZlibDecompress(rawBytes)
                Log.d(TAG, "Decompressed: ${rawBytes.size} → ${rawData.size} bytes")
            } else {
                rawData = rawBytes
            }

            // ---- Step 2: Route by magic ----
            if (rawData.size < 4) {
                return Result.failure(Exception("文件太小，无法识别格式"))
            }
            val magic = rawData.copyOfRange(0, 4)

            var anyModified: Boolean
            var subgroupsModified = 0
            var patchesApplied = 0
            when {
                magic.contentEquals(RSB_MAGIC) -> {
                    Log.d(TAG, "Detected RSB container")
                    val rsbResult = patchRsbLegacy(rawData, patchIndex)

                    // Check for basename conflicts
                    if (rsbResult.conflicts.isNotEmpty()) {
                        val msg = rsbResult.conflicts.joinToString("\n") { c ->
                            "'${c.basename}' 存在于: ${c.subgroups.joinToString(", ")}"
                        }
                        return Result.failure(Exception("文件名冲突 — 以下补丁在多个子组中重名:\n$msg"))
                    }

                    rawData = rsbResult.data
                    anyModified = rsbResult.anyModified
                    subgroupsModified = rsbResult.subgroupsModified
                    patchesApplied = rsbResult.patchesApplied
                }
                magic.contentEquals(RSGP_MAGIC) -> {
                    Log.d(TAG, "Detected standalone RSGP container")
                    val (patched, wasModified) = patchRsgpLegacy(rawData, patchIndex)
                    rawData = patched
                    anyModified = wasModified
                    if (wasModified) {
                        subgroupsModified = 1
                        patchesApplied = patchIndex.fileCount
                    }
                }
                else -> {
                    val hex = magic.joinToString("") { "%02X".format(it) }
                    return Result.failure(Exception("未知文件格式: magic=$hex"))
                }
            }

            if (!anyModified) {
                // No modifications — write original bytes as-is
                val outDir = DocumentFile.fromTreeUri(context, outputUri)!!
                val existing = outDir.findFile(outputName)
                if (existing != null) existing.delete()
                val outFile = outDir.createFile("application/octet-stream", outputName)
                    ?: return Result.failure(Exception("无法创建输出文件"))
                context.contentResolver.openOutputStream(outFile.uri)?.use { it.write(rawBytes) }
                Log.d(TAG, "No modifications — copied original as-is")
                return Result.success(PackResult(outputName, 0, 0, rawBytes.size.toLong()))
            }

            // ---- Step 3: Re-apply outer compression ----
            val finalData: ByteArray
            if (outerCompressed) {
                finalData = popcapZlibCompress(rawData)
                Log.d(TAG, "Re-compressed: ${rawBytes.size} → ${finalData.size} bytes")
            } else {
                finalData = rawData
            }

            // Write output
            val outDir = DocumentFile.fromTreeUri(context, outputUri)!!
            val existing = outDir.findFile(outputName)
            if (existing != null) existing.delete()
            val outFile = outDir.createFile("application/octet-stream", outputName)
                ?: return Result.failure(Exception("无法创建输出文件"))
            context.contentResolver.openOutputStream(outFile.uri)?.use { it.write(finalData) }

            Log.d(TAG, "Written: $outputName (${finalData.size} bytes), " +
                "$patchesApplied patches in $subgroupsModified subgroups")
            Result.success(PackResult(outputName, patchesApplied, subgroupsModified, finalData.size.toLong()))
        } catch (e: Exception) {
            Log.e(TAG, "packSmf failed", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // Patch index builder
    // =========================================================================

    /**
     * Scan [patchDirUri] once and cache all patch file bytes in memory.
     */
    private fun buildPatchIndex(context: Context, patchDirUri: Uri): PatchIndex? {
        val patchDir = DocumentFile.fromTreeUri(context, patchDirUri) ?: return null
        val files = patchDir.listFiles()
        val map = mutableMapOf<String, ByteArray>()
        val lowerMap = mutableMapOf<String, String>()

        for (file in files) {
            if (file.isDirectory) continue
            val rawName = file.name
            if (rawName.isNullOrEmpty()) continue
            val basename = rawName.substringAfterLast('/').substringAfterLast('\\')
            if (basename.isEmpty()) continue
            try {
                val bytes = context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
                if (bytes != null) {
                    map[basename] = bytes
                    lowerMap[basename.lowercase()] = basename
                }
            } catch (_: Exception) {
                // Skip unreadable files
            }
        }

        Log.d(TAG, "PatchIndex: ${map.size} files cached")
        if (map.isEmpty() && files.isNotEmpty()) {
            Log.w(TAG, "All ${files.size} entries were filtered out — check permissions")
        }
        return PatchIndex(map, lowerMap, map.size)
    }

    // =========================================================================
    // File list parsing — 1:1 _parse_rsgp_file_list (lines 33-89)
    // =========================================================================

    /**
     * Parse the prefix-compressed file list from an RSGP subgroup's info section.
     *
     * Character-by-character port of smf_packer.py:_parse_rsgp_file_list.
     *
     * Critical invariants:
     * - [nameDict] keys are always "one character behind" the current fileName.
     *   They do NOT include the just-read byte. This matches Python's
     *   `file_name += byte_char` (adds PREVIOUS byte, then reads NEXT byte).
     * - Every character (including NUL) has a 3-byte u24 length field after it.
     *   NUL's length must be consumed before exiting the inner loop.
     * - Prefix selection: iterate in insertion order (LinkedHashMap), last
     *   non-expired key wins (longest active prefix in a linear chain).
     */
    private fun parseRsgpFileList(
        subdata: ByteArray,
        infoOffset: Int,
        infoSize: Int
    ): List<RsgpFileEntry> {
        val infoLimit = minOf(infoOffset + infoSize, subdata.size)
        val nameDict = mutableMapOf<String, Int>()  // LinkedHashMap — insertion order
        val entries = mutableListOf<RsgpFileEntry>()

        var pos = infoOffset

        while (pos + 3 < infoLimit) {
            // Clean expired prefixes & pick the last non-expired one
            var fileName = ""
            val iter = nameDict.iterator()
            while (iter.hasNext()) {
                val (key, cov) = iter.next()
                if (cov + infoOffset < pos) {
                    iter.remove()
                } else {
                    fileName = key
                }
            }

            // Read characters until NUL terminator.
            // Python: `byte_char = b""` then `while byte_char != b"\x00" ...`
            // First iteration adds b"" (no-op), reads first real char.
            // When NUL is read: loop body still executes (reads NUL's length),
            // then next iteration's check fails.
            var prevByte: Int = -1  // -1 = no previous byte (Python's b"")
            var first = true
            while (pos + 3 < infoLimit) {
                if (!first && prevByte == 0) break  // NUL → exit inner loop
                if (!first) {
                    fileName += prevByte.toChar()
                }

                // Read current byte
                prevByte = subdata[pos].toInt() and 0xFF
                pos++

                // Read 3-byte length (every byte has one, including NUL)
                val u24 = subdata.readU24LE(pos)
                pos += 3
                val length = 4 * u24
                if (length != 0) {
                    // Store with fileName BEFORE adding current byte (matches Python)
                    nameDict[fileName] = length
                }

                first = false
            }

            if (pos + 12 > infoLimit) break

            // Read entry metadata (RsgpPart0ExtraInfo)
            val isImage = subdata.readU32LE(pos) == 1
            pos += 4
            val foffset = subdata.readU32LE(pos)
            pos += 4
            val fsize = subdata.readU32LE(pos)
            val sizePos = pos
            pos += 4

            if (isImage) {
                pos += 20  // RsgpPart1ExtraInfo
            }

            val decodedName = fileName.replace('\\', '/')

            if (decodedName.isNotEmpty()) {
                entries.add(RsgpFileEntry(
                    name = decodedName,
                    isImage = isImage,
                    offset = foffset,
                    size = fsize,
                    sizePos = sizePos,
                    offsetPos = sizePos - 4
                ))
            }
        }

        return entries
    }

    /**
     * Extract all file names from an RSGP subgroup for duplicate-basename pre-scan.
     * 1:1 _get_rsgp_file_names (lines 92-99).
     */
    private fun getRsgpFileNames(subdata: ByteArray): List<String> {
        if (subdata.size < 80) return emptyList()
        val infoSize = subdata.readU32LE(72)
        val infoOffset = subdata.readU32LE(76)
        val entries = parseRsgpFileList(subdata, infoOffset, infoSize)
        return entries.map { it.name }.filter { it.isNotEmpty() }
    }

    // =========================================================================
    // _patch_rsgp_legacy — 1:1 port (lines 514-703)
    // =========================================================================

    /**
     * Full-featured RSGP patching with zlib compression support.
     *
     * 1:1 port of smf_packer.py:_patch_rsgp_legacy.
     *
     * Key behaviors that must match Python exactly:
     * 1. "Look-back-one" file patching: process entry N-1 while iterating on entry N
     * 2. 4096 padding BEFORE compression (extend_to_4096 on decompressed data)
     * 3. Image section overwrites info section: subdata[image_data_offset:] = image_data
     * 4. RSGP header fields updated in-place on subdata
     * 5. Final subdata padded to 4096 alignment
     *
     * @param subdata The RSGP subgroup byte array (modified in-place style)
     * @param patchIndex Pre-indexed patch files
     * @param overrideDataComp Override data compression flag (-1 = use original)
     * @param overrideImageComp Override image compression flag (-1 = use original)
     * @return Pair of (patched_subdata, was_modified)
     */
    private fun patchRsgpLegacy(
        subdata: ByteArray,
        patchIndex: PatchIndex,
        overrideDataComp: Int = -1,
        overrideImageComp: Int = -1
    ): Pair<ByteArray, Boolean> {
        // ---- Parse RSGP header (80 bytes) ----
        // Layout (offsets from start):
        //   [0:4]pgsr [4:8]ver [8:16]pad [16:20]comp_flags [20:24]header_len
        //   [24:28]data_off [28:32]comp_data [32:36]decomp_data
        //   [36:40]pad [40:44]img_off [44:48]comp_img [48:52]decomp_img
        //   [52:72]pad [72:76]info_size [76:80]info_off
        var compFlags = subdata.readU32LE(16)
        val dataOffset = subdata.readU32LE(24)
        val compDataSize = subdata.readU32LE(28)
        val decompDataSize = subdata.readU32LE(32)
        val imageDataOffset = subdata.readU32LE(40)
        val compImageSize = subdata.readU32LE(44)
        val decompImageSize = subdata.readU32LE(48)
        val infoSize = subdata.readU32LE(72)
        val infoOffset = subdata.readU32LE(76)

        // ---- Parse file list and build data_dict / image_dict ----
        val entries = parseRsgpFileList(subdata, infoOffset, infoSize)

        // Build dicts with sentinel entries (Python lines 543-579)
        // Sentinel: key="" with file_offset = decomp_size (marks end boundary)
        val dataDict = mutableMapOf<String, FileDictEntry>()
        val imageDict = mutableMapOf<String, FileDictEntry>()
        dataDict[""] = FileDictEntry(0, decompDataSize)
        imageDict[""] = FileDictEntry(0, decompImageSize)

        for (e in entries) {
            if (e.isImage) {
                imageDict[e.name] = FileDictEntry(e.infoPos, e.offset)
            } else {
                dataDict[e.name] = FileDictEntry(e.infoPos, e.offset)
            }
        }

        // ---- Early return: no patches (basename-only lookup) ----
        var hasAnyPatch = false
        for ((name, _) in dataDict) {
            if (name.isNotEmpty() && patchIndex.contains(basenameOf(name))) {
                hasAnyPatch = true; break
            }
        }
        if (!hasAnyPatch) {
            for ((name, _) in imageDict) {
                if (name.isNotEmpty() && patchIndex.contains(basenameOf(name))) {
                    hasAnyPatch = true; break
                }
            }
        }
        if (!hasAnyPatch) {
            return Pair(subdata, false)
        }

        // ---- Decompress data section ----
        var data: ByteArray
        if (compFlags and 2 == 0) {
            // Uncompressed
            data = subdata.copyOfRange(dataOffset, dataOffset + compDataSize)
        } else if (compDataSize != 0) {
            data = zlibDecompress(subdata.copyOfRange(dataOffset, dataOffset + compDataSize))
        } else {
            data = ByteArray(0)
        }

        // ---- Decompress image section ----
        var imageData: ByteArray
        if (decompImageSize != 0) {
            if (compFlags and 1 == 0) {
                imageData = subdata.copyOfRange(imageDataOffset, imageDataOffset + compImageSize)
            } else {
                imageData = zlibDecompress(subdata.copyOfRange(imageDataOffset, imageDataOffset + compImageSize))
            }
        } else {
            imageData = ByteArray(0)
        }

        // ---- Patch data files (look-back-one pattern) ----
        var decodedName = ""
        var dataShift = 0
        var fileOffset = 0  // adjusted start of current file in data
        var dataPatchCount = 0

        val sortedDataNames = dataDict.keys.sortedBy { dataDict[it]!!.fileOffset }
        for (nameNew in sortedDataNames) {
            var newOffset = dataShift + dataDict[nameNew]!!.fileOffset

            if (decodedName.isNotEmpty()) {
                val fileInfo = dataDict[decodedName]!!.fileInfo
                val patchBytes = patchIndex.getBytes(basenameOf(decodedName))

                if (patchBytes != null) {
                    // Grow data if needed
                    val needed = fileOffset + patchBytes.size
                    if (needed > data.size) {
                        data = data + ByteArray(needed - data.size)
                    }
                    // data[file_offset:new_offset] = patch_data
                    data = byteArraySliceAssign(data, fileOffset, newOffset, patchBytes)
                    // Update size in subdata: subdata[file_info-4:file_info]
                    subdata.writeU32LE(fileInfo - 4, patchBytes.size)
                    dataShift += fileOffset + patchBytes.size - newOffset
                    newOffset = fileOffset + patchBytes.size
                    dataPatchCount++
                    Log.d(TAG, "    [PATCH] $decodedName (${patchBytes.size} bytes)")
                }
                // Update offset in subdata: subdata[file_info-8:file_info-4]
                subdata.writeU32LE(fileInfo - 8, fileOffset)
            }
            fileOffset = newOffset
            decodedName = nameNew
        }

        // ---- Patch image files (look-back-one pattern) ----
        decodedName = ""
        var imageShift = 0
        fileOffset = 0
        var imagePatchCount = 0

        val sortedImageNames = imageDict.keys.sortedBy { imageDict[it]!!.fileOffset }
        for (nameNew in sortedImageNames) {
            var newOffset = imageShift + imageDict[nameNew]!!.fileOffset

            if (decodedName.isNotEmpty()) {
                val fileInfo = imageDict[decodedName]!!.fileInfo
                val patchBytes = patchIndex.getBytes(basenameOf(decodedName))

                if (patchBytes != null && patchBytes.isNotEmpty()) {
                    val needed = fileOffset + patchBytes.size
                    if (needed > imageData.size) {
                        imageData = imageData + ByteArray(needed - imageData.size)
                    }
                    imageData = byteArraySliceAssign(imageData, fileOffset, newOffset, patchBytes)
                    subdata.writeU32LE(fileInfo - 24, patchBytes.size)
                    imageShift += fileOffset + patchBytes.size - newOffset
                    newOffset = fileOffset + patchBytes.size
                    imagePatchCount++
                    Log.d(TAG, "    [PATCH] $decodedName (${patchBytes.size} bytes, image)")
                }
                subdata.writeU32LE(fileInfo - 28, fileOffset)
            }
            fileOffset = newOffset
            decodedName = nameNew
        }

        val patchCount = dataPatchCount + imagePatchCount
        if (patchCount == 0) {
            return Pair(subdata, false)
        }

        // ---- Working copy of subdata (Python modifies in-place; we build new) ----
        var result = subdata.copyOf()

        // ---- Recompress and write data section ----
        if (overrideDataComp >= 0) {
            compFlags = (compFlags and 2.inv()) or overrideDataComp
        }
        // 4096 padding BEFORE compression (Python line 662)
        data = data + extendTo4096(data.size)
        val newDecompDataSize = data.size
        val newCompDataSize: Int
        val finalData: ByteArray

        if (compFlags and 2 == 0) {
            // Uncompressed
            newCompDataSize = newDecompDataSize
            finalData = data
        } else {
            val compressed = zlibCompress(data, 9)
            val compAligned = compressed + extendTo4096(compressed.size)
            newCompDataSize = compAligned.size
            finalData = compAligned
            Log.d(TAG, "    Data section: $newDecompDataSize → $newCompDataSize bytes")
        }

        // result[data_offset:image_data_offset] = final_data
        result = byteArraySliceAssign(result, dataOffset, imageDataOffset, finalData)
        // result[28:36] = struct.pack('<II', comp_data_size, decomp_data_size)
        result.writeU32LE(28, newCompDataSize)
        result.writeU32LE(32, newDecompDataSize)
        // result[40:44] = struct.pack('<I', data_offset + comp_data_size)
        val newImageOffset = dataOffset + newCompDataSize
        result.writeU32LE(40, newImageOffset)

        // ---- Recompress and write image section ----
        if (imageData.isNotEmpty()) {
            if (overrideImageComp >= 0) {
                compFlags = (compFlags and 1.inv()) or overrideImageComp
            }
            // 4096 padding BEFORE compression (Python line 685)
            imageData = imageData + extendTo4096(imageData.size)
            val newDecompImageSize = imageData.size
            val newCompImageSize: Int
            val finalImage: ByteArray

            if (compFlags and 1 == 0) {
                newCompImageSize = newDecompImageSize
                finalImage = imageData
            } else {
                val compressed = zlibCompress(imageData, 9)
                val compAligned = compressed + extendTo4096(compressed.size)
                newCompImageSize = compAligned.size
                finalImage = compAligned
            }

            // result[image_data_offset:] = image_data   ← overwrites info section! (Python line 698)
            result = byteArrayReplaceFrom(result, newImageOffset, finalImage)
            // result[44:52] = struct.pack('<II', comp_image_size, decomp_image_size)
            result.writeU32LE(44, newCompImageSize)
            result.writeU32LE(48, newDecompImageSize)
        }

        // result[16:20] = struct.pack('<I', comp_flags)
        result.writeU32LE(16, compFlags)
        // result += extend_to_4096(len(result))  (Python line 702)
        result = result + extendTo4096(result.size)

        return Pair(result, true)
    }

    /**
     * Result from [patchRsbLegacy].
     * @param conflicts non-empty if duplicate-basename conflicts were detected
     */
    private data class RsbLegacyResult(
        val data: ByteArray,
        val anyModified: Boolean,
        val conflicts: List<ConflictError>,
        val subgroupsModified: Int,
        val patchesApplied: Int
    )

    // =========================================================================
    // _patch_rsb_legacy — 1:1 port (lines 706-810)
    // =========================================================================

    /**
     * Full-featured RSB patching with compression + outer zlib support.
     *
     * 1:1 port of smf_packer.py:_patch_rsb_legacy.
     *
     * Key behaviors:
     * 1. Parse RSB header + subgroup info table
     * 2. Pre-scan: detect duplicate basenames across subgroups (flat conflict)
     * 3. Sort subgroups by offset for correct shift cascading
     * 4. For each subgroup: reconstruct RSGP header from authoritative info table
     * 5. Call _patch_rsgp_legacy; replace subgroup in-place with slice assignment
     * 6. Cascade offset shifts automatically through the subgroup offset field
     *
     * @param rawData Full decompressed RSB container
     * @param patchIndex Pre-indexed patch files
     * @param subgroupFilter Optional subgroup name prefix filter
     */
    private fun patchRsbLegacy(
        rawData: ByteArray,
        patchIndex: PatchIndex,
        subgroupFilter: String? = null
    ): RsbLegacyResult {
        var data = rawData

        // ---- Parse RSB header (file positioned at offset 4, after "1bsr") ----
        val version = data.readU32LE(4)
        // Skip: pad[8:12], header_size[12:16], file_list_size+offset[16:24],
        //       pad[24:32], sg_list_size+offset[32:40]
        val sgInfoEntries = data.readU32LE(40)
        val sgInfoOffset = data.readU32LE(44)
        val sgInfoEntrySize = data.readU32LE(48)
        // rest of header: 44 bytes [52:96], +4 if version==4 [96:100]

        Log.d(TAG, "  RSB: v$version, $sgInfoEntries subgroups, entry_size=$sgInfoEntrySize")

        // ---- Read subgroup info table ----
        // Info table entry layout (204 bytes per entry):
        //   [0:128] name  [128:132] rsg_offset  [132:136] rsg_size
        //   [136:140] subgroup_id
        //   [140:160] comp_flags..decomp_data_size (5 × u32)
        //   [160:164] decomp_data_size_b
        //   [164:176] image_data_offset..decomp_image_size (3 × u32)
        //   [176:204] remaining
        data class SgInfo(
            val name: String,
            val infoStart: Int,
            val rsgOffset: Int,
            val rsgSize: Int
        )

        val subgroupList = linkedMapOf<String, SgInfo>()
        var pos = sgInfoOffset
        for (i in 0 until sgInfoEntries) {
            val nameBytes = data.copyOfRange(pos, pos + 128)
            val nameEnd = nameBytes.indexOf(0.toByte())
            val name = String(nameBytes, 0, if (nameEnd >= 0) nameEnd else 128, Charsets.UTF_8)

            val rsgOffset = data.readU32LE(pos + 128)
            // Subgroup size on disk = image_data_offset + compressed_image_size
            val imgOff = data.readU32LE(pos + 164)
            val compImg = data.readU32LE(pos + 168)
            val rsgSize = imgOff + compImg

            subgroupList[name] = SgInfo(name, pos, rsgOffset, rsgSize)
            pos += sgInfoEntrySize
        }

        // ---- Pre-scan: detect duplicate basenames across subgroups ----
        val fnameToSgs = mutableMapOf<String, MutableList<String>>()
        for ((sgName, info) in subgroupList) {
            val sgOffset = info.rsgOffset
            val sgSize = info.rsgSize
            // Extract subgroup data to parse file names
            val subdata = data.copyOfRange(sgOffset, sgOffset + sgSize)
            for (fname in getRsgpFileNames(subdata)) {
                val basename = basenameOf(fname)
                if (basename.isEmpty()) continue
                fnameToSgs.getOrPut(basename) { mutableListOf() }
                if (sgName !in fnameToSgs[basename]!!) {
                    fnameToSgs[basename]!!.add(sgName)
                }
            }
        }

        val conflicts = mutableListOf<ConflictError>()
        for ((basename, sgNames) in fnameToSgs) {
            if (sgNames.size > 1 && patchIndex.contains(basename)) {
                conflicts.add(ConflictError(basename, sgNames.toList()))
            }
        }

        if (conflicts.isNotEmpty()) {
            Log.w(TAG, "Conflicts detected: ${conflicts.size}")
            for (c in conflicts) {
                Log.w(TAG, "  '${c.basename}' in: ${c.subgroups.joinToString(", ")}")
            }
            return RsbLegacyResult(rawData, false, conflicts, 0, 0)
        }

        // ---- Patch subgroups (sorted by offset for correct cascade) ----
        var rsgShift = 0
        var anyModified = false
        var subgroupsModified = 0

        val sortedSgs = subgroupList.values.sortedBy { it.rsgOffset }

        for (sg in sortedSgs) {
            val name = sg.name
            val infoStart = sg.infoStart
            val oldOffset = sg.rsgOffset
            val oldSize = sg.rsgSize

            // Apply cumulative shift to this subgroup's offset
            val rsgOffset = oldOffset + rsgShift

            // Subgroup filter
            if (subgroupFilter != null && !name.lowercase().startsWith(subgroupFilter.lowercase())) {
                // Still cascade offset
                data.writeU32LE(infoStart + 128, rsgOffset)
                continue
            }

            // Read subgroup data at SHIFTED offset (Python: raw_data[rsg_offset:rsg_offset+rsg_size])
            var subdata = data.copyOfRange(rsgOffset, minOf(rsgOffset + oldSize, data.size))

            // Skip empty subgroups (too small for valid RSGP header)
            if (subdata.size < 80) {
                data.writeU32LE(infoStart + 128, rsgOffset)
                continue
            }

            // Reconstruct RSGP header from info table (authoritative source)
            //   subdata[:4] = b'pgsr'
            //   subdata[16:36] = raw_data[info_start + 140:info_start + 160]
            //   subdata[40:52] = raw_data[info_start + 164:info_start + 176]
            RSGP_MAGIC.copyInto(subdata, 0)
            data.copyInto(subdata, 16, infoStart + 140, infoStart + 160)
            data.copyInto(subdata, 40, infoStart + 164, infoStart + 176)

            // Check for full .rsg override file
            var modified: Boolean
            val rsgOverrideName = name + ".rsg"
            if (patchIndex.contains(rsgOverrideName)) {
                subdata = patchIndex.getBytes(rsgOverrideName)!!
                modified = true
            } else {
                val (patched, wasModified) = patchRsgpLegacy(subdata, patchIndex)
                subdata = patched
                modified = wasModified
            }

            if (modified) {
                anyModified = true
                subgroupsModified++
                // subdata[:4] = b'pgsr'  (Python line 796)
                RSGP_MAGIC.copyInto(subdata, 0)
                // subdata += extend_to_4096(len(subdata))  (Python line 797)
                subdata = subdata + extendTo4096(subdata.size)
                val newSize = subdata.size

                // Grow raw_data if needed
                val needed = rsgOffset + newSize
                if (needed > data.size) {
                    data = data + ByteArray(needed - data.size)
                }

                // raw_data[rsg_offset:rsg_offset + rsg_size] = subdata
                // Python slice assignment cascades offsets automatically
                val oldEnd = rsgOffset + oldSize
                data = byteArraySliceAssign(data, rsgOffset, oldEnd, subdata)

                // Update size in info table
                data.writeU32LE(infoStart + 132, newSize)

                // Copy updated RSGP header fields back to info table
                // raw_data[info_start+140:info_start+176] =
                //     subdata[16:36] + subdata[32:36] + subdata[40:52]
                subdata.copyInto(data, infoStart + 140, 16, 36)   // [140:160]
                subdata.copyInto(data, infoStart + 160, 32, 36)    // [160:164]
                subdata.copyInto(data, infoStart + 164, 40, 52)    // [164:176]

                rsgShift += newSize - oldSize

                Log.d(TAG, "  [PATCH] Subgroup: $name ($oldSize → $newSize bytes)")
            }

            // Update offset in info table (always — cascade)
            data.writeU32LE(infoStart + 128, rsgOffset)
        }

        return RsbLegacyResult(data, anyModified, emptyList(), subgroupsModified, patchIndex.fileCount)
    }

    // =========================================================================
    // ByteArray slice assignment helpers (simulate Python bytearray)
    // =========================================================================

    /**
     * Simulate Python `data[from:to] = replacement`.
     * Replaces the range [from, to) with [replacement], shifting subsequent bytes.
     * Returns a new ByteArray (Kotlin ByteArray is immutable-sized).
     */
    private fun byteArraySliceAssign(
        data: ByteArray,
        from: Int,
        to: Int,
        replacement: ByteArray
    ): ByteArray {
        val newSize = data.size - (to - from) + replacement.size
        val result = ByteArray(newSize)
        data.copyInto(result, 0, 0, from)
        replacement.copyInto(result, from)
        data.copyInto(result, from + replacement.size, to, data.size)
        return result
    }

    /**
     * Simulate Python `data[from:] = replacement`.
     * Replaces everything from [from] to end with [replacement].
     */
    private fun byteArrayReplaceFrom(
        data: ByteArray,
        from: Int,
        replacement: ByteArray
    ): ByteArray {
        val newSize = from + replacement.size
        val result = ByteArray(newSize)
        data.copyInto(result, 0, 0, from)
        replacement.copyInto(result, from)
        return result
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    /** 4096-byte alignment padding. 1:1 extend_to_4096 (line 509-511). */
    private fun extendTo4096(size: Int): ByteArray {
        val padding = (4096 - (size and 4095)) and 4095
        return ByteArray(padding)
    }

    /** Extract basename from an internal path. Matches Python Path(name).name. */
    private fun basenameOf(path: String): String {
        return path.substringAfterLast('/').substringAfterLast('\\')
    }

    // ---- ByteArray little-endian read/write helpers ----

    private fun ByteArray.readU32LE(offset: Int): Int {
        return (this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8) or
            ((this[offset + 2].toInt() and 0xFF) shl 16) or
            ((this[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun ByteArray.writeU32LE(offset: Int, value: Int) {
        this[offset] = (value and 0xFF).toByte()
        this[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        this[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        this[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    private fun ByteArray.readU24LE(offset: Int): Int {
        return (this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8) or
            ((this[offset + 2].toInt() and 0xFF) shl 16)
    }

    // ---- Zlib helpers ----

    private fun zlibDecompress(data: ByteArray): ByteArray {
        return try {
            val inflater = Inflater()
            val result = ByteArrayOutputStream()
            inflater.setInput(data)
            val buf = ByteArray(4096)
            while (!inflater.finished()) {
                val n = inflater.inflate(buf)
                if (n > 0) result.write(buf, 0, n)
            }
            inflater.end()
            result.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "zlib decompress failed", e)
            data  // fallback: return as-is
        }
    }

    private fun zlibCompress(data: ByteArray, level: Int = 9): ByteArray {
        val deflater = Deflater(level)
        val result = ByteArrayOutputStream()
        try {
            deflater.setInput(data)
            deflater.finish()
            val buf = ByteArray(4096)
            while (!deflater.finished()) {
                val n = deflater.deflate(buf)
                if (n > 0) result.write(buf, 0, n)
            }
        } finally {
            deflater.end()
        }
        return result.toByteArray()
    }

    // ---- PopCap Zlib helpers ----

    /**
     * Decompress PopCap Zlib format (0xDEADFED4 header + zlib stream).
     * 1:1 match of the decompression in pack_smf line 438.
     */
    private fun popcapZlibDecompress(data: ByteArray): ByteArray {
        val decompSize = data.readU32LE(4)
        if (decompSize <= 0 || decompSize >= 512_000_000) return data

        val inflater = Inflater(true)
        return try {
            inflater.setInput(data, 8, data.size - 8)
            val result = ByteArrayOutputStream()
            val buf = ByteArray(65536)
            while (!inflater.finished()) {
                val n = inflater.inflate(buf)
                if (n > 0) result.write(buf, 0, n)
            }
            result.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "PopCap zlib decompress failed", e)
            data
        } finally {
            inflater.end()
        }
    }

    /**
     * Compress into PopCap Zlib format.
     * 1:1 match of re-compression in pack_smf lines 482-490.
     *
     * Format: magic(4) + decomp_size(4) + reserved(4) + zlib(9) + 4096-pad
     */
    private fun popcapZlibCompress(data: ByteArray): ByteArray {
        val aligned = data + extendTo4096(data.size)
        val compressed = zlibCompress(aligned, 9)
        val compAligned = compressed + extendTo4096(compressed.size)

        val result = ByteArray(12 + compAligned.size)
        result.writeU32LE(0, POPCAP_ZLIB_MAGIC)
        result.writeU32LE(4, aligned.size)
        // bytes 8-11 remain 0 (reserved)
        compAligned.copyInto(result, 12)
        return result
    }
}
