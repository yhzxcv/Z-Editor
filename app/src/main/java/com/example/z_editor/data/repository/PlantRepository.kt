package com.example.z_editor.data.repository

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

enum class PlantCategory(val label: String) {
    Quality("按品质"),
    Role("按作用"),
    Attribute("按属性"),
    Other("其它分类"),
    Collection("我的收藏"),
}

enum class PlantTag(val label: String, val iconName: String?, val category: PlantCategory) {
    All("全部植物", null, PlantCategory.Quality),

    // --- 按品质 (Quality) ---
    White("白色品质", "Plant_White.webp", PlantCategory.Quality),
    Green("绿色品质", "Plant_Green.webp", PlantCategory.Quality),
    Blue("蓝色品质", "Plant_Blue.webp", PlantCategory.Quality),
    Purple("紫色品质", "Plant_Purple.webp", PlantCategory.Quality),
    Orange("橙色品质", "Plant_Orange.webp", PlantCategory.Quality),

    // --- 按作用 (Role) ---
    Assist("辅助植物", "Plant_Assist.webp", PlantCategory.Role),
    Remote("远程植物", "Plant_Remote.webp", PlantCategory.Role),
    Productor("生产植物", "Plant_Productor.webp", PlantCategory.Role),
    Defence("坚韧植物", "Plant_Defence.webp", PlantCategory.Role),
    Vanguard("先锋植物", "Plant_Vanguard.webp", PlantCategory.Role),
    Trapper("奇兵植物", "Plant_Trapper.webp", PlantCategory.Role),

    // --- 按属性 (Attribute) ---
    Fire("火焰属性", "Plant_Fire.webp", PlantCategory.Attribute),
    Ice("冰冻属性", "Plant_Ice.webp", PlantCategory.Attribute),
    Magic("魔法属性", "Plant_Magic.webp", PlantCategory.Attribute),
    Poison("毒液属性", "Plant_Poison.webp", PlantCategory.Attribute),
    Electric("雷电属性", "Plant_Electric.webp", PlantCategory.Attribute),
    Physics("物理属性", "Plant_Physics.webp", PlantCategory.Attribute),

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

    val favoriteIds = mutableStateListOf<String>()
    private const val PREF_NAME = "z_editor_prefs"
    private const val KEY_FAVORITES = "favorite_plants"

    /**
     * 初始化：从 assets 读取 JSON，并加载收藏
     */
    fun init(context: Context) {
        loadFavorites(context)
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

    private fun loadFavorites(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedSet = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        favoriteIds.clear()
        favoriteIds.addAll(savedSet)
    }

    fun toggleFavorite(context: Context, plantId: String) {
        if (favoriteIds.contains(plantId)) {
            favoriteIds.remove(plantId)
        } else {
            favoriteIds.add(plantId)
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putStringSet(KEY_FAVORITES, favoriteIds.toSet()) }
    }

    fun isFavorite(plantId: String): Boolean {
        return favoriteIds.contains(plantId)
    }

    /**
     * 混合搜索
     */
    fun search(query: String, tag: PlantTag?, category: PlantCategory): List<PlantInfo> {
        if (!isLoaded) return emptyList()

        val baseList = if (category == PlantCategory.Collection) {
            allPlants.filter { favoriteIds.contains(it.id) }
        } else {
            if (tag != null && tag != PlantTag.All) {
                allPlants.filter { it.tags.contains(tag) }
            } else {
                allPlants
            }
        }

        if (query.isBlank()) return baseList
        val lowerQ = query.lowercase()
        return baseList.filter {
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