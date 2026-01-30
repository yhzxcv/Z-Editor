package com.example.z_editor.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.z_editor.data.StarChallengeBeatTheLevelData
import com.example.z_editor.data.StarChallengeBlowZombieData
import com.example.z_editor.data.StarChallengeKillZombiesInTimeData
import com.example.z_editor.data.StarChallengePlantFoodNonuseData
import com.example.z_editor.data.StarChallengePlantSurviveData
import com.example.z_editor.data.StarChallengePlantsLostData
import com.example.z_editor.data.StarChallengeSaveMowerData
import com.example.z_editor.data.StarChallengeSimultaneousPlantsData
import com.example.z_editor.data.StarChallengeSpendSunHoldoutData
import com.example.z_editor.data.StarChallengeSunProducedData
import com.example.z_editor.data.StarChallengeSunReducedData
import com.example.z_editor.data.StarChallengeSunUsedData
import com.example.z_editor.data.StarChallengeTargetScoreData
import com.example.z_editor.data.StarChallengeUnfreezePlantsData
import com.example.z_editor.data.StarChallengeZombieDistanceData
import com.example.z_editor.data.StarChallengeZombieSpeedData

// 挑战元数据模型
data class ChallengeTypeInfo(
    val title: String,          // 显示名称
    val objClass: String,       // Pvz2 类名
    val defaultAlias: String,   // 默认别名前缀
    val description: String,    // 详细描述
    val icon: ImageVector,      // UI图标
    val initialDataFactory: () -> Any = { Any() } // 默认数据生成器
)

object ChallengeRepository {
    private val allChallenges = listOf(
        ChallengeTypeInfo(
            title = "关卡提示文字",
            objClass = "StarChallengeBeatTheLevelProps",
            defaultAlias = "BeatTheLevel",
            description = "在关卡开头用弹窗显示提示文字",
            icon = Icons.Default.Campaign,
            initialDataFactory = { StarChallengeBeatTheLevelData() }
        ),
        ChallengeTypeInfo(
            title = "不丢车挑战",
            objClass = "StarChallengeSaveMowersProps",
            defaultAlias = "SaveMowers",
            description = "不损失小推车，在庭院模块下会引发闪退",
            icon = Icons.Default.CleaningServices,
            initialDataFactory = { StarChallengeSaveMowerData() }
        ),
        ChallengeTypeInfo(
            title = "禁用能量豆挑战",
            objClass = "StarChallengePlantFoodNonuseProps",
            defaultAlias = "PlantfoodNonuse",
            description = "关卡过程中禁止使用能量豆",
            icon = Icons.Default.Eco,
            initialDataFactory = { StarChallengePlantFoodNonuseData() }
        ),
        ChallengeTypeInfo(
            title = "幸存植物挑战",
            objClass = "StarChallengePlantsSurviveProps",
            defaultAlias = "PlantsSurive",
            description = "需要指定数量的植物在游戏结束时存活",
            icon = Icons.Default.Security,
            initialDataFactory = { StarChallengePlantSurviveData() }
        ),
        ChallengeTypeInfo(
            title = "花坛线挑战",
            objClass = "StarChallengeZombieDistanceProps",
            defaultAlias = "ZombieDistance",
            description = "不能让僵尸踩踏到花坛线",
            icon = Icons.Default.DoNotStep,
            initialDataFactory = { StarChallengeZombieDistanceData() }
        ),
        ChallengeTypeInfo(
            title = "生产阳光挑战",
            objClass = "StarChallengeSunProducedProps",
            defaultAlias = "SunProduced",
            description = "关卡结束前生产一定数量阳光",
            icon = Icons.Default.WbSunny,
            initialDataFactory = { StarChallengeSunProducedData() }
        ),
        ChallengeTypeInfo(
            title = "阳光限额挑战",
            objClass = "StarChallengeSunUsedProps",
            defaultAlias = "SunUsed",
            description = "关卡中阳光的限额使用",
            icon = Icons.Default.Savings,
            initialDataFactory = { StarChallengeSunUsedData() }
        ),
        ChallengeTypeInfo(
            title = "保持阳光挑战",
            objClass = "StarChallengeSpendSunHoldoutProps",
            defaultAlias = "SpendSunHoldout",
            description = "保持一段时间不使用阳光",
            icon = Icons.Default.HourglassEmpty,
            initialDataFactory = { StarChallengeSpendSunHoldoutData() }
        ),
        ChallengeTypeInfo(
            title = "消灭僵尸挑战",
            objClass = "StarChallengeKillZombiesInTimeProps",
            defaultAlias = "KillZombies",
            description = "在一定时间内消灭指定数量僵尸",
            icon = Icons.Default.PestControl,
            initialDataFactory = { StarChallengeKillZombiesInTimeData() }
        ),
        ChallengeTypeInfo(
            title = "僵尸提速挑战",
            objClass = "StarChallengeZombieSpeedProps",
            defaultAlias = "ZombieSpeed",
            description = "所有僵尸的速度获得一定的增幅",
            icon = Icons.Default.Speed,
            initialDataFactory = { StarChallengeZombieSpeedData() }
        ),
        ChallengeTypeInfo(
            title = "阳光减收挑战",
            objClass = "StarChallengeSunReducedProps",
            defaultAlias = "SunReduced",
            description = "获取的阳光会被按照一定比例减少",
            icon = Icons.Default.BrightnessLow,
            initialDataFactory = { StarChallengeSunReducedData() }
        ),
        ChallengeTypeInfo(
            title = "植物限损挑战",
            objClass = "StarChallengePlantsLostProps",
            defaultAlias = "PlantsLost",
            description = "损失的植物不能超过一定限额",
            icon = Icons.Default.HeartBroken,
            initialDataFactory = { StarChallengePlantsLostData() }
        ),
        ChallengeTypeInfo(
            title = "限制种植数挑战",
            objClass = "StarChallengeSimultaneousPlantsProps",
            defaultAlias = "SimultaneousPlants",
            description = "限制所有植物同时在场的数量",
            icon = Icons.Default.Forest,
            initialDataFactory = { StarChallengeSimultaneousPlantsData() }
        ),
        ChallengeTypeInfo(
            title = "解冻植物挑战",
            objClass = "StarChallengeUnfreezePlantsProps",
            defaultAlias = "UnfreezePlants",
            description = "解冻一定数量的植物",
            icon = Icons.Default.AcUnit,
            initialDataFactory = { StarChallengeUnfreezePlantsData() }
        ),
        ChallengeTypeInfo(
            title = "吹飞僵尸挑战",
            objClass = "StarChallengeBlowZombieProps",
            defaultAlias = "BlowZombie",
            description = "吹飞一定数量的僵尸",
            icon = Icons.Default.Air,
            initialDataFactory = { StarChallengeBlowZombieData() }
        ),
        ChallengeTypeInfo(
            title = "获取积分挑战",
            objClass = "StarChallengeTargetScoreProps",
            defaultAlias = "ReachTheScore",
            description = "获取目标积分，需要开启关卡计分模块",
            icon = Icons.Default.Scoreboard,
            initialDataFactory = { StarChallengeTargetScoreData() }
        ),
    )

    /**
     * 搜索挑战
     */
    fun search(query: String): List<ChallengeTypeInfo> {
        if (query.isBlank()) return allChallenges

        val lowerQ = query.lowercase()
        return allChallenges.filter {
            it.title.lowercase().contains(lowerQ) ||
                    it.objClass.lowercase().contains(lowerQ) ||
                    it.defaultAlias.lowercase().contains(lowerQ)
        }
    }

    /**
     * 根据 ObjClass 获取信息
     */
    fun getInfo(objClass: String): ChallengeTypeInfo? {
        return allChallenges.find { it.objClass == objClass }
    }
}