package com.example.pvz2leveleditor.views.editor.pages.module

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pvz2leveleditor.data.ConveyorBeltData
import com.example.pvz2leveleditor.data.DropDelayConditionData
import com.example.pvz2leveleditor.data.InitialPlantListData
import com.example.pvz2leveleditor.data.LevelDefinitionData
import com.example.pvz2leveleditor.data.repository.PlantRepository
import com.example.pvz2leveleditor.data.repository.PlantTag
import com.example.pvz2leveleditor.data.PvzLevelFile
import com.example.pvz2leveleditor.data.repository.ReferenceRepository
import com.example.pvz2leveleditor.data.RtidParser
import com.example.pvz2leveleditor.data.SpeedConditionData
import com.example.pvz2leveleditor.views.components.AssetImage
import com.example.pvz2leveleditor.views.editor.EditorHelpDialog
import com.example.pvz2leveleditor.views.editor.HelpSection
import com.example.pvz2leveleditor.views.editor.NumberInputDouble
import com.example.pvz2leveleditor.views.editor.NumberInputInt
import com.google.gson.Gson

private val gson = Gson()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConveyorSeedBankPropertiesEP(
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
            rootLevelFile.objects.find { it.aliases?.contains(alias) == true }?.objClass == "ConveyorBeltProperties" ||
                    ReferenceRepository.getObjClass(alias) == "ConveyorBeltProperties"
        }
    }

    val currentAlias = if (targetModuleRtid != null) {
        RtidParser.parse(targetModuleRtid)?.alias ?: "ConveyorBelt"
    } else {
        "ConveyorBelt"
    }

    val conveyorDataState = remember {
        val localObj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }

        val data = if (localObj != null) {
            try {
                val parsed = gson.fromJson(localObj.objData, ConveyorBeltData::class.java)
                if (parsed.speedConditions.isEmpty() && parsed.dropDelayConditions.isEmpty()) {
                    createDefaultConveyorData()
                } else {
                    parsed
                }
            } catch (_: Exception) {
                createDefaultConveyorData()
            }
        } else {
            createDefaultConveyorData()
        }
        mutableStateOf(data)
    }

    fun sync() {
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }?.let {
            it.objData = gson.toJsonTree(conveyorDataState.value)
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
                title = { Text("传送带配置") },
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
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White // 确保图标是白色的
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "传送带模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = Color(0xFF1976D2) // 使用与TopBar一致的主题色
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "传送带模式会按照设定的权重随机生成卡片。需要配置植物池以及刷新延迟数据。"
                )
                HelpSection(
                    title = "植物池与权重",
                    body = "某种植物出现的概率为这种植物的权重占所有植物权重的比例。可以通过设置两个阈值动态调整植物的权重。"
                )
                HelpSection(
                    title = "刷新延迟",
                    body = "控制卡片生成的间隔时间。可以根据积压的植物数量调整传送间隔。通常植物积压越多，生成越慢。"
                )
                HelpSection(
                    title = "传输速度",
                    body = "控制卡片在传送带上移动的物理速度，标准速度为 100。可以根据积压数量进行分段变速。"
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
            // === 区域 1: 传送带植物列表 ===
            ConveyorPlantListEditor(
                items = conveyorDataState.value.initialPlantList,
                onListChanged = { newList ->
                    conveyorDataState.value =
                        conveyorDataState.value.copy(initialPlantList = newList)
                    sync()
                },
                onRequestPlantSelection = onRequestPlantSelection
            )

            // === 区域 2: 掉落延迟控制 ===
            ConveyorConditionEditor(
                title = "刷新延迟 (DropDelayConditions)",
                subtitle = "单位: 秒 (卡包越多，生成越慢)",
                headers = "卡包数 (MaxPackets)" to "延迟秒 (Delay)",
                items = conveyorDataState.value.dropDelayConditions,
                extractMaxPackets = { it.maxPacketsDelay },
                onAdd = {
                    var nextMax =
                        (conveyorDataState.value.dropDelayConditions.maxOfOrNull { it.maxPacketsDelay }
                            ?: 0) + 2
                    if (nextMax >= 9) nextMax = 9
                    conveyorDataState.value.dropDelayConditions.add(
                        DropDelayConditionData(
                            delay = 6,
                            maxPacketsDelay = nextMax
                        )
                    )
                    sync()
                },
                onRemove = { index ->
                    conveyorDataState.value.dropDelayConditions.removeAt(index)
                    sync()
                },
                onValueChange = { sync() },
                contentRow = { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputInt(
                            value = item.maxPacketsDelay,
                            onValueChange = { item.maxPacketsDelay = it },
                            label = "阈值",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputInt(
                            value = item.delay,
                            onValueChange = { item.delay = it },
                            label = "延迟",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            )

            // === 区域 3: 速度控制 ===
            ConveyorConditionEditor(
                title = "传输速度 (SpeedConditions)",
                subtitle = "标准值为100，值越大越快",
                headers = "卡包数 (MaxPackets)" to "速度值 (Speed)",
                items = conveyorDataState.value.speedConditions,
                extractMaxPackets = { it.maxPacketsSpeed },
                onAdd = {
                    // 添加时自动递增 MaxPackets
                    var nextMax =
                        (conveyorDataState.value.speedConditions.maxOfOrNull { it.maxPacketsSpeed }
                            ?: 0) + 2
                    if (nextMax >= 9) nextMax = 9
                    conveyorDataState.value.speedConditions.add(
                        SpeedConditionData(
                            speed = 100,
                            maxPacketsSpeed = nextMax
                        )
                    )
                    sync()
                },
                onRemove = { index ->
                    conveyorDataState.value.speedConditions.removeAt(index)
                    sync()
                },
                onValueChange = { sync() },
                contentRow = { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputInt(
                            value = item.maxPacketsSpeed,
                            onValueChange = { item.maxPacketsSpeed = it },
                            label = "阈值",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputInt(
                            value = item.speed,
                            onValueChange = { item.speed = it },
                            label = "速度",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            )

            Spacer(Modifier.height(32.dp))

        }
    }
}

// -----------------------------------------------------------
// 辅助函数：创建默认数据 (严格按照要求)
// -----------------------------------------------------------
private fun createDefaultConveyorData(): ConveyorBeltData {
    return ConveyorBeltData(
        initialPlantList = mutableListOf(), // 可为空

        speedConditions = mutableListOf(
            SpeedConditionData(maxPacketsSpeed = 0, speed = 100) // 初始值
        ),

        dropDelayConditions = mutableListOf(
            DropDelayConditionData(maxPacketsDelay = 0, delay = 3),
            DropDelayConditionData(maxPacketsDelay = 2, delay = 6),
            DropDelayConditionData(maxPacketsDelay = 4, delay = 8),
            DropDelayConditionData(maxPacketsDelay = 9, delay = 10)
        )
    )
}

// -----------------------------------------------------------
// 组件：植物列表编辑器 (修复删除闪退 + 集成选择器)
// -----------------------------------------------------------
@Composable
fun ConveyorPlantListEditor(
    items: MutableList<InitialPlantListData>,
    onListChanged: (MutableList<InitialPlantListData>) -> Unit,
    onRequestPlantSelection: ((String) -> Unit) -> Unit
) {
    var showEditDialog by remember { mutableStateOf<InitialPlantListData?>(null) }

    val listKey = remember { mutableIntStateOf(0) }

    if (showEditDialog != null) {
        PlantDetailDialog(
            data = showEditDialog!!,
            onDismiss = { showEditDialog = null },
            onConfirm = {
                listKey.intValue++ // 强制列表重组
                onListChanged(items)
                showEditDialog = null
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalShipping, null, tint = Color(0xFF1976D2))
                Spacer(Modifier.width(12.dp))
                Text(
                    "传送带植物池",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1976D2)
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    onRequestPlantSelection { selectedId ->
                        val newPlant = InitialPlantListData(plantType = selectedId)
                        items.add(newPlant)
                        listKey.intValue++
                        onListChanged(items)
                    }
                }) {
                    Icon(Icons.Default.Add, "添加", tint = Color(0xFF1976D2))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            if (items.isEmpty()) {
                Text(
                    "暂无植物，请添加",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                key(listKey.intValue) {
                    items.forEachIndexed { index, plant ->
                        PlantRow(
                            plant = plant,
                            onEdit = { showEditDialog = plant },
                            onDelete = {
                                val newList = items.toMutableList()
                                newList.removeAt(index)
                                onListChanged(newList)
                                listKey.intValue++
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlantRow(
    plant: InitialPlantListData,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .clickable { onEdit() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val info = remember(plant.plantType) { PlantRepository.search(plant.plantType, PlantTag.All).firstOrNull() }
        val displayName = PlantRepository.getName(plant.plantType)

        val placeholderContent = @Composable {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFBDBDBD), CircleShape)
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }
        AssetImage(
            path = if (info?.icon != null) "images/plants/${info.icon}" else null,
            contentDescription = displayName,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color.LightGray, CircleShape),
            filterQuality = FilterQuality.Medium,
            placeholder = placeholderContent
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val levelText = if (plant.iLevel == null) "随账号" else "${plant.iLevel}"
                Text(
                    "权重: ${plant.weight}  等级: $levelText",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.width(8.dp))
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        }
    }
}

// -----------------------------------------------------------
// 组件：植物详细参数编辑弹窗 (简化版：编辑时隐藏ID选择)
// -----------------------------------------------------------
@Composable
fun PlantDetailDialog(
    data: InitialPlantListData,
    onDismiss: () -> Unit,
    onConfirm: (InitialPlantListData) -> Unit
) {
    // 如果是编辑模式，直接使用原数据ID；如果是新建，使用临时状态
    var tempType by remember { mutableStateOf(data.plantType) }

    var tempWeight by remember { mutableIntStateOf(data.weight) }
    var tempLevel by remember { mutableIntStateOf(data.iLevel ?: 0) }

    var tempMaxCount by remember { mutableIntStateOf(data.maxCount) }
    var tempMaxWeightFactor by remember { mutableDoubleStateOf(data.maxWeightFactor) }

    var tempMinCount by remember { mutableIntStateOf(data.minCount) }
    var tempMinWeightFactor by remember { mutableDoubleStateOf(data.minWeightFactor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "编辑: ${PlantRepository.getName(data.plantType)}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputInt(tempWeight, { tempWeight = it }, "初始权重", Modifier.weight(1f))

                    NumberInputInt(
                        value = tempLevel,
                        onValueChange = { input ->
                            tempLevel = input.coerceIn(0, 5)
                        },
                        label = "植物等级",
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    "等级输入 0 表示等级跟随玩家账号",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp)
                )

                HorizontalDivider(thickness = 0.5.dp)

                Column {
                    Text(
                        "上限控制 (Max Limits)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputInt(
                            tempMaxCount,
                            { tempMaxCount = it },
                            "最大数量阈值",
                            Modifier.weight(1f)
                        )
                        NumberInputDouble(
                            tempMaxWeightFactor,
                            { tempMaxWeightFactor = it },
                            "达标后权重倍率",
                            Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = if (tempMaxWeightFactor == 0.0) "达到 $tempMaxCount 株后停止刷新"
                        else if (tempMaxCount > 0) "达到 $tempMaxCount 株后权重变为 ${(tempWeight * tempMaxWeightFactor).toInt()}"
                        else "",
                        fontSize = 12.sp,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Column {
                    Text(
                        "下限控制 (Min Limits)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputInt(
                            tempMinCount,
                            { tempMinCount = it },
                            "最小数量阈值",
                            Modifier.weight(1f)
                        )
                        NumberInputDouble(
                            tempMinWeightFactor,
                            { tempMinWeightFactor = it },
                            "未达标权重倍率",
                            Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = if (tempMinCount > 0) "不满 $tempMinCount 株前权重变为 ${(tempWeight * tempMinWeightFactor).toInt()}"
                        else "",
                        fontSize = 12.sp,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                data.plantType = tempType
                data.weight = tempWeight
                data.maxCount = tempMaxCount
                data.maxWeightFactor = tempMaxWeightFactor
                data.minCount = tempMinCount
                data.minWeightFactor = tempMinWeightFactor
                data.iLevel = if (tempLevel <= 0) null else tempLevel
                onConfirm(data)
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// -----------------------------------------------------------
// 组件：通用条件列表编辑器 (包含 MaxPackets=0 保护)
// -----------------------------------------------------------
@Composable
fun <T> ConveyorConditionEditor(
    title: String,
    subtitle: String,
    headers: Pair<String, String>,
    items: MutableList<T>,
    extractMaxPackets: (T) -> Int,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onValueChange: () -> Unit,
    contentRow: @Composable (T) -> Unit
) {
    val refreshKey = remember { mutableIntStateOf(0) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(subtitle, fontSize = 11.sp, color = Color.Gray)
                }
                IconButton(onClick = {
                    onAdd()
                    refreshKey.intValue++
                }) {
                    Icon(Icons.Default.Add, "添加", tint = Color(0xFF1976D2))
                }
            }

            Spacer(Modifier.height(8.dp))

            // 表头
            Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    headers.first,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    headers.second,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(32.dp))
            }

            Spacer(Modifier.height(4.dp))

            key(refreshKey.intValue) {
                items.forEachIndexed { index, item ->
                    val isBaseCondition = extractMaxPackets(item) == 0

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            contentRow(item)
                        }

                        if (isBaseCondition) {
                            Icon(
                                Icons.Default.Lock,
                                "基础项不可删",
                                tint = Color.LightGray,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(6.dp)
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    onRemove(index)
                                    refreshKey.intValue++
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    LaunchedEffect(item) { onValueChange() }
                }
            }
        }
    }
}