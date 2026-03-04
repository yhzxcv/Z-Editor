package com.example.z_editor.data.repository

import androidx.compose.ui.graphics.vector.ImageVector

data class ModuleUIInfo(
    val rtid: String,
    val alias: String,
    val objClass: String,
    val friendlyName: String,
    val description: String,
    val icon: ImageVector,
    val isCore: Boolean
)

data class ModuleConflictRule(
    val conflictingClasses: Set<String>,
    val title: String = "模块逻辑冲突",
    val description: String? = null
)

object ConflictRegistry {
    val rules = listOf(
        ModuleConflictRule(
            conflictingClasses = setOf("SeedBankProperties", "ConveyorSeedBankProperties"),
            description = "种子库与传送带模块的ui会相互遮挡，而且有可能闪退，需要确保种子库处于预选模式。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("VaseBreakerPresetProperties", "StandardLevelIntroProperties"),
            description = "砸罐子模式下不需要添加开局转场动画。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("LastStandMinigameProperties", "StandardLevelIntroProperties"),
            description = "坚不可摧模式下不需要添加开局转场动画。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("LastStandMinigameProperties", "ZombossBattleModuleProperties"),
            description = "僵王战需要特殊的僵王战坚不可摧模式开启。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("EvilDaveProperties", "ZombiesDeadWinConProperties"),
            description = "我是僵尸模式下不能添加僵尸掉落模块。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("EvilDaveProperties", "ZombiesAteYourBrainsProperties"),
            description = "我是僵尸模式下不能添加僵尸胜利判定。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("ZombossBattleModuleProperties", "ZombiesDeadWinConProperties"),
            description = "僵王战模式下使用死亡掉落会导致无法正常结算。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("ZombossBattleIntroProperties", "StandardLevelIntroProperties"),
            description = "两种关卡开局转场不能同时出现，否则僵王血量无法正常显示。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("InitialPlantEntryProperties", "RoofProperties"),
            description = "在屋顶无法进行预置植物，会引发闪退。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("ProtectThePlantChallengeProperties", "RoofProperties"),
            description = "在屋顶无法进行预置植物，会引发闪退。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("CustomLevelModuleProperties", "LawnMowerProperties"),
            description = "庭院模块下使用小推车无效。"
        )
    )
}