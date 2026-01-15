package com.example.z_editor.data.repository

import android.content.Context
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.PvzObject
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.ZombiePropertySheetData
import com.example.z_editor.data.ZombieStats
import com.example.z_editor.data.ZombieTypeData
import com.google.gson.Gson
import com.google.gson.JsonElement
import java.io.InputStreamReader

object ZombiePropertiesRepository {
    private val gson = Gson()

    private val statsCache = mutableMapOf<String, ZombieStats>()
    private val aliasToTypeCache = mutableMapOf<String, String>()

    private val originalTypeJsonCache = mutableMapOf<String, Pair<String, JsonElement>>()
    private val originalPropsJsonCache = mutableMapOf<String, Pair<String, JsonElement>>()

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return

        try {
            val propsFileMap = loadReferenceFile(context, "reference/PropertySheets.json")
            val typesFileMap = loadReferenceFile(context, "reference/ZombieTypes.json")

            typesFileMap.forEach { (alias, typeObj) ->
                try {
                    val typeData = gson.fromJson(typeObj.objData, ZombieTypeData::class.java)
                    val typeName = typeData.typeName

                    if (typeName.isNotBlank()) {
                        aliasToTypeCache[alias] = typeName
                        aliasToTypeCache[typeName] = typeName
                        originalTypeJsonCache[typeName] = typeObj.objClass to typeObj.objData

                        val propsAlias = RtidParser.parse(typeData.properties)?.alias ?: ""
                        val propsObj = propsFileMap[propsAlias]
                        if (propsObj != null) {
                            val sheet = gson.fromJson(propsObj.objData, ZombiePropertySheetData::class.java)
                            originalPropsJsonCache[typeName] = propsObj.objClass to propsObj.objData

                            val stats = ZombieStats(
                                id = typeName,
                                hp = sheet.hitpoints,
                                cost = sheet.wavePointCost,
                                weight = sheet.weight,
                                speed = sheet.speed,
                                eatDPS = sheet.eatDPS,
                                sizeType = sheet.sizeType.toString()
                            )
                            statsCache[typeName] = stats
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadReferenceFile(context: Context, path: String): Map<String, PvzObject> {
        return try {
            val inputStream = context.assets.open(path)
            val root = gson.fromJson(InputStreamReader(inputStream), PvzLevelFile::class.java)
            root.objects.associateBy { it.aliases?.firstOrNull() ?: "unknown" }
        } catch (e: Exception) {
            println("Error loading $path: ${e.message}")
            emptyMap()
        }
    }

    fun getTypeNameByAlias(alias: String): String = aliasToTypeCache[alias] ?: alias

    fun getStats(typeName: String): ZombieStats =
        statsCache[typeName] ?: ZombieStats(typeName, 0.0, 0, 0, 0.0, 0.0, "unknown")

    fun isValidAlias(alias: String): Boolean {
        return aliasToTypeCache.containsKey(alias)
    }

    fun getTemplateJson(typeName: String): Pair<Pair<String, JsonElement>, Pair<String, JsonElement>>? {
        val typeData = originalTypeJsonCache[typeName]
        val propsData = originalPropsJsonCache[typeName]

        if (typeData != null && propsData != null) {
            return typeData to propsData
        }
        return null
    }
}