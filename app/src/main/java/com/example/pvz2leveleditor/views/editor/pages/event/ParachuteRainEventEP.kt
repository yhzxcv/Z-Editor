package com.example.pvz2leveleditor.views.editor.pages.event

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pvz2leveleditor.data.*
import com.example.pvz2leveleditor.views.editor.*
import com.google.gson.Gson

private val gson = Gson()

private val PRESET_PARACHUTE_ZOMBIES = listOf(
    "lostcity_lostpilot" to "失落飞行员",
    "zcrop_helpdesk" to "Z公司服务台"
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

    val actionDataState = remember {
        val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        val data = try {
            gson.fromJson(obj?.objData, ParachuteRainEventData::class.java)
        } catch (e: Exception) {
            ParachuteRainEventData()
        }
        mutableStateOf(data)
    }

    fun sync(newData: ParachuteRainEventData) {
        actionDataState.value = newData
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }?.let {
            it.objData = gson.toJsonTree(newData)
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
                        Text("编辑 $currentAlias", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("事件类型：空降突袭", fontSize = 14.sp, fontWeight = FontWeight.Normal)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "帮助", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF9800),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        // 帮助弹窗
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "空降突袭说明",
                onDismiss = { showHelpDialog = false },
                themeColor = Color(0xFFFF9800)
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "僵尸会从屏幕上方掉落突袭，通常用于失落之城的飞行员僵尸。僵尸的阶级随地图阶级序列。"
                )
                HelpSection(
                    title = "生成逻辑",
                    body = "事件触发后，僵尸会分批次从天而降。可以控制总数量和每批次之间的时间间隔。僵尸会随机降落在选择的列数。"
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
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === 区域 1: 僵尸类型配置 (带下拉列表) ===
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("空降单位配置", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
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
                                                Text(label, fontWeight = FontWeight.Bold)
                                                Text(code, fontSize = 10.sp, color = Color.Gray)
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("生成数量控制", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberInputInt(
                                value = actionDataState.value.spiderCount,
                                onValueChange = { sync(actionDataState.value.copy(spiderCount = it)) },
                                label = "总数量 (Total)",
                                modifier = Modifier.weight(1f)
                            )
                            NumberInputInt(
                                value = actionDataState.value.groupSize,
                                onValueChange = { sync(actionDataState.value.copy(groupSize = it)) },
                                label = "每批数量 (GroupSize)",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // === 区域 3: 范围与时间 ===
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("位置与时间参数", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(12.dp))

                        // 列范围
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberInputInt(
                                value = actionDataState.value.columnStart,
                                onValueChange = { sync(actionDataState.value.copy(columnStart = it)) },
                                label = "起始列 (Start)",
                                modifier = Modifier.weight(1f)
                            )
                            NumberInputInt(
                                value = actionDataState.value.columnEnd,
                                onValueChange = { sync(actionDataState.value.copy(columnEnd = it)) },
                                label = "结束列 (End)",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // 时间参数
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberInputDouble(
                                value = actionDataState.value.timeBetweenGroups,
                                onValueChange = { sync(actionDataState.value.copy(timeBetweenGroups = it)) },
                                label = "批次间隔 (秒)",
                                modifier = Modifier.weight(1f)
                            )
                            NumberInputDouble(
                                value = actionDataState.value.zombieFallTime,
                                onValueChange = { sync(actionDataState.value.copy(zombieFallTime = it)) },
                                label = "降落耗时 (秒)",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        NumberInputDouble(
                            value = actionDataState.value.timeBeforeFullSpawn,
                            onValueChange = { sync(actionDataState.value.copy(timeBeforeFullSpawn = it)) },
                            label = "完全生成前摇时间 (TimeBeforeFullSpawn)",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // === 区域 4: 提示信息 ===
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("红色字幕警告信息", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = actionDataState.value.waveStartMessage,
                            onValueChange = { sync(actionDataState.value.copy(waveStartMessage = it)) },
                            label = { Text("WaveStartMessage") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "空降开始时在屏幕中央显示的红字警告，不支持输入中文",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}