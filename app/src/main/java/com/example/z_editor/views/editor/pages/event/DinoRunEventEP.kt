package com.example.z_editor.views.editor.pages.event

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.DinoRunActionPropsData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.ui.theme.LocalDarkTheme
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
fun DinoRunEventEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: LazyListState
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, DinoRunActionPropsData::class.java)
    val actionDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    val isDark = LocalDarkTheme.current
    // 主题色随恐龙事件用浅绿色
    val themeColor = if (isDark) PvzLightGreenDark else PvzLightGreenLight

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        // 底部内边距统一走 contentWindowInsets：键盘弹出时自动在底部留出空间，避免遮挡输入框
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        topBar = {
            CommonEditorTopAppBar(
                title = "编辑 $currentAlias",
                subtitle = "事件类型：龙潮",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "龙潮事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "此事件用于在波次中让恐龙沿指定行奔跑冲击场地（龙潮），被波及的格子会显示危险警告。"
                )
                HelpSection(
                    title = "参数说明",
                    body = "DinoRow 为龙潮出现的行（自上往下 0-4）；TimeInterval 为相邻龙潮的间隔时间；WaveStartMessage 为波次开始时的警告文本。"
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
                            value = actionDataState.value.dinoRow,
                            onValueChange = { newVal ->
                                actionDataState.value =
                                    actionDataState.value.copy(dinoRow = newVal.coerceIn(0, 4))
                                sync()
                            },
                            color = themeColor,
                            label = "龙潮所在行 (DinoRow)",
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
                            label = "间隔时间 (TimeInterval)",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // === 区域 2: 波次开始提示 ===
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
