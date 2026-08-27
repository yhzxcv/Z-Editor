package com.example.z_editor.data

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject

data class ParsedLevelData(
    val levelDef: LevelDefinitionData?,
    val waveManager: WaveManagerData?,
    val waveModule: WaveManagerModuleData?,
    val objectMap: Map<String, PvzObject>
)

object LevelParser {
    private val gson = Gson()

    /**
     * 剔除 objects 里的 null 元素并容忍 objects 整体为 null。
     * Gson 对数组多余逗号宽容（[a,,b]→[a,null,b]、[a,b,]→[a,b,null]），会解析出 null 元素；
     * 关卡对象不可能为 null，统一剔除防下游 NPE。返回无 null 的新列表，不改原列表。
     */
    fun sanitizeObjectList(objects: List<PvzObject?>?): List<PvzObject> =
        objects?.filterNotNull() ?: emptyList()

    /**
     * 递归剔除 JsonElement 树中数组里的 null 元素，并深清 objData 嵌套数组（如 "Waves":[1,2,] → [1,2,null]）。
     * 多余逗号只在**数组**里产生 null 元素；对象值 "k":null 需显式写出、不会由此产生，予以保留
     * （可能是有意为之）。返回重建的新树，不改原树。
     */
    fun sanitizeJsonElement(json: JsonElement?): JsonElement {
        if (json == null || json.isJsonNull) return json ?: JsonNull.INSTANCE
        if (json.isJsonArray) {
            val out = JsonArray()
            for (e in json.asJsonArray) {
                if (e.isJsonNull) continue
                out.add(sanitizeJsonElement(e))
            }
            return out
        }
        if (json.isJsonObject) {
            val out = JsonObject()
            for ((k, v) in json.asJsonObject.entrySet()) {
                out.add(k, sanitizeJsonElement(v))
            }
            return out
        }
        return json
    }

    /**
     * 深清关卡对象：剔除 objects 列表里的 null 元素，并对每个对象的 objData 递归剔除数组中的 null 元素
     * （多余逗号在 objData 嵌套数组里也会留 null，仅清列表清不干净，落盘与下游解析仍会炸）。
     * 就地重建 objData（同一 PvzObject 实例，加载端 retainAll 就地剔除才能生效），返回无 null 的新列表。
     */
    fun sanitizeLevelObjects(objects: List<PvzObject?>?): List<PvzObject> {
        val clean = objects?.filterNotNull() ?: return emptyList()
        for (obj in clean) {
            obj.objData = sanitizeJsonElement(obj.objData)
        }
        return clean
    }

    fun parseLevel(levelFile: PvzLevelFile): ParsedLevelData {
        val objects = sanitizeObjectList(levelFile.objects)
        val objectMap = objects.associateBy { it.aliases?.firstOrNull() ?: "unknown" }

        val levelDefObj = objects.find { it.objClass == "LevelDefinition" }
        // objData 结构损坏（如字段类型错配、objdata 是数字/数组而非对象）时 Gson 抛 JsonSyntaxException；
        // 容错返回 null 让编辑器降级显示而非闪退。
        val levelDefData = if (levelDefObj != null) {
            try {
                gson.fromJson(levelDefObj.objData, LevelDefinitionData::class.java)
            } catch (_: Exception) {
                null
            }
        } else null

        val waveModObj = objects.find { it.objClass == "WaveManagerModuleProperties" }
        val waveModData =
            try {
                waveModObj?.let { gson.fromJson(it.objData, WaveManagerModuleData::class.java) }
            } catch (_: Exception) {
                null
            }

        val waveMgrObj = objects.find { it.objClass == "WaveManagerProperties" }
        val waveMgrData = if (waveMgrObj != null) {
            try {
                gson.fromJson(waveMgrObj.objData, WaveManagerData::class.java)
            } catch (_: Exception) {
                null
            }
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
        val objects = sanitizeObjectList(levelFile.objects)
        if (objects.none { it.objClass == "LevelDefinition" }) return emptyList()
        val reachable = computeReachableObjects(levelFile)
        return objects.filter { it !in reachable }
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
        val objects = sanitizeObjectList(levelFile.objects)
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
     * Modules 数组缺失/损坏时返回空。`@CurrentLevel` 引用关卡文件中不存在对应对象的别名，
     * 或 `@LevelModules` 引用参考文件 LevelModules.json 中不存在的别名，均判定为失效。
     * `levelModuleAliases` 传 null（参考文件未加载）时 `@LevelModules` 不参与判断，避免误报。
     */
    fun findInvalidLevelModuleReferences(
        levelFile: PvzLevelFile,
        levelModuleAliases: Set<String>? = null
    ): List<String> {
        val objects = sanitizeObjectList(levelFile.objects)
        val levelDefObj = objects.find { it.objClass == "LevelDefinition" }
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
        val fileAliases = objects.flatMap { it.aliases ?: emptyList() }.toHashSet()
        return modules.filter { rtid ->
            val info = RtidParser.parse(rtid) ?: return@filter false
            when (info.source) {
                "CurrentLevel" -> info.alias !in fileAliases
                // 参考文件未加载（null）时视为有效，无法校验不误报
                "LevelModules" -> levelModuleAliases != null && info.alias !in levelModuleAliases
                else -> false
            }
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