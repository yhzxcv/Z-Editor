package com.example.z_editor.views.editor.pages.event

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.GravestonePoolItem
import com.example.z_editor.data.LocationData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.SpawnGraveStonesData
import com.example.z_editor.data.repository.GridItemRepository
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpawnGraveStonesEventEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    onRequestGridItemSelection: ((String) -> Unit) -> Unit,
    scrollState: LazyListState
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val themeColor = Color(0xFF607D8B)

    val internalObjectAliases = remember(rootLevelFile.objects.size, rootLevelFile.hashCode()) {
        rootLevelFile.objects.flatMap { it.aliases ?: emptyList() }.toSet()
    }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, SpawnGraveStonesData::class.java)
    val actionDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    fun togglePosition(col: Int, row: Int) {
        val currentPool = actionDataState.value.spawnPositionsPool.toMutableList()
        val existing = currentPool.find { it.x == col && it.y == row }

        if (existing != null) {
            currentPool.remove(existing)
        } else {
            currentPool.add(LocationData(x = col, y = row))
        }

        actionDataState.value = actionDataState.value.copy(spawnPositionsPool = currentPool)
        sync()
    }

    fun handleAddItem() {
        onRequestGridItemSelection { typeName ->
            val fullRtid = RtidParser.build(typeName, "GridItemTypes")
            val newList = actionDataState.value.gravestonePool.toMutableList()
            val existingIndex = newList.indexOfFirst { it.type == fullRtid }
            if (existingIndex != -1) {
                val item = newList[existingIndex]
                newList[existingIndex] = item.copy(count = item.count + 1)
            } else {
                newList.add(GravestonePoolItem(count = 1, type = fullRtid))
            }
            actionDataState.value = actionDataState.value.copy(gravestonePool = newList)
            sync()
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "编辑 $currentAlias",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("事件类型：墓碑生成", fontSize = 14.sp, fontWeight = FontWeight.Normal)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "帮助", tint = Color.White)
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
                title = "墓碑生成事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "此事件用于在波次进行中随机生成障碍物，例如黑暗时代的生成墓碑事件。"
                )
                HelpSection(
                    title = "生成逻辑",
                    body = "该事件从上面的格子中随机选取可使用的格子生成目标障碍物。障碍物数量总和不能超过上方位置池的坐标总数，否则多余的物品将无法生成。"
                )
                HelpSection(
                    title = "资源缺失",
                    body = "在部分缺少墓碑出土特效的地图可能会出现阳光贴图的情况，请谨慎使用此事件。"
                )
                HelpSection(
                    title = "自定义相关",
                    body = "通过此事件生成的障碍物由于包裹了 RTID 语句，可以用于自定义障碍物属性，软件暂时不支持此功能。"
                )
            }
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === 区域 1: 候选位置池 ===
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "候选位置池 (SpawnPositionsPool)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = themeColor
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "点击格子以选中/取消选中，选中的格子即为可能的生成点",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(16.dp))

                        // 9x5 网格显示
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
                                            val isSelected =
                                                actionDataState.value.spawnPositionsPool.any { it.x == col && it.y == row }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .border(0.5.dp, Color(0xFF8D6E63))
                                                    .background(
                                                        if (isSelected) Color(0xFF8BC34A).copy(alpha = 0.8f)
                                                        else Color.Transparent
                                                    )
                                                    .clickable { togglePosition(col, row) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 统计信息
                        val posCount = actionDataState.value.spawnPositionsPool.size
                        val itemCount = actionDataState.value.gravestonePool.sumOf { it.count }
                        Spacer(Modifier.height(8.dp))
                        Row {
                            Text(
                                "候选位置数: $posCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "待生成物品总数: $itemCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (itemCount > posCount) Color.Red else themeColor
                            )
                        }
                        if (itemCount > posCount) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "警告：物品总数超过了候选位置数，部分物品将无法生成！",
                                color = Color.Red,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE9EEF5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Info, null, tint = themeColor)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "此事件中墓碑这类和植物冲突的障碍物会因为植物阻挡而无法生成，强制生成需要采用其它方法。",
                                fontSize = 12.sp,
                                color = themeColor,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // === 区域 2: 物品池 ===
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "物品池 (GravestonePool)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = themeColor
                    )
                    Button(
                        onClick = { handleAddItem() },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("添加类型", fontSize = 13.sp)
                    }
                }
            }

            items(actionDataState.value.gravestonePool.size) { index ->
                val item = actionDataState.value.gravestonePool[index]

                val parsed = RtidParser.parse(item.type)
                val alias = parsed?.alias ?: item.type
                val source = parsed?.source

                val isValid = if (source == "CurrentLevel") {
                    internalObjectAliases.contains(alias)
                } else {
                    GridItemRepository.isValid(alias)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = if (!isValid) Color(0xFFF8F1F1) else Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                    border = if (!isValid) androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.Red
                    ) else null
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssetImage(
                            path = GridItemRepository.getIconPath(alias),
                            contentDescription = alias,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEEEEEE)),
                            filterQuality = FilterQuality.Medium,
                            placeholder = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Widgets, null, tint = Color.Gray)
                                }
                            }
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = GridItemRepository.getName(alias),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = alias,
                                fontSize = 10.sp,
                                color = if (!isValid) Color.Red else Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        NumberInputInt(
                            value = item.count,
                            onValueChange = { newVal ->
                                val newList = actionDataState.value.gravestonePool.toMutableList()
                                newList[index] = item.copy(count = newVal)
                                actionDataState.value =
                                    actionDataState.value.copy(gravestonePool = newList)
                                sync()
                            },
                            label = "数量",
                            color = themeColor,
                            modifier = Modifier.width(80.dp)
                        )

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                val newList = actionDataState.value.gravestonePool.toMutableList()
                                newList.removeAt(index)
                                actionDataState.value =
                                    actionDataState.value.copy(gravestonePool = newList)
                                sync()
                            }
                        ) {
                            Icon(Icons.Default.Delete, null, tint = Color.LightGray)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}