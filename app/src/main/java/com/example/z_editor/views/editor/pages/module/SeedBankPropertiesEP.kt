package com.example.z_editor.views.editor.pages.module

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.SeedBankData
import com.example.z_editor.data.repository.PlantRepository
import com.example.z_editor.data.repository.ZombieRepository
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import com.google.gson.Gson

private val gson = Gson()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SeedBankPropertiesEP(
    rtid: String,
    rootLevelFile: PvzLevelFile,
    onBack: () -> Unit,
    onRequestPlantSelection: ((String) -> Unit) -> Unit,
    onRequestZombieSelection: ((String) -> Unit) -> Unit,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""

    val seedBankDataState = remember {
        val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        val data = try {
            gson.fromJson(obj?.objData, SeedBankData::class.java)
        } catch (_: Exception) {
            SeedBankData()
        }
        mutableStateOf(data)
    }

    val isZombieMode = seedBankDataState.value.zombieMode == true
    val isReversedZombie = seedBankDataState.value.seedPacketType == "UIIZombieSeedPacket"

    fun syncData() {
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }?.let {
            it.objData = gson.toJsonTree(seedBankDataState.value)
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
                title = {
                    Text(
                        if (isZombieMode) "种子库 (我是僵尸)" else "种子库配置",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
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
                    containerColor = if (isZombieMode) Color(0xFF654B80) else Color(0xFF388E3C), // 僵尸模式变色
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "种子库模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = if (isZombieMode) Color(0xFF654B80) else Color(0xFF388E3C)
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "种子库可以允许玩家选择已有的植物，在庭院模块下通过定义全局阶级可以实现全植物可用。"
                )
                HelpSection(
                    title = "预选植物",
                    body = "选择方式为自选时，开始游戏前会让玩家在种子库补齐植物到卡槽总数。选择方式为预选时，玩家会带着预选设置页的植物直接开始游戏。"
                )
                HelpSection(
                    title = "黑白名单",
                    body = "白名单为空时不作限制，若白名单有植物则只能从白名单内选择。黑名单为额外禁用植物，优先级高于白名单。"
                )
                HelpSection(
                    title = "进阶玩法",
                    body = "当选择模式是preset时，将选卡模块放在传送带模块前面可以让传送带中文消耗阳光种植，放在后面可以让预选卡种植不消耗阳光。"
                )
                HelpSection(
                    title = "我是僵尸模式",
                    body = "启用我是僵尸模式后，种子库将转变为僵尸选择器。此时选卡模式强制为 Preset。如果关卡中同时存在植物卡槽模式和僵尸卡槽模式，需锁定至相同阶级。"
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
            // === 第一部分：基础设置 ===
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Yard,
                            null,
                            tint = if (isZombieMode) Color.Gray else Color(0xFF388E3C)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("基础规则", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    HorizontalDivider(thickness = 0.5.dp)

                    Text("选卡模式", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectionMethodChip(
                            label = "自选 (Chooser)",
                            selected = seedBankDataState.value.selectionMethod == "chooser" && !isZombieMode,
                            enabled = !isZombieMode,
                            onClick = {
                                seedBankDataState.value =
                                    seedBankDataState.value.copy(selectionMethod = "chooser")
                                syncData()
                            }
                        )
                        SelectionMethodChip(
                            label = "锁定 (Preset)",
                            selected = seedBankDataState.value.selectionMethod == "preset" || isZombieMode,
                            enabled = !isZombieMode,
                            onClick = {
                                seedBankDataState.value =
                                    seedBankDataState.value.copy(selectionMethod = "preset")
                                syncData()
                            }
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.alpha(if (isZombieMode) 0.5f else 1f)
                    ) {
                        NumberInputInt(
                            color = if (isZombieMode) Color.Gray else Color(0xFF388E3C),
                            value = seedBankDataState.value.globalLevel ?: 0,
                            onValueChange = { input ->
                                val clamped = input.coerceIn(0, 5)
                                val finalVal = if (clamped == 0) null else clamped
                                seedBankDataState.value =
                                    seedBankDataState.value.copy(globalLevel = finalVal)
                                syncData()
                            },
                            label = "植物等级 (0-5)",
                            modifier = Modifier.weight(1f)
                        )

                        NumberInputInt(
                            color = if (isZombieMode) Color.Gray else Color(0xFF388E3C),
                            value = seedBankDataState.value.overrideSeedSlotsCount ?: 0,
                            onValueChange = { input ->
                                val clamped = input.coerceIn(0, 9)
                                seedBankDataState.value =
                                    seedBankDataState.value.copy(overrideSeedSlotsCount = clamped)
                                syncData()
                            },
                            modifier = Modifier.weight(1f),
                            label = "卡槽数量 (0-9)"
                        )
                    }
                }
            }

            // === 第二部分：列表编辑器 ===

            if (isZombieMode) {
                ResourceListEditor(
                    title = "可用僵尸列表",
                    description = "我是僵尸模式下供玩家使用的僵尸",
                    items = seedBankDataState.value.presetPlantList,
                    accentColor = Color(0xFF654B80),
                    isZombie = true,
                    onListChanged = { newList ->
                        seedBankDataState.value =
                            seedBankDataState.value.copy(presetPlantList = newList)
                        syncData()
                    },
                    onAddRequest = onRequestZombieSelection
                )
            } else {
                // 1. 预设植物
                ResourceListEditor(
                    title = "预设植物 (PresetPlantList)",
                    description = "开局自带的植物",
                    items = seedBankDataState.value.presetPlantList,
                    accentColor = Color(0xFF1976D2),
                    isZombie = false,
                    onListChanged = { newList ->
                        seedBankDataState.value =
                            seedBankDataState.value.copy(presetPlantList = newList)
                        syncData()
                    },
                    onAddRequest = onRequestPlantSelection
                )

                // 2. 白名单
                ResourceListEditor(
                    title = "白名单 (WhiteList)",
                    description = "仅允许选择这些植物 (空则不限制)",
                    items = seedBankDataState.value.plantWhiteList,
                    accentColor = Color(0xFF388E3C), // 绿
                    isZombie = false,
                    onListChanged = { newList ->
                        seedBankDataState.value =
                            seedBankDataState.value.copy(plantWhiteList = newList)
                        syncData()
                    },
                    onAddRequest = onRequestPlantSelection
                )

                // 3. 黑名单
                ResourceListEditor(
                    title = "黑名单 (BlackList)",
                    description = "禁止选择这些植物",
                    items = seedBankDataState.value.plantBlackList,
                    accentColor = Color(0xFFD32F2F), // 红
                    isZombie = false,
                    onListChanged = { newList ->
                        seedBankDataState.value =
                            seedBankDataState.value.copy(plantBlackList = newList)
                        syncData()
                    },
                    onAddRequest = onRequestPlantSelection
                )
            }

            if (isZombieMode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Info, null, tint = Color(0xFF654B80))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "只有一部分僵尸为iz适配了卡槽和阳光，在僵尸选择页面里的其它分类里可以找到。",
                                fontSize = 12.sp,
                                color = Color(0xFF654B80),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // === 第三部分：底部开关 ===
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiPeople, null, tint = Color(0xFF654B80))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "我是僵尸模式",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "启用后将转变为放置僵尸的玩法，选卡方式将被锁定",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isZombieMode,
                        onCheckedChange = { checked ->
                            var newData = seedBankDataState.value.copy(zombieMode = checked)
                            newData = if (checked) {
                                newData.copy(selectionMethod = "preset")
                            } else {
                                newData.copy(seedPacketType = null)
                            }
                            seedBankDataState.value = newData
                            syncData()
                        }
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                if (isZombieMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FlipCameraAndroid,
                                    null,
                                    tint = Color(0xFF654B80)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "反转僵尸阵营",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "启用后放置的僵尸将变为植物阵营，可用于ZVZ",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = isReversedZombie,
                            onCheckedChange = { checked ->
                                val newData = seedBankDataState.value.copy(
                                    seedPacketType = if (checked) "UIIZombieSeedPacket" else null
                                )
                                seedBankDataState.value = newData
                                syncData()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}


// ==========================================
// 辅助组件 (更新后)
// ==========================================

@Composable
fun SelectionMethodChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true, // 新增参数
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = if (enabled) Color(0xFFE8F5E9) else Color.LightGray.copy(alpha = 0.3f),
            selectedLabelColor = if (enabled) Color(0xFF2E7D32) else Color.Gray,
            disabledContainerColor = Color.Transparent,
            disabledLabelColor = Color.Gray
        )
    )
}

/**
 * 通用资源列表编辑器 (原 PlantListEditor)
 * 支持显示植物或僵尸名称
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResourceListEditor(
    title: String,
    description: String,
    items: MutableList<String>,
    accentColor: Color,
    isZombie: Boolean, // 新增参数：区分是植物还是僵尸
    onListChanged: (MutableList<String>) -> Unit,
    onAddRequest: ((String) -> Unit) -> Unit
) {

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, color = accentColor)
                    Text(description, fontSize = 11.sp, color = Color.Gray)
                }
                IconButton(onClick = {
                    onAddRequest { selectedId ->
                        val newList = items.toMutableList().apply { add(selectedId) }
                        onListChanged(newList)
                    }
                }) {
                    Icon(Icons.Default.Add, "添加", tint = accentColor)
                }
            }
            Spacer(Modifier.height(8.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), MaterialTheme.shapes.small)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("列表为空", color = Color.LightGray, fontSize = 12.sp)
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEachIndexed { index, itemId ->
                        // 根据 isZombie 决定解析方式
                        val displayName = if (isZombie) {
                            ZombieRepository.getName(itemId)
                        } else {
                            PlantRepository.getName(itemId)
                        }

                        InputChip(
                            selected = true,
                            onClick = {},
                            label = { Text(displayName) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "删除",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            val newList =
                                                items.toMutableList().apply { removeAt(index) }
                                            onListChanged(newList)
                                        }
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.1f),
                                selectedLabelColor = Color.Black,
                                selectedTrailingIconColor = accentColor
                            ),
                            border = InputChipDefaults.inputChipBorder(
                                enabled = true,
                                selected = true,
                                borderColor = accentColor.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }
    }
}