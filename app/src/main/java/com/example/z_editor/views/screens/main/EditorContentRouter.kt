package com.example.z_editor.views.screens.main

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.example.z_editor.data.EditorSubScreen
import com.example.z_editor.data.LevelParser
import com.example.z_editor.data.ModuleMetadata
import com.example.z_editor.data.ModuleRegistry
import com.example.z_editor.data.ParsedLevelData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.repository.ReferenceRepository
import com.example.z_editor.views.editor.pages.others.CustomZombiePropertiesEP
import com.example.z_editor.views.editor.pages.event.BassRainEventEP
import com.example.z_editor.views.editor.pages.event.BeachStageEventEP
import com.example.z_editor.views.editor.pages.event.BlackHoleEventEP
import com.example.z_editor.views.editor.pages.event.DinoEventEP
import com.example.z_editor.views.editor.pages.event.FairyTaleFogWaveActionPropsEP
import com.example.z_editor.views.editor.pages.event.FairyTaleWindWaveActionPropsEP
import com.example.z_editor.views.editor.pages.event.FrostWindEventEP
import com.example.z_editor.views.editor.pages.event.InvalidEventEP
import com.example.z_editor.views.editor.pages.event.MagicMirrorEventEP
import com.example.z_editor.views.editor.pages.event.ModifyConveyorEventEP
import com.example.z_editor.views.editor.pages.event.ParachuteRainEventEP
import com.example.z_editor.views.editor.pages.event.RaidingPartyEventEP
import com.example.z_editor.views.editor.pages.event.SpawnGraveStonesEventEP
import com.example.z_editor.views.editor.pages.event.SpawnModernPortalsWaveActionPropsEP
import com.example.z_editor.views.editor.pages.event.SpawnZombiesFromGridItemSpawnerEventEP
import com.example.z_editor.views.editor.pages.event.SpawnZombiesFromGroundEventEP
import com.example.z_editor.views.editor.pages.event.SpawnZombiesJitteredWaveActionPropsEP
import com.example.z_editor.views.editor.pages.event.SpiderRainEventEP
import com.example.z_editor.views.editor.pages.event.StormZombieSpawnerPropsEP
import com.example.z_editor.views.editor.pages.event.TidalChangeEventEP
import com.example.z_editor.views.editor.pages.event.ZombiePotionActionPropsEP
import com.example.z_editor.views.editor.pages.module.BowlingMinigamePropertiesEP
import com.example.z_editor.views.editor.pages.module.ConveyorSeedBankPropertiesEP
import com.example.z_editor.views.editor.pages.module.DeathHoleModuleEP
import com.example.z_editor.views.editor.pages.module.IncreasedCostModulePropertiesEP
import com.example.z_editor.views.editor.pages.module.InitialGridItemEntryEP
import com.example.z_editor.views.editor.pages.module.InitialPlantEntryEP
import com.example.z_editor.views.editor.pages.module.InitialZombieEntryEP
import com.example.z_editor.views.editor.pages.module.LastStandMinigamePropertiesEP
import com.example.z_editor.views.editor.pages.module.LevelMutatorMaxSunPropsEP
import com.example.z_editor.views.editor.pages.module.LevelMutatorStartingPlantfoodPropsEP
import com.example.z_editor.views.editor.pages.module.ManholePipelinePropertiesEP
import com.example.z_editor.views.editor.pages.module.PiratePlankPropertiesEP
import com.example.z_editor.views.editor.pages.module.PowerTilePropertiesEP
import com.example.z_editor.views.editor.pages.module.ProtectTheGridItemChallengePropertiesEP
import com.example.z_editor.views.editor.pages.module.ProtectThePlantChallengePropertiesEP
import com.example.z_editor.views.editor.pages.module.RailcartPropertiesEP
import com.example.z_editor.views.editor.pages.module.RainDarkPropertiesEP
import com.example.z_editor.views.editor.pages.module.RoofPropertiesEP
import com.example.z_editor.views.editor.pages.module.SeedBankPropertiesEP
import com.example.z_editor.views.editor.pages.module.StarChallengeModulePropertiesEP
import com.example.z_editor.views.editor.pages.module.SunBombChallengePropertiesEP
import com.example.z_editor.views.editor.pages.module.SunDropperPropertiesEP
import com.example.z_editor.views.editor.pages.module.TidePropertiesEP
import com.example.z_editor.views.editor.pages.module.WarMistPropertiesEP
import com.example.z_editor.views.editor.pages.module.WaveManagerModulePropertiesEP
import com.example.z_editor.views.editor.pages.module.ZombieMoveFastModulePropertiesEP
import com.example.z_editor.views.editor.pages.module.ZombiePotionModulePropertiesEP
import com.example.z_editor.views.editor.pages.others.LevelDefinitionEP
import com.example.z_editor.views.editor.pages.others.UnknownEP
import com.example.z_editor.views.editor.pages.others.WaveManagerPropertiesEP
import com.example.z_editor.views.editor.tabs.IZombieTab
import com.example.z_editor.views.editor.tabs.LevelSettingsTab
import com.example.z_editor.views.editor.tabs.VaseBreakerTab
import com.example.z_editor.views.editor.tabs.WaveTimelineTab
import com.example.z_editor.views.editor.tabs.ZombossBattleTab
import com.example.z_editor.views.screens.select.ChallengeSelectionScreen
import com.example.z_editor.views.screens.select.EventSelectionScreen
import com.example.z_editor.views.screens.select.GridItemSelectionScreen
import com.example.z_editor.views.screens.select.ModuleSelectionScreen
import com.example.z_editor.views.screens.select.PlantSelectionScreen
import com.example.z_editor.views.screens.select.StageSelectionScreen
import com.example.z_editor.views.screens.select.ToolSelectionScreen
import com.example.z_editor.views.screens.select.ZombieSelectionScreen
import com.example.z_editor.views.screens.select.ZombossSelectionScreen

/**
 * 路由分发器：只负责根据 targetState 渲染对应的页面
 */
@Composable
fun EditorContentRouter(
    targetState: EditorSubScreen,
    rootLevelFile: PvzLevelFile?,
    parsedData: ParsedLevelData?,
    missingModules: List<ModuleMetadata>,
    currentTab: EditorTabType,
    getLazyState: (String) -> LazyListState,
    getScrollState: (String) -> ScrollState,
    refreshTrigger: Int,
    actions: EditorActions
) {
    if (parsedData == null || rootLevelFile == null) return

    when (targetState) {
        // ======================== 主页面：Tab 结构 ========================
        EditorSubScreen.None -> {
            when (currentTab) {
                EditorTabType.Settings -> {
                    key(refreshTrigger) {
                        LevelSettingsTab(
                            levelDef = parsedData.levelDef,
                            objectMap = parsedData.objectMap,
                            scrollState = getLazyState("GlobalSettingsTab"),
                            onEditBasicInfo = { actions.navigateTo(EditorSubScreen.BasicInfo) },
                            onEditModule = { rtid ->
                                val info = RtidParser.parse(rtid)
                                val alias = info?.alias ?: ""
                                val objClass = if (info?.source == "CurrentLevel")
                                    parsedData.objectMap[alias]?.objClass else {
                                    ReferenceRepository.getObjClass(alias)
                                } ?: "Unknown"
                                val metadata = ModuleRegistry.getMetadata(objClass)
                                actions.navigateTo(metadata.navigationFactory(rtid))
                            },
                            missingModules = missingModules,
                            onRemoveModule = actions.onRemoveModule,
                            onNavigateToAddModule = { actions.navigateTo(EditorSubScreen.ModuleSelection) },
                        )
                    }
                }

                EditorTabType.Timeline -> {
                    WaveTimelineTab(
                        rootLevelFile = rootLevelFile,
                        waveManager = parsedData.waveManager,
                        waveModule = parsedData.waveModule,
                        objectMap = parsedData.objectMap,
                        scrollState = getLazyState("WaveTimelineTab"),
                        refreshTrigger = refreshTrigger,
                        onEditEvent = { rtid, waveIdx ->
                            val alias = LevelParser.extractAlias(rtid)
                            val isInvalid = !parsedData.objectMap.containsKey(alias)
                            if (isInvalid) {
                                actions.navigateTo(EditorSubScreen.InvalidEvent(rtid, waveIdx))
                            } else {
                                val obj = parsedData.objectMap[alias]
                                val nextScreen = when (obj?.objClass) {
                                    "SpawnZombiesJitteredWaveActionProps" -> EditorSubScreen.JitteredWaveDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "SpawnZombiesFromGroundSpawnerProps" -> EditorSubScreen.GroundWaveDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "SpawnModernPortalsWaveActionProps" -> EditorSubScreen.PortalDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "ModifyConveyorWaveActionProps" -> EditorSubScreen.ModifyConveyorDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "StormZombieSpawnerProps" -> EditorSubScreen.StormDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "RaidingPartyZombieSpawnerProps" -> EditorSubScreen.RaidingDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "SpiderRainZombieSpawnerProps" -> EditorSubScreen.SpiderRainDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "ParachuteRainZombieSpawnerProps" -> EditorSubScreen.ParachuteRainDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "BassRainZombieSpawnerProps" -> EditorSubScreen.BassRainDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "TidalChangeWaveActionProps" -> EditorSubScreen.TidalChangeDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "BeachStageEventZombieSpawnerProps" -> EditorSubScreen.BeachStageEventDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "BlackHoleWaveActionProps" -> EditorSubScreen.BlackHoleDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "FrostWindWaveActionProps" -> EditorSubScreen.FrostWindDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "DinoWaveActionProps" -> EditorSubScreen.DinoEventDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "SpawnGravestonesWaveActionProps" -> EditorSubScreen.SpawnGravestonesDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "SpawnZombiesFromGridItemSpawnerProps" -> EditorSubScreen.GridItemSpawnerDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "ZombiePotionActionProps" -> EditorSubScreen.ZombiePotionActionDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "WaveActionMagicMirrorTeleportationArrayProps2" -> EditorSubScreen.MagicMirrorDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "FairyTaleFogWaveActionProps" -> EditorSubScreen.FairyTaleFogDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    "FairyTaleWindWaveActionProps" -> EditorSubScreen.FairyTaleWindDetail(
                                        rtid,
                                        waveIdx
                                    )

                                    else -> EditorSubScreen.UnknownDetail(rtid)
                                }
                                actions.navigateTo(nextScreen)
                            }
                        },
                        onNavigateToAddEvent = { waveIdx ->
                            actions.navigateTo(
                                EditorSubScreen.EventSelection(
                                    waveIdx
                                )
                            )
                        },
                        onEditSettings = { actions.navigateTo(EditorSubScreen.WaveManagerSettings) },
                        onWavesChanged = actions.onWavesChanged,
                        onCreateContainer = actions.onCreateWaveContainer,
                        onDeleteContainer = actions.onDeleteWaveContainer,
                        parsedData = parsedData,
                        onEditCustomZombie = actions.onEditCustomZombie
                    )
                }

                EditorTabType.VaseBreaker -> {
                    VaseBreakerTab(
                        rootLevelFile = rootLevelFile,
                        onRequestPlantSelection = actions.onLaunchMultiPlantSelector,
                        onRequestZombieSelection = actions.onLaunchMultiZombieSelector,
                        scrollState = getLazyState("VaseBreakerTab"),
                        refreshTrigger = refreshTrigger
                    )
                }

                EditorTabType.IZombie -> {
                    IZombieTab(
                        rootLevelFile = rootLevelFile,
                        parsedData = parsedData
                    )
                }

                EditorTabType.BossFight -> {
                    ZombossBattleTab(
                        rootLevelFile = rootLevelFile,
                        onLaunchZombossSelector = actions.onLaunchZombossSelector
                    )
                }
            }
        }

        // ======================== 具体子页面：模块 ========================

        EditorSubScreen.BasicInfo -> LevelDefinitionEP(
            rootLevelFile = rootLevelFile,
            onBack = actions.navigateBack,
            onNavigateToStageSelection = { actions.navigateTo(EditorSubScreen.StageSelection) },
            scrollState = getScrollState("BasicInfo")
        )

        EditorSubScreen.WaveManagerSettings -> {
            val hasConveyor = parsedData.levelDef?.modules?.any { rtid ->
                val info = RtidParser.parse(rtid)
                val alias = info?.alias ?: ""
                val objClass =
                    if (info?.source == "CurrentLevel") parsedData.objectMap[alias]?.objClass
                    else ReferenceRepository.getObjClass(alias)
                objClass == "ConveyorSeedBankProperties"
            } == true
            WaveManagerPropertiesEP(
                rootLevelFile = rootLevelFile,
                hasConveyor = hasConveyor,
                onBack = {
                    actions.navigateBack()
                },
                scrollState = getScrollState("WaveManagerSettings")
            )
        }


        is EditorSubScreen.LastStandMinigame -> LastStandMinigamePropertiesEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("LastStandMinigame")
        )

        is EditorSubScreen.SunDropper -> SunDropperPropertiesEP(
            rtid = targetState.rtid,
            rootLevelFile = rootLevelFile,
            onBack = actions.navigateBack,
            onToggleMode = actions.onToggleSunDropperMode,
            scrollState = getScrollState("SunDropper")
        )

        is EditorSubScreen.SeedBank -> SeedBankPropertiesEP(
            rtid = targetState.rtid,
            rootLevelFile = rootLevelFile,
            onBack = actions.navigateBack,
            onRequestPlantSelection = actions.onLaunchMultiPlantSelector,
            onRequestZombieSelection = actions.onLaunchMultiZombieSelector,
            scrollState = getScrollState("SeedBank")
        )

        is EditorSubScreen.ConveyorBelt -> ConveyorSeedBankPropertiesEP(
            rtid = targetState.rtid,
            rootLevelFile = rootLevelFile,
            onBack = actions.navigateBack,
            onRequestToolSelection = actions.onLaunchToolSelector,
            onRequestPlantSelection = actions.onLaunchPlantSelector,
            scrollState = getScrollState("ConveyorBelt")
        )

        is EditorSubScreen.InitialPlantEntry -> InitialPlantEntryEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            onRequestPlantSelection = actions.onLaunchPlantSelector
        )

        is EditorSubScreen.InitialZombieEntry -> InitialZombieEntryEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            onRequestZombieSelection = actions.onLaunchZombieSelector
        )

        is EditorSubScreen.InitialGridItemEntry -> InitialGridItemEntryEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            onRequestGridItemSelection = actions.onLaunchGridItemSelector
        )

        is EditorSubScreen.ProtectTheGridItem -> ProtectTheGridItemChallengePropertiesEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            onRequestGridItemSelection = actions.onLaunchGridItemSelector
        )

        is EditorSubScreen.ProtectThePlant -> ProtectThePlantChallengePropertiesEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            onRequestPlantSelection = actions.onLaunchPlantSelector
        )

        is EditorSubScreen.SunBombChallenge -> SunBombChallengePropertiesEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("SunBomb")
        )

        is EditorSubScreen.StarChallenge -> StarChallengeModulePropertiesEP(
            rtid = targetState.rtid,
            rootLevelFile = rootLevelFile,
            objectMap = parsedData.objectMap,
            onBack = actions.navigateBack,
            onNavigateToAddChallenge = {
                actions.onLaunchChallengeSelector { info ->
                    actions.onAddChallenge(info)
                    actions.navigateTo(EditorSubScreen.StarChallenge(targetState.rtid))
                }
            },
            scrollState = getScrollState("StarChallenge")
        )

        is EditorSubScreen.PiratePlank -> PiratePlankPropertiesEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            levelDef = parsedData.levelDef!!,
            scrollState = getScrollState("PiratePlank")
        )

        is EditorSubScreen.RoofProperties -> RoofPropertiesEP(
        rtid = targetState.rtid,
        onBack = actions.navigateBack,
        rootLevelFile = rootLevelFile,
            levelDef = parsedData.levelDef!!,
        scrollState = getScrollState("RoofProperties")
        )

        is EditorSubScreen.Tide -> TidePropertiesEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("Tide")
        )

        is EditorSubScreen.Railcart -> RailcartPropertiesEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("Railcart")
        )

        is EditorSubScreen.PowerTile -> PowerTilePropertiesEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("PowerTile")
        )

        is EditorSubScreen.WaveManagerModule -> WaveManagerModulePropertiesEP(
            rtid = targetState.rtid,
            rootLevelFile = rootLevelFile,
            onBack = actions.navigateBack,
            onRequestZombieSelection = actions.onLaunchZombieSelector,
            scrollState = getScrollState("WaveManagerModule")
        )

        is EditorSubScreen.RainDarkProperties -> RainDarkPropertiesEP(
            currentRtid = targetState.rtid,
            levelDef = parsedData.levelDef!!,
            onBack = actions.navigateBack,
            onUpdate = actions.onWavesChanged
        )

        is EditorSubScreen.WarMistProperties -> WarMistPropertiesEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("WarMistProperties")
        )

        is EditorSubScreen.ZombiePotionModuleProperties -> ZombiePotionModulePropertiesEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            onRequestGridItemSelection = actions.onLaunchGridItemSelector,
            scrollState = getScrollState("ZombiePotionModuleProperties")
        )

        is EditorSubScreen.IncreasedCostModule -> IncreasedCostModulePropertiesEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("IncreasedCostModule")
        )

        is EditorSubScreen.DeathHoleModule -> DeathHoleModuleEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("DeathHoleModule")
        )

        is EditorSubScreen.ZombieMoveFastModule -> ZombieMoveFastModulePropertiesEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("ZombieMoveFastModule")
        )

        is EditorSubScreen.MaxSunModule -> LevelMutatorMaxSunPropsEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("MaxSunModule")
        )

        is EditorSubScreen.StartingPlantfoodModule -> LevelMutatorStartingPlantfoodPropsEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("StartingPlantfoodModule")
        )

        is EditorSubScreen.BowlingMinigameModule -> BowlingMinigamePropertiesEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("BowlingMinigameModule")
        )

        is EditorSubScreen.ManholePipelineModule -> ManholePipelinePropertiesEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("ManholePipelineModule")
        )

        is EditorSubScreen.UnknownDetail -> UnknownEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            scrollState = getScrollState("UnknownDetail")
        )

        // ======================== 具体子页面：事件详情 ========================

        is EditorSubScreen.JitteredWaveDetail -> SpawnZombiesJitteredWaveActionPropsEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            onRequestZombieSelection = actions.onLaunchZombieSelector,
            onRequestPlantSelection = actions.onLaunchPlantSelector,
            scrollState = getLazyState(targetState.rtid),
            onInjectZombie = actions.onInjectZombie,
            onEditCustomZombie = actions.onEditCustomZombie
        )

        is EditorSubScreen.GroundWaveDetail -> SpawnZombiesFromGroundEventEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            onRequestZombieSelection = actions.onLaunchZombieSelector,
            onRequestPlantSelection = actions.onLaunchPlantSelector,
            scrollState = getLazyState(targetState.rtid),
            onInjectZombie = actions.onInjectZombie,
            onEditCustomZombie = actions.onEditCustomZombie
        )

        is EditorSubScreen.ModifyConveyorDetail -> ModifyConveyorEventEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            onRequestPlantSelection = actions.onLaunchPlantSelector,
            scrollState = getScrollState("ModifyConveyorDetail")
        )

        is EditorSubScreen.PortalDetail -> SpawnModernPortalsWaveActionPropsEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("PortalDetail")
        )

        is EditorSubScreen.StormDetail -> StormZombieSpawnerPropsEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            onRequestZombieSelection = actions.onLaunchZombieSelector,
            scrollState = getLazyState(targetState.rtid),
            onInjectZombie = actions.onInjectZombie,
            onEditCustomZombie = actions.onEditCustomZombie
        )

        is EditorSubScreen.RaidingDetail -> RaidingPartyEventEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("RaidingDetail")
        )

        is EditorSubScreen.SpiderRainDetail -> SpiderRainEventEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getLazyState(targetState.rtid)
        )

        is EditorSubScreen.ParachuteRainDetail -> ParachuteRainEventEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getLazyState(targetState.rtid)
        )

        is EditorSubScreen.BassRainDetail -> BassRainEventEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getLazyState(targetState.rtid)
        )

        is EditorSubScreen.BeachStageEventDetail -> BeachStageEventEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            onRequestZombieSelection = actions.onLaunchZombieSelector,
            scrollState = getLazyState(targetState.rtid)
        )

        is EditorSubScreen.TidalChangeDetail -> TidalChangeEventEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("TidalChangeDetail")
        )

        is EditorSubScreen.BlackHoleDetail -> BlackHoleEventEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("BlackHoleDetail")
        )

        is EditorSubScreen.FrostWindDetail -> FrostWindEventEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("FrostWindDetail")
        )

        is EditorSubScreen.DinoEventDetail -> DinoEventEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("DinoEventDetail")
        )

        is EditorSubScreen.SpawnGravestonesDetail -> SpawnGraveStonesEventEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            onRequestGridItemSelection = actions.onLaunchGridItemSelector,
            scrollState = getLazyState(targetState.rtid)
        )

        is EditorSubScreen.GridItemSpawnerDetail -> SpawnZombiesFromGridItemSpawnerEventEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            onRequestZombieSelection = actions.onLaunchZombieSelector,
            onRequestGridItemSelection = actions.onLaunchGridItemSelector,
            scrollState = getLazyState(targetState.rtid),
            onInjectZombie = actions.onInjectZombie,
            onEditCustomZombie = actions.onEditCustomZombie
        )

        is EditorSubScreen.ZombiePotionActionDetail -> ZombiePotionActionPropsEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            onRequestGridItemSelection = actions.onLaunchGridItemSelector
        )

        is EditorSubScreen.MagicMirrorDetail -> MagicMirrorEventEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile
        )

        is EditorSubScreen.FairyTaleFogDetail -> FairyTaleFogWaveActionPropsEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("FairyTaleFogDetail")
        )

        is EditorSubScreen.FairyTaleWindDetail -> FairyTaleWindWaveActionPropsEP(
            rtid = targetState.rtid,
            onBack = actions.navigateBack,
            rootLevelFile = rootLevelFile,
            scrollState = getScrollState("FairyTaleWindDetail")
        )

        is EditorSubScreen.InvalidEvent -> InvalidEventEP(
            rtid = targetState.rtid,
            waveIndex = targetState.waveIndex,
            onDeleteReference = { rtid -> actions.onDeleteEventReference(rtid) },
            onBack = actions.navigateBack,
            scrollState = getScrollState("InvalidEvent")
        )


        is EditorSubScreen.CustomZombieProperties -> {
            CustomZombiePropertiesEP(
                rtid = targetState.rtid,
                rootLevelFile = rootLevelFile,
                onBack = { actions.navigateBack() },
                scrollState = getScrollState("CustomZombie_${targetState.rtid}")
            )
        }

        // ======================== 选择器与导航 ========================

        is EditorSubScreen.EventSelection -> EventSelectionScreen(
            waveIndex = targetState.waveIndex,
            onEventSelected = { meta -> actions.onAddEvent(meta, targetState.waveIndex) },
            onBack = actions.navigateBack
        )

        is EditorSubScreen.PlantSelection -> PlantSelectionScreen(
            isMultiSelect = targetState.isMultiSelect,
            onPlantSelected = { id -> actions.onSelectorResult(id) },
            onMultiPlantSelected = { ids -> actions.onSelectorResult(ids) },
            onBack = actions.onSelectorCancel
        )

        is EditorSubScreen.ZombieSelection -> ZombieSelectionScreen(
            isMultiSelect = targetState.isMultiSelect,
            onZombieSelected = { id -> actions.onSelectorResult(id) },
            onMultiZombieSelected = { ids -> actions.onSelectorResult(ids) },
            onBack = actions.onSelectorCancel
        )

        EditorSubScreen.GridItemSelection -> GridItemSelectionScreen(
            onGridItemSelected = { id -> actions.onSelectorResult(id) },
            onBack = actions.onSelectorCancel
        )

        EditorSubScreen.ModuleSelection -> {
            val existingClasses = parsedData.levelDef?.modules?.mapNotNull { rtid ->
                val info = RtidParser.parse(rtid)
                if (info?.source == "CurrentLevel") parsedData.objectMap[info.alias]?.objClass
                else ReferenceRepository.getObjClass(info?.alias ?: "")
            }?.toSet() ?: emptySet()

            ModuleSelectionScreen(
                existingObjClasses = existingClasses,
                onModuleSelected = { meta -> actions.onAddModule(meta) },
                onBack = actions.navigateBack
            )
        }

        EditorSubScreen.StageSelection -> StageSelectionScreen(
            onStageSelected = actions.onStageSelected,
            onBack = actions.onStageCanceled
        )

        EditorSubScreen.ChallengeSelection -> ChallengeSelectionScreen(
            onChallengeSelected = { info -> actions.onChallengeSelected(info) },
            onBack = actions.onSelectorCancel
        )

        EditorSubScreen.ToolSelection -> ToolSelectionScreen(
            onToolSelected = { id -> actions.onSelectorResult(id) },
            onBack = actions.onSelectorCancel
        )

        EditorSubScreen.ZombossSelection -> {
            ZombossSelectionScreen(
                onSelected = { id -> actions.onSelectorResult(id) },
                onBack = actions.onSelectorCancel
            )
        }
    }
}