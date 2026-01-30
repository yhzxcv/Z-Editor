package com.example.z_editor.data.repository

data class GridItemInfo(
    val typeName: String,
    val name: String,
    val category: GridItemCategory,
    val icon: String?
)

enum class GridItemCategory(val label: String) {
    All("全部物品"),
    Scene("场景布置"),
    Trap("互动陷阱"),
    Plants("生成物品")
}

/**
 * 障碍物数据仓库
 * 目前是静态数据，未来可扩展为从 GridItemTypes.json 加载
 */
object GridItemRepository {

    private val staticItems = listOf(
        GridItemInfo("gravestone_egypt", "埃及墓碑", GridItemCategory.Scene, "gravestone_egypt.png"),
        GridItemInfo("gravestone_dark", "黑暗墓碑", GridItemCategory.Scene, "gravestone_dark.png"),
        GridItemInfo("gravestoneZombieOnDestruction", "僵尸墓碑", GridItemCategory.Scene, "gravestone_dark.png"),
        GridItemInfo("gravestonePlantfoodOnDestruction", "能量豆墓碑", GridItemCategory.Scene, "gravestonePlantfoodOnDestruction.png"),
        GridItemInfo("gravestoneSunOnDestruction", "阳光墓碑", GridItemCategory.Scene, "gravestoneSunOnDestruction.png"),
        GridItemInfo("gravestone_battlez_sun", "联赛大阳光墓碑", GridItemCategory.Scene, "gravestoneSunOnDestruction.png"),

        GridItemInfo("heian_box_sun", "阳光赛钱箱", GridItemCategory.Scene, "heian_box_sun.png"),
        GridItemInfo("heian_box_plantfood", "能量豆赛钱箱", GridItemCategory.Scene, "heian_box_plantfood.png"),
        GridItemInfo("heian_box_levelup", "升级赛钱箱", GridItemCategory.Scene, "heian_box_levelup.png"),
        GridItemInfo("heian_box_seedpacket", "种子赛钱箱", GridItemCategory.Scene, "heian_box_seedpacket.png"),
        GridItemInfo("goldtile", "黄金地砖", GridItemCategory.Scene, "goldtile.png"),
        GridItemInfo("fake_mold", "霉菌地面", GridItemCategory.Scene, "fake_mold.png"),

        GridItemInfo("printer_small_paper", "纸屑", GridItemCategory.Scene, null),
        GridItemInfo("pvz1grid", "回忆锤僵尸墓碑", GridItemCategory.Scene, null),
        GridItemInfo("score_2x_tile", "联赛2倍分数砖", GridItemCategory.Scene, null),
        GridItemInfo("score_3x_tile", "联赛3倍分数砖", GridItemCategory.Scene, null),
        GridItemInfo("score_5x_tile", "联赛5倍分数砖", GridItemCategory.Scene, null),

        GridItemInfo("zombiepotion_speed", "疾速药水", GridItemCategory.Trap, "zombiepotion_speed.png"),
        GridItemInfo("zombiepotion_toughness", "坚韧药水", GridItemCategory.Trap, "zombiepotion_toughness.png"),
        GridItemInfo("zombiepotion_invisible", "隐身药水", GridItemCategory.Trap, "zombiepotion_invisible.png"),
        GridItemInfo("zombiepotion_poison", "剧毒药水", GridItemCategory.Trap, "zombiepotion_poison.png"),

        GridItemInfo("boulder_trap_falling_forward", "滚石陷阱", GridItemCategory.Trap, "boulder_trap_falling_forward.png"),
        GridItemInfo("flame_spreader_trap", "火焰陷阱", GridItemCategory.Trap, "flame_spreader_trap.png"),
        GridItemInfo("bufftile_shield", "护盾瓷砖", GridItemCategory.Trap, "bufftile_shield.png"),
        GridItemInfo("bufftile_speed", "疾速瓷砖", GridItemCategory.Trap, "bufftile_speed.png"),
        GridItemInfo("bufftile_attack", "攻击瓷砖", GridItemCategory.Trap, "bufftile_attack.png"),
        GridItemInfo("zombie_bound_tile", "僵尸跳板", GridItemCategory.Trap, "zombie_bound_tile.png"),
        GridItemInfo("zombie_changer", "僵尸改造机", GridItemCategory.Trap, "zombie_changer.png"),

        GridItemInfo("slider_up", "上行冰河浮冰", GridItemCategory.Trap, "slider_up.png"),
        GridItemInfo("slider_down", "下行冰河浮冰", GridItemCategory.Trap, "slider_down.png"),
        GridItemInfo("slider_up_modern", "上行摩登浮标", GridItemCategory.Trap, "slider_up_modern.png"),
        GridItemInfo("slider_down_modern", "下行摩登浮标", GridItemCategory.Trap, "slider_down_modern.png"),

        GridItemInfo("christmas_protect", "元宝", GridItemCategory.Trap, null),
        GridItemInfo("dumpling", "饺子", GridItemCategory.Trap, null),
        GridItemInfo("turkey", "火鸡", GridItemCategory.Trap, null),
        GridItemInfo("tangyuan", "汤圆", GridItemCategory.Trap, null),

        GridItemInfo("lilypad", "莲叶", GridItemCategory.Plants, null),
        GridItemInfo("flowerpot", "花盆", GridItemCategory.Plants, null),
        GridItemInfo("FrozenIcebloom", "寒冰蓓蕾", GridItemCategory.Plants, null),
        GridItemInfo("FrozenChillyPepper", "寒冰辣椒", GridItemCategory.Plants, null),

        GridItemInfo("cavalrygun", "骑兵长枪", GridItemCategory.Plants, null),
        GridItemInfo("surfboard", "冲浪板", GridItemCategory.Plants, null),
        GridItemInfo("backpack", "冒险家背包", GridItemCategory.Plants, null),
        GridItemInfo("eightiesarcadecabinet", "街机", GridItemCategory.Plants, null),
        GridItemInfo("gridItem_sushi", "寿司", GridItemCategory.Plants, null),
        GridItemInfo("dinoegg_zomshell", "恐龙蛋-小鬼", GridItemCategory.Plants, null),
        GridItemInfo("dinoegg_ptero", "恐龙蛋-翼龙", GridItemCategory.Plants, null),
        GridItemInfo("dinoegg_bronto", "恐龙蛋-雷龙", GridItemCategory.Plants, null),
        GridItemInfo("dinoegg_tyranno", "恐龙蛋-霸王龙", GridItemCategory.Plants, null),
        GridItemInfo("lollipops", "棒棒糖", GridItemCategory.Plants, null),
        GridItemInfo("gliding", "飞行器残骸", GridItemCategory.Plants, null),
        GridItemInfo("heavy_shield", "近卫重盾", GridItemCategory.Plants, null),
    )

    val allItems: List<GridItemInfo>
        get() = staticItems

    private val validAliasesSet: Set<String> by lazy {
        allItems.map { it.typeName }.toSet()
    }

    fun getByCategory(category: GridItemCategory): List<GridItemInfo> {
        return if (category == GridItemCategory.All) {
            allItems
        } else {
            allItems.filter { it.category == category }
        }
    }

    fun getName(typeName: String): String {
        val staticName = allItems.find { it.typeName == typeName }?.name
        if (staticName != null) return staticName
        return typeName
    }

    fun getIconPath(typeName: String): String? {
        val icon = allItems.find { it.typeName == typeName }?.icon
        return if (icon != null) "images/griditems/$icon" else null
    }

    fun isValid(typeName: String): Boolean {
        if (allItems.any { it.typeName == typeName }) return true
        return ReferenceRepository.isValidGridItem(typeName)
    }

    // 用于将 TypeName 转化为 RTID 包裹的代号
    fun buildGridAliases(id: String): String {
        return if (id == "gravestone_egypt") "gravestone"
        else id
    }

    fun search(query: String): List<GridItemInfo> {
        if (query.isBlank()) return allItems
        return allItems.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.typeName.contains(query, ignoreCase = true)
        }
    }
}