package com.example.z_editor.data.repository

enum class StageType {
    All,
    Main,
    Extra,
    Seasons,
    Special
}

data class StageItem(
    val alias: String,
    val name: String,
    val iconName: String?,
    val type: StageType
)

object StageRepository {
    private val STAGE_DATABASE = listOf(

        StageItem("TutorialStage", "教程庭院", "Stage_Modern.png", StageType.Main),
        StageItem("EgyptStage", "神秘埃及", "Stage_Egypt.png", StageType.Main),
        StageItem("PirateStage", "海盗港湾", "Stage_Pirate.png", StageType.Main),
        StageItem("WestStage", "狂野西部", "Stage_West.png", StageType.Main),
        StageItem("KongfuStage", "功夫世界", "Stage_Kongfu.png", StageType.Main),
        StageItem("FutureStage", "遥远未来", "Stage_Future.png", StageType.Main),
        StageItem("DarkStage", "黑暗时代", "Stage_Dark.png", StageType.Main),
        StageItem("BeachStage", "巨浪沙滩", "Stage_Beach.png", StageType.Main),
        StageItem("IceageStage", "冰河世纪", "Stage_Iceage.png", StageType.Main),
        StageItem("LostCityStage", "失落之城", "Stage_LostCity.png", StageType.Main),
        StageItem("SkycityStage", "天空之城", "Stage_Skycity.png", StageType.Main),
        StageItem("EightiesStage", "摇滚年代", "Stage_Eighties.png", StageType.Main),
        StageItem("DinoStage", "恐龙危机", "Stage_Dino.png", StageType.Main),
        StageItem("ModernStage", "现代世界", "Stage_Modern.png", StageType.Main),
        StageItem("SteamStage", "蒸汽时代", "Stage_Steam.png", StageType.Main),
        StageItem("RenaiStage", "复兴时代", "Stage_Renai.png", StageType.Main),
        StageItem("HeianStage", "平安时代", "Stage_Heian.png", StageType.Main),
        StageItem("DeepseaStage", "深海地图", "Stage_Atlantis.png", StageType.Main),
        StageItem("DeepseaLandStage", "亚特兰蒂斯", "Stage_Atlantis.png", StageType.Main),

        StageItem("FairyTaleStage", "童话森林", null, StageType.Extra),
        StageItem("ZCorpStage", "Z公司", null, StageType.Extra),
        StageItem("FrontLawnSpringStage", "复活节", null, StageType.Extra),
        StageItem("ChildrenDayStage", "儿童节", null, StageType.Extra),
        StageItem("HalloweenStage", "万圣节", null, StageType.Extra),
        StageItem("UnchartedAnniversaryStage", "周年庆", null, StageType.Extra),
        StageItem("VacationLostCityStage", "失落火山", null, StageType.Extra),
        StageItem("UnchartedIceageStage", "冰河再临", null, StageType.Extra),
        StageItem("RunningNormalStage", "地铁酷跑联动", null, StageType.Extra),
        StageItem("UnchartedNeedforspeedStage", "极品飞车联动", null, StageType.Extra),
        StageItem("UnchartedNo42UniverseStage", "平行宇宙秘境", null, StageType.Extra),
        StageItem("JourneyToTheWestStage", "西游地图", null, StageType.Extra),
        StageItem("RiftStage", "潘妮的追击", null, StageType.Extra),
        StageItem("JoustStage", "超Z联赛", null, StageType.Extra),

        StageItem("TwisterStage", "前院白天", null, StageType.Seasons),
        StageItem("NightStage", "前院夜晚", null, StageType.Seasons),
        StageItem("PoolDaylightStage", "泳池白天", null, StageType.Seasons),
        StageItem("PoolNightStage", "泳池夜晚", null, StageType.Seasons),
        StageItem("RoofStage", "屋顶白天", null, StageType.Seasons),
        StageItem("RoofNightStage", "屋顶夜晚", null, StageType.Seasons),
        StageItem("NewYearDaylightStage", "新春白天", null, StageType.Seasons),
        StageItem("NewYearNightStage", "新春黑夜", null, StageType.Seasons),
        StageItem("SpringDaylightStage", "春日白天", null, StageType.Seasons),
        StageItem("SpringNightStage", "春日夜晚", null, StageType.Seasons),
        StageItem("SummerDaylightStage", "仲夏白天", null, StageType.Seasons),
        StageItem("SummerNightStage", "仲夏夜晚", null, StageType.Seasons),
        StageItem("AutumnEarlyStage", "秋季初秋", null, StageType.Seasons),
        StageItem("AutumnLateStage", "秋季晚秋", null, StageType.Seasons),
        StageItem("SnowModernStage", "冬日白天", null, StageType.Seasons),
        StageItem("SnowNightStage", "冬日夜晚", null, StageType.Seasons),
        StageItem("SnowRoofStage", "冬日屋顶", null, StageType.Seasons),
        StageItem("UnchartedArbordayStage", "踏雪寻春", null, StageType.Seasons),

        StageItem("TheatreDarkStage", "黑暗剧院", null, StageType.Special),
        StageItem("BeachSnakeStage", "鳄梨贪吃蛇", null, StageType.Special),
        StageItem("IceageRiverCrossingStage", "渡渡鸟历险", null, StageType.Special),
        StageItem("IceageEliminateStage", "冰河连连看", null, StageType.Special),
        StageItem("SkycityFishingStage", "一炮当关", null, StageType.Special),
        StageItem("SkycityPooyanStage", "壮植凌云", null, StageType.Special),
        StageItem("AquariumStage", "水族馆", null, StageType.Special),
        StageItem("BowlingStage", "保龄球", null, StageType.Special),
        StageItem("WhackAMoleStage", "锤僵尸", null, StageType.Special),
        StageItem("CardGameStage", "牌面纷争", null, StageType.Special),
        StageItem("OverwhelmStage", "排山倒海", null, StageType.Special),
        StageItem("OverwhelmSnowModernStage", "冬日排山倒海", null, StageType.Special),
        StageItem("OverwhelmSnowRoofStage", "冬日排山倒海屋顶", null, StageType.Special),
        StageItem("OverwhelmSnowNightStage", "冬日排山倒海夜晚", null, StageType.Special)
    )


    val allItems: List<StageItem>
        get() = STAGE_DATABASE

    fun getByType(type: StageType): List<StageItem> {
        return if (type == StageType.All) {
            allItems
        } else {
            allItems.filter { it.type == type }
        }
    }
}