package com.example.pvz2leveleditor.views.editor.pages.others

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pvz2leveleditor.data.LevelDefinitionData
import com.example.pvz2leveleditor.data.RtidParser
import com.example.pvz2leveleditor.views.editor.EditorHelpDialog
import com.example.pvz2leveleditor.views.editor.HelpSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelDefinitionEP(
    levelDef: LevelDefinitionData,
    onBack: () -> Unit,
    onNavigateToStageSelection: () -> Unit,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    // --- 状态绑定 ---
    var name by remember { mutableStateOf(levelDef.name) }
    var description by remember { mutableStateOf(levelDef.description) }
    var levelNumber by remember { mutableStateOf(levelDef.levelNumber.toString()) }

    var startingSunInput by remember { mutableStateOf(levelDef.startingSun?.toString() ?: "") }

    var victoryExpanded by remember { mutableStateOf(false) }
    var currentVictoryRtid by remember { mutableStateOf(levelDef.victoryModule) }
    val victoryOptions = listOf(
        "RTID(VictoryOutro@LevelModules)" to "默认胜利 (VictoryOutro)"
    )
    val currentVictoryLabel =
        victoryOptions.find { it.first == currentVictoryRtid }?.second ?: levelDef.victoryModule

    var lootExpanded by remember { mutableStateOf(false) }
    var currentLootRtid by remember { mutableStateOf(levelDef.loot) }
    val lootOptions = listOf(
        "RTID(DefaultLoot@LevelModules)" to "默认掉落 (DefaultLoot)",
        "RTID(NoLoot@LevelModules)" to "无掉落 (NoLoot)"
    )
    val currentLootLabel =
        lootOptions.find { it.first == currentLootRtid}?.second ?: levelDef.loot

    var disablePeavine by remember { mutableStateOf(levelDef.disablePeavine ?: false) }
    var isArtifactDisabled by remember { mutableStateOf(levelDef.isArtifactDisabled ?: false) }

    val currentStageInfo = remember(levelDef.stageModule) {
        RtidParser.parse(levelDef.stageModule)
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text("关卡基本信息") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "帮助说明", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF388E3C),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "关卡定义基础说明",
                onDismiss = { showHelpDialog = false },
                themeColor = Color(0xFF388E3C) // 使用与TopBar一致的主题色
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "关卡定义是pvz2关卡的根节点，不仅存放了关卡全局属性，还通过模块列表逐级链接了关卡内所有模块。本页面可以对关卡全局属性进行修改。"
                )
                HelpSection(
                    title = "基础信息界面",
                    body = "这里可以定义关卡名称、描述、初始阳光等基本信息。关卡描述字段是进入关卡播放转场动画时的白色字幕。初始阳光是指计算账号阳光强化前的可用阳光。"
                )
                HelpSection(
                    title = "场景设置",
                    body = "这里可以从列表中选择关卡的地图，注意海盗地图默认无甲板。结算方式仅适用于部分特殊关卡种类，一般不建议对其进行修改。"
                )
                HelpSection(
                    title = "限制选项",
                    body = "这里提供禁用豆藤共生和神器开关。在庭院模块下神器自动禁用，无需手动开启禁用开关。"
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
            Text(
                "基础信息",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF388E3C)
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    levelDef.name = it
                },
                label = { Text("关卡名称 (Name)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = levelNumber,
                    onValueChange = {
                        levelNumber = it
                        levelDef.levelNumber = it.toIntOrNull() ?: levelDef.levelNumber
                    },
                    label = { Text("关卡序号") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = startingSunInput,
                    onValueChange = {
                        startingSunInput = it
                        val sunVal = it.toIntOrNull()
                        levelDef.startingSun = sunVal
                    },
                    label = { Text("初始阳光") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    levelDef.description = it
                },
                label = { Text("关卡描述 (Description)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            HorizontalDivider()

            Text(
                "场景设置",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF388E3C)
            )

            Card(
                onClick = onNavigateToStageSelection,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Map, null, tint = Color(0xFF388E3C))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("关卡地图 (StageModule)", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = currentStageInfo?.alias ?: "未选择 / Unknown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = lootExpanded,
                onExpandedChange = { lootExpanded = !lootExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = currentLootLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("关卡默认掉落 (Loot)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lootExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF388E3C),
                        focusedLabelColor = Color(0xFF388E3C)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = lootExpanded,
                    onDismissRequest = { lootExpanded = false }
                ) {
                    lootOptions.forEach { (rtid, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                levelDef.loot = rtid
                                currentLootRtid = rtid
                                lootExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = victoryExpanded,
                onExpandedChange = { victoryExpanded = !victoryExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = currentVictoryLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("胜利结算方式 (VictoryModule)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = victoryExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF388E3C),
                        focusedLabelColor = Color(0xFF388E3C)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = victoryExpanded,
                    onDismissRequest = { victoryExpanded = false }
                ) {
                    victoryOptions.forEach { (rtid, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                levelDef.victoryModule = rtid
                                currentVictoryRtid = rtid
                                victoryExpanded = false
                            }
                        )
                    }
                }
            }

            Text(
                "使用默认结算方式以外的结算方式可能会因为模块冲突导致关卡闪退，请谨慎使用",
                fontSize = 12.sp,
                color = Color.Gray
            )

            HorizontalDivider()

            Text(
                "限制选项",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF388E3C)
            )

            SwitchOptionItem(
                title = "禁用豆藤共生 (DisablePeavine)",
                subtitle = "开启后，无法在本关卡使用豆藤共生",
                checked = disablePeavine,
                onCheckedChange = {
                    disablePeavine = it
                    levelDef.disablePeavine = if (it) true else null
                }
            )

            SwitchOptionItem(
                title = "禁用神器 (IsArtifactDisabled)",
                subtitle = "开启后，本关卡无法携带神器",
                checked = isArtifactDisabled,
                onCheckedChange = {
                    isArtifactDisabled = it
                    levelDef.isArtifactDisabled = if (it) true else null
                }
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// 辅助组件：带文字说明的开关行
@Composable
fun SwitchOptionItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray, lineHeight = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF388E3C))
        )
    }
}