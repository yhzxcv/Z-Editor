package com.example.z_editor.datapack.pam

import com.example.z_editor.datapack.smf.AtlasSplitter
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

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
     * 预览常驻位图的**像素预算**：所有部件按目标尺寸算的像素数之和不超过它。
     * 8M 像素 × 4 字节 ≈ 32 MB，见 [previewScale]。
     */
    private const val PREVIEW_BUDGET_PIXELS = 8.0 * 1024 * 1024

    /** 取景框长边的**目标像素上限**：预览控件本身约一千像素宽，再细的分辨率也画不出来。 */
    private const val PREVIEW_MAX_EDGE = 1536.0

    /**
     * 一张裁好、**并已缩放到目标尺寸**的图。
     *
     * 目标尺寸在没限分辨率时（`imageScale == 1`）恒等于 `PamInfo.image[i].size`；限了分辨率
     * 时是它的 `imageScale` 倍（`roundToInt` 后至少 1 像素），见 [previewScale] 与 [resolve]
     * 的 `imageScale` 参数。
     *
     * 所以调用方可以直接拿 [width]/[height] 去建位图，**但坐标必须按 `声明尺寸 ÷ 实际尺寸`
     * 补一个缩放** —— 帧变换矩阵是按声明尺寸的像素空间定义的。预览侧的补法见
     * `PamPreviewScreen.PreviewCanvas` 里的 `imageFit`。
     */
    class Crop(val width: Int, val height: Int, val pixels: IntArray)

    data class Result(
        /**
         * `PamInfo.image` 的下标 -> 已裁好并缩放的图。没有的项表示没解析出来。
         * **流式模式（[resolve] 传了 `onCrop`）下恒为空**，因为裁一张就交出去一张了。
         */
        val crops: Map<Int, Crop>,
        /** 1×1 全透明占位图，按设计跳过。 */
        val skippedPlaceholders: Int,
        val failures: List<String>,
        /**
         * 观测到的**档位系数**（裁切矩形 ÷ PAM 声明尺寸）的中位数；一张都没裁出来时为 null。
         * 约 1.28 / 0.64 / 0.32 分别对应 1536 / 768 / 384 档，见 [describeScale]。
         *
         * 注意它**与 `imageScale` 无关**：算的是裁切矩形对声明尺寸的比，限分辨率不改这个比，
         * 所以提示语里的档位判断不会被限分辨率带偏。
         */
        val scaleFactor: Double?,
        /** 成功解析出的部件数。流式模式下 [crops] 是空的，只能看这个。 */
        val resolvedCount: Int = crops.size,
    )

    /**
     * @param loadAtlas 给定图集，返回**整张**图集的像素（行主序、长度 `width * height`）；
     *   返回 null 表示这张图集读不出来（文件缺失、块尺寸推不出、解码失败……），
     *   该图集下的所有图片会统一计入 [Result.failures]。
     * @param imageScale 目标尺寸的全局缩小系数，取值见 [previewScale]；`1.0` 表示按 PAM 声明的
     *   原尺寸出图。**只影响位图分辨率，不影响几何**：调用方按 `声明尺寸 ÷ 实际尺寸` 把变换
     *   矩阵补回来即可（见 [Crop] 的说明）。
     * @param onCrop 流式消费：每裁好一张立刻回调，**且不再攒进 [Result.crops]**。回调返回后
     *   这张图的 `IntArray` 就已无人引用，可以立刻被回收 —— 于是"载入期所有部件的像素同时
     *   活着"那份内存整个消失。预览走的就是这条路（回调里直接建位图）。
     */
    fun resolve(
        pam: PamInfo,
        plan: AtlasSplitter.Plan,
        loadAtlas: (atlasId: String, atlas: AtlasSplitter.Atlas) -> IntArray?,
        imageScale: Double = 1.0,
        onCrop: ((imageIndex: Int, crop: Crop) -> Unit)? = null,
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
        var resolved = 0

        // 按图集分组，一张图集**整批裁完就丢**：`full` 出了这轮循环体就再没有引用，下一张图集
        // 可以立刻复用这块内存。峰值因此是「单张图集 + 该图集的一张裁片」，而不是「单张图集 +
        // 所有图集的所有裁片」—— 后者在 4096² 图集上就是几百 MB 的瞬时尖峰。
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
                // 目标尺寸 = 声明尺寸 × imageScale。系数为 1 时逐字节等于从前。
                val tw = scaledSize(dw, imageScale)
                val th = scaledSize(dh, imageScale)
                try {
                    require(AtlasSplitter.isRectInBounds(rect, atlas)) {
                        "矩形 (${rect.ax},${rect.ay},${rect.aw},${rect.ah}) 超出 ${atlas.name} ${atlas.width}x${atlas.height}"
                    }
                    val raw = AtlasSplitter.crop(full, atlas.width, rect)
                    val crop = if (rect.aw == tw && rect.ah == th) {
                        Crop(tw, th, raw)
                    } else {
                        Crop(tw, th, resize(raw, rect.aw, rect.ah, tw, th))
                    }
                    resolved++
                    if (onCrop != null) onCrop(imageIndex, crop) else crops[imageIndex] = crop
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
            resolvedCount = resolved,
        )
    }

    /** 声明尺寸 × 系数，至少 1 像素；系数 ≥ 1 时原样返回（不做放大）。 */
    private fun scaledSize(v: Int, scale: Double): Int =
        if (scale >= 1.0) v else (v * scale).roundToInt().coerceAtLeast(1)

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
     * 裁出来的 [src]（[sw]×[sh]）缩到 [dw]×[dh]，按缩小倍数挑一条重采样路径。
     *
     * 缩小 **2 倍以上**走 [rescaleBox]（方框平均），否则走 [rescale]（2×2 双线性）。
     * 分界线卡在 2 倍是有意的：`imageScale == 1` 时唯一的缩小是「1536 档裁出来的 1.28 倍」，
     * 落在双线性那边 —— 于是**限分辨率整个关掉时，输出与从前逐像素一致**。
     */
    private fun resize(src: IntArray, sw: Int, sh: Int, dw: Int, dh: Int): IntArray =
        if (sw >= dw * 2 || sh >= dh * 2) rescaleBox(src, sw, sh, dw, dh)
        else rescale(src, sw, sh, dw, dh)

    /**
     * 方框平均缩小：每个目标像素取源图里落进它方框的**全部**像素平均（按 alpha 预乘）。
     *
     * [rescale] 只读 2×2 邻域，缩 2.56 倍时有 61% 的源像素一次都没被读过，高频细节会闪成
     * 摩尔纹 —— 而预览限分辨率后正好落在那个区间，所以另开一条。方框全采，缩多少倍都不漏。
     *
     * 预乘的理由与 [rescale] 相同：不预乘的话，半透明边缘会把透明像素的黑色一起平均进来。
     */
    private fun rescaleBox(src: IntArray, sw: Int, sh: Int, dw: Int, dh: Int): IntArray {
        val out = IntArray(dw * dh)
        val stepX = sw.toDouble() / dw
        val stepY = sh.toDouble() / dh
        for (y in 0 until dh) {
            // 方框取 [y·step, (y+1)·step) 的**整数像素**边界；退化成空框时兜到 1 像素高。
            val y0 = (y * stepY).toInt().coerceIn(0, sh - 1)
            val y1 = ((y + 1) * stepY).toInt().coerceIn(y0 + 1, sh)
            for (x in 0 until dw) {
                val x0 = (x * stepX).toInt().coerceIn(0, sw - 1)
                val x1 = ((x + 1) * stepX).toInt().coerceIn(x0 + 1, sw)
                var sa = 0L
                var sr = 0L
                var sg = 0L
                var sb = 0L
                for (sy in y0 until y1) {
                    val row = sy * sw
                    for (sx in x0 until x1) {
                        val p = src[row + sx]
                        val a = (p ushr 24) and 0xFF
                        sa += a
                        sr += ((p ushr 16) and 0xFF) * a
                        sg += ((p ushr 8) and 0xFF) * a
                        sb += (p and 0xFF) * a
                    }
                }
                // sa == 0 时整框全透明，out 本来就是 0，跳过省一次除法
                if (sa == 0L) continue
                val n = ((y1 - y0) * (x1 - x0)).toLong()
                val ai = (sa.toDouble() / n).roundToInt().coerceIn(0, 255)
                out[y * dw + x] = if (ai == 0) 0 else (ai shl 24) or
                    ((sr.toDouble() / sa).roundToInt().coerceIn(0, 255) shl 16) or
                    ((sg.toDouble() / sa).roundToInt().coerceIn(0, 255) shl 8) or
                    (sb.toDouble() / sa).roundToInt().coerceIn(0, 255)
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
     * 预览该把部件缩到多小：返回一个 `0 < s ≤ 1` 的全局系数，[resolve] 的 `imageScale` 直接吃它。
     *
     * 两道上限取**更紧的那个**：
     *
     *  * **像素预算** [PREVIEW_BUDGET_PIXELS] —— 所有部件按目标尺寸算的像素数之和不超过它。
     *    这是真正兜住 OOM 的那道：一张 4096² 图集切出来的几十个部件，声明尺寸之和轻易上千万
     *    像素，全按原尺寸常驻就是几百 MB。因为面积按 s² 缩，所以取平方根。
     *
     *  * **取景框长边** [PREVIEW_MAX_EDGE] —— 预览控件只有一千来像素宽，比这更细的分辨率在屏幕
     *    上根本体现不出来。部件多而碎的 PAM 靠这道压得更狠。
     *
     * 两道都不紧张时返回 `1.0`，即**出图与加限分辨率之前逐像素一致**（[resize] 的分流保证了
     * 这一点）。真正紧张时的观感与"该不该换 1536 档图集"由 [describeScale] 那条提示负责，
     * 这里不重复报。
     *
     * 只看**声明尺寸**、不看部件到底有没有被画到：没被引用的图片也照样算进预算。偏保守，
     * 但省得为了几 MB 去穿整条求值链。
     *
     * @param viewWidth / @param viewHeight 取景框（`PamTimeline.bounds` 的并集）的宽高；传 0
     *   表示未知，那道上限就不生效。
     */
    fun previewScale(pam: PamInfo, viewWidth: Double, viewHeight: Double): Double {
        var total = 0.0
        for (im in pam.image) {
            val w = im.size?.getOrNull(0) ?: continue
            val h = im.size?.getOrNull(1) ?: continue
            if (w > 0 && h > 0) total += w.toDouble() * h
        }
        val byBudget =
            if (total <= PREVIEW_BUDGET_PIXELS) 1.0 else sqrt(PREVIEW_BUDGET_PIXELS / total)

        val edge = maxOf(viewWidth, viewHeight)
        val byEdge = if (edge <= PREVIEW_MAX_EDGE) 1.0 else PREVIEW_MAX_EDGE / edge

        return minOf(1.0, byBudget, byEdge)
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
