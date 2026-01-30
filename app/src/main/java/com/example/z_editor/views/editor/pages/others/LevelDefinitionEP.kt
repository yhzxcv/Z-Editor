package com.example.z_editor.views.editor.pages.others

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
import com.example.z_editor.data.LevelDefinitionData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelDefinitionEP(
    rootLevelFile: PvzLevelFile,
    onBack: () -> Unit,
    onNavigateToStageSelection: () -> Unit,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val obj = remember(rootLevelFile) {
        rootLevelFile.objects.find { it.objClass == "LevelDefinition" }
    }
    val syncManager = rememberJsonSync(obj, LevelDefinitionData::class.java)
    var levelDef by syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    var levelNumberInput by remember(levelDef.levelNumber) {
        mutableStateOf(levelDef.levelNumber?.toString() ?: "")
    }

    var startingSunInput by remember(levelDef.startingSun) {
        mutableStateOf(levelDef.startingSun?.toString() ?: "")
    }

    var victoryExpanded by remember { mutableStateOf(false) }
    val victoryOptions = listOf(
        "RTID(VictoryOutro@LevelModules)" to "默认胜利 (VictoryOutro)",
        "RTID(ZombossVictoryOutro@LevelModules)" to "僵王战胜利 (ZombossVictoryOutro)"
    )
    val currentVictoryLabel =
        victoryOptions.find { it.first == levelDef.victoryModule }?.second ?: levelDef.victoryModule

    var lootExpanded by remember { mutableStateOf(false) }
    val lootOptions = listOf(
        "RTID(DefaultLoot@LevelModules)" to "默认掉落 (DefaultLoot)",
        "RTID(NoLoot@LevelModules)" to "无掉落 (NoLoot)"
    )
    val currentLootLabel =
        lootOptions.find { it.first == levelDef.loot }?.second ?: levelDef.loot

    var musicTypeExpanded by remember { mutableStateOf(false) }
    val musicTypeOptions = listOf(
        "" to "默认",
        "MiniGame_A" to "小游戏 A (MiniGame_A)",
        "MiniGame_B" to "小游戏 B (MiniGame_B)"
    )
    val currentMusicTypeLabel = musicTypeOptions.find { it.first == levelDef.musicType }?.second
        ?: levelDef.musicType.ifEmpty { "默认" }

    val currentStageInfo = remember(levelDef.stageModule) {
        RtidParser.parse(levelDef.stageModule)
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text("关卡基本信息", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MaterialTheme.colorScheme.surface)
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "帮助说明", tint = MaterialTheme.colorScheme.surface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.surface,
                    actionIconContentColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "关卡定义基础说明",
                onDismiss = { showHelpDialog = false },
                themeColor = MaterialTheme.colorScheme.primary
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
                fontWeight = FontWeight.Bold, fontSize = 16.sp,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = levelDef.name,
                onValueChange = {
                    levelDef = levelDef.copy(name = it)
                    sync()
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                label = { Text("关卡名称 (Name)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = levelNumberInput,
                    onValueChange = {
                        levelNumberInput = it
                        val num = it.toIntOrNull()
                        levelDef = levelDef.copy(levelNumber = num)
                        sync()
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    label = { Text("关卡序号") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = startingSunInput,
                    onValueChange = {
                        startingSunInput = it
                        val sunVal = it.toIntOrNull()
                        levelDef = levelDef.copy(startingSun = sunVal)
                        sync()
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    label = { Text("初始阳光") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = levelDef.description,
                onValueChange = {
                    levelDef = levelDef.copy(description = it)
                    sync()
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                label = { Text("关卡描述 (Description)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text(
                "场景设置",
                fontWeight = FontWeight.Bold, fontSize = 16.sp,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                onClick = onNavigateToStageSelection,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Map, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("关卡地图 (StageModule)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = currentStageInfo?.alias ?: "未选择 / Unknown",
                            fontSize = 16.sp
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            ExposedDropdownMenuBox(
                expanded = musicTypeExpanded,
                onExpandedChange = { musicTypeExpanded = !musicTypeExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = currentMusicTypeLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("音乐类型 (MusicType)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = musicTypeExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = musicTypeExpanded,
                    onDismissRequest = { musicTypeExpanded = false }
                ) {
                    musicTypeOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                levelDef = levelDef.copy(musicType = value)
                                sync()
                                musicTypeExpanded = false
                            }
                        )
                    }
                }
            }

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
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = lootExpanded,
                    onDismissRequest = { lootExpanded = false }
                ) {
                    lootOptions.forEach { (rtid, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                levelDef = levelDef.copy(loot = rtid)
                                sync()
                                lootExpanded = false
                            }
                        )
                    }
                }
            }

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
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = victoryExpanded,
                    onDismissRequest = { victoryExpanded = false }
                ) {
                    victoryOptions.forEach { (rtid, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                levelDef = levelDef.copy(victoryModule = rtid)
                                sync()
                                victoryExpanded = false
                            }
                        )
                    }
                }
            }

            Text(
                "使用默认结算方式以外的结算方式可能会因为模块冲突导致关卡闪退，请谨慎使用",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text(
                "限制选项",
                fontWeight = FontWeight.Bold, fontSize = 16.sp,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            SwitchOptionItem(
                title = "禁用豆藤共生 (DisablePeavine)",
                subtitle = "开启后，无法在本关卡使用豆藤共生",
                checked = levelDef.disablePeavine == true,
                onCheckedChange = {
                    levelDef = levelDef.copy(disablePeavine = if (it) true else null)
                    sync()
                }
            )

            SwitchOptionItem(
                title = "禁用神器 (IsArtifactDisabled)",
                subtitle = "开启后，关卡无法携带神器，庭院模式下自动生效",
                checked = levelDef.isArtifactDisabled == true,
                onCheckedChange = {
                    levelDef = levelDef.copy(isArtifactDisabled = if (it) true else null)
                    sync()
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
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = Color.Transparent,

                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        )
    }
}