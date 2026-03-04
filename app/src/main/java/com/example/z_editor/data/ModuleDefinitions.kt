package com.example.z_editor.data

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.NextPlan
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.BlurCircular
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
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
import androidx.compose.material.icons.filled.PinDrop
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
import androidx.compose.material.icons.filled.Timer
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
import com.example.z_editor.R
import com.example.z_editor.data.BungeeTarget
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
    data class LawnMowerDetail(val rtid: String) : EditorSubScreen()
    data class LastStandMinigame(val rtid: String) : EditorSubScreen()
    data class SunDropper(val rtid: String) : EditorSubScreen()
    data class SunBombChallenge(val rtid: String) : EditorSubScreen()
    data class SeedBank(val rtid: String) : EditorSubScreen()
    data class ConveyorBelt(val rtid: String) : EditorSubScreen()
    data class WaveManagerModule(val rtid: String) : EditorSubScreen()
    data class InitialPlantProperties(val rtid: String) : EditorSubScreen()
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
    data class ZombieRushModule(val rtid: String) : EditorSubScreen()
    data class MaxSunModule(val rtid: String) : EditorSubScreen()
    data class StartingPlantfoodModule(val rtid: String) : EditorSubScreen()
    data class BowlingMinigameModule(val rtid: String) : EditorSubScreen()
    data class PennyClassroomModule(val rtid: String) : EditorSubScreen()
    data class SeedRainModule(val rtid: String) : EditorSubScreen()
    data class TunnelDefendModule(val rtid: String) : EditorSubScreen()
    data class StarChallenge(val rtid: String) : EditorSubScreen()
    data class PickupCollectableTutorial(val rtid: String) : EditorSubScreen()
    data class RiftTimedSunModule(val rtid: String) : EditorSubScreen()

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
    data class BungeeActionDetail(val rtid: String, val waveIndex: Int) : EditorSubScreen()
    data class InvalidEvent(val rtid: String, val waveIndex: Int) : EditorSubScreen()
}


data class EventMetadata(
    val title: Int,
    val description: Int,
    val icon: ImageVector,
    val color: Color,
    val darkColor: Color,
    val defaultAlias: String,
    val defaultObjClass: String,
    val initialDataFactory: () -> Any,
    val summaryProvider: ((Context, PvzObject) -> String)? = null
)

object EventRegistry {
    private val registry = mapOf(
        "SpawnZombiesFromGroundSpawnerProps" to EventMetadata(
            title = R.string.event_ground_spawner_title,
            description = R.string.event_ground_spawner_desc,
            icon = Icons.Default.Groups,
            color = Color(0xFF936457),
            darkColor = Color(0xFFC2A197),
            defaultAlias = "GroundSpawner",
            defaultObjClass = "SpawnZombiesFromGroundSpawnerProps",
            initialDataFactory = { WaveActionData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, SpawnZombiesFromGroundData::class.java)
                    context.getString(R.string.event_format_zombies, data.zombies.size)
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "SpawnZombiesJitteredWaveActionProps" to EventMetadata(
            title = R.string.event_jittered_title,
            description = R.string.event_jittered_desc,
            icon = Icons.Default.Groups,
            color = Color(0xFF2196F3),
            darkColor = Color(0xFF90CAF9),
            defaultAlias = "Jittered",
            defaultObjClass = "SpawnZombiesJitteredWaveActionProps",
            initialDataFactory = { WaveActionData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, WaveActionData::class.java)
                    context.getString(R.string.event_format_zombies, data.zombies.size)
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "FrostWindWaveActionProps" to EventMetadata(
            title = R.string.event_frost_wind_title,
            description = R.string.event_frost_wind_desc,
            icon = Icons.Default.AcUnit,
            color = Color(0xFF0288D1),
            darkColor = Color(0xFF90CAF9),
            defaultAlias = "FrostWindEvent",
            defaultObjClass = "FrostWindWaveActionProps",
            initialDataFactory = { FrostWindWaveActionPropsData() },
            summaryProvider = { context, obj ->
                try {
                    val data =
                        Gson().fromJson(obj.objData, FrostWindWaveActionPropsData::class.java)
                    context.getString(R.string.event_format_winds, data.winds.size)
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "BeachStageEventZombieSpawnerProps" to EventMetadata(
            title = R.string.event_low_tide_title,
            description = R.string.event_low_tide_desc,
            icon = Icons.Default.Water,
            color = Color(0xFF00ACC1),
            darkColor = Color(0xFF81D4FA),
            defaultAlias = "LowTideEvent",
            defaultObjClass = "BeachStageEventZombieSpawnerProps",
            initialDataFactory = { BeachStageEventData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, BeachStageEventData::class.java)
                    context.getString(R.string.event_format_zombies, data.zombieCount)
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "TidalChangeWaveActionProps" to EventMetadata(
            title = R.string.event_tide_change_title,
            description = R.string.event_tide_change_desc,
            icon = Icons.Default.WaterDrop,
            color = Color(0xFF00ACC1),
            darkColor = Color(0xFF81D4FA),
            defaultAlias = "TidalChangeEvent",
            defaultObjClass = "TidalChangeWaveActionProps",
            initialDataFactory = { TidalChangeWaveActionData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, TidalChangeWaveActionData::class.java)
                    context.getString(
                        R.string.event_format_position,
                        data.tidalChange.changeAmount.toString()
                    )
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "ModifyConveyorWaveActionProps" to EventMetadata(
            title = R.string.event_conveyor_mod_title,
            description = R.string.event_conveyor_mod_desc,
            icon = Icons.Default.Transform,
            color = Color(0xFF4AC380),
            darkColor = Color(0xFF7CBD99),
            defaultAlias = "ModConveyorEvent",
            defaultObjClass = "ModifyConveyorWaveActionProps",
            initialDataFactory = { ModifyConveyorWaveActionData() },
            summaryProvider = { context, obj ->
                try {
                    val data =
                        Gson().fromJson(obj.objData, ModifyConveyorWaveActionData::class.java)
                    context.getString(
                        R.string.event_format_conveyor,
                        data.addList.size,
                        data.removeList.size
                    )
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "DinoWaveActionProps" to EventMetadata(
            title = R.string.event_dino_summon_title,
            description = R.string.event_dino_summon_desc,
            icon = Icons.Default.Pets,
            color = Color(0xFF91B900),
            darkColor = Color(0xFFA2B659),
            defaultAlias = "DinoTimeEvent",
            defaultObjClass = "DinoWaveActionProps",
            initialDataFactory = { DinoWaveActionPropsData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, DinoWaveActionPropsData::class.java)
                    val typeMap = mapOf(
                        "raptor" to context.getString(R.string.event_dino_raptor),
                        "stego" to context.getString(R.string.event_dino_stego),
                        "ptero" to context.getString(R.string.event_dino_ptero),
                        "tyranno" to context.getString(R.string.event_dino_tyranno),
                        "ankylo" to context.getString(R.string.event_dino_ankylo)
                    )
                    "${typeMap[data.dinoType] ?: data.dinoType} ${data.dinoRow + 1}"
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "BungeeWaveActionProps" to EventMetadata(
            title = R.string.event_bungee_title,
            description = R.string.event_bungee_desc,
            icon = Icons.Default.PinDrop,
            color = Color(0xFFFF9800),
            darkColor = Color(0xFFFFCC80),
            defaultAlias = "BungeeActionEvent",
            defaultObjClass = "BungeeWaveActionProps",
            initialDataFactory = { BungeeWaveActionData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, BungeeWaveActionData::class.java)
                    "${data.target.mX}, ${data.target.mY}"
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "SpawnModernPortalsWaveActionProps" to EventMetadata(
            title = R.string.event_portal_title,
            description = R.string.event_portal_desc,
            icon = Icons.Default.HourglassEmpty,
            color = Color(0xFFFF9800),
            darkColor = Color(0xFFFFCC80),
            defaultAlias = "PortalEvent",
            defaultObjClass = "SpawnModernPortalsWaveActionProps",
            initialDataFactory = { PortalEventData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, PortalEventData::class.java)
                    "${data.portalColumn}, ${data.portalRow}"
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "StormZombieSpawnerProps" to EventMetadata(
            title = R.string.event_storm_raid_title,
            description = R.string.event_storm_raid_desc,
            icon = Icons.Default.Storm,
            color = Color(0xFFFF9800),
            darkColor = Color(0xFFFFCC80),
            defaultAlias = "StormEvent",
            defaultObjClass = "StormZombieSpawnerProps",
            initialDataFactory = { StormZombieSpawnerPropsData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, StormZombieSpawnerPropsData::class.java)
                    context.getString(R.string.event_format_zombies, data.zombies.size)
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "RaidingPartyZombieSpawnerProps" to EventMetadata(
            title = R.string.event_pirate_raid_title,
            description = R.string.event_pirate_raid_desc,
            icon = Icons.Default.Tsunami,
            color = Color(0xFFFF9800),
            darkColor = Color(0xFFFFCC80),
            defaultAlias = "RaidingPartyEvent",
            defaultObjClass = "RaidingPartyZombieSpawnerProps",
            initialDataFactory = { RaidingPartyEventData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, RaidingPartyEventData::class.java)
                    context.getString(R.string.event_format_zombies, data.swashbucklerCount)
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "ZombiePotionActionProps" to EventMetadata(
            title = R.string.event_potion_drop_title,
            description = R.string.event_potion_drop_desc,
            icon = Icons.Default.Science,
            color = Color(0xFF607D8B),
            darkColor = Color(0xFFB0BEC5),
            defaultAlias = "PotionEvent",
            defaultObjClass = "ZombiePotionActionProps",
            initialDataFactory = { ZombiePotionActionPropsData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, ZombiePotionActionPropsData::class.java)
                    context.getString(R.string.event_format_items, data.potions.size)
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "SpawnGravestonesWaveActionProps" to EventMetadata(
            title = R.string.event_obstacle_gen_title,
            description = R.string.event_obstacle_gen_desc,
            icon = Icons.Filled.Unarchive,
            color = Color(0xFF607D8B),
            darkColor = Color(0xFFB0BEC5),
            defaultAlias = "GravestonesEvent",
            defaultObjClass = "SpawnGravestonesWaveActionProps",
            initialDataFactory = { SpawnGraveStonesData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, SpawnGraveStonesData::class.java)
                    context.getString(R.string.event_format_types, data.gravestonePool.size)
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "SpawnZombiesFromGridItemSpawnerProps" to EventMetadata(
            title = R.string.event_obstacle_spawner_title,
            description = R.string.event_obstacle_spawner_desc,
            icon = Icons.Default.Groups,
            color = Color(0xFF607D8B),
            darkColor = Color(0xFFB0BEC5),
            defaultAlias = "GraveSpawner",
            defaultObjClass = "SpawnZombiesFromGridItemSpawnerProps",
            initialDataFactory = { SpawnZombiesFromGridItemData() },
            summaryProvider = { context, obj ->
                try {
                    val data =
                        Gson().fromJson(obj.objData, SpawnZombiesFromGridItemData::class.java)
                    context.getString(R.string.event_format_zombies, data.zombies.size)
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "FairyTaleFogWaveActionProps" to EventMetadata(
            title = R.string.event_fairy_fog_title,
            description = R.string.event_fairy_fog_desc,
            icon = Icons.Default.Cloud,
            color = Color(0xFFBE5DBA),
            darkColor = Color(0xFFBD99BB),
            defaultAlias = "FairyFogEvent",
            defaultObjClass = "FairyTaleFogWaveActionProps",
            initialDataFactory = { FairyTaleFogWaveActionData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, FairyTaleFogWaveActionData::class.java)
                    "mX: ${data.range.mX}"
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "FairyTaleWindWaveActionProps" to EventMetadata(
            title = R.string.event_fairy_wind_title,
            description = R.string.event_fairy_wind_desc,
            icon = Icons.Default.Air,
            color = Color(0xFFBE5DBA),
            darkColor = Color(0xFFBD99BB),
            defaultAlias = "WindEvent",
            defaultObjClass = "FairyTaleWindWaveActionProps",
            initialDataFactory = { FairyTaleWindWaveActionData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, FairyTaleWindWaveActionData::class.java)
                    context.getString(R.string.event_format_seconds, data.duration)
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "SpiderRainZombieSpawnerProps" to EventMetadata(
            title = R.string.event_imp_rain_title,
            description = R.string.event_imp_rain_desc,
            icon = Icons.Default.BugReport,
            color = Color(0xFF9C27B0),
            darkColor = Color(0xFFB39DDB),
            defaultAlias = "SpiderRainEvent",
            defaultObjClass = "SpiderRainZombieSpawnerProps",
            initialDataFactory = { ParachuteRainEventData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, ParachuteRainEventData::class.java)
                    context.getString(R.string.event_format_zombies, data.spiderCount)
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "ParachuteRainZombieSpawnerProps" to EventMetadata(
            title = R.string.event_parachute_rain_title,
            description = R.string.event_parachute_rain_desc,
            icon = Icons.Default.AirplanemodeActive,
            color = Color(0xFF9C27B0),
            darkColor = Color(0xFFB39DDB),
            defaultAlias = "ParachuteRainEvent",
            defaultObjClass = "ParachuteRainZombieSpawnerProps",
            initialDataFactory = { ParachuteRainEventData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, ParachuteRainEventData::class.java)
                    context.getString(R.string.event_format_zombies, data.spiderCount)
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "BassRainZombieSpawnerProps" to EventMetadata(
            title = R.string.event_bass_rain_title,
            description = R.string.event_bass_rain_desc,
            icon = Icons.Default.Speaker,
            color = Color(0xFF9C27B0),
            darkColor = Color(0xFFB39DDB),
            defaultAlias = "BassRainEvent",
            defaultObjClass = "BassRainZombieSpawnerProps",
            initialDataFactory = { ParachuteRainEventData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, ParachuteRainEventData::class.java)
                    context.getString(R.string.event_format_zombies, data.spiderCount)
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "BlackHoleWaveActionProps" to EventMetadata(
            title = R.string.event_black_hole_title,
            description = R.string.event_black_hole_desc,
            icon = Icons.Default.BlurCircular,
            color = Color(0xFF7C30D9),
            darkColor = Color(0xFFA179D2),
            defaultAlias = "BlackHoleEvent",
            defaultObjClass = "BlackHoleWaveActionProps",
            initialDataFactory = { BlackHoleEventData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, BlackHoleEventData::class.java)
                    context.getString(R.string.event_format_columns, data.colNumPlantIsDragged)
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
                }
            }
        ),
        "WaveActionMagicMirrorTeleportationArrayProps2" to EventMetadata(
            title = R.string.event_magic_mirror_title,
            description = R.string.event_magic_mirror_desc,
            icon = Icons.AutoMirrored.Filled.CompareArrows,
            color = Color(0xFF7C30D9),
            darkColor = Color(0xFFA179D2),
            defaultAlias = "MirrorEvent",
            defaultObjClass = "WaveActionMagicMirrorTeleportationArrayProps2",
            initialDataFactory = { MagicMirrorWaveActionData() },
            summaryProvider = { context, obj ->
                try {
                    val data = Gson().fromJson(obj.objData, MagicMirrorWaveActionData::class.java)
                    context.getString(R.string.event_format_mirrors, data.arrays.size)
                } catch (_: Exception) {
                    context.getString(R.string.event_error_parse)
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
enum class ModuleCategory(@StringRes val titleRes: Int) {
    Base(R.string.module_category_base),
    Mode(R.string.module_category_mode),
    Scene(R.string.module_category_scene),
}

data class ModuleMetadata(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
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
        titleRes = R.string.module_unknown_title,
        descriptionRes = R.string.module_unknown_desc,
        icon = Icons.Default.Extension,
        isCore = false,
        category = ModuleCategory.Base,
        defaultAlias = "Unknown",
        navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
    )

    private val registry = mapOf(
        "WaveManagerModuleProperties" to ModuleMetadata(
            titleRes = R.string.module_wave_manager_title,
            descriptionRes = R.string.module_wave_manager_desc,
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
            titleRes = R.string.module_custom_level_title,
            descriptionRes = R.string.module_custom_level_desc,
            icon = Icons.Default.Home,
            isCore = false,
            category = ModuleCategory.Base,
            defaultAlias = "DefaultCustomLevel",
            defaultSource = "LevelModules",
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "StandardLevelIntroProperties" to ModuleMetadata(
            titleRes = R.string.module_intro_title,
            descriptionRes = R.string.module_intro_desc,
            icon = Icons.Default.MovieFilter,
            isCore = false,
            category = ModuleCategory.Base,
            defaultAlias = "StandardIntro",
            defaultSource = "LevelModules",
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "ZombiesAteYourBrainsProperties" to ModuleMetadata(
            titleRes = R.string.module_zombies_ate_title,
            descriptionRes = R.string.module_zombies_ate_desc,
            icon = Icons.Default.Dangerous,
            isCore = false,
            category = ModuleCategory.Base,
            defaultAlias = "DefaultZombieWinCondition",
            defaultSource = "LevelModules",
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "ZombiesDeadWinConProperties" to ModuleMetadata(
            titleRes = R.string.module_zombies_dead_title,
            descriptionRes = R.string.module_zombies_dead_desc,
            icon = Icons.Default.Redeem,
            isCore = false,
            category = ModuleCategory.Base,
            defaultAlias = "ZombiesDeadWinCon",
            defaultSource = "LevelModules",
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "PennyClassroomModuleProperties" to ModuleMetadata(
            titleRes = R.string.module_penny_classroom_title,
            descriptionRes = R.string.module_penny_classroom_desc,
            icon = Icons.Default.Layers,
            isCore = true,
            category = ModuleCategory.Base,
            defaultAlias = "PennyClassroom",
            defaultSource = "CurrentLevel",
            initialDataFactory = { PennyClassroomModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.PennyClassroomModule(rtid) }
        ),
        "SeedBankProperties" to ModuleMetadata(
            titleRes = R.string.module_seed_bank_title,
            descriptionRes = R.string.module_seed_bank_desc,
            icon = Icons.Default.Yard,
            isCore = true,
            allowMultiple = true,
            category = ModuleCategory.Base,
            defaultAlias = "SeedBank",
            initialDataFactory = { SeedBankData() },
            navigationFactory = { rtid -> EditorSubScreen.SeedBank(rtid) }
        ),
        "ConveyorSeedBankProperties" to ModuleMetadata(
            titleRes = R.string.module_conveyor_title,
            descriptionRes = R.string.module_conveyor_desc,
            icon = Icons.Default.LinearScale,
            isCore = true,
            category = ModuleCategory.Base,
            defaultAlias = "ConveyorBelt",
            initialDataFactory = { ConveyorBeltData() },
            navigationFactory = { rtid -> EditorSubScreen.ConveyorBelt(rtid) }
        ),
        "SunDropperProperties" to ModuleMetadata(
            titleRes = R.string.module_sun_dropper_title,
            descriptionRes = R.string.module_sun_dropper_desc,
            icon = Icons.Default.WbSunny,
            isCore = true,
            category = ModuleCategory.Base,
            defaultAlias = "DefaultSunDropper",
            defaultSource = "LevelModules",
            navigationFactory = { rtid -> EditorSubScreen.SunDropper(rtid) }
        ),
        "LevelMutatorMaxSunProps" to ModuleMetadata(
            titleRes = R.string.module_max_sun_title,
            descriptionRes = R.string.module_max_sun_desc,
            icon = Icons.Default.BrightnessHigh,
            isCore = true,
            category = ModuleCategory.Base,
            defaultAlias = "OverrideMaxSun",
            defaultSource = "CurrentLevel",
            initialDataFactory = { LevelMutatorMaxSunPropsData() },
            navigationFactory = { rtid -> EditorSubScreen.MaxSunModule(rtid) }
        ),
        "LevelMutatorStartingPlantfoodProps" to ModuleMetadata(
            titleRes = R.string.module_starting_plantfood_title,
            descriptionRes = R.string.module_starting_plantfood_desc,
            icon = Icons.Default.Eco,
            isCore = true,
            category = ModuleCategory.Base,
            defaultAlias = "OverrideStartingPlantFood",
            defaultSource = "CurrentLevel",
            initialDataFactory = { LevelMutatorStartingPlantfoodPropsData() },
            navigationFactory = { rtid -> EditorSubScreen.StartingPlantfoodModule(rtid) }
        ),
        "StarChallengeModuleProperties" to ModuleMetadata(
            titleRes = R.string.module_star_challenge_title,
            descriptionRes = R.string.module_star_challenge_desc,
            icon = Icons.AutoMirrored.Filled.FactCheck,
            isCore = true,
            category = ModuleCategory.Base,
            defaultAlias = "ChallengeModule",
            defaultSource = "CurrentLevel",
            initialDataFactory = { StarChallengeModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.StarChallenge(rtid) }
        ),
        "LawnMowerProperties" to ModuleMetadata(
            titleRes = R.string.module_lawn_mower_title,
            descriptionRes = R.string.module_lawn_mower_desc,
            icon = Icons.Default.CleaningServices,
            isCore = true,
            category = ModuleCategory.Base,
            defaultAlias = "ModernMowers",
            defaultSource = "LevelModules",
            navigationFactory = { rtid -> EditorSubScreen.LawnMowerDetail(rtid) }
        ),
        "LevelScoringModuleProperties" to ModuleMetadata(
            titleRes = R.string.module_scoring_title,
            descriptionRes = R.string.module_scoring_desc,
            icon = Icons.Default.Scoreboard,
            isCore = false,
            category = ModuleCategory.Base,
            defaultAlias = "LevelScoring",
            defaultSource = "CurrentLevel",
            initialDataFactory = { LevelScoringData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),

        "BowlingMinigameProperties" to ModuleMetadata(
            titleRes = R.string.module_bowling_beach_title,
            descriptionRes = R.string.module_bowling_beach_desc,
            icon = Icons.Default.SportsEsports,
            isCore = true,
            category = ModuleCategory.Mode,
            defaultAlias = "BowlingBulbMinigame",
            defaultSource = "CurrentLevel",
            initialDataFactory = { BowlingMinigamePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.BowlingMinigameModule(rtid) }
        ),
        "NewBowlingMinigameProperties" to ModuleMetadata(
            titleRes = R.string.module_bowling_nut_title,
            descriptionRes = R.string.module_bowling_nut_desc,
            icon = Icons.Default.SportsEsports,
            isCore = false,
            category = ModuleCategory.Mode,
            defaultAlias = "NewBowlingBulbMinigame",
            defaultSource = "CurrentLevel",
            initialDataFactory = { NewBowlingMinigamePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "VaseBreakerPresetProperties" to ModuleMetadata(
            titleRes = R.string.module_vase_preset_title,
            descriptionRes = R.string.module_vase_preset_desc,
            icon = Icons.Default.Grid4x4,
            isCore = false,
            category = ModuleCategory.Mode,
            defaultAlias = "VaseBreakerProps",
            defaultSource = "CurrentLevel",
            initialDataFactory = { VaseBreakerPresetData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "VaseBreakerArcadeModuleProperties" to ModuleMetadata(
            titleRes = R.string.module_vase_arcade_title,
            descriptionRes = R.string.module_vase_arcade_desc,
            icon = Icons.Default.SportsEsports,
            isCore = false,
            category = ModuleCategory.Mode,
            defaultAlias = "VaseBreakerArcade",
            defaultSource = "LevelModules",
            initialDataFactory = { VaseBreakerArcadeModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "VaseBreakerFlowModuleProperties" to ModuleMetadata(
            titleRes = R.string.module_vase_flow_title,
            descriptionRes = R.string.module_vase_flow_desc,
            icon = Icons.AutoMirrored.Filled.NextPlan,
            isCore = false,
            category = ModuleCategory.Mode,
            defaultAlias = "VaseBreakerFlow",
            defaultSource = "LevelModules",
            initialDataFactory = { VaseBreakerFlowModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "EvilDaveProperties" to ModuleMetadata(
            titleRes = R.string.module_izombie_title,
            descriptionRes = R.string.module_izombie_desc,
            icon = Icons.Default.EmojiPeople,
            isCore = false,
            category = ModuleCategory.Mode,
            defaultAlias = "EvilDave",
            defaultSource = "CurrentLevel",
            initialDataFactory = { EvilDavePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "ZombossBattleModuleProperties" to ModuleMetadata(
            titleRes = R.string.module_zomboss_battle_title,
            descriptionRes = R.string.module_zomboss_battle_desc,
            icon = Icons.Default.Dangerous,
            isCore = false,
            category = ModuleCategory.Mode,
            defaultAlias = "ZombossBattle",
            defaultSource = "CurrentLevel",
            initialDataFactory = { ZombossBattleModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "ZombossBattleIntroProperties" to ModuleMetadata(
            titleRes = R.string.module_zomboss_intro_title,
            descriptionRes = R.string.module_zomboss_intro_desc,
            icon = Icons.Default.MovieFilter,
            isCore = false,
            category = ModuleCategory.Mode,
            defaultAlias = "ZombossBattleIntro",
            defaultSource = "CurrentLevel",
            initialDataFactory = { ZombossBattleIntroData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "SeedRainProperties" to ModuleMetadata(
            titleRes = R.string.module_seed_rain_title,
            descriptionRes = R.string.module_seed_rain_desc,
            icon = Icons.Default.Thunderstorm,
            isCore = true,
            category = ModuleCategory.Mode,
            defaultAlias = "SeedRain",
            defaultSource = "CurrentLevel",
            initialDataFactory = { SeedRainPropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.SeedRainModule(rtid) }
        ),
        "LastStandMinigameProperties" to ModuleMetadata(
            titleRes = R.string.module_last_stand_title,
            descriptionRes = R.string.module_last_stand_desc,
            icon = Icons.Default.Shield,
            isCore = true,
            category = ModuleCategory.Mode,
            defaultAlias = "LastStand",
            defaultSource = "CurrentLevel",
            initialDataFactory = { LastStandMinigamePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.LastStandMinigame(rtid) }
        ),
        "PVZ1OverwhelmModuleProperties" to ModuleMetadata(
            titleRes = R.string.module_overwhelm_title,
            descriptionRes = R.string.module_overwhelm_desc,
            icon = Icons.Default.LocalFlorist,
            isCore = false,
            category = ModuleCategory.Mode,
            defaultAlias = "PVZ1Overwhelm",
            defaultSource = "CurrentLevel",
            initialDataFactory = { PVZ1OverwhelmModulePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.UnknownDetail(rtid) }
        ),
        "SunBombChallengeProperties" to ModuleMetadata(
            titleRes = R.string.module_sun_bomb_title,
            descriptionRes = R.string.module_sun_bomb_desc,
            icon = Icons.Default.BrightnessHigh,
            isCore = true,
            category = ModuleCategory.Mode,
            defaultAlias = "SunBombs",
            defaultSource = "CurrentLevel",
            initialDataFactory = { SunBombChallengeData() },
            navigationFactory = { rtid -> EditorSubScreen.SunBombChallenge(rtid) }
        ),
        "IncreasedCostModuleProperties" to ModuleMetadata(
            titleRes = R.string.module_increased_cost_title,
            descriptionRes = R.string.module_increased_cost_desc,
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            isCore = true,
            category = ModuleCategory.Mode,
            defaultAlias = "IncreasedCostModule",
            defaultSource = "CurrentLevel",
            initialDataFactory = { IncreasedCostModulePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.IncreasedCostModule(rtid) }
        ),
        "DeathHoleModuleProperties" to ModuleMetadata(
            titleRes = R.string.module_death_hole_title,
            descriptionRes = R.string.module_death_hole_desc,
            icon = Icons.Default.TripOrigin,
            isCore = true,
            category = ModuleCategory.Mode,
            defaultAlias = "DeathHoleModule",
            defaultSource = "CurrentLevel",
            initialDataFactory = { DeathHoleModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.DeathHoleModule(rtid) }
        ),
        "ZombieMoveFastModuleProperties" to ModuleMetadata(
            titleRes = R.string.module_zombie_fast_title,
            descriptionRes = R.string.module_zombie_fast_desc,
            icon = Icons.Default.FastForward,
            isCore = true,
            category = ModuleCategory.Mode,
            defaultAlias = "FastSpeed",
            defaultSource = "CurrentLevel",
            initialDataFactory = { ZombieMoveFastModulePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.ZombieMoveFastModule(rtid) }
        ),
        "ZombieRushModuleProperties" to ModuleMetadata(
            titleRes = R.string.module_zombie_rush_title,
            descriptionRes = R.string.module_zombie_rush_desc,
            icon = Icons.Default.Timer,
            isCore = true,
            category = ModuleCategory.Mode,
            defaultAlias = "ZombieRushModule",
            defaultSource = "CurrentLevel",
            initialDataFactory = { ZombieRushModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.ZombieRushModule(rtid) }
        ),
        "PickupCollectableTutorialProperties" to ModuleMetadata(
            titleRes = R.string.module_pick_coin_title,
            descriptionRes = R.string.module_pick_coin_desc,
            icon = Icons.AutoMirrored.Filled.Message,
            isCore = true,
            category = ModuleCategory.Mode,
            defaultAlias = "FirstCoinTutorial",
            defaultSource = "CurrentLevel",
            initialDataFactory = { PickupCollectableTutorialData() },
            navigationFactory = { rtid -> EditorSubScreen.PickupCollectableTutorial(rtid) }
        ),
        "LevelMutatorRiftTimedSunProps" to ModuleMetadata(
            titleRes = R.string.module_rift_sun_title,
            descriptionRes = R.string.module_rift_sun_desc,
            icon = Icons.Default.WbSunny,
            isCore = true,
            category = ModuleCategory.Mode,
            defaultAlias = "OverrideRiftTimedSun",
            defaultSource = "CurrentLevel",
            initialDataFactory = { RiftTimedSunModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.RiftTimedSunModule(rtid) }
        ),

        "InitialPlantProperties" to ModuleMetadata(
            titleRes = R.string.module_initial_plant_old_title,
            descriptionRes = R.string.module_initial_plant_old_desc,
            icon = Icons.Default.Widgets,
            isCore = true,
            allowMultiple = true,
            category = ModuleCategory.Scene,
            defaultAlias = "FrozenPlantPlacement",
            defaultSource = "CurrentLevel",
            initialDataFactory = { InitialPlantPropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.InitialPlantProperties(rtid) }
        ),
        "InitialPlantEntryProperties" to ModuleMetadata(
            titleRes = R.string.module_initial_plant_title,
            descriptionRes = R.string.module_initial_plant_desc,
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
            titleRes = R.string.module_initial_zombie_title,
            descriptionRes = R.string.module_initial_zombie_desc,
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
            titleRes = R.string.module_initial_grid_item_title,
            descriptionRes = R.string.module_initial_grid_item_desc,
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
            titleRes = R.string.module_protect_plant_title,
            descriptionRes = R.string.module_protect_plant_desc,
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
            titleRes = R.string.module_protect_item_title,
            descriptionRes = R.string.module_protect_item_desc,
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
            titleRes = R.string.module_zombie_potion_title,
            descriptionRes = R.string.module_zombie_potion_desc,
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
            titleRes = R.string.module_pirate_plank_title,
            descriptionRes = R.string.module_pirate_plank_desc,
            icon = Icons.Default.EditRoad,
            isCore = true,
            category = ModuleCategory.Scene,
            defaultAlias = "PiratePlanks",
            defaultSource = "CurrentLevel",
            initialDataFactory = { PiratePlankPropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.PiratePlank(rtid) }
        ),
        "RailcartProperties" to ModuleMetadata(
            titleRes = R.string.module_railcart_title,
            descriptionRes = R.string.module_railcart_desc,
            icon = Icons.Default.EditRoad,
            isCore = true,
            category = ModuleCategory.Scene,
            defaultAlias = "Railcarts",
            defaultSource = "CurrentLevel",
            initialDataFactory = { RailcartPropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.Railcart(rtid) }
        ),
        "PowerTileProperties" to ModuleMetadata(
            titleRes = R.string.module_power_tile_title,
            descriptionRes = R.string.module_power_tile_desc,
            icon = Icons.Default.Bolt,
            isCore = true,
            category = ModuleCategory.Scene,
            defaultAlias = "FutureLinkedTileGroups",
            defaultSource = "CurrentLevel",
            initialDataFactory = { PowerTilePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.PowerTile(rtid) }
        ),
        "ManholePipelineModuleProperties" to ModuleMetadata(
            titleRes = R.string.module_manhole_pipeline_title,
            descriptionRes = R.string.module_manhole_pipeline_desc,
            icon = Icons.Default.Timeline,
            isCore = true,
            category = ModuleCategory.Scene,
            defaultAlias = "ManholePipeline",
            defaultSource = "CurrentLevel",
            initialDataFactory = { ManholePipelineModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.ManholePipelineModule(rtid) }
        ),
        "TunnelDefendModuleProperties" to ModuleMetadata(
            titleRes = R.string.module_tunnel_defend_title,
            descriptionRes = R.string.module_tunnel_defend_desc,
            icon = Icons.Default.Timeline,
            isCore = true,
            category = ModuleCategory.Scene,
            defaultAlias = "TunnelDefend",
            defaultSource = "CurrentLevel",
            initialDataFactory = { TunnelDefendModuleData() },
            navigationFactory = { rtid -> EditorSubScreen.TunnelDefendModule(rtid) }
        ),
        "RoofProperties" to ModuleMetadata(
            titleRes = R.string.module_roof_props_title,
            descriptionRes = R.string.module_roof_props_desc,
            icon = Icons.Default.LocalFlorist,
            isCore = true,
            category = ModuleCategory.Scene,
            defaultAlias = "RoofProps",
            defaultSource = "CurrentLevel",
            initialDataFactory = { RoofPropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.RoofProperties(rtid) }
        ),
        "TideProperties" to ModuleMetadata(
            titleRes = R.string.module_tide_title,
            descriptionRes = R.string.module_tide_desc,
            icon = Icons.Default.WaterDrop,
            isCore = true,
            category = ModuleCategory.Scene,
            defaultAlias = "Tide",
            defaultSource = "CurrentLevel",
            initialDataFactory = { TidePropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.Tide(rtid) }
        ),
        "WarMistProperties" to ModuleMetadata(
            titleRes = R.string.module_war_mist_title,
            descriptionRes = R.string.module_war_mist_desc,
            icon = Icons.Default.Cloud,
            isCore = true,
            category = ModuleCategory.Scene,
            defaultAlias = "WarMist",
            defaultSource = "CurrentLevel",
            initialDataFactory = { WarMistPropertiesData() },
            navigationFactory = { rtid -> EditorSubScreen.WarMistProperties(rtid) }
        ),
        "RainDarkProperties" to ModuleMetadata(
            titleRes = R.string.module_rain_dark_title,
            descriptionRes = R.string.module_rain_dark_desc,
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