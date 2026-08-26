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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.z_editor.data.DinoTreadActionPropsData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzGridBgDark
import com.example.z_editor.ui.theme.PvzGridBorder
import com.example.z_editor.ui.theme.PvzLightGreenDark
import com.example.z_editor.ui.theme.PvzLightGreenLight
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

// ======================== 编辑器界面 ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DinoTreadActionPropsEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: LazyListState
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, DinoTreadActionPropsData::class.java)
    val actionDataState = syncManager.dataState

    fun sync() {
        val v = actionDataState.value
        var updated = v
        // GridXMin / GridXMax 都是落点中心列，强制保持一致（以 GridXMax 为准）
        if (v.gridXMin != v.gridXMax) {
            updated = updated.copy(
                gridXMin = v.gridXMax.coerceIn(0, 8),
                gridXMax = v.gridXMax.coerceIn(0, 8)
            )
        }
        if (updated != v) {
            actionDataState.value = updated
        }
        syncManager.sync()
    }

    // 点击格子设置落点中心：行写入 GridY，列同时写入 GridXMin / GridXMax
    fun setLanding(col: Int, row: Int) {
        actionDataState.value =
            actionDataState.value.copy(gridY = row, gridXMin = col, gridXMax = col)
        sync()
    }

    val isDark = LocalDarkTheme.current
    // 主题色随恐龙事件用浅绿色
    val themeColor = if (isDark) PvzLightGreenDark else PvzLightGreenLight

    // 落点中心
    val centerRow = actionDataState.value.gridY.coerceIn(0, 4)
    val centerCol = actionDataState.value.gridXMin.coerceIn(0, 8)

    val isDangerCell: (Int, Int) -> Boolean = { row, col ->
        col in (centerCol - 1)..(centerCol + 1) && row in (centerRow - 1)..(centerRow + 1)
    }

    val dangerFill = MaterialTheme.colorScheme.onError.copy(alpha = if (isDark) 0.45f else 0.32f)
    val centerFill = MaterialTheme.colorScheme.onError.copy(alpha = if (isDark) 0.7f else 0.6f)

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        // 底部内边距统一走 contentWindowInsets：键盘弹出时自动在底部留出空间，避免遮挡输入框
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        topBar = {
            CommonEditorTopAppBar(
                title = "编辑 $currentAlias",
                subtitle = "事件类型：雷龙踩踏",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "雷龙踩踏事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "此事件用于在波次中让雷龙踩踏场地，被标记的危险区格子会在踩踏前显示落点警告。"
                )
                HelpSection(
                    title = "落点中心",
                    body = "GridY 为落点中心行，GridXMin / GridXMax 为落点中心列。危险区覆盖落点及其周边 8 格。"
                )
                HelpSection(
                    title = "参数说明",
                    body = "TimeInterval 为踩踏的时间间隔；WaveStartMessage 为波次开始时的警告文本。"
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
            // === 区域 1: 落点网格 ===
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "落点预览",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = themeColor
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "点击格子设置落点中心，红色区域为落点及其周边 8 格组成的危险区",
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
                                .background(if (isDark) PvzGridBgDark else Color.White)
                                .border(1.dp, PvzGridBorder, RoundedCornerShape(6.dp))
                        ) {
                            Column(Modifier.fillMaxSize()) {
                                for (row in 0..4) {
                                    Row(Modifier.weight(1f)) {
                                        for (col in 0..8) {
                                            val isCenter = row == centerRow && col == centerCol
                                            val isDanger = isDangerCell(row, col)

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .border(0.5.dp, PvzGridBorder)
                                                    .background(
                                                        when {
                                                            isCenter -> centerFill
                                                            isDanger -> dangerFill
                                                            else -> Color.Transparent
                                                        }
                                                    )
                                                    .clickable { setLanding(col, row) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isCenter) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(MaterialTheme.colorScheme.onError)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // 图例说明
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(20.dp)
                                    .background(centerFill)
                                    .border(0.5.dp, PvzGridBorder)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "落点中心",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(24.dp))
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(20.dp)
                                    .background(dangerFill)
                                    .border(0.5.dp, PvzGridBorder)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "危险区 (落点+周边8格)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            value = actionDataState.value.gridY,
                            onValueChange = { newVal ->
                                actionDataState.value =
                                    actionDataState.value.copy(gridY = newVal.coerceIn(0, 4))
                                sync()
                            },
                            color = themeColor,
                            label = "落点中心行 (GridY)",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        NumberInputInt(
                            value = actionDataState.value.gridXMin,
                            onValueChange = { newVal ->
                                val c = newVal.coerceIn(0, 8)
                                actionDataState.value =
                                    actionDataState.value.copy(gridXMin = c, gridXMax = c)
                                sync()
                            },
                            color = themeColor,
                            label = "落点中心列 (GridXMin/Max)",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        NumberInputInt(
                            value = actionDataState.value.timeInterval,
                            onValueChange = { newVal ->
                                actionDataState.value =
                                    actionDataState.value.copy(timeInterval = newVal)
                                sync()
                            },
                            color = themeColor,
                            label = "踩踏间隔 (TimeInterval)",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // === 区域 3: 波次开始提示 ===
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "波次开始提示",
                            color = themeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = actionDataState.value.waveStartMessage,
                            onValueChange = { newVal ->
                                actionDataState.value =
                                    actionDataState.value.copy(waveStartMessage = newVal)
                                sync()
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                cursorColor = themeColor,
                                selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
                                    handleColor = themeColor,
                                    backgroundColor = themeColor.copy(alpha = 0.4f)
                                ),
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedBorderColor = themeColor,
                                focusedLabelColor = themeColor
                            ),
                            label = { Text("警告文本 (WaveStartMessage)", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}
