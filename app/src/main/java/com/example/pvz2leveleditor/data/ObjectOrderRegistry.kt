package com.example.pvz2leveleditor.data

object ObjectOrderRegistry {
    // 定义排序优先级列表，越靠前越优先写入文件
    private val ORDER_LIST = listOf(
        "LevelDefinition",


        "SeedBankProperties",
        "ConveyorSeedBankProperties",
        "SunDropperProperties",
        "SunBombChallengeProperties",

        "InitialPlantEntryProperties",
        "InitialZombieProperties",
        "InitialGridItemProperties",


        "StarChallengeModuleProperties",

        "StarChallengeBeatTheLevelProps",
        "StarChallengeSaveMowerProps",
        "StarChallengePlantFoodNonuseProps",
        "StarChallengePlantSurviveProps",
        "StarChallengeZombieDistanceProps",
        "StarChallengeSunProducedProps",
        "StarChallengeSunUsedProps",
        "StarChallengeSpendSunHoldoutProps",
        "StarChallengeKillZombiesInTimeProps",
        "StarChallengeZombieSpeedProps",
        "StarChallengeSunReducedProps",
        "StarChallengePlantsLostProps",
        "StarChallengeSimultaneousPlantsProps",
        "StarChallengeUnfreezePlantsProps",
        "StarChallengeBlowZombieProps",


        "WaveManagerModuleProperties",
        "WaveManagerProperties",

        "SpawnZombiesJitteredWaveActionProps",
        "SpawnZombiesFromGroundSpawnerProps",
        "StormZombieSpawnerProps",
        "RaidingPartyZombieSpawnerProps",
        "SpawnModernPortalsWaveActionProps",
        "ModifyConveyorWaveActionProps",
    )

    // 为了性能，转为 Map 加速查找
    private val ORDER_MAP: Map<String, Int> = ORDER_LIST.withIndex().associate { it.value to it.index }

    /**
     * 获取对象的排序权重。
     * 如果在列表中，返回索引（越小越前）。
     * 如果不在列表中，返回 Int.MAX_VALUE（放到最后）。
     */
    fun getPriority(objClass: String): Int {
        return ORDER_MAP[objClass] ?: Int.MAX_VALUE
    }

    /**
     * 比较器：用于 List<PvzObject> 的排序
     */
    val comparator = Comparator<PvzObject> { o1, o2 ->
        val p1 = getPriority(o1.objClass)
        val p2 = getPriority(o2.objClass)

        when {
            // 1. 两个都在白名单里，按白名单顺序排
            p1 != Int.MAX_VALUE && p2 != Int.MAX_VALUE -> p1 - p2

            // 2. 只有一个在白名单，白名单的排前面
            p1 != Int.MAX_VALUE -> -1
            p2 != Int.MAX_VALUE -> 1

            // 3. 两个都不在白名单 (Unknown)，按 objClass 字母顺序排，方便归类
            o1.objClass != o2.objClass -> o1.objClass.compareTo(o2.objClass)

            // 4. objClass 也一样（比如多个 JitteredWave），按 Alias 字母顺序排，保证稳定性
            else -> {
                val alias1 = o1.aliases?.firstOrNull() ?: ""
                val alias2 = o2.aliases?.firstOrNull() ?: ""
                alias1.compareTo(alias2)
            }
        }
    }
}