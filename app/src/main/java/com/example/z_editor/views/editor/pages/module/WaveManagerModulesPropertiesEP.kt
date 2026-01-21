package com.example.z_editor.views.editor.pages.module

import android.widget.Toast
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.DynamicZombieGroup
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.WaveManagerModuleData
import com.example.z_editor.data.repository.ZombiePropertiesRepository
import com.example.z_editor.data.repository.ZombieRepository
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaveManagerModulePropertiesEP(
    rootLevelFile: PvzLevelFile,
    rtid: String,
    onBack: () -> Unit,
    onRequestZombieSelection: ((String) -> Unit) -> Unit,
    scrollState: ScrollState
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val obj = remember(rootLevelFile) {
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    }
    val syncManager = rememberJsonSync(obj, WaveManagerModuleData::class.java)
    val moduleData = syncManager.dataState.value

    fun sync() {
        syncManager.sync()
    }

    LaunchedEffect(Unit) {
        var needsUpdate = false
        val currentData = syncManager.dataState.value

        if (currentData.dynamicZombies.isEmpty()) {
            currentData.dynamicZombies.add(DynamicZombieGroup())
            needsUpdate = true
        } else {
            // 检查并修复每个组的 level 列表长度，确保与 zombiePool 长度一致
            currentData.dynamicZombies.forEach { group ->
                while (group.zombieLevel.size < group.zombiePool.size) {
                    group.zombieLevel.add(1)
                    needsUpdate = true
                }
            }
        }

        if (needsUpdate) {
            sync()
        }
    }

    var refreshTrigger by remember { mutableIntStateOf(0) }

    val actualWaveMgrAlias = remember(rootLevelFile.objects) {
        rootLevelFile.objects.find { it.objClass == "WaveManagerProperties" }?.aliases?.firstOrNull()
    }

    // 使用 moduleData 获取当前值。注意处理 waveManagerProps 可能为 null 的情况
    val currentPropsAlias = RtidParser.parse(moduleData.waveManagerProps ?: "")?.alias
    val isPropsValid = actualWaveMgrAlias != null && currentPropsAlias == actualWaveMgrAlias

    // 检查 LastStand 逻辑
    val hasLastStandModule = remember(rootLevelFile.objects) {
        rootLevelFile.objects.any { it.objClass == "LastStandMinigameProperties" }
    }

    LaunchedEffect(hasLastStandModule) {
        if (hasLastStandModule && moduleData.manualStartup != true) {
            // 更新 State 并同步
            syncManager.dataState.value = moduleData.copy(manualStartup = true)
            sync()
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
            })
        },
        topBar = {
            TopAppBar(
                title = { Text("波次管理器配置", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null,
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "帮助说明", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF673AB7),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "波次管理器模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = Color(0xFF673AB7)
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "波次管理器模块是波次事件容器的前置定义。只有添加了波次管理器模块软件才会开放波次事件编辑入口。"
                )
                HelpSection(
                    title = "关联波次参数",
                    body = "波次管理器用一个Rtid语句块指向波次事件容器。这里会自动校验该语句是否正确地指向容器。"
                )
                HelpSection(
                    title = "点数分配设置",
                    body = "此处原为国际版旧版本的动态难度机制，在中文版和新版本用于实现点数出怪。这里可以修改关卡点数配置情况。"
                )
                HelpSection(
                    title = "点数出怪介绍",
                    body = "点数出怪会根据僵尸消耗的点数在有效波次中额外刷新僵尸。常规波次点数上限为60000，旗帜波点数会变为2.5倍。"
                )
                HelpSection(
                    title = "僵尸池设置",
                    body = "点数为正数时，出怪使用的僵尸从僵尸池中选取。在波次容器编辑页面内可查看每种僵尸的出现期望。点数为负数时，会从自然出怪事件中扣除相应点数的僵尸。注意点数出怪池不应该写精英怪以及自定义僵尸。"
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isPropsValid) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isPropsValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                            null,
                            tint = if (isPropsValid) Color(0xFF388E3C) else Color(0xFFD32F2F)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("关联波次参数 (WaveManagerProps)", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "当前值: ${moduleData.waveManagerProps ?: "null"}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    if (actualWaveMgrAlias == null) {
                        Text(
                            "错误：当前关卡不存在波次容器，该模块无法正常工作",
                            color = Color(0xFFD32F2F),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (!isPropsValid && actualWaveMgrAlias != null) {
                        Text(
                            "错误：当前指向无效。这会导致波次无法正确加载。",
                            color = Color(0xFFD32F2F),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Button(
                            onClick = {
                                val newProps = RtidParser.build(actualWaveMgrAlias, "CurrentLevel")
                                syncManager.dataState.value = moduleData.copy(waveManagerProps = newProps)
                                sync()
                            },
                            modifier = Modifier.padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text("自动修正至: $actualWaveMgrAlias", fontSize = 12.sp)
                        }
                    }
                }
            }

            val firstGroup = moduleData.dynamicZombies[0]

            Text(
                "点数分配设置",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF673AB7),
                fontSize = 14.sp
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NumberInputInt(
                        firstGroup.startingWave,
                        {
                            firstGroup.startingWave = it
                            sync()
                        },
                        "起始波次 (StartingWave)",
                        color = Color(0xFF673AB7)
                    )
                    NumberInputInt(
                        firstGroup.startingPoints,
                        {
                            firstGroup.startingPoints = it
                            sync()
                        },
                        "起始点数 (StartingPoints)",
                        color = Color(0xFF673AB7)
                    )
                    NumberInputInt(
                        firstGroup.pointIncrement,
                        {
                            firstGroup.pointIncrement = it
                            sync()
                        },
                        "每波点数增量 (PointIncrement)",
                        color = Color(0xFF673AB7)
                    )
                }
            }

            Text(
                "僵尸池 (ZombiePool)",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF673AB7),
                fontSize = 14.sp
            )

            key(refreshTrigger) {
                ZombiePoolEditor(
                    zombiePool = firstGroup.zombiePool,
                    zombieLevel = firstGroup.zombieLevel,
                    onAdd = {
                        onRequestZombieSelection { selectedId ->
                            val isElite = ZombieRepository.isElite(selectedId)
                            val aliases = ZombieRepository.buildAliases(selectedId)
                            if (!isElite) {
                                firstGroup.zombiePool.add(RtidParser.build(aliases, "ZombieTypes"))
                                firstGroup.zombieLevel.add(1)
                                sync()
                                refreshTrigger++
                            } else Toast.makeText(context, "不能添加精英僵尸", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRemove = { index ->
                        if (index < firstGroup.zombiePool.size) {
                            firstGroup.zombiePool.removeAt(index)
                            if (index < firstGroup.zombieLevel.size) firstGroup.zombieLevel.removeAt(
                                index
                            )
                            sync()
                            refreshTrigger++
                        }
                    },
                    onLevelChange = { index, newLevel ->
                        if (index < firstGroup.zombieLevel.size) {
                            val newLevels = firstGroup.zombieLevel.toMutableList()
                            newLevels[index] = newLevel
                            firstGroup.zombieLevel.clear()
                            firstGroup.zombieLevel.addAll(newLevels)
                            sync()
                            refreshTrigger++
                        }
                    }
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Info, null, tint = Color(0xFF7B1FA2))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "非庭院模式下关卡僵尸阶级只定义到5阶，点数为负数时会从自然出怪事件中扣除僵尸。",
                            fontSize = 12.sp,
                            color = Color(0xFF7B1FA2),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ZombiePoolEditor(
    zombiePool: List<String>,
    zombieLevel: List<Int>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onLevelChange: (Int, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        zombiePool.forEachIndexed { index, rtid ->
            val alias = RtidParser.parse(rtid)?.alias ?: rtid
            val typeName = ZombiePropertiesRepository.getTypeNameByAlias(alias)
            val displayName = ZombieRepository.getName(typeName)
            val level = zombieLevel.getOrNull(index) ?: 1
            val info = remember(typeName) {
                ZombieRepository.getZombieInfoById(typeName)
            }

            val placeholderContent = @Composable {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        fontSize = 24.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssetImage(
                    path = if (info?.icon != null) "images/zombies/${info.icon}" else null,
                    contentDescription = displayName,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp)),
                    filterQuality = FilterQuality.Medium,
                    placeholder = placeholderContent
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(typeName, fontSize = 12.sp, color = Color.Gray)
                    Text(
                        "等级: $level",
                        fontSize = 12.sp,
                        color = if (level >= 6) Color(0xFFD32F2F) else Color(0xFF388E3C)
                    )
                }

                IconButton(onClick = { if (level > 1) onLevelChange(index, level - 1) }) {
                    Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = { if (level < 10) onLevelChange(index, level + 1) },
                    enabled = level < 10
                ) {
                    Icon(
                        Icons.Default.Add, null,
                        modifier = Modifier.size(16.dp),
                        tint = if (level < 10) LocalContentColor.current else Color.LightGray
                    )
                }

                IconButton(onClick = { onRemove(index) }) {
                    Icon(Icons.Default.Delete, null, tint = Color.LightGray)
                }
            }
        }

        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF673AB7))
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("添加新僵尸到池中")
        }
    }
}