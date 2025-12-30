package com.example.pvz2leveleditor.views.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewModule
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// === 数据结构保持不变 ===
data class GridItemInfo(val typeName: String, val name: String, val category: GridItemCategory)

enum class GridItemCategory(val label: String) {
    Scene("场景布置"),
    Trap("陷阱强化")
}

object GridItemRepository {
    val allItems = listOf(
        GridItemInfo("gravestone_egypt", "埃及墓碑", GridItemCategory.Scene),
        GridItemInfo("gravestone_dark", "黑暗墓碑", GridItemCategory.Scene),
        GridItemInfo("gravestoneSunOnDestruction", "阳光墓碑", GridItemCategory.Scene),
        GridItemInfo("gravestonePlantfoodOnDestruction", "能量豆墓碑", GridItemCategory.Scene),

        GridItemInfo("heian_box_sun", "阳光赛钱箱", GridItemCategory.Scene),
        GridItemInfo("heian_box_plantfood", "能量豆赛钱箱", GridItemCategory.Scene),
        GridItemInfo("heian_box_levelup", "升级赛钱箱", GridItemCategory.Scene),
        GridItemInfo("heian_box_seedpacket", "种子赛钱箱", GridItemCategory.Scene),

        GridItemInfo("slider_up", "上行冰河浮冰", GridItemCategory.Scene),
        GridItemInfo("slider_down", "下行冰河浮冰", GridItemCategory.Scene),
        GridItemInfo("slider_up_modern", "上行摩登浮标", GridItemCategory.Scene),
        GridItemInfo("slider_down_modern", "下行摩登浮标", GridItemCategory.Scene),
        GridItemInfo("goldtile", "黄金地砖", GridItemCategory.Scene),
        GridItemInfo("fake_mold", "霉菌地面", GridItemCategory.Scene),
        GridItemInfo("printer_small_paper", "小团纸屑", GridItemCategory.Scene),

        GridItemInfo("boulder_trap_falling_forward", "滚石陷阱", GridItemCategory.Trap),
        GridItemInfo("flame_spreader_trap", "火焰陷阱", GridItemCategory.Trap),
        GridItemInfo("bufftile_shield", "护盾瓷砖", GridItemCategory.Trap),
        GridItemInfo("bufftile_speed", "疾速瓷砖", GridItemCategory.Trap),
        GridItemInfo("bufftile_attack", "攻击瓷砖", GridItemCategory.Trap),
        GridItemInfo("zombie_bound_tile", "僵尸跳板", GridItemCategory.Trap),
        GridItemInfo("zombie_changer", "僵尸改造机", GridItemCategory.Trap),

        GridItemInfo("zombiepotion_speed", "疾速药水", GridItemCategory.Trap),
        GridItemInfo("zombiepotion_toughness", "坚韧药水", GridItemCategory.Trap),
        GridItemInfo("zombiepotion_invisible", "隐身药水", GridItemCategory.Trap),
        GridItemInfo("zombiepotion_poison", "剧毒药水", GridItemCategory.Trap),
    )

    fun getByCategory(category: GridItemCategory): List<GridItemInfo> {
        return allItems.filter { it.category == category }
    }

    fun getName(typeName: String): String {
        return allItems.find { it.typeName == typeName }?.name ?: typeName
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridItemSelectionScreen(
    onGridItemSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(GridItemCategory.Scene) }

    // 过滤逻辑：同时匹配 分类 和 搜索词
    val displayList = remember(searchQuery, selectedCategory) {
        GridItemRepository.getByCategory(selectedCategory).filter {
            searchQuery.isBlank() ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.typeName.contains(searchQuery, ignoreCase = true)
        }
    }

    val themeColor = Color(0xFF795548) // 褐色主题，对应障碍物/泥土

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
                    // --- 顶部搜索栏区域 (完全复刻 StageSelectionScreen) ---
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
                                    "搜索物品...",
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
                        selectedTabIndex = GridItemCategory.values().indexOf(selectedCategory),
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            val index = GridItemCategory.values().indexOf(selectedCategory)
                            if (index < tabPositions.size) {
                                SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[index]),
                                    color = Color.White,
                                    height = 3.dp
                                )
                            }
                        },
                    ) {
                        GridItemCategory.values().forEach { category ->
                            Tab(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                text = { Text(category.label, fontWeight = FontWeight.Bold) },
                                unselectedContentColor = Color.White.copy(alpha = 0.7f)
                            )
                        }
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
                columns = GridCells.Adaptive(minSize = 160.dp), // 自适应宽度，每行通常2个
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayList) { item ->
                    GridItemSelectionCard(
                        item = item,
                        onClick = { onGridItemSelected(item.typeName) }
                    )
                }
            }

            // 空状态提示
            if (displayList.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ViewModule,
                        null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("未找到相关物品", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun GridItemSelectionCard(item: GridItemInfo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.typeName,
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