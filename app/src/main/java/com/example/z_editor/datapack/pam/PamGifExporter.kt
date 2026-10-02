package com.example.z_editor.datapack.pam

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.example.z_editor.datapack.pam.gif.GifEncoder
import com.example.z_editor.datapack.pam.gif.GifPaletteBuilder
import com.example.z_editor.datapack.ui.FALLBACK_CANVAS
import com.example.z_editor.datapack.ui.PamFramePainter
import com.example.z_editor.datapack.ui.PreviewTransform
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * PAM 动画 → GIF。**同步阻塞**，调用方负责放到 `Dispatchers.Default`。
 *
 * ## 两遍法，内存与帧数无关
 *
 * 第一遍逐帧渲染进**同一张复用位图**、`getPixels` 进复用的 IntArray、按步长采样进全局直方图
 * 后即弃；第二遍重新渲染 → [GifEncoder.writeFrame] → LZW 直接进流。
 * 全程不保留任何一帧的像素，峰值 ≈ 位图 + 像素数组 + 编码器表 —— 1936 帧的动画与 2 帧的动画
 * 占用一样多。代价是渲染两遍（渲染比量化贵，但换来的是"帧多了不会 OOM"）。
 *
 * ## 画质与几何
 *
 * 像素来源是预览常驻位图（WYSIWYG，不做原尺寸重裁）；绘制走 [PamFramePainter]，
 * 与预览**同一份**代码和 [PreviewTransform] 取景公式 —— 导出与预览不一致是这类功能最容易
 * 出的问题，共用绘制器就是为此。
 */
object PamGifExporter {

    /** 导出边长上限。再大对 GIF 没意义（≤256 色），只是白烧内存。 */
    const val EXPORT_MAX_EDGE = 2048

    private const val MIN_EDGE = 16

    /**
     * 直方图采样步长。取质数：规则的网格步长碰上条纹背景会**采出一整族同相位像素**，
     * 把调色板预算浪费在背景的少数几种颜色上。
     */
    private const val HISTOGRAM_STRIDE = 7

    private const val STREAM_BUFFER = 64 * 1024

    /** 背景色。GIF 的透明是二值的，软边会被硬切。 */
    enum class Background { TRANSPARENT, WHITE, BLACK }

    /**
     * @param longEdge 最长边的目标像素数；null = 用取景框原始尺寸（约等于不缩放）
     */
    data class Options(
        val longEdge: Int? = null,
        val background: Background = Background.TRANSPARENT,
        val dither: Boolean = false,
        val loopForever: Boolean = true,
    )

    sealed interface Result {
        data class Ok(
            val file: File,
            val byteSize: Long,
            val frames: Int,
            val width: Int,
            val height: Int,
            val delayCs: Int,
        ) : Result

        data class Failed(val message: String) : Result
    }

    // ---- 纯决策函数（无 Android 依赖，单测直测） ----

    /**
     * 导出画布尺寸：按最长边等比缩，夹在 [MIN_EDGE]..[EXPORT_MAX_EDGE]。
     *
     * 取景框为 null 或尺寸非正时走 [FALLBACK_CANVAS] 正方（与预览同一套兜底）。
     * 特意**不夹宽高比**：控件那侧 `coerceIn(0.25, 4.0)` 是给控件塑形用的，导出要按真实
     * 取景框出图（见 [PreviewTransform.framing]）。
     *
     * @param longEdge null = 取景框原始尺寸
     */
    fun exportSize(view: PamTimeline.Bounds?, longEdge: Int?): Pair<Int, Int> {
        val viewW = view?.width?.takeIf { it > 0.0 } ?: FALLBACK_CANVAS
        val viewH = view?.height?.takeIf { it > 0.0 } ?: FALLBACK_CANVAS
        // 原始尺寸模式：长边向上取整（宁可多一像素也不要把内容裁掉）
        val long = (longEdge ?: ceil(max(viewW, viewH)).toInt()).coerceIn(MIN_EDGE, EXPORT_MAX_EDGE)
        val scale = long / max(viewW, viewH)
        return (viewW * scale).roundToInt().coerceAtLeast(1) to
                (viewH * scale).roundToInt().coerceAtLeast(1)
    }

    /**
     * 每帧延迟，单位厘秒（GIF 的时间单位）。
     *
     * 下限 2 厘秒不是随便定的：<2 的延迟会被浏览器**钳成 10 厘秒**（历史遗留的兼容行为），
     * 于是"设成 1 想更快"反而变得极慢。上限 60000 是 GCE 的 u16 字段边界。
     */
    fun delayCs(fps: Double): Int {
        if (!fps.isFinite() || fps <= 0.0) return 10 // 帧率缺失时按 100ms 走，好过抛异常
        return (1000.0 / fps / 10.0).roundToInt().coerceIn(2, 60000)
    }

    // ---- 导出 ----

    /**
     * @param timelineIndex 取景框下标（与 `preview.bounds` 对齐；主时间线是那个 `MAIN_SPRITE` 项）
     * @param frames 整条时间线的求值结果（[PamTimeline.evaluateAll] 的产物）
     * @param hiddenLayers 当前隐藏的图层号集合 —— 导出遵循它，所见即所得
     * @param onProgress (遍号 1/2, 本遍已完成帧数, 总帧数)。两遍都会报到，UI 据此画进度
     */
    fun export(
        preview: PamPreviewRunner.Preview,
        timelineIndex: Int,
        frames: List<List<PamTimeline.DrawOp>>,
        hiddenLayers: Set<Int>,
        startFrame: Int,
        endInclusive: Int,
        fps: Double,
        options: Options,
        output: File,
        onProgress: (phase: Int, done: Int, total: Int) -> Unit,
    ): Result {
        val start = startFrame.coerceAtLeast(0)
        val end = endInclusive.coerceAtMost(frames.lastIndex)
        if (frames.isEmpty() || start > end) return Result.Failed("没有可导出的帧")

        val total = end - start + 1
        val view = PreviewTransform.paddedView(preview.bounds.getOrNull(timelineIndex))
        val (width, height) = exportSize(view, options.longEdge)
        val delay = delayCs(fps)
        val transparent = options.background == Background.TRANSPARENT

        var bitmap: Bitmap? = null
        var out: BufferedOutputStream? = null
        try {
            // 用局部 val：`bitmap` 是可空 var，被下面的 render 闭包捕获后无法智能转换
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap = bmp
            val canvas = Canvas(bmp)
            val pixels = IntArray(width * height)
            val painter = PamFramePainter(preview)

            fun render(i: Int) {
                bmp.eraseColor(
                    when (options.background) {
                        Background.TRANSPARENT -> Color.TRANSPARENT
                        Background.WHITE -> Color.WHITE
                        Background.BLACK -> Color.BLACK
                    }
                )
                val ops = frames[i]
                // filter 新建列表：绝不能原地改 frames 里缓存共享的那份
                val visible = if (hiddenLayers.isEmpty()) ops else ops.filter { it.layer !in hiddenLayers }
                painter.draw(canvas, visible, width.toFloat(), height.toFloat(), view)
                bmp.getPixels(pixels, 0, width, 0, 0, width, height)
            }

            // 第一遍：采样建全局调色板
            val hist = GifPaletteBuilder.Histogram()
            for (i in start..end) {
                render(i)
                hist.add(pixels, pixels.size, HISTOGRAM_STRIDE)
                onProgress(1, i - start + 1, total)
            }
            val palette = GifPaletteBuilder.build(hist, wantTransparent = transparent)

            // 第二遍：重新渲染并写盘
            val bos = BufferedOutputStream(FileOutputStream(output), STREAM_BUFFER)
            out = bos
            val enc = GifEncoder.start(bos, width, height, palette, options.loopForever)
            for (i in start..end) {
                render(i)
                enc.writeFrame(pixels, delay, options.dither)
                onProgress(2, i - start + 1, total)
            }
            enc.finish()
            bos.flush()
            bos.close()
            out = null

            return Result.Ok(
                file = output,
                byteSize = output.length(),
                frames = total,
                width = width,
                height = height,
                delayCs = delay,
            )
        } catch (e: Throwable) {
            // 连 OutOfMemoryError 一起接：导出不该把整个页面带走
            runCatching { out?.close() }
            // 删半成品：源文件旁躺一个截断的 .gif，下次重名解析会把它当成"已存在"而加 `~`
            runCatching { output.delete() }
            return Result.Failed(e.message ?: e.javaClass.simpleName)
        } finally {
            bitmap?.recycle()
        }
    }
}
