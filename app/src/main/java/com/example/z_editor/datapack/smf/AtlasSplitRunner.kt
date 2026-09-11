package com.example.z_editor.datapack.smf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.z_editor.datapack.rton.RtonConverter
import java.io.File
import java.io.FileOutputStream

/**
 * 图集拆分的执行侧：解码图集、裁剪、落盘、报进度。
 *
 * 与 `AtlasSplitter`（纯算法、可单测）分工：本类才碰 Android
 * （`Bitmap` / `BitmapFactory` / `Log`），因为单测配置没有
 * `testOptions { isReturnDefaultValues = true }`，一调 Android stub 就抛 `not mocked`。
 * 两个入口（SMF 解包页的开关、独立拆分页）共用本类，避免在 UI 里重复。
 *
 * **内存**：一张 4096×4096 图集结成 ARGB 是 64 MB，所以**一张图集处理完才做下一张**，
 * 不并行、不缓存整批。峰值就是单张图集。
 */
object AtlasSplitRunner {

    private const val TAG = "AtlasSplitRunner"

    data class Options(
        /** 跳过 1×1 全透明占位图（dark 包 28 张，已核实是真占位而非裁错）。 */
        val skipPlaceholders: Boolean = true,
    )

    data class Stats(
        val split: Int,
        val skippedPlaceholder: Int,
        val failed: Int,
        /** 实际解码成功的图集数 = [fromPtx5x5] + [fromPtx6x6] + [fromPng] */
        val atlasUsed: Int,
        /** 经 `.ptx` 解码、反推出 5×5 块尺寸的图集数 */
        val fromPtx5x5: Int,
        /** 经 `.ptx` 解码、反推出 6×6 块尺寸的图集数 */
        val fromPtx6x6: Int,
        /**
         * 经 `.png` 解码的图集数。PNG 头直接给宽高，推不出（也不需要）ASTC 块尺寸，
         * 所以单列一项，好让 [atlasUsed] 的三个分项能对上。
         */
        val fromPng: Int,
        val outputDir: File,
        /** 失败明细，最多留 [MAX_REPORTED_FAILURES] 条，避免结果区被刷屏。 */
        val failures: List<String>,
    )

    /** 解析 RTON 的三种结局，让 UI 能针对「已加密但没配密钥」给出可操作的提示。 */
    sealed interface PlanResult {
        data class Ok(val plan: AtlasSplitter.Plan) : PlanResult
        data object EncryptedNoKey : PlanResult
        data class Failed(val message: String) : PlanResult
    }

    /**
     * 读 RTON 文件并解析。**已加密的会自动过一层解密**（[RtonConverter.decryptRtonBytes]），
     * 用户不必先手动解密。
     *
     * @param key `datapack_prefs` 里的 `encryption_key`，与批量转换共用同一份
     */
    fun planFromFile(rtonFile: File, key: String?): PlanResult {
        val bytes = try {
            rtonFile.readBytes()
        } catch (ex: Exception) {
            Log.w(TAG, "读取 RTON 失败: ${rtonFile.absolutePath}", ex)
            return PlanResult.Failed("读取失败：${ex.message ?: ex.javaClass.simpleName}")
        }

        val plain = if (AtlasSplitter.isPlainRton(bytes)) {
            bytes
        } else {
            val k = key?.takeIf { it.isNotBlank() } ?: return PlanResult.EncryptedNoKey
            try {
                RtonConverter.decryptRtonBytes(bytes, k)
            } catch (ex: Exception) {
                Log.w(TAG, "RTON 解密失败: ${rtonFile.absolutePath}", ex)
                return PlanResult.Failed("解密失败：${ex.message ?: ex.javaClass.simpleName}")
            }
        }

        return try {
            PlanResult.Ok(AtlasSplitter.plan(plain))
        } catch (ex: Exception) {
            Log.w(TAG, "RTON 解析失败: ${rtonFile.absolutePath}", ex)
            PlanResult.Failed(ex.message ?: "解析失败")
        }
    }

    /**
     * 拆分前的体检：把 `plan` 和磁盘上的图集对上。
     *
     * 刻意放在真正开拆之前，这样「图集缺文件」「矩形越界」能在点按钮的瞬间就报出来，
     * 而不是跑到一半才失败。
     */
    data class Prepared(
        val plan: AtlasSplitter.Plan,
        val files: Map<String, File>,
        val missingAtlasNames: List<String>,
        val badRects: List<String>,
        val outputRoot: File,
    ) {
        val resolvedAtlasCount: Int get() = files.size

        /** 按选项实际会被切出来的张数（已扣掉越界与占位图）。 */
        fun splittableCount(options: Options): Int =
            plan.byAtlas.entries.sumOf { (atlasId, images) ->
                if (!files.containsKey(atlasId)) return@sumOf 0
                val atlas = plan.atlases.getValue(atlasId)
                images.count { s ->
                    AtlasSplitter.isRectInBounds(s, atlas) &&
                        !(options.skipPlaceholders && AtlasSplitter.isPlaceholder(s))
                }
            }

        /** 一条都拆不了的原因；空表表示可以开拆。 */
        fun blockingErrors(): List<String> {
            val errs = ArrayList<String>()
            if (plan.atlases.isEmpty()) errs += "RTON 里没有图集条目，可能不是资源清单文件"
            if (plan.imageCount == 0) errs += "RTON 里没有图片条目（带 ax/ay/aw/ah 的资源）"
            if (resolvedAtlasCount == 0 && plan.atlases.isNotEmpty()) {
                errs += "图集目录里没有任何一张 RTON 所列的图集（${missingAtlasNames.take(3).joinToString("、")}…）"
            }
            return errs
        }
    }

    fun prepare(plan: AtlasSplitter.Plan, atlasDir: File, outputRoot: File): Prepared {
        val onDisk = AtlasSplitter.scanAtlasDir(atlasDir)

        val files = LinkedHashMap<String, File>()
        val missing = ArrayList<String>()
        for ((id, atlas) in plan.atlases) {
            val f = onDisk[atlas.name.lowercase()]
            if (f == null) missing += atlas.name else files[id] = f
        }

        val badRects = ArrayList<String>()
        for ((atlasId, images) in plan.byAtlas) {
            val atlas = plan.atlases[atlasId] ?: continue
            for (s in images) {
                if (!AtlasSplitter.isRectInBounds(s, atlas)) {
                    badRects += "${s.id} (${s.ax},${s.ay},${s.aw},${s.ah}) 超出 " +
                        "${atlas.name} ${atlas.width}x${atlas.height}"
                }
            }
        }

        return Prepared(plan, files, missing, badRects, outputRoot)
    }

    /**
     * 执行拆分。单张图片失败不中断整批，计入 [Stats.failed] 并在 [Stats.failures] 里列出。
     *
     * @param onProgress `(已完成, 总数, 当前图片)`；总数按 [Prepared.splittableCount] 算
     */
    fun run(
        prepared: Prepared,
        options: Options = Options(),
        onProgress: (done: Int, total: Int, name: String) -> Unit = { _, _, _ -> },
    ): Stats {
        val plan = prepared.plan
        val total = prepared.splittableCount(options)
        var done = 0

        var split = 0
        var skipped = 0
        var failed = 0
        var atlasUsed = 0
        var from5x5 = 0
        var from6x6 = 0
        var fromPng = 0
        val failures = ArrayList<String>()

        for ((atlasId, images) in plan.byAtlas) {
            val atlas = plan.atlases[atlasId] ?: continue
            val file = prepared.files[atlasId] ?: continue

            val loaded = loadAtlas(file, atlas)
            if (loaded == null) {
                failed += images.size
                failures += "${atlas.name}: 图集加载失败（${file.name}）"
                done += images.size
                onProgress(done, total, atlas.name)
                continue
            }

            atlasUsed++
            when (loaded.kind) {
                LoadedAtlas.Kind.PTX_5x5 -> from5x5++
                LoadedAtlas.Kind.PTX_6x6 -> from6x6++
                LoadedAtlas.Kind.PNG -> fromPng++
            }

            try {
                for (s in images) {
                    if (!AtlasSplitter.isRectInBounds(s, atlas)) {
                        failed++
                        failures += "${s.id}: 矩形越界"
                        done++
                        continue
                    }
                    if (options.skipPlaceholders && AtlasSplitter.isPlaceholder(s)) {
                        skipped++
                        done++
                        onProgress(done, total, s.id)
                        continue
                    }

                    val rel = safeRelPath(s.path)
                    if (rel == null) {
                        failed++
                        failures += "${s.id}: path 不安全或为空，已跳过"
                        done++
                        continue
                    }

                    try {
                        writePng(File(prepared.outputRoot, "$rel.png"), loaded.cropTo(s), s.aw, s.ah)
                        split++
                    } catch (ex: Exception) {
                        failed++
                        failures += "${s.id}: ${ex.message ?: ex.javaClass.simpleName}"
                        Log.w(TAG, "图片写出失败: $rel (${s.aw}x${s.ah})", ex)
                    }
                    done++
                    onProgress(done, total, s.id)
                }
            } finally {
                loaded.close()
            }
        }

        return Stats(
            split = split,
            skippedPlaceholder = skipped,
            failed = failed,
            atlasUsed = atlasUsed,
            fromPtx5x5 = from5x5,
            fromPtx6x6 = from6x6,
            fromPng = fromPng,
            outputDir = prepared.outputRoot,
            failures = failures.take(MAX_REPORTED_FAILURES),
        )
    }

    // ---- 图集加载 ----

    /**
     * 已解码的一整张图集。
     *
     * `.ptx` 与 `.png` 只能留其一：前者是 `IntArray`、后者是 `Bitmap`，
     * 都是 64 MB 级别的单块内存，不并存。
     */
    private class LoadedAtlas(
        val kind: Kind,
        private val pixels: IntArray?,
        private val bitmap: Bitmap?,
        private val width: Int,
    ) {
        enum class Kind { PTX_5x5, PTX_6x6, PNG }

        /** 裁出 [s] 的矩形。`.ptx` 走 `AtlasSplitter.crop`（已被单测覆盖），`.png` 用 `getPixels` 直接读子矩形。 */
        fun cropTo(s: AtlasSplitter.Image): IntArray {
            val px = pixels
            if (px != null) return AtlasSplitter.crop(px, width, s)
            val out = IntArray(s.aw * s.ah)
            bitmap!!.getPixels(out, 0, s.aw, s.ax, s.ay, s.aw, s.ah)
            return out
        }

        fun close() {
            bitmap?.recycle()
        }
    }

    private fun loadAtlas(file: File, atlas: AtlasSplitter.Atlas): LoadedAtlas? {
        return try {
            if (file.extension.equals("png", ignoreCase = true)) {
                loadPng(file, atlas)
            } else {
                loadPtx(file, atlas)
            }
        } catch (ex: Exception) {
            Log.w(TAG, "图集解码异常: ${file.name} (${atlas.width}x${atlas.height})", ex)
            null
        }
    }

    private fun loadPng(file: File, atlas: AtlasSplitter.Atlas): LoadedAtlas? {
        val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        // PNG 头自带宽高，与 RTON 自报的尺寸核对一下：对不上说明配对错了图集，
        // 与其按错尺寸裁出一堆烂图，不如直接判这张图集加载失败。
        if (bmp.width != atlas.width || bmp.height != atlas.height) {
            Log.w(
                TAG,
                "${file.name} 尺寸 ${bmp.width}x${bmp.height} 与 RTON 记录的 " +
                    "${atlas.width}x${atlas.height} 不一致，跳过该图集",
            )
            bmp.recycle()
            return null
        }
        return LoadedAtlas(LoadedAtlas.Kind.PNG, pixels = null, bitmap = bmp, width = bmp.width)
    }

    private fun loadPtx(file: File, atlas: AtlasSplitter.Atlas): LoadedAtlas? {
        // ATLASES/ 下的 .PTX 没有 PTX 头，尺寸只能靠 (RTON 宽, RTON 高, 文件大小) 反推块尺寸。
        val block = AtlasSplitter.inferAstcBlock(atlas.width, atlas.height, file.length())
        if (block == null) {
            Log.w(
                TAG,
                "${file.name} 大小 ${file.length()} 与 ${atlas.width}x${atlas.height} " +
                    "对不上 ASTC 5x5/6x6，跳过该图集",
            )
            return null
        }
        val (bw, bh) = block
        val format = if (bw == 5) PtxDecoder.FMT_ASTC_5x5 else PtxDecoder.FMT_ASTC_6x6
        val info = PtxDecoder.PtxInfo(
            width = atlas.width,
            height = atlas.height,
            // PtxDecoder.decode 只吃 width/height/format，不校验 check；
            // 解包后 PTX_INFO 表已丢失，就地按定义合成即可。
            check = atlas.width * 16 / bw,
            format = format,
            alphaSize = 0,
            alphaFormat = 0,
        )
        val pixels = PtxDecoder.decode(file.readBytes(), info) ?: return null
        return LoadedAtlas(
            kind = if (bw == 5) LoadedAtlas.Kind.PTX_5x5 else LoadedAtlas.Kind.PTX_6x6,
            pixels = pixels,
            bitmap = null,
            width = atlas.width,
        )
    }

    // ---- 落盘 ----

    private fun writePng(target: File, pixels: IntArray, w: Int, h: Int) {
        target.parentFile?.mkdirs()
        val bitmap = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
        try {
            FileOutputStream(target).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw IllegalStateException("PNG 编码失败")
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * 把 RTON 的 `path` 镜像成相对路径。任一段不安全就整体作废，绝不写出输出根之外。
     *
     * `SmfUnpacker.sanitizePath` 是同类风险的更重版本（还管保留名、最大组件长度），
     * 但它对目录树是清理式的；这里面对的是 RTON 里的资源路径，宁可判废也不改名，
     * 免得产物文件名和游戏内路径对不上。
     */
    private fun safeRelPath(path: List<String>): String? {
        if (path.isEmpty()) return null
        val segs = ArrayList<String>(path.size)
        for (raw in path) {
            if (raw.isEmpty() || raw == "." || raw == "..") return null
            var seg = raw.trim().trimEnd(' ', '.')
            if (seg.isEmpty()) return null
            seg = seg.replace(UNSAFE_CHARS, "_")
            if (seg.startsWith(".")) seg = "_$seg"
            if (seg.length > MAX_SEGMENT) seg = seg.take(MAX_SEGMENT)
            segs += seg
        }
        return segs.joinToString("/")
    }

    private val UNSAFE_CHARS = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]")
    private const val MAX_SEGMENT = 100
    private const val MAX_REPORTED_FAILURES = 20
}
