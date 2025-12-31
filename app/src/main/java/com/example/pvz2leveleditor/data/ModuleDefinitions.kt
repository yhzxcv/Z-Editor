package com.example.pvz2leveleditor.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Storm
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Tsunami
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

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
    object PlantSelection : EditorSubScreen()
    object ZombieSelection : EditorSubScreen()
    object ModuleSelection : EditorSubScreen()
    object StageSelection : EditorSubScreen()
    object GridItemSelection : EditorSubScreen()
    object ChallengeSelection : EditorSubScreen()
    data class EventSelection(val waveIndex: Int) : EditorSubScreen()

    // 具体模块页
    data class SunDropper(val rtid: String) : EditorSubScreen()
    data class SeedBank(val rtid: String) : EditorSubScreen()
    data class ConveyorBelt(val rtid: String) : EditorSubScreen()
    data class WaveManagerModule(val rtid: String) : EditorSubScreen()
    data class InitialPlantEntry(val rtid: String) : EditorSubScreen()
    data class InitialZombieEntry(val rtid: String) : EditorSubScreen()
    data class InitialGridItemEntry(val rtid: String) : EditorSubScreen()
    data class SunBombChallenge(val rtid: String) : EditorSubScreen()
    data class StarChallenge(val rtid: String) : EditorSubScreen()

    // 波次事件页
    data class UnknownDetail(val rtid: String) : EditorSubScreen()
    data class JitteredWaveDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class GroundWaveDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class ModifyConveyorDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class PortalDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class StormDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class RaidingDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class ParachuteRainDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class InvalidEvent(val rtid: String, val waveIndex: Int) : EditorSubScreen()
}


data class EventMetadata(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val defaultAlias: String,
    val defaultObjClass: String,
    val initialDataFactory: () -> Any,
    val summaryProvider: ((PvzObject) -> String)? = null
)

object EventRegistry {
    private val registry = mapOf(
        "ModifyConveyorWaveActionProps" to EventMetadata(
            title = "传送带变更",
            description = "动态添加或移除传送带上的卡片",
            icon = Icons.Default.Transform,
            color = Color(0xFF4AC380),
            defaultAlias = "ModConveyorEvent",
            defaultObjClass = "ModifyConveyorWaveActionProps",
            initialDataFactory = { ModifyConveyorWaveActionData() },
            summaryProvider = { obj ->
                try {
                    val gson = com.google.gson.Gson()
                    val data = gson.fromJson(obj.objData, ModifyConveyorWaveActionData::class.java)
                    "+${data.addList.size} / -${data.removeList.size}"
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
            defaultAlias = "JitteredWave",
            defaultObjClass = "SpawnZombiesJitteredWaveActionProps",
            initialDataFactory = { WaveActionData() },
            summaryProvider = { obj ->
                try {
                    val gson = com.google.gson.Gson()
                    val data = gson.fromJson(obj.objData, WaveActionData::class.java)
                    "${data.zombies.size} 僵尸"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "SpawnZombiesFromGroundSpawnerProps" to EventMetadata(
            title = "地底出怪",
            description = "从地下出现的出怪事件",
            icon = Icons.Default.Groups,
            color = Color(0xFF936457),
            defaultAlias = "GroundSpawner",
            defaultObjClass = "SpawnZombiesFromGroundSpawnerProps",
            initialDataFactory = { WaveActionData() },
            summaryProvider = { obj ->
                try {
                    val gson = com.google.gson.Gson()
                    val data = gson.fromJson(obj.objData, SpawnZombiesFromGroundData::class.java)
                    "${data.zombies.size} 僵尸"
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
            defaultAlias = "PortalEvent",
            defaultObjClass = "SpawnModernPortalsWaveActionProps",
            initialDataFactory = { PortalEventData() },
            summaryProvider = { obj ->
                try {
                    val gson = com.google.gson.Gson()
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
            defaultAlias = "StormEvent",
            defaultObjClass = "StormZombieSpawnerProps",
            initialDataFactory = { StormZombieSpawnerPropsData() },
            summaryProvider = { obj ->
                try {
                    val gson = com.google.gson.Gson()
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
            defaultAlias = "RaidingPartyEvent",
            defaultObjClass = "RaidingPartyZombieSpawnerProps",
            initialDataFactory = { StormZombieSpawnerPropsData() },
            summaryProvider = { obj ->
                try {
                    val gson = com.google.gson.Gson()
                    val data = gson.fromJson(obj.objData, RaidingPartyEventData::class.java)
                    "${data.swashbucklerCount} 僵尸"
                } catch (_: Exception) {
                    "解析错误"
                }
            }
        ),
        "ParachuteRainZombieSpawnerProps" to EventMetadata(
            title = "空降突袭",
            description = "僵尸依靠降落伞或绳索从天而降",
            icon = Icons.Default.Air,
            color = Color(0xFFFF9800),
            defaultAlias = "ParachuteRainEvent",
            defaultObjClass = "ParachuteRainZombieSpawnerProps",
            initialDataFactory = { ParachuteRainEventData() },
            summaryProvider = { obj ->
                try {
                    val gson = com.google.gson.Gson()
                    val data = gson.fromJson(obj.objData, ParachuteRainEventData::class.java)
                    "${data.spiderCount} 只空降"
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
data class ModuleMetadata(
    val title: String,          // 显示标题 (如 "阳光掉落")
    val description: String,    // 简短描述
    val icon: ImageVector,      // 图标
    val isCore: Boolean,        // 是否为核心模块 (大卡片显示)

    val defaultAlias: String,
    val defaultSource: String = "CurrentLevel",

    val initialDataFactory: (() -> Any)? = null,
    val navigationFactory: (String) -> EditorSubScreen
)

/**
 * ==========================================
 * 3. 模块注册表 (核心逻辑)
 * ==========================================
 */
object ModuleRegistry {

    // 默认回退配置 (用于未知模块)
    private val DEFAULT_METADATA = ModuleMetadata(
        title = "未知模块",
        description = "通用参数编辑器",
        icon = Icons.Default.Extension,
        isCore = false,
        defaultAlias = "Unknown",
        navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
    )

    // 注册表：Key = objClass (字符串)
    private val registry = mapOf(
        "SunDropperProperties" to ModuleMetadata(
            title = "阳光掉落",
            description = "控制天上掉落阳光的频率",
            icon = Icons.Default.WbSunny,
            isCore = true,
            defaultAlias = "DefaultSunDropper",
            defaultSource = "LevelModules",
            navigationFactory = { rtid -> EditorSubScreen.SunDropper(rtid) }
        ),
        "SeedBankProperties" to ModuleMetadata(
            title = "种子库",
            description = "预设卡槽植物与选卡方式",
            icon = Icons.Default.Yard,
            isCore = true,
            defaultAlias = "SeedBank",
            initialDataFactory = { SeedBankData() },
            navigationFactory = { rtid -> EditorSubScreen.SeedBank(rtid) }
        ),
        "ConveyorSeedBankProperties" to ModuleMetadata(
            title = "传送带",
            description = "预设传送带植物种类和权重",
            icon = Icons.Default.LinearScale,
            isCore = true,
            defaultAlias = "ConveyorBelt",
            initialDataFactory = { ConveyorBeltData() },
            navigationFactory = { rtid -> EditorSubScreen.ConveyorBelt(rtid) }
        ),
        "WaveManagerModuleProperties" to ModuleMetadata(
            title = "波次管理器",
            description = "点数出怪的全局配置",
            icon = Icons.Default.Timeline,
            isCore = true,
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
        "InitialPlantEntryProperties" to ModuleMetadata(
            title = "预置植物",
            description = "关卡开始时场上已存在的植物",
            icon = Icons.Default.LocalFlorist,
            isCore = true,
            defaultAlias = "InitialPlants",
            defaultSource = "CurrentLevel",
            initialDataFactory = { InitialPlantEntryData() },
            navigationFactory = { rtid -> EditorSubScreen.InitialPlantEntry(rtid) }
        ),
        "InitialZombieProperties" to ModuleMetadata(
            title = "预置僵尸",
            description = "关卡开始时场上已存在的僵尸",
            icon = Icons.Default.DirectionsWalk,
            isCore = true,
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
            defaultAlias = "GridItemPlacement",
            defaultSource = "CurrentLevel",
            initialDataFactory = { InitialGridItemEntryData() },
            navigationFactory = { rtid -> EditorSubScreen.InitialGridItemEntry(rtid) }
        ),
        "SunBombChallengeProperties" to ModuleMetadata(
            title = "太阳炸弹",
            description = "配置掉落的太阳爆炸范围和伤害",
            icon = Icons.Default.BrightnessHigh,
            isCore = true,
            defaultAlias = "SunBombs",
            defaultSource = "CurrentLevel",
            initialDataFactory = { SunBombChallengeData() },
            navigationFactory = { rtid -> EditorSubScreen.SunBombChallenge(rtid) }
        ),
        "StarChallengeModuleProperties" to ModuleMetadata(
            title = "挑战模块",
            description = "设置关卡的限制条件与挑战目标",
            icon = Icons.AutoMirrored.Filled.FactCheck,
            isCore = true,
            defaultAlias = "ChallengeModule",
            defaultSource = "CurrentLevel",
            initialDataFactory = { StarChallengeModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.StarChallenge(rtid) }
        ),


        "CustomLevelModuleProperties" to ModuleMetadata(
            title = "庭院模块",
            description = "开启后关卡适配庭院框架",
            icon = Icons.Default.Home,
            isCore = false,
            defaultAlias = "DefaultCustomLevel",
            defaultSource = "LevelModules",
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "StandardLevelIntroProperties" to ModuleMetadata(
            title = "转场动画",
            description = "关卡开始时的摄像机平移",
            icon = Icons.Default.MovieFilter,
            isCore = false,
            defaultAlias = "StandardIntro",
            defaultSource = "LevelModules",
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "ZombiesAteYourBrainsProperties" to ModuleMetadata(
            title = "失败判定",
            description = "僵尸进屋判负的位置",
            icon = Icons.Default.Dangerous,
            isCore = false,
            defaultAlias = "DefaultZombieWinCondition",
            defaultSource = "LevelModules",
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "ZombiesDeadWinConProperties" to ModuleMetadata(
            title = "死亡掉落",
            description = "关卡稳定运行必须模块",
            icon = Icons.Default.Redeem,
            isCore = false,
            defaultAlias = "ZombiesDeadWinCon",
            defaultSource = "LevelModules",
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        // 可以在此继续添加更多模块...
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