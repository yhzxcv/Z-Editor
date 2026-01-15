package com.example.z_editor.data

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
    @SerializedName("LevelNumber") var levelNumber: Int? = 1,
    @SerializedName("Description") var description: String = "",
    @SerializedName("StageModule") var stageModule: String = "",
    @SerializedName("Loot") var loot: String = "RTID(DefaultLoot@LevelModules)",
    @SerializedName("StartingSun") var startingSun: Int? = 200,
    @SerializedName("VictoryModule") var victoryModule: String = "RTID(VictoryOutro@LevelModules)",
    @SerializedName("MusicType") var musicType: String = "",
    @SerializedName("DisablePeavine") var disablePeavine: Boolean? = null,
    @SerializedName("IsArtifactDisabled") var isArtifactDisabled: Boolean? = null,
    @SerializedName("Modules") val modules: MutableList<String> = mutableListOf()
    // 例如: ["RTID(StandardIntro@LevelModules)", "RTID(DefaultSunDropper@LevelModules)"]
)

// === 波次管理器 ===
data class WaveManagerModuleData(
    @SerializedName("DynamicZombies") var dynamicZombies: MutableList<DynamicZombieGroup> = mutableListOf(),
    @SerializedName("WaveManagerProps") var waveManagerProps: String = "",
    @SerializedName("ManualStartup") var manualStartup: Boolean? = null
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
    @SerializedName("WaveCount") var waveCount: Int = 1,
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

// === 坚不可摧模块 ===
data class LastStandMinigamePropertiesData(
    @SerializedName("StartingSun") var startingSun: Int = 2000,
    @SerializedName("StartingPlantfood") var startingPlantfood: Int = 0
)

// === 阳光掉落模块 ===
data class SunDropperPropertiesData(
    @SerializedName("InitialSunDropDelay") var initialSunDropDelay: Double = 2.0,
    @SerializedName("SunCountdownBase") var sunCountdownBase: Double = 4.25,
    @SerializedName("SunCountdownMax") var sunCountdownMax: Double = 9.5,
    @SerializedName("SunCountdownIncreasePerSun") var sunCountdownIncreasePerSun: Double = 0.1,
    @SerializedName("SunCountdownRange") var sunCountdownRange: Double = 2.75
)

// === 种子库模块 ===
data class SeedBankData(
    @SerializedName("PresetPlantList") var presetPlantList: MutableList<String> = mutableListOf(),
    @SerializedName("PlantWhiteList") var plantWhiteList: MutableList<String> = mutableListOf(),
    @SerializedName("PlantBlackList") var plantBlackList: MutableList<String> = mutableListOf(),
    @SerializedName("SelectionMethod") var selectionMethod: String = "chooser",
    @SerializedName("GlobalLevel") var globalLevel: Int? = null,
    @SerializedName("OverrideSeedSlotsCount") var overrideSeedSlotsCount: Int? = 8,
    @SerializedName("ZombieMode") var zombieMode: Boolean? = null,
    @SerializedName("SeedPacketType") var seedPacketType: String? = null
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

// === 保护障碍物模块 ===
data class ProtectTheGridItemChallengePropertiesData(
    @SerializedName("Description") var description: String = "",
    @SerializedName("MustProtectCount") var mustProtectCount: Int = 0,
    @SerializedName("GridItems") var gridItems: MutableList<ProtectGridItemData> = mutableListOf()
)

data class ProtectGridItemData(
    @SerializedName("GridX") var gridX: Int = 0,
    @SerializedName("GridY") var gridY: Int = 0,
    @SerializedName("GridItemType") var gridItemType: String = ""
)

// === 保护植物模块 ===
data class ProtectThePlantChallengePropertiesData(
    @SerializedName("MustProtectCount") var mustProtectCount: Int = 0,
    @SerializedName("Plants") var plants: MutableList<ProtectPlantData> = mutableListOf()
)

data class ProtectPlantData(
    @SerializedName("GridX") var gridX: Int = 0,
    @SerializedName("GridY") var gridY: Int = 0,
    @SerializedName("PlantType") var plantType: String = ""
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

data class StarChallengeTargetScoreData(
    @SerializedName("TargetScore") var targetScore: Int = 20000
)

// === 甲板模块 ===
data class PiratePlankPropertiesData(
    @SerializedName("PlankRows") var plankRows: MutableList<Int> = mutableListOf()
)

// === 潮水模块 ===
data class TidePropertiesData(
    @SerializedName("StartingWaveLocation") var startingWaveLocation: Int = 0
)

// === 矿车模块 ===
data class RailcartPropertiesData(
    @SerializedName("RailcartType") var railcartType: String = "railcart_cowboy",
    @SerializedName("Railcarts") var railcarts: MutableList<RailcartData> = mutableListOf(),
    @SerializedName("Rails") var rails: MutableList<RailData> = mutableListOf()
)

data class RailcartData(
    @SerializedName("Column") var column: Int = 0,
    @SerializedName("Row") var row: Int = 0
)

data class RailData(
    @SerializedName("Column") var column: Int = 0,
    @SerializedName("RowStart") var rowStart: Int = 0,
    @SerializedName("RowEnd") var rowEnd: Int = 0
)

// === 能量瓷砖模块 ===
data class PowerTilePropertiesData(
    @SerializedName("LinkedTiles") var linkedTiles: MutableList<LinkedTileData> = mutableListOf()
)

data class LinkedTileData(
    @SerializedName("Group") var group: String = "alpha",
    @SerializedName("PropagationDelay") var propagationDelay: Double = 1.5,
    @SerializedName("Location") var location: TileLocationData = TileLocationData()
)

data class TileLocationData(
    @SerializedName("mX") var mx: Int = 0,
    @SerializedName("mY") var my: Int = 0
)

// === 迷雾模块 ===
data class WarMistPropertiesData(
    @SerializedName("m_iInitMistPosX") var initMistPosX: Int = 5,
    @SerializedName("m_iNormValX") var normValX: Int = 1000,
    @SerializedName("m_iBloverEffectInterval") var bloverEffectInterval: Int = 15
)

// === 僵尸药水模块 ===
data class ZombiePotionModulePropertiesData(
    @SerializedName("InitialPotionCount") var initialPotionCount: Int = 10,
    @SerializedName("MaxPotionCount") var maxPotionCount: Int = 60,
    @SerializedName("PotionSpawnTimer") var potionSpawnTimer: PotionSpawnTimerData = PotionSpawnTimerData(),
    @SerializedName("PotionTypes") var potionTypes: MutableList<String> = mutableListOf()
)

data class PotionSpawnTimerData(
    @SerializedName("Min") var min: Int = 12,
    @SerializedName("Max") var max: Int = 16
)

// === 通货膨胀模块 ===
data class IncreasedCostModulePropertiesData(
    @SerializedName("BaseCostIncreased") var baseCostIncreased: Int = 25,
    @SerializedName("MaxIncreasedCount") var maxIncreasedCount: Int = 10
)

// === 死亡坑洞模块 ===
data class DeathHoleModuleData(
    @SerializedName("LifeTime") var lifeTime: Int = 10
)

// === 加速进场模块 ===
data class ZombieMoveFastModulePropertiesData(
    @SerializedName("StopColumn") var stopColumn: Int = 6,
    @SerializedName("SpeedUp") var speedUp: Double = 3.0
)

// === 阳光上限覆写 ===
data class LevelMutatorMaxSunPropsData(
    @SerializedName("MaxSunOverride") var maxSunOverride: Int = 9900,
    @SerializedName("DifficultyProps") var difficultyProps: String = "RTID(LevelModuleDifficultyMaxSun@LevelModulesDifficulty)",
    @SerializedName("IconImage") var iconImage: String = "IMAGE_UI_PENNY_PURSUITS_DIFFICULTY_MODIFIER_ICONS_DIFFICULTY_MODIFIER_STARTING_SUN",
    @SerializedName("IconText") var iconText: String = "",
)

// === 初始能量豆覆写 ===
data class LevelMutatorStartingPlantfoodPropsData(
    @SerializedName("StartingPlantfoodOverride") var startingPlantfoodOverride: Int = 0,
    @SerializedName("DifficultyProps") var difficultyProps: String = "RTID(LevelModuleDifficultyStartingPlantfood@LevelModulesDifficulty)",
    @SerializedName("IconImage") var iconImage: String = "IMAGE_UI_PENNY_PURSUITS_DIFFICULTY_MODIFIER_ICONS_DIFFICULTY_MODIFIER_PF",
    @SerializedName("IconText") var iconText: String = "",
)

// === 沙滩保龄球配置 ===
data class BowlingMinigamePropertiesData(
    @SerializedName("BowlingFoulLine") var bowlingFoulLine: Int = 2
)

// === 坚果保龄球配置 ===
class NewBowlingMinigamePropertiesData

// === 排山倒海配置 ===
class PVZ1OverwhelmModulePropertiesData

// === 屋顶花盆模块 ===
data class RoofPropertiesData(
    @SerializedName("FlowerPotStartColumn") var flowerPotStartColumn: Int = 0,
    @SerializedName("FlowerPotEndColumn") var flowerPotEndColumn: Int = 2
)

// === 积分模块 ===
data class LevelScoringData(
    @SerializedName("PlantBonusMultiplier") var plantBonusMultiplier: Double = 0.0,
    @SerializedName("PlantBonuses") var plantBonuses: MutableList<String> = mutableListOf(),
    @SerializedName("ScoringRulesType") var scoringRulesType: String = "NoMultiplier",
    @SerializedName("StartingPlantfood") var startingPlantfood: Int = 0
)

// === 地道模块 ===
data class ManholePipelineModuleData(
    @SerializedName("OperationTimePerGrid") var operationTimePerGrid: Int = 1,
    @SerializedName("DamagePerSecond") var damagePerSecond: Int = 30,
    @SerializedName("PipelineList") var pipelineList: MutableList<PipelineData> = mutableListOf()
)

data class PipelineData(
    @SerializedName("StartX") var startX: Int = 0,
    @SerializedName("StartY") var startY: Int = 0,
    @SerializedName("EndX") var endX: Int = 0,
    @SerializedName("EndY") var endY: Int = 0
)

// ======================== 2. 物体属性解析 ========================

// === 通用僵尸数据 ===
data class ZombieSpawnData(
    @SerializedName("Type") var type: String,
    @SerializedName("Level") var level: Int? = null,
    @JsonAdapter(FlexibleIntAdapter::class)
    @SerializedName("Row") var row: Int? = null,
    @Transient var isElite: Boolean = false
)

// === 僵尸属性解析 ===
data class ZombieStats(
    val id: String,
    val hp: Double,
    val cost: Int,
    val weight: Int,
    val speed: Double,
    val eatDPS: Double,
    val sizeType: String
)

data class ZombieTypeData(
    @SerializedName("TypeName") var typeName: String = "",
    @SerializedName("Properties") var properties: String = "",
    @SerializedName("Resistences") var resistences: MutableList<Double>? = null
)

data class RectData(
    @SerializedName("mX") var mX: Int = 0,
    @SerializedName("mY") var mY: Int = 0,
    @SerializedName("mWidth") var mWidth: Int = 0,
    @SerializedName("mHeight") var mHeight: Int = 0
)

data class Point2D(
    @SerializedName("x") var x: Int = 0,
    @SerializedName("y") var y: Int = 0
)

data class Point3DDouble(
    @SerializedName("x") var x: Double = 0.0,
    @SerializedName("y") var y: Double = 0.0,
    @SerializedName("z") var z: Double = 0.0
)

data class ZombiePropertySheetData(
    @SerializedName("Hitpoints") var hitpoints: Double = 0.0,
    @SerializedName("Speed") var speed: Double = 0.0,
    @SerializedName("SpeedVariance") var speedVariance: Double? = null,
    @SerializedName("EatDPS") var eatDPS: Double = 0.0,
    @SerializedName("Weight") var weight: Int = 0,
    @SerializedName("WavePointCost") var wavePointCost: Int = 0,
    @SerializedName("SizeType") var sizeType: String? = null,
    @SerializedName("HitRect") var hitRect: RectData? = null,
    @SerializedName("AttackRect") var attackRect: RectData? = null,
    @SerializedName("ArtCenter") var artCenter: Point2D? = null,
    @SerializedName("ShadowOffset") var shadowOffset: Point3DDouble? = null,
    @SerializedName("GroundTrackName") var groundTrackName: String = "",
    @SerializedName("CanSpawnPlantFood") var canSpawnPlantFood: Boolean = false,
    @SerializedName("CanSurrender") var canSurrender: Boolean? = null,
    @SerializedName("EnableShowHealthBarByDamage") var enableShowHealthBarByDamage: Boolean? = null,
    @SerializedName("CanBePlantTossedweak") var canBePlantTossedweak: Boolean? = null,
    @SerializedName("CanBePlantTossedStrong") var canBePlantTossedStrong: Boolean? = null,
    @SerializedName("CanBeLaunchedByPlants") var canBeLaunchedByPlants: Boolean? = null,
    @SerializedName("DrawHealthBarTime") var drawHealthBarTime: Double? = null,
    @SerializedName("EnableEliteImmunities") var enableEliteImmunities: Boolean? = null,
    @SerializedName("EnableEliteScale") var enableEliteScale: Boolean? = null,
    @SerializedName("CanTriggerZombieWin") var canTriggerZombieWin: Boolean? = null,
    @SerializedName("ChillInsteadOfFreeze") var chillInsteadOfFreeze: Boolean? = null,
    @SerializedName("EliteScale") var eliteScale: Double? = null,
    @SerializedName("ArmDropFraction") var armDropFraction: Int? = null,
    @SerializedName("HeadDropFraction") var headDropFraction: Int? = null,
)


data class LocationData(
    @SerializedName("mX") var x: Int = 0,
    @SerializedName("mY") var y: Int = 0
)

// ======================== 3. 具体事件定义 ========================


// === 自然出怪事件 ===
data class WaveActionData(
    @SerializedName("NotificationEvents") var notificationEvents: MutableList<String>? = null,
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
    @SerializedName("SpiderZombieName") var spiderZombieName: String = "",
    @SerializedName("TimeBeforeFullSpawn") var timeBeforeFullSpawn: Double = 1.0,
    @SerializedName("TimeBetweenGroups") var timeBetweenGroups: Double = 1.5,
    @SerializedName("ZombieFallTime") var zombieFallTime: Double = 4.5,
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

// === 潮水变更事件 ===
data class TidalChangeWaveActionData(
    @SerializedName("TidalChange") var tidalChange: TidalChangeInternalData = TidalChangeInternalData()
)

data class TidalChangeInternalData(
    @SerializedName("ChangeAmount") var changeAmount: Int = 0,
    @SerializedName("ChangeType") var changeType: String = "absolute"
)

// === 退潮僵尸事件 ===
data class BeachStageEventData(
    @SerializedName("ColumnStart") var columnStart: Int = 5,
    @SerializedName("ColumnEnd") var columnEnd: Int = 9,
    @SerializedName("GroupSize") var groupSize: Int = 1,
    @SerializedName("ZombieCount") var zombieCount: Int = 1,
    @SerializedName("ZombieName") var zombieName: String = "beach",
    @SerializedName("TimeBeforeFullSpawn") var timeBeforeFullSpawn: Double = 1.0,
    @SerializedName("TimeBetweenGroups") var timeBetweenGroups: Double = 0.5,
    @SerializedName("WaveStartMessage") var waveStartMessage: String = ""
)

// === 黑洞事件 ===
data class BlackHoleEventData(
    @SerializedName("ColNumPlantIsDragged") var colNumPlantIsDragged: Int = 0
)

// === 寒风事件 ===
data class FrostWindWaveActionPropsData(
    @SerializedName("Winds") var winds: MutableList<FrostWindData> = mutableListOf()
)

data class FrostWindData(
    @SerializedName("Direction") var direction: String = "right",
    @SerializedName("Row") var row: Int = 2
)

// === 恐龙召唤事件 ===
data class DinoWaveActionPropsData(
    @SerializedName("DinoRow") var dinoRow: Int = 2,
    @SerializedName("DinoType") var dinoType: String = "raptor",
    @SerializedName("DinoWaveDuration") var dinoWaveDuration: Int = 2
)

// === 障碍物生成事件 ===
data class SpawnGraveStonesData(
    @SerializedName("GravestonePool") var gravestonePool: MutableList<GravestonePoolItem> = mutableListOf(),
    @SerializedName("SpawnPositionsPool") var spawnPositionsPool: MutableList<LocationData> = mutableListOf()
)

data class GravestonePoolItem(
    @SerializedName("Count") var count: Int = 1,
    @SerializedName("Type") var type: String = ""
)

// === 障碍物出怪事件 ===
data class SpawnZombiesFromGridItemData(
    @SerializedName("WaveStartMessage") var waveStartMessage: String? = null,
    @SerializedName("ZombieSpawnWaitTime") var zombieSpawnWaitTime: Int = 0,
    @SerializedName("GridTypes") var gridTypes: MutableList<String> = mutableListOf(),
    @SerializedName("Zombies") var zombies: MutableList<ZombieSpawnData> = mutableListOf()
)

// === 投放药水事件 ===
data class ZombiePotionActionPropsData(
    @SerializedName("Potions") var potions: MutableList<ZombiePotionData> = mutableListOf()
)

data class ZombiePotionData(
    @SerializedName("Location") var location: LocationData = LocationData(),
    @SerializedName("Type") var type: String = ""
)

// === 魔镜传送事件 ===
data class MagicMirrorWaveActionData(
    @SerializedName("MagicMirrorTeleportationArrays")
    var arrays: MutableList<MagicMirrorArrayData> = mutableListOf()
)

data class MagicMirrorArrayData(
    @SerializedName("Mirror1GridX") var mirror1GridX: Int = 2,
    @SerializedName("Mirror1GridY") var mirror1GridY: Int = 2,
    @SerializedName("Mirror2GridX") var mirror2GridX: Int = 6,
    @SerializedName("Mirror2GridY") var mirror2GridY: Int = 2,
    @SerializedName("TypeIndex") var typeIndex: Int = 1,
    @SerializedName("MirrorExistDuration") var mirrorExistDuration: Int = 300
)

// === 童话迷雾事件 ===
data class FairyTaleFogWaveActionData(
    @SerializedName("MovingTime") var movingTime: Double = 3.0,
    @SerializedName("FogType") var fogType: String = "fairy_tale_fog_lvl1",
    @SerializedName("Range") var range: FogRangeData = FogRangeData()
)

data class FogRangeData(
    @SerializedName("mX") var mX: Int = 4,
    @SerializedName("mY") var mY: Int = 0,
    @SerializedName("mWidth") var mWidth: Int = 8,
    @SerializedName("mHeight") var mHeight: Int = 5
)

// === 童话微风事件 ===
data class FairyTaleWindWaveActionData(
    @SerializedName("Duration") var duration: Double = 5.0,
    @SerializedName("VelocityScale") var velocityScale: Double = 2.0
)

// ======================== 4. 特殊模式模块数据定义 ========================

// === 罐子内容配置===
data class VaseBreakerPresetData(
    @SerializedName("MinColumnIndex") var minColumnIndex: Int = 4,
    @SerializedName("MaxColumnIndex") var maxColumnIndex: Int = 8,
    @SerializedName("NumColoredPlantVases") var numColoredPlantVases: Int = 0,
    @SerializedName("NumColoredZombieVases") var numColoredZombieVases: Int = 0,
    @SerializedName("GridSquareBlacklist") var gridSquareBlacklist: MutableList<LocationData> = mutableListOf(),
    @SerializedName("Vases") var vases: MutableList<VaseDefinition> = mutableListOf()
)

data class VaseDefinition(
    @SerializedName("ZombieTypeName") var zombieTypeName: String? = null,
    @SerializedName("PlantTypeName") var plantTypeName: String? = null,
    @SerializedName("CollectableTypeName") var collectableTypeName: String? = null,
    @SerializedName("Count") var count: Int = 1
)

// === 砸罐子环境配置 ===
class VaseBreakerArcadeModuleData

// === 砸罐子流程配置 ===
class VaseBreakerFlowModuleData

// === 我是僵尸模块 ===
data class EvilDavePropertiesData(
    @SerializedName("PlantDistance") var plantDistance: Int = 4
)

// === 僵王战模块 ===
data class ZombossBattleModuleData(
    @SerializedName("ReservedColumnCount") var reservedColumnCount: Int = 2,
    @SerializedName("ZombossMechType") var zombossMechType: String = "zombossmech_egypt",
    @SerializedName("ZombossStageCount") var zombossStageCount: Int = 3,
    @SerializedName("ZombossDeathRow") var zombossDeathRow: Int = 3,
    @SerializedName("ZombossDeathColumn") var zombossDeathColumn: Int = 5,
    @SerializedName("ZombossSpawnGridPosition") var zombossSpawnGridPosition: LocationData? = LocationData()
)

// === 僵王战转场模块 ===
data class ZombossBattleIntroData(
    @SerializedName("PanStartOffset") var panStartOffset: Int = 78,
    @SerializedName("PanEndOffset") var panEndOffset: Int = 486,
    @SerializedName("PanRightDuration") var panRightDuration: Double = 1.5,
    @SerializedName("PanLeftDuration") var panLeftDuration: Double = 1.5,
    @SerializedName("ZombossPhaseCount") var zombossPhaseCount: Int = 3,
    @SerializedName("SkipShowingStreetBossBattle") var skipShowingStreetBossBattle: Boolean = false,
)
