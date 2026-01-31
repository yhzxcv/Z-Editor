package com.example.z_editor.data.repository

data class PortalWorldDef(
    val typeCode: String,
    val name: String,
    val representativeZombies: List<String>
)
object PortalRepository {
    val PORTAL_DEFINITIONS = listOf(
    PortalWorldDef("egypt", "主线埃及", listOf("ra", "tomb_raiser", "pharaoh")),
    PortalWorldDef("egypt_2", "埃及2号", listOf("explorer")),
    PortalWorldDef("pirate", "主线海盗", listOf("pirate_captain", "seagull", "barrelroller")),
    PortalWorldDef("west", "主线西部", listOf("piano", "prospector", "poncho_plate")),
    PortalWorldDef("future", "主线未来", listOf("future_protector", "mech_cone", "football_mech")),
    PortalWorldDef("future_2", "未来2号", listOf("future_jetpack", "mech_cone", "future_armor1")),
    PortalWorldDef("dark", "主线黑暗", listOf("dark_juggler")),
    PortalWorldDef("beach", "主线沙滩", listOf("beach_octopus", "beach_surfer")),
    PortalWorldDef("iceage", "主线冰河", listOf("iceage_hunter", "iceage_weaselhoarder")),
    PortalWorldDef("lostcity", "主线失落", listOf("lostcity_excavator", "lostcity_jane")),
    PortalWorldDef("eighties", "主线摇滚", listOf("eighties_breakdancer", "eighties_mc")),
    PortalWorldDef("dino", "主线恐龙", listOf("dino_imp", "dino_bully")),
    PortalWorldDef("dangerroom_egypt", "无尽埃及", listOf("ra", "explorer", "pharaoh")),
    PortalWorldDef("dangerroom_pirate", "无尽海盗", listOf("pirate_captain", "seagull", "barrelroller")),
    PortalWorldDef("dangerroom_west", "无尽西部", listOf("piano", "chicken_farmer", "poncho")),
    PortalWorldDef("dangerroom_future", "无尽未来", listOf("future_jetpack", "future_protector", "mech_cone")),
    PortalWorldDef("dangerroom_dark", "无尽黑暗", listOf("dark_armor3", "dark_juggler", "dark_wizard")),
    PortalWorldDef("dangerroom_beach", "无尽沙滩", listOf("beach_surfer", "beach_snorkel", "beach_octopus")),
    PortalWorldDef("dangerroom_iceage", "无尽冰河", listOf("iceage_dodo", "iceage_weaselhoarder", "iceage_armor3")),
    PortalWorldDef("dangerroom_lostcity", "无尽失落", listOf("lostcity_bug", "lostcity_excavator", "lostcity_crystalskull")),
    PortalWorldDef("dangerroom_eighties", "无尽摇滚", listOf("eighties_8bit_armor1", "eighties_8bit_armor2", "eighties_boombox")),
    PortalWorldDef("dangerroom_dino", "无尽恐龙", listOf("dino_bully", "dino_imp", "dino_armor3")),
    PortalWorldDef("pvz1_Zombotany", "植物僵尸", listOf("zombie_snowpea", "zombie_gatlingpea", "zombie_explodenut", "zombie_jalapeno")),
    PortalWorldDef("pvz1_Slime", "史莱姆僵尸", listOf("slimes")),
    PortalWorldDef("pvz1_Universe", "42号宇宙", listOf("universe_uncharted_lostcity_jane", "universe_uncharted_allstar", "universe_uncharted_lostcity_excavator", "universe_uncharted_prospector")),
    PortalWorldDef("pvz1_Uncharted", "41号宇宙", listOf("uncharted_qigong", "uncharted_crystalskull", "uncharted_miner", "uncharted_gentleman")),
    PortalWorldDef("pvz1_elite_roman_healer_normal", "普通治愈者", listOf("elite_roman_healer")),
    PortalWorldDef("pvz1_elite_skycity_electric_normal", "普通闪电枪", listOf("elite_skycity_electric")),
    PortalWorldDef("pvz1_elite_roman_ballista_normal", "普通投罐车", listOf("elite_roman_ballista")),
    PortalWorldDef("pvz1_elite_heian_onmyoji_normal", "普通阴阳师", listOf("elite_heian_onmyoji")),
    PortalWorldDef("pvz1_elite_roman_healer_hard", "困难治愈者", listOf("elite_roman_healer")),
    PortalWorldDef("pvz1_elite_skycity_electric_hard", "困难闪电枪", listOf("elite_skycity_electric")),
    PortalWorldDef("pvz1_elite_roman_ballista_hard", "困难投罐车", listOf("elite_roman_ballista")),
    PortalWorldDef("pvz1_elite_heian_onmyoji_hard", "困难阴阳师", listOf("elite_heian_onmyoji")),
    PortalWorldDef("iceage_hunter_elite", "精英猎人", listOf("iceage_hunter_elite")),
    PortalWorldDef("iceage_chief_elite", "精英酋长", listOf("iceage_chief_elite")),
    PortalWorldDef("iceage_weaselhoarder_elite", "精英冰鼬", listOf("iceage_weaselhoarder_elite")),
    PortalWorldDef("bumpercar_elite", "精英碰碰车", listOf("bumpercar_elite")),
    PortalWorldDef("dark_wizard_elite", "精英巫师", listOf("dark_wizard_elite")),
    PortalWorldDef("dark_king_elite", "精英国王", listOf("dark_king_elite")),
)

}