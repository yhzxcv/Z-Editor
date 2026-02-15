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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.z_editor.data.InitialPlantPlacementData
import com.example.z_editor.data.InitialPlantPropertiesData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.repository.PlantRepository
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzGridHighLight
import com.example.z_editor.ui.theme.PvzLightGreenDark
import com.example.z_editor.ui.theme.PvzLightGreenLight
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialPlantPropertiesEP(
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

    var editingPlacement by remember { mutableStateOf<InitialPlantPlacementData?>(null) }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, InitialPlantPropertiesData::class.java)
    val moduleDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    val sortedPlacements = remember(moduleDataState.value.placements) {
        moduleDataState.value.placements.sortedWith(compareBy({ it.gridY }, { it.gridX }))
    }

    fun handleSelectPlant() {
        onRequestPlantSelection { plantType ->
            val newList = moduleDataState.value.placements.toMutableList()
            val newPlacement = InitialPlantPlacementData(
                gridX = selectedX,
                gridY = selectedY,
                typeName = plantType,
                level = 1,
                condition = null
            )
            newList.add(newPlacement)
            moduleDataState.value = moduleDataState.value.copy(placements = newList)
            sync()
        }
    }

    fun updatePlacement(
        oldPlacement: InitialPlantPlacementData,
        newPlacement: InitialPlantPlacementData
    ) {
        val newList = moduleDataState.value.placements.toMutableList()
        val index = newList.indexOf(oldPlacement)
        if (index != -1) {
            newList[index] = newPlacement
            moduleDataState.value = moduleDataState.value.copy(placements = newList)
            sync()
        }
    }

    fun deletePlacement(targetPlacement: InitialPlantPlacementData) {
        val newList = moduleDataState.value.placements.toMutableList()
        newList.remove(targetPlacement)
        moduleDataState.value = moduleDataState.value.copy(placements = newList)
        sync()
    }

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzLightGreenDark else PvzLightGreenLight

    if (editingPlacement != null) {
        var tempLevelFloat by remember { mutableFloatStateOf(editingPlacement!!.level.toFloat()) }
        var tempCondition by remember { mutableStateOf(editingPlacement!!.condition) }
        var conditionExpanded by remember { mutableStateOf(false) }

        val conditionOptions = listOf(
            null to "无状态 (null)",
            "icecubed" to "冰封状态 (Icecubed)"
        )

        AlertDialog(
            onDismissRequest = { editingPlacement = null },
            title = {
                val name = PlantRepository.getName(editingPlacement!!.typeName)
                Text("编辑 $name")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("等级: ${tempLevelFloat.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.weight(1f))
                        }
                        Slider(
                            value = tempLevelFloat,
                            onValueChange = { tempLevelFloat = it },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                    }

                    // 状态下拉框 (Condition)
                    ExposedDropdownMenuBox(
                        expanded = conditionExpanded,
                        onExpandedChange = { conditionExpanded = !conditionExpanded }
                    ) {
                        OutlinedTextField(
                            value = conditionOptions.find { it.first == tempCondition }?.second
                                ?: "未知状态",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("初始状态 (Condition)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) },
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
                            expanded = conditionExpanded,
                            onDismissRequest = { conditionExpanded = false }
                        ) {
                            conditionOptions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        tempCondition = value
                                        conditionExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val updated = editingPlacement!!.copy(
                        level = tempLevelFloat.toInt(),
                        condition = tempCondition
                    )
                    updatePlacement(editingPlacement!!, updated)
                    editingPlacement = null
                }) { Text("保存") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            deletePlacement(editingPlacement!!)
                            editingPlacement = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onError)
                    ) { Text("删除") }

                    Spacer(Modifier.width(8.dp))

                    TextButton(onClick = { editingPlacement = null }) { Text("取消") }
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
            CommonEditorTopAppBar(
                title = "初始植物配置",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "初始植物配置模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "此模块用于在关卡开始前配置植物布局，与预置植物布局类似，但结构不同且支持特殊状态。"
                )
                HelpSection(
                    title = "特殊状态",
                    body = "可以为植物设置冰封状态，常见于冰河世界关卡。"
                )
                HelpSection(
                    title = "复活之战模式",
                    body = "开启复活之战模式后，初始植物将于开始游戏后被销毁。注意中文版没有销毁植物的火焰效果。"
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .clickable {
                                val currentVal =
                                    moduleDataState.value.isInitialIntensiveCarrotPlacements == true
                                val newVal = if (!currentVal) true else null
                                moduleDataState.value = moduleDataState.value.copy(
                                    isInitialIntensiveCarrotPlacements = newVal
                                )
                                sync()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "复活之战模式",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "IsInitialIntensiveCarrotPlacements",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = moduleDataState.value.isInitialIntensiveCarrotPlacements == true,
                            onCheckedChange = { checked ->
                                val newVal = if (checked) true else null
                                moduleDataState.value = moduleDataState.value.copy(
                                    isInitialIntensiveCarrotPlacements = newVal
                                )
                                sync()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = themeColor,
                                checkedBorderColor = Color.Transparent,

                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.widthIn(max = 480.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(
                                        "选中位置",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "R${selectedY + 1} : C${selectedX + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = themeColor
                                    )
                                }
                                Spacer(Modifier.weight(1f))

                                Button(
                                    onClick = { handleSelectPlant() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = themeColor
                                    )
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("在此放置")
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.8f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0xFF3C483D) else Color(0xFFE8F5E9))
                                    .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(6.dp))
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    for (row in 0..4) {
                                        Row(Modifier.weight(1f)) {
                                            for (col in 0..8) {
                                                val isSelected =
                                                    (row == selectedY && col == selectedX)
                                                val cellPlacements =
                                                    moduleDataState.value.placements.filter {
                                                        it.gridX == col && it.gridY == row
                                                    }
                                                val count = cellPlacements.size
                                                val firstPlacement = cellPlacements.firstOrNull()

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .border(
                                                            0.5.dp,
                                                            if (isSelected) themeColor else Color(
                                                                0xFFA5D6A7
                                                            )
                                                        )
                                                        .background(
                                                            if (isSelected) PvzGridHighLight
                                                            else Color.Transparent
                                                        )
                                                        .clickable {
                                                            selectedX = col
                                                            selectedY = row
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (count > 0 && firstPlacement != null) {
                                                        PlantIconSmall2(firstPlacement.typeName)
                                                        if (count > 1) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .background(
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "植物分布列表 (行优先排序)",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            items(sortedPlacements) { placement ->
                InitialPlacementCard(
                    placement = placement,
                    isSelected = (placement.gridX == selectedX && placement.gridY == selectedY),
                    onClick = {
                        selectedX = placement.gridX
                        selectedY = placement.gridY
                        editingPlacement = placement
                    }
                )
            }
        }
    }
}

// === 辅助组件 ===

@Composable
fun PlantIconSmall2(plantType: String) {
    val info = remember(plantType) { PlantRepository.getPlantInfoById(plantType) }
    val cardShape = RoundedCornerShape(4.dp)

    AssetImage(
        path = if (info?.icon != null) "images/plants/${info.icon}" else "images/others/unknown.webp",
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize(0.9f)
            .clip(cardShape)
            .border(0.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, cardShape),
        contentScale = ContentScale.Crop,
        filterQuality = FilterQuality.Low
    )
}

@Composable
fun InitialPlacementCard(
    placement: InitialPlantPlacementData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val plantType = placement.typeName
    val info = remember(plantType) { PlantRepository.getPlantInfoById(plantType) }
    val isDark = LocalDarkTheme.current

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) if (isDark) Color(0xFF3C483D) else Color(0xFFD5F3D6)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFF4CAF50)) else null,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AssetImage(
                    path = if (info?.icon != null) "images/plants/${info.icon}" else "images/others/unknown.webp",
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F8E9)),
                    placeholder = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                )
            }

            Spacer(Modifier.width(8.dp))

            Column {
                Text(
                    text = "R${placement.gridY + 1}:C${placement.gridX + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF2E7D32)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${placement.condition}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}