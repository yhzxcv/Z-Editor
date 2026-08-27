package com.example.z_editor.views.editor.pages.module

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.LunarMineVeinModulePropertiesData
import com.example.z_editor.data.LunarMineVeinPlacementData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzBluePrimary
import com.example.z_editor.ui.theme.PvzGridHighLight
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorContentWindowInsets
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

private const val VEIN_ICON_PATH = "images/griditems/mine_vein.webp"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunarMineVeinModulePropertiesEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, LunarMineVeinModulePropertiesData::class.java)
    val moduleDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    var selectedX by remember { mutableIntStateOf(0) }
    var selectedY by remember { mutableIntStateOf(0) }

    val sortedItems = remember(moduleDataState.value.veinPlacements) {
        moduleDataState.value.veinPlacements.sortedWith(compareBy({ it.gridY }, { it.gridX }))
    }

    fun addAtSelected() {
        // 每个格子最多一条矿脉，已存在时静默忽略
        val hasExisting = moduleDataState.value.veinPlacements.any {
            it.gridX == selectedX && it.gridY == selectedY
        }
        if (hasExisting) return
        val newList = moduleDataState.value.veinPlacements.toMutableList()
        newList.add(
            LunarMineVeinPlacementData(
                gridX = selectedX,
                gridY = selectedY,
                emergenceWave = 1
            )
        )
        moduleDataState.value = moduleDataState.value.copy(veinPlacements = newList)
        sync()
    }

    fun deleteItem(targetItem: LunarMineVeinPlacementData) {
        val newList = moduleDataState.value.veinPlacements.toMutableList()
        newList.remove(targetItem)
        moduleDataState.value = moduleDataState.value.copy(veinPlacements = newList)
        sync()
    }

    fun updateWave(targetItem: LunarMineVeinPlacementData, wave: Int) {
        val newList = moduleDataState.value.veinPlacements.toMutableList()
        val index = newList.indexOfFirst { it === targetItem }
        if (index != -1) {
            newList[index] = targetItem.copy(emergenceWave = wave)
            moduleDataState.value = moduleDataState.value.copy(veinPlacements = newList)
            sync()
        }
    }

    // 矿脉详情弹窗 (删除确认)
    var itemToDelete by remember { mutableStateOf<LunarMineVeinPlacementData?>(null) }

    val isDark = LocalDarkTheme.current
    val themeColor = PvzBluePrimary

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("移除矿脉", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "确定要移除 R${itemToDelete!!.gridY + 1}:C${itemToDelete!!.gridX + 1} 处的矿脉吗？"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteItem(itemToDelete!!)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onError)
                ) { Text("移除") }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("取消") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        contentWindowInsets = EditorContentWindowInsets(),
        topBar = {
            CommonEditorTopAppBar(
                title = "月球矿脉布局",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "月球矿脉模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "用于在月球基地地图上放置正在生长的矿脉资源。开局即可采集的矿脉可在障碍物预置障碍物模块放置。"
                )
                HelpSection(
                    title = "出现波次",
                    body = "可以设置矿脉出现的波次数，对应关卡第几波开始出现该矿脉。"
                )
                HelpSection(
                    title = "格点坐标",
                    body = "矿脉位置用网格坐标显示，每个格子最多放置一条生长中矿脉。"
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // === 区域 1: 网格选择器 (作为列表头，跨满全宽) ===
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.widthIn(max = 480.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(
                                        "选中位置",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "R${selectedY + 1} : C${selectedX + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = themeColor
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                Button(
                                    onClick = { addAtSelected() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = themeColor
                                    )
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("添加矿脉")
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.8f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0xFF31383B) else Color(0xFFD7ECF1))
                                    .border(1.dp, Color(0xFF6B899A), RoundedCornerShape(6.dp))
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    for (row in 0..4) {
                                        Row(Modifier.weight(1f)) {
                                            for (col in 0..8) {
                                                val isSelected =
                                                    (row == selectedY && col == selectedX)
                                                val hasVein =
                                                    moduleDataState.value.veinPlacements.any { it.gridX == col && it.gridY == row }

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .border(
                                                            0.5.dp,
                                                            if (isSelected) themeColor else Color(
                                                                0xFF6B899A
                                                            )
                                                        )
                                                        .background(
                                                            if (isSelected) PvzGridHighLight
                                                            else Color.Transparent
                                                        )
                                                        .clickable {
                                                            selectedX = col
                                                            selectedY = row
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (hasVein) {
                                                        AssetImage(
                                                            path = VEIN_ICON_PATH,
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .fillMaxSize(0.9f)
                                                                .clip(RoundedCornerShape(4.dp)),
                                                            contentScale = ContentScale.Fit,
                                                            filterQuality = FilterQuality.Low
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item (span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Info, null, tint = themeColor)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "矿脉出现波次不能填0。此模块仅用于配置可生长矿脉，配置战斗开始时已有的矿脉需要在预置障碍物模块里添加。",
                                fontSize = 12.sp,
                                color = themeColor,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // === 区域 2: 标题 (作为列表头，跨满全宽) ===
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "矿脉分布列表 (行优先排序)",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            // === 区域 3: 矿脉列表 (正常的 Grid Items) ===
            items(sortedItems) { item ->
                MineVeinCard(
                    item = item,
                    isSelected = (item.gridX == selectedX && item.gridY == selectedY),
                    isDark = isDark,
                    themeColor = themeColor,
                    onClick = {
                        selectedX = item.gridX
                        selectedY = item.gridY
                    },
                    onWaveChange = { wave -> updateWave(item, wave) },
                    onDelete = { itemToDelete = item }
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(300.dp))
            }
        }
    }
}

@Composable
private fun MineVeinCard(
    item: LunarMineVeinPlacementData,
    isSelected: Boolean,
    isDark: Boolean,
    themeColor: Color,
    onClick: () -> Unit,
    onWaveChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) if (isDark) Color(0xFF31383B) else Color(0xFFD7ECF1)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFF6CA4B4)) else null,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssetImage(
                    path = VEIN_ICON_PATH,
                    contentDescription = "矿脉",
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .size(28.dp),
                    filterQuality = FilterQuality.Medium,
                    placeholder = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF407A9A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "矿",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "矿脉",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColor,
                        maxLines = 1
                    )
                    Text(
                        text = "R${item.gridY + 1}:C${item.gridX + 1}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "移除矿脉",
                        tint = MaterialTheme.colorScheme.onError.copy(0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            NumberInputInt(
                value = item.emergenceWave,
                onValueChange = onWaveChange,
                label = "出现波次",
                color = themeColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
