package com.example.pvz2leveleditor.data

import com.google.gson.Gson

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
}