package com.example.z_editor.views.editor.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.LocationData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.VaseBreakerPresetData
import com.example.z_editor.data.VaseDefinition
import com.example.z_editor.data.repository.PlantRepository
import com.example.z_editor.data.repository.PlantTag
import com.example.z_editor.data.repository.ZombieRepository
import com.example.z_editor.data.repository.ZombieTag
import com.example.z_editor.views.components.AssetImage
import com.google.gson.Gson

private val gson = Gson()

@Composable
fun VaseBreakerTab(
    rootLevelFile: PvzLevelFile,
    refreshTrigger: Int,
    scrollState: LazyListState,
    onRequestPlantSelection: ((List<String>) -> Unit) -> Unit,
    onRequestZombieSelection: ((List<String>) -> Unit) -> Unit
) {
    val presetObj = remember(rootLevelFile, refreshTrigger) {
        rootLevelFile.objects.find { it.objClass == "VaseBreakerPresetProperties" }
    }

    if (presetObj == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("数据异常：未找到罐子配置模块")
        }
        return
    }

    val dataState = remember(presetObj) {
        val parsedData = try {
            gson.fromJson(presetObj.objData, VaseBreakerPresetData::class.java)
        } catch (_: Exception) {
            VaseBreakerPresetData()
        }
        mutableStateOf(parsedData)
    }

    fun sync() {
        presetObj.objData = gson.toJsonTree(dataState.value)
    }

    fun updateState(newData: VaseBreakerPresetData) {
        dataState.value = newData
        sync()
    }

    fun updateData(mutation: (VaseBreakerPresetData) -> Unit) {
        val currentVases = ArrayList(dataState.value.vases)
        val currentBlacklist = ArrayList(dataState.value.gridSquareBlacklist)

        val newData = dataState.value.copy(
            vases = currentVases,
            gridSquareBlacklist = currentBlacklist
        )
        mutation(newData)
        dataState.value = newData
        sync()
    }

    val data = dataState.value

    val minCol = data.minColumnIndex
    val maxCol = data.maxColumnIndex

    val blacklistCount = data.gridSquareBlacklist.count {
        it.x in minCol..maxCol && it.y in 0..4
    }

    val totalSlots = (maxCol - minCol + 1) * 5 - blacklistCount
    val currentAssigned = data.vases.sumOf { it.count }
    val isCapacityError = totalSlots != currentAssigned

    var showAddDialog by remember { mutableStateOf(false) }
    var showCollectableDialog by remember { mutableStateOf(false) }

    var vaseToDelete by remember { mutableStateOf<VaseDefinition?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp, top = 16.dp, start = 16.dp, end = 16.dp), // 调整边距
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "罐子生成范围与禁用格点",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF5D4037)
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            BoundaryStepper(
                                label = "起始列 (Min)",
                                value = minCol,
                                onMinus = {
                                    if (minCol > 0) updateData { it.minColumnIndex = minCol - 1 }
                                },
                                onPlus = {
                                    if (minCol < maxCol) updateData { it.minColumnIndex = minCol + 1 }
                                }
                            )

                            BoundaryStepper(
                                label = "结束列 (Max)",
                                value = maxCol,
                                onMinus = {
                                    if (maxCol > minCol) updateData { it.maxColumnIndex = maxCol - 1 }
                                },
                                onPlus = {
                                    if (maxCol < 8) updateData { it.maxColumnIndex = maxCol + 1 }
                                }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "点击格点可切换禁用状态（禁用点将不生成罐子）",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.8f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFD7CCC8))
                                .border(1.dp, Color(0xFFA1887F), RoundedCornerShape(6.dp))
                        ) {
                            Column(Modifier.fillMaxSize()) {
                                for (row in 0..4) {
                                    Row(Modifier.weight(1f)) {
                                        for (col in 0..8) {
                                            val isActiveZone = col in minCol..maxCol
                                            val isBlacklisted = data.gridSquareBlacklist.any { it.x == col && it.y == row } == true

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .border(0.5.dp, Color(0xFF8D6E63).copy(alpha = 0.5f))
                                                    .background(
                                                        when {
                                                            isBlacklisted -> Color.Black.copy(alpha = 0.6f) // 黑名单显示深色
                                                            isActiveZone -> Color(0xFF9D7165)
                                                            else -> Color.Transparent
                                                        }
                                                    )
                                                    .clickable {
                                                        updateData { mutableData ->
                                                            val blacklist = mutableData.gridSquareBlacklist
                                                            val existing = blacklist.find { it.x == col && it.y == row }

                                                            if (existing != null) {
                                                                blacklist.remove(existing)
                                                            } else {
                                                                blacklist.add(LocationData(col, row))
                                                            }
                                                            mutableData.gridSquareBlacklist = blacklist
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isBlacklisted) {
                                                    Icon(
                                                        Icons.Default.Block,
                                                        contentDescription = null,
                                                        tint = Color.White.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                } else if (row == 4) {
                                                    Text(
                                                        text = "${col + 1}",
                                                        fontSize = 10.sp,
                                                        color = if (isActiveZone) Color.White.copy(0.7f) else Color(0xFF5D4037).copy(0.5f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isCapacityError) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isCapacityError) Color.Red else Color(0xFF4CAF50),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isCapacityError) Icons.Default.Warning else Icons.Default.CheckCircle,
                                null,
                                tint = if (isCapacityError) Color.Red else Color(0xFF4CAF50)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isCapacityError) "配置数量与有效容量不符" else "配置数量已匹配容量",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isCapacityError) Color.Red else Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "有效容量: $totalSlots  |  已配置: $currentAssigned",
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("特殊罐子设置", fontWeight = FontWeight.Bold, color = Color(0xFF5D4037), fontSize = 18.sp)
                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("植物罐子 (绿罐)", modifier = Modifier.width(150.dp), fontSize = 16.sp)
                            Spacer(Modifier.weight(1f))
                            Stepper(
                                value = data.numColoredPlantVases,
                                onChange = {
                                    updateState(data.copy(numColoredPlantVases = it))
                                }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(0.3f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("僵尸罐子 (紫罐)", modifier = Modifier.width(150.dp), fontSize = 16.sp)
                            Spacer(Modifier.weight(1f))
                            Stepper(
                                value = data.numColoredZombieVases,
                                onChange = {
                                    updateState(data.copy(numColoredZombieVases = it))
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "罐子内容列表",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }

            items(data.vases, key = { System.identityHashCode(it) }) { vase ->
                VaseItemRow(
                    vase = vase,
                    onDelete = { vaseToDelete = vase },
                    onCountChange = { newCount ->
                        updateData { mutableData ->
                            val index = mutableData.vases.indexOfFirst { v -> v === vase }
                            if (index != -1) {
                                mutableData.vases[index] = mutableData.vases[index].copy(count = newCount)
                            }
                        }
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = Color(0xFF795548),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, null)
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加罐子内容") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("植物 (Plant)") },
                        modifier = Modifier.clickable {
                            showAddDialog = false
                            onRequestPlantSelection { plantIds ->
                                val newList = data.vases.toMutableList()
                                plantIds.forEach { plantId ->
                                    newList.add(VaseDefinition(plantTypeName = plantId, count = 1))
                                }
                                updateState(data.copy(vases = newList))
                            }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("僵尸 (Zombie)") },
                        modifier = Modifier.clickable {
                            showAddDialog = false
                            onRequestZombieSelection { zombieIds ->
                                val newList = data.vases.toMutableList()
                                zombieIds.forEach { zombieId ->
                                    newList.add(VaseDefinition(zombieTypeName = zombieId, count = 1))
                                }
                                updateState(data.copy(vases = newList))
                            }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("道具 (Collectable)") },
                        modifier = Modifier.clickable {
                            showAddDialog = false
                            showCollectableDialog = true
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }

    if (showCollectableDialog) {
        AlertDialog(
            onDismissRequest = { showCollectableDialog = false },
            title = { Text("选择道具类型") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    collectableTypes.forEach { info ->
                        ListItem(
                            headlineContent = { Text(info.name) },
                            supportingContent = { Text(info.id, fontSize = 10.sp) },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.LightGray)
                                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AssetImage(
                                        path = "images/others/${info.iconName}",
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp),
                                        placeholder = { Icon(Icons.Default.Stars, null, tint = Color(0xFFFFC107)) }
                                    )
                                }
                            },
                            modifier = Modifier
                                .clickable {
                                    updateData { it.vases.add(VaseDefinition(collectableTypeName = info.id, count = 1)) }
                                    showCollectableDialog = false
                                }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (vaseToDelete != null) {
        AlertDialog(
            onDismissRequest = { vaseToDelete = null },
            title = { Text("移除配置") },
            text = { Text("确定要移除该配置项吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        val newList = data.vases.toMutableList()
                        newList.remove(vaseToDelete)
                        updateState(data.copy(vases = newList))
                        vaseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("移除") }
            },
            dismissButton = {
                TextButton(onClick = { vaseToDelete = null }) { Text("取消") }
            }
        )
    }
}

data class CollectableType(
    val id: String,
    val name: String,
    val iconName: String
)

private val collectableTypes = listOf(
    CollectableType("plantfood", "能量豆", "plantfood.webp"),
    CollectableType("sun_large", "大型阳光", "sun_large.webp")
)


// === 辅助组件 ===
@Composable
fun VaseItemRow(
    vase: VaseDefinition,
    onDelete: () -> Unit,
    onCountChange: (Int) -> Unit
) {
    val type: String
    val name: String
    val iconPath: String?

    if (vase.plantTypeName != null) {
        type = "植物"
        val alias = RtidParser.parse(vase.plantTypeName!!)?.alias ?: vase.plantTypeName!!
        name = PlantRepository.getName(alias)
        val pInfo = PlantRepository.search(alias, PlantTag.All).firstOrNull()
        iconPath = if (pInfo?.icon != null) "images/plants/${pInfo.icon}" else null
    } else if (vase.zombieTypeName != null) {
        type = "僵尸"
        val alias = RtidParser.parse(vase.zombieTypeName!!)?.alias ?: vase.zombieTypeName!!
        name = ZombieRepository.getName(alias)
        val zInfo = ZombieRepository.search(alias, ZombieTag.All).firstOrNull()
        iconPath = if (zInfo?.icon != null) "images/zombies/${zInfo.icon}" else null
    }  else {
        type = "道具"
        val info = collectableTypes.find { it.id == vase.collectableTypeName }
        name = info?.name ?: vase.collectableTypeName ?: "未知道具"
        iconPath = info?.let { "images/others/${it.iconName}" }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (iconPath != null) {
                    AssetImage(
                        path = iconPath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        filterQuality = FilterQuality.Medium,
                        placeholder = { Text(name.take(1)) }
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(type, fontSize = 12.sp, color = Color.Gray)
            }

            Stepper(value = vase.count, onChange = onCountChange)

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, null, tint = Color.LightGray.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
            }
        }
    }
}


@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color(0xFFF5F5F5), RoundedCornerShape(6.dp))
            .border(1.dp, Color.LightGray.copy(0.5f), RoundedCornerShape(6.dp))
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(32.dp)
                .clickable { if (value >= 1) onChange(value - 1) },
            contentAlignment = Alignment.Center
        ) {
            Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
        }

        Text(
            "$value",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Box(
            modifier = Modifier
                .width(32.dp)
                .height(32.dp)
                .clickable { onChange(value + 1) },
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
        }
    }
}

@Composable
fun BoundaryStepper(
    label: String,
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color(0xFFEFEBE9), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF8D6E63), RoundedCornerShape(8.dp))
        ) {
            IconButton(
                onClick = onMinus,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Remove, null, tint = Color(0xFF5D4037), modifier = Modifier.size(16.dp))
            }

            Box(
                modifier = Modifier
                    .width(32.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${value + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF3E2723)
                )
            }

            IconButton(
                onClick = onPlus,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF5D4037), modifier = Modifier.size(16.dp))
            }
        }
    }
}