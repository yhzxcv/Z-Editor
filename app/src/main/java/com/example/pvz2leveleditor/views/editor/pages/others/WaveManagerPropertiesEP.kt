package com.example.pvz2leveleditor.views.editor.pages.others

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.pvz2leveleditor.data.WaveManagerData
import com.example.pvz2leveleditor.views.editor.EditorHelpDialog
import com.example.pvz2leveleditor.views.editor.HelpSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaveManagerPropertiesEP(
    waveManager: WaveManagerData,
    hasConveyor: Boolean,
    onBack: () -> Unit,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    // --- 状态绑定 ---
    var flagInterval by remember { mutableStateOf(waveManager.flagWaveInterval.toString()) }
    var maxHealth by remember { mutableStateOf(waveManager.maxNextWaveHealthPercentage.toString()) }
    var minHealth by remember { mutableStateOf(waveManager.minNextWaveHealthPercentage.toString()) }

    // SuppressFlagZombie
    var suppressFlag by remember { mutableStateOf(waveManager.suppressFlagZombie ?: false) }

    // LevelJam
    var jamExpanded by remember { mutableStateOf(false) }
    val jamOptions = listOf(
        null to "默认/无 (None)",
        "pop" to "流行 (Pop)",
        "rap" to "说唱 (Rap)",
        "metal" to "重金属 (Metal)",
        "punk" to "朋克 (Punk)",
        "8bit" to "街机 (8-Bit)"
    )
    var selectedJamCode by remember { mutableStateOf(waveManager.levelJam) }
    val selectedJamLabel =
        jamOptions.find { it.first == selectedJamCode }?.second ?: "默认/无 (None)"

    // === 新增：时间控制相关 ===

    val defaultFirstWaveSecs = if (hasConveyor) 5 else 12
    val currentFirstWaveVal = if (hasConveyor) {
        waveManager.zombieCountDownFirstWaveConveyorSecs ?: defaultFirstWaveSecs
    } else {
        waveManager.zombieCountDownFirstWaveSecs ?: defaultFirstWaveSecs
    }
    var firstWaveInput by remember { mutableStateOf(currentFirstWaveVal.toString()) }

    val defaultHugeDelay = 5
    val currentHugeDelayVal = waveManager.zombieCountDownHugeWaveDelay ?: defaultHugeDelay
    var hugeWaveInput by remember { mutableStateOf(currentHugeDelayVal.toString()) }

    // 统一保存逻辑：时间字段变化时调用
    fun saveTimeSettings() {
        val inputFirst = firstWaveInput.toIntOrNull() ?: defaultFirstWaveSecs
        if (hasConveyor) {
            waveManager.zombieCountDownFirstWaveConveyorSecs =
                if (inputFirst == 5) null else inputFirst
            waveManager.zombieCountDownFirstWaveSecs = null
        } else {
            waveManager.zombieCountDownFirstWaveSecs = if (inputFirst == 12) null else inputFirst
            waveManager.zombieCountDownFirstWaveConveyorSecs = null
        }
        val inputHuge = hugeWaveInput.toIntOrNull() ?: defaultHugeDelay
        waveManager.zombieCountDownHugeWaveDelay = if (inputHuge == 5) null else inputHuge
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text("波次事件参数配置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "返回",
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
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "波次事件容器说明",
                onDismiss = { showHelpDialog = false },
                themeColor = Color(0xFF1976D2)
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
                    body = "每一波的刷新血线都在最大值和最小值之间浮动。当前波次内所有方式生成的僵尸的总血量低于这一比例就会自动刷新下一波，"
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
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1976D2)
            )

            OutlinedTextField(
                value = flagInterval,
                onValueChange = {
                    flagInterval = it
                    waveManager.flagWaveInterval = it.toIntOrNull() ?: waveManager.flagWaveInterval
                },
                label = { Text("旗帜间隔 (FlagWaveInterval)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                "当场上僵尸总血量低于以下百分比时，自动提前刷新下一波",
                fontSize = 12.sp,
                color = Color.Gray
            )

            val isMaxError =
                maxHealth.toDoubleOrNull()?.let { it !in 0.0..1.0 } ?: (maxHealth.isNotEmpty())
            val isMinError =
                minHealth.toDoubleOrNull()?.let { it !in 0.0..1.0 } ?: (minHealth.isNotEmpty())

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = maxHealth,
                    onValueChange = { str ->
                        maxHealth = str
                        val v = str.toDoubleOrNull()
                        if (v != null && v in 0.0..1.0) {
                            waveManager.maxNextWaveHealthPercentage = v
                        }
                    },
                    label = { Text("最大刷新血线") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = isMaxError,
                    supportingText = {
                        if (isMaxError) Text("输入需在0到1之间", color = Color.Red)
                    }
                )

                OutlinedTextField(
                    value = minHealth,
                    onValueChange = { str ->
                        minHealth = str
                        val v = str.toDoubleOrNull()
                        if (v != null && v in 0.0..1.0) {
                            waveManager.minNextWaveHealthPercentage = v
                        }
                    },
                    label = { Text("最小刷新血线") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    supportingText = {
                        if (isMinError) Text("输入需在0到1之间", color = Color.Red)
                    }
                )
            }


            HorizontalDivider()
            Text(
                "时间控制",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1976D2)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = firstWaveInput,
                    onValueChange = {
                        firstWaveInput = it
                        saveTimeSettings()
                    },
                    label = {
                        Text(if (hasConveyor) "首波延迟 (传送带)" else "首波延迟 (普通)")
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = hugeWaveInput,
                    onValueChange = {
                        hugeWaveInput = it
                        saveTimeSettings()
                    },
                    label = { Text("旗帜波延迟") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Text(
                text = if (hasConveyor) "检测到传送带模块，已自动应用 Conveyor 延迟设置" else "未检测到传送带模块，应用普通模式延迟设置",
                fontSize = 12.sp, color = Color.Gray
            )

            HorizontalDivider()
            Text(
                "特殊设置",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1976D2)
            )

            Surface(
                color = Color(0xFFE0EBF8),
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
                        Text("SuppressFlagZombie", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = suppressFlag,
                        onCheckedChange = { isChecked ->
                            suppressFlag = isChecked
                            waveManager.suppressFlagZombie = if (isChecked) true else null
                        }
                    )
                }
            }
            Text(
                "开启后，大波次来袭时不会生成带旗帜的领头僵尸",
                fontSize = 12.sp,
                color = Color.Gray
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
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
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
                                selectedJamCode = code
                                waveManager.levelJam = code
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
                color = Color.Gray
            )

            Spacer(Modifier.height(48.dp))
        }
    }
}