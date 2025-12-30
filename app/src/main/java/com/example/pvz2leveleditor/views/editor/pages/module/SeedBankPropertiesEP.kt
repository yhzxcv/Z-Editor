package com.example.pvz2leveleditor.views.editor.pages.module

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pvz2leveleditor.data.LevelDefinitionData
import com.example.pvz2leveleditor.data.Repository.PlantRepository
import com.example.pvz2leveleditor.data.PvzLevelFile
import com.example.pvz2leveleditor.data.Repository.ReferenceRepository
import com.example.pvz2leveleditor.data.RtidParser
import com.example.pvz2leveleditor.data.SeedBankData
import com.example.pvz2leveleditor.views.editor.EditorHelpDialog
import com.example.pvz2leveleditor.views.editor.HelpSection
import com.example.pvz2leveleditor.views.editor.NumberInputInt
import com.google.gson.Gson

private val gson = Gson()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SeedBankPropertiesEP(
    rootLevelFile: PvzLevelFile,
    levelDef: LevelDefinitionData,
    onBack: () -> Unit,
    onRequestPlantSelection: ((String) -> Unit) -> Unit,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    val targetModuleRtid = remember(levelDef.modules) {
        levelDef.modules.find { rtid ->
            val info = RtidParser.parse(rtid)
            val alias = info?.alias ?: ""
            rootLevelFile.objects.find { it.aliases?.contains(alias) == true }?.objClass == "SeedBankProperties" ||
                    ReferenceRepository.getObjClass(alias) == "SeedBankProperties"
        }
    }

    val currentAlias = if (targetModuleRtid != null) {
        RtidParser.parse(targetModuleRtid)?.alias ?: "SeedBank"
    } else {
        "SeedBank"
    }

    val seedBankDataState = remember {
        val localObj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        val data = if (localObj != null) {
            try {
                gson.fromJson(localObj.objData, SeedBankData::class.java)
            } catch (e: Exception) {
                SeedBankData()
            }
        } else {
            SeedBankData()
        }
        mutableStateOf(data)
    }

    fun syncData() {
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }?.let {
            it.objData = gson.toJsonTree(seedBankDataState.value)
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus() // 点击空白处清除焦点
            })
        },
        topBar = {
            TopAppBar(
                title = { Text("种子库配置") },
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
                title = "种子库模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = Color(0xFF388E3C) // 使用与TopBar一致的主题色
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
                        Icon(Icons.Default.Yard, null, tint = Color(0xFF388E3C))
                        Spacer(Modifier.width(12.dp))
                        Text("基础规则", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    HorizontalDivider(thickness = 0.5.dp)

                    // 选卡模式 (SelectionMethod)
                    Text("选卡模式", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectionMethodChip(
                            label = "自选 (Chooser)",
                            selected = seedBankDataState.value.selectionMethod == "chooser",
                            onClick = {
                                seedBankDataState.value =
                                    seedBankDataState.value.copy(selectionMethod = "chooser")
                                syncData()
                            }
                        )
                        SelectionMethodChip(
                            label = "锁定 (Preset)",
                            selected = seedBankDataState.value.selectionMethod == "preset",
                            onClick = {
                                seedBankDataState.value =
                                    seedBankDataState.value.copy(selectionMethod = "preset")
                                syncData()
                            }
                        )
                    }

                    // 数值设置 (Level & Slots)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 植物等级 (0-5)，0 表示跟随账号(null)
                        NumberInputInt(
                            value = seedBankDataState.value.globalLevel ?: 0,
                            onValueChange = { input ->
                                val clamped = input.coerceIn(0, 5)
                                val finalVal = if (clamped == 0) null else clamped

                                seedBankDataState.value = seedBankDataState.value.copy(globalLevel = finalVal)
                                syncData()
                            },
                            label = "植物等级 (0-5)",
                            modifier = Modifier.weight(1f)
                        )

                        // 卡槽数量 (0-9)
                        NumberInputInt(
                            value = seedBankDataState.value.overrideSeedSlotsCount ?: 0,
                            onValueChange = { input ->
                                val clamped = input.coerceIn(0, 9)

                                seedBankDataState.value = seedBankDataState.value.copy(overrideSeedSlotsCount = clamped)
                                syncData()
                            },
                            label = "卡槽数量 (0-9)",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = "等级输入 0 表示等级跟随玩家账号，庭院模块启用后，对卡槽数量的修改不生效",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 20.sp
                    )
                }
            }
            // === 第二部分：列表编辑器 ===

            // 1. 预设植物
            PlantListEditor(
                title = "预设植物 (PresetPlantList)",
                description = "开局自带的植物",
                items = seedBankDataState.value.presetPlantList,
                accentColor = Color(0xFF1976D2),
                onListChanged = { newList ->
                    seedBankDataState.value =
                        seedBankDataState.value.copy(presetPlantList = newList)
                    syncData()
                },
                onAddRequest = onRequestPlantSelection
            )

            // 2. 白名单
            PlantListEditor(
                title = "白名单 (WhiteList)",
                description = "仅允许选择这些植物 (空则不限制)",
                items = seedBankDataState.value.plantWhiteList,
                accentColor = Color(0xFF388E3C), // 绿
                onListChanged = { newList ->
                    seedBankDataState.value = seedBankDataState.value.copy(plantWhiteList = newList)
                    syncData()
                },
                onAddRequest = onRequestPlantSelection
            )

            // 3. 黑名单
            PlantListEditor(
                title = "黑名单 (BlackList)",
                description = "禁止选择这些植物",
                items = seedBankDataState.value.plantBlackList,
                accentColor = Color(0xFFD32F2F), // 红
                onListChanged = { newList ->
                    seedBankDataState.value = seedBankDataState.value.copy(plantBlackList = newList)
                    syncData()
                },
                onAddRequest = onRequestPlantSelection
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}


// ==========================================
// 辅助组件
// ==========================================

@Composable
fun SelectionMethodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFE8F5E9),
            selectedLabelColor = Color(0xFF2E7D32)
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlantListEditor(
    title: String,
    description: String,
    items: MutableList<String>,
    accentColor: Color,
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
                    items.forEachIndexed { index, plantId ->
                        InputChip(
                            selected = true,
                            onClick = {},
                            label = { Text(PlantRepository.getName(plantId)) },
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