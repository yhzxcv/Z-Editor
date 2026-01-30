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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzLightGreenDark
import com.example.z_editor.ui.theme.PvzLightGreenLight
import com.example.z_editor.ui.theme.PvzPurpleDark
import com.example.z_editor.ui.theme.PvzPurpleLight
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SeedBankPropertiesEP(
    rtid: String,
    rootLevelFile: PvzLevelFile,
    onBack: () -> Unit,
    onRequestPlantSelection: ((List<String>) -> Unit) -> Unit,
    onRequestZombieSelection: ((List<String>) -> Unit) -> Unit,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, SeedBankData::class.java)
    val seedBankDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    val isZombieMode = seedBankDataState.value.zombieMode == true
    val isReversedZombie = seedBankDataState.value.seedPacketType == "UIIZombieSeedPacket"

    val isDark = LocalDarkTheme.current
    val themeColor = if (isZombieMode) if (isDark) PvzPurpleDark else PvzPurpleLight
    else if (isDark) PvzLightGreenDark else PvzLightGreenLight

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
            })
        },
        topBar = {
            CommonEditorTopAppBar(
                title = if (isZombieMode) "种子库 (我是僵尸)" else "种子库设置",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "种子库模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "种子库可以允许玩家选择已有的植物，在庭院模块下可以定义全局阶级且实现全植物可用。"
                )
                HelpSection(
                    title = "黑白名单",
                    body = "白名单为空时不作限制，要注意平行宇宙的植物无法放入白名单。黑名单为额外禁用植物，优先级高于白名单。"
                )
                HelpSection(
                    title = "我是僵尸模式",
                    body = "启用我是僵尸模式后，需要预设关卡的可用僵尸。此时选卡模式强制为预选。如果关卡中同时存在植物卡槽模式和僵尸卡槽模式，需锁定至相同阶级。"
                )
                HelpSection(
                    title = "卡槽占位",
                    body = "非法的代号在卡槽中会空缺。在植物模式下僵尸代号非法，反之亦然，可以用此特点在关卡里拼接两种模式的卡槽。注意要将僵尸卡槽置于前面。"
                )
                HelpSection(
                    title = "进阶玩法",
                    body = "当选择模式是预选时，将选卡模块放在传送带模块前面可以让传送带中文消耗阳光种植，放在后面可以让预选卡种植不消耗阳光。"
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === 第一部分：基础设置 ===
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Yard,
                            null,
                            tint = if (isZombieMode) MaterialTheme.colorScheme.onSurfaceVariant else themeColor
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("基础规则", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(Modifier.height(8.dp))

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    Text("选卡模式", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectionMethodChip(
                            label = "自选 (Chooser)",
                            selected = seedBankDataState.value.selectionMethod == "chooser" && !isZombieMode,
                            enabled = !isZombieMode,
                            onClick = {
                                seedBankDataState.value =
                                    seedBankDataState.value.copy(selectionMethod = "chooser")
                                sync()
                            }
                        )
                        SelectionMethodChip(
                            label = "预选 (Preset)",
                            selected = seedBankDataState.value.selectionMethod == "preset" || isZombieMode,
                            enabled = !isZombieMode,
                            onClick = {
                                seedBankDataState.value =
                                    seedBankDataState.value.copy(selectionMethod = "preset")
                                sync()
                            }
                        )
                    }

                    Text(
                        "选择模式为预选时，无论预选卡片数量多少都会立即进入游戏",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.alpha(if (isZombieMode) 0.5f else 1f)
                    ) {
                        NumberInputInt(
                            color = if (isZombieMode) MaterialTheme.colorScheme.onSurfaceVariant else themeColor,
                            value = seedBankDataState.value.globalLevel ?: 0,
                            onValueChange = { input ->
                                val clamped = input.coerceIn(0, 5)
                                val finalVal = if (clamped == 0) null else clamped
                                seedBankDataState.value =
                                    seedBankDataState.value.copy(globalLevel = finalVal)
                                sync()
                            },
                            label = "植物等级 (0-5)",
                            modifier = Modifier.weight(1f)
                        )

                        NumberInputInt(
                            color = if (isZombieMode) MaterialTheme.colorScheme.onSurfaceVariant else themeColor,
                            value = seedBankDataState.value.overrideSeedSlotsCount ?: 0,
                            onValueChange = { input ->
                                val clamped = input.coerceIn(0, 9)
                                seedBankDataState.value =
                                    seedBankDataState.value.copy(overrideSeedSlotsCount = clamped)
                                sync()
                            },
                            modifier = Modifier.weight(1f),
                            label = "卡槽数量 (0-9)"
                        )
                    }
                    Text(
                        "庭院模式下，对卡槽数量的更改无效，自选模式会锁定8槽",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // === 第二部分：列表编辑器 ===

            if (isZombieMode) {
                ResourceListEditor(
                    title = "可用僵尸列表",
                    description = "我是僵尸模式下供玩家使用的僵尸",
                    items = seedBankDataState.value.presetPlantList,
                    accentColor = themeColor,
                    isZombie = true,
                    onListChanged = { newList ->
                        seedBankDataState.value =
                            seedBankDataState.value.copy(presetPlantList = newList)
                        sync()
                    },
                    onAddRequest = onRequestZombieSelection
                )
            } else {
                // 1. 预设植物
                ResourceListEditor(
                    title = "预选植物 (PresetPlantList)",
                    description = "开局自带的植物",
                    items = seedBankDataState.value.presetPlantList,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    isZombie = false,
                    onListChanged = { newList ->
                        seedBankDataState.value =
                            seedBankDataState.value.copy(presetPlantList = newList)
                        sync()
                    },
                    onAddRequest = onRequestPlantSelection
                )

                // 2. 白名单
                ResourceListEditor(
                    title = "白名单 (WhiteList)",
                    description = "仅允许选择这些植物 (空则不限制)",
                    items = seedBankDataState.value.plantWhiteList,
                    accentColor = MaterialTheme.colorScheme.primary,
                    isZombie = false,
                    onListChanged = { newList ->
                        seedBankDataState.value =
                            seedBankDataState.value.copy(plantWhiteList = newList)
                        sync()
                    },
                    onAddRequest = onRequestPlantSelection
                )

                // 3. 黑名单
                ResourceListEditor(
                    title = "黑名单 (BlackList)",
                    description = "禁止选择这些植物",
                    items = seedBankDataState.value.plantBlackList,
                    accentColor = MaterialTheme.colorScheme.onError,
                    isZombie = false,
                    onListChanged = { newList ->
                        seedBankDataState.value =
                            seedBankDataState.value.copy(plantBlackList = newList)
                        sync()
                    },
                    onAddRequest = onRequestPlantSelection
                )
            }

            if (isZombieMode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Info, null, tint = themeColor)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "只有一部分僵尸为iz适配了卡槽和阳光，在僵尸选择页面里的其它分类里可以找到。",
                                fontSize = 12.sp,
                                color = themeColor,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // === 第三部分：底部开关 ===
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.EmojiPeople,
                                null,
                                tint = if (isDark) PvzPurpleDark else PvzPurpleLight
                            )
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                if (isZombieMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FlipCameraAndroid,
                                    null,
                                    tint = themeColor
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isReversedZombie,
                            onCheckedChange = { checked ->
                                val newData = seedBankDataState.value.copy(
                                    seedPacketType = if (checked) "UIIZombieSeedPacket" else null
                                )
                                seedBankDataState.value = newData
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
            selectedContainerColor = if (enabled) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.2f
            ),
            selectedLabelColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = Color.Transparent,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
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
    isZombie: Boolean,
    onListChanged: (MutableList<String>) -> Unit,
    onAddRequest: ((List<String>) -> Unit) -> Unit
) {

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, color = accentColor)
                    Text(
                        description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    onAddRequest { selectedIds ->
                        val currentList = items.toMutableList()

                        selectedIds.forEach { newId ->
                            val alias =
                                if (isZombie) ZombieRepository.buildZombieAliases(newId) else newId
                            currentList.add(alias)
                        }
                        onListChanged(currentList)
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
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.small
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "列表为空",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEachIndexed { index, itemId ->
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
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface,
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