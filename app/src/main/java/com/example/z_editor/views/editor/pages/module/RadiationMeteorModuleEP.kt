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
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RadiationMeteorModulePropertiesData
import com.example.z_editor.data.RadiationMeteorSpawnData
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

private const val METEOR_ICON_PATH = "images/griditems/radiation_meteor.webp"
private const val METEOR_COLOR = 0xFFF57F17

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadiationMeteorModuleEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, RadiationMeteorModulePropertiesData::class.java)
    val moduleDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    var selectedX by remember { mutableIntStateOf(0) }
    var selectedY by remember { mutableIntStateOf(0) }

    val sortedItems = remember(moduleDataState.value.spawnSchedule) {
        moduleDataState.value.spawnSchedule.sortedWith(
            compareBy(
                { it.wave },
                { it.gridY },
                { it.gridX })
        )
    }

    fun addAtSelected() {
        val newList = moduleDataState.value.spawnSchedule.toMutableList()
        newList.add(
            RadiationMeteorSpawnData(
                wave = 1,
                gridX = selectedX,
                gridY = selectedY
            )
        )
        moduleDataState.value = moduleDataState.value.copy(spawnSchedule = newList)
        sync()
    }

    // 允许重合时多条记录可能结构相等，必须按引用删除
    fun deleteItem(targetItem: RadiationMeteorSpawnData) {
        val newList = moduleDataState.value.spawnSchedule.toMutableList()
        val index = newList.indexOfFirst { it === targetItem }
        if (index != -1) {
            newList.removeAt(index)
            moduleDataState.value = moduleDataState.value.copy(spawnSchedule = newList)
            sync()
        }
    }

    fun updateWave(targetItem: RadiationMeteorSpawnData, wave: Int) {
        val newList = moduleDataState.value.spawnSchedule.toMutableList()
        val index = newList.indexOfFirst { it === targetItem }
        if (index != -1) {
            newList[index] = targetItem.copy(wave = wave)
            moduleDataState.value = moduleDataState.value.copy(spawnSchedule = newList)
            sync()
        }
    }

    // 陨石详情弹窗 (删除确认)
    var itemToDelete by remember { mutableStateOf<RadiationMeteorSpawnData?>(null) }

    val isDark = LocalDarkTheme.current
    val themeColor = PvzBluePrimary

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("移除陨石", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "确定要移除 R${itemToDelete!!.gridY + 1}:C${itemToDelete!!.gridX + 1} 处第 ${itemToDelete!!.wave} 波降落的陨石吗？"
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
                title = "放射性陨石布局",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "放射性陨石模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "用于在月球基地地图上从天而降放射性陨石，落地后需要玩家开采。"
                )
                HelpSection(
                    title = "陨石参数",
                    body = "警告时间为陨石落下前的预警时长；污染间隔为落地后产生污染物的间隔；开采时间为采集所需时长；能量奖励为开采完成后获得的电力。"
                )
                HelpSection(
                    title = "格点坐标",
                    body = "陨石位置用网格坐标显示，每个格子允许多颗陨石在不同波次降落。降落波次指开局后经过多少波出现。"
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
                                    Text("添加陨石")
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
                                                val count =
                                                    moduleDataState.value.spawnSchedule.count {
                                                        it.gridX == col && it.gridY == row
                                                    }

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
                                                    if (count > 0) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            AssetImage(
                                                                path = METEOR_ICON_PATH,
                                                                contentDescription = null,
                                                                modifier = Modifier
                                                                    .fillMaxSize(0.9f)
                                                                    .clip(RoundedCornerShape(4.dp)),
                                                                contentScale = ContentScale.Fit,
                                                                filterQuality = FilterQuality.Low,
                                                                placeholder = {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(26.dp)
                                                                            .clip(
                                                                                RoundedCornerShape(
                                                                                    13.dp
                                                                                )
                                                                            )
                                                                            .background(
                                                                                Color(
                                                                                    METEOR_COLOR
                                                                                )
                                                                            ),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Text(
                                                                            text = "陨",
                                                                            fontSize = 11.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = Color.White
                                                                        )
                                                                    }
                                                                }
                                                            )
                                                            if (count > 1) {
                                                                // 同格多颗陨石时叠加数量角标
                                                                Text(
                                                                    text = "×$count",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White,
                                                                    modifier = Modifier
                                                                        .align(Alignment.BottomEnd)
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .background(Color(0xCC000000))
                                                                        .padding(
                                                                            horizontal = 3.dp,
                                                                            vertical = 1.dp
                                                                        )
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

            // === 区域 2: 陨石参数 (跨满全宽) ===
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "陨石参数",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = themeColor
                        )
                        NumberInputInt(
                            value = moduleDataState.value.warningDuration,
                            onValueChange = {
                                moduleDataState.value =
                                    moduleDataState.value.copy(warningDuration = it); sync()
                            },
                            label = "警告时间 (秒)",
                            color = themeColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                        NumberInputInt(
                            value = moduleDataState.value.pollutionInterval,
                            onValueChange = {
                                moduleDataState.value =
                                    moduleDataState.value.copy(pollutionInterval = it); sync()
                            },
                            label = "污染间隔 (秒)",
                            color = themeColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                        NumberInputInt(
                            value = moduleDataState.value.miningDurationRequired,
                            onValueChange = {
                                moduleDataState.value =
                                    moduleDataState.value.copy(miningDurationRequired = it); sync()
                            },
                            label = "开采时间 (秒)",
                            color = themeColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                        NumberInputInt(
                            value = moduleDataState.value.powerRewardOnDestroy,
                            onValueChange = {
                                moduleDataState.value =
                                    moduleDataState.value.copy(powerRewardOnDestroy = it); sync()
                            },
                            label = "能量奖励",
                            color = themeColor,
                            modifier = Modifier.fillMaxWidth()
                        )
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
                                text = "陨石的降落波次参数表示距开始多少波，例如填0则会出现在关卡第一波。",
                                fontSize = 12.sp,
                                color = themeColor,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // === 区域 3: 标题 (作为列表头，跨满全宽) ===
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "陨石降落列表 (按波次排序)",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            // === 区域 4: 陨石列表 (正常的 Grid Items) ===
            items(sortedItems) { item ->
                MeteorCard(
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

        }
    }
}

@Composable
private fun MeteorCard(
    item: RadiationMeteorSpawnData,
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
                    path = METEOR_ICON_PATH,
                    contentDescription = "陨石",
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .size(28.dp),
                    filterQuality = FilterQuality.Medium,
                    placeholder = {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(METEOR_COLOR)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "陨",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "放射性陨石",
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
                        contentDescription = "移除陨石",
                        tint = MaterialTheme.colorScheme.onError.copy(0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            NumberInputInt(
                value = item.wave,
                onValueChange = onWaveChange,
                label = "降落波次",
                color = themeColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
