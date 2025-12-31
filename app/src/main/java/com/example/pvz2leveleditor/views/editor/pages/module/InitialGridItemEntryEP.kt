package com.example.pvz2leveleditor.views.editor.pages.module

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pvz2leveleditor.data.InitialGridItemData
import com.example.pvz2leveleditor.data.InitialGridItemEntryData
import com.example.pvz2leveleditor.data.PvzLevelFile
import com.example.pvz2leveleditor.data.RtidParser
import com.example.pvz2leveleditor.views.editor.EditorHelpDialog
import com.example.pvz2leveleditor.views.editor.HelpSection
import com.example.pvz2leveleditor.views.screens.GridItemRepository
import com.google.gson.Gson

private val gson = Gson()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialGridItemEntryEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    onRequestGridItemSelection: ((String) -> Unit) -> Unit,
    scrollState: ScrollState
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    // 1. 数据状态初始化
    val moduleDataState = remember {
        val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        val data = try {
            gson.fromJson(obj?.objData, InitialGridItemEntryData::class.java)
        } catch (_: Exception) {
            InitialGridItemEntryData()
        }
        mutableStateOf(data)
    }

    // 2. UI 交互状态
    var selectedX by remember { mutableIntStateOf(0) }
    var selectedY by remember { mutableIntStateOf(0) }

    fun sync() {
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }?.let {
            it.objData = gson.toJsonTree(moduleDataState.value)
        }
    }

    moduleDataState.value.placements.filter {
        it.gridX == selectedX && it.gridY == selectedY
    }

    val sortedItems = remember(moduleDataState.value.placements) {
        moduleDataState.value.placements.sortedWith(compareBy({ it.gridX }, { it.gridY }))
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
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
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
            TopAppBar(
                title = { Text("场地物品布局") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "帮助说明", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF795548), // 褐色主题
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "场地物品模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = Color(0xFF795548)
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "用于在关卡开始时在场地上预置墓碑等障碍物或其他事件物品。"
                )
                HelpSection(
                    title = "格点坐标",
                    body = "障碍物所在的位置用网格坐标显示，可以在同一个位置堆放多个障碍物。"
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
                .background(Color(0xFFF5F5F5))
        ) {
            // === 区域 1: 网格选择器 (作为列表头，跨满全宽) ===
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.widthIn(max = 480.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("选中位置", fontSize = 12.sp, color = Color.Gray)
                                    Text(
                                        "R${selectedY + 1} : C${selectedX + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = Color(0xFF795548)
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                Button(
                                    onClick = { handleSelectItem() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF795548))
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
                                    .background(Color(0xFFD7CCC8))
                                    .border(1.dp, Color(0xFFA1887F), RoundedCornerShape(6.dp))
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    for (row in 0..4) {
                                        Row(Modifier.weight(1f)) {
                                            for (col in 0..8) {
                                                val isSelected = (row == selectedY && col == selectedX)
                                                val cellItems =
                                                    moduleDataState.value.placements.filter { it.gridX == col && it.gridY == row }
                                                val count = cellItems.size

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .border(0.5.dp, Color(0xFF8D6E63))
                                                        .background(
                                                            if (isSelected) Color(0xFFFFEB3B).copy(
                                                                alpha = 0.5f
                                                            ) else Color.Transparent
                                                        )
                                                        .clickable {
                                                            selectedX = col
                                                            selectedY = row
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (count > 0) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize(0.9f)
                                                                .background(
                                                                    Color(0xFF5D4037),
                                                                    RoundedCornerShape(4.dp)
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = if (count > 1) "+$count" else cellItems[0].typeName.take(
                                                                    1
                                                                ).uppercase(),
                                                                color = Color.White,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold
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

            // === 区域 2: 标题 (作为列表头，跨满全宽) ===
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "物品分布列表 (列优先排序)",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
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
fun GridItemCard(
    item: InitialGridItemData,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFFF9C4) else Color.White
        ),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFFFBC02D)) else null,
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "R${item.gridY + 1}:C${item.gridX + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF5D4037)
                )
                Spacer(Modifier.weight(1f))
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
            Text(
                text = GridItemRepository.getName(item.typeName),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1
            )
            Text(
                text = item.typeName,
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1
            )
        }
    }
}