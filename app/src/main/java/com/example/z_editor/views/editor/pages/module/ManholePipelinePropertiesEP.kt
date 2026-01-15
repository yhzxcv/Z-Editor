package com.example.z_editor.views.editor.pages.module

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.LevelParser
import com.example.z_editor.data.ManholePipelineModuleData
import com.example.z_editor.data.PipelineData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManholePipelinePropertiesEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: ScrollState
) {
    val currentAlias = LevelParser.extractAlias(rtid)
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    var selectedIndex by remember { mutableIntStateOf(0) }
    var isEditingEnd by remember { mutableStateOf(false) }

    val themeColor = Color(0xFF607D8B)

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, ManholePipelineModuleData::class.java)
    val moduleDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    LaunchedEffect(Unit) {
        if (moduleDataState.value.pipelineList.isEmpty()) {
            val newList = mutableListOf(PipelineData(6, 2, 2, 2))
            moduleDataState.value = moduleDataState.value.copy(pipelineList = newList)
            sync()
        }
    }

    val currentPipeline = moduleDataState.value.pipelineList.getOrNull(selectedIndex)

    fun updateCurrentPipeline(newData: PipelineData) {
        if (selectedIndex in moduleDataState.value.pipelineList.indices) {
            val newList = moduleDataState.value.pipelineList.toMutableList()
            newList[selectedIndex] = newData
            moduleDataState.value = moduleDataState.value.copy(pipelineList = newList)
            sync()
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text("地下管道设置", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
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
                title = "地下管道模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "定义场景中的地下管道系统，常用于蒸汽时代地图。管道连接两点，僵尸可以通过管道进行移动。"
                )
                HelpSection(
                    title = "全局参数",
                    body = "传输耗时指僵尸经过每单位格子的传输时间。伤害量指有平顶菇时僵尸在管道内受到的的持续伤害。"
                )
                HelpSection(
                    title = "操作指南",
                    body = "在上方列表选择管道组，下方网格显示管道布局。点击“放置起点”或“放置终点”切换模式，然后点击网格设定位置。同色连线表示管道流向。"
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .background(Color(0xFFF5F5F5)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // === 区域 1: 管道组列表管理 ===
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(moduleDataState.value.pipelineList) { index, _ ->
                    FilterChip(
                        selected = index == selectedIndex,
                        onClick = { selectedIndex = index },
                        label = { Text("管道 ${index + 1}") },
                        leadingIcon = {
                            if (index == selectedIndex) Icon(
                                Icons.Default.Timeline,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (moduleDataState.value.pipelineList.size > 1) {
                                Icon(
                                    Icons.Default.Close,
                                    "删除",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            val newList =
                                                moduleDataState.value.pipelineList.toMutableList()
                                            newList.removeAt(index)
                                            moduleDataState.value =
                                                moduleDataState.value.copy(pipelineList = newList)
                                            if (selectedIndex >= newList.size) selectedIndex =
                                                (newList.size - 1).coerceAtLeast(0)
                                            sync()
                                        }
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeColor.copy(alpha = 0.2f),
                            selectedLabelColor = themeColor
                        )
                    )
                }
                item {
                    IconButton(
                        onClick = {
                            val newList = moduleDataState.value.pipelineList.toMutableList()
                            newList.add(PipelineData(6, 2, 2, 2))
                            moduleDataState.value =
                                moduleDataState.value.copy(pipelineList = newList)
                            selectedIndex = newList.lastIndex
                            sync()
                        }
                    ) {
                        Icon(Icons.Default.Add, "添加组", tint = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // === 区域 2: 全局参数 ===
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "全局参数",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = themeColor
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumberInputInt(
                            value = moduleDataState.value.operationTimePerGrid,
                            onValueChange = {
                                moduleDataState.value =
                                    moduleDataState.value.copy(operationTimePerGrid = it)
                                sync()
                            },
                            label = "传输耗时 (秒/格)",
                            modifier = Modifier.weight(1f),
                            color = themeColor
                        )
                        NumberInputInt(
                            value = moduleDataState.value.damagePerSecond,
                            onValueChange = {
                                moduleDataState.value =
                                    moduleDataState.value.copy(damagePerSecond = it)
                                sync()
                            },
                            label = "每秒伤害",
                            modifier = Modifier.weight(1f),
                            color = themeColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // === 区域 3: 当前管道配置 ===
            if (currentPipeline != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "管道 ${selectedIndex + 1} 布局",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = themeColor
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "起点: (${currentPipeline.startY + 1}, ${currentPipeline.startX + 1}) → 终点: (${currentPipeline.endY + 1}, ${currentPipeline.endX + 1})",
                                fontSize = 11.sp, color = Color.Gray
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFFEEEEEE))
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isEditingEnd) themeColor else Color.Transparent)
                                    .clickable { isEditingEnd = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Logout, null,
                                        tint = if (isEditingEnd) Color.White else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "放置终点 (出口)",
                                        color = if (isEditingEnd) Color.White else Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (!isEditingEnd) themeColor else Color.Transparent)
                                    .clickable { isEditingEnd = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Login, null,
                                        tint = if (!isEditingEnd) Color.White else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "放置起点 (入口)",
                                        color = if (!isEditingEnd) Color.White else Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.widthIn(max = 480.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
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
                                                val isTargetCell =
                                                    (!isEditingEnd && currentPipeline.startX == col && currentPipeline.startY == row)
                                                            || (isEditingEnd && currentPipeline.endX == col && currentPipeline.endY == row)
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .border(
                                                            width = if (isTargetCell) 1.5.dp else 0.5.dp,
                                                            color = if (isTargetCell) themeColor else Color(
                                                                0xFFA1887F
                                                            )
                                                        )
                                                        .background(
                                                            if (isTargetCell) Color(0xFFB0B0B0).copy(
                                                                alpha = 0.6f
                                                            ) else Color.Transparent
                                                        )
                                                        .clickable {
                                                            if (isEditingEnd) {
                                                                if (currentPipeline.startX != col || currentPipeline.startY != row) {
                                                                    updateCurrentPipeline(
                                                                        currentPipeline.copy(
                                                                            endX = col,
                                                                            endY = row
                                                                        )
                                                                    )
                                                                }
                                                            } else {
                                                                if (currentPipeline.endX != col || currentPipeline.endY != row) {
                                                                    updateCurrentPipeline(
                                                                        currentPipeline.copy(
                                                                            startX = col,
                                                                            startY = row
                                                                        )
                                                                    )
                                                                }
                                                            }
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(Modifier.fillMaxSize()) {
                                    for (row in 0..4) {
                                        Row(Modifier.weight(1f)) {
                                            for (col in 0..8) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val pointsAtCell =
                                                        moduleDataState.value.pipelineList.mapIndexedNotNull { index, pipe ->
                                                            if (pipe.startX == col && pipe.startY == row) Triple(
                                                                index,
                                                                true,
                                                                pipe
                                                            )
                                                            else if (pipe.endX == col && pipe.endY == row) Triple(
                                                                index,
                                                                false,
                                                                pipe
                                                            )
                                                            else null
                                                        }

                                                    val pointToShow =
                                                        pointsAtCell.find { it.first == selectedIndex }
                                                            ?: pointsAtCell.lastOrNull()

                                                    if (pointToShow != null) {
                                                        val (idx, isStart, _) = pointToShow
                                                        val isSelected = idx == selectedIndex

                                                        val imageName =
                                                            if (isStart) "steam_down.png" else "steam_up.png"

                                                        AssetImage(
                                                            path = "images/griditems/$imageName",
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .fillMaxSize(0.9f)
                                                                .clip(RoundedCornerShape(4.dp)),
                                                            filterQuality = FilterQuality.Medium
                                                        )

                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(2.dp)
                                                                .size(18.dp)
                                                                .background(
                                                                    color = if (isSelected) themeColor else Color.Gray.copy(
                                                                        alpha = 0.8f
                                                                    ),
                                                                    shape = CircleShape
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "${idx + 1}",
                                                                color = Color.White,
                                                                fontSize = 10.sp,
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
            } else {
                Box(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("数据异常，请重新添加管道")
                }
            }
        }
    }
}