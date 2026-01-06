package com.example.z_editor.views.editor.pages.event

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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.PotionLocationData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.ZombiePotionActionPropsData
import com.example.z_editor.data.ZombiePotionData
import com.example.z_editor.data.repository.GridItemRepository
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.google.gson.Gson

private val gson = Gson()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZombiePotionActionPropsEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    onRequestGridItemSelection: ((String) -> Unit) -> Unit
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val themeColor = Color(0xFF607D8B)

    val eventDataState = remember {
        val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        val data = try {
            gson.fromJson(obj?.objData, ZombiePotionActionPropsData::class.java)
        } catch (_: Exception) {
            ZombiePotionActionPropsData()
        }
        mutableStateOf(data)
    }

    var selectedX by remember { mutableIntStateOf(0) }
    var selectedY by remember { mutableIntStateOf(0) }

    fun sync() {
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }?.let {
            it.objData = gson.toJsonTree(eventDataState.value)
        }
    }

    val sortedItems = remember(eventDataState.value.potions) {
        eventDataState.value.potions.sortedWith(compareBy({ it.location.y }, { it.location.x }))
    }

    fun handleSelectItem() {
        onRequestGridItemSelection { typeName ->
            val newList = eventDataState.value.potions.toMutableList()
            val newItem = ZombiePotionData(
                location = PotionLocationData(x = selectedX, y = selectedY),
                type = typeName
            )
            newList.add(newItem)
            eventDataState.value = eventDataState.value.copy(potions = newList)
            sync()
        }
    }

    fun deleteItem(targetItem: ZombiePotionData) {
        val newList = eventDataState.value.potions.toMutableList()
        newList.remove(targetItem)
        eventDataState.value = eventDataState.value.copy(potions = newList)
        sync()
    }

    var itemToDelete by remember { mutableStateOf<ZombiePotionData?>(null) }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("移除药水") },
            text = {
                Text(
                    "确定要移除 R${itemToDelete!!.location.y + 1}:C${itemToDelete!!.location.x + 1} 处的 ${
                        GridItemRepository.getName(itemToDelete!!.type)
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
            TopAppBar(title = {
                Column {
                    Text("编辑 $currentAlias", fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "事件类型：药水投放",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            },
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
                    containerColor = themeColor,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "投放药水事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "此事件可以在场地上强行生成药水，能无视植物，可以作为障碍物生成事件的替代。"
                )
                HelpSection(
                    title = "生成方式",
                    body = "与障碍物生成的预选池不同，这个事件能精准地在固定格点强行生成障碍物并挤走植物。"
                )
                HelpSection(
                    title = "自定义相关",
                    body = "此事件内使用硬编码的障碍物类型名，只能对少部分预留了接口的障碍物进行自定义。"
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
                                        color = themeColor
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                Button(
                                    onClick = { handleSelectItem() },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
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
                                    .background(Color(0xFFE8E7F6))
                                    .border(1.dp, Color(0xFF9DA0DB), RoundedCornerShape(6.dp))
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    for (row in 0..4) {
                                        Row(Modifier.weight(1f)) {
                                            for (col in 0..8) {
                                                val isSelected = (row == selectedY && col == selectedX)
                                                val cellItems = eventDataState.value.potions.filter {
                                                    it.location.x == col && it.location.y == row
                                                }
                                                val count = cellItems.size
                                                val firstItem = cellItems.firstOrNull()

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .border(0.5.dp, Color(0xFF9DA0DB))
                                                        .background(
                                                            if (isSelected) Color(0xFFEBF13E).copy(alpha = 0.5f)
                                                            else Color.Transparent
                                                        )
                                                        .clickable {
                                                            selectedX = col
                                                            selectedY = row
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (count > 0 && firstItem != null) {
                                                        PotionIconSmall(firstItem.type)
                                                        if (count > 1) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .background(
                                                                        color = Color.Gray,
                                                                        shape = RoundedCornerShape(bottomStart = 4.dp)
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

            // === 区域 2: 标题 ===
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "物品分布列表 (行优先排序)",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            // === 区域 3: 物品列表 ===
            items(sortedItems) { item ->
                PotionItemCard(
                    item = item,
                    isSelected = (item.location.x == selectedX && item.location.y == selectedY),
                    onClick = {
                        selectedX = item.location.x
                        selectedY = item.location.y
                    },
                    onDelete = { itemToDelete = item }
                )
            }
        }
    }
}

@Composable
fun PotionIconSmall(typeName: String) {
    val iconPath = remember(typeName) { GridItemRepository.getIconPath(typeName) }
    val cardShape = RoundedCornerShape(3.dp)

    if (iconPath != null) {
        AssetImage(
            path = iconPath,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.9f).clip(cardShape),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.Low
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(0.8f).background(Color(0xFF3A47B7), cardShape),
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
fun PotionItemCard(
    item: ZombiePotionData,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE3E4F3) else Color.White
        ),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFF3A48B9)) else null,
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = GridItemRepository.getName(item.type),
                    fontSize = 14.sp,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                AssetImage(
                    path = GridItemRepository.getIconPath(item.type),
                    contentDescription = item.type,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).size(36.dp),
                    filterQuality = FilterQuality.Medium,
                    placeholder = {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0xFFEFEFF8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.type.take(1),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3A47B7)
                            )
                        }
                    }
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "R${item.location.y + 1}:C${item.location.x + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF3A47B7)
                )
            }
        }
    }
}