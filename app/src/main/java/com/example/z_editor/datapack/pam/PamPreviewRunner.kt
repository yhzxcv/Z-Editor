package com.example.z_editor.datapack.pam

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.z_editor.datapack.smf.AtlasSplitRunner
import com.example.z_editor.datapack.smf.AtlasSplitter
import com.example.z_editor.datapack.smf.PtxDecoder
import java.io.File
import kotlin.math.roundToInt

/**
 * PAM 预览的 Android 侧编排：把三份输入（PAM / 图集目录 / RTON 清单）变成「可画的帧」。
 *
 * 与 `PamTimeline`（纯算法）、`PamAssets`（纯算法）分工：本类才碰 Android
 * （`Bitmap` / `BitmapFactory` / `Log`），因为单测配置没有
 * `testOptions { isReturnDefaultValues = true }`，一调 Android stub 就抛 `not mocked`。
 * 形态照 `AtlasSplitRunner`。
 *
 * **内存**（4096×4096 图集下从前会直接 OOM）。三份开销，分别由下面两道各压一份：
 *
 *  1. **载入期"所有部件的像素同时活着"** —— [PamAssets.resolve] 走流式回调，裁一张就转一张
 *     位图，那个 `IntArray` 随即失去引用。于是这份从 Σ裁片 变成 1 张裁片。
 *  2. **播放期常驻位图** —— 位图要留到预览关掉为止，这是**播放时**真正吃内存的那份。按
 *     `PamAssets.previewScale` 的全局系数缩小（像素预算 + 取景框长边两道上限）；系数为 1.0
 *     时输出与从前逐像素一致。
 *  3. **单张图集本身** —— PTX 只能整张解（4096² = 67 MB），PNG 还要多一份 `Bitmap`。
 *
 * 注意 1 与 2 的**峰值**是持平的：`Bitmap.createBitmap` 是拷贝，所以从前那份"裁片"在建成
 * 位图之前一直活着，峰值为 `图集 + Σ裁片`；现在换成 `图集 + Σ位图`。真正让峰值掉下来的是 2
 * （把 Σ位图 压到像素预算以内）。3 才是剩下的大头，要按块区域解码才消得掉 —— ASTC 无行填充、
 * 块行连续，理论上可行，见 `AtlasSplitter.inferAstcBlock` 那条精确相等的判据。
 */
object PamPreviewRunner {

    private const val TAG = "PamPreviewRunner"

    data class Input(
        val pamFile: File,
        /** `ATLASES/` 那一级。 */
        val atlasDir: File,
        /** `RESOURCES*.RTON`。已加密的会自动过一层解密。 */
        val rtonFile: File,
    )

    data class Preview(
        val pam: PamInfo,
        /** `PamInfo.image` 下标 -> 位图。调用方用完必须调 [close]。 */
        val images: Map<Int, Bitmap>,
        /** 可播的时间线；至少一条。 */
        val timelines: List<PamTimeline.SpriteRef>,
        /**
         * 与 [timelines] **一一对应**的取景框：每条时间线所有帧的绘制范围并集。
         * 预览照它等比缩放居中，内容才不会被裁掉。个别时间线一帧都画不出东西时为 null。
         */
        val bounds: List<PamTimeline.Bounds?>,
        val skippedPlaceholders: Int,
        /** 失败明细；**已由 [PamAssets] 截到 20 条**，这里不再截一遍。 */
        val failures: List<String>,
        /** 值得单说一句的情况，比如"这份 PAM 不含图片"。 */
        val note: String?,
    ) {
        fun close() {
            images.values.forEach { it.recycle() }
        }
    }

    sealed interface LoadResult {
        data class Ok(val preview: Preview) : LoadResult
        data class Failed(val message: String) : LoadResult
    }

    /**
     * 读入并解析三份输入。同步阻塞，调用方负责放到 `Dispatchers.IO` 上跑。
     *
     * @param key `datapack_prefs` 里的 `encryption_key`，与批量转换、图集拆分共用同一份
     */
    fun load(input: Input, key: String?): LoadResult {
        // 分阶段计时：实机"加载非常久"到底卡在哪一段，只有 logcat 能回答 —— 桌面 JVM 上没有
        // .PTX 样本，ASTC 解码那段本地量不出来。四个阶段各记一次，最后打一行。
        val t0 = System.nanoTime()
        val pamBytes = try {
            input.pamFile.readBytes()
        } catch (ex: Exception) {
            Log.w(TAG, "读取 PAM 失败: ${input.pamFile.absolutePath}", ex)
            return LoadResult.Failed("读取 PAM 失败：${ex.message ?: ex.javaClass.simpleName}")
        }

        val pam = try {
            PamBinaryReader.decode(pamBytes)
        } catch (ex: Exception) {
            Log.w(TAG, "解析 PAM 失败: ${input.pamFile.absolutePath}", ex)
            return LoadResult.Failed("解析 PAM 失败：${ex.message ?: ex.javaClass.simpleName}")
        }
        val tParse = System.nanoTime()

        val timelines = PamTimeline.sprites(pam)
        if (timelines.isEmpty()) {
            return LoadResult.Failed("这份 PAM 里既没有精灵也没有主时间线，没有可播放的内容")
        }

        // 取景框要在载入时一次算好：放进合成里每帧重算会直接把播放卡死。
        // 这正是"载入非常久"的头号开销 —— 从前它是 O(帧数²)，对**每条**时间线都要跑一遍
        // （预览只显示一条，可取景框得知道所有候选的尺寸才能定缩放）。现已改走
        // `PamTimeline.replay` 推进式重放，见 `PamTimeline.bounds` 的注释。只管几何、不碰
        // 图集，所以放在加载图集之前。
        val bounds = timelines.map {
            PamTimeline.bounds(pam, it.index, it.index == PamTimeline.MAIN_SPRITE)
        }
        val tBounds = System.nanoTime()

        if (!PamAssets.needsAtlas(pam)) {
            return LoadResult.Ok(
                Preview(pam, emptyMap(), timelines, bounds, 0, emptyList(), "这份 PAM 不含图片，不需要图集"),
            )
        }

        val plan = when (val r = AtlasSplitRunner.planFromFile(input.rtonFile, key)) {
            is AtlasSplitRunner.PlanResult.Ok -> r.plan
            AtlasSplitRunner.PlanResult.EncryptedNoKey ->
                return LoadResult.Failed("RTON 已加密，请先在数据包工具页配置密钥")

            is AtlasSplitRunner.PlanResult.Failed -> return LoadResult.Failed("RTON：${r.message}")
        }

        val onDisk = AtlasSplitter.scanAtlasDir(input.atlasDir)
        if (onDisk.isEmpty()) {
            return LoadResult.Failed("图集目录里没有 .PTX / .png 文件：${input.atlasDir.absolutePath}")
        }
        val tPlan = System.nanoTime()

        // 限分辨率：部件位图要常驻到预览关掉为止，4096² 图集切出来的几十个部件按原尺寸全留着
        // 就是几百 MB。取景框用各条时间线里最大的那个 —— 系数必须**全局唯一**，因为 `images`
        // 是按 image 下标共享的，逐帧/逐时间线各缩一个系数会让同一张图有两个尺寸。
        val viewW = bounds.filterNotNull().maxOfOrNull { it.width } ?: 0.0
        val viewH = bounds.filterNotNull().maxOfOrNull { it.height } ?: 0.0
        val imageScale = PamAssets.previewScale(pam, viewW, viewH)

        // 流式：`PamAssets` 裁好一张就回调一张，这里立刻转成位图，那个 `IntArray` 随即失去引用。
        // 于是"所有部件的**像素**同时活着"那份内存消失（图集本身仍在，见类注释）。
        //
        // `PamAssets` 交出来的图**已经是目标尺寸**（它负责把 RTON 各档裁出的矩形统一缩到
        // 标称尺寸，见 PamAssets.rescale），所以这里直接建位图。
        //
        // 早先这里有一道 `w * h != px.size` 的严格相等守卫，因为 PAM 记的是 1200 基准档的
        // 标称尺寸、而裁切矩形是「标称 × 档位/1200」，非 1200 档必然不等 —— 于是部件被一个个
        // 丢掉，实机上就是"缺胳膊少腿、腿接不上"。守卫已删，别再加回来。
        val bitmaps = HashMap<Int, Bitmap>()
        val assets = PamAssets.resolve(
            pam = pam,
            plan = plan,
            loadAtlas = { _, atlas ->
                val f = onDisk[atlas.name.lowercase()] ?: return@resolve null
                decodeAtlas(f, atlas)
            },
            imageScale = imageScale,
            onCrop = { index, crop ->
                bitmaps[index] =
                    Bitmap.createBitmap(crop.pixels, crop.width, crop.height, Bitmap.Config.ARGB_8888)
            },
        )

        // 实机冒烟时从 logcat 里就能看出哪一份开销是大头：常驻位图（限分辨率管这份）对
        // 最大单张图集的瞬时缓冲（要区域解码才压得下去，见类注释 3）。
        val tAssets = System.nanoTime()
        val pixels = bitmaps.values.sumOf { it.width.toLong() * it.height }
        val biggestAtlas = plan.atlases.values.maxOfOrNull { it.width.toLong() * it.height } ?: 0L
        val ms = { from: Long, to: Long -> (to - from) / 1_000_000 }
        Log.i(
            TAG,
            "解析到 ${assets.resolvedCount} 个部件，预览分辨率 ${(imageScale * 100).roundToInt()}%；" +
                "常驻位图 $pixels 像素（约 ${pixels * 4 / 1024 / 1024} MB）；" +
                "最大图集 $biggestAtlas 像素（约 ${biggestAtlas * 4 / 1024 / 1024} MB，瞬时）",
        )
        Log.i(
            TAG,
            "分阶段耗时（ms）：读+解析 PAM ${ms(t0, tParse)}；算取景框 ${ms(tParse, tBounds)}" +
                "（${timelines.size} 条时间线）；读 RTON+扫图集 ${ms(tBounds, tPlan)}；" +
                "解图集+裁切建位图 ${ms(tPlan, tAssets)}；合计 ${ms(t0, tAssets)}",
        )

        val base = when {
            bitmaps.isEmpty() -> "一张图都没解析出来，预览只会显示变换后的空框"
            assets.skippedPlaceholders > 0 && assets.scaleFactor != null ->
                PamAssets.describeScale(assets.scaleFactor) +
                    "；另跳过 ${assets.skippedPlaceholders} 张 1×1 占位图"

            assets.skippedPlaceholders > 0 ->
                "跳过 ${assets.skippedPlaceholders} 张 1×1 占位图（那是资源清单里的空条目）"

            assets.scaleFactor != null -> PamAssets.describeScale(assets.scaleFactor)

            else -> null
        }
        val note = listOfNotNull(
            base,
            // 一张都没解析出来时别提分辨率 —— 那会读成"已经渲染了，只是糊"，与实情相反
            if (imageScale < 1.0 && bitmaps.isNotEmpty()) {
                "部件过大，预览已按 %.0f%% 分辨率渲染以控制内存".format(imageScale * 100)
            } else null,
        ).joinToString("；").ifEmpty { null }

        return LoadResult.Ok(
            Preview(
                pam = pam,
                images = bitmaps,
                timelines = timelines,
                bounds = bounds,
                skippedPlaceholders = assets.skippedPlaceholders,
                failures = assets.failures,
                note = note,
            ),
        )
    }

    // ---- 图集解码 ----

    /**
     * 把一张图集**整张**解成 ARGB（行主序）。
     *
     * 刻意不和 `AtlasSplitRunner` 内部那份 `loadAtlas` 复用：那份为了省一次拷贝会保留
     * `Bitmap` 本体（`.png` 时），这份统一只交回 `IntArray`，关注点不同，各自写清楚更好。
     */
    private fun decodeAtlas(file: File, atlas: AtlasSplitter.Atlas): IntArray? = try {
        if (file.extension.equals("png", ignoreCase = true)) {
            decodePng(file, atlas)
        } else {
            decodePtx(file, atlas)
        }
    } catch (ex: Exception) {
        Log.w(TAG, "图集解码异常: ${file.name} (${atlas.width}x${atlas.height})", ex)
        null
    }

    private fun decodePng(file: File, atlas: AtlasSplitter.Atlas): IntArray? {
        val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        try {
            // PNG 头自带宽高，与 RTON 自报的尺寸核对：对不上说明配对错了图集，
            // 与其按错尺寸裁出一堆烂图，不如直接判这张图集加载失败。
            if (bmp.width != atlas.width || bmp.height != atlas.height) {
                Log.w(
                    TAG,
                    "${file.name} 尺寸 ${bmp.width}x${bmp.height} 与 RTON 记录的 " +
                        "${atlas.width}x${atlas.height} 不一致，跳过该图集",
                )
                return null
            }
            val out = IntArray(bmp.width * bmp.height)
            bmp.getPixels(out, 0, bmp.width, 0, 0, bmp.width, bmp.height)
            return out
        } finally {
            bmp.recycle()
        }
    }

    private fun decodePtx(file: File, atlas: AtlasSplitter.Atlas): IntArray? {
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
        val (bw, _) = block
        val info = PtxDecoder.PtxInfo(
            width = atlas.width,
            height = atlas.height,
            // PtxDecoder.decode 只吃 width/height/format，不校验 check；
            // 解包后 PTX_INFO 表已丢失，就地按定义合成即可。
            check = atlas.width * 16 / bw,
            format = if (bw == 5) PtxDecoder.FMT_ASTC_5x5 else PtxDecoder.FMT_ASTC_6x6,
            alphaSize = 0,
            alphaFormat = 0,
        )
        return PtxDecoder.decode(file.readBytes(), info)
    }
}
