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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.ProtectPlantData
import com.example.z_editor.data.ProtectThePlantChallengePropertiesData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.repository.PlantRepository
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectThePlantChallengePropertiesEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    onRequestPlantSelection: ((String) -> Unit) -> Unit
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val themeColor = Color(0xFF4CAF50)
    val lightThemeColor = Color(0xFFE8F5E9)
    val borderThemeColor = Color(0xFFA5D6A7)

    val obj = remember(rootLevelFile) {
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    }
    val syncManager = rememberJsonSync(obj, ProtectThePlantChallengePropertiesData::class.java)
    var moduleData by syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    LaunchedEffect(moduleData.plants.size) {
        if (moduleData.mustProtectCount != moduleData.plants.size) {
            moduleData = moduleData.copy(mustProtectCount = moduleData.plants.size)
            sync()
        }
    }

    var selectedX by remember { mutableIntStateOf(2) }
    var selectedY by remember { mutableIntStateOf(2) }

    val sortedPlants = remember(moduleData.plants) {
        moduleData.plants.sortedWith(compareBy({ it.gridY }, { it.gridX }))
    }

    fun handleAddPlant() {
        onRequestPlantSelection { plantType ->
            val newList = moduleData.plants.toMutableList()
            newList.removeAll { it.gridX == selectedX && it.gridY == selectedY }

            val newItem = ProtectPlantData(
                gridX = selectedX,
                gridY = selectedY,
                plantType = plantType
            )
            newList.add(newItem)
            moduleData = moduleData.copy(plants = newList)
            sync()
        }
    }

    fun deletePlant(target: ProtectPlantData) {
        val newList = moduleData.plants.toMutableList()
        newList.remove(target)
        moduleData = moduleData.copy(plants = newList)
        sync()
    }

    var itemToDelete by remember { mutableStateOf<ProtectPlantData?>(null) }
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("移除保护植物") },
            text = {
                val name = PlantRepository.getName(itemToDelete!!.plantType)
                Text("确定要移除 R${itemToDelete!!.gridY + 1}:C${itemToDelete!!.gridX + 1} 处的 $name 吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletePlant(itemToDelete!!)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("移除") }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("取消") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text("保护植物挑战", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
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
                    containerColor = themeColor,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "保护植物挑战说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "定义关卡中必须保护的植物。如果这些植物被僵尸吃掉或摧毁，关卡失败。"
                )
                HelpSection(
                    title = "自动计数",
                    body = "软件会自动跟随您添加的植物数量更新需要保护的植物数量。"
                )
                HelpSection(
                    title = "网格操作",
                    body = "在上方网格中点击选择坐标，然后点击‘添加植物’按钮选择要保护的植物种类。"
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
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE9EEF5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Info, null, tint = themeColor)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "此模块中的植物只能随玩家账号阶级，可以使用平行宇宙的植物统一等级。",
                                fontSize = 12.sp,
                                color = themeColor,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.widthIn(max = 480.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("目标位置", fontSize = 12.sp, color = Color.Gray)
                                    Text(
                                        "R${selectedY + 1} : C${selectedX + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = themeColor
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                Button(
                                    onClick = { handleAddPlant() },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("添加植物")
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.8f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(lightThemeColor)
                                    .border(1.dp, borderThemeColor, RoundedCornerShape(6.dp))
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    for (row in 0..4) {
                                        Row(Modifier.weight(1f)) {
                                            for (col in 0..8) {
                                                val isSelected =
                                                    (row == selectedY && col == selectedX)
                                                val cellPlant =
                                                    moduleData.plants.find { it.gridX == col && it.gridY == row }

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .border(0.5.dp, borderThemeColor)
                                                        .background(
                                                            if (isSelected) Color(0xFFEBF13E).copy(
                                                                alpha = 0.5f
                                                            )
                                                            else Color.Transparent
                                                        )
                                                        .clickable {
                                                            selectedX = col
                                                            selectedY = row
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (cellPlant != null) {
                                                        ProtectPlantIconSmall(cellPlant.plantType)
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

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "保护植物列表",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            items(sortedPlants) { plant ->
                ProtectPlantCard(
                    plant = plant,
                    isSelected = (plant.gridX == selectedX && plant.gridY == selectedY),
                    onClick = {
                        selectedX = plant.gridX
                        selectedY = plant.gridY
                    },
                    onDelete = { itemToDelete = plant }
                )
            }
        }
    }
}

@Composable
fun ProtectPlantIconSmall(plantType: String) {
    val info = remember(plantType) { PlantRepository.getPlantInfoById(plantType) }
    if (info?.icon != null) {
        AssetImage(
            path = "images/plants/${info.icon}",
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(0.9f)
                .clip(RoundedCornerShape(4.dp))
                .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Medium
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize(0.8f)
                .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                plantType.take(1).uppercase(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProtectPlantCard(
    plant: ProtectPlantData,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val info = remember(plant.plantType) { PlantRepository.getPlantInfoById(plant.plantType) }
    val displayName = info?.name ?: plant.plantType

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.White
        ),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFF4CAF50)) else null,
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AssetImage(
                    path = if (info?.icon != null) "images/plants/${info.icon}" else null,
                    contentDescription = null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .size(36.dp)
                        .border(0.5.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                    filterQuality = FilterQuality.Medium
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "R${plant.gridY + 1}:C${plant.gridX + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF33691E)
                )
            }
        }
    }
}