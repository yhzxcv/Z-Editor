package com.example.z_editor.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

enum class PlantCategory(val label: String) {
    Quality("按品质"),
    Role("按作用"),
    Attribute("按属性"),
    Other("其他分类")
}

enum class PlantTag(val label: String, val iconName: String?, val category: PlantCategory) {
    All("全部植物", null, PlantCategory.Quality),

    // --- 按品质 (Quality) ---
    White("白色品质", "Plant_White.png", PlantCategory.Quality),
    Green("绿色品质", "Plant_Green.png", PlantCategory.Quality),
    Blue("蓝色品质", "Plant_Blue.png", PlantCategory.Quality),
    Purple("紫色品质", "Plant_Purple.png", PlantCategory.Quality),
    Orange("橙色品质", "Plant_Orange.png", PlantCategory.Quality),

    // --- 按作用 (Role) ---
    Assist("辅助植物", "Plant_Assist.png", PlantCategory.Role),
    Remote("远程植物", "Plant_Remote.png", PlantCategory.Role),
    Productor("生产植物", "Plant_Productor.png", PlantCategory.Role),
    Defence("坚韧植物", "Plant_Defence.png", PlantCategory.Role),
    Vanguard("先锋植物", "Plant_Vanguard.png", PlantCategory.Role),
    Trapper("奇兵植物", "Plant_Trapper.png", PlantCategory.Role),

    // --- 按属性 (Attribute) ---
    Fire("火焰属性", "Plant_Fire.png", PlantCategory.Attribute),
    Ice("冰冻属性", "Plant_Ice.png", PlantCategory.Attribute),
    Magic("魔法属性", "Plant_Magic.png", PlantCategory.Attribute),
    Poison("毒液属性", "Plant_Poison.png", PlantCategory.Attribute),
    Electric("雷电属性", "Plant_Electric.png", PlantCategory.Attribute),
    Physics("物理属性", "Plant_Physics.png", PlantCategory.Attribute),

    // --- 其他植物 (Other) ---
    Original("一代植物", null, PlantCategory.Other),
    Parallel("平行世界", null, PlantCategory.Other),
}


data class PlantInfo(
    val id: String,
    val name: String,
    val tags: List<PlantTag>,
    val icon: String?
)

private data class RawPlantData(
    val id: String,
    val name: String,
    val tags: List<String>?,
    val icon: String?
)

object PlantRepository {
    private var allPlants = listOf<PlantInfo>()
    private var isLoaded = false

    /**
     * 初始化：从 assets 读取 JSON
     */
    fun init(context: Context) {
        if (isLoaded) return

        try {
            val inputStream = context.assets.open("resources/Plants.json")
            val reader = InputStreamReader(inputStream)

            val gson = Gson()
            val listType = object : TypeToken<List<RawPlantData>>() {}.type
            val rawList: List<RawPlantData> = gson.fromJson(reader, listType)

            allPlants = rawList.map { raw ->
                PlantInfo(
                    id = raw.id,
                    name = raw.name,
                    icon = raw.icon,
                    tags = raw.tags?.mapNotNull { tagStr ->
                        PlantTag.entries.find { it.name.equals(tagStr, ignoreCase = true) }
                    } ?: listOf(PlantTag.All)
                )
            }

            isLoaded = true
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
            allPlants = listOf(PlantInfo("error", "数据加载失败", listOf(PlantTag.All), null))
        }
    }

    /**
     * 混合搜索
     */
    fun search(query: String, tag: PlantTag): List<PlantInfo> {
        if (!isLoaded) return emptyList()
        val tagFiltered = if (tag == PlantTag.All) {
            allPlants
        } else {
            allPlants.filter { it.tags.contains(tag) }
        }
        if (query.isBlank()) return tagFiltered
        val lowerQ = query.lowercase()
        return tagFiltered.filter {
            it.id.lowercase().contains(lowerQ) || it.name.contains(lowerQ)
        }
    }

    fun getPlantInfoById(id: String): PlantInfo? {
        return allPlants.find { it.id.equals(id, ignoreCase = true) }
    }

    fun getName(id: String): String {
        return allPlants.find { it.id == id }?.name ?: id
    }
}