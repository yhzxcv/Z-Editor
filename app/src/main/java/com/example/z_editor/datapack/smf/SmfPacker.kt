package com.example.z_editor.datapack.smf

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.z_editor.datapack.smf.SmfPacker.patchRsbLegacy
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
 * Patch matching (structure-first, resolved per patch file):
 *   1. Structured: the patch's relative path under [patchDir] is treated as the
 *      package's internal path (patches root = package root). If a file with that
 *      exact internal path exists, the patch is injected there — and only there,
 *      never into another file that happens to share its basename.
 *   2. Flat fallback (only if no structured path matched): the patch is applied
 *      iff exactly ONE file in the package has that basename.
 *   3. A fallback that would match two or more same-named files is ambiguous — the
 *      patch is skipped and reported. A patch that matches nothing is reported.
 * This lets duplicate-named files be targeted unambiguously by directory.
 * Only modified files need to be placed.
 */
object SmfPacker {

    private const val TAG = "SmfPacker"
    private const val POPCAP_ZLIB_MAGIC = 0xDEADFED4.toInt()

    internal val RSB_MAGIC =
        byteArrayOf('1'.code.toByte(), 'b'.code.toByte(), 's'.code.toByte(), 'r'.code.toByte())
    internal val RSGP_MAGIC =
        byteArrayOf('p'.code.toByte(), 'g'.code.toByte(), 's'.code.toByte(), 'r'.code.toByte())

    // ---- Public data classes ----

    data class PackResult(
        val outputName: String,
        val patchesApplied: Int,
        val subgroupsModified: Int,
        val outputSize: Long,
        /** Patches skipped because they would match more than one package file. */
        val ambiguousPatches: List<AmbiguousPatch> = emptyList(),
        /** Patches that matched no package file at all (reported, not injected). */
        val unmatchedPatches: List<String> = emptyList(),
        /** Count of patches applied via flat basename fallback (vs structured path). */
        val flatFallbackCount: Int = 0
    )

    /** A patch file that was skipped because it would match more than one file. */
    data class AmbiguousPatch(
        val patch: String,          // patch file's relative path under the patches dir
        val matches: List<String>   // package entry paths it matched ambiguously
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
    internal data class RsgpFileEntry(
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
    // PatchFiles — pre-indexed patch files for O(1) path lookup
    // =========================================================================

    /**
     * Pre-indexed patch files for O(1) lookup.
     *
     * Replaces repeated [DocumentFile.findFile] calls (SAF IPC + directory scan)
     * with a single upfront recursive scan and in-memory [HashMap] lookups.
     *
     * Each patch file is indexed by its **relative path** under the patches
     * directory, using '/' separators (e.g. "RTID/levels/1234.json"). A file
     * placed flat at the patches root gets a bare basename key ("1234.json").
     *
     * Lookup is **case-insensitive** because PvZ2 RSGP internal paths may differ
     * in case from the actual filesystem names (e.g. "PLANT.rton" vs "plant.rton"),
     * and SAF [DocumentFile.findFile] delegates to the filesystem which is often
     * case-insensitive (FAT32/exFAT).
     */
    private class PatchFiles(
        private val bytesMap: Map<String, ByteArray>,
        private val lowerMap: Map<String, String>,
        val fileCount: Int
    ) {
        /** All canonical patch keys (relative paths). */
        val keys: Set<String> get() = bytesMap.keys

        /**
         * Case-insensitive lookup; returns the canonical stored key if present,
         * or null if the patch does not exist.
         */
        fun canonicalKey(key: String): String? {
            if (bytesMap.containsKey(key)) return key
            return lowerMap[key.lowercase()]
        }

        /** Bytes for a key (case-insensitive), or null. */
        fun get(key: String): ByteArray? {
            bytesMap[key]?.let { return it }
            val lowerKey = lowerMap[key.lowercase()] ?: return null
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
            val patchFiles = buildPatchFiles(context, patchDirUri)
                ?: return Result.failure(Exception("无法访问补丁目录"))

            Log.d(TAG, "Pack: ${patchFiles.fileCount} patches indexed")

            // Read template
            var rawBytes =
                context.contentResolver.openInputStream(templateUri)?.use { it.readBytes() }
                    ?: return Result.failure(Exception("无法读取模板文件"))

            // ---- Step 0: RSLB outer compression (new format) ----
            // Decompress only — the output is never re-wrapped in RSLB.
            if (RslbDecompressor.isRslb(rawBytes)) {
                Log.d(TAG, "Detected RSLB outer compression")
                rawBytes = try {
                    RslbDecompressor.decompress(rawBytes)
                } catch (e: Exception) {
                    return Result.failure(Exception("RSLB 外层压缩解压失败: ${e.message}", e))
                }
                Log.d(TAG, "RSLB decompressed: ${rawBytes.size} bytes")
            }

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
            var ambiguous = emptyList<AmbiguousPatch>()
            var unmatched = emptyList<String>()
            var flatFallbackCount = 0
            when {
                magic.contentEquals(RSB_MAGIC) -> {
                    Log.d(TAG, "Detected RSB container")
                    val rsbResult = patchRsbLegacy(rawData, patchFiles)
                    rawData = rsbResult.data
                    anyModified = rsbResult.anyModified
                    subgroupsModified = rsbResult.subgroupsModified
                    patchesApplied = rsbResult.patchesApplied
                    ambiguous = rsbResult.ambiguous
                    unmatched = rsbResult.unmatched
                    flatFallbackCount = rsbResult.flatFallbackCount
                }

                magic.contentEquals(RSGP_MAGIC) -> {
                    Log.d(TAG, "Detected standalone RSGP container")
                    val entries = getRsgpFileNames(rawData).map { "" to it }
                    val resolution = resolvePatches(patchFiles, entries, emptyList())
                    val rsgpResult = patchRsgpLegacy(rawData, resolution.patchByEntry)
                    rawData = rsgpResult.data
                    anyModified = rsgpResult.modified
                    patchesApplied = rsgpResult.applied
                    if (rsgpResult.modified) subgroupsModified = 1
                    ambiguous = resolution.ambiguous
                    unmatched = resolution.unmatched
                    flatFallbackCount = resolution.flatFallbackCount
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
                return Result.success(
                    PackResult(
                        outputName, 0, 0, rawBytes.size.toLong(),
                        ambiguous, unmatched, flatFallbackCount
                    )
                )
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

            Log.d(
                TAG, "Written: $outputName (${finalData.size} bytes), " +
                        "$patchesApplied patches in $subgroupsModified subgroups"
            )
            Result.success(
                PackResult(
                    outputName,
                    patchesApplied,
                    subgroupsModified,
                    finalData.size.toLong(),
                    ambiguous,
                    unmatched,
                    flatFallbackCount
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "packSmf failed", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // Patch index builder
    // =========================================================================

    /**
     * Recursively scan [patchDirUri] once and cache all patch file bytes in
     * memory, keyed by their relative path under the patches directory.
     *
     * A nested file (e.g. `RTID/levels/1234.json`) is keyed by its full
     * relative path; a file at the patches root is keyed by its basename only.
     * This is what lets the packer match by package structure first.
     */
    private fun buildPatchFiles(context: Context, patchDirUri: Uri): PatchFiles? {
        val patchDir = DocumentFile.fromTreeUri(context, patchDirUri) ?: return null
        val map = mutableMapOf<String, ByteArray>()
        val lowerMap = mutableMapOf<String, String>()
        var filesSeen = 0

        fun scan(dir: DocumentFile, prefix: String) {
            for (file in dir.listFiles()) {
                if (file.isDirectory) {
                    val dirName = file.name ?: continue
                    scan(file, if (prefix.isEmpty()) dirName else "$prefix/$dirName")
                    continue
                }
                val rawName = file.name ?: continue
                if (rawName.isEmpty()) continue
                // Relative path always uses '/' regardless of platform separator
                val relPath = if (prefix.isEmpty()) rawName else "$prefix/$rawName"
                val key = relPath.replace('\\', '/')
                if (key.isEmpty()) continue
                filesSeen++
                try {
                    val bytes =
                        context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        map[key] = bytes
                        lowerMap[key.lowercase()] = key
                    }
                } catch (_: Exception) {
                    // Skip unreadable files
                }
            }
        }
        scan(patchDir, "")

        Log.d(TAG, "PatchFiles: ${map.size} files cached")
        if (map.isEmpty() && filesSeen > 0) {
            Log.w(TAG, "All $filesSeen files were filtered out — check permissions")
        }
        return PatchFiles(map, lowerMap, map.size)
    }

    // =========================================================================
    // Patch resolution — structure-first, flat fallback, ambiguity reporting
    // =========================================================================

    /**
     * Outcome of resolving every patch file against the package's entries.
     *
     * @param patchByEntry entry internal path → patch bytes to inject into it
     * @param subgroupOverrideBy subgroup name → full replacement bytes (".rsg" override)
     * @param ambiguous patches skipped because they matched multiple files
     * @param unmatched patches that matched no file at all
     * @param flatFallbackCount how many patches were injected via basename fallback
     */
    private data class PatchResolution(
        val patchByEntry: Map<String, ByteArray>,
        val subgroupOverrideBy: Map<String, ByteArray>,
        val ambiguous: List<AmbiguousPatch>,
        val unmatched: List<String>,
        val flatFallbackCount: Int
    )

    /**
     * Resolve every patch file to at most one target package file.
     *
     * Rules, per patch file (keyed by its relative path under the patches dir):
     *  0. Whole-subgroup override: a patch named "<subgroup>.rsg" replaces the
     *     entire subgroup. Resolved first so it is never re-treated as a file.
     *  1. Structured (default): the patch's relative path is treated as the
     *     package's internal path (patches root = package root). If a file with
     *     that exact internal path exists it is the target — and only it. If the
     *     same path exists in more than one subgroup the patch is ambiguous.
     *  2. Flat fallback (only if step 1 did not match): applied iff exactly ONE
     *     file in the package has that basename. Two or more same-named files →
     *     ambiguous (reported, skipped). Zero → unmatched (reported).
     *
     * Matching is case-insensitive on both sides (PvZ2 internal paths vs
     * filesystem names may differ in case).
     *
     * @param patchFiles indexed patch files
     * @param entries (subgroupName, entryInternalPath) pairs across all subgroups
     * @param subgroupNames subgroup names for whole-subgroup ".rsg" overrides
     */
    private fun resolvePatches(
        patchFiles: PatchFiles,
        entries: List<Pair<String, String>>,
        subgroupNames: List<String>
    ): PatchResolution {
        // Case-insensitive indexes over every entry in the package
        val sgByPathLower = mutableMapOf<String, MutableSet<String>>()      // lower(entry path) -> subgroups
        val actualPathByLower = mutableMapOf<String, String>()              // lower(entry path) -> entry path
        val sgByBasenameLower = mutableMapOf<String, MutableSet<Pair<String, String>>>() // lower(basename) -> (sg, entry path)
        for ((sg, name) in entries) {
            if (name.isEmpty()) continue
            sgByPathLower.getOrPut(name.lowercase()) { mutableSetOf() }.add(sg)
            actualPathByLower.putIfAbsent(name.lowercase(), name)
            sgByBasenameLower.getOrPut(basenameOf(name).lowercase()) { mutableSetOf() }.add(sg to name)
        }

        val patchByEntry = mutableMapOf<String, ByteArray>()
        val subgroupOverrideBy = mutableMapOf<String, ByteArray>()
        val claimedBy = mutableMapOf<String, String>()   // entry path -> patch key that claimed it
        val ambiguous = mutableListOf<AmbiguousPatch>()
        val unmatched = mutableListOf<String>()
        var flatFallbackCount = 0
        val used = mutableSetOf<String>()                // canonical patch keys already resolved

        // 0. Whole-subgroup overrides: a patch named "<subgroup>.rsg" replaces the subgroup.
        for (sg in subgroupNames) {
            val canon = patchFiles.canonicalKey(sg + ".rsg") ?: continue
            subgroupOverrideBy[sg] = patchFiles.get(canon)!!
            used += canon
        }

        // 1. Structured: exact internal path (case-insensitive).
        for (key in patchFiles.keys) {
            if (key in used) continue
            val bytes = patchFiles.get(key) ?: continue
            val subgroups = sgByPathLower[key.lowercase()]
            if (subgroups != null) {
                val actual = actualPathByLower[key.lowercase()]!!
                if (subgroups.size > 1) {
                    // Same internal path exists in several subgroups → cannot choose.
                    ambiguous.add(AmbiguousPatch(key, listOf(actual)))
                } else {
                    patchByEntry[actual] = bytes
                    claimedBy[actual] = key
                }
                used += key
            }
        }

        // 2. Flat fallback: basename, only when exactly one file has that name.
        for (key in patchFiles.keys) {
            if (key in used) continue
            val bytes = patchFiles.get(key) ?: continue
            val candidates = sgByBasenameLower[basenameOf(key).lowercase()]
            when {
                candidates == null || candidates.isEmpty() -> unmatched.add(key)
                candidates.size > 1 -> ambiguous.add(
                    AmbiguousPatch(key, candidates.map { it.second }.distinct().sorted())
                )
                else -> {
                    val target = candidates.first().second
                    if (target in claimedBy) {
                        // Another patch already targets this file (e.g. a structured one).
                        ambiguous.add(AmbiguousPatch(key, listOf(target)))
                    } else {
                        patchByEntry[target] = bytes
                        claimedBy[target] = key
                        flatFallbackCount++
                    }
                }
            }
            used += key
        }

        return PatchResolution(patchByEntry, subgroupOverrideBy, ambiguous, unmatched, flatFallbackCount)
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
    internal fun parseRsgpFileList(
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
                entries.add(
                    RsgpFileEntry(
                        name = decodedName,
                        isImage = isImage,
                        offset = foffset,
                        size = fsize,
                        sizePos = sizePos,
                        offsetPos = sizePos - 4
                    )
                )
            }
        }

        return entries
    }

    /**
     * Extract all file names from an RSGP subgroup.
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

    /** Result of [patchRsgpLegacy]: patched data, modified flag, and how many
     *  entries were actually replaced. */
    private data class RsgpPatchResult(
        val data: ByteArray,
        val modified: Boolean,
        val applied: Int
    )

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
     * @param patchMap resolved entry path → patch bytes (from [resolvePatches]);
     *   only exact entry paths appear as keys, so lookups are direct.
     * @param overrideDataComp Override data compression flag (-1 = use original)
     * @param overrideImageComp Override image compression flag (-1 = use original)
     */
    private fun patchRsgpLegacy(
        subdata: ByteArray,
        patchMap: Map<String, ByteArray>,
        overrideDataComp: Int = -1,
        overrideImageComp: Int = -1
    ): RsgpPatchResult {
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

        // ---- Early return: no patches apply to this container ----
        var hasAnyPatch = false
        for ((name, _) in dataDict) {
            if (name.isNotEmpty() && name in patchMap) {
                hasAnyPatch = true; break
            }
        }
        if (!hasAnyPatch) {
            for ((name, _) in imageDict) {
                if (name.isNotEmpty() && name in patchMap) {
                    hasAnyPatch = true; break
                }
            }
        }
        if (!hasAnyPatch) {
            return RsgpPatchResult(subdata, false, 0)
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
                imageData = zlibDecompress(
                    subdata.copyOfRange(
                        imageDataOffset,
                        imageDataOffset + compImageSize
                    )
                )
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
                val patchBytes = patchMap[decodedName]

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
                val patchBytes = patchMap[decodedName]

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
            return RsgpPatchResult(subdata, false, 0)
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

        return RsgpPatchResult(result, true, patchCount)
    }

    /**
     * Result from [patchRsbLegacy].
     * @param ambiguous patches skipped because they matched multiple files
     * @param unmatched patches that matched no file at all
     * @param flatFallbackCount patches applied via basename fallback
     */
    private data class RsbLegacyResult(
        val data: ByteArray,
        val anyModified: Boolean,
        val subgroupsModified: Int,
        val patchesApplied: Int,
        val ambiguous: List<AmbiguousPatch>,
        val unmatched: List<String>,
        val flatFallbackCount: Int
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
     * 2. Resolve every patch file to at most one entry via [resolvePatches]
     *    (structure-first, flat fallback, ambiguity/未匹配 reported — never aborts)
     * 3. Sort subgroups by offset for correct shift cascading
     * 4. For each subgroup: reconstruct RSGP header from authoritative info table
     * 5. Call _patch_rsgp_legacy; replace subgroup in-place with slice assignment
     * 6. Cascade offset shifts automatically through the subgroup offset field
     *
     * @param rawData Full decompressed RSB container
     * @param patchFiles Pre-indexed patch files
     * @param subgroupFilter Optional subgroup name prefix filter
     */
    private fun patchRsbLegacy(
        rawData: ByteArray,
        patchFiles: PatchFiles,
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

        // ---- Resolve every patch file against all subgroup entries ----
        val entries = mutableListOf<Pair<String, String>>()
        for ((sgName, info) in subgroupList) {
            val subdata = data.copyOfRange(info.rsgOffset, info.rsgOffset + info.rsgSize)
            for (fname in getRsgpFileNames(subdata)) {
                if (fname.isNotEmpty()) entries.add(sgName to fname)
            }
        }
        val resolution = resolvePatches(patchFiles, entries, subgroupList.keys.toList())
        for (a in resolution.ambiguous) {
            Log.w(TAG, "  AMBIGUOUS '${a.patch}' matches: ${a.matches.joinToString(", ")}")
        }
        for (u in resolution.unmatched) {
            Log.w(TAG, "  UNMATCHED '$u'")
        }

        // ---- Patch subgroups (sorted by offset for correct cascade) ----
        var rsgShift = 0
        var anyModified = false
        var subgroupsModified = 0
        var patchesApplied = 0

        val sortedSgs = subgroupList.values.sortedBy { it.rsgOffset }

        for (sg in sortedSgs) {
            val name = sg.name
            val infoStart = sg.infoStart
            val oldOffset = sg.rsgOffset
            val oldSize = sg.rsgSize

            // Apply cumulative shift to this subgroup's offset
            val rsgOffset = oldOffset + rsgShift

            // Subgroup filter
            if (subgroupFilter != null && !name.lowercase()
                    .startsWith(subgroupFilter.lowercase())
            ) {
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

            // Check for full .rsg override file (resolved in step 0 of resolvePatches)
            var modified: Boolean
            var applied = 0
            val override = resolution.subgroupOverrideBy[name]
            if (override != null) {
                subdata = override
                modified = true
                applied = 1
            } else {
                val rsgpResult = patchRsgpLegacy(subdata, resolution.patchByEntry)
                subdata = rsgpResult.data
                modified = rsgpResult.modified
                applied = rsgpResult.applied
            }

            if (modified) {
                anyModified = true
                subgroupsModified++
                patchesApplied += applied
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

        return RsbLegacyResult(
            data,
            anyModified,
            subgroupsModified,
            patchesApplied,
            resolution.ambiguous,
            resolution.unmatched,
            resolution.flatFallbackCount
        )
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

    internal fun ByteArray.readU32LE(offset: Int): Int {
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

    internal fun zlibDecompress(data: ByteArray): ByteArray {
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
    internal fun popcapZlibDecompress(data: ByteArray): ByteArray {
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
