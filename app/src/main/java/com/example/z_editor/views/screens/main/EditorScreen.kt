package com.example.z_editor.views.screens.main

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.EditorSubScreen
import com.example.z_editor.data.LevelParser
import com.example.z_editor.data.ModuleMetadata
import com.example.z_editor.data.ModuleRegistry
import com.example.z_editor.data.ParsedLevelData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.PvzObject
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.StarChallengeModuleData
import com.example.z_editor.data.WaveManagerData
import com.example.z_editor.data.WaveManagerModuleData
import com.example.z_editor.data.repository.ChallengeTypeInfo
import com.example.z_editor.data.repository.LevelRepository
import com.example.z_editor.data.repository.PlantRepository
import com.example.z_editor.data.repository.ReferenceRepository
import com.example.z_editor.data.repository.ZombiePropertiesRepository
import com.example.z_editor.data.repository.ZombieRepository
import com.google.gson.Gson

private val gson = Gson()

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(fileName: String, fileUri: Uri?, onBack: () -> Unit) {
    val context = LocalContext.current

    // ======================== 状态管理 ========================
    var currentFileName by remember { mutableStateOf(fileName) }
    var rootLevelFile by remember { mutableStateOf<PvzLevelFile?>(null) }
    var parsedData by remember { mutableStateOf<ParsedLevelData?>(null) }

    var availableTabs by remember { mutableStateOf(listOf(EditorTabType.Settings)) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var currentSubScreen by remember { mutableStateOf<EditorSubScreen>(EditorSubScreen.None) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    var missingModules by remember { mutableStateOf<List<ModuleMetadata>>(emptyList()) }

    // 滚动状态保持
    val lazyListStates = remember { mutableMapOf<String, LazyListState>() }
    val scrollStates = remember { mutableMapOf<String, ScrollState>() }

    fun getLazyState(key: String) = lazyListStates.getOrPut(key) { LazyListState() }
    fun getScrollState(key: String) = scrollStates.getOrPut(key) { ScrollState(0) }

    // 选择器状态
    var previousSubScreen by remember { mutableStateOf<EditorSubScreen>(EditorSubScreen.None) }
    var genericSelectionCallback by remember { mutableStateOf<((Any) -> Unit)?>(null) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // ======================== 核心逻辑 ========================

    fun updateWaveManagerManualStartup(enable: Boolean) {
        if (parsedData == null || rootLevelFile == null) return
        val waveModObj =
            rootLevelFile!!.objects.find { it.objClass == "WaveManagerModuleProperties" } ?: return
        try {
            val modData = gson.fromJson(waveModObj.objData, WaveManagerModuleData::class.java)
            val targetValue = if (enable) true else null
            if (modData.manualStartup != targetValue) {
                modData.manualStartup = targetValue
                waveModObj.objData = gson.toJsonTree(modData)
                parsedData = parsedData!!.copy(waveModule = modData)
                refreshTrigger++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun recalculateLevelState() {
        if (rootLevelFile == null || parsedData == null) return

        val existingClasses = rootLevelFile!!.objects.map { it.objClass }.toSet()
            .plus(parsedData!!.levelDef?.modules?.map { rtid ->
                val alias = RtidParser.parse(rtid)?.alias ?: ""
                ReferenceRepository.getObjClass(alias) ?: alias
            } ?: emptyList())

        val newTabs = mutableListOf(EditorTabType.Settings)
        if (existingClasses.contains("WaveManagerModuleProperties")) newTabs.add(EditorTabType.Timeline)
        if (existingClasses.contains("EvilDaveProperties")) newTabs.add(EditorTabType.IZombie)
        if (existingClasses.contains("VaseBreakerPresetProperties")) newTabs.add(EditorTabType.VaseBreaker)
        if (existingClasses.contains("ZombossBattleModuleProperties")) newTabs.add(EditorTabType.BossFight)
        availableTabs = newTabs

        val isVaseBreaker = existingClasses.contains("VaseBreakerPresetProperties") ||
                existingClasses.contains("VaseBreakerArcadeModuleProperties") ||
                existingClasses.contains("VaseBreakerArcadeModuleProperties")
        val isZombossBattle = existingClasses.contains("ZombossBattleModuleProperties") ||
                existingClasses.contains("ZombossBattleIntroProperties")
        val isLastStand = existingClasses.contains("LastStandMinigameProperties")
        val isEvilDave = existingClasses.contains("EvilDaveProperties")

        val missingList = mutableListOf<String>()
        if (!existingClasses.contains("CustomLevelModuleProperties")) missingList.add("CustomLevelModuleProperties")
        if (!existingClasses.contains("ZombiesAteYourBrainsProperties")) {
            if (!isEvilDave) missingList.add("ZombiesAteYourBrainsProperties")
        }
        if (!existingClasses.contains("ZombiesDeadWinConProperties")) {
            if (!isEvilDave && !isZombossBattle) missingList.add("ZombiesDeadWinConProperties")
        }
        if (!existingClasses.contains("StandardLevelIntroProperties")) {
            if (!isVaseBreaker && !isLastStand && !isZombossBattle) missingList.add("StandardLevelIntroProperties")
        }
        if (isVaseBreaker) {
            if (!existingClasses.contains("VaseBreakerPresetProperties")) missingList.add("VaseBreakerPresetProperties")
            if (!existingClasses.contains("VaseBreakerArcadeModuleProperties")) missingList.add("VaseBreakerArcadeModuleProperties")
            if (!existingClasses.contains("VaseBreakerFlowModuleProperties")) missingList.add("VaseBreakerFlowModuleProperties")
        }
        if (isEvilDave) {
            if (!existingClasses.contains("InitialPlantEntryProperties")) missingList.add("InitialPlantEntryProperties")
            if (!existingClasses.contains("SeedBankProperties")) missingList.add("SeedBankProperties")
        }
        if (isZombossBattle) {
            if (!existingClasses.contains("ZombossBattleModuleProperties")) missingList.add("ZombossBattleModuleProperties")
            if (!existingClasses.contains("ZombossBattleIntroProperties")) missingList.add("ZombossBattleIntroProperties")
        }
        if (isLastStand) {
            if (!existingClasses.contains("SeedBankProperties")) missingList.add("SeedBankProperties")
        }

        missingModules = missingList.mapNotNull { objClass ->
            val meta = ModuleRegistry.getMetadata(objClass)
            if (meta.title == "未知模块" && objClass != "Unknown") null else meta
        }
    }

    fun injectCustomZombie(originalAlias: String): String? {
        val typeName = ZombiePropertiesRepository.getTypeNameByAlias(originalAlias)
        val template = ZombiePropertiesRepository.getTemplateJson(typeName)
        if (template == null) {
            Toast.makeText(context, "无法获取 $typeName 的原始数据模板", Toast.LENGTH_SHORT).show()
            return null
        }

        val (typeTemplate, propsTemplate) = template
        val (typeClass, typeJsonSource) = typeTemplate
        val (propsClass, propsJsonSource) = propsTemplate

        val baseName = typeName
        var index = 1
        while (rootLevelFile!!.objects.any { it.aliases?.contains("${baseName}_$index") == true }) {
            index++
        }
        val newTypeAlias = "${baseName}_$index"

        var propsIndex = index
        while (rootLevelFile!!.objects.any { it.aliases?.contains("${baseName}_props_$propsIndex") == true }) {
            propsIndex++
        }
        val newPropsAlias = "${baseName}_props_$propsIndex"

        val newPropsJson =
            gson.fromJson(gson.toJson(propsJsonSource), com.google.gson.JsonElement::class.java)

        val newPropsObj = PvzObject(
            aliases = listOf(newPropsAlias),
            objClass = propsClass,
            objData = newPropsJson
        )

        val newTypeJson =
            gson.fromJson(gson.toJson(typeJsonSource), com.google.gson.JsonObject::class.java)
        newTypeJson.addProperty("Properties", RtidParser.build(newPropsAlias, "CurrentLevel"))

        val newTypeObj = PvzObject(
            aliases = listOf(newTypeAlias),
            objClass = typeClass,
            objData = newTypeJson
        )

        rootLevelFile!!.objects.add(newPropsObj)
        rootLevelFile!!.objects.add(newTypeObj)

        val newObjectMap =
            rootLevelFile!!.objects.associateBy { it.aliases?.firstOrNull() ?: "unknown" }
        parsedData = parsedData!!.copy(objectMap = newObjectMap)
        refreshTrigger++

        Toast.makeText(context, "已注入自定义僵尸: $newTypeAlias", Toast.LENGTH_SHORT).show()
        return RtidParser.build(newTypeAlias, "CurrentLevel")
    }

    LaunchedEffect(currentFileName) {
        ReferenceRepository.init(context)
        ZombieRepository.init(context)
        ZombiePropertiesRepository.init(context)
        PlantRepository.init(context)
        val file = LevelRepository.loadLevel(context, currentFileName)
        if (file != null) {
            rootLevelFile = file
            parsedData = LevelParser.parseLevel(file)
            recalculateLevelState()
            selectedTabIndex = 0
        } else {
            Toast.makeText(context, "文件加载失败", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }
    LaunchedEffect(refreshTrigger) { recalculateLevelState() }

    // --- 业务操作 ---
    fun performSave(isExit: Boolean = false) {
        if (rootLevelFile == null || parsedData == null) return
        try {
            LevelRepository.saveAndExport(context, fileUri!!, currentFileName, rootLevelFile!!)
            val msg = if (isExit) "已自动保存并退出" else "保存成功"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "保存出错: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun handleExit() {
        performSave(isExit = true)
        onBack()
    }

    fun navigateBackToMain() {
        if (rootLevelFile != null && parsedData != null) {
            val newObjectMap =
                rootLevelFile!!.objects.associateBy { it.aliases?.firstOrNull() ?: "unknown" }
            val waveModObj =
                rootLevelFile!!.objects.find { it.objClass == "WaveManagerModuleProperties" }
            val waveMgrObj = rootLevelFile!!.objects.find { it.objClass == "WaveManagerProperties" }
            parsedData = parsedData!!.copy(
                objectMap = newObjectMap,
                waveModule = waveModObj?.let {
                    gson.fromJson(
                        it.objData,
                        WaveManagerModuleData::class.java
                    )
                },
                waveManager = waveMgrObj?.let {
                    gson.fromJson(
                        it.objData,
                        WaveManagerData::class.java
                    )
                }
            )
            refreshTrigger++
        }
        currentSubScreen = EditorSubScreen.None
    }

    // ======================== Action 定义 ========================
    val actions = remember(rootLevelFile, parsedData) {
        EditorActions(
            navigateTo = { currentSubScreen = it },
            navigateBack = { navigateBackToMain() },

            onRemoveModule = { rtid ->
                val info = RtidParser.parse(rtid)
                val alias = info?.alias ?: ""
                val objClass = if (info?.source == "CurrentLevel") {
                    parsedData!!.objectMap[alias]?.objClass
                } else {
                    ReferenceRepository.getObjClass(alias)
                }

                val removed = parsedData!!.levelDef!!.modules.remove(rtid)

                if (removed) {
                    val levelDefObj =
                        rootLevelFile!!.objects.find { it.objClass == "LevelDefinition" }
                    if (levelDefObj != null && levelDefObj.objData.isJsonObject) {
                        try {
                            val json = levelDefObj.objData.asJsonObject
                            if (json.has("Modules")) {
                                val modulesArray = json.getAsJsonArray("Modules")
                                val iter = modulesArray.iterator()
                                while (iter.hasNext()) {
                                    if (iter.next().asString == rtid) {
                                        iter.remove()
                                        break
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (objClass == "StarChallengeModuleProperties") {
                        val moduleObj =
                            rootLevelFile!!.objects.find { it.aliases?.contains(alias) == true }
                        if (moduleObj != null) {
                            try {
                                val challengeData = gson.fromJson(
                                    moduleObj.objData,
                                    StarChallengeModuleData::class.java
                                )
                                val allChallengeRtids = challengeData.challenges.flatten()
                                var deletedCount = 0
                                allChallengeRtids.forEach { challengeRtid ->
                                    val cInfo = RtidParser.parse(challengeRtid)
                                    if (cInfo?.source == "CurrentLevel") {
                                        if (rootLevelFile!!.objects.removeAll {
                                                it.aliases?.contains(
                                                    cInfo.alias
                                                ) == true
                                            }) {
                                            deletedCount++
                                        }
                                    }
                                }
                                if (deletedCount > 0) Toast.makeText(
                                    context,
                                    "移除了 $deletedCount 个关联挑战",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    if (info?.source == "CurrentLevel") {
                        val wasRemoved =
                            rootLevelFile!!.objects.removeAll { it.aliases?.contains(alias) == true }
                    }

                    if (objClass == "WaveManagerModuleProperties") {
                        parsedData = parsedData!!.copy(waveModule = null)
                    }
                    if (objClass == "LastStandMinigameProperties") {
                        updateWaveManagerManualStartup(false)
                    }

                    val newObjectMap = rootLevelFile!!.objects.associateBy {
                        it.aliases?.firstOrNull() ?: "unknown"
                    }

                    val currentLevelDef = parsedData!!.levelDef!!
                    val newLevelDef = currentLevelDef.copy(
                        modules = ArrayList(currentLevelDef.modules)
                    )
                    parsedData = parsedData!!.copy(
                        levelDef = newLevelDef,
                        objectMap = newObjectMap
                    )

                    refreshTrigger++
                    Toast.makeText(context, "已移除模块", Toast.LENGTH_SHORT).show()
                }
            },

            onAddModule = { meta ->
                val isDefaultExist =
                    rootLevelFile!!.objects.any { it.aliases?.contains(meta.defaultAlias) == true }
                if (!meta.allowMultiple && isDefaultExist) {
                    Toast.makeText(
                        context,
                        "${meta.title} 模块已存在，不可重复添加",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    var finalAlias = meta.defaultAlias
                    if (rootLevelFile!!.objects.any { it.aliases?.contains(finalAlias) == true }) {
                        var count = 0
                        val prefix = meta.defaultAlias
                        while (rootLevelFile!!.objects.any { it.aliases?.contains(finalAlias) == true }) {
                            count++
                            finalAlias = "$prefix$count"
                        }
                    }

                    val newRtid = RtidParser.build(finalAlias, meta.defaultSource)

                    if (meta.defaultSource == "CurrentLevel") {
                        if (meta.initialDataFactory != null) {
                            var initialData = meta.initialDataFactory.invoke()
                            if (initialData is WaveManagerModuleData) {
                                val actualWaveMgrAlias =
                                    rootLevelFile!!.objects.find { it.objClass == "WaveManagerProperties" }?.aliases?.firstOrNull()
                                        ?: "WaveManagerProps"
                                initialData = initialData.copy(
                                    waveManagerProps = RtidParser.build(
                                        actualWaveMgrAlias,
                                        "CurrentLevel"
                                    )
                                )
                            }
                            val newObj = PvzObject(
                                aliases = listOf(finalAlias),
                                objClass = ModuleRegistry.getAllKnownModules().entries.find { it.value == meta }?.key
                                    ?: "Unknown",
                                objData = gson.toJsonTree(initialData)
                            )
                            rootLevelFile!!.objects.add(newObj)

                            val newObjectMap = rootLevelFile!!.objects.associateBy {
                                it.aliases?.firstOrNull() ?: "unknown"
                            }
                            parsedData = parsedData!!.copy(objectMap = newObjectMap)
                        }
                    }

                    parsedData!!.levelDef?.modules?.add(newRtid)

                    val levelDefObj =
                        rootLevelFile!!.objects.find { it.objClass == "LevelDefinition" }
                    if (levelDefObj != null && levelDefObj.objData.isJsonObject) {
                        try {
                            val json = levelDefObj.objData.asJsonObject
                            if (!json.has("Modules")) {
                                json.add("Modules", com.google.gson.JsonArray())
                            }
                            val modulesArray = json.getAsJsonArray("Modules")
                            modulesArray.add(newRtid)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (meta.defaultAlias == "LastStandMinigame") updateWaveManagerManualStartup(
                        true
                    )

                    refreshTrigger++
                    Toast.makeText(context, "已添加 ${meta.title}", Toast.LENGTH_SHORT).show()
                    currentSubScreen = EditorSubScreen.None
                }
            },

            onAddEvent = { meta, waveIndex ->
                val waveEvents = parsedData!!.waveManager!!.waves[waveIndex - 1]
                val prefix = "Wave${waveIndex}${meta.defaultAlias}"
                var count = 0
                var newAlias = "$prefix$count"
                while (rootLevelFile!!.objects.any { it.aliases?.contains(newAlias) == true }) {
                    count++; newAlias = "$prefix$count"
                }
                val newRtid = RtidParser.build(newAlias, "CurrentLevel")
                val newObj = PvzObject(
                    aliases = listOf(newAlias),
                    objClass = meta.defaultObjClass,
                    objData = gson.toJsonTree(meta.initialDataFactory())
                )
                rootLevelFile!!.objects.add(newObj)
                waveEvents.add(newRtid)
                rootLevelFile!!.objects.find { it.objClass == "WaveManagerProperties" }
                    ?.let { it.objData = gson.toJsonTree(parsedData!!.waveManager) }
                val newObjectMap =
                    rootLevelFile!!.objects.associateBy { it.aliases?.firstOrNull() ?: "unknown" }
                parsedData = parsedData!!.copy(objectMap = newObjectMap)
                refreshTrigger++
                currentSubScreen = EditorSubScreen.None
            },

            onWavesChanged = {
                val currentWaveManager = parsedData!!.waveManager!!
                val newWaveManager = currentWaveManager.copy(
                    waves = ArrayList(currentWaveManager.waves)
                )
                rootLevelFile!!.objects.find { it.objClass == "WaveManagerProperties" }?.let {
                    it.objData = gson.toJsonTree(newWaveManager)
                }
                val newObjectMap =
                    rootLevelFile!!.objects.associateBy { it.aliases?.firstOrNull() ?: "unknown" }
                parsedData = parsedData!!.copy(
                    objectMap = newObjectMap,
                    waveManager = newWaveManager
                )
                refreshTrigger++
            },

            onLevelDefChanged = {
                val currentLevelDef = parsedData!!.levelDef!!
                rootLevelFile!!.objects.find { it.objClass == "LevelDefinition" }?.let {
                    it.objData = gson.toJsonTree(currentLevelDef)
                }
                parsedData = parsedData!!.copy(levelDef = currentLevelDef)
                refreshTrigger++
            },

            onDeleteEventReference = { rtid ->
                parsedData?.waveManager?.waves?.forEach { wave -> wave.removeAll { it == rtid } }
                rootLevelFile?.objects?.find { it.objClass == "WaveManagerProperties" }
                    ?.let { it.objData = gson.toJsonTree(parsedData!!.waveManager) }
                refreshTrigger++
                currentSubScreen = EditorSubScreen.None
            },

            onSaveWaveManager = {
                rootLevelFile!!.objects.find { it.objClass == "WaveManagerProperties" }
                    ?.let { it.objData = gson.toJsonTree(parsedData!!.waveManager) }
            },

            onCreateWaveContainer = {
                if (rootLevelFile != null) {
                    val defaultData = WaveManagerData(waves = mutableListOf(mutableListOf()))
                    var newAlias = "WaveManagerProps"
                    var count = 0
                    while (rootLevelFile!!.objects.any { it.aliases?.contains(newAlias) == true }) {
                        count++; newAlias = "WaveManagerProps$count"
                    }
                    val newObj = PvzObject(
                        aliases = listOf(newAlias),
                        objClass = "WaveManagerProperties",
                        objData = gson.toJsonTree(defaultData)
                    )
                    rootLevelFile!!.objects.add(newObj)
                    parsedData?.waveModule?.let { mod ->
                        mod.waveManagerProps = RtidParser.build(newAlias, "CurrentLevel")
                        rootLevelFile!!.objects.find { it.objClass == "WaveManagerModuleProperties" }
                            ?.let { it.objData = gson.toJsonTree(mod) }
                    }
                    val newObjectMap = rootLevelFile!!.objects.associateBy {
                        it.aliases?.firstOrNull() ?: "unknown"
                    }
                    parsedData = parsedData!!.copy(
                        objectMap = newObjectMap,
                        waveManager = defaultData,
                        waveModule = parsedData?.waveModule
                    )
                    refreshTrigger++
                    Toast.makeText(context, "已初始化波次容器 ($newAlias)", Toast.LENGTH_SHORT)
                        .show()
                }
            },

            onDeleteWaveContainer = {
                if (rootLevelFile != null) {
                    val removed =
                        rootLevelFile!!.objects.removeAll { it.objClass == "WaveManagerProperties" }
                    val modObj =
                        rootLevelFile!!.objects.find { it.objClass == "WaveManagerModuleProperties" }
                    if (modObj != null) {
                        try {
                            val modData =
                                gson.fromJson(modObj.objData, WaveManagerModuleData::class.java)
                            modData.waveManagerProps = ""
                            modObj.objData = gson.toJsonTree(modData)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (removed) {
                        Toast.makeText(context, "已删除波次容器", Toast.LENGTH_SHORT).show()
                        val newObjectMap = rootLevelFile!!.objects.associateBy {
                            it.aliases?.firstOrNull() ?: "unknown"
                        }
                        parsedData = parsedData!!.copy(
                            objectMap = newObjectMap,
                            waveManager = null,
                            waveModule = modObj?.let {
                                gson.fromJson(
                                    it.objData,
                                    WaveManagerModuleData::class.java
                                )
                            }
                                ?: parsedData!!.waveModule
                        )
                        refreshTrigger++
                    }
                }
            },

            onStageSelected = { newRtid ->
                val levelDefObj = rootLevelFile!!.objects.find { it.objClass == "LevelDefinition" }
                if (levelDefObj != null) {
                    try {
                        val json = if (levelDefObj.objData.isJsonObject)
                            levelDefObj.objData.asJsonObject
                        else
                            gson.toJsonTree(parsedData!!.levelDef).asJsonObject
                        json.addProperty("StageModule", newRtid)

                        levelDefObj.objData = json

                        parsedData?.levelDef?.stageModule = newRtid
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                currentSubScreen = EditorSubScreen.BasicInfo
                refreshTrigger++
            },

            onStageCanceled = {
                currentSubScreen = EditorSubScreen.BasicInfo
                refreshTrigger++
            },

            onAddChallenge = { info ->
                var alias = info.defaultAlias
                var count = 0
                while (rootLevelFile!!.objects.any { it.aliases?.contains(alias) == true }) {
                    count++; alias = "${info.defaultAlias}$count"
                }
                val newData = info.initialDataFactory()
                val newObj = PvzObject(
                    aliases = listOf(alias),
                    objClass = info.objClass,
                    objData = gson.toJsonTree(newData)
                )
                rootLevelFile!!.objects.add(newObj)
                val newObjectMap =
                    rootLevelFile!!.objects.associateBy { it.aliases?.firstOrNull() ?: "unknown" }
                parsedData = parsedData!!.copy(objectMap = newObjectMap)

                val challengeModRtid = parsedData!!.levelDef?.modules?.find { modRtid ->
                    val rtidInfo = RtidParser.parse(modRtid)
                    val modAlias = rtidInfo?.alias ?: ""
                    parsedData!!.objectMap[modAlias]?.objClass == "StarChallengeModuleProperties"
                }
                if (challengeModRtid != null) {
                    val challengeModInfo = RtidParser.parse(challengeModRtid)
                    val challengeModObj = parsedData!!.objectMap[challengeModInfo!!.alias]
                    if (challengeModObj != null) {
                        val modData = try {
                            gson.fromJson(
                                challengeModObj.objData,
                                StarChallengeModuleData::class.java
                            )
                        } catch (_: Exception) {
                            StarChallengeModuleData()
                        }
                        val newChallengeRtid = RtidParser.build(alias, "CurrentLevel")
                        if (modData.challenges.isEmpty()) modData.challenges.add(mutableListOf())
                        modData.challenges[0].add(newChallengeRtid)
                        challengeModObj.objData = gson.toJsonTree(modData)
                    }
                }
                refreshTrigger++
            },

            onToggleSunDropperMode = { enableCustom, currentData ->
                val levelDef = parsedData!!.levelDef!!
                val objects = rootLevelFile!!.objects

                val moduleIndex = levelDef.modules.indexOfFirst { rtid ->
                    val alias = RtidParser.parse(rtid)?.alias ?: ""
                    val objClass = parsedData!!.objectMap[alias]?.objClass
                        ?: ReferenceRepository.getObjClass(alias)
                    objClass == "SunDropperProperties"
                }
                val alias = "DefaultSunDropper"

                val targetRtid: String

                if (enableCustom) {
                    targetRtid = RtidParser.build(alias, "CurrentLevel")
                    if (moduleIndex != -1) levelDef.modules[moduleIndex] = targetRtid
                    else levelDef.modules.add(targetRtid)

                    val existingObj = objects.find { it.aliases?.contains(alias) == true }
                    if (existingObj == null) {
                        val newObj = PvzObject(
                            aliases = listOf(alias),
                            objClass = "SunDropperProperties",
                            objData = gson.toJsonTree(currentData)
                        )
                        objects.add(newObj)
                    }

                } else {
                    targetRtid = RtidParser.build(alias, "LevelModules")
                    if (moduleIndex != -1) levelDef.modules[moduleIndex] = targetRtid
                    objects.removeAll { it.aliases?.contains(alias) == true }
                }

                val newObjectMap = objects.associateBy { it.aliases?.firstOrNull() ?: "unknown" }
                parsedData = parsedData!!.copy(objectMap = newObjectMap)

                refreshTrigger++
                currentSubScreen = EditorSubScreen.SunDropper(targetRtid)
            },

            // --- 选择器逻辑 ---
            onLaunchMultiPlantSelector = { cb ->
                previousSubScreen = currentSubScreen
                genericSelectionCallback = { result -> cb(result as List<String>) }
                currentSubScreen = EditorSubScreen.PlantSelection(isMultiSelect = true)
            },

            onLaunchMultiZombieSelector = { cb ->
                previousSubScreen = currentSubScreen
                genericSelectionCallback = { result -> cb(result as List<String>) }
                currentSubScreen = EditorSubScreen.ZombieSelection(isMultiSelect = true)
            },
            onLaunchPlantSelector = { cb ->
                previousSubScreen = currentSubScreen
                genericSelectionCallback = { id -> cb(id as String) }
                currentSubScreen = EditorSubScreen.PlantSelection()
            },
            onLaunchZombieSelector = { cb ->
                previousSubScreen = currentSubScreen
                genericSelectionCallback = { id -> cb(id as String) }
                currentSubScreen = EditorSubScreen.ZombieSelection()
            },
            onLaunchGridItemSelector = { cb ->
                previousSubScreen = currentSubScreen; genericSelectionCallback =
                { id -> cb(id as String) }; currentSubScreen = EditorSubScreen.GridItemSelection
            },
            onLaunchChallengeSelector = { cb ->
                previousSubScreen = currentSubScreen; genericSelectionCallback =
                { info -> cb(info as ChallengeTypeInfo) }; currentSubScreen =
                EditorSubScreen.ChallengeSelection
            },
            onSelectorResult = { result ->
                genericSelectionCallback?.invoke(result)
                genericSelectionCallback = null
                currentSubScreen = previousSubScreen
                refreshTrigger++
            },
            onSelectorCancel = {
                genericSelectionCallback = null
                currentSubScreen = previousSubScreen
            },
            onChallengeSelected = { info ->
                (genericSelectionCallback as? ((ChallengeTypeInfo) -> Unit))?.invoke(info)
                genericSelectionCallback = null
                currentSubScreen = previousSubScreen
                refreshTrigger++
            },
            onLaunchToolSelector = { cb ->
                previousSubScreen = currentSubScreen
                genericSelectionCallback = { id -> cb(id as String) }
                currentSubScreen = EditorSubScreen.ToolSelection
            },
            onLaunchZombossSelector = { cb ->
                previousSubScreen = currentSubScreen
                genericSelectionCallback = { id -> cb(id as String) }
                currentSubScreen = EditorSubScreen.ZombossSelection
            },
            onInjectZombie = { alias -> injectCustomZombie(alias) },
            onEditCustomZombie = { rtid ->
                currentSubScreen = EditorSubScreen.CustomZombieProperties(rtid)
            }
        )
    }

    // ======================== UI 渲染 ========================

    BackHandler(enabled = currentSubScreen != EditorSubScreen.None) {
        currentSubScreen = EditorSubScreen.None
    }
    BackHandler(enabled = currentSubScreen == EditorSubScreen.None) { handleExit() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = currentSubScreen,
            label = "Nav",
            contentKey = { targetState -> targetState::class },
            transitionSpec = {
                if (initialState::class == targetState::class) {
                    androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                }
                if (targetState == EditorSubScreen.PlantSelection() || targetState == EditorSubScreen.ZombieSelection()
                    || targetState == EditorSubScreen.PlantSelection(isMultiSelect = true)
                    || targetState == EditorSubScreen.ZombieSelection(isMultiSelect = true)
                    || targetState is EditorSubScreen.CustomZombieProperties
                    || targetState == EditorSubScreen.StageSelection || targetState == EditorSubScreen.GridItemSelection
                    || targetState == EditorSubScreen.ChallengeSelection || targetState == EditorSubScreen.ToolSelection
                ) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width / 3 } + fadeOut())
                } else if (initialState == EditorSubScreen.PlantSelection() || initialState == EditorSubScreen.ZombieSelection()
                    || initialState == EditorSubScreen.PlantSelection(isMultiSelect = true)
                    || initialState == EditorSubScreen.ZombieSelection(isMultiSelect = true)
                    || initialState is EditorSubScreen.CustomZombieProperties
                    || initialState == EditorSubScreen.StageSelection || initialState == EditorSubScreen.GridItemSelection
                    || initialState == EditorSubScreen.ChallengeSelection || initialState == EditorSubScreen.ToolSelection
                ) {
                    (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut())
                } else if (targetState != EditorSubScreen.None && initialState == EditorSubScreen.None) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut())
                }
            }
        ) { targetState ->
            if (targetState == EditorSubScreen.None) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    currentFileName.substringBeforeLast("."),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { handleExit() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        "Back",
                                        tint = Color.White
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    performSave(isExit = false)
                                    currentSubScreen = EditorSubScreen.JsonView(currentFileName)
                                }) {
                                    Icon(Icons.Default.Code, "查看代码", tint = Color.White)
                                }
                                IconButton(onClick = { performSave(isExit = false) }) {
                                    Icon(
                                        Icons.Default.Save,
                                        "保存",
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF4CAF50), titleContentColor = Color.White
                            )
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        if (availableTabs.size > 1) {
                            ScrollableTabRow(
                                selectedTabIndex = selectedTabIndex,
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFF1976D2),
                                edgePadding = 0.dp,
                                indicator = { tabPositions ->
                                    val index = selectedTabIndex
                                    if (index < tabPositions.size) {
                                        SecondaryIndicator(
                                            Modifier.tabIndicatorOffset(tabPositions[index]),
                                            color = Color(0xFF1976D2),
                                            height = 3.dp
                                        )
                                    }
                                },
                            ) {
                                availableTabs.forEachIndexed { index, tabType ->
                                    Tab(
                                        modifier = if (availableTabs.size == 2) Modifier.width(
                                            screenWidth / 2
                                        )
                                        else Modifier.width(screenWidth / 3),
                                        selected = selectedTabIndex == index,
                                        onClick = { selectedTabIndex = index },
                                        text = { Text(tabType.title) },
                                        icon = { Icon(tabType.icon, null) }
                                    )
                                }
                            }
                        }

                        EditorContentRouter(
                            targetState = EditorSubScreen.None,
                            rootLevelFile = rootLevelFile,
                            parsedData = parsedData,
                            missingModules = missingModules,
                            currentTab = availableTabs.getOrElse(selectedTabIndex) { EditorTabType.Settings },
                            getLazyState = ::getLazyState,
                            getScrollState = ::getScrollState,
                            refreshTrigger = refreshTrigger,
                            actions = actions
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    EditorContentRouter(
                        targetState = targetState,
                        rootLevelFile = rootLevelFile,
                        parsedData = parsedData,
                        missingModules = missingModules,
                        currentTab = availableTabs.getOrElse(selectedTabIndex) { EditorTabType.Settings },
                        getLazyState = ::getLazyState,
                        getScrollState = ::getScrollState,
                        refreshTrigger = refreshTrigger,
                        actions = actions
                    )
                }
            }
        }
    }
}