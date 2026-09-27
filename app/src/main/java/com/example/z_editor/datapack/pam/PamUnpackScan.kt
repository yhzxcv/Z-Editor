package com.example.z_editor.datapack.pam

import com.example.z_editor.datapack.smf.AtlasSplitter
import java.io.File

object PamUnpackScan {

    const val MAX_DEPTH = 24

    const val MAX_ANCESTOR_HOPS = 8

    const val MAX_LISTED_PAMS = 500

    data class Entry(val relativePath: String, val file: File)

    sealed interface ListResult {
        data class Ok(val entries: List<Entry>, val total: Int) : ListResult

        data class Failed(val message: String) : ListResult
    }

    data class Layout(
        val root: File,
        val pam: File,
        val atlasDir: File?,
        val rtonFile: File?,
        val notes: List<String>,
    ) {
        val complete: Boolean get() = atlasDir != null && rtonFile != null
    }

    fun listPams(root: File, limit: Int = MAX_LISTED_PAMS): ListResult {
        if (!root.isDirectory) {
            return ListResult.Failed("解包产物目录不存在或不是目录：${root.absolutePath}")
        }
        // listFiles() 为 null 表示读不出（没权限），与"空目录"（空数组）是两回事。
        if (root.listFiles() == null) {
            return ListResult.Failed("读不出这个目录，请确认已授予「所有文件访问权限」：${root.absolutePath}")
        }

        val found = ArrayList<File>()
        root.walkTopDown()
            .maxDepth(MAX_DEPTH)
            .onEnter { it == root || !it.name.startsWith(".") }
            .forEach { if (it.isFile && it.name.endsWith(PAM_EXT, ignoreCase = true)) found += it }

        val entries = found
            .map { Entry(relativePath(it, root), it) }
            .sortedBy { it.relativePath }

        return ListResult.Ok(entries.take(limit), total = entries.size)
    }

    fun locate(root: File, pam: File): Layout {
        val notes = ArrayList<String>()

        val unpackRoot = findUnpackRoot(pam, root)
        if (unpackRoot != root) {
            notes += "从这份 PAM 向上找到了解包根：${unpackRoot.absolutePath}"
        }

        val atlasDir = findAtlasDir(unpackRoot)
        if (atlasDir == null) {
            notes += "没找到装着图集文件的「ATLASES」目录（解包产物根下应该有这一层）"
        }

        val ids = try {
            resourceIdsOf(PamBinaryReader.decode(pam.readBytes()))
        } catch (ex: Exception) {
            notes += "这份 PAM 读不出来（${ex.message ?: ex.javaClass.simpleName}），RTON 只能按文件名挑"
            emptySet()
        }

        val (rton, note) = pickRton(rtonCandidates(unpackRoot), ids, groupOf(pam, unpackRoot))
        if (note != null) notes += note

        return Layout(unpackRoot, pam, atlasDir, rton, notes)
    }

    internal fun resourceIdsOf(pam: PamInfo): Set<String> =
        pam.image.mapNotNull { PamAssets.resourceIdOf(it.name)?.lowercase() }.toSet()

    internal fun findUnpackRoot(pam: File, fallback: File): File {
        var dir = pam.parentFile
        var withRton: File? = null
        var withAtlas: File? = null
        var hops = 0
        while (dir != null && hops < MAX_ANCESTOR_HOPS) {
            // 用 atlasHere（只查本层与"ATLASES 下一层"）而不是 findAtlasDir：
            // 后者带一次 maxDepth=3 的递归兜底，放进逐层循环会在深层目录上白扫很多遍。
            val hasAtlas = atlasHere(dir) != null
            val hasRton = rtonCandidates(dir).isNotEmpty()
            if (hasAtlas && hasRton) return dir
            if (withRton == null && hasRton) withRton = dir
            if (withAtlas == null && hasAtlas) withAtlas = dir
            dir = dir.parentFile
            hops++
        }
        return withRton ?: withAtlas ?: fallback
    }

    internal fun findAtlasDir(root: File): File? =
        atlasHere(root)
            ?: root.walkTopDown()
                .maxDepth(3)
                .onEnter { !it.name.startsWith(".") }
                .firstOrNull { it.isDirectory && it.name.equals(ATLASES, ignoreCase = true) && hasAtlasFiles(it) }

    internal fun rtonCandidates(root: File): List<File> {
        val all = childDirNamed(root, PROPERTIES)
            ?.listFiles()
            ?.filter { it.isFile && it.name.endsWith(RTON_EXT, ignoreCase = true) }
            .orEmpty()
        val resources = all.filter { it.name.uppercase().startsWith(RESOURCES_PREFIX) }
        return (resources.ifEmpty { all }).sortedBy { it.name.lowercase() }
    }

    internal fun scanHits(bytes: ByteArray, ids: Set<String>): Int {
        if (ids.isEmpty()) return 0
        if (!AtlasSplitter.isPlainRton(bytes)) return -1
        val text = String(bytes, Charsets.ISO_8859_1)
        val upper = ids.mapTo(HashSet(ids.size)) { it.uppercase() }

        val minLen = ids.minOf { it.length }.coerceAtLeast(MIN_TOKEN_LEN)
        var hits = 0
        for (m in Regex("[A-Za-z0-9_]{$minLen,}").findAll(text)) {
            val v = m.value
            if (v in ids || v in upper) hits++
        }
        return hits
    }

    internal fun pickRton(
        candidates: List<File>,
        ids: Set<String>,
        group: String?,
    ): Pair<File?, String?> {
        if (candidates.isEmpty()) return null to "「$PROPERTIES」目录里没有 .RTON 文件"
        if (candidates.size == 1) return candidates[0] to null

        val wanted = group?.let { "$RESOURCES_PREFIX${it.uppercase()}" }
        val ranked = candidates.sortedWith(
            compareByDescending<File> { wanted != null && it.name.uppercase().startsWith(wanted) }
                .thenByDescending { it.name.uppercase().startsWith(RESOURCES_PREFIX) }
                .thenBy { it.name.lowercase() }
        )

        if (ids.isEmpty()) {
            return ranked[0] to "这份 PAM 不含图片，RTON 按文件名挑了 ${ranked[0].name}"
        }

        var best = ranked[0]
        var bestHits = Int.MIN_VALUE
        for (f in ranked) {
            val bytes = runCatching { f.readBytes() }.getOrNull() ?: continue
            val hits = scanHits(bytes, ids)
            if (hits > bestHits) {
                best = f
                bestHits = hits
            }
            // 满分不可能被超越，剩下的候选不必再读
            if (bestHits == ids.size) break
        }

        val note = if (bestHits <= 0) {
            "有 ${candidates.size} 份候选 RTON，都与这份 PAM 的资源 id 对不上，按文件名挑了 ${best.name}"
        } else {
            "有 ${candidates.size} 份候选 RTON，挑了 ${best.name}（命中 $bestHits/${ids.size} 个资源 id）"
        }
        return best to note
    }

    private const val ATLASES = "ATLASES"
    private const val PROPERTIES = "PROPERTIES"
    private const val RESOURCES_PREFIX = "RESOURCES"
    private const val PAM_EXT = ".pam"
    private const val RTON_EXT = ".rton"

    private const val MIN_TOKEN_LEN = 3

    private fun atlasHere(dir: File): File? {
        if (dir.name.equals(ATLASES, ignoreCase = true) && hasAtlasFiles(dir)) return dir
        val atlas = childDirNamed(dir, ATLASES) ?: return null
        if (hasAtlasFiles(atlas)) return atlas
        // 产物把图集按档位分了子目录时，真正装着文件的是下一层
        return atlas.listFiles()?.firstOrNull { it.isDirectory && hasAtlasFiles(it) }
    }

    private fun hasAtlasFiles(dir: File): Boolean = AtlasSplitter.scanAtlasDir(dir).isNotEmpty()

    private fun childDirNamed(dir: File, name: String): File? =
        dir.listFiles()?.firstOrNull { it.isDirectory && it.name.equals(name, ignoreCase = true) }

    private fun relativePath(file: File, root: File): String {
        val base = root.absolutePath.trimEnd(File.separatorChar)
        return file.absolutePath.removePrefix(base)
            .trimStart(File.separatorChar)
            .replace(File.separatorChar, '/')
    }

    /** 相对路径的各段。 */
    private fun relativeSegments(file: File, root: File): List<String> =
        relativePath(file, root).split('/').filter { it.isNotEmpty() }

    /**
     * PAM 在哪个包组下：相对路径里**第一个纯数字段**（档位）的下一段。
     *
     * `IMAGES/1200/PLANT1/PLANT/AKEE/AKEE.PAM` → `PLANT1`。
     * 认不出档位就返回 null（只影响排序提示，不影响正确性）。
     */
    internal fun groupOf(pam: File, root: File): String? {
        val segs = relativeSegments(pam, root)
        for (i in segs.indices) {
            if (segs[i].all { it.isDigit() }) return segs.getOrNull(i + 1)
        }
        return null
    }
}
