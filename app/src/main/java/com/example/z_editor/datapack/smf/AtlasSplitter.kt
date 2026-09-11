package com.example.z_editor.datapack.smf

import com.example.z_editor.datapack.rton.RtonParser
import java.io.File

/**
 * 图集拆分（纯 Kotlin / JVM 可测，无 Android 依赖）。
 *
 * 解包产物里 `PROPERTIES/RESOURCES*.RTON` 记录了每张图片在图集上的矩形，
 * `ATLASES/` 下的 `.PTX` 是图集本体（ASTC 5x5/6x6），本类负责把两者对上并拆出独立图片。
 *
 * 用「图片」而不是「精灵」：这些资源在 RTON 里的类型就是 `Image`，绝大多数是 UI 图、
 * 背景、图标，不是会动的精灵。类型名与 RTON 保持一致（[Image] ↔ `type: "Image"`）。
 *
 * 配方链条：
 * ```
 * Image 资源 --(parent)--> 图集资源 id --(path.last())--> ATLASES/<NAME>
 *     --> 解码 --> 裁 (ax, ay, aw, ah) --> 独立图片
 * ```
 *
 * 已在 dark 包（929/929）与 dynamic 包（867/867）上全量验证。
 *
 * 本类只管「算」：解析 RTON 得到 [Plan]、反推块尺寸、裁剪像素。解码与落盘在
 * `AtlasSplitRunner`（那边才碰 Android）。
 */
object AtlasSplitter {

    /** 图集上的一块矩形 + 它在游戏资源树里的原路径。 */
    data class Image(
        val id: String,
        val path: List<String>,
        val parentId: String,
        val ax: Int,
        val ay: Int,
        val aw: Int,
        val ah: Int,
    )

    /** 一张图集。`name` 取自 RTON 的 `path.last()`，与 `ATLASES/` 下的文件名对应（不区分大小写）。 */
    data class Atlas(
        val id: String,
        val name: String,
        val width: Int,
        val height: Int,
    )

    /**
     * 拆分计划：把 RTON 里的资源摊平、去重、按图集归好组。
     *
     * @param atlases  图集 id -> 图集
     * @param byAtlas  图集 id -> 挂在这张图集下的图片（[orphans] 已排除）
     * @param orphans  `parent` 指向的 id 不是图集的图片——正常包应为空
     */
    data class Plan(
        val atlases: Map<String, Atlas>,
        val byAtlas: Map<String, List<Image>>,
        val orphans: List<Image>,
    ) {
        val imageCount: Int get() = byAtlas.values.sumOf { it.size }
    }

    // ---- RTON ----

    /** RTON 明文 magic。加密的 RTON 前 4 字节不是它，需先过 `RtonConverter.decryptRtonBytes`。 */
    private val MAGIC = byteArrayOf('R'.code.toByte(), 'T'.code.toByte(), 'O'.code.toByte(), 'N'.code.toByte())

    /**
     * 是否为明文 RTON（而非加密 RTON 或别的文件）。
     *
     * 必须在调 [RtonParser.parse] 之前判：那边对非 RTON magic **静默返回空 map**
     * （见 `RtonParser.parse`），不判就会把「加密 RTON」表现成「解析出 0 张图片」的假成功。
     */
    fun isPlainRton(bytes: ByteArray): Boolean {
        if (bytes.size < MAGIC.size) return false
        for (i in MAGIC.indices) if (bytes[i] != MAGIC[i]) return false
        return true
    }

    /**
     * 解析 RTON 字节为拆分计划。
     *
     * @throws IllegalArgumentException 不是明文 RTON（调用方应先解密）
     */
    fun plan(rtonBytes: ByteArray): Plan {
        require(isPlainRton(rtonBytes)) {
            val head = rtonBytes.take(4).joinToString(" ") { "%02X".format(it) }
            "不是明文 RTON（前 4 字节 $head）：可能已加密或不是 RTON 文件"
        }
        return planFromRoot(RtonParser.parse(rtonBytes))
    }

    /**
     * 从已解析的 RTON 根对象建计划。
     *
     * 与 [plan] 拆开是为了可测：测试用 map 字面量直接构造输入，
     * 就能精确命中「跟进 subgroups / 按 id 去重 / 区分图集与图片」这三处最易错的逻辑，
     * 无需在仓库里入库任何游戏数据。
     */
    internal fun planFromRoot(root: Map<String, Any?>): Plan {
        // ---- 1. 摊平 resources，按 id 去重 ----
        // groups[].subgroups[] 是「对别的 group 的引用」，目标 group 本身也在顶层 groups 里；
        // 仍显式跟进（带 visited 防环），以免将来出现嵌套布局时漏收。
        val groups = root["groups"] as? List<*> ?: emptyList<Any?>()
        val groupById = HashMap<String, Map<*, *>>()
        for (g in groups) {
            val gm = g as? Map<*, *> ?: continue
            val gid = gm["id"] as? String ?: continue
            groupById[gid] = gm
        }

        // 同一资源在 RTON 里会被重复引用约 2 次（dark 2002→1002，dynamic 9342→8421），
        // 必须去重，否则图片数翻倍。
        val flat = LinkedHashMap<String, Map<*, *>>()
        val visitedGroups = HashSet<String>()

        fun collect(gm: Map<*, *>) {
            for (r in gm["resources"] as? List<*> ?: emptyList<Any?>()) {
                val rm = r as? Map<*, *> ?: continue
                val rid = rm["id"] as? String ?: continue
                flat.putIfAbsent(rid, rm)
            }
            for (sg in gm["subgroups"] as? List<*> ?: emptyList<Any?>()) {
                val sm = sg as? Map<*, *> ?: continue
                val sid = sm["id"] as? String ?: continue
                if (!visitedGroups.add(sid)) continue
                groupById[sid]?.let { collect(it) }
            }
        }

        for (g in groups) {
            val gm = g as? Map<*, *> ?: continue
            val gid = gm["id"] as? String
            if (gid != null && !visitedGroups.add(gid)) continue
            collect(gm)
        }

        // ---- 2. 分类 ----
        // 图片 = 带 ax 的条目；图集 = atlas 为真的条目（不带 ax）。
        // 不靠 `type` 区分——图集条目的 type 也是 Image。
        val images = ArrayList<Image>()
        val atlases = LinkedHashMap<String, Atlas>()

        for ((id, r) in flat) {
            val ax = r.intOrNull("ax")
            if (ax != null) {
                images += Image(
                    id = id,
                    path = r.stringList("path"),
                    parentId = r["parent"] as? String ?: "",
                    ax = ax,
                    ay = r.intOrNull("ay") ?: 0,
                    aw = r.intOrNull("aw") ?: 0,
                    ah = r.intOrNull("ah") ?: 0,
                )
            } else if (r["atlas"] == true) {
                val path = r.stringList("path")
                atlases[id] = Atlas(
                    id = id,
                    name = path.lastOrNull() ?: id,
                    width = r.intOrNull("width") ?: 0,
                    height = r.intOrNull("height") ?: 0,
                )
            }
        }

        // ---- 3. 按图集归组 ----
        val byAtlas = LinkedHashMap<String, MutableList<Image>>()
        val orphans = ArrayList<Image>()
        for (s in images) {
            if (atlases.containsKey(s.parentId)) {
                byAtlas.getOrPut(s.parentId) { ArrayList() } += s
            } else {
                orphans += s
            }
        }

        return Plan(atlases = atlases, byAtlas = byAtlas, orphans = orphans)
    }

    // ---- 图集文件 ----

    /** 候选 ASTC 块尺寸，按尝试顺序。本包实测只用这两种。 */
    private val ASTC_CANDIDATES = listOf(5 to 5, 6 to 6)

    /**
     * 扫描图集目录：小写文件名（无扩展名）-> 文件。
     *
     * 同时收 `.ptx` 与 `.png`——`SmfUnpacker` 的 `convertPtxToPng` 会把 `ATLASES/` 下的每张 `.PTX`
     * 也转成 `.png`，两个开关可能同时开。**同名时 `.png` 优先**：PNG 头自带宽高，
     * 免去反推 ASTC 块尺寸，也绕开 1×1 纹理两种块尺寸算出同样字节数的歧义。
     */
    fun scanAtlasDir(dir: File): Map<String, File> {
        val out = HashMap<String, File>()
        for (f in dir.listFiles() ?: return out) {
            if (!f.isFile) continue
            val ext = f.extension.lowercase()
            if (ext != "ptx" && ext != "png") continue
            val key = f.nameWithoutExtension.lowercase()
            val cur = out[key]
            if (cur == null || (ext == "png" && cur.extension.lowercase() != "png")) out[key] = f
        }
        return out
    }

    /**
     * 从 (RTON 宽, RTON 高, 文件大小) 反推 ASTC 块尺寸。
     *
     * `ATLASES/` 下的 `.PTX` **没有 PTX 头**，前 32 字节就是图像数据，解包后 `PTX_INFO` 表也已丢失，
     * 所以只能这样反推：`ceil(w/bw) * ceil(h/bh) * 16 == fileSize`。
     *
     * @return `(bw, bh)`，两种都不匹配时返回 null（说明不是 ASTC 或尺寸对不上）
     */
    fun inferAstcBlock(pxW: Int, pxH: Int, fileSize: Long): Pair<Int, Int>? {
        if (pxW <= 0 || pxH <= 0 || fileSize <= 0) return null
        for ((bw, bh) in ASTC_CANDIDATES) {
            val need = ((pxW + bw - 1) / bw).toLong() * ((pxH + bh - 1) / bh) * 16
            if (need == fileSize) return bw to bh
        }
        return null
    }

    // ---- 裁剪 ----

    /** 矩形是否落在图集内且非退化。用于拆分前的体检，避免跑到一半才失败。 */
    fun isRectInBounds(s: Image, atlas: Atlas): Boolean =
        s.ax >= 0 && s.ay >= 0 && s.aw > 0 && s.ah > 0 &&
            s.ax + s.aw <= atlas.width && s.ay + s.ah <= atlas.height

    /** 1×1 占位图：dark 包有 28 张，均为全透明（已核实是真占位，不是裁错）。 */
    fun isPlaceholder(s: Image): Boolean = s.aw == 1 && s.ah == 1

    /**
     * 从整张图集的像素里裁出 [s] 的矩形。
     *
     * @param pixels 行主序、长度 `atlasW * atlasH`
     * @throws IllegalArgumentException 矩形越界或退化
     */
    fun crop(pixels: IntArray, atlasW: Int, s: Image): IntArray {
        require(atlasW > 0) { "图集宽度必须为正，实际 $atlasW" }
        require(pixels.size % atlasW == 0) { "像素数 ${pixels.size} 不是宽度 $atlasW 的整数倍" }
        require(s.aw > 0 && s.ah > 0) { "图片 ${s.id} 的尺寸非法: ${s.aw}x${s.ah}" }
        require(s.ax >= 0 && s.ay >= 0) { "图片 ${s.id} 的起点为负: (${s.ax}, ${s.ay})" }
        val atlasH = pixels.size / atlasW
        require(s.ax + s.aw <= atlasW && s.ay + s.ah <= atlasH) {
            "图片 ${s.id} 矩形越界: (${s.ax},${s.ay},${s.aw},${s.ah}) 超出 ${atlasW}x$atlasH"
        }
        val out = IntArray(s.aw * s.ah)
        for (row in 0 until s.ah) {
            System.arraycopy(pixels, (s.ay + row) * atlasW + s.ax, out, row * s.aw, s.aw)
        }
        return out
    }

    // ---- 解析辅助 ----

    /**
     * 按 [Number] 读再收窄。
     *
     * 不能直接 `as Int`：`RtonParser` 的 varint/zigzag 解出来是 `Long`，
     * 8/16/32 位才是 `Int`，同一字段在不同包里可能是不同宽度。
     */
    private fun Map<*, *>.intOrNull(key: String): Int? = (this[key] as? Number)?.toInt()

    private fun Map<*, *>.stringList(key: String): List<String> =
        (this[key] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
}
