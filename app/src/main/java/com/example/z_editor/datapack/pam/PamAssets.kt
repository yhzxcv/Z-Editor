package com.example.z_editor.datapack.pam

import com.example.z_editor.datapack.smf.AtlasSplitter
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 把 PAM 用到的图片，经 RTON 清单 + 图集，解析成 ARGB 像素。
 *
 * 纯 JVM：图集的**解码**通过 [resolve] 的 `loadAtlas` 参数注入（`ATLASES/` 下可能是 `.PTX`
 * 也可能是 `.png`，后者要碰 `BitmapFactory`），所以本类可单测 —— 测试传一个造假像素的
 * lambda 即可，不必真去解一张图集。
 *
 *
 * `PamImage.name` 的形态是 `短名|资源全大写ID`，例如
 * `Background_Dark_Brazier_bottom_440x229|IMAGE_BACKGROUNDS_BACKGROUND_DARK_BRAZIER_BOTTOM_…_440X229`。
 * 竖线后半段**就是 RTON 里图片条目的 `id`**（与 `IMAGE_CREDITS_PVZ2_LOGO_CREDITS` 同构），
 * 所以按 id 精确查即可，不需要拆词猜大小写。
 *
 * PAM 自身**不带源矩形** —— `src_rect` 位在真样本 13 万+ 个 change 里 0 命中，所以
 * `ax/ay/aw/ah` 只能来自 RTON。`AtlasSplitter.Plan.byAtlas` 正好已按资源 id 索引，直接用。
 */
object PamAssets {

    /** 失败明细的条数上限，避免结果区被刷屏（与 `AtlasSplitRunner` 同惯例）。 */
    private const val MAX_REPORTED_FAILURES = 20

    /**
     * 一张裁好、**并已缩放到 PAM 声明尺寸**的图。
     *
     * [width] / [height] 恒等于 `PamInfo.image[i].size`，所以调用方可以直接拿去建位图，
     * 不需要（也不应该）再拿 PAM 的尺寸去核对 —— 见 [rescale] 的说明。
     */
    class Crop(val width: Int, val height: Int, val pixels: IntArray)

    data class Result(
        /** `PamInfo.image` 的下标 -> 已裁好并缩放的图。没有的项表示没解析出来。 */
        val crops: Map<Int, Crop>,
        /** 1×1 全透明占位图，按设计跳过。 */
        val skippedPlaceholders: Int,
        val failures: List<String>,
        /**
         * 观测到的**档位系数**（裁切矩形 ÷ PAM 声明尺寸）的中位数；一张都没裁出来时为 null。
         * 约 1.28 / 0.64 / 0.32 分别对应 1536 / 768 / 384 档，见 [describeScale]。
         */
        val scaleFactor: Double?,
    ) {
        val resolvedCount: Int get() = crops.size
    }

    /**
     * @param loadAtlas 给定图集，返回**整张**图集的像素（行主序、长度 `width * height`）；
     *   返回 null 表示这张图集读不出来（文件缺失、块尺寸推不出、解码失败……），
     *   该图集下的所有图片会统一计入 [Result.failures]。
     */
    fun resolve(
        pam: PamInfo,
        plan: AtlasSplitter.Plan,
        loadAtlas: (atlasId: String, atlas: AtlasSplitter.Atlas) -> IntArray?,
    ): Result {
        val index = resourceIndex(plan)

        val failures = ArrayList<String>()
        var skipped = 0

        // image 下标 -> (图集 id, 矩形)
        val targets = LinkedHashMap<Int, Pair<String, AtlasSplitter.Image>>()
        pam.image.forEachIndexed { i, im ->
            val raw = im.name
            val id = resourceIdOf(raw)
            if (id == null) {
                failures += "image[$i]: 名字为空或没有可用的资源 id（原值 ${raw ?: "null"}）"
                return@forEachIndexed
            }
            val hit = index[id.lowercase()]
            if (hit == null) {
                failures += "image[$i] $id: RTON 清单里没有这个资源 id"
                return@forEachIndexed
            }
            if (AtlasSplitter.isPlaceholder(hit.second)) {
                skipped++
                return@forEachIndexed
            }
            targets[i] = hit
        }

        val crops = HashMap<Int, Crop>()
        val ratios = ArrayList<Double>()
        for ((atlasId, entries) in targets.entries.groupBy { it.value.first }) {
            val atlas = plan.atlases[atlasId]
            if (atlas == null) {
                entries.forEach { failures += "image[${it.key}]: RTON 里没有 id 为 $atlasId 的图集" }
                continue
            }

            val full = loadAtlas(atlasId, atlas)
            if (full == null) {
                entries.forEach { failures += "image[${it.key}]: 图集 ${atlas.name} 读不出来" }
                continue
            }

            for ((imageIndex, pair) in entries) {
                val rect = pair.second
                val declared = pam.image.getOrNull(imageIndex)?.size
                val dw = declared?.getOrNull(0) ?: 0
                val dh = declared?.getOrNull(1) ?: 0
                if (dw <= 0 || dh <= 0) {
                    failures += "image[$imageIndex] ${rect.id}: PAM 没记这个部件的尺寸（${dw}x$dh），无法确定该缩到多大"
                    continue
                }
                try {
                    require(AtlasSplitter.isRectInBounds(rect, atlas)) {
                        "矩形 (${rect.ax},${rect.ay},${rect.aw},${rect.ah}) 超出 ${atlas.name} ${atlas.width}x${atlas.height}"
                    }
                    val raw = AtlasSplitter.crop(full, atlas.width, rect)
                    crops[imageIndex] = if (rect.aw == dw && rect.ah == dh) {
                        Crop(dw, dh, raw)
                    } else {
                        Crop(dw, dh, rescale(raw, rect.aw, rect.ah, dw, dh))
                    }
                    if (rect.aw > 0 && rect.ah > 0) {
                        ratios += (rect.aw.toDouble() / dw + rect.ah.toDouble() / dh) / 2.0
                    }
                } catch (ex: Exception) {
                    failures += "image[$imageIndex] ${rect.id}: ${ex.message ?: ex.javaClass.simpleName}"
                }
            }
        }

        return Result(
            crops = crops,
            skippedPlaceholders = skipped,
            failures = failures.take(MAX_REPORTED_FAILURES),
            scaleFactor = ratios.takeIf { it.isNotEmpty() }?.sorted()?.let { it[it.size / 2] },
        )
    }

    /**
     * 把裁出来的 [src]（[sw]×[sh]）等比缩放到 PAM 声明的 [dw]×[dh]。
     *
     * ## 为什么必须缩
     *
     * PAM 的 `image[].size` 是 **1200 基准档的标称尺寸**，与它自己在哪个档位的目录下无关；
     * 而 RTON 里同一个资源 id 在 384/768/1536 **每一档各有一条**，裁出来的矩形是
     * `标称 × 档位/1200`。实测 33717 组「声明尺寸 ↔ 三档裁切矩形」对照，系数中位数
     * 1.2837 / 0.6434 / 0.3235，即理论值 1.28 / 0.64 / 0.32（余量是矩形取整）。
     *
     * 所以把 1536 档裁出来的图**直接按 `image[].size` 建位图会大 1.28 倍**：部件之间会
     * 互相错开、看着"接不上"。原先这里是一道 `w * h != px.size` 的严格相等守卫，做法是
     * **丢弃**该部件 —— 于是画面缺胳膊少腿。改成缩放后，预览在几何上与档位无关。
     *
     * 顺带一提：这里**不做**长宽比校验。实测残差中位 0.17px，只有 25/33717 组超过 3px，
     * 且全是 `1586x49`、`12x156` 这类细长图 —— 短边 ±1px 的取整被摊到长边上放大了，
     * 不是坏数据。用百分比阈值反而会误伤小图（`33x14` 差 1px 就是 7%）。
     */
    private fun rescale(src: IntArray, sw: Int, sh: Int, dw: Int, dh: Int): IntArray {
        val out = IntArray(dw * dh)
        val stepX = sw.toDouble() / dw
        val stepY = sh.toDouble() / dh
        val maxX = (sw - 1).toDouble()
        val maxY = (sh - 1).toDouble()
        for (y in 0 until dh) {
            // 用像素中心对齐（+0.5 / −0.5），否则整张图会往左上角偏半个像素
            val fy = ((y + 0.5) * stepY - 0.5).coerceIn(0.0, maxY)
            val y0 = fy.toInt()
            val y1 = minOf(y0 + 1, sh - 1)
            val wy = fy - y0
            for (x in 0 until dw) {
                val fx = ((x + 0.5) * stepX - 0.5).coerceIn(0.0, maxX)
                val x0 = fx.toInt()
                val x1 = minOf(x0 + 1, sw - 1)
                out[y * dw + x] = blend4(
                    src[y0 * sw + x0], src[y0 * sw + x1],
                    src[y1 * sw + x0], src[y1 * sw + x1],
                    fx - x0, wy,
                )
            }
        }
        return out
    }

    /**
     * 四邻域双线性混合。**先按 alpha 预乘再插值**：直接对 ARGB 分量插值的话，
     * 半透明边缘会把透明像素的黑色也混进来，剪纸动画的柔边会整圈发黑。
     */
    private fun blend4(p00: Int, p10: Int, p01: Int, p11: Int, wx: Double, wy: Double): Int {
        val w00 = (1 - wx) * (1 - wy)
        val w10 = wx * (1 - wy)
        val w01 = (1 - wx) * wy
        val w11 = wx * wy

        val a00 = (p00 ushr 24) and 0xFF
        val a10 = (p10 ushr 24) and 0xFF
        val a01 = (p01 ushr 24) and 0xFF
        val a11 = (p11 ushr 24) and 0xFF

        val a = a00 * w00 + a10 * w10 + a01 * w01 + a11 * w11
        val ai = a.roundToInt().coerceIn(0, 255)
        if (ai == 0) return 0
        val r = ((p00 ushr 16) and 0xFF) * a00 * w00 + ((p10 ushr 16) and 0xFF) * a10 * w10 +
            ((p01 ushr 16) and 0xFF) * a01 * w01 + ((p11 ushr 16) and 0xFF) * a11 * w11
        val g = ((p00 ushr 8) and 0xFF) * a00 * w00 + ((p10 ushr 8) and 0xFF) * a10 * w10 +
            ((p01 ushr 8) and 0xFF) * a01 * w01 + ((p11 ushr 8) and 0xFF) * a11 * w11
        val b = (p00 and 0xFF) * a00 * w00 + (p10 and 0xFF) * a10 * w10 +
            (p01 and 0xFF) * a01 * w01 + (p11 and 0xFF) * a11 * w11
        return (ai shl 24) or
            ((r / a).roundToInt().coerceIn(0, 255) shl 16) or
            ((g / a).roundToInt().coerceIn(0, 255) shl 8) or
            (b / a).roundToInt().coerceIn(0, 255)
    }

    /**
     * 把档位系数说成人话，给结果区用。
     *
     * 图集是「拿哪个档位裁的」直接决定预览清不清晰：384 档裁出来要放大 3 倍，会明显发糊，
     * 这时该提示用户换成 1536 档那套图集。
     */
    fun describeScale(scale: Double): String {
        val tier = listOf(384, 768, 1536).minByOrNull { abs(it / 1200.0 - scale) }
            ?: return "档位系数 %.3f".format(scale)
        val base = "图集按 %d 档裁出（%.3f 倍），已缩到 PAM 声明的尺寸".format(tier, scale)
        return if (tier < 1536) "$base —— 换 1536 档的图集会更清晰" else base
    }

    /**
     * 资源 id（小写）-> (图集 id, 矩形)。图集内同一资源可能被引用多次，保留第一条即可。
     *
     * ## 关于档位（读这段前先看 [rescale]）
     *
     * `plan.byAtlas` 是 `AtlasSplitter.planFromRoot` 按资源 id **去重之后**的结果，而同一个
     * id 在 RTON 里 384/768/1536 三档各有一条 —— 所以这里拿到的是**其中一档**（实测该 RTON
     * 的遍历顺序恒为 1536 在前，即拿到的总是 1536 档，裁出来最清楚）。
     *
     * 因为 [rescale] 会把裁出来的图统一缩到 PAM 声明的尺寸，**选到哪一档都不影响几何**，
     * 只影响清晰度（见 [describeScale]）。
     *
     * 唯一的硬性要求是：用户指定的 `ATLASES/` 目录里得有**这一档**的文件。整包解出来的
     * 目录三档俱全，没问题；要是有人只留了一档且不是 1536，这里会**报错**（"图集 X 读不出来"）
     * 而不是画错 —— 逐张报失败是有意为之。
     *
     * 之所以不在这里挑一档「磁盘上真实存在的」，是因为档位信息在 `planFromRoot` 摊平去重时
     * 就丢了，要改得动 `AtlasSplitter`；而那条路是图集拆分功能（dark 929/929、dynamic 867/867
     * 逐像素验证过）在走的，不为了这个边角情况去动它。
     */
    private fun resourceIndex(
        plan: AtlasSplitter.Plan,
    ): Map<String, Pair<String, AtlasSplitter.Image>> {
        val out = HashMap<String, Pair<String, AtlasSplitter.Image>>()
        for ((atlasId, images) in plan.byAtlas) {
            for (im in images) out.putIfAbsent(im.id.lowercase(), atlasId to im)
        }
        return out
    }

    /**
     * 取 `短名|资源ID` 里竖线后半段。
     *
     * 真样本里这一半恒存在，但**没有竖线时退回整个名字**去查 —— 查不到会记进
     * [Result.failures]，不会静默画错。
     *
     * `internal` 而非 `private`：[PamUnpackScan] 给候选 RTON 打分时也要提取这份 id，
     * 而"打分口径"必须与这里的查找口径**是同一个函数** —— 另抄一份的话两边会静默漂移，
     * 变成"排名说这份 RTON 能覆盖，解析却说清单里没有这个资源 id"。
     */
    internal fun resourceIdOf(name: String?): String? {
        val n = name?.trim().orEmpty()
        if (n.isEmpty()) return null
        val bar = n.lastIndexOf('|')
        val id = if (bar >= 0) n.substring(bar + 1) else n
        return id.trim().ifEmpty { null }
    }

    /** 便于调用方判断"这个 PAM 到底需不需要图集"：一张图都没有时可以直接跳过加载。 */
    fun needsAtlas(pam: PamInfo): Boolean = pam.image.isNotEmpty()

    /** 图集目录扫描的转发，省得调用方同时 import 两个类。 */
    fun scanAtlasDir(dir: File): Map<String, File> = AtlasSplitter.scanAtlasDir(dir)
}
