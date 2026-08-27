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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.z_editor.data.EventRegistry
import com.example.z_editor.data.LocationData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RocketPoolItem
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.SpawnRocketLandingData
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzGrayDark
import com.example.z_editor.ui.theme.PvzGrayLight
import com.example.z_editor.ui.theme.PvzGridBorder
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

private const val ROCKET_TYPE = "RTID(rocket_landing@GridItemTypes)"

// ======================== 编辑器界面 ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpawnRocketLandingEventEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: LazyListState
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, SpawnRocketLandingData::class.java)
    val actionDataState = syncManager.dataState

    fun sync() {
        val v = actionDataState.value
        // RocketPool 固定写入：仅 rocket_landing，Count 跟随 SpawnCount
        val fixedPool = mutableListOf(RocketPoolItem(count = v.spawnCount, type = ROCKET_TYPE))
        if (v.rocketPool != fixedPool) {
            actionDataState.value = v.copy(rocketPool = fixedPool)
        }
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

    val isDark = LocalDarkTheme.current
    // 主题色跟随事件注册表的赤红配色，与事件列表页保持一致
    val eventMeta = remember { EventRegistry.getMetadata("SpawnRocketLandingWaveActionProps") }
    val themeColor = if (isDark) eventMeta?.darkColor ?: PvzGrayDark else eventMeta?.color ?: PvzGrayLight

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        // 底部内边距统一走 contentWindowInsets：键盘弹出时自动在底部留出空间，避免遮挡输入框
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        topBar = {
            CommonEditorTopAppBar(
                title = "编辑 $currentAlias",
                subtitle = "事件类型：火箭降落",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "火箭降落事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "此事件用于在波次进行中让火箭从空中降落，用于月球基地。"
                )
                HelpSection(
                    title = "生成逻辑",
                    body = "事件会从下方候选位置池中随机选取格子降落火箭，火箭总数不能超过候选位置数，否则多余的火箭将无法降落。"
                )
            }
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === 区域 1: 候选位置池 ===
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            "点击格子以选中/取消选中，选中的格子即为可能的降落点",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))

                        // 9x5 网格显示
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.8f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF3B332F) else Color(0xFFD7CCC8))
                                .border(1.dp, PvzGridBorder, RoundedCornerShape(6.dp))
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
                                                    .border(0.5.dp, PvzGridBorder)
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
                        val rocketCount = actionDataState.value.spawnCount
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
                                "火箭总数: $rocketCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (rocketCount > posCount) MaterialTheme.colorScheme.onError else themeColor
                            )
                        }
                        if (rocketCount > posCount) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "警告：火箭总数超过了候选位置数，部分火箭将无法降落！",
                                color = MaterialTheme.colorScheme.onError,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // === 区域 2: 基础参数 ===
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "基础参数",
                            color = themeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        NumberInputInt(
                            value = actionDataState.value.spawnCount,
                            onValueChange = { newVal ->
                                actionDataState.value =
                                    actionDataState.value.copy(spawnCount = newVal)
                                sync()
                            },
                            color = themeColor,
                            label = "火箭总数 (SpawnCount)",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "本波次降落的火箭总数，RocketPool 的数量会同步为该值",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        NumberInputInt(
                            value = actionDataState.value.spawnInterval,
                            onValueChange = { newVal ->
                                actionDataState.value =
                                    actionDataState.value.copy(spawnInterval = newVal)
                                sync()
                            },
                            color = themeColor,
                            label = "降落间隔 (SpawnInterval)",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "相邻火箭降落的时间间隔",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
