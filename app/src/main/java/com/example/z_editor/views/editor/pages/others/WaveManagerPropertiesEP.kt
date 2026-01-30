package com.example.z_editor.views.editor.pages.others

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.material3.Surface
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
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.WaveManagerData
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzBlueDark
import com.example.z_editor.ui.theme.PvzBlueLight
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaveManagerPropertiesEP(
    rootLevelFile: PvzLevelFile,
    hasConveyor: Boolean,
    onBack: () -> Unit,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    // [SyncManager] 初始化
    val obj = remember(rootLevelFile) {
        rootLevelFile.objects.find { it.objClass == "WaveManagerProperties" }
    }
    val syncManager = rememberJsonSync(obj, WaveManagerData::class.java)
    var waveManager by syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    // --- 状态绑定 (需要 String 转换的字段) ---
    var flagInterval by remember(waveManager.flagWaveInterval) { mutableStateOf(waveManager.flagWaveInterval.toString()) }

    // LevelJam 下拉框状态
    var jamExpanded by remember { mutableStateOf(false) }
    val jamOptions = listOf(
        null to "默认/无 (None)",
        "jam_pop" to "流行 (Pop)",
        "jam_rap" to "说唱 (Rap)",
        "jam_metal" to "重金属 (Metal)",
        "jam_punk" to "朋克 (Punk)",
        "jam_8bit" to "街机 (8-Bit)"
    )
    val selectedJamLabel =
        jamOptions.find { it.first == waveManager.levelJam }?.second ?: "默认/无 (None)"

    // === 时间控制相关 ===
    val defaultFirstWaveSecs = if (hasConveyor) 5 else 12
    val currentFirstWaveVal = if (hasConveyor) {
        waveManager.zombieCountDownFirstWaveConveyorSecs ?: defaultFirstWaveSecs
    } else {
        waveManager.zombieCountDownFirstWaveSecs ?: defaultFirstWaveSecs
    }
    var firstWaveInput by remember(currentFirstWaveVal) { mutableStateOf(currentFirstWaveVal.toString()) }

    val defaultHugeDelay = 5
    val currentHugeDelayVal = waveManager.zombieCountDownHugeWaveDelay ?: defaultHugeDelay
    var hugeWaveInput by remember(currentHugeDelayVal) { mutableStateOf(currentHugeDelayVal.toString()) }

    // 保存时间设置的逻辑
    fun saveTimeSettings(firstValStr: String, hugeValStr: String) {
        val inputFirst = firstValStr.toIntOrNull() ?: defaultFirstWaveSecs
        val inputHuge = hugeValStr.toIntOrNull() ?: defaultHugeDelay

        var newMgr = waveManager
        if (hasConveyor) {
            newMgr = newMgr.copy(
                zombieCountDownFirstWaveConveyorSecs = if (inputFirst == 5) null else inputFirst,
                zombieCountDownFirstWaveSecs = null
            )
        } else {
            newMgr = newMgr.copy(
                zombieCountDownFirstWaveSecs = if (inputFirst == 12) null else inputFirst,
                zombieCountDownFirstWaveConveyorSecs = null
            )
        }
        newMgr = newMgr.copy(
            zombieCountDownHugeWaveDelay = if (inputHuge == 5) null else inputHuge
        )
        waveManager = newMgr
        sync()
    }

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzBlueDark else PvzBlueLight

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "波次事件参数配置",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "返回",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            "帮助说明",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColor,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "波次事件容器说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "波次事件容器用于按波次顺序存放关卡的众多事件，大部分关卡都是通过波次事件进行出怪安排的。这个页面用于调控波次事件容器的全局参数。"
                )
                HelpSection(
                    title = "旗帜间隔",
                    body = "旗帜间隔指旗帜波波僵尸的相隔小波数，除此之外每关的最后一波也会是旗帜波。旗帜波享有额外的点数加成和刷新间隔。"
                )
                HelpSection(
                    title = "刷新血线",
                    body = "每一波的刷新血线都在最大值和最小值之间浮动。当前波次内通过自然出现方式生成的僵尸的总血量低于这一比例就会自动刷新下一波，"
                )
                HelpSection(
                    title = "时间控制",
                    body = "第一波僵尸到来前的时间间隔会随关卡是否有传送带变化，从自选卡的12秒变为5秒。旗帜波延迟指的是红字提示到僵尸刷新的间隔。"
                )
                HelpSection(
                    title = "音乐类型",
                    body = "本设置项只适用于摩登世界地图，用于设定一类不可更改的全局背景音乐为魔音僵尸提供技能。"
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "基础参数",
                fontWeight = FontWeight.Bold, fontSize = 16.sp,
                style = MaterialTheme.typography.titleMedium,
                color = themeColor
            )

            OutlinedTextField(
                value = flagInterval,
                onValueChange = {
                    flagInterval = it
                    val num = it.toIntOrNull()
                    if (num != null) {
                        waveManager = waveManager.copy(flagWaveInterval = num)
                        sync()
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    cursorColor = themeColor,
                    selectionColors = TextSelectionColors(
                        handleColor = themeColor,
                        backgroundColor = themeColor.copy(alpha = 0.4f)
                    ),
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedBorderColor = themeColor,
                    focusedLabelColor = themeColor
                ),
                label = { Text("旗帜间隔 (FlagWaveInterval)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NumberInputDouble(
                    value = waveManager.maxNextWaveHealthPercentage,
                    onValueChange = { input ->
                        val clamped = input.coerceIn(0.0, 1.0)
                        if (waveManager.maxNextWaveHealthPercentage != clamped) {
                            waveManager = waveManager.copy(maxNextWaveHealthPercentage = clamped)
                            sync()
                        }
                    },
                    color = themeColor,
                    label = "最大刷新血线",
                    modifier = Modifier.weight(1f)
                )

                NumberInputDouble(
                    value = waveManager.minNextWaveHealthPercentage,
                    onValueChange = { input ->
                        val clamped = input.coerceIn(0.0, 1.0)
                        if (waveManager.minNextWaveHealthPercentage != clamped) {
                            waveManager = waveManager.copy(minNextWaveHealthPercentage = clamped)
                            sync()
                        }
                    },
                    color = themeColor,
                    label = "最小刷新血线",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                "刷新血线的值需要为0到1之间的数",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "时间控制",
                fontWeight = FontWeight.Bold, fontSize = 16.sp,
                style = MaterialTheme.typography.titleMedium,
                color = themeColor
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = firstWaveInput,
                    onValueChange = {
                        firstWaveInput = it
                        saveTimeSettings(it, hugeWaveInput)
                    },
                    label = {
                        Text(if (hasConveyor) "首波延迟 (传送带)" else "首波延迟 (普通)")
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = themeColor,
                        selectionColors = TextSelectionColors(
                            handleColor = themeColor,
                            backgroundColor = themeColor.copy(alpha = 0.4f)
                        ),
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = themeColor,
                        focusedLabelColor = themeColor
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = hugeWaveInput,
                    onValueChange = {
                        hugeWaveInput = it
                        saveTimeSettings(firstWaveInput, it)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = themeColor,
                        selectionColors = TextSelectionColors(
                            handleColor = themeColor,
                            backgroundColor = themeColor.copy(alpha = 0.4f)
                        ),
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = themeColor,
                        focusedLabelColor = themeColor
                    ),
                    label = { Text("旗帜波延迟") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Text(
                text = if (hasConveyor) "检测到传送带模块，已自动应用 Conveyor 延迟设置" else "未检测到传送带模块，应用普通模式延迟设置",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "特殊设置",
                fontWeight = FontWeight.Bold, fontSize = 16.sp,
                style = MaterialTheme.typography.titleMedium,
                color = themeColor
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("屏蔽旗帜僵尸", fontWeight = FontWeight.Bold)
                        Text(
                            "SuppressFlagZombie",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = waveManager.suppressFlagZombie == true,
                        onCheckedChange = { isChecked ->
                            waveManager =
                                waveManager.copy(suppressFlagZombie = if (isChecked) true else null)
                            sync()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = themeColor,
                            checkedBorderColor = Color.Transparent,

                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    )
                }
            }
            Text(
                "开启后，大波次来袭时不会生成带旗帜的领头僵尸",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = jamExpanded,
                onExpandedChange = { jamExpanded = !jamExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedJamLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("背景音乐类型 (LevelJam)") },
                    leadingIcon = { Icon(Icons.Default.MusicNote, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = jamExpanded) },
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
                    expanded = jamExpanded,
                    onDismissRequest = { jamExpanded = false }
                ) {
                    jamOptions.forEach { (code, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                waveManager = waveManager.copy(levelJam = code)
                                sync()
                                jamExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
            Text(
                "该设置仅在摩登世界有效，用于为关卡加入不可更改的全局音乐",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(48.dp))
        }
    }
}