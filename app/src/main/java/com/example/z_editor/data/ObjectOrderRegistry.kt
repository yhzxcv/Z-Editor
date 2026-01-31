package com.example.z_editor.data

object ObjectOrderRegistry {
    private val ORDER_LIST = listOf(
        "LevelDefinition",


        "SeedBankProperties",
        "ConveyorSeedBankProperties",
        "PennyClassroomModuleProperties",

        "SunDropperProperties",
        "SunBombChallengeProperties",
        "LastStandMinigameProperties",
        "BowlingMinigameProperties",
        "NewBowlingMinigameProperties",
        "SeedRainProperties",

        "PiratePlankProperties",
        "TideProperties",
        "RoofProperties",
        "RailcartProperties",
        "PowerTileProperties",
        "ZombiePotionModuleProperties",
        "WarMistProperties",

        "ZombieMoveFastModuleProperties",
        "IncreasedCostModuleProperties",
        "DeathHoleModuleProperties",
        "LevelScoringModuleProperties",
        "LevelMutatorStartingPlantfoodProps",
        "LevelMutatorMaxSunProps",

        "InitialPlantEntryProperties",
        "InitialZombieProperties",
        "InitialGridItemProperties",
        "ProtectThePlantChallengeProperties",
        "ProtectTheGridItemChallengeProperties",

        "ZombossBattleIntroProperties",
        "ZombossBattleModuleProperties",
        "VaseBreakerPresetProperties",
        "EvilDaveProperties",

        "StarChallengeModuleProperties",

        "StarChallengeBeatTheLevelProps",
        "StarChallengeSaveMowersProps",
        "StarChallengePlantFoodNonuseProps",
        "StarChallengePlantsSurviveProps",
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
        "StarChallengeTargetScoreProps",


        "WaveManagerModuleProperties",
        "WaveManagerProperties",

        "SpawnZombiesJitteredWaveActionProps",
        "SpawnZombiesFromGroundSpawnerProps",
        "SpawnZombiesFromGridItemSpawnerProps",
        "BeachStageEventZombieSpawnerProps",

        "StormZombieSpawnerProps",
        "RaidingPartyZombieSpawnerProps",

        "SpiderRainZombieSpawnerProps",
        "ParachuteRainZombieSpawnerProps",
        "BassRainZombieSpawnerProps",

        "SpawnModernPortalsWaveActionProps",
        "FrostWindWaveActionProps",
        "DinoWaveActionProps",

        "TidalChangeWaveActionProps",
        "BlackHoleWaveActionProps",

        "ZombiePotionActionProps",
        "SpawnGravestonesWaveActionProps",

        "ModifyConveyorWaveActionProps",

        "ZombieType",
        "ZombiePropertySheet",
    )

    private val ORDER_MAP: Map<String, Int> = ORDER_LIST.withIndex().associate { it.value to it.index }

    /**
     * 获取对象的排序权重。
     * 如果在列表中，返回索引（越小越前）。
     * 如果不在列表中，返回 Int.MAX_VALUE（放到最后）。
     */
    fun getPriority(objClass: String): Int {
        return ORDER_MAP[objClass] ?: Int.MAX_VALUE
    }

    private val naturalStringComparator = Comparator<String> { s1, s2 ->
        var i = 0
        var j = 0
        while (i < s1.length && j < s2.length) {
            val c1 = s1[i]
            val c2 = s2[j]

            // 如果两个字符都是数字，提取完整的数字部分进行数值比较
            if (c1.isDigit() && c2.isDigit()) {
                var num1 = 0L
                while (i < s1.length && s1[i].isDigit()) {
                    if (num1 < 100000000000000000L) {
                        num1 = num1 * 10 + (s1[i] - '0')
                    }
                    i++
                }

                var num2 = 0L
                while (j < s2.length && s2[j].isDigit()) {
                    if (num2 < 100000000000000000L) {
                        num2 = num2 * 10 + (s2[j] - '0')
                    }
                    j++
                }

                if (num1 != num2) {
                    return@Comparator num1.compareTo(num2)
                }
            } else {
                if (c1 != c2) {
                    return@Comparator c1.compareTo(c2)
                }
                i++
                j++
            }
        }
        // 如果前面都一样，长度短的排前面
        s1.length - s2.length
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
                naturalStringComparator.compare(alias1, alias2)
            }
        }
    }
}