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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.InitialGridItemData
import com.example.z_editor.data.InitialGridItemEntryData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.repository.GridItemRepository
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzBlueDark
import com.example.z_editor.ui.theme.PvzBlueLight
import com.example.z_editor.ui.theme.PvzGridHighLight
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import rememberJsonSync


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialGridItemEntryEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    onRequestGridItemSelection: ((String) -> Unit) -> Unit
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, InitialGridItemEntryData::class.java)
    val moduleDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    var selectedX by remember { mutableIntStateOf(0) }
    var selectedY by remember { mutableIntStateOf(0) }


    val sortedItems = remember(moduleDataState.value.placements) {
        moduleDataState.value.placements.sortedWith(compareBy({ it.gridY }, { it.gridX }))
    }

    fun handleSelectItem() {
        onRequestGridItemSelection { typeName ->
            val newList = moduleDataState.value.placements.toMutableList()
            val newItem = InitialGridItemData(
                gridX = selectedX,
                gridY = selectedY,
                typeName = typeName
            )
            newList.add(newItem)
            moduleDataState.value = moduleDataState.value.copy(placements = newList)
            sync()
        }
    }

    fun deleteItem(targetItem: InitialGridItemData) {
        val newList = moduleDataState.value.placements.toMutableList()
        newList.remove(targetItem)
        moduleDataState.value = moduleDataState.value.copy(placements = newList)
        sync()
    }

    // 障碍物详情弹窗 (删除确认)
    var itemToDelete by remember { mutableStateOf<InitialGridItemData?>(null) }

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzBlueDark else PvzBlueLight

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("移除物品") },
            text = {
                Text(
                    "确定要移除 R${itemToDelete!!.gridY + 1}:C${itemToDelete!!.gridX + 1} 处的 ${
                        GridItemRepository.getName(
                            itemToDelete!!.typeName
                        )
                    } 吗？"
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
        topBar = {
            CommonEditorTopAppBar(
                title = "场地物品布局",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "场地物品模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "用于在关卡开始时在场地上预置墓碑等障碍物或其他事件物品。"
                )
                HelpSection(
                    title = "格点坐标",
                    body = "障碍物所在的位置用网格坐标显示，可以在同一个位置堆放多个障碍物。"
                )
                HelpSection(
                    title = "莲叶生成",
                    body = "巨浪沙滩地图的初始莲叶一般是靠初始障碍物设置模块生成的。"
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
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
                                    onClick = { handleSelectItem() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = themeColor
                                    )
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("添加物品")
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
                                                val cellItems =
                                                    moduleDataState.value.placements.filter { it.gridX == col && it.gridY == row }
                                                val count = cellItems.size
                                                val firstItem = cellItems.firstOrNull()

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
                                                    if (count > 0 && firstItem != null) {
                                                        GridItemIconSmall(firstItem.typeName)
                                                        if (count > 1) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .background(
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        shape = RoundedCornerShape(
                                                                            bottomStart = 4.dp
                                                                        )
                                                                    )
                                                                    .padding(horizontal = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = "+$count",
                                                                    color = Color.White,
                                                                    fontSize = 8.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    textAlign = TextAlign.Center
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
                }
            }

            // === 区域 2: 标题 (作为列表头，跨满全宽) ===
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "物品分布列表 (行优先排序)",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            // === 区域 3: 物品列表 (正常的 Grid Items) ===
            items(sortedItems) { item ->
                GridItemCard(
                    item = item,
                    isSelected = (item.gridX == selectedX && item.gridY == selectedY),
                    onClick = {
                        selectedX = item.gridX
                        selectedY = item.gridY
                    },
                    onDelete = { itemToDelete = item }
                )
            }
        }
    }
}

@Composable
fun GridItemIconSmall(typeName: String) {
    val iconPath = remember(typeName) { GridItemRepository.getIconPath(typeName) }

    val cardShape = RoundedCornerShape(4.dp)

    if (iconPath != null) {
        AssetImage(
            path = iconPath,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(0.9f)
                .clip(cardShape),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.Low
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize(0.8f)
                .background(Color(0xFF407A9A), cardShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = typeName.take(1).uppercase(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GridItemCard(
    item: InitialGridItemData,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = LocalDarkTheme.current
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
                Text(
                    text = GridItemRepository.getName(item.typeName),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AssetImage(
                    path = GridItemRepository.getIconPath(item.typeName),
                    contentDescription = item.typeName,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .size(36.dp),
                    filterQuality = FilterQuality.Medium,
                    placeholder = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF5EEE8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.typeName.take(1),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF407A9A)
                            )
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "R${item.gridY + 1}:C${item.gridX + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF407A9A)
                )
            }
        }
    }
}