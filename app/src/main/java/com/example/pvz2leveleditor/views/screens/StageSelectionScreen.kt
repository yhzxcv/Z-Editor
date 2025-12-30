package com.example.pvz2leveleditor.views.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells.Adaptive
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pvz2leveleditor.data.RtidParser
import com.example.pvz2leveleditor.views.components.AssetImage

// === 1. 数据结构 ===

enum class StageType {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StageSelectionScreen(
    onStageSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    val displayStages = remember(searchQuery, selectedTab) {
        val targetType =
            if (selectedTab == 0) StageType.Main else if (selectedTab == 1) StageType.Extra else if (selectedTab == 2) StageType.Seasons else StageType.Special
        STAGE_DATABASE.filter {
            it.type == targetType &&
                    (searchQuery.isBlank() ||
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            it.alias.contains(searchQuery, ignoreCase = true))
        }
    }

    val themeColor = Color(0xFF388E3C)

    Scaffold(
        topBar = {
            Surface(
                color = themeColor,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    // --- 顶部搜索栏区域 (仿照 PlantSelectionScreen) ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        Spacer(Modifier.width(16.dp))

                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "搜索地图",
                                    fontSize = 16.sp,
                                    color = Color.Gray
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(25.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = themeColor
                            ),
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                    }) { Icon(Icons.Default.Clear, null, tint = Color.Gray) }
                                }
                            } else null,
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )
                    }

                    // --- Tab 分类 ---
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = Color.White,
                                    height = 3.dp
                                )
                            }
                        },
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("主线世界", fontWeight = FontWeight.Bold) },
                            unselectedContentColor = Color.White.copy(alpha = 0.7f)
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("活动/秘境", fontWeight = FontWeight.Bold) },
                            unselectedContentColor = Color.White.copy(alpha = 0.7f)
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("一代/季节", fontWeight = FontWeight.Bold) },
                            unselectedContentColor = Color.White.copy(alpha = 0.7f)
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("小游戏", fontWeight = FontWeight.Bold) },
                            unselectedContentColor = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            LazyVerticalGrid(
                columns = Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayStages) { stage ->
                    StageGridItem(
                        stage = stage,
                        onClick = {
                            val rtid = RtidParser.build(stage.alias, "LevelModules")
                            onStageSelected(rtid)
                        }
                    )
                }
            }

            if (displayStages.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ImageNotSupported,
                        null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("未找到相关地图", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun StageGridItem(
    stage: StageItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (stage.iconName != null) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE0E0E0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AssetImage(
                        path = "images/stages/${stage.iconName}",
                        contentDescription = stage.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFEEEEEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stage.alias.take(1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            // 文字区域
            Text(
                text = stage.name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = stage.alias,
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 12.sp
            )
        }
    }
}