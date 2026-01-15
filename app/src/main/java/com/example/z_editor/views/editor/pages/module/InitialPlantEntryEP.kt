package com.example.z_editor.views.editor.pages.module

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.InitialPlantData
import com.example.z_editor.data.InitialPlantEntryData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.repository.PlantRepository
import com.example.z_editor.data.repository.PlantTag
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialPlantEntryEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    onRequestPlantSelection: ((String) -> Unit) -> Unit
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    var selectedX by remember { mutableIntStateOf(0) }
    var selectedY by remember { mutableIntStateOf(0) }

    var editingPlant by remember { mutableStateOf<InitialPlantData?>(null) }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, InitialPlantEntryData::class.java)
    val moduleDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    moduleDataState.value.plants.find {
        it.gridX == selectedX && it.gridY == selectedY
    }

    moduleDataState.value.plants.filter {
        it.gridX == selectedX && it.gridY == selectedY
    }

    val sortedPlants = remember(moduleDataState.value.plants) {
        moduleDataState.value.plants.sortedWith(compareBy({ it.gridY }, { it.gridX }))
    }

    fun handleSelectPlant() {
        onRequestPlantSelection { plantType ->
            val newList = moduleDataState.value.plants.toMutableList()
            val newPlant = InitialPlantData(
                gridX = selectedX,
                gridY = selectedY,
                level = 1,
                plantTypes = mutableListOf(plantType),
                avatar = false
            )
            newList.add(newPlant)
            moduleDataState.value = moduleDataState.value.copy(plants = newList)
            sync()
        }
    }

    fun updatePlant(oldPlant: InitialPlantData, newPlant: InitialPlantData) {
        val newList = moduleDataState.value.plants.toMutableList()
        val index = newList.indexOf(oldPlant)
        if (index != -1) {
            newList[index] = newPlant
            moduleDataState.value = moduleDataState.value.copy(plants = newList)
            sync()
        }
    }

    fun deletePlant(targetPlant: InitialPlantData) {
        val newList = moduleDataState.value.plants.toMutableList()
        newList.remove(targetPlant)
        moduleDataState.value = moduleDataState.value.copy(plants = newList)
        sync()
    }

    if (editingPlant != null) {
        var tempLevelFloat by remember { mutableFloatStateOf(editingPlant!!.level.toFloat()) }
        var tempAvatar by remember { mutableStateOf(editingPlant!!.avatar) }

        AlertDialog(
            onDismissRequest = { editingPlant = null },
            title = {
                val name = PlantRepository.getName(editingPlant!!.plantTypes.firstOrNull() ?: "")
                Text("编辑 $name")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("等级: ${tempLevelFloat.toInt()}", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                        }
                        Slider(
                            value = tempLevelFloat,
                            onValueChange = { tempLevelFloat = it },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempAvatar = !tempAvatar },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("佩戴第一装扮 (Avatar)", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Switch(checked = tempAvatar, onCheckedChange = { tempAvatar = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val updated = editingPlant!!.copy(
                        level = tempLevelFloat.toInt(),
                        avatar = tempAvatar
                    )
                    updatePlant(editingPlant!!, updated)
                    editingPlant = null
                }) { Text("保存") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            deletePlant(editingPlant!!)
                            editingPlant = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) { Text("删除") }

                    Spacer(Modifier.width(8.dp))

                    TextButton(onClick = { editingPlant = null }) { Text("取消") }
                }
            }
        )
    }


    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
            })
        },
        topBar = {
            TopAppBar(
                title = { Text("预置植物布局", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "帮助说明", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "预置植物模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = Color(0xFF4CAF50)
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "此模块用于在关卡开始前在草坪上预先放置植物。可以设置植物的阶级和是否携带一装。"
                )
                HelpSection(
                    title = "格点坐标",
                    body = "植物所在的位置用网格坐标显示，可以在同一个位置堆放多株植物。"
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // === 区域 1: 网格选择器 (跨满全宽) ===
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.widthIn(max = 480.dp) // 限制最大宽度
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // 状态栏
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("选中位置", fontSize = 12.sp, color = Color.Gray)
                                    Text(
                                        "R${selectedY + 1} : C${selectedX + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                                Spacer(Modifier.weight(1f))

                                Button(
                                    onClick = { handleSelectPlant() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50)
                                    )
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("在此放置")
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // 9x5 网格绘制
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.8f) // 限制宽度后，这个高度也会被限制
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(6.dp))
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    for (row in 0..4) {
                                        Row(Modifier.weight(1f)) {
                                            for (col in 0..8) {
                                                val isSelected =
                                                    (row == selectedY && col == selectedX)
                                                val cellPlants =
                                                    moduleDataState.value.plants.filter { it.gridX == col && it.gridY == row }
                                                val firstPlant = cellPlants.firstOrNull()
                                                var count = cellPlants.size

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .border(0.5.dp, Color(0xFFA5D6A7))
                                                        .background(
                                                            if (isSelected) Color(0xFFFCF2B1)
                                                            else Color.Transparent
                                                        )
                                                        .clickable {
                                                            selectedX = col
                                                            selectedY = row
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (count > 0) {
                                                        if (firstPlant != null) {
                                                            val type =
                                                                firstPlant.plantTypes.firstOrNull()
                                                                    ?: ""
                                                            PlantIconSmall(type)
                                                        }
                                                        if (count > 1) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .background(
                                                                        color = Color.Gray,
                                                                        shape = RoundedCornerShape(
                                                                            bottomStart = 4.dp
                                                                        )
                                                                    )
                                                                    .padding(horizontal = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = "+$count",
                                                                    color = Color.White,
                                                                    fontSize = 8.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // === 区域 2: 标题 (跨满全宽) ===
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "植物分布列表 (行优先排序)",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            // === 区域 3: 物品列表 ===
            items(sortedPlants) { plant ->
                InitialPlantCard(
                    plant = plant,
                    isSelected = (plant.gridX == selectedX && plant.gridY == selectedY),
                    onClick = {
                        selectedX = plant.gridX
                        selectedY = plant.gridY
                        editingPlant = plant
                    }
                )
            }
        }
    }
}


// === 辅助组件 ===

@Composable
fun PlantIconSmall(plantType: String) {
    val info = remember(plantType) {
        PlantRepository.getPlantInfoById(plantType)
    }
    val cardShape = RoundedCornerShape(4.dp)

    if (info?.icon != null) {
        AssetImage(
            path = "images/plants/${info.icon}",
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(0.9f)
                .clip(cardShape)
                .border(0.5.dp, Color.Gray, cardShape),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Low
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize(0.7f)
                .background(Color(0xFF2E7D32), cardShape)
        )
    }
}

@Composable
fun InitialPlantCard(
    plant: InitialPlantData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val plantType = plant.plantTypes.firstOrNull() ?: "Unknown"
    val info = remember(plantType) { PlantRepository.getPlantInfoById(plantType) }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFD5F3D6) else Color.White
        ),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFF4CAF50)) else null,
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AssetImage(
                    path = if (info?.icon != null) "images/plants/${info.icon}" else null,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F8E9))
                        .border(0.5.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                    placeholder = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Gray)
                        )
                    }
                )
                if (plant.avatar) {
                    Icon(
                        Icons.Default.Star,
                        null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color.White, CircleShape)
                            .border(0.5.dp, Color.LightGray, CircleShape)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column {
                Text(
                    text = "R${plant.gridY + 1}:C${plant.gridX + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF2E7D32)
                )
                Text(
                    text = "等级: ${plant.level}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}