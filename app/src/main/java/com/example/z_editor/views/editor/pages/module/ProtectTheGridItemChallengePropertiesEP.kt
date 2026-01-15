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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.ProtectGridItemData
import com.example.z_editor.data.ProtectTheGridItemChallengePropertiesData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.repository.GridItemRepository
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectTheGridItemChallengePropertiesEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    onRequestGridItemSelection: ((String) -> Unit) -> Unit
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val obj = remember(rootLevelFile) {
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    }
    val syncManager = rememberJsonSync(obj, ProtectTheGridItemChallengePropertiesData::class.java)
    var moduleData by syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    LaunchedEffect(moduleData.gridItems.size) {
        if (moduleData.mustProtectCount != moduleData.gridItems.size) {
            moduleData = moduleData.copy(mustProtectCount = moduleData.gridItems.size)
            sync()
        }
    }

    var selectedX by remember { mutableIntStateOf(0) }
    var selectedY by remember { mutableIntStateOf(0) }

    val sortedItems = remember(moduleData.gridItems) {
        moduleData.gridItems.sortedWith(compareBy({ it.gridY }, { it.gridX }))
    }

    fun handleAddItem() {
        onRequestGridItemSelection { typeName ->
            val newList = moduleData.gridItems.toMutableList()
            newList.removeAll { it.gridX == selectedX && it.gridY == selectedY }

            val newItem = ProtectGridItemData(
                gridX = selectedX,
                gridY = selectedY,
                gridItemType = typeName
            )
            newList.add(newItem)
            moduleData = moduleData.copy(gridItems = newList)
            sync()
        }
    }

    fun deleteItem(targetItem: ProtectGridItemData) {
        val newList = moduleData.gridItems.toMutableList()
        newList.remove(targetItem)
        moduleData = moduleData.copy(gridItems = newList)
        sync()
    }

    var itemToDelete by remember { mutableStateOf<ProtectGridItemData?>(null) }
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("移除保护目标") },
            text = {
                Text("确定要移除 R${itemToDelete!!.gridY + 1}:C${itemToDelete!!.gridX + 1} 处的 ${GridItemRepository.getName(itemToDelete!!.gridItemType)} 吗？")
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
                title = { Text("保护物品挑战", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
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
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "保护物品挑战说明",
                onDismiss = { showHelpDialog = false },
                themeColor = Color(0xFF1976D2)
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "定义关卡中必须保护的物品。如果这些物品被破坏会导致关卡失败。"
                )
                HelpSection(
                    title = "自动计数",
                    body = "软件会自动跟随您添加的物品数量更新需要保护的障碍物数量。"
                )
                HelpSection(
                    title = "操作指引",
                    body = "在上方网格中点击选择坐标，然后点击‘添加目标’按钮选择要保护的物品类型。"
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
            // === 区域 1: 描述输入框 ===
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "挑战描述 (Description)",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = moduleData.description,
                            onValueChange = {
                                moduleData = moduleData.copy(description = it)
                                sync()
                            },
                            label = {},
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "当前保护目标数量: ${moduleData.mustProtectCount}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // === 区域 2: 网格选择器 (跨满) ===
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
                                    Text("目标位置", fontSize = 12.sp, color = Color.Gray)
                                    Text(
                                        "R${selectedY + 1} : C${selectedX + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = Color(0xFF1976D2)
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                Button(
                                    onClick = { handleAddItem() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("添加目标")
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.8f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE3F2FD))
                                    .border(1.dp, Color(0xFF90CAF9), RoundedCornerShape(6.dp))
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    for (row in 0..4) {
                                        Row(Modifier.weight(1f)) {
                                            for (col in 0..8) {
                                                val isSelected = (row == selectedY && col == selectedX)
                                                val cellItem = moduleData.gridItems.find { it.gridX == col && it.gridY == row }

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .border(0.5.dp, Color(0xFFBBDEFB))
                                                        .background(
                                                            if (isSelected) Color(0xFFFCF2B1)
                                                            else Color.Transparent
                                                        )
                                                        .clickable {
                                                            selectedX = col
                                                            selectedY = row
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (cellItem != null) {
                                                        ProtectItemIconSmall(cellItem.gridItemType)
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

            // === 区域 3: 标题 (跨满) ===
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "保护目标列表 (行优先排序)",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            // === 区域 4: 列表展示 ===
            items(sortedItems) { item ->
                ProtectItemCard(
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

// === 辅助组件 ===

@Composable
fun ProtectItemIconSmall(typeName: String) {
    val iconPath = remember(typeName) { GridItemRepository.getIconPath(typeName) }

    if (iconPath != null) {
        AssetImage(
            path = iconPath,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.9f),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.Low
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize(0.8f)
                .background(Color(0xFF1976D2), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                typeName.take(1).uppercase(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProtectItemCard(
    item: ProtectGridItemData,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
        ),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFF1976D2)) else null,
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = GridItemRepository.getName(item.gridItemType),
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
                    path = GridItemRepository.getIconPath(item.gridItemType),
                    contentDescription = item.gridItemType,
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
                                text = item.gridItemType.take(1),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2)
                            )
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "R${item.gridY + 1}:C${item.gridX + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1976D2)
                )
            }
        }
    }
}