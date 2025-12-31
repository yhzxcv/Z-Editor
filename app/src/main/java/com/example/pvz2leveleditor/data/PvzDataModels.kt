package com.example.pvz2leveleditor.data

import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

// === 文件根结构 ===
data class PvzLevelFile(
    val objects: MutableList<PvzObject>,
    val version: Int = 1
)

// === 通用对象外壳 ===
data class PvzObject(
    var aliases: List<String>? = null,
    @SerializedName("objclass") val objClass: String,
    @SerializedName("objdata") var objData: JsonElement
)

// ======================== 1. 关卡抬头与全局属性 ========================

// === 关卡定义 ===
data class LevelDefinitionData(
    @SerializedName("Name") var name: String = "",
    @SerializedName("LevelNumber") var levelNumber: Int = 1,
    @SerializedName("Description") var description: String = "",
    @SerializedName("StageModule") var stageModule: String = "",
    @SerializedName("Loot") var loot: String = "RTID(DefaultLoot@LevelModules)",
    @SerializedName("StartingSun") var startingSun: Int? = 200,
    @SerializedName("VictoryModule") var victoryModule: String = "RTID(VictoryOutro@LevelModules)",
    @SerializedName("DisablePeavine") var disablePeavine: Boolean? = null,
    @SerializedName("IsArtifactDisabled") var isArtifactDisabled: Boolean? = null,
    @SerializedName("Modules") val modules: MutableList<String> = mutableListOf()
    // 例如: ["RTID(StandardIntro@LevelModules)", "RTID(DefaultSunDropper@LevelModules)"]
)

// === 波次管理器 ===
data class WaveManagerModuleData(
    @SerializedName("DynamicZombies") var dynamicZombies: MutableList<DynamicZombieGroup> = mutableListOf(),
    @SerializedName("WaveManagerProps") var waveManagerProps: String = ""
)

data class DynamicZombieGroup(
    @SerializedName("PointIncrementPerWave") var pointIncrement: Int = 0,
    @SerializedName("StartingPoints") var startingPoints: Int = 0,
    @SerializedName("StartingWave") var startingWave: Int = 0,
    @SerializedName("ZombiePool") var zombiePool: MutableList<String> = mutableListOf(),
    @SerializedName("ZombieLevel") var zombieLevel: MutableList<Int> = mutableListOf()
)

// === 波次列表 ===
data class WaveManagerData(
    @SerializedName("WaveCount") var waveCount: Int = 0,
    @SerializedName("FlagWaveInterval") var flagWaveInterval: Int = 10,
    @SerializedName("SuppressFlagZombie") var suppressFlagZombie: Boolean? = null,
    @SerializedName("LevelJam") var levelJam: String? = null,
    @SerializedName("ZombieCountDownFirstWaveSecs") var zombieCountDownFirstWaveSecs: Int? = 12,
    @SerializedName("ZombieCountDownFirstWaveConveyorSecs") var zombieCountDownFirstWaveConveyorSecs: Int? = 5,
    @SerializedName("ZombieCountDownHugeWaveDelay") var zombieCountDownHugeWaveDelay: Int? = 5,
    @SerializedName("MaxNextWaveHealthPercentage") var maxNextWaveHealthPercentage: Double = 0.85,
    @SerializedName("MinNextWaveHealthPercentage") var minNextWaveHealthPercentage: Double = 0.70,
    @SerializedName("Waves") val waves: MutableList<MutableList<String>> = mutableListOf()
    // 注意：Waves 是二维数组 [ ["RTID(Wave1)"], ["RTID(Wave8)", "RTID(Portal)"] ]
)

// === 阳光掉落模块 ===
data class SunDropperPropertiesData(
    @SerializedName("InitialSunDropDelay") var initialSunDropDelay: Double = 2.0,
    @SerializedName("SunCountDownBase") var sunCountDownBase: Double = 4.25,
    @SerializedName("SunCountDownMax") var sunCountDownMax: Double = 9.5,
    @SerializedName("SunCountDownIncreasePerSun") var sunCountDownIncreasePerSun: Double = 0.1,
    @SerializedName("SunCountDownRange") var sunCountDownRange: Double = 2.75
)

// === 种子库模块 ===
data class SeedBankData(
    @SerializedName("PresetPlantList") var presetPlantList: MutableList<String> = mutableListOf(),
    @SerializedName("PlantWhiteList") var plantWhiteList: MutableList<String> = mutableListOf(),
    @SerializedName("PlantBlackList") var plantBlackList: MutableList<String> = mutableListOf(),
    @SerializedName("SelectionMethod") var selectionMethod: String = "chooser",
    @SerializedName("GlobalLevel") var globalLevel: Int? = null,
    @SerializedName("OverrideSeedSlotsCount") var overrideSeedSlotsCount: Int? = 8
)

// === 传送带模块 ===
data class ConveyorBeltData(
    @SerializedName("InitialPlantList") var initialPlantList: MutableList<InitialPlantListData> = mutableListOf(),
    @SerializedName("DropDelayConditions") var dropDelayConditions: MutableList<DropDelayConditionData> = mutableListOf(),
    @SerializedName("SpeedConditions") var speedConditions: MutableList<SpeedConditionData> = mutableListOf()
)

data class DropDelayConditionData(
    @SerializedName("Delay") var delay: Int = 0,
    @SerializedName("MaxPackets") var maxPacketsDelay: Int = 0
)

data class SpeedConditionData(
    @SerializedName("Speed") var speed: Int = 0,
    @SerializedName("MaxPackets") var maxPacketsSpeed: Int = 0
)

data class InitialPlantListData(
    @SerializedName("PlantType") var plantType: String = "",
    @SerializedName("iLevel") var iLevel: Int? = null,
    @SerializedName("Weight") var weight: Int = 100,
    @SerializedName("MaxCount") var maxCount: Int = 0,
    @SerializedName("MaxWeightFactor") var maxWeightFactor: Double = 1.0,
    @SerializedName("MinCount") var minCount: Int = 0,
    @SerializedName("MinWeightFactor") var minWeightFactor: Double = 1.0
)

// === 初始植物模块 ===
data class InitialPlantEntryData(
    @SerializedName("Plants") var plants: MutableList<InitialPlantData> = mutableListOf()
)

data class InitialPlantData(
    @SerializedName("GridX") var gridX: Int = 0,
    @SerializedName("GridY") var gridY: Int = 0,
    @SerializedName("Level") var level: Int = 1,
    @SerializedName("Avatar") var avatar: Boolean = false,
    @SerializedName("PlantTypes") var plantTypes: MutableList<String> = mutableListOf()
)

// === 初始僵尸模块 ===
data class InitialZombieEntryData(
    @SerializedName("InitialZombiePlacements") var placements: MutableList<InitialZombieData> = mutableListOf()
)

data class InitialZombieData(
    @SerializedName("GridX") var gridX: Int = 0,
    @SerializedName("GridY") var gridY: Int = 0,
    @SerializedName("TypeName") var typeName: String = "",
    @SerializedName("Condition") var condition: String = "icecubed"
)

// === 初始障碍物模块 ===
data class InitialGridItemEntryData(
    @SerializedName("InitialGridItemPlacements") var placements: MutableList<InitialGridItemData> = mutableListOf()
)

data class InitialGridItemData(
    @SerializedName("GridX") var gridX: Int = 0,
    @SerializedName("GridY") var gridY: Int = 0,
    @SerializedName("TypeName") var typeName: String = ""
)

// === 太阳炸弹挑战模块 ===
data class SunBombChallengeData(
    @SerializedName("PlantBombExplosionRadius") var plantBombExplosionRadius: Int = 25,
    @SerializedName("ZombieBombExplosionRadius") var zombieBombExplosionRadius: Int = 80,
    @SerializedName("PlantDamage") var plantDamage: Int = 1000,
    @SerializedName("ZombieDamage") var zombieDamage: Int = 500
)

// === 三星挑战模块 ===
data class StarChallengeModuleData(
    @SerializedName("ChallengesAlwaysAvailable") var challengesAlwaysAvailable: Boolean = true,
    @SerializedName("Challenges") var challenges: MutableList<MutableList<String>> = mutableListOf()
)

data class StarChallengeBeatTheLevelData(
    @SerializedName("Description") var description: String = "",
    @SerializedName("DescriptiveName") var descriptiveName: String = ""
)

class StarChallengeSaveMowerData

class StarChallengePlantFoodNonuseData

data class StarChallengePlantSurviveData(
    @SerializedName("Count") var count: Int = 10
)

data class StarChallengeZombieDistanceData(
    @SerializedName("TargetDistance") var targetDistance: Double = 5.0
)

data class StarChallengeSunProducedData(
    @SerializedName("TargetSun") var targetSun: Int = 0
)

data class StarChallengeSunUsedData(
    @SerializedName("MaximumSun") var maximumSun: Int = 2000
)

data class StarChallengeSpendSunHoldoutData(
    @SerializedName("HoldoutSeconds") var holdoutSeconds: Int = 0
)

data class StarChallengeKillZombiesInTimeData(
    @SerializedName("ZombiesToKill") var zombiesToKill: Int = 10,
    @SerializedName("Time") var time: Int = 10
)

data class StarChallengeZombieSpeedData(
    @SerializedName("SpeedModifier") var speedModifier: Double = 0.0
)

data class StarChallengeSunReducedData(
    @SerializedName("sunModifier") var sunModifier: Double = 0.0
)

data class StarChallengePlantsLostData(
    @SerializedName("MaximumPlantsLost") var maximumPlantsLost: Int = 10
)

data class StarChallengeSimultaneousPlantsData(
    @SerializedName("MaximumPlants") var maximumPlants: Int = 10
)

data class StarChallengeUnfreezePlantsData(
    @SerializedName("Count") var count: Int = 0
)

data class StarChallengeBlowZombieData(
    @SerializedName("Count") var count: Int = 0
)

// ======================== 2. 物体属性解析 ========================

// === 僵尸属性解析 ===
data class ZombieTypeData(
    @SerializedName("TypeName") var typeName: String = "",
    @SerializedName("Properties") var properties: String = ""
)

data class ZombiePropertySheetData(
    @SerializedName("WavePointCost") var wavePointCost: Int = 0,
    @SerializedName("Weight") var weight: Double = 0.0,
    @SerializedName("Hitpoints") var hitpoints: Double = 0.0,
    @SerializedName("Speed") var speed: Double = 0.0,
    @SerializedName("EatDPS") var eatDPS: Double = 0.0,
    @SerializedName("CanSurrender") var canSurrender: Boolean = false,
    @SerializedName("SizeType") var sizeType: String = ""
)

data class ZombieStats(
    val id: String,
    val hp: Double,
    val cost: Int,
    val weight: Int,
    val speed: Double,
    val eatDPS: Double,
    val sizeType: String
)

// ======================== 3. 具体事件定义 ========================

// === 通用僵尸数据 ===
data class ZombieSpawnData(
    @SerializedName("Type") var type: String,
    @SerializedName("Level") var level: Int? = 1,
    @JsonAdapter(FlexibleIntAdapter::class)
    @SerializedName("Row") var row: Int? = null,
    @Transient var isElite: Boolean = false
)

// === 自然出怪事件 ===
data class WaveActionData(
    @SerializedName("AdditionalPlantfood") var additionalPlantFood: Int? = null,
    @SerializedName("SpawnPlantName") var spawnPlantName: MutableList<String>? = null,
    @SerializedName("Zombies") val zombies: MutableList<ZombieSpawnData> = mutableListOf()
)

// === 地下出怪事件 ===
data class SpawnZombiesFromGroundData(
    @SerializedName("ColumnStart") var columnStart: Int = 6,
    @SerializedName("ColumnEnd") var columnEnd: Int = 9,
    @SerializedName("AdditionalPlantfood") var additionalPlantFood: Int? = null,
    @SerializedName("SpawnPlantName") var spawnPlantName: MutableList<String>? = null,
    @SerializedName("Zombies") val zombies: MutableList<ZombieSpawnData> = mutableListOf()
)

// === 传送门事件 ===
data class PortalEventData(
    @SerializedName("PortalType") var portalType: String = "egypt",
    @JsonAdapter(FlexibleIntAdapter::class)
    @SerializedName("PortalColumn") var portalColumn: Int = 5,
    @JsonAdapter(FlexibleIntAdapter::class)
    @SerializedName("PortalRow") var portalRow: Int = 3,
    @SerializedName("SpawnEffect") var spawnEffect: String = "",
    @SerializedName("SpawnSoundID") var spawnSoundID: String = "",
    @SerializedName("IgnoreGraveStone") var ignoreGraveStone: Boolean = false
)

// === 风暴突袭事件 ===
data class StormZombieSpawnerPropsData(
    @SerializedName("ColumnStart") var columnStart: Int = 5,
    @SerializedName("ColumnEnd") var columnEnd: Int = 9,
    @SerializedName("GroupSize") var groupSize: Int = 1,
    @SerializedName("TimeBetweenGroups") var timeBetweenGroups: Int = 1,
    @SerializedName("Type") var type: String = "sandstorm",
    @SerializedName("Zombies") var zombies: MutableList<StormZombieData> = mutableListOf()
)

data class StormZombieData(
    @SerializedName("Type") var type: String = ""
)

// === 海盗登船事件 ===
data class RaidingPartyEventData(
    @SerializedName("GroupSize") var groupSize: Int = 5,
    @SerializedName("SwashbucklerCount") var swashbucklerCount: Int = 5,
    @SerializedName("TimeBetweenGroups") var timeBetweenGroups: Int = 2
)

// === 空降事件 ===
data class ParachuteRainEventData(
    @SerializedName("ColumnStart") var columnStart: Int = 5,
    @SerializedName("ColumnEnd") var columnEnd: Int = 9,
    @SerializedName("GroupSize") var groupSize: Int = 1,
    @SerializedName("SpiderCount") var spiderCount: Int = 1,
    @SerializedName("SpiderZombieName") var spiderZombieName: String = "lostcity_lostpilot",
    @SerializedName("TimeBeforeFullSpawn") var timeBeforeFullSpawn: Double = 1.0,
    @SerializedName("TimeBetweenGroups") var timeBetweenGroups: Double = 0.5,
    @SerializedName("ZombieFallTime") var zombieFallTime: Double = 1.0,
    @SerializedName("WaveStartMessage") var waveStartMessage: String = ""
)

// === 传送带变更事件 ===
data class ModifyConveyorWaveActionData(
    @SerializedName("Add") var addList: MutableList<ModifyConveyorPlantData> = mutableListOf(),
    @SerializedName("Remove") var removeList: MutableList<ModifyConveyorRemoveData> = mutableListOf()
)

data class ModifyConveyorPlantData(
    @SerializedName("Type") var type: String = "",
    @SerializedName("iLevel") var iLevel: Int? = null,
    @SerializedName("Weight") var weight: Int = 100,
    @SerializedName("MaxCount") var maxCount: Int = 0,
    @SerializedName("MaxWeightFactor") var maxWeightFactor: Double = 1.0,
    @SerializedName("MinCount") var minCount: Int = 0,
    @SerializedName("MinWeightFactor") var minWeightFactor: Double = 1.0
)

data class ModifyConveyorRemoveData(
    @SerializedName("Type") var type: String = ""
)