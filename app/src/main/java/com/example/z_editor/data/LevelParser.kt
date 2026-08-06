package com.example.z_editor.data

import com.google.gson.Gson
import com.google.gson.JsonElement

data class ParsedLevelData(
    val levelDef: LevelDefinitionData?,
    val waveManager: WaveManagerData?,
    val waveModule: WaveManagerModuleData?,
    val objectMap: Map<String, PvzObject>
)

object LevelParser {
    private val gson = Gson()

    fun parseLevel(levelFile: PvzLevelFile): ParsedLevelData {
        val objectMap = levelFile.objects.associateBy { it.aliases?.firstOrNull() ?: "unknown" }

        val levelDefObj = levelFile.objects.find { it.objClass == "LevelDefinition" }
        val levelDefData = if (levelDefObj != null) {
            gson.fromJson(levelDefObj.objData, LevelDefinitionData::class.java)
        } else null

        val waveModObj = levelFile.objects.find { it.objClass == "WaveManagerModuleProperties" }
        val waveModData =
            waveModObj?.let { gson.fromJson(it.objData, WaveManagerModuleData::class.java) }

        val waveMgrObj = levelFile.objects.find { it.objClass == "WaveManagerProperties" }
        val waveMgrData = if (waveMgrObj != null) {
            gson.fromJson(waveMgrObj.objData, WaveManagerData::class.java)
        } else null

        return ParsedLevelData(levelDefData, waveMgrData, waveModData, objectMap)
    }

    fun extractAlias(rtid: String): String {
        return rtid.substringAfter("(").substringBefore("@")
    }

    /**
     * 找出所有"孤立/未引用"的 object（失效模块）：
     * 从 LevelDefinition（根）出发，沿所有 object 的 objData 里出现的 RTID(...) 引用做 BFS。
     * 任何不被 RTID 链引用的 object 即为失效模块，返回待清除列表。
     * 文件里没有 LevelDefinition（无根）时返回空（不清除任何对象）。
     *
     * 引用格式统一为 `RTID(别名@来源)`（自定义对象/波次事件，来源多为 CurrentLevel）。
     * 解析时取 `RTID(...)` 内、`@` 前的部分作为别名；为容错同时支持无 `@` 的 `RTID(别名)`
     * （实际文件不应出现，防御性保留以防误删）。
     */
    fun findOrphanedObjects(levelFile: PvzLevelFile): List<PvzObject> {
        if (levelFile.objects.none { it.objClass == "LevelDefinition" }) return emptyList()
        val reachable = computeReachableObjects(levelFile)
        return levelFile.objects.filter { it !in reachable }
    }

    /**
     * 返回从 LevelDefinition（根）经 RTID 链可达的所有 object。
     * 成员判定按**引用同一性**（IdentityHashMap 语义）：结构相等但实例不同的对象不算同一个，
     * 避免 data class 结构相等导致的误判。无根时返回空集。
     *
     * 供"顺藤摸瓜"级联删除使用：删除某个模块前后各算一次可达集合，差集即为
     * 因该次删除而失去所有引用的内联子对象（挑战任务实体、波次容器/事件等）。
     */
    fun computeReachableObjects(levelFile: PvzLevelFile): Set<PvzObject> {
        val objects = levelFile.objects
        val rootIndex = objects.indexOfFirst { it.objClass == "LevelDefinition" }
        if (rootIndex < 0) return emptySet()
        // 别名 → 对象索引；同一别名多个对象时后者覆盖（与编辑器 objectMap 语义一致）
        val aliasToIndex = HashMap<String, Int>()
        objects.forEachIndexed { i, o ->
            for (a in o.aliases ?: emptyList()) aliasToIndex[a] = i
        }
        val reachable = java.util.IdentityHashMap<PvzObject, Boolean>()
        val queue = ArrayDeque<Int>()
        reachable[objects[rootIndex]] = true
        queue.add(rootIndex)
        val rtidRegex = Regex("""RTID\(([^()]+)\)""")
        while (queue.isNotEmpty()) {
            val i = queue.removeFirst()
            for (alias in collectRtidAliases(objects[i].objData, rtidRegex)) {
                val ti = aliasToIndex[alias] ?: continue
                val target = objects[ti]
                if (reachable[target] == null) {
                    reachable[target] = true
                    queue.add(ti)
                }
            }
        }
        return reachable.keys
    }

    /**
     * 找出关卡定义 Modules 列表中"失效"的模块引用：
     * 指向 `@CurrentLevel`（模块对象应存在于本文件）却在本文件里找不到对应 object 的 RTID 语句
     * （悬空引用）。这类模块引用不到任何实际内容，游戏加载时该模块不会生效，属于失效模块。
     * 返回失效的 RTID 字符串列表（保持 Modules 中的顺序，已去重）；无 LevelDefinition 或
     * Modules 数组缺失/损坏时返回空。`@LevelModules` 来源（内置于游戏的定义）不参与判断。
     */
    fun findInvalidLevelModuleReferences(levelFile: PvzLevelFile): List<String> {
        val levelDefObj = levelFile.objects.find { it.objClass == "LevelDefinition" }
            ?: return emptyList()
        val modules = try {
            val json = levelDefObj.objData
            if (json.isJsonObject && json.asJsonObject.has("Modules")) {
                json.asJsonObject.getAsJsonArray("Modules")
                    .mapNotNull { e ->
                        e.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
                    }
            } else emptyList()
        } catch (_: Exception) {
            return emptyList()
        }
        if (modules.isEmpty()) return emptyList()
        val fileAliases = levelFile.objects.flatMap { it.aliases ?: emptyList() }.toHashSet()
        return modules.filter { rtid ->
            val info = RtidParser.parse(rtid)
            info != null && info.source == "CurrentLevel" && info.alias !in fileAliases
        }.distinct()
    }

    /** 递归遍历 objData（JsonElement），收集所有字符串里 RTID(...) 的别名（@ 前部分）。 */
    private fun collectRtidAliases(
        json: JsonElement,
        regex: Regex,
        out: MutableList<String> = mutableListOf()
    ): List<String> {
        when {
            json.isJsonPrimitive -> {
                val prim = json.asJsonPrimitive
                if (prim.isString) {
                    for (m in regex.findAll(prim.asString)) {
                        out.add(m.groupValues[1].substringBefore("@"))
                    }
                }
            }
            json.isJsonArray -> for (e in json.asJsonArray) collectRtidAliases(e, regex, out)
            json.isJsonObject -> for (entry in json.asJsonObject.entrySet()) {
                collectRtidAliases(entry.value, regex, out)
            }
        }
        return out
    }
}