package com.example.z_editor.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader


enum class ZombieCategory(val label: String) {
    Main("按世界"),
    Size("按体型"),
    Other("其他分类")
}

enum class ZombieTag(val label: String, val iconName: String?, val category: ZombieCategory) {
    All("全部僵尸", null, ZombieCategory.Main),

    // --- 主线世界 ---
    Egypt_Pirate("埃及/海盗", null, ZombieCategory.Main),
    West_Future("西部/未来", null, ZombieCategory.Main),
    Dark_Beach("黑暗/沙滩", null, ZombieCategory.Main),
    Iceage_Lostcity("冰河/失落", null, ZombieCategory.Main),
    Kongfu_Skycity("功夫/天空", null, ZombieCategory.Main),
    Eighties_Dino("摇滚/恐龙", null, ZombieCategory.Main),
    Modern_Pvz1("摩登/一代", null, ZombieCategory.Main),
    Steam_Renai("蒸汽/复兴", null, ZombieCategory.Main),
    Henai_Atlantis("平安/海底", null, ZombieCategory.Main),
    Tale_ZCorp("童话/Z公司", null, ZombieCategory.Main),
    Parkour_Speed("跑酷/飞车", null, ZombieCategory.Main),
    Memory("回忆之旅", null, ZombieCategory.Main),
    Universe("平行宇宙", null, ZombieCategory.Main),
    Tothewest("西游记", null, ZombieCategory.Main),
    Festival1("节日串烧1", null, ZombieCategory.Main),
    Festival2("节日串烧2", null, ZombieCategory.Main),
    Roman("罗马世界", null, ZombieCategory.Main),

    // --- 僵尸体型 ---
    Pet("僵尸宠物", "Zombie_Pet.png", ZombieCategory.Size),
    Imp("小鬼僵尸", "Zombie_Imp.png", ZombieCategory.Size),
    Basic("普通僵尸", "Zombie_Basic.png", ZombieCategory.Size),
    Fat("肥胖僵尸", "Zombie_Fat.png", ZombieCategory.Size),
    Strong("壮汉僵尸", "Zombie_Strong.png", ZombieCategory.Size),
    Gargantuar("巨人僵尸", "Zombie_Gargantuar.png", ZombieCategory.Size),
    Elite("精英僵尸", "Zombie_Elite.png", ZombieCategory.Size),

    // --- 其他 ---
    Evildave("适配iz", null, ZombieCategory.Other),
}

data class ZombieInfo(
    val id: String,
    val name: String,
    val tags: List<ZombieTag>,
    val icon: String?
)

private data class RawZombieData(
    val id: String,
    val name: String,
    val tags: List<String>?,
    val icon: String?
)

object ZombieRepository {
    private var allZombies = listOf<ZombieInfo>()
    private var isLoaded = false

    private val uiConfiguredAliases = HashSet<String>()

    fun init(context: Context) {
        if (isLoaded) return
        try {
            val inputStream = context.assets.open("resources/Zombies.json")
            val reader = InputStreamReader(inputStream)
            val gson = Gson()
            val listType = object : TypeToken<List<RawZombieData>>() {}.type
            val rawList: List<RawZombieData> = gson.fromJson(reader, listType)

            allZombies = rawList.map { raw ->
                uiConfiguredAliases.add(raw.id)
                ZombieInfo(
                    id = raw.id,
                    name = raw.name,
                    icon = raw.icon,
                    tags = raw.tags?.mapNotNull { tagStr ->
                        ZombieTag.entries.find { it.name.equals(tagStr, ignoreCase = true) }
                    } ?: listOf(ZombieTag.All)
                )
            }
            isLoaded = true
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
            allZombies = listOf(ZombieInfo("error", "加载失败", listOf(ZombieTag.All), null))
        }
    }

    fun search(query: String, tag: ZombieTag): List<ZombieInfo> {
        if (!isLoaded) return emptyList()
        val tagFiltered = if (tag == ZombieTag.All) {
            allZombies
        } else {
            allZombies.filter { it.tags.contains(tag) }
        }
        if (query.isBlank()) return tagFiltered
        val lowerQ = query.lowercase()
        return tagFiltered.filter {
            it.id.lowercase().contains(lowerQ) || it.name.contains(lowerQ)
        }
    }

    fun getName(id: String): String {
        val uiName = allZombies.find { it.id == id }?.name
        if (uiName != null) return uiName
        return id
    }

    fun getZombieInfoById(id: String): ZombieInfo? {
        return allZombies.find { it.id.equals(id, ignoreCase = true) }
    }

    fun isElite(id: String): Boolean {
        val zombie = allZombies.find { it.id == id } ?: return false
        return zombie.tags.contains(ZombieTag.Elite)
    }

    fun isValid(id: String): Boolean {
        if (uiConfiguredAliases.contains(id)) return true
        return ZombiePropertiesRepository.isValidAlias(id)
    }

    fun buildAliases(id: String): String {
        return if (id.contains("iceage_fat_weasel")) "iceage_fat_weasel_elite"
        else id
    }

    fun resolveZombieType(rtid: String, objectMap: Map<String, com.example.z_editor.data.PvzObject>? = null): Pair<String, Boolean> {
        val parsed = com.example.z_editor.data.RtidParser.parse(rtid) ?: return Pair("", false)
        val alias = parsed.alias
        if (parsed.source == "ZombieTypes") {
            val typeName = ZombiePropertiesRepository.getTypeNameByAlias(alias)
            val isValid = uiConfiguredAliases.contains(typeName) || ZombiePropertiesRepository.isValidAlias(alias)
            return Pair(typeName, isValid) as Pair<String, Boolean>
        }

        if (parsed.source == "CurrentLevel" && objectMap != null) {
            val customZombieObj = objectMap[alias]
            if (customZombieObj != null) {
                try {
                    val gson = Gson()
                    if (customZombieObj.objClass == "ZombieType") {
                        val data = gson.fromJson(customZombieObj.objData, com.example.z_editor.data.ZombieTypeData::class.java)
                        val baseType = data.typeName
                        val isValidBase = uiConfiguredAliases.contains(baseType) || ZombiePropertiesRepository.isValidAlias(baseType)
                        return Pair(baseType, isValidBase)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return Pair(alias, true)
            } else {
                return Pair(alias, false)
            }
        }
        return Pair(alias, false)
    }
}