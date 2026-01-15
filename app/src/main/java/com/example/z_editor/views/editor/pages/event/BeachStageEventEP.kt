package com.example.z_editor.views.editor.pages.event

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.z_editor.data.BeachStageEventData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.repository.ZombiePropertiesRepository
import com.example.z_editor.data.repository.ZombieRepository
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputDouble
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeachStageEventEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: LazyListState,
    onRequestZombieSelection: ((String) -> Unit) -> Unit
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }


    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, BeachStageEventData::class.java)
    val actionDataState = syncManager.dataState

    fun sync(newData: BeachStageEventData) {
        actionDataState.value = newData
        syncManager.sync()
    }

    val currentZombieInfo = remember(actionDataState.value.zombieName) {
        val realName = ZombiePropertiesRepository.getTypeNameByAlias(actionDataState.value.zombieName)
        val name = ZombieRepository.getName(realName)
        ZombieRepository.getZombieInfoById(name) to realName
    }


    val themeColor = Color(0xFF00ACC1)

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("编辑 $currentAlias", fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("事件类型：退潮突袭", fontSize = 14.sp, fontWeight = FontWeight.Normal)
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
                    containerColor = themeColor,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "退潮突袭事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "僵尸会从水下浮现。通常用于巨浪沙滩的潜水僵尸，或者需要在低潮期出现的僵尸。"
                )
                HelpSection(
                    title = "生成机制",
                    body = "与空降事件类似，僵尸会分批次出现。可以指定总数量和出现范围。"
                )
                HelpSection(
                    title = "僵尸种类",
                    body = "在单个事件中只能出现一种僵尸，若想实现多种僵尸出现需要额外添加若干次事件。"
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
            // === 区域 1: 僵尸类型选择 ===
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Water, null, tint = themeColor)
                            Spacer(Modifier.width(12.dp))
                            Text("突袭单位配置", color = themeColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE0F7FA), RoundedCornerShape(8.dp))
                                .border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onRequestZombieSelection { selectedId ->
                                        val aliases = ZombieRepository.buildAliases(selectedId)
                                        sync(actionDataState.value.copy(zombieName = aliases))
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (info, realTypeName) = currentZombieInfo
                            val displayName = info?.name ?: realTypeName

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White, CircleShape)
                                    .border(1.dp, Color.LightGray, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                AssetImage(
                                    path = if (info?.icon != null) "images/zombies/${info.icon}" else null,
                                    contentDescription = displayName,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    filterQuality = FilterQuality.Medium,
                                    placeholder = {
                                        Text(displayName.take(1), fontWeight = FontWeight.Bold, color = themeColor)
                                    }
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                Text(realTypeName, fontSize = 12.sp, color = Color.Gray)
                            }

                            Icon(Icons.Default.Edit, "更改", tint = themeColor)
                        }
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
                        Text("生成数量控制", color = themeColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberInputInt(
                                value = actionDataState.value.zombieCount,
                                onValueChange = { sync(actionDataState.value.copy(zombieCount = it)) },
                                label = "总数量 (ZombieCount)",
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("位置与时间参数", color = themeColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(12.dp))

                        // 列范围
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

                        // 时间参数
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberInputDouble(
                                value = actionDataState.value.timeBetweenGroups,
                                onValueChange = { sync(actionDataState.value.copy(timeBetweenGroups = it)) },
                                label = "批次间隔 (秒)",
                                modifier = Modifier.weight(1f),
                                color = themeColor
                            )
                            NumberInputDouble(
                                value = actionDataState.value.timeBeforeFullSpawn,
                                onValueChange = {
                                    sync(
                                        actionDataState.value.copy(
                                            timeBeforeFullSpawn = it
                                        )
                                    )
                                },
                                label = "生成前摇 (秒)",
                                modifier = Modifier.weight(1f),
                                color = themeColor
                            )
                        }
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
                        Text("红色字幕警告信息", color = themeColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = actionDataState.value.waveStartMessage,
                            onValueChange = { sync(actionDataState.value.copy(waveStartMessage = it)) },
                            label = { Text("WaveStartMessage") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                focusedLabelColor = themeColor
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "事件开始时在屏幕中央显示的红字警告，不支持输入中文",
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