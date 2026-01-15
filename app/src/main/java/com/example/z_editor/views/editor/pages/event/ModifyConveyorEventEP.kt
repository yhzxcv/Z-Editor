package com.example.z_editor.views.editor.pages.event

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.ModifyConveyorPlantData
import com.example.z_editor.data.ModifyConveyorRemoveData
import com.example.z_editor.data.ModifyConveyorWaveActionData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.repository.PlantRepository
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputDouble
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModifyConveyorEventEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    onRequestPlantSelection: ((String) -> Unit) -> Unit,
    scrollState: ScrollState
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val hasConveyorModule = remember(rootLevelFile) {
        rootLevelFile.objects.any { it.objClass == "ConveyorSeedBankProperties" }
    }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, ModifyConveyorWaveActionData::class.java)
    val actionDataState = syncManager.dataState

    fun sync(newData: ModifyConveyorWaveActionData) {
        actionDataState.value = newData
        syncManager.sync()
    }

    fun wrapRtid(plantId: String): String = "RTID($plantId@PlantTypes)"
    fun unwrapRtid(rtidStr: String): String = RtidParser.parse(rtidStr)?.alias ?: rtidStr

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "编辑 $currentAlias",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "事件类型：传送带变更",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
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
                    containerColor = Color(0xFF4AC380),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "传送带事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = Color(0xFF4AC380)
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "这个事件可以在游戏运行过程中更改传送带的配置情况，具体参数和传送带模块基本一致，请确保关卡中已经加入了传送带模块。"
                )
                HelpSection(
                    title = "添加植物",
                    body = "可以把新植物加入传送带。如果传送带上已经有该植物，会把之前的数据覆盖掉。"
                )
                HelpSection(
                    title = "移除植物",
                    body = "移除植物在庭院模板下不生效，需要通过将植物权重强行设置为0来替代此效果。"
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasConveyorModule) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Red
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "模块缺失警告",
                                fontWeight = FontWeight.Bold,
                                color = Color.Red,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "关卡未检测到传送带模块，此事件在游戏中可能无法生效，甚至导致闪退",
                                fontSize = 14.sp,
                                color = Color(0xFFC62828),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // === 区域 1: 添加植物列表 (Add) ===
            ModifyConveyorList(
                title = "添加植物 (Add List)",
                icon = Icons.Default.AddCircleOutline,
                titleColor = Color(0xFF2E7D32),
                items = actionDataState.value.addList,
                onListChanged = { newList ->
                    sync(actionDataState.value.copy(addList = newList))
                },
                onRequestPlantSelection = { callback ->
                    onRequestPlantSelection { id -> callback(wrapRtid(id)) }
                },
                unwrapRtid = ::unwrapRtid
            )

            // === 区域 2: 移除植物列表 (Remove) ===
            ModifyConveyorRemoveList(
                title = "移除植物 (Remove List)",
                icon = Icons.Default.RemoveCircleOutline,
                titleColor = Color(0xFFC62828),
                items = actionDataState.value.removeList,
                onListChanged = { newList ->
                    sync(actionDataState.value.copy(removeList = newList))
                },
                onRequestPlantSelection = { callback ->
                    onRequestPlantSelection { id -> callback(wrapRtid(id)) }
                },
                unwrapRtid = ::unwrapRtid
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F6E8)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Info, null, tint = Color(0xFF4AC380))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "在庭院模块下，移除植物事件不生效，可以改用添加权重为0的同种植物覆盖定义。",
                            fontSize = 12.sp,
                            color = Color(0xFF4AC380),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// -----------------------------------------------------------
// 组件：修改列表编辑器 (模仿 ConveyorPlantListEditor)
// -----------------------------------------------------------
@Composable
fun ModifyConveyorList(
    title: String,
    icon: ImageVector,
    titleColor: Color,
    items: MutableList<ModifyConveyorPlantData>,
    onListChanged: (MutableList<ModifyConveyorPlantData>) -> Unit,
    onRequestPlantSelection: ((String) -> Unit) -> Unit,
    unwrapRtid: (String) -> String
) {
    var showEditDialog by remember { mutableStateOf<ModifyConveyorPlantData?>(null) }
    // 强制刷新 Key
    val listKey = remember { mutableIntStateOf(0) }

    if (showEditDialog != null) {
        ModifyConveyorPlantDialog(
            data = showEditDialog!!,
            onDismiss = { showEditDialog = null },
            onConfirm = {
                listKey.intValue++
                onListChanged(items)
                showEditDialog = null
            },
            unwrapRtid = unwrapRtid
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题栏
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = titleColor)
                Spacer(Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = titleColor)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    onRequestPlantSelection { rtidWithWrapper ->
                        val newPlant = ModifyConveyorPlantData(type = rtidWithWrapper, weight = 100)
                        items.add(newPlant)
                        listKey.intValue++
                        onListChanged(items)
                    }
                }) {
                    Icon(Icons.Default.Add, "添加", tint = titleColor)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            if (items.isEmpty()) {
                Text(
                    "列表为空",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                key(listKey.intValue) {
                    items.forEachIndexed { index, plant ->
                        ModifyConveyorPlantRow(
                            plant = plant,
                            unwrapRtid = unwrapRtid,
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

// -----------------------------------------------------------
// 组件：单行植物显示 (解析 RTID 后显示图片)
// -----------------------------------------------------------
@Composable
fun ModifyConveyorPlantRow(
    plant: ModifyConveyorPlantData,
    unwrapRtid: (String) -> String,
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
        val realId = unwrapRtid(plant.type)
        val info = remember(realId) {
            PlantRepository.getPlantInfoById(realId)
        }
        val displayName = PlantRepository.getName(realId)

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
                Text(
                    "权重: ${plant.weight}  等级: ${plant.iLevel ?: "随账号"}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        }
    }
}

// -----------------------------------------------------------
// 组件：植物参数编辑弹窗
// -----------------------------------------------------------
@Composable
fun ModifyConveyorPlantDialog(
    data: ModifyConveyorPlantData,
    onDismiss: () -> Unit,
    onConfirm: (ModifyConveyorPlantData) -> Unit,
    unwrapRtid: (String) -> String
) {
    var tempWeight by remember { mutableIntStateOf(data.weight) }

    var tempLevel by remember { mutableStateOf(data.iLevel) }

    var tempMaxCount by remember { mutableIntStateOf(data.maxCount) }
    var tempMaxWeightFactor by remember { mutableDoubleStateOf(data.maxWeightFactor) }
    var tempMinCount by remember { mutableIntStateOf(data.minCount) }
    var tempMinWeightFactor by remember { mutableDoubleStateOf(data.minWeightFactor) }

    val plantName = PlantRepository.getName(unwrapRtid(data.type))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑: $plantName", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputInt(
                        value = tempWeight,
                        onValueChange = { tempWeight = it },
                        label = "变更后权重",
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF1976D2),
                    )

                    NumberInputInt(
                        value = tempLevel ?: 0,
                        onValueChange = { input ->
                            val clamped = input.coerceIn(0, 5)
                            tempLevel = if (clamped == 0) null else clamped
                        },
                        label = "植物等级",
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF1976D2)
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
                            value = tempMaxCount,
                            onValueChange = { tempMaxCount = it },
                            label = "最大数量",
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF1976D2)
                        )
                        NumberInputDouble(
                            value = tempMaxWeightFactor,
                            onValueChange = { tempMaxWeightFactor = it },
                            label = "达标权重倍率",
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF1976D2)
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
                            value = tempMinCount,
                            onValueChange = { tempMinCount = it },
                            label = "最小数量",
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF1976D2)
                        )
                        NumberInputDouble(
                            value = tempMinWeightFactor,
                            onValueChange = { tempMinWeightFactor = it },
                            label = "未达标权重倍率",
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF1976D2)
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
                // 回写数据
                data.weight = tempWeight
                data.maxCount = tempMaxCount
                data.maxWeightFactor = tempMaxWeightFactor
                data.minCount = tempMinCount
                data.minWeightFactor = tempMinWeightFactor
                data.iLevel = tempLevel
                onConfirm(data)
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun ModifyConveyorRemoveList(
    title: String,
    icon: ImageVector,
    titleColor: Color,
    items: MutableList<ModifyConveyorRemoveData>,
    onListChanged: (MutableList<ModifyConveyorRemoveData>) -> Unit,
    onRequestPlantSelection: ((String) -> Unit) -> Unit,
    unwrapRtid: (String) -> String
) {
    val listKey = remember { mutableIntStateOf(0) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = titleColor)
                Spacer(Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = titleColor)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    onRequestPlantSelection { rtidWithWrapper ->
                        val newRemoveItem = ModifyConveyorRemoveData(type = rtidWithWrapper)
                        items.add(newRemoveItem)
                        listKey.intValue++
                        onListChanged(items)
                    }
                }) {
                    Icon(Icons.Default.Add, "添加", tint = titleColor)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            if (items.isEmpty()) {
                Text(
                    "列表为空",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                key(listKey.intValue) {
                    items.forEachIndexed { index, item ->
                        ModifyConveyorRemoveRow(
                            item = item,
                            unwrapRtid = unwrapRtid,
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

// -----------------------------------------------------------
// 新增组件：移除列表单行展示 (无参数显示)
// -----------------------------------------------------------
@Composable
fun ModifyConveyorRemoveRow(
    item: ModifyConveyorRemoveData,
    unwrapRtid: (String) -> String,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val realId = unwrapRtid(item.type)
        val info = remember(realId) {
            PlantRepository.getPlantInfoById(realId)
        }
        val displayName = PlantRepository.getName(realId)

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
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                text = realId,
                fontSize = 11.sp,
                color = Color.Gray,
                maxLines = 1
            )
        }

        // 4. 删除按钮
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        }
    }
}