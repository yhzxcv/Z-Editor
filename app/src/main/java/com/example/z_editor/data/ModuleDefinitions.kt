package com.example.z_editor.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.NextPlan
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.BlurCircular
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EditRoad
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Scoreboard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storm
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.filled.Tsunami
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.gson.Gson

/**
 * ==========================================
 * 1. 编辑器导航状态 (原 EditorScreen 内部类)
 * ==========================================
 * 移到此处是为了让 ModuleRegistry 可以引用它，
 * 从而在元数据中定义跳转目标。
 */
sealed class EditorSubScreen {
    object None : EditorSubScreen()
    object BasicInfo : EditorSubScreen()
    object WaveManagerSettings : EditorSubScreen()
    object ModuleSelection : EditorSubScreen()
    object StageSelection : EditorSubScreen()
    object GridItemSelection : EditorSubScreen()
    object ChallengeSelection : EditorSubScreen()
    object ToolSelection : EditorSubScreen()
    object ZombossSelection : EditorSubScreen()
    data class CustomZombieProperties(val rtid: String) : EditorSubScreen()
    data class PlantSelection(val isMultiSelect: Boolean = false) : EditorSubScreen()
    data class ZombieSelection(val isMultiSelect: Boolean = false) : EditorSubScreen()
    data class EventSelection(val waveIndex: Int) : EditorSubScreen()
    data class JsonView(val fileName: String) : EditorSubScreen()

    // 具体模块页
    data class LastStandMinigame(val rtid: String) : EditorSubScreen()
    data class SunDropper(val rtid: String) : EditorSubScreen()
    data class SunBombChallenge(val rtid: String) : EditorSubScreen()
    data class SeedBank(val rtid: String) : EditorSubScreen()
    data class ConveyorBelt(val rtid: String) : EditorSubScreen()
    data class WaveManagerModule(val rtid: String) : EditorSubScreen()
    data class InitialPlantEntry(val rtid: String) : EditorSubScreen()
    data class InitialZombieEntry(val rtid: String) : EditorSubScreen()
    data class InitialGridItemEntry(val rtid: String) : EditorSubScreen()
    data class ProtectTheGridItem(val rtid: String) : EditorSubScreen()
    data class ProtectThePlant(val rtid: String) : EditorSubScreen()
    data class Railcart(val rtid: String) : EditorSubScreen()
    data class PowerTile(val rtid: String) : EditorSubScreen()
    data class PiratePlank(val rtid: String) : EditorSubScreen()
    data class RoofProperties(val rtid: String) : EditorSubScreen()
    data class ManholePipelineModule(val rtid: String) : EditorSubScreen()
    data class Tide(val rtid: String) : EditorSubScreen()
    data class RainDarkProperties(val rtid: String) : EditorSubScreen()
    data class WarMistProperties(val rtid: String) : EditorSubScreen()
    data class ZombiePotionModuleProperties(val rtid: String) : EditorSubScreen()
    data class IncreasedCostModule(val rtid: String) : EditorSubScreen()
    data class DeathHoleModule(val rtid: String) : EditorSubScreen()
    data class ZombieMoveFastModule(val rtid: String) : EditorSubScreen()
    data class MaxSunModule(val rtid: String) : EditorSubScreen()
    data class StartingPlantfoodModule(val rtid: String) : EditorSubScreen()
    data class BowlingMinigameModule(val rtid: String) : EditorSubScreen()
    data class PennyClassroomModule(val rtid: String) : EditorSubScreen()
    data class SeedRainModule(val rtid: String) : EditorSubScreen()
    data class StarChallenge(val rtid: String) : EditorSubScreen()

    // 波次事件页
    data class UnknownDetail(val rtid: String) : EditorSubScreen()
    data class JitteredWaveDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class GroundWaveDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class ModifyConveyorDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class PortalDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class StormDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class RaidingDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class SpiderRainDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class ParachuteRainDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class BassRainDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class TidalChangeDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class BeachStageEventDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class BlackHoleDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class FrostWindDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class DinoEventDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class SpawnGravestonesDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class GridItemSpawnerDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class ZombiePotionActionDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class MagicMirrorDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class FairyTaleFogDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class FairyTaleWindDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class InvalidEvent(val rtid: String, val waveIndex: Int) : EditorSubScreen()
}


data class EventMetadata(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val darkColor: Color,
    val defaultAlias: String,
    val defaultObjClass: String,
    val initialDataFactory: () -> Any,
    val summaryProvider: ((PvzObject) -> String)? = null
)

object EventRegistry {
    private val registry = mapOf(
        "SpawnZombiesFromGroundSpawnerProps" to EventMetadata(
            title = "地底出怪",
            description = "从地下生成僵尸的出怪事件",
            icon = Icons.Default.Groups,
            color = Color(0xFF936457),
            darkColor = Color(0xFFC2A197),
            defaultAlias = "GroundSpawner",
            defaultObjClass = "SpawnZombiesFromGroundSpawnerProps",
            initialDataFactory = { WaveActionData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, SpawnZombiesFromGroundData::class.java)
                    "${data.zombies.size} 僵尸"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "SpawnZombiesJitteredWaveActionProps" to EventMetadata(
            title = "普通出怪",
            description = "最基础的自然出怪事件",
            icon = Icons.Default.Groups,
            color = Color(0xFF2196F3),
            darkColor = Color(0xFF90CAF9),
            defaultAlias = "Jittered",
            defaultObjClass = "SpawnZombiesJitteredWaveActionProps",
            initialDataFactory = { WaveActionData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, WaveActionData::class.java)
                    "${data.zombies.size} 僵尸"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "FrostWindWaveActionProps" to EventMetadata(
            title = "寒风侵袭",
            description = "在指定行吹起寒风冻结植物",
            icon = Icons.Default.AcUnit,
            color = Color(0xFF0288D1),
            darkColor = Color(0xFF90CAF9),
            defaultAlias = "FrostWindEvent",
            defaultObjClass = "FrostWindWaveActionProps",
            initialDataFactory = { FrostWindWaveActionPropsData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, FrostWindWaveActionPropsData::class.java)
                    "${data.winds.size} 股寒风"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "BeachStageEventZombieSpawnerProps" to EventMetadata(
            title = "退潮突袭",
            description = "僵尸在退潮时浮现突袭",
            icon = Icons.Default.Water,
            color = Color(0xFF00ACC1),
            darkColor = Color(0xFF81D4FA),
            defaultAlias = "LowTideEvent",
            defaultObjClass = "BeachStageEventZombieSpawnerProps",
            initialDataFactory = { BeachStageEventData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, BeachStageEventData::class.java)
                    "${data.zombieCount} 僵尸"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "TidalChangeWaveActionProps" to EventMetadata(
            title = "潮水变更",
            description = "改变潮水位置",
            icon = Icons.Default.WaterDrop,
            color = Color(0xFF00ACC1),
            darkColor = Color(0xFF81D4FA),
            defaultAlias = "TidalChangeEvent",
            defaultObjClass = "TidalChangeWaveActionProps",
            initialDataFactory = { TidalChangeWaveActionData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, TidalChangeWaveActionData::class.java)
                    "位置: ${data.tidalChange.changeAmount}"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "ModifyConveyorWaveActionProps" to EventMetadata(
            title = "传送带变更",
            description = "动态添加或移除传送带上的卡片",
            icon = Icons.Default.Transform,
            color = Color(0xFF4AC380),
            darkColor = Color(0xFF7CBD99),
            defaultAlias = "ModConveyorEvent",
            defaultObjClass = "ModifyConveyorWaveActionProps",
            initialDataFactory = { ModifyConveyorWaveActionData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, ModifyConveyorWaveActionData::class.java)
                    "+${data.addList.size} / -${data.removeList.size}"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "DinoWaveActionProps" to EventMetadata(
            title = "恐龙召唤",
            description = "在指定行召唤一只恐龙协助僵尸",
            icon = Icons.Default.Pets,
            color = Color(0xFF91B900),
            darkColor = Color(0xFFA2B659),
            defaultAlias = "DinoTimeEvent",
            defaultObjClass = "DinoWaveActionProps",
            initialDataFactory = { DinoWaveActionPropsData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, DinoWaveActionPropsData::class.java)
                    val typeMap = mapOf(
                        "raptor" to "迅猛龙", "stego" to "剑龙",
                        "ptero" to "翼龙", "tyranno" to "霸王龙",
                        "ankylo" to "甲龙"
                    )
                    "${typeMap[data.dinoType] ?: data.dinoType} ${data.dinoRow + 1}"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "SpawnModernPortalsWaveActionProps" to EventMetadata(
            title = "时空裂缝",
            description = "在指定位置召唤时空裂缝",
            icon = Icons.Default.HourglassEmpty,
            color = Color(0xFFFF9800),
            darkColor = Color(0xFFFFCC80),
            defaultAlias = "PortalEvent",
            defaultObjClass = "SpawnModernPortalsWaveActionProps",
            initialDataFactory = { PortalEventData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, PortalEventData::class.java)
                    data.portalType
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "StormZombieSpawnerProps" to EventMetadata(
            title = "风暴突袭",
            description = "沙尘暴或暴风雪运送僵尸",
            icon = Icons.Default.Storm,
            color = Color(0xFFFF9800),
            darkColor = Color(0xFFFFCC80),
            defaultAlias = "StormEvent",
            defaultObjClass = "StormZombieSpawnerProps",
            initialDataFactory = { StormZombieSpawnerPropsData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, StormZombieSpawnerPropsData::class.java)
                    "${data.zombies.size} 僵尸"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "RaidingPartyZombieSpawnerProps" to EventMetadata(
            title = "海盗登船",
            description = "生成若干个飞索僵尸的事件",
            icon = Icons.Default.Tsunami,
            color = Color(0xFFFF9800),
            darkColor = Color(0xFFFFCC80),
            defaultAlias = "RaidingPartyEvent",
            defaultObjClass = "RaidingPartyZombieSpawnerProps",
            initialDataFactory = { StormZombieSpawnerPropsData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, RaidingPartyEventData::class.java)
                    "${data.swashbucklerCount} 僵尸"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "ZombiePotionActionProps" to EventMetadata(
            title = "投放药水",
            description = "在场地固定位置强行生成障碍物",
            icon = Icons.Default.Science,
            color = Color(0xFF607D8B),
            darkColor = Color(0xFFB0BEC5),
            defaultAlias = "PotionEvent",
            defaultObjClass = "ZombiePotionActionProps",
            initialDataFactory = { ZombiePotionActionPropsData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, ZombiePotionActionPropsData::class.java)
                    "${data.potions.size} 个"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "SpawnGravestonesWaveActionProps" to EventMetadata(
            title = "障碍物生成",
            description = "在场地的空位处生成障碍物",
            icon = Icons.Filled.Unarchive,
            color = Color(0xFF607D8B),
            darkColor = Color(0xFFB0BEC5),
            defaultAlias = "GravestonesEvent",
            defaultObjClass = "SpawnGravestonesWaveActionProps",
            initialDataFactory = { SpawnGraveStonesData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, SpawnGraveStonesData::class.java)
                    "${data.gravestonePool.size} 种"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "SpawnZombiesFromGridItemSpawnerProps" to EventMetadata(
            title = "障碍物出怪",
            description = "从指定的障碍物种类生成僵尸",
            icon = Icons.Default.Groups,
            color = Color(0xFF607D8B),
            darkColor = Color(0xFFB0BEC5),
            defaultAlias = "GraveSpawner",
            defaultObjClass = "SpawnZombiesFromGridItemSpawnerProps",
            initialDataFactory = { SpawnZombiesFromGridItemData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, SpawnZombiesFromGridItemData::class.java)
                    "${data.zombies.size} 僵尸"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "FairyTaleFogWaveActionProps" to EventMetadata(
            title = "童话迷雾",
            description = "生成覆盖场地的迷雾",
            icon = Icons.Default.Cloud,
            color = Color(0xFFBE5DBA),
            darkColor = Color(0xFFBD99BB),
            defaultAlias = "FairyFogEvent",
            defaultObjClass = "FairyTaleFogWaveActionProps",
            initialDataFactory = { FairyTaleFogWaveActionData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, FairyTaleFogWaveActionData::class.java)
                    "mX: ${data.range.mX}"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "FairyTaleWindWaveActionProps" to EventMetadata(
            title = "童话微风",
            description = "把场上所有童话迷雾吹走的风",
            icon = Icons.Default.Air,
            color = Color(0xFFBE5DBA),
            darkColor = Color(0xFFBD99BB),
            defaultAlias = "WindEvent",
            defaultObjClass = "FairyTaleWindWaveActionProps",
            initialDataFactory = { FairyTaleWindWaveActionData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, FairyTaleWindWaveActionData::class.java)
                    "${data.duration}秒"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "SpiderRainZombieSpawnerProps" to EventMetadata(
            title = "小鬼空降",
            description = "僵尸依靠降落伞从天而降",
            icon = Icons.Default.BugReport,
            color = Color(0xFF9C27B0),
            darkColor = Color(0xFFB39DDB),
            defaultAlias = "SpiderRainEvent",
            defaultObjClass = "SpiderRainZombieSpawnerProps",
            initialDataFactory = { ParachuteRainEventData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, ParachuteRainEventData::class.java)
                    "${data.spiderCount} 僵尸"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "ParachuteRainZombieSpawnerProps" to EventMetadata(
            title = "降落伞空降",
            description = "僵尸依靠降落伞从天而降",
            icon = Icons.Default.AirplanemodeActive,
            color = Color(0xFF9C27B0),
            darkColor = Color(0xFFB39DDB),
            defaultAlias = "ParachuteRainEvent",
            defaultObjClass = "ParachuteRainZombieSpawnerProps",
            initialDataFactory = { ParachuteRainEventData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, ParachuteRainEventData::class.java)
                    "${data.spiderCount} 僵尸"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "BassRainZombieSpawnerProps" to EventMetadata(
            title = "贝斯/喷射空降",
            description = "贝斯手或喷射器僵尸从天而降",
            icon = Icons.Default.Speaker,
            color = Color(0xFF9C27B0),
            darkColor = Color(0xFFB39DDB),
            defaultAlias = "BassRainEvent",
            defaultObjClass = "BassRainZombieSpawnerProps",
            initialDataFactory = { ParachuteRainEventData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, ParachuteRainEventData::class.java)
                    "${data.spiderCount} 僵尸"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "BlackHoleWaveActionProps" to EventMetadata(
            title = "黑洞吸引",
            description = "生成黑洞吸引所有植物",
            icon = Icons.Default.BlurCircular,
            color = Color(0xFF7C30D9),
            darkColor = Color(0xFFA179D2),
            defaultAlias = "BlackHoleEvent",
            defaultObjClass = "BlackHoleWaveActionProps",
            initialDataFactory = { BlackHoleEventData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, BlackHoleEventData::class.java)
                    "${data.colNumPlantIsDragged} 列"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "WaveActionMagicMirrorTeleportationArrayProps2" to EventMetadata(
            title = "魔镜传送",
            description = "在场地上生成成对的传送门",
            icon = Icons.AutoMirrored.Filled.CompareArrows,
            color = Color(0xFF7C30D9),
            darkColor = Color(0xFFA179D2),
            defaultAlias = "MirrorEvent",
            defaultObjClass = "WaveActionMagicMirrorTeleportationArrayProps2",
            initialDataFactory = { MagicMirrorWaveActionData() },
            summaryProvider = { obj ->
                try {
                    val gson = Gson()
                    val data = gson.fromJson(obj.objData, MagicMirrorWaveActionData::class.java)
                    "${data.arrays.size} 组魔镜"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),

        )

    fun getAll() = registry.values.toList()
    fun getMetadata(objClass: String?): EventMetadata? {
        if (objClass == null) return null
        return registry[objClass]
    }
}

/**
 * ==========================================
 * 2. 模块元数据定义
 * ==========================================
 * 定义一个模块在列表中长什么样，以及点击后去哪里
 */

enum class ModuleCategory(val title: String) {
    Base("基础功能"),
    Mode("特殊模式"),
    Scene("场地配置"),
}

data class ModuleMetadata(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isCore: Boolean,
    val category: ModuleCategory,

    val defaultAlias: String,
    val defaultSource: String = "CurrentLevel",
    val allowMultiple: Boolean = false,

    val initialDataFactory: (() -> Any)? = null,
    val navigationFactory: (String) -> EditorSubScreen
)

/**
 * ==========================================
 * 3. 模块注册表 (核心逻辑)
 * ==========================================
 */
object ModuleRegistry {

    private val DEFAULT_METADATA = ModuleMetadata(
        title = "未知模块",
        description = "通用参数编辑器",
        icon = Icons.Default.Extension,
        isCore = false,
        category = ModuleCategory.Base,
        defaultAlias = "Unknown",
        navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
    )

    private val registry = mapOf(
        "WaveManagerModuleProperties" to ModuleMetadata(
            title = "波次管理器",
            description = "管理关卡的波次事件总配置",
            icon = Icons.Default.Timeline,
            isCore = true,
            category = ModuleCategory.Base,
            defaultAlias = "NewWaves",
            initialDataFactory = {
                WaveManagerModuleData(
                    waveManagerProps = "RTID(WaveManagerProps@CurrentLevel)",
                    dynamicZombies = mutableListOf(
                        DynamicZombieGroup(
                            pointIncrement = 0,
                            startingPoints = 0,
                            startingWave = 0,
                            zombiePool = mutableListOf(),
                            zombieLevel = mutableListOf()
                        )
                    )
                )
            },
            navigationFactory = { rtid -> EditorSubScreen.WaveManagerModule(rtid) }
        ),
        "CustomLevelModuleProperties" to ModuleMetadata(
            title = "庭院模块",
            description = "开启后关卡适配庭院框架",
            icon = Icons.Default.Home,
            isCore = false,
            category = ModuleCategory.Base,
            defaultAlias = "DefaultCustomLevel",
            defaultSource = "LevelModules",
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "StandardLevelIntroProperties" to ModuleMetadata(
            title = "转场动画",
            description = "关卡开始时的摄像机平移",
            icon = Icons.Default.MovieFilter,
            isCore = false,
            category = ModuleCategory.Base,
            defaultAlias = "StandardIntro",
            defaultSource = "LevelModules",
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "ZombiesAteYourBrainsProperties" to ModuleMetadata(
            title = "失败判定",
            description = "僵尸进屋判负的位置",
            icon = Icons.Default.Dangerous,
            isCore = false,
            category = ModuleCategory.Base,
            defaultAlias = "DefaultZombieWinCondition",
            defaultSource = "LevelModules",
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "ZombiesDeadWinConProperties" to ModuleMetadata(
            title = "死亡掉落",
            description = "关卡稳定运行必须模块",
            icon = Icons.Default.Redeem,
            isCore = false,
            category = ModuleCategory.Base,
            defaultAlias = "ZombiesDeadWinCon",
            defaultSource = "LevelModules",
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "PennyClassroomModuleProperties" to ModuleMetadata(
            title = "阶级定义",
            description = "全局定义植物阶级，能覆盖其他模块",
            icon = Icons.Default.Layers,
            isCore = true,
            category = ModuleCategory.Base,
            defaultAlias = "PennyClassroom",
            defaultSource = "CurrentLevel",
            initialDataFactory = { PennyClassroomModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.PennyClassroomModule(rtid) }
        ),
        "SeedBankProperties" to ModuleMetadata(
            title = "种子库",
            description = "预设卡槽植物与选卡方式",
            icon = Icons.Default.Yard,
            isCore = true,
            allowMultiple = true,
            category = ModuleCategory.Base,
            defaultAlias = "SeedBank",
            initialDataFactory = { SeedBankData() },
            navigationFactory = { rtid -> EditorSubScreen.SeedBank(rtid) }
        ),
        "ConveyorSeedBankProperties" to ModuleMetadata(
            title = "传送带",
            description = "预设传送带植物种类和权重",
            icon = Icons.Default.LinearScale,
            isCore = true,
            category = ModuleCategory.Base,
            defaultAlias = "ConveyorBelt",
            initialDataFactory = { ConveyorBeltData() },
            navigationFactory = { rtid -> EditorSubScreen.ConveyorBelt(rtid) }
        ),
        "SunDropperProperties" to ModuleMetadata(
            title = "阳光掉落",
            description = "控制天上掉落阳光的频率",
            icon = Icons.Default.WbSunny,
            isCore = true,
            category = ModuleCategory.Base,
            defaultAlias = "DefaultSunDropper",
            defaultSource = "LevelModules",
            navigationFactory = { rtid -> EditorSubScreen.SunDropper(rtid) }
        ),
        "LevelMutatorMaxSunProps" to ModuleMetadata(
            title = "阳光上限",
            description = "覆盖关卡最大阳光存储值",
            icon = Icons.Default.BrightnessHigh,
            isCore = true,
            category = ModuleCategory.Base,
            defaultAlias = "OverrideMaxSun",
            defaultSource = "CurrentLevel",
            initialDataFactory = { LevelMutatorMaxSunPropsData() },
            navigationFactory = { rtid -> EditorSubScreen.MaxSunModule(rtid) }
        ),
        "LevelMutatorStartingPlantfoodProps" to ModuleMetadata(
            title = "初始能量豆",
            description = "覆盖关卡开始时的能量豆数量",
            icon = Icons.Default.Eco,
            isCore = true,
            category = ModuleCategory.Base,
            defaultAlias = "OverrideStartingPlantFood",
            defaultSource = "CurrentLevel",
            initialDataFactory = { LevelMutatorStartingPlantfoodPropsData() },
            navigationFactory = { rtid -> EditorSubScreen.StartingPlantfoodModule(rtid) }
        ),
        "StarChallengeModuleProperties" to ModuleMetadata(
            title = "挑战模块",
            description = "设置关卡的限制条件与挑战目标",
            icon = Icons.AutoMirrored.Filled.FactCheck,
            isCore = true,
            category = ModuleCategory.Base,
            defaultAlias = "ChallengeModule",
            defaultSource = "CurrentLevel",
            initialDataFactory = { StarChallengeModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.StarChallenge(rtid) }
        ),
        "LevelScoringModuleProperties" to ModuleMetadata(
            title = "积分模块",
            description = "启用积分模块，杀死僵尸获得分数",
            icon = Icons.Default.Scoreboard,
            isCore = false,
            category = ModuleCategory.Base,
            defaultAlias = "LevelScoring",
            defaultSource = "CurrentLevel",
            initialDataFactory = { LevelScoringData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),

        "BowlingMinigameProperties" to ModuleMetadata(
            title = "沙滩保龄球",
            description = "设置禁种线以及禁用铲子",
            icon = Icons.Default.SportsEsports,
            isCore = true,
            category = ModuleCategory.Mode,
            defaultAlias = "BowlingBulbMinigame",
            defaultSource = "CurrentLevel",
            initialDataFactory = { BowlingMinigamePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.BowlingMinigameModule(rtid) }
        ),
        "NewBowlingMinigameProperties" to ModuleMetadata(
            title = "坚果保龄球",
            description = "在固定位置绘制保龄球警戒线",
            icon = Icons.Default.SportsEsports,
            isCore = false,
            category = ModuleCategory.Mode,
            defaultAlias = "NewBowlingBulbMinigame",
            defaultSource = "CurrentLevel",
            initialDataFactory = { NewBowlingMinigamePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "VaseBreakerPresetProperties" to ModuleMetadata(
            title = "罐子布局",
            description = "配置罐子的内容，需要另外两个模块支持",
            icon = Icons.Default.Grid4x4,
            isCore = false,
            category = ModuleCategory.Mode,
            defaultAlias = "VaseBreakerProps",
            defaultSource = "CurrentLevel",
            initialDataFactory = { VaseBreakerPresetData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "VaseBreakerArcadeModuleProperties" to ModuleMetadata(
            title = "砸罐子模式",
            description = "开启砸罐子模式的基础环境与UI支持",
            icon = Icons.Default.SportsEsports,
            isCore = false,
            category = ModuleCategory.Mode,
            defaultAlias = "VaseBreakerArcade",
            defaultSource = "LevelModules",
            initialDataFactory = { VaseBreakerArcadeModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "VaseBreakerFlowModuleProperties" to ModuleMetadata(
            title = "砸罐子动画",
            description = "控制砸罐子开始前罐子掉下来的动画",
            icon = Icons.AutoMirrored.Filled.NextPlan,
            isCore = false,
            category = ModuleCategory.Mode,
            defaultAlias = "VaseBreakerFlow",
            defaultSource = "LevelModules",
            initialDataFactory = { VaseBreakerFlowModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "EvilDaveProperties" to ModuleMetadata(
            title = "我是僵尸模式",
            description = "启用我是僵尸模式，需配置僵尸卡槽和预置植物",
            icon = Icons.Default.EmojiPeople,
            isCore = false,
            category = ModuleCategory.Mode,
            defaultAlias = "EvilDave",
            defaultSource = "CurrentLevel",
            initialDataFactory = { EvilDavePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "ZombossBattleModuleProperties" to ModuleMetadata(
            title = "僵王战模式",
            description = "配置僵王战模式参数以及僵王种类",
            icon = Icons.Default.Dangerous,
            isCore = false,
            category = ModuleCategory.Mode,
            defaultAlias = "ZombossBattle",
            defaultSource = "CurrentLevel",
            initialDataFactory = { ZombossBattleModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "ZombossBattleIntroProperties" to ModuleMetadata(
            title = "僵王转场",
            description = "控制Boss战前的过场动画与血条显示",
            icon = Icons.Default.MovieFilter,
            isCore = false,
            category = ModuleCategory.Mode,
            defaultAlias = "ZombossBattleIntro",
            defaultSource = "CurrentLevel",
            initialDataFactory = { ZombossBattleIntroData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "SeedRainProperties" to ModuleMetadata(
            title = "种子雨",
            description = "控制植物、僵尸或能量豆从天而降",
            icon = Icons.Default.Thunderstorm,
            isCore = true,
            category = ModuleCategory.Mode,
            defaultAlias = "SeedRain",
            defaultSource = "CurrentLevel",
            initialDataFactory = { SeedRainPropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.SeedRainModule(rtid) }
        ),
        "LastStandMinigameProperties" to ModuleMetadata(
            title = "坚不可摧",
            description = "设置初始资源，开启布阵阶段",
            icon = Icons.Default.Shield,
            isCore = true,
            category = ModuleCategory.Mode,
            defaultAlias = "LastStand",
            defaultSource = "CurrentLevel",
            initialDataFactory = { LastStandMinigamePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.LastStandMinigame(rtid) }
        ),
        "PVZ1OverwhelmModuleProperties" to ModuleMetadata(
            title = "排山倒海",
            description = "排山倒海小游戏，需配合传送带",
            icon = Icons.Default.LocalFlorist,
            isCore = false,
            category = ModuleCategory.Mode,
            defaultAlias = "PVZ1Overwhelm",
            defaultSource = "CurrentLevel",
            initialDataFactory = { PVZ1OverwhelmModulePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "SunBombChallengeProperties" to ModuleMetadata(
            title = "太阳炸弹",
            description = "配置掉落的太阳爆炸范围和伤害",
            icon = Icons.Default.BrightnessHigh,
            isCore = true,
            category = ModuleCategory.Mode,
            defaultAlias = "SunBombs",
            defaultSource = "CurrentLevel",
            initialDataFactory = { SunBombChallengeData() },
            navigationFactory = { rtid -> EditorSubScreen.SunBombChallenge(rtid) }
        ),
        "IncreasedCostModuleProperties" to ModuleMetadata(
            title = "通货膨胀",
            description = "植物阳光价格随种植次数递增",
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            isCore = true,
            category = ModuleCategory.Mode,
            defaultAlias = "IncreasedCostModule",
            defaultSource = "CurrentLevel",
            initialDataFactory = { IncreasedCostModulePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.IncreasedCostModule(rtid) }
        ),
        "DeathHoleModuleProperties" to ModuleMetadata(
            title = "遗落坑洞",
            description = "植物消失后留下不可种植的坑洞",
            icon = Icons.Default.TripOrigin,
            isCore = true,
            category = ModuleCategory.Mode,
            defaultAlias = "DeathHoleModule",
            defaultSource = "CurrentLevel",
            initialDataFactory = { DeathHoleModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.DeathHoleModule(rtid) }
        ),
        "ZombieMoveFastModuleProperties" to ModuleMetadata(
            title = "加速进场",
            description = "僵尸入场时快速移动一段距离",
            icon = Icons.Default.FastForward,
            isCore = true,
            category = ModuleCategory.Mode,
            defaultAlias = "FastSpeed",
            defaultSource = "CurrentLevel",
            initialDataFactory = { ZombieMoveFastModulePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.ZombieMoveFastModule(rtid) }
        ),

        "InitialPlantEntryProperties" to ModuleMetadata(
            title = "预置植物",
            description = "关卡开始时场上已存在的植物",
            icon = Icons.Default.Widgets,
            isCore = true,
            allowMultiple = true,
            category = ModuleCategory.Scene,
            defaultAlias = "InitialPlants",
            defaultSource = "CurrentLevel",
            initialDataFactory = { InitialPlantEntryData() },
            navigationFactory = { rtid -> EditorSubScreen.InitialPlantEntry(rtid) }
        ),
        "InitialZombieProperties" to ModuleMetadata(
            title = "预置僵尸",
            description = "关卡开始时场上已存在的僵尸",
            icon = Icons.Default.Widgets,
            isCore = true,
            allowMultiple = true,
            category = ModuleCategory.Scene,
            defaultAlias = "FrozenZombiePlacement",
            defaultSource = "CurrentLevel",
            initialDataFactory = { InitialZombieEntryData() },
            navigationFactory = { rtid -> EditorSubScreen.InitialZombieEntry(rtid) }
        ),
        "InitialGridItemProperties" to ModuleMetadata(
            title = "预置障碍物",
            description = "关卡开始时场上已存在的障碍物",
            icon = Icons.Default.Widgets,
            isCore = true,
            allowMultiple = true,
            category = ModuleCategory.Scene,
            defaultAlias = "GridItemPlacement",
            defaultSource = "CurrentLevel",
            initialDataFactory = { InitialGridItemEntryData() },
            navigationFactory = { rtid -> EditorSubScreen.InitialGridItemEntry(rtid) }
        ),
        "ProtectThePlantChallengeProperties" to ModuleMetadata(
            title = "保护植物挑战",
            description = "设置关卡中必须保护的植物",
            icon = Icons.Default.Security,
            isCore = true,
            allowMultiple = true,
            category = ModuleCategory.Scene,
            defaultAlias = "ProtectThePlant",
            defaultSource = "CurrentLevel",
            initialDataFactory = { ProtectThePlantChallengePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.ProtectThePlant(rtid) }
        ),
        "ProtectTheGridItemChallengeProperties" to ModuleMetadata(
            title = "保护物品挑战",
            description = "设置关卡中必须保护且不能被破坏的物品",
            icon = Icons.Default.Security,
            isCore = true,
            allowMultiple = true,
            category = ModuleCategory.Scene,
            defaultAlias = "ProtectTheGridItem",
            defaultSource = "CurrentLevel",
            initialDataFactory = { ProtectTheGridItemChallengePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.ProtectTheGridItem(rtid) }
        ),
        "ZombiePotionModuleProperties" to ModuleMetadata(
            title = "僵尸药水",
            description = "配置黑暗时代药水自动生成机制",
            icon = Icons.Default.Science,
            isCore = true,
            allowMultiple = true,
            category = ModuleCategory.Scene,
            defaultAlias = "ZombiePotions",
            defaultSource = "CurrentLevel",
            initialDataFactory = { ZombiePotionModulePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.ZombiePotionModuleProperties(rtid) }
        ),
        "PiratePlankProperties" to ModuleMetadata(
            title = "海盗甲板",
            description = "配置海盗地图的甲板行数",
            icon = Icons.Default.EditRoad,
            isCore = true,
            category = ModuleCategory.Scene,
            defaultAlias = "PiratePlanks",
            defaultSource = "CurrentLevel",
            initialDataFactory = { PiratePlankPropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.PiratePlank(rtid) }
        ),
        "RailcartProperties" to ModuleMetadata(
            title = "矿车轨道",
            description = "配置矿车与轨道初始布局",
            icon = Icons.Default.EditRoad,
            isCore = true,
            category = ModuleCategory.Scene,
            defaultAlias = "Railcarts",
            defaultSource = "CurrentLevel",
            initialDataFactory = { RailcartPropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.Railcart(rtid) }
        ),
        "PowerTileProperties" to ModuleMetadata(
            title = "能量瓷砖",
            description = "配置能量豆联动效果与瓷砖布局",
            icon = Icons.Default.Bolt,
            isCore = true,
            category = ModuleCategory.Scene,
            defaultAlias = "FutureLinkedTileGroups",
            defaultSource = "CurrentLevel",
            initialDataFactory = { PowerTilePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.PowerTile(rtid) }
        ),
        "ManholePipelineModuleProperties" to ModuleMetadata(
            title = "地下管道",
            description = "配置蒸汽时代的地下传输管道",
            icon = Icons.Default.Timeline,
            isCore = true,
            category = ModuleCategory.Scene,
            defaultAlias = "ManholePipeline",
            defaultSource = "CurrentLevel",
            initialDataFactory = { ManholePipelineModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.ManholePipelineModule(rtid) }
        ),
        "RoofProperties" to ModuleMetadata(
            title = "屋顶花盆",
            description = "配置屋顶关卡的预置花盆列数",
            icon = Icons.Default.LocalFlorist,
            isCore = true,
            category = ModuleCategory.Scene,
            defaultAlias = "RoofProps",
            defaultSource = "CurrentLevel",
            initialDataFactory = { RoofPropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.RoofProperties(rtid) }
        ),
        "TideProperties" to ModuleMetadata(
            title = "潮水系统",
            description = "开启关卡中的潮水系统，需最后添加",
            icon = Icons.Default.WaterDrop,
            isCore = true,
            category = ModuleCategory.Scene,
            defaultAlias = "Tide",
            defaultSource = "CurrentLevel",
            initialDataFactory = { TidePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.Tide(rtid) }
        ),
        "WarMistProperties" to ModuleMetadata(
            title = "迷雾系统",
            description = "设置战场迷雾覆盖范围与交互",
            icon = Icons.Default.Cloud,
            isCore = true,
            category = ModuleCategory.Scene,
            defaultAlias = "WarMist",
            defaultSource = "CurrentLevel",
            initialDataFactory = { WarMistPropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.WarMistProperties(rtid) }
        ),
        "RainDarkProperties" to ModuleMetadata(
            title = "环境天气",
            description = "设置关卡的雨雪、雷电等环境特效",
            icon = Icons.Default.AcUnit,
            isCore = true,
            category = ModuleCategory.Scene,
            defaultAlias = "DefaultSnow",
            defaultSource = "LevelModules",
            navigationFactory = { rtid -> EditorSubScreen.RainDarkProperties(rtid) }
        ),

        )


    fun getMetadata(objClass: String): ModuleMetadata {
        registry[objClass]?.let { return it }

        return when {
            else -> DEFAULT_METADATA
        }
    }

    /**
     * 获取所有注册的模块 (供"添加模块"列表使用)
     */
    fun getAllKnownModules() = registry
}