package com.example.z_editor.datapack.pam

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

object PamTimeline {

    /** main sprite 的伪下标。它不在 `PamInfo.sprite` 里，单独拿一条时间线表示。 */
    const val MAIN_SPRITE = -1

    /** 嵌套精灵的展开深度上限，防数据损坏导致的无限递归。 */
    private const val MAX_NEST_DEPTH = 8

    /** 未染色的 tint，即"原色"。 */
    private const val NO_TINT = 0xFFFFFF

    private val IDENTITY = listOf(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

    data class DrawOp(
        /** `PamInfo.image` 的下标。 */
        val imageIndex: Int,
        val tx: Double,
        val ty: Double,
        val m0: Double,
        val m1: Double,
        val m2: Double,
        val m3: Double,
        /** 0..255。 */
        val alpha: Int,
        /**
         * `0xRRGGBB`，`0xFFFFFF` 表示原色。来自 `PamMove.color` 的 RGB 分量，**逐通道乘算**。
         *
         * 绝大多数 color 是 `(255,255,255,α)`（只调透明度），但真样本里有 819 条灰度染色
         * （`(228,228,228,255)`、`(166,166,166,255)`…），所以不能只取 alpha。
         */
        val tint: Int,
        /** 真样本 0 命中，先带着不实现。 */
        val additive: Boolean,
        /**
         * 这个 op 属于哪一层 —— 求值这条时间线时它所在**顶层槽位**的 index。
         *
         * 嵌套精灵展开出来的 op 记的是**父槽位**，不是子精灵自己的槽位（见 [expand]）：
         * 界面上关掉一个图层要连它整棵子树一起关，否则关了个寂寞。
         *
         * 界面按它过滤显隐（见 `PamPreviewScreen` 的图层面板）。
         */
        val layer: Int = 0,
    )

    /** 可预览的一条时间线，供界面列出来让用户选。 */
    data class SpriteRef(val index: Int, val name: String, val frameCount: Int)

    /** 图层直接画一张图，还是嵌套一条子时间线（后者关掉时会连整棵子树一起关）。 */
    enum class LayerKind { Image, Sprite }

    /**
     * 一条时间线上出现过的**图层**（= 槽位），供界面列出来勾选显隐。
     *
     * 取的是整条时间线的**并集**而不是当前帧的：槽位随帧 append / remove，
     * 只看当前帧的话列表会在播放时不停抖动，也没法提前关掉一个还没出现的图层。
     *
     * `index` 就是槽位下标，同时**也是绘制序**（小的在下面）。
     * 实测 144274 个槽位里，同一个 index 换过内容的有 **0 处** —— 所以一个 index
     * 自始至终是同一个东西，[name] 取首次出现时的名字即可，不存在"中途改名"。
     */
    data class LayerRef(
        val index: Int,
        val name: String,
        val kind: LayerKind,
        /** 该槽位存活的帧范围，含两端。 */
        val firstFrame: Int,
        val lastFrame: Int,
    )

    /**
     * 一段帧标签：从某个带 label 的帧起，到下一个带 label 的帧的**前一帧**。
     *
     * label 是 PAM 给帧段起的名字（AKEE 主时间线就是 `idle` / `idle2` / `attack` /
     * `plantfood` / `water`），实测 564/565 个 PAM 都有。
     * 第一个 label **之前**的帧不属于任何段，此时 [label] 为 null。
     */
    data class LabelSpan(val label: String?, val start: Int, val endInclusive: Int)

    /** PAM 坐标里的一个矩形范围（y 向下，与屏幕坐标系一致）。 */
    data class Bounds(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double) {
        val width: Double get() = maxX - minX
        val height: Double get() = maxY - minY
    }

    /**
     * 把一条时间线**所有帧**的绘制范围并起来，给预览取景用。
     *
     * 为什么要逐帧扫，而不是直接拿 `pam.size` 当画布：实测内容**经常超出声明尺寸** ——
     * `MAINMENU_BACKGROUND` 声明 2304×1536，可内容一直画到 x=3088、y=2438。只按声明尺寸取景
     * 就会把内容裁掉，实机上正是"动画一大半超出屏幕"。（声明尺寸本身也有坑，见
     * `PamBinaryReader.decode` 里 position 有符号 / size 无符号那段。）
     *
     * 逐帧调用 [evaluate] 是 O(帧数²)（每帧都要从第 0 帧重放），而真样本推翻了早先"最坏约
     * 100ms、付得起"的估算：**565 个样本的全部时间线扫一遍要 116.7 秒**，最坏的
     * `ZOMBIE_DINO_STEGOSAURUS` 单这一个调用就 6.1 秒 —— 实机上就是"载入非常久"。
     * 现已改走 [replay] 推进式重放，同一份数据降到毫秒级。
     *
     * @param includeDeclaredCanvas 是否把 `pam.size` 那块矩形也并进来。**主时间线给 true**：
     *   声明尺寸描述的就是它，一并显示才知道动画对不对得上画布。**子精灵给 false**：子精灵是
     *   部件库，`pam.size` 根本不是它的取景框，并进来只会让部件缩成一小团。
     * @return 一帧都画不出东西时返回 null
     */
    fun bounds(pam: PamInfo, spriteIndex: Int, includeDeclaredCanvas: Boolean): Bounds? {
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE

        // 各 image 声明尺寸的 w/h 预先摊平成一对 DoubleArray。逐条 op 去
        // `pam.image[i].size.getOrNull(0)?.toDouble()` 每取一次就装箱一个 `Int?`，而下面那圈
        // 是热点：最坏的样本（BACKGROUND_EIGHTIES_..._TOP）合计要折 540 万个四边形。
        //
        // **这一项的收益没有单独量出来**，别把它当成已验证的优化：本机 profiler 的噪声底
        // （同一份代码前后两遍的差）有 1.5 秒，比它的效应还大。留着是因为它同时把"越界下标"
        // 和"没记尺寸"两条跳过路径并成了一条，读起来更直白。
        val dims = DoubleArray(pam.image.size * 2)
        for (i in pam.image.indices) {
            val s = pam.image[i].size
            dims[i * 2] = s?.getOrNull(0)?.toDouble() ?: 0.0
            dims[i * 2 + 1] = s?.getOrNull(1)?.toDouble() ?: 0.0
        }

        // 精灵下标不存在时**不早退**：从前 frameCount 会给 0、循环空转，于是 includeDeclaredCanvas
        // 仍能把声明画布并进来。这里保持同样的行为。
        val sprite = spriteAt(pam, spriteIndex)
        if (sprite != null) {
            // 只取几何、不留帧，所以回调里就地折叠，不materialize 任何列表
            replay(pam, sprite, 0, HashMap()) { _, ops ->
                for (op in ops) {
                    // 越界下标与"没记尺寸"都归到 w/h <= 0 那一支，与从前的 `?: continue` 等价
                    val ii = op.imageIndex * 2
                    if (ii < 0 || ii + 1 >= dims.size) continue
                    val w = dims[ii]
                    val h = dims[ii + 1]
                    if (w <= 0.0 || h <= 0.0) continue
                    // 位图四角经仿射变换后的范围。写开是为了不在每个 op 上分配临时容器 ——
                    // 单帧最多 1447 个 op，逐帧扫下来这个循环很烫。
                    val x0 = op.tx
                    val y0 = op.ty
                    val x1 = op.m0 * w + op.tx
                    val y1 = op.m2 * w + op.ty
                    val x2 = op.m0 * w + op.m1 * h + op.tx
                    val y2 = op.m2 * w + op.m3 * h + op.ty
                    val x3 = op.m1 * h + op.tx
                    val y3 = op.m3 * h + op.ty
                    minX = minOf(minX, minOf(minOf(x0, x1), minOf(x2, x3)))
                    maxX = maxOf(maxX, maxOf(maxOf(x0, x1), maxOf(x2, x3)))
                    minY = minOf(minY, minOf(minOf(y0, y1), minOf(y2, y3)))
                    maxY = maxOf(maxY, maxOf(maxOf(y0, y1), maxOf(y2, y3)))
                }
            }
        }

        if (includeDeclaredCanvas) {
            val cw = pam.size.getOrNull(0) ?: 0.0
            val ch = pam.size.getOrNull(1) ?: 0.0
            if (cw > 0.0 && ch > 0.0) {
                minX = minOf(minX, 0.0)
                minY = minOf(minY, 0.0)
                maxX = maxOf(maxX, cw)
                maxY = maxOf(maxY, ch)
            }
        }

        if (minX > maxX || minY > maxY) return null
        return Bounds(minX, minY, maxX, maxY)
    }

    private class Slot(
        var image: Int? = null,
        var sprite: Int? = null,
        /** 被 append 时的帧号，嵌套精灵靠它算 elapsed。 */
        var firstFrame: Int = 0,
        var timescale: Double = 1.0,
        var preloadFrames: Int = 0,
        var tx: Double = 0.0,
        var ty: Double = 0.0,
        /** 2×2 线性部分，**行主序** `[m0 m1; m2 m3]`，与 [DrawOp] 同序（不是 `PamMove` 那个列主序）。 */
        var m0: Double = 1.0,
        var m1: Double = 0.0,
        var m2: Double = 0.0,
        var m3: Double = 1.0,
        var alpha: Int = 255,
        var tint: Int = NO_TINT,
        var additive: Boolean = false,
    )

    /**
     * 全部可预览的时间线：**main sprite 排第一**，其后才是 `PamInfo.sprite` 各条（没有 main 就不列）。
     *
     * 主时间线是绝大多数 PAM 唯一想看的那条（精灵表里那几条通常是它引用的零件，
     * 单独播出来没有意义），排第一才能让它成为默认选中项 —— 界面上的 `spriteSel` 初值是 0。
     */
    fun sprites(pam: PamInfo): List<SpriteRef> {
        val out = ArrayList<SpriteRef>()
        pam.mainSprite?.let {
            out += SpriteRef(MAIN_SPRITE, it.name.orEmpty().ifBlank { "主时间线" }, it.frame.size)
        }
        pam.sprite.forEachIndexed { i, s ->
            out += SpriteRef(i, s.name.orEmpty().ifBlank { "精灵 $i" }, s.frame.size)
        }
        return out
    }

    fun frameCount(pam: PamInfo, spriteIndex: Int): Int =
        spriteAt(pam, spriteIndex)?.frame?.size ?: 0

    /**
     * 这条时间线上出现过哪些图层，按槽位下标升序（= 绘制序，小的在下面）。
     *
     * 只走一遍帧、每帧快照一次还活着的槽位，所以比 [bounds] 那种逐帧重放的便宜得多。
     *
     * 名字的来源：`PamAdd.name` 看着最合适，但**实测 144274 条 append 里 0 条非空**，
     * 所以只能退回资源自己的名字 —— 图片取 `akee_52x24|IMAGE_PLANT_AKEE_...` 竖线前的
     * 那半段，嵌套精灵取 `PamSprite.name`。都不是给人看的名字，但足以区分。
     */
    fun layers(pam: PamInfo, spriteIndex: Int): List<LayerRef> {
        val sprite = spriteAt(pam, spriteIndex) ?: return emptyList()
        val live = sortedMapOf<Int, Slot>()
        val first = HashMap<Int, Int>()
        val last = HashMap<Int, Int>()
        val names = HashMap<Int, String>()
        val kinds = HashMap<Int, LayerKind>()

        for (i in sprite.frame.indices) {
            applyFrame(pam, sprite.frame[i], i, live)
            for ((index, slot) in live) {
                if (first[index] == null) {
                    first[index] = i
                    names[index] = layerName(pam, slot)
                    kinds[index] = if (slot.sprite != null) LayerKind.Sprite else LayerKind.Image
                }
                last[index] = i
            }
        }

        return first.keys.sorted().map { index ->
            LayerRef(
                index = index,
                name = names[index] ?: "图层 $index",
                kind = kinds[index] ?: LayerKind.Image,
                firstFrame = first.getValue(index),
                lastFrame = last.getValue(index),
            )
        }
    }

    private fun layerName(pam: PamInfo, slot: Slot): String {
        slot.sprite?.let { s ->
            val n = pam.sprite.getOrNull(s)?.name
            if (!n.isNullOrBlank()) return n
            return "精灵 $s"
        }
        val i = slot.image ?: return "槽位"
        val n = pam.image.getOrNull(i)?.name
        // `akee_52x24|IMAGE_PLANT_AKEE_AKEE_52X24` —— 竖线前是图集里的图名，后面的资源 ID 太长
        if (n.isNullOrBlank()) return "图片 $i"
        return n.substringBefore('|')
    }

    /**
     * 帧标签分段，见 [LabelSpan]。没有 label 的时间线返回空表。
     *
     * 纯扫描，不进 `applyFrame` —— label 是帧自己的属性，与槽位状态无关。
     */
    fun labels(pam: PamInfo, spriteIndex: Int): List<LabelSpan> {
        val frames = spriteAt(pam, spriteIndex)?.frame ?: return emptyList()
        if (frames.isEmpty()) return emptyList()

        val out = ArrayList<LabelSpan>()
        var label: String? = null
        var start = 0
        frames.forEachIndexed { i, f ->
            val next = f.label
            // 第 0 帧就带 label 时 i > start 不成立，正好不产生"开头那段无标签"的空段
            if (next != null) {
                if (i > start) out += LabelSpan(label, start, i - 1)
                label = next
                start = i
            }
        }
        out += LabelSpan(label, start, frames.size - 1)
        return out
    }

    /**
     * 把 [spriteIndex] 这条时间线推进到第 [frame] 帧，返回该帧的绘制列表。
     *
     * **必须从第 0 帧累积**：真样本 365,214 条 change 的 index **100% 命中**"到目前为止
     * 还活着的槽位"集合，0 例外 —— 若 index 是帧内局部下标，绝无可能出现这种结果。
     *
     * 返回顺序 = **槽位 index 升序**（`SexyFramework` 与 pam-viewer 都是这个绘制序），
     * 嵌套精灵就地深度优先展开。
     *
     * @param spriteIndex `PamInfo.sprite` 的下标；[MAIN_SPRITE] 表示主时间线
     * @throws IllegalArgumentException 精灵下标不存在，或帧里引用了越界的 image/sprite
     */
    fun evaluate(pam: PamInfo, spriteIndex: Int, frame: Int): List<DrawOp> {
        val sprite = spriteAt(pam, spriteIndex)
            ?: throw IllegalArgumentException("PAM 里没有下标为 $spriteIndex 的精灵")
        return evaluateSprite(pam, sprite, frame, 0, spriteIndex, HashMap())
    }

    /**
     * 把一条时间线的**每一帧**都算出来，按帧序返回；等价于
     * `(0 until frameCount).map { evaluate(pam, spriteIndex, it) }`，但**只重放一遍**。
     *
     * 为什么要单开这个入口：[evaluate] 的语义是"从第 0 帧重放到第 f 帧"（槽位状态是累积的），
     * 于是逐帧调用就是 O(帧数²)。实测 565 个真样本里最坏的一例 ——
     * `ZOMBIE_DINO_STEGOSAURUS` 的 1936 帧时间线 —— 桌面 JVM 上要 6.2 秒，全部样本合计
     * 117 秒；手机只会更慢，实机"载入要非常久"就是这里。推进式重放把同一份数据压到毫秒级。
     *
     * 结果与逐帧调用**完全相同**，有对拍测试守着（`PamTimelineTest`，以及真样本上的
     * `PamTimelineRealTest`）。返回的 List 会被内部缓存共享，调用方**不得修改**。
     *
     * @throws IllegalArgumentException 精灵下标不存在
     */
    fun evaluateAll(pam: PamInfo, spriteIndex: Int): List<List<DrawOp>> {
        val sprite = spriteAt(pam, spriteIndex)
            ?: throw IllegalArgumentException("PAM 里没有下标为 $spriteIndex 的精灵")
        val out = ArrayList<List<DrawOp>>(sprite.frame.size)
        replay(pam, sprite, 0, HashMap()) { _, ops -> out += ops }
        return out
    }

    /**
     * 逐帧推进一条时间线，每帧回调一次当帧的绘制指令。**整个求值链上唯一的重放实现**：
     * [evaluateAll] 与 [bounds] 都走它，于是"怎么重放"只有一处定义。
     *
     * 之所以是"推进"而不是每帧从第 0 帧重放，见 [evaluateAll]。
     *
     * **还没做的那一步**：这里每帧仍会把指令 materialize 成一个 `List<DrawOp>`（`bounds` 只要
     * 几何，根本不需要对象）。profiler 显示剩下的开销以**分配**为主而不是算术 —— 最坏的样本
     * （`BACKGROUND_EIGHTIES_..._TOP`，单帧上千条 op）在热堆上反而更慢。要再快就得让 `bounds`
     * 走一条标量 sink 的路（`emit(imageIndex, tx, ty, m0..m3, alpha, tint, additive)`），
     * 顺带把嵌套缓存里的子级指令也按标量存。**代价是求值链又多一条路径**，而当初 `bounds` 与
     * `evaluate` 两份实现分叉正是这一轮要修的毛病，所以没有确凿的设备数据前不要动。
     *
     * @param cache 嵌套精灵的帧结果缓存，**跨帧复用** —— 这是 [expand] 那层 O(帧数²) 的解药，
     *   见 [evaluateSprite]
     */
    private fun replay(
        pam: PamInfo,
        sprite: PamSprite,
        depth: Int,
        cache: MutableMap<Long, List<DrawOp>>,
        onFrame: (frame: Int, ops: List<DrawOp>) -> Unit,
    ) {
        // 槽位表在帧之间**累积**（applyFrame 一路叠上去），这正是"推进"能等价于"每帧重放"
        // 的原因：第 f 帧的状态就是重放 0..f 的结果。
        val slots = sortedMapOf<Int, Slot>()
        for (i in sprite.frame.indices) {
            applyFrame(pam, sprite.frame[i], i, slots)
            onFrame(i, slots.entries.flatMap { (index, slot) -> expand(pam, slot, i, depth, index, cache) })
        }
    }

    /**
     * @param spriteIndex 只用于 [frameKey] 组装缓存键
     * @param cache 见 [replay]。命中就不必从第 0 帧重放 —— 这是嵌套精灵那层 O(帧数²) 的解药：
     *   子时间线的帧号随父帧反复回到同一批值（`(elapsed × timescale + preload) % 子帧数`），
     *   不缓存的话父级每推一帧都要把子时间线整个重放一遍。
     */
    private fun evaluateSprite(
        pam: PamInfo,
        sprite: PamSprite,
        frame: Int,
        depth: Int,
        spriteIndex: Int,
        cache: MutableMap<Long, List<DrawOp>>,
    ): List<DrawOp> {
        val key = frameKey(spriteIndex, depth, frame)
        cache[key]?.let { return it }

        // 用有序表：绘制序就是 index 升序，而 LinkedHashMap 是插入序，两者不同
        val slots = sortedMapOf<Int, Slot>()
        val last = minOf(frame, sprite.frame.size - 1)
        for (i in 0..last) applyFrame(pam, sprite.frame[i], i, slots)
        // 用 entries 而不是 values：槽位下标要当图层号带下去。
        // （嵌套精灵自己也会走到这里，此时 index 是它的槽位号，但父级 expand 会把它换成
        //  父槽位号 —— 见 expand 里的 `layer = layer`。）
        val ops = slots.entries.flatMap { (index, slot) -> expand(pam, slot, frame, depth, index, cache) }
        cache[key] = ops
        return ops
    }

    /**
     * 缓存键：把 `(精灵下标, 层级, 帧号)` 打包进一个 Long，24/8/32 位分段。
     *
     *  * 精灵下标 **+1** 再取低 24 位 —— `+1` 是为了让 [MAIN_SPRITE] 的 `-1` 落到 0，不与任何
     *    真实下标撞车（真样本最多 7242 个精灵，24 位还富余三个数量级）；
     *  * 帧号取低 32 位 —— Int 本来就装得下，`and` 是为了挡住 [evaluate] 被传入负数帧的情形，
     *    否则符号位会一路溢出到高位去和精灵下标混起来；
     *  * **层级必须进键**：同一份子精灵理论上可以在不同层级被引用，而层级决定 [MAX_NEST_DEPTH]
     *    的截断，混用会拿到错的展开结果。
     */
    private fun frameKey(spriteIndex: Int, depth: Int, frame: Int): Long =
        (((spriteIndex + 1).toLong() and 0xFFFFFFL) shl 40) or
            ((depth.toLong() and 0xFFL) shl 32) or
            (frame.toLong() and 0xFFFFFFFFL)

    private fun spriteAt(pam: PamInfo, index: Int): PamSprite? =
        if (index == MAIN_SPRITE) pam.mainSprite else pam.sprite.getOrNull(index)

    private fun applyFrame(pam: PamInfo, frame: PamFrame, frameIndex: Int, slots: MutableMap<Int, Slot>) {
        for (r in frame.remove) slots.remove(r.index)

        for (a in frame.append) {
            val slot = Slot(
                firstFrame = frameIndex,
                timescale = a.timescale ?: 1.0,
                preloadFrames = a.preloadFrames,
                additive = a.additive,
            )
            if (a.sprite) {
                require(a.resource in pam.sprite.indices) {
                    "帧里的 append 指向 sprite[${a.resource}]，但这份 PAM 只有 ${pam.sprite.size} 个精灵"
                }
                slot.sprite = a.resource
            } else {
                require(a.resource in pam.image.indices) {
                    "帧里的 append 指向 image[${a.resource}]，但这份 PAM 只有 ${pam.image.size} 张图片"
                }
                slot.image = a.resource
            }
            slots[a.index] = slot
        }

        for (c in frame.change) {
            // 改一个没被 append 过的槽：真样本里不存在，忽略比报错稳
            val slot = slots[c.index] ?: continue
            applyMove(slot, c)
        }
    }

    /**
     * 应用一次 change。
     *
     * **按「设置」而非「累加」解释**：真样本 MAIN 有 100 帧却只有 1073 个 change，而槽位
     * 编号可达 81 —— 说明只有发生变化的槽会被写进帧里，没被写的保持上一帧的值。这正是
     * 关键帧语义。
     *
     * transform 的长度决定这次改哪几项：6 = 完整矩阵 + 平移，3 = 旋转 + 平移，
     * 2 = 只改平移（矩阵保持不变）。末两位恒为平移，由 `PamBinaryReader.readMove` 保证。
     */
    private fun applyMove(slot: Slot, move: PamMove) {
        val t = move.transform
        when (t.size) {
            6 -> {
                slot.m0 = t[0]; slot.m1 = t[2]; slot.m2 = t[1]; slot.m3 = t[3]
                slot.tx = t[4]; slot.ty = t[5]
            }

            3 -> {
                val r = t[0]
                slot.m0 = cos(r); slot.m1 = -sin(r); slot.m2 = sin(r); slot.m3 = cos(r)
                slot.tx = t[1]; slot.ty = t[2]
            }

            2 -> {
                slot.tx = t[0]; slot.ty = t[1]
            }

            else -> throw IllegalArgumentException(
                "change 的 transform 长度必须是 6（矩阵）/ 3（旋转）/ 2（平移），实际 ${t.size} 项",
            )
        }

        move.color?.let { c ->
            if (c.size >= 4) {
                slot.alpha = channel(c[3])
                slot.tint = (channel(c[0]) shl 16) or (channel(c[1]) shl 8) or channel(c[2])
            }
        }
    }

    private fun channel(v: Double): Int = (v * 255.0).roundToInt().coerceIn(0, 255)

    /**
     * 把一个槽位展开成图片层的绘制指令。
     *
     * 嵌套精灵的帧号随父级推进：`(floor(elapsed × timescale) + preload) % 子帧数`，
     * 其中 `elapsed = 当前帧 - 该层被 append 时的帧号`。真样本里 `timescale` 恒为 1、
     * `preload` 恒为 0，退化成 `elapsed % 子帧数`；但 7242 个精灵里有 722 个是多帧的，
     * 固定取第 0 帧会在这些身上画错。
     *
     * @param layer 该槽位在**顶层**时间线上的下标。嵌套展开时子精灵自己算出来的图层号
     *   会被这里覆盖成父槽位号，这样界面上关一个图层就是关掉它的整棵子树。
     */
    private fun expand(
        pam: PamInfo,
        slot: Slot,
        frame: Int,
        depth: Int,
        layer: Int,
        cache: MutableMap<Long, List<DrawOp>>,
    ): List<DrawOp> {
        slot.image?.let { return applyImageTransform(pam, slot, it, layer) }

        val subIndex = slot.sprite ?: return emptyList()
        if (depth >= MAX_NEST_DEPTH) return emptyList()
        val sub = pam.sprite.getOrNull(subIndex) ?: return emptyList()
        if (sub.frame.isEmpty()) return emptyList()

        val elapsed = (frame - slot.firstFrame).coerceAtLeast(0)
        val scaled = floor(elapsed * slot.timescale).toInt()
        val childFrame = (((scaled + slot.preloadFrames) % sub.frame.size) + sub.frame.size) % sub.frame.size

        return evaluateSprite(pam, sub, childFrame, depth + 1, subIndex, cache).map { op ->
            // 父级变换套在子级之上：p -> M_child·p + t_child -> M_parent·(…) + t_parent
            op.copy(
                m0 = slot.m0 * op.m0 + slot.m1 * op.m2,
                m1 = slot.m0 * op.m1 + slot.m1 * op.m3,
                m2 = slot.m2 * op.m0 + slot.m3 * op.m2,
                m3 = slot.m2 * op.m1 + slot.m3 * op.m3,
                tx = slot.m0 * op.tx + slot.m1 * op.ty + slot.tx,
                ty = slot.m2 * op.tx + slot.m3 * op.ty + slot.ty,
                alpha = slot.alpha * op.alpha / 255,
                tint = multiplyTint(slot.tint, op.tint),
                additive = slot.additive || op.additive,
                // 子精灵算出来的是它自己的槽位号，这里换成父槽位号
                layer = layer,
            )
        }
    }

    /**
     * 「最终绘制矩阵 = 帧变换 × 图片自己的 transform」—— 图片 transform 在最内层。
     *
     * 实测**不能自作主张按居中处理**：它是该图在 sprite 局部空间里的注册点。11239 条里
     * 只有 1 条接近居中、8 条全零，其余都是任意小偏移（440×229 的图 → `(-85.1, -28.85)`）。
     *
     * 单位也**和 move 不一样**：图片的 2×2 除以 1310720（= 20 × 65536），move 的是 65536。
     * `PamBinaryReader` 已分别换算好，此处直接相乘即可。
     */
    private fun applyImageTransform(
        pam: PamInfo,
        slot: Slot,
        imageIndex: Int,
        layer: Int,
    ): List<DrawOp> {
        // 模型声明是"恒 6 项"，但 JSON 是用户可改的，长度不对时退回单位阵而不是崩
        val img = pam.image.getOrNull(imageIndex)?.transform?.takeIf { it.size >= 6 } ?: IDENTITY
        // 同样先列主序 → 行主序（与 `applyMove` 的六项分支一个道理）。平移那两项不受影响。
        // 真样本里 image 矩阵全是纯等比对角阵 `diag(s,s)`，转置看不出差别 —— 所以这里错了
        // 也不会被真样本测试发现，只能靠手写非对角阵的用例守。
        val n00 = img[0]
        val n01 = img[2]
        val n10 = img[1]
        val n11 = img[3]
        return listOf(
            DrawOp(
                imageIndex = imageIndex,
                tx = slot.m0 * img[4] + slot.m1 * img[5] + slot.tx,
                ty = slot.m2 * img[4] + slot.m3 * img[5] + slot.ty,
                m0 = slot.m0 * n00 + slot.m1 * n10,
                m1 = slot.m0 * n01 + slot.m1 * n11,
                m2 = slot.m2 * n00 + slot.m3 * n10,
                m3 = slot.m2 * n01 + slot.m3 * n11,
                alpha = slot.alpha,
                tint = slot.tint,
                additive = slot.additive,
                layer = layer,
            ),
        )
    }

    /** 逐通道乘算，`0xFFFFFF` 是单位元。 */
    private fun multiplyTint(a: Int, b: Int): Int {
        if (a == NO_TINT) return b
        if (b == NO_TINT) return a
        val r = ((a shr 16 and 0xFF) * (b shr 16 and 0xFF)) / 255
        val g = ((a shr 8 and 0xFF) * (b shr 8 and 0xFF)) / 255
        val bl = ((a and 0xFF) * (b and 0xFF)) / 255
        return (r shl 16) or (g shl 8) or bl
    }
}
