package com.example.z_editor.datapack.pam

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.z_editor.datapack.smf.AtlasSplitRunner
import com.example.z_editor.datapack.smf.AtlasSplitter
import com.example.z_editor.datapack.smf.PtxDecoder
import java.io.File

/**
 * PAM 预览的 Android 侧编排：把三份输入（PAM / 图集目录 / RTON 清单）变成「可画的帧」。
 *
 * 与 `PamTimeline`（纯算法）、`PamAssets`（纯算法）分工：本类才碰 Android
 * （`Bitmap` / `BitmapFactory` / `Log`），因为单测配置没有
 * `testOptions { isReturnDefaultValues = true }`，一调 Android stub 就抛 `not mocked`。
 * 形态照 `AtlasSplitRunner`。
 *
 * **内存**：一张 4096×4096 图集结成 ARGB 是 64 MB，所以逐张解码、裁完即弃，
 * 峰值为单张图集加几十张裁好的小图。整批不缓存。
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

        val timelines = PamTimeline.sprites(pam)
        if (timelines.isEmpty()) {
            return LoadResult.Failed("这份 PAM 里既没有精灵也没有主时间线，没有可播放的内容")
        }

        // 取景框要在载入时一次算好：逐帧扫是 O(帧数²)（最坏约 100ms），放进合成里每帧重算
        // 会直接把播放卡死。只管几何、不碰图集，所以放在加载图集之前。
        val bounds = timelines.map {
            PamTimeline.bounds(pam, it.index, it.index == PamTimeline.MAIN_SPRITE)
        }

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

        val assets = PamAssets.resolve(pam, plan) { _, atlas ->
            val f = onDisk[atlas.name.lowercase()] ?: return@resolve null
            decodeAtlas(f, atlas)
        }

        // `PamAssets` 交出来的图**已经是 PAM 声明的尺寸**（它负责把 RTON 各档裁出的矩形
        // 统一缩到标称尺寸，见 PamAssets.rescale），所以这里直接建位图。
        //
        // 早先这里有一道 `w * h != px.size` 的严格相等守卫，因为 PAM 记的是 1200 基准档的
        // 标称尺寸、而裁切矩形是「标称 × 档位/1200」，非 1200 档必然不等 —— 于是部件被一个个
        // 丢掉，实机上就是"缺胳膊少腿、腿接不上"。守卫已删，别再加回来。
        val bitmaps = HashMap<Int, Bitmap>()
        for ((index, crop) in assets.crops) {
            bitmaps[index] = Bitmap.createBitmap(crop.pixels, crop.width, crop.height, Bitmap.Config.ARGB_8888)
        }

        val note = when {
            bitmaps.isEmpty() -> "一张图都没解析出来，预览只会显示变换后的空框"
            assets.skippedPlaceholders > 0 && assets.scaleFactor != null ->
                PamAssets.describeScale(assets.scaleFactor) +
                    "；另跳过 ${assets.skippedPlaceholders} 张 1×1 占位图"

            assets.skippedPlaceholders > 0 ->
                "跳过 ${assets.skippedPlaceholders} 张 1×1 占位图（那是资源清单里的空条目）"

            assets.scaleFactor != null -> PamAssets.describeScale(assets.scaleFactor)

            else -> null
        }

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
