package com.example.z_editor.views.editor.pages.event

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.ParachuteRainEventData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzLightPurpleDark
import com.example.z_editor.ui.theme.PvzLightPurpleLight
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputDouble
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

private val PRESET_PARACHUTE_ZOMBIES = listOf(
    "lostcity_lostpilot" to "失落飞行员 (lostcity_lostpilot)",
    "zcorp_helpdesk" to "Z公司服务台 (zcorp_helpdesk)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParachuteRainEventEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: LazyListState
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, ParachuteRainEventData::class.java)
    val actionDataState = syncManager.dataState

    fun sync(newData: ParachuteRainEventData) {
        actionDataState.value = newData
        syncManager.sync()
    }

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzLightPurpleDark else PvzLightPurpleLight

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            CommonEditorTopAppBar(
                title = "编辑 $currentAlias",
                subtitle = "事件类型：空降突袭",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "降落伞空降事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "僵尸会从屏幕上方掉落突袭，通常用于失落之城的飞行员僵尸。僵尸的阶级随地图阶级序列。"
                )
                HelpSection(
                    title = "生成逻辑",
                    body = "事件触发后，僵尸会分批次从天而降。可以控制总数量和每批次之间的时间间隔。僵尸会随机降落在选择的列数。若到达了下落总前摇时间剩下的僵尸会立即出现。"
                )
                HelpSection(
                    title = "字幕信息",
                    body = "事件出现前会出现红色字幕提示，可以不设置僵尸让这个事件变为纯粹的提示显示。在此页面使用中文会显示乱码。"
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
            // === 区域 1: 僵尸类型配置 (带下拉列表) ===
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AirplanemodeActive, null, tint = themeColor)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "空降单位配置",
                                color = themeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        var expanded by remember { mutableStateOf(false) }
                        val currentName = actionDataState.value.spiderZombieName

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = currentName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("僵尸代号 (SpiderZombieName)") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedBorderColor = themeColor,
                                    focusedLabelColor = themeColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                PRESET_PARACHUTE_ZOMBIES.forEach { (code, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(label)
                                            }
                                        },
                                        onClick = {
                                            sync(actionDataState.value.copy(spiderZombieName = code))
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            // === 区域 2: 数量与批次 ===
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "生成数量控制",
                            color = themeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberInputInt(
                                value = actionDataState.value.spiderCount,
                                onValueChange = { sync(actionDataState.value.copy(spiderCount = it)) },
                                label = "总数量 (Total)",
                                modifier = Modifier.weight(1f),
                                color = themeColor
                            )
                            NumberInputInt(
                                value = actionDataState.value.groupSize,
                                onValueChange = { sync(actionDataState.value.copy(groupSize = it)) },
                                label = "每批数量 (GroupSize)",
                                modifier = Modifier.weight(1f),
                                color = themeColor
                            )
                        }
                    }
                }
            }

            // === 区域 3: 范围与时间 ===
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "位置与时间参数",
                            color = themeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberInputInt(
                                value = actionDataState.value.columnStart,
                                onValueChange = { sync(actionDataState.value.copy(columnStart = it)) },
                                label = "起始列 (Start)",
                                modifier = Modifier.weight(1f),
                                color = themeColor
                            )
                            NumberInputInt(
                                value = actionDataState.value.columnEnd,
                                onValueChange = { sync(actionDataState.value.copy(columnEnd = it)) },
                                label = "结束列 (End)",
                                modifier = Modifier.weight(1f),
                                color = themeColor
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberInputDouble(
                                value = actionDataState.value.timeBetweenGroups,
                                onValueChange = { sync(actionDataState.value.copy(timeBetweenGroups = it)) },
                                label = "批次间隔 (秒)",
                                modifier = Modifier.weight(1f),
                                color = themeColor
                            )
                            NumberInputDouble(
                                value = actionDataState.value.zombieFallTime,
                                onValueChange = { sync(actionDataState.value.copy(zombieFallTime = it)) },
                                label = "降落耗时 (秒)",
                                modifier = Modifier.weight(1f),
                                color = themeColor
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        NumberInputDouble(
                            value = actionDataState.value.timeBeforeFullSpawn,
                            onValueChange = { sync(actionDataState.value.copy(timeBeforeFullSpawn = it)) },
                            label = "生成前摇时间 (TimeBeforeFullSpawn)",
                            modifier = Modifier.fillMaxWidth(),
                            color = themeColor
                        )
                    }
                }
            }

            // === 区域 4: 提示信息 ===
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "红色字幕警告信息",
                            color = themeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = actionDataState.value.waveStartMessage,
                            onValueChange = { sync(actionDataState.value.copy(waveStartMessage = it)) },
                            label = { Text("WaveStartMessage") },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedBorderColor = themeColor,
                                focusedLabelColor = themeColor
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "空降开始时在屏幕中央显示的红字警告，不支持输入中文",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}