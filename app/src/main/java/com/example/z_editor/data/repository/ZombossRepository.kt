package com.example.z_editor.data.repository

data class ZombossInfo(
    val id: String,
    val name: String,
    val icon: String,
    val tag: ZombossTag,
    val defaultPhaseCount: Int
)

enum class ZombossTag(val label: String) {
    All("全部僵王"),
    Main("主线/摩登"),
    Challenge("挑战/周年"),
    Pvz1("回忆之旅"),
}

object ZombossRepository {
    val allZombosses = listOf(

        ZombossInfo("zombossmech_egypt", "神秘埃及僵王 (狮身终结者)", "zombossmech_egypt.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_pirate", "海盗港湾僵王 (甲板漫步者)", "zombossmech_pirate.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_cowboy", "狂野西部僵王 (牛车格斗者)", "zombossmech_cowboy.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_future", "遥远未来僵王 (明日破译者)", "zombossmech_future.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_dark", "黑暗时代僵王 (巨龙驾驭者)", "zombossmech_dark.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_beach", "巨浪沙滩僵王 (狂鲨潜袭者)", "zombossmech_beach.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_iceage", "冰河世界僵王 (獠牙征服者)", "zombossmech_iceage.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_skycity", "天空之城僵王 (秃鹫战机)", "zombossmech_skycity.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_lostcity", "失落之城僵王 (云霄航行者)", "zombossmech_lostcity.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_eighties", "摇滚年代僵王 (狂舞灭碎者)", "zombossmech_eighties.webp", ZombossTag.Main, 5),
        ZombossInfo("zombossmech_dino", "恐龙危机僵王 (金刚咆哮者)", "zombossmech_dino.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_steam", "蒸汽时代僵王 (蒸汽火车头)", "zombossmech_steam.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_renai", "复兴时代僵王 (剧团操纵者)", "zombossmech_renai.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_hydra", "童话森林僵王 (魔咒吟唱者)", "zombossmech_hydra.webp", ZombossTag.Main, 3),

        ZombossInfo("zombossmech_modern_egypt", "摩登埃及僵王 (狮身终结者)", "zombossmech_egypt.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_modern_pirate", "摩登海盗僵王 (甲板漫步者)", "zombossmech_pirate.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_modern_cowboy", "摩登西部僵王 (牛车格斗者)", "zombossmech_cowboy.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_modern_future", "摩登未来僵王 (明日破译者)", "zombossmech_future.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_modern_dark", "摩登黑暗僵王 (巨龙驾驭者)", "zombossmech_dark.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_modern_beach", "摩登沙滩僵王 (狂鲨潜袭者)", "zombossmech_beach.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_modern_iceage", "摩登冰河僵王 (獠牙征服者)", "zombossmech_iceage.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_modern_lostcity", "摩登失落僵王 (云霄航行者)", "zombossmech_lostcity.webp", ZombossTag.Main, 3),
        ZombossInfo("zombossmech_modern_eighties", "摩登摇滚僵王 (狂舞灭碎者)", "zombossmech_eighties.webp", ZombossTag.Main, 5),
        ZombossInfo("zombossmech_modern_dino", "摩登恐龙僵王 (金刚咆哮者)", "zombossmech_dino.webp", ZombossTag.Main, 3),

        ZombossInfo("zombossmech_egypt_vacation", "挑战埃及僵王 (狮身终结者)", "zombossmech_egypt.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_pirate_vacation", "挑战海盗僵王 (甲板漫步者)", "zombossmech_pirate.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_cowboy_vacation", "挑战西部僵王 (牛车格斗者)", "zombossmech_cowboy.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_future_vacation", "挑战未来僵王 (明日破译者)", "zombossmech_future.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_dark_vacation", "挑战黑暗僵王 (巨龙驾驭者)", "zombossmech_dark.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_beach_vacation", "挑战沙滩僵王 (狂鲨潜袭者)", "zombossmech_beach.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_iceage_vacation", "挑战冰河僵王 (獠牙征服者)", "zombossmech_iceage.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_skycity_vacation", "挑战天空僵王 (秃鹫战机)", "zombossmech_skycity.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_lostcity_vacation", "挑战失落僵王 (云霄航行者)", "zombossmech_lostcity.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_eighties_vacation", "挑战摇滚僵王 (狂舞灭碎者)", "zombossmech_eighties.webp", ZombossTag.Challenge, 5),
        ZombossInfo("zombossmech_dino_vacation", "挑战恐龙僵王 (金刚咆哮者)", "zombossmech_dino.webp", ZombossTag.Challenge, 3),

        ZombossInfo("zombossmech_egypt_12th", "周年埃及僵王 (狮身终结者)", "zombossmech_egypt.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_pirate_12th", "周年海盗僵王 (甲板漫步者)", "zombossmech_pirate.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_cowboy_12th", "周年西部僵王 (牛车格斗者)", "zombossmech_cowboy.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_future_12th", "周年未来僵王 (明日破译者)", "zombossmech_future.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_dark_12th", "周年黑暗僵王 (巨龙驾驭者)", "zombossmech_dark.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_beach_12th", "周年沙滩僵王 (狂鲨潜袭者)", "zombossmech_beach.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_iceage_12th", "周年冰河僵王 (獠牙征服者)", "zombossmech_iceage.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_skycity_12th", "周年天空僵王 (秃鹫战机)", "zombossmech_skycity.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_lostcity_12th", "周年失落僵王 (云霄航行者)", "zombossmech_lostcity.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_eighties_12th", "周年摇滚僵王 (狂舞灭碎者)", "zombossmech_eighties.webp", ZombossTag.Challenge, 5),
        ZombossInfo("zombossmech_dino_12th", "周年恐龙僵王 (金刚咆哮者)", "zombossmech_dino.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_steam_12th", "周年蒸汽僵王 (蒸汽火车头)", "zombossmech_steam.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_renai_12th", "周年复兴僵王 (剧团操纵者)", "zombossmech_renai.webp", ZombossTag.Challenge, 3),
        ZombossInfo("zombossmech_hydra_12th", "周年童话僵王 (魔咒吟唱者)", "zombossmech_hydra.webp", ZombossTag.Challenge, 3),

        ZombossInfo("zombossmech_pvz1_robot_normal", "无畏者2号简单版 (优化版本)", "zombossmech_pvz1_robot.webp", ZombossTag.Pvz1, 4),
        ZombossInfo("zombossmech_pvz1_robot_hard", "无畏者2号困难版 (优化版本)", "zombossmech_pvz1_robot.webp", ZombossTag.Pvz1, 4),
        ZombossInfo("zombossmech_pvz1_robot_1", "无畏者2号第1种 (旧版回忆)", "zombossmech_pvz1_robot.webp", ZombossTag.Pvz1, 4),
        ZombossInfo("zombossmech_pvz1_robot_2", "无畏者2号第2种 (旧版回忆)", "zombossmech_pvz1_robot.webp", ZombossTag.Pvz1, 4),
        ZombossInfo("zombossmech_pvz1_robot_3", "无畏者2号第3种 (旧版回忆)", "zombossmech_pvz1_robot.webp", ZombossTag.Pvz1, 4),
        ZombossInfo("zombossmech_pvz1_robot_4", "无畏者2号第4种 (旧版回忆)", "zombossmech_pvz1_robot.webp", ZombossTag.Pvz1, 4),
        ZombossInfo("zombossmech_pvz1_robot_5", "无畏者2号第5种 (旧版回忆)", "zombossmech_pvz1_robot.webp", ZombossTag.Pvz1, 4),
        ZombossInfo("zombossmech_pvz1_robot_6", "无畏者2号第6种 (旧版回忆)", "zombossmech_pvz1_robot.webp", ZombossTag.Pvz1, 4),
        ZombossInfo("zombossmech_pvz1_robot_7", "无畏者2号第7种 (旧版回忆)", "zombossmech_pvz1_robot.webp", ZombossTag.Pvz1, 4),
        ZombossInfo("zombossmech_pvz1_robot_8", "无畏者2号第8种 (旧版回忆)", "zombossmech_pvz1_robot.webp", ZombossTag.Pvz1, 4),
        ZombossInfo("zombossmech_pvz1_robot_9", "无畏者2号第9种 (旧版回忆)", "zombossmech_pvz1_robot.webp", ZombossTag.Pvz1, 4),
    )

    fun get(id: String): ZombossInfo? = allZombosses.find { it.id == id }

    fun getName(id: String): String = get(id)?.name ?: id

    fun search(query: String, selectedTag: ZombossTag): List<ZombossInfo> {
        val tagFiltered = if (selectedTag == ZombossTag.All) {
            allZombosses
        } else {
            allZombosses.filter { it.tag == selectedTag }
        }

        if (query.isBlank()) return tagFiltered

        val lowerQ = query.lowercase()
        return tagFiltered.filter {
            it.id.lowercase().contains(lowerQ) || it.name.contains(lowerQ)
        }
    }
}