package com.example.z_editor.datapack.smf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.Inflater

/**
 * SMF unpacker — extracts every RSGP subgroup's files from an RSB container
 * (or a standalone RSGP) into a plain output directory using java.io.File.
 *
 * 1:1 port of scripts/pvz2_tool/smf_unpacker.py.  Reads go through SAF
 * (contentResolver), writes go to a real public folder via java.io.File.
 * The caller must hold MANAGE_EXTERNAL_STORAGE before writing.
 *
 * **大文件**：整条链路是"按需定位读"的，不把包读进内存。输入优先直接拿
 * SAF 的 fd 做定位读（[ChannelByteSource]），拿不到才流式拷进临时文件；
 * 每个子组的 data/image 段要么是源文件里的一段区间（零拷贝），要么解压后
 * 落临时文件，只有小段才在内存里。条目逐个流式拷到输出。
 * 于是常驻内存与包大小无关 —— 500MB 的包在 256MB 堆上也能解。
 *
 * 失败一律走 [Result.failure]，并且顶层连 `Throwable` 一起兜 —— 原先只
 * `catch (Exception)`，而 `OutOfMemoryError` 是 `Error`，会直接穿透出去
 * 把应用闪退掉（这正是 500M 数据包闪退的根因）。
 *
 * Deliberately shares SmfPacker's parseRsgpFileList / magic constants and
 * does NOT reuse any patching logic.  Data/image section decompression uses
 * a strict inflater (fails loudly instead of silently returning garbage).
 */
object SmfUnpacker {
    private const val TAG = "SmfUnpacker"
    private const val MAX_DECOMP_SIZE = 512_000_000

    /** 解压后的段超过这个值就落临时文件，不再整段放内存。 */
    private const val IN_MEMORY_SECTION_LIMIT = 32L * 1024 * 1024

    /** 单张 PTX 条目的上限（64M 像素的 ASTC 5x5 约 41MB，留余量）。 */
    private const val MAX_PTX_ENTRY = 64 * 1024 * 1024

    /** RSB 子组信息表里实际会读到的最远字段：+200 的 ptxBeforeNumber（读到 +204）。 */
    private const val SG_ENTRY_READ_MAX = 204L

    /** 文件列表区上限，防止损坏的 infoSize 让我们一次性分配几个 G。 */
    private const val MAX_INFO_BYTES = 64L * 1024 * 1024

    /** 临时工作目录名（外置私有目录 / 内部 cache 下）。 */
    private const val WORK_DIR_NAME = "smf_unpack_tmp"

    private const val POPCAP_MAGIC = 0xDEADFED4.toInt()
    private const val READ_BUFFER = 64 * 1024

    /** 外层 PopCap 压缩的三种候选布局：(流起始偏移, nowrap)。 */
    private val POPCAP_LAYOUTS = listOf(8 to false, 8 to true, 12 to false)

    data class UnpackOptions(
        /** Only extract entries whose internal path ends with .rton (case-insensitive). */
        val onlyRton: Boolean = false,
        /** Skip image entries entirely (still counted in progress). */
        val skipImages: Boolean = false,
        /** Only process subgroups whose name starts with this prefix. */
        val subgroupFilterPrefix: String? = null,
        /**
         * 把 .ptx 图像条目解码成 PNG 写出（同名 .png），而不是原样落地 .ptx。
         *
         * 解不出来的（格式未支持、PTX_INFO 对不上）**回退为写原始 .ptx**，绝不静默丢文件
         * —— 见 [UnpackResult.pngFallback]。独立的 RSGP 没有 PTX_INFO 表，全部回退。
         */
        val convertPtxToPng: Boolean = false,
        /**
         * 转 PNG 的同时**保留原 .ptx**（.png 与 .ptx 并存）。
         *
         * 仅在 [convertPtxToPng] 为真时有意义；关闭时 .ptx 本来就会原样落地，无需再留。
         * 解码失败的那部分不受影响——它们无论开关如何都只有 .ptx。
         */
        val keepPtx: Boolean = false
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
        /** Entries written under a sanitized path different from their internal path. */
        val sanitizedCount: Int,
        /** Number of subgroups actually processed (after filter + bounds checks). */
        val subgroupsProcessed: Int,
        /** .ptx 成功解码并按 .png 写出的数量（仅 convertPtxToPng 时非零）。 */
        val pngWritten: Int,
        /** .ptx 解不出来、按原样写回 .ptx 的数量（未支持格式 / 无 PTX_INFO / 解码失败）。 */
        val pngFallback: Int,
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
        var workDir: File? = null
        return try {
            if (!outputRootDir.mkdirs() && !outputRootDir.isDirectory) {
                return Result.failure(Exception("无法创建输出目录（可能缺少存储权限）"))
            }
            workDir = newWorkDir(context)
            val result = openInput(context, inputUri, workDir).use { raw ->
                unwrapOuterLayers(raw, workDir).use { payload ->
                    unpackSource(payload, outputRootDir, workDir, options, onProgress)
                }
            }
            Result.success(result)
        } catch (e: Throwable) {
            Log.e(TAG, "unpack failed", e)
            Result.failure(friendlyError(e))
        } finally {
            workDir?.let { runCatching { it.deleteRecursively() } }
        }
    }

    // ---- 输入 / 外层包装 ----

    /**
     * 打开输入。首选直接拿 SAF 的 fd 做定位读：500MB 的包不必先复制一份到
     * 临时目录（那既费时又费盘）。拿不到可定位的 fd（云盘 provider 给的是
     * 管道）才回退到流式复制。
     */
    private fun openInput(context: Context, uri: Uri, workDir: File): ByteSource {
        val pfd = try {
            context.contentResolver.openFileDescriptor(uri, "r")
        } catch (e: Exception) {
            Log.w(TAG, "openFileDescriptor 失败，回退到流式复制", e)
            null
        }
        if (pfd != null) {
            val fis = try {
                FileInputStream(pfd.fileDescriptor)
            } catch (e: Exception) {
                runCatching { pfd.close() }
                return copyToTemp(context, uri, workDir)
            }
            val size = runCatching { fis.channel.size() }.getOrDefault(0L)
            if (size > 0) {
                val src = ChannelByteSource(fis.channel, size) {
                    runCatching { fis.close() }
                    runCatching { pfd.close() }
                }
                // 管道型 fd 不支持定位读（pread 会 ESPIPE）——探一下再决定
                if (src.readFully(0, 4) != null) return src
                runCatching { src.close() }
            } else {
                runCatching { fis.close() }
                runCatching { pfd.close() }
            }
        }
        return copyToTemp(context, uri, workDir)
    }

    private fun copyToTemp(context: Context, uri: Uri, workDir: File): ByteSource {
        val tmp = File(workDir, "input.bin")
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("无法读取输入文件")
        input.use { ins ->
            FileOutputStream(tmp).use { out -> ins.copyTo(out, READ_BUFFER) }
        }
        Log.i(TAG, "输入已流式复制到临时文件: ${tmp.length()} bytes")
        return openFileSource(tmp) ?: throw IllegalStateException("无法打开临时文件")
    }

    /**
     * 剥掉外层包装（RSLB → PopCap zlib），返回可直接解析的载荷源。
     *
     * 每剥一层都是**流式**写进临时文件，不再整块进内存 —— 原先
     * `decompress` 里那个 `ByteArrayOutputStream(totalUncomp.toInt())`
     * 本身就是一次几百 MB 的分配。
     */
    private fun unwrapOuterLayers(src: ByteSource, workDir: File): ByteSource {
        val opened = mutableListOf(src)
        var cur = src
        // 现实中最多一层；留两轮纯粹是为了兼容 RSLB 里再套 PopCap 的理论情况
        var depth = 0
        while (depth++ < 2) {
            val next = unwrapOnce(cur, workDir) ?: break
            opened += next
            cur = next
        }
        return if (opened.size == 1) cur else CloseAllSource(cur, opened)
    }

    /** 没有可剥的外层时返回 null。 */
    private fun unwrapOnce(src: ByteSource, workDir: File): ByteSource? {
        if (RslbDecompressor.isRslb(src)) {
            Log.i(TAG, "Detected RSLB outer compression")
            val out = File(workDir, "rslb_inner.bin")
            FileOutputStream(out).use { RslbDecompressor.decompressTo(src, it) }
            Log.i(TAG, "RSLB decompressed: ${out.length()} bytes")
            return openFileSource(out) ?: throw IllegalStateException("RSLB 解压产物无法打开")
        }
        if (src.size >= 4 && src.u32LE(0) == POPCAP_MAGIC) {
            Log.i(TAG, "Detected PopCap outer compression")
            return unwrapPopcap(src, workDir)
        }
        return null
    }

    /**
     * 外层 PopCap 压缩：真实文件用 byte 8 起的 zlib 流，SmfPacker 自己的
     * 压缩/解压助手则假定 byte 8 / byte 12 起的裸 deflate。逐个候选试，
     * 用结果的容器 magic 判优，这样无论哪个约定产出的文件都能正解。
     */
    private fun unwrapPopcap(src: ByteSource, workDir: File): ByteSource {
        for ((offset, nowrap) in POPCAP_LAYOUTS) {
            if (offset >= src.size) continue
            if (!probePayloadMagic(src, offset.toLong(), nowrap)) continue
            val out = File(workDir, "outer_${offset}_$nowrap.bin")
            try {
                FileOutputStream(out).use { inflateTo(src, offset.toLong(), src.size - offset, it) }
                Log.i(TAG, "外层解压成功 (offset=$offset, nowrap=$nowrap): ${out.length()} bytes")
                return openFileSource(out) ?: throw IllegalStateException("外层解压产物无法打开")
            } catch (e: Exception) {
                Log.w(TAG, "外层候选 (offset=$offset, nowrap=$nowrap) 失败", e)
                runCatching { out.delete() }
            }
        }
        throw IllegalStateException("外层 PopCap 压缩解压失败")
    }

    /** 只解压开头几个字节，看它是不是 RSB/RSGP 的 magic。 */
    private fun probePayloadMagic(src: ByteSource, offset: Long, nowrap: Boolean): Boolean {
        return try {
            val head = src.readFully(offset, minOf(READ_BUFFER.toLong(), src.size - offset).toInt())
                ?: return false
            val inflater = Inflater(nowrap)
            try {
                inflater.setInput(head)
                val out = ByteArray(4)
                var got = 0
                while (got < 4) {
                    val n = inflater.inflate(out, got, 4 - got)
                    if (n == 0) break
                    got += n
                }
                got == 4 && (out.contentEquals(SmfPacker.RSB_MAGIC) ||
                        out.contentEquals(SmfPacker.RSGP_MAGIC))
            } finally {
                inflater.end()
            }
        } catch (e: Exception) {
            false
        }
    }

    // ---- 核心：从字节源解包（纯 JVM，可单测） ----

    /**
     * 从已就绪的（外层已剥掉的）字节源解包。无 `android.*` 依赖，
     * 单测可以直接用 [ArrayByteSource] 喂合成包。
     */
    internal fun unpackSource(
        src: ByteSource,
        outputRootDir: File,
        workDir: File,
        options: UnpackOptions,
        onProgress: (done: Int, total: Int, name: String?) -> Unit
    ): UnpackResult {
        if (src.size < 4) throw IllegalStateException("文件太小，无法识别格式")
        val magic = src.readFully(0, 4) ?: throw IllegalStateException("无法读取文件头")
        val state = State(options, onProgress)
        when {
            magic.contentEquals(SmfPacker.RSB_MAGIC) -> unpackRsb(src, outputRootDir, workDir, state)
            magic.contentEquals(SmfPacker.RSGP_MAGIC) -> unpackRsgp(src, outputRootDir, workDir, state)
            else -> {
                val hex = magic.joinToString("") { "%02X".format(it) }
                throw IllegalStateException("未知文件格式: magic=$hex")
            }
        }
        return state.toResult(outputRootDir)
    }

    // ---- RSB container ----

    private fun unpackRsb(src: ByteSource, outputRootDir: File, workDir: File, state: State) {
        if (src.size < 52) throw IllegalStateException("RSB 文件头不完整")
        // RSB header: SUBGROUP_INFO_ENTRIES/OFFSET/ENTRY_SIZE at 40/44/48.
        val sgInfoEntries = src.u32LE(40)
        val sgInfoOffset = src.u32At(44)
        val sgInfoEntrySize = src.u32LE(48)
        val stride = sgInfoEntrySize.coerceIn(1, 65536).toLong()

        // 纹理表（PTX→PNG 用）。解析失败/不存在时为空表，此时所有 .ptx 都回退成原样落地。
        state.ptxInfos = RsbTextureIndex.parseHeader(src)
            ?.let { RsbTextureIndex.parsePtxInfos(src, it) }
            ?: emptyList()

        val subgroups = mutableListOf<Subgroup>()

        // ---- Pre-pass: read subgroup info + file lists, count progress total ----
        var pos = sgInfoOffset
        var i = 0
        while (i < sgInfoEntries && pos + stride <= src.size) {
            // 这条目要读到 +200 的 ptx_BeforeNumber（即第 204 字节）才算完整
            if (pos + SG_ENTRY_READ_MAX > src.size) break
            val name = src.fixedCString(pos, 128)
            val rsgOffset = src.u32At(pos + 128)
            val infoStart = pos
            pos += stride
            i++

            val filter = state.options.subgroupFilterPrefix
            if (filter != null && !name.startsWith(filter, ignoreCase = true)) continue

            // Subgroup's own RSGP header must fit.
            if (rsgOffset + 80 > src.size) continue

            // Authoritative RSGP fields come from the info table (offsets 140..172).
            val compFlags = src.u32LE(infoStart + 140)
            val dataOffset = src.u32At(infoStart + 148)
            val compDataSize = src.u32At(infoStart + 152)
            val decompDataSize = src.u32At(infoStart + 156)
            val imageOffset = src.u32At(infoStart + 164)
            val compImageSize = src.u32At(infoStart + 168)
            val decompImageSize = src.u32At(infoStart + 172)

            // INFO_SIZE/OFFSET are read from the subgroup's own RSGP header (72/76).
            val infoSize = src.u32At(rsgOffset + 72)
            val infoOffset = src.u32At(rsgOffset + 76)

            subgroups += Subgroup(
                rsgOffset, compFlags,
                dataOffset, compDataSize, decompDataSize,
                imageOffset, compImageSize, decompImageSize,
                // 本子组第一张纹理在全局 PTX_INFO 表里的起始下标
                src.u32LE(infoStart + 200),
                readFileList(src, rsgOffset + infoOffset, infoSize)
            )
            state.subgroupsProcessed++
        }

        countTotal(state, subgroups)
        for (sg in subgroups) processSubgroup(src, sg, outputRootDir, workDir, state)
    }

    // ---- Standalone RSGP ----

    private fun unpackRsgp(src: ByteSource, outputRootDir: File, workDir: File, state: State) {
        if (src.size < 80) throw IllegalStateException("RSGP 文件头不完整")
        val compFlags = src.u32LE(16)
        val dataOffset = src.u32At(24)
        val compDataSize = src.u32At(28)
        val decompDataSize = src.u32At(32)
        val imageOffset = src.u32At(40)
        val compImageSize = src.u32At(44)
        val decompImageSize = src.u32At(48)
        val infoSize = src.u32At(72)
        val infoOffset = src.u32At(76)

        val sg = Subgroup(
            0L, compFlags,
            dataOffset, compDataSize, decompDataSize,
            imageOffset, compImageSize, decompImageSize,
            // 独立 RSGP 没有 PTX_INFO 表 → 无法解码纹理，.ptx 一律原样落地
            0,
            readFileList(src, infoOffset, infoSize)
        )
        state.ptxInfos = emptyList()
        state.subgroupsProcessed = 1

        countTotal(state, listOf(sg))
        processSubgroup(src, sg, outputRootDir, workDir, state)
    }

    /**
     * 读一段文件列表区。语义与改流式之前一致：区间超出文件尾就**截断**
     * （而不是整段作废），损坏的长度字段也不会让我们一次性分配过大内存。
     */
    private fun readFileList(
        src: ByteSource,
        offset: Long,
        size: Long
    ): List<SmfPacker.RsgpFileEntry> {
        if (offset < 0 || size <= 0 || offset >= src.size) return emptyList()
        val avail = minOf(size, src.size - offset, MAX_INFO_BYTES)
        if (avail <= 0) return emptyList()
        val buf = src.readFully(offset, avail.toInt()) ?: return emptyList()
        // 以 0 为基准解析切片，与原先"用绝对偏移 + 文件长度做上界"等价
        return SmfPacker.parseRsgpFileList(buf, 0, buf.size)
    }

    // ---- Shared processing ----

    private fun countTotal(state: State, subgroups: List<Subgroup>) {
        for (sg in subgroups) {
            for (e in sg.entries) {
                if (isWanted(e, state.options)) state.total++
            }
        }
    }

    /** 该条目这次是否需要落地（进度条口径，与提取循环保持一致）。 */
    private fun isWanted(e: SmfPacker.RsgpFileEntry, options: UnpackOptions): Boolean {
        if (e.name.isEmpty()) return false
        return !(options.onlyRton && !e.name.endsWith(".rton", ignoreCase = true))
    }

    private fun processSubgroup(
        src: ByteSource,
        sg: Subgroup,
        outputRootDir: File,
        workDir: File,
        state: State
    ) {
        val options = state.options

        // 先判断这一段到底用不用得上：没人要的段连解压都不做。
        // 500M 级数据包里 image 段动辄几百 MB，这一条是省内存的大头
        // （勾上「跳过图片」或「仅解包 RTON」时整段都不会被碰）。
        val needData = sg.entries.any { isWanted(it, options) && !it.isImage }
        val needImage = !options.skipImages && sg.entries.any { isWanted(it, options) && it.isImage }

        val data = if (needData) openDataSection(src, sg, workDir) else null
        val image = if (needImage) openImageSection(src, sg, workDir) else null

        try {
            for (e in sg.entries) {
                if (!isWanted(e, options)) continue

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
                // ---- .ptx → PNG（可选） ----
                // 解码失败一律回退成原样写 .ptx：宁可给用户原始文件，也不能静默丢。
                if (options.convertPtxToPng && e.isImage && safeRel.endsWith(".ptx", true)) {
                    val pngRel = safeRel.substringBeforeLast('.') + ".png"
                    if (writePtxPng(section, e, safeRel, pngRel, sg, state, outputRootDir)) {
                        // 解码成功、PNG 已落盘。keepPtx 时继续往下按原路径补写一份 .ptx，否则到此为止。
                        if (!options.keepPtx) continue
                    } else {
                        state.pngFallback++
                        // fall through：按原路径写回 .ptx
                    }
                }

                val target = File(outputRootDir, safeRel)
                target.parentFile?.mkdirs()
                try {
                    FileOutputStream(target).use {
                        section.copyTo(it, e.offset.toLong(), e.size.toLong())
                    }
                    state.fileCount++
                    state.bytesWritten += e.size.toLong()
                    if (safeRel != e.name) state.sanitizedCount++
                } catch (e2: Exception) {
                    Log.w(TAG, "write failed: $safeRel", e2)
                    state.skippedInvalid++
                }
            }
        } finally {
            // FileSection.close() 顺手删临时文件，必须保证走到
            runCatching { data?.close() }
            runCatching { image?.close() }
        }
    }

    // ---- 段的打开 ----

    /**
     * data 段。分支顺序与旧的 `slice/inflate` 判断逐条对齐，保证
     * `skippedOob` / `skippedInvalid` 的计数口径不变。
     */
    private fun openDataSection(src: ByteSource, sg: Subgroup, workDir: File): Section? {
        if (sg.compFlags and 2 == 0) return slice(src, sg.rsgOffset + sg.dataOffset, sg.compDataSize)
        if (sg.compDataSize == 0L) return null
        if (sg.decompDataSize !in 1..MAX_DECOMP_SIZE.toLong()) return null
        return inflateSection(
            src, sg.rsgOffset + sg.dataOffset, sg.compDataSize, sg.decompDataSize, workDir, "data"
        )
    }

    /** image 段。同上。 */
    private fun openImageSection(src: ByteSource, sg: Subgroup, workDir: File): Section? {
        if (sg.decompImageSize == 0L) return null
        if (sg.compFlags and 1 == 0) return slice(src, sg.rsgOffset + sg.imageOffset, sg.compImageSize)
        if (sg.compImageSize == 0L) return null
        if (sg.decompImageSize !in 1..MAX_DECOMP_SIZE.toLong()) return null
        return inflateSection(
            src, sg.rsgOffset + sg.imageOffset, sg.compImageSize, sg.decompImageSize, workDir, "image"
        )
    }

    /** 源文件里的一段区间；越界返回 null。 */
    private fun slice(src: ByteSource, start: Long, size: Long): Section? {
        if (start < 0 || size < 0) return null
        if (start + size > src.size) return null
        return SourceSlice(src, start, size)
    }

    /**
     * 解压一段出来。小段放内存，大段落临时文件 —— 大包的 image 段解压后
     * 能到几百 MB，正是原先 `copyOfRange` 把堆撑爆的地方。
     */
    private fun inflateSection(
        src: ByteSource,
        start: Long,
        compSize: Long,
        decompSize: Long,
        workDir: File,
        tag: String
    ): Section? {
        if (start < 0 || compSize <= 0 || start + compSize > src.size) return null
        return try {
            if (decompSize <= IN_MEMORY_SECTION_LIMIT) {
                val dest = ByteArray(decompSize.toInt())
                val sink = FixedBufferStream(dest)
                inflateTo(src, start, compSize, sink)
                MemorySection(if (sink.written == dest.size) dest else dest.copyOf(sink.written))
            } else {
                val file = File(workDir, "section_$tag.bin")
                try {
                    FileOutputStream(file).use { inflateTo(src, start, compSize, it) }
                    FileSection.open(file)
                } catch (e: Exception) {
                    runCatching { file.delete() }
                    throw e
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "$tag 段解压失败（已按跳过处理）", e)
            null
        }
    }

    // ---- PTX → PNG ----

    /**
     * 尝试把一条 .ptx 条目解码成 PNG 写出。
     * @return true = PNG 已写出（调用方应跳过原始 .ptx）；false = 解不出来，调用方回退写 .ptx。
     *
     * 三道闸：没有 part1Index / PTX_INFO 下标越界 / 格式未实现 —— 任一不过都回退，不猜。
     * 大纹理的 `IntArray` + `Bitmap` 可能吃满堆，这里连 `Throwable` 一起兜住：
     * 宁可回退成原样写 .ptx，也不能因为一张图把整次解包搞崩。
     */
    private fun writePtxPng(
        section: Section,
        e: SmfPacker.RsgpFileEntry,
        safeRel: String,
        pngRel: String,
        sg: Subgroup,
        state: State,
        outputRootDir: File
    ): Boolean {
        if (e.part1Index < 0) return false
        val info = state.ptxInfos.getOrNull(sg.ptxBeforeNumber + e.part1Index) ?: return false
        if (!PtxDecoder.isSupported(info.format)) return false
        if (e.size > MAX_PTX_ENTRY) return false

        val bytes = section.readInto(e.offset.toLong(), e.size) ?: return false
        val pixels = try {
            PtxDecoder.decode(bytes, info) ?: return false
        } catch (ex: Throwable) {
            Log.w(
                TAG,
                "PTX 解码失败 $safeRel (${PtxDecoder.formatName(info.format)} " +
                    "${info.width}x${info.height})",
                ex
            )
            return false
        }

        val target = File(outputRootDir, pngRel)
        return try {
            target.parentFile?.mkdirs()
            val bitmap = Bitmap.createBitmap(pixels, info.width, info.height, Bitmap.Config.ARGB_8888)
            val ok = FileOutputStream(target).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
            if (!ok) return false
            state.fileCount++
            state.bytesWritten += target.length()
            state.pngWritten++
            if (pngRel != e.name) state.sanitizedCount++
            true
        } catch (ex: Throwable) {
            Log.w(TAG, "PNG 写入失败: $pngRel", ex)
            false
        }
    }

    // ---- 解压（流式） ----

    /**
     * 从 [src] 的 `[offset, offset+compSize)` 解 zlib 流写进 [sink]。
     * 严格模式：损坏 / 截断 / 超限一律抛，不静默返回半截数据。
     */
    private fun inflateTo(src: ByteSource, offset: Long, compSize: Long, sink: OutputStream) {
        if (offset < 0 || compSize <= 0 || offset + compSize > src.size) {
            throw IllegalStateException("压缩数据区间越界")
        }
        val inflater = Inflater(false)
        try {
            val inBuf = ByteArray(READ_BUFFER)
            val outBuf = ByteArray(READ_BUFFER)
            var pos = offset
            val end = offset + compSize
            var total = 0L
            while (!inflater.finished()) {
                if (inflater.needsInput()) {
                    if (pos >= end) throw IllegalStateException("解压数据不完整")
                    val want = minOf(inBuf.size.toLong(), end - pos).toInt()
                    val n = src.readInto(pos, inBuf, 0, want)
                    if (n <= 0) throw IllegalStateException("解压数据不完整")
                    pos += n
                    inflater.setInput(inBuf, 0, n)
                }
                val n = inflater.inflate(outBuf)
                if (n > 0) {
                    sink.write(outBuf, 0, n)
                    total += n
                    if (total > MAX_DECOMP_SIZE) throw IllegalStateException("解压数据过大")
                } else if (!inflater.needsInput() && !inflater.finished()) {
                    throw IllegalStateException("解压停滞")
                }
            }
        } finally {
            inflater.end()
        }
    }

    /** 定长输出缓冲：写满即报错（声明尺寸就这么大，多出来的说明文件不自洽）。 */
    private class FixedBufferStream(private val dest: ByteArray) : OutputStream() {
        var written = 0
            private set

        override fun write(b: Int) {
            if (written + 1 > dest.size) throw IllegalStateException("解压数据超过声明尺寸")
            dest[written++] = b.toByte()
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            if (written + len > dest.size) throw IllegalStateException("解压数据超过声明尺寸")
            System.arraycopy(b, off, dest, written, len)
            written += len
        }
    }

    // ---- 辅助 ----

    /** 临时工作目录。放应用专属外置目录：容量比内部 cache 宽裕得多，且不需要权限。 */
    private fun newWorkDir(context: Context): File {
        val external = context.getExternalFilesDir(null)
        if (external != null) {
            val dir = File(external, WORK_DIR_NAME)
            runCatching { dir.deleteRecursively() }
            if (dir.mkdirs() || dir.isDirectory) return dir
        }
        val fallback = File(context.cacheDir, WORK_DIR_NAME)
        runCatching { fallback.deleteRecursively() }
        fallback.mkdirs()
        return fallback
    }

    /**
     * 把底层异常翻成人能看的话。
     *
     * `OutOfMemoryError` 单独处理：说明这次确实撞上内存墙了（比如
     * PTX→PNG 的巨型纹理），给出可操作的下一步，而不是一句 "null"。
     */
    private fun friendlyError(e: Throwable): Exception = when (e) {
        is OutOfMemoryError ->
            Exception("内存不足，解包中断。可先勾选「跳过图片」或「仅解包 RTON」降低内存占用。", e)

        is Exception -> e
        else -> Exception("${e.javaClass.simpleName}: ${e.message}", e)
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

    /**
     * 一个子组的解析结果。偏移/尺寸一律 Long（u32 无符号解释），
     * 免得 >2GB 的文件里偏移读成负数。
     */
    private data class Subgroup(
        val rsgOffset: Long,
        val compFlags: Int,
        val dataOffset: Long,
        val compDataSize: Long,
        val decompDataSize: Long,
        val imageOffset: Long,
        val compImageSize: Long,
        val decompImageSize: Long,
        /** 本子组第一张纹理在全局 PTX_INFO 表里的起始下标（0 = 独立 RSGP，无表）。 */
        val ptxBeforeNumber: Int,
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
        var pngWritten = 0
        var pngFallback = 0
        /** RSB 的 PTX_INFO 表；独立 RSGP 恒为空表。 */
        var ptxInfos: List<PtxDecoder.PtxInfo> = emptyList()

        fun toResult(outputDir: File) = UnpackResult(
            fileCount = fileCount,
            bytesWritten = bytesWritten,
            skippedImages = skippedImages,
            skippedZeroLength = skippedZeroLength,
            skippedOob = skippedOob,
            skippedUnsafePaths = skippedUnsafePaths,
            skippedInvalid = skippedInvalid,
            sanitizedCount = sanitizedCount,
            subgroupsProcessed = subgroupsProcessed,
            pngWritten = pngWritten,
            pngFallback = pngFallback,
            outputDir = outputDir
        )
    }
}

/** 关掉主源的同时把中途产生的中间源（临时文件）一起收掉。 */
private class CloseAllSource(
    private val primary: ByteSource,
    private val all: List<ByteSource>
) : ByteSource by primary {
    override fun close() {
        all.forEach { runCatching { it.close() } }
    }
}
