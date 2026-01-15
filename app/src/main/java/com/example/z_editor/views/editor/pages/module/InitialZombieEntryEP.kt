package com.example.z_editor.views.editor.pages.module

import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.InitialZombieData
import com.example.z_editor.data.InitialZombieEntryData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.repository.ZombieRepository
import com.example.z_editor.data.repository.ZombieTag
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import rememberJsonSync

private val ZOMBIE_CONDITIONS = listOf(
    "icecubed" to "冰块封装 (icecubed)",
    "freeze" to "冰冻状态 (freeze)",
    "stun" to "眩晕状态 (stun)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialZombieEntryEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    onRequestZombieSelection: ((String) -> Unit) -> Unit
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var selectedX by remember { mutableIntStateOf(0) }
    var selectedY by remember { mutableIntStateOf(0) }

    var editingPlacement by remember { mutableStateOf<InitialZombieData?>(null) }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, InitialZombieEntryData::class.java)
    val moduleDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    moduleDataState.value.placements.filter {
        it.gridX == selectedX && it.gridY == selectedY
    }

    val sortedPlacements = remember(moduleDataState.value.placements) {
        moduleDataState.value.placements.sortedWith(compareBy({ it.gridY }, { it.gridX }))
    }

    fun handleAddZombie() {
        onRequestZombieSelection { selectedId ->
            val isElite = ZombieRepository.isElite(selectedId)
            val aliases = ZombieRepository.buildAliases(selectedId)
            if (!isElite) {
                val newList = moduleDataState.value.placements.toMutableList()
                val newPlacement = InitialZombieData(
                    gridX = selectedX,
                    gridY = selectedY,
                    typeName = aliases,
                    condition = "icecubed"
                )
                newList.add(newPlacement)
                moduleDataState.value = moduleDataState.value.copy(placements = newList)
                sync()
            } else Toast.makeText(context, "不能添加精英僵尸", Toast.LENGTH_SHORT).show()
        }
    }

    fun updatePlacement(oldItem: InitialZombieData, newItem: InitialZombieData) {
        val newList = moduleDataState.value.placements.toMutableList()
        val index = newList.indexOf(oldItem)
        if (index != -1) {
            newList[index] = newItem
            moduleDataState.value = moduleDataState.value.copy(placements = newList)
            sync()
        }
    }

    fun deletePlacement(targetItem: InitialZombieData) {
        val newList = moduleDataState.value.placements.toMutableList()
        newList.remove(targetItem)
        moduleDataState.value = moduleDataState.value.copy(placements = newList)
        sync()
    }

    val themeColor = Color(0xFF654B80)

    if (editingPlacement != null) {
        var tempCondition by remember { mutableStateOf(editingPlacement!!.condition) }

        val isPredefined = ZOMBIE_CONDITIONS.any { it.first == tempCondition }
        var isCustomInput by remember { mutableStateOf(!isPredefined) }

        var menuExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingPlacement = null },
            title = {
                val name = ZombieRepository.getName(editingPlacement!!.typeName)
                Text("编辑预置僵尸: $name", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCustomInput = !isCustomInput },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("开启手动输入", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = isCustomInput,
                            onCheckedChange = { isCustomInput = it }
                        )
                    }

                    if (isCustomInput) {
                        // === 模式 A: 自定义文本输入 ===
                        OutlinedTextField(
                            value = tempCondition,
                            onValueChange = { tempCondition = it },
                            label = { Text("输入标准状态值") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text("使用自定义输入需确保输入值准确", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        // === 模式 B: 下拉菜单选择 ===
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val displayLabel =
                                ZOMBIE_CONDITIONS.find { it.first == tempCondition }?.second
                                    ?: tempCondition.ifEmpty { "请选择状态" }

                            OutlinedTextField(
                                value = displayLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("预设状态列表") },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.ArrowDropDown,
                                        null
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                enabled = false
                            )

                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { menuExpanded = true }
                            )

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .heightIn(max = 300.dp)
                            ) {
                                ZOMBIE_CONDITIONS.forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(label)
                                            }
                                        },
                                        onClick = {
                                            tempCondition = value
                                            menuExpanded = false
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = if (tempCondition == value)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                Color.Unspecified
                                        )
                                    )
                                }
                            }
                        }
                        Text(
                            "点击输入框从预设的状态列表中选择",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val updated = editingPlacement!!.copy(condition = tempCondition)
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
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) { Text("删除") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { editingPlacement = null }) { Text("取消") }
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text("预置僵尸布局", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "说明", tint = Color.White)
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
                title = "预置僵尸模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "此模块用于在关卡开始前在草坪上预先放置僵尸。在主线常用于冰河世界放置被冰冻的僵尸。"
                )
                HelpSection(
                    title = "格点坐标",
                    body = "僵尸所在的位置用网格坐标显示，可以在同一个位置堆放多只僵尸。"
                )
                HelpSection(
                    title = "预设状态",
                    body = "决定僵尸的预设状态，如果非冰冻眩晕等会抢跑导致失败。这里提供了手动输入开关输入标准状态。"
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
                Box(contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.widthIn(max = 480.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("选中位置", fontSize = 12.sp, color = Color.Gray)
                                    Text(
                                        "R${selectedY + 1} : C${selectedX + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = themeColor
                                    )
                                }
                                Spacer(Modifier.weight(1f))

                                Button(
                                    onClick = { handleAddZombie() },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("放置僵尸")
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.8f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEFEFFF))
                                    .border(
                                        1.dp,
                                        Color(0xFFBAC4FA),
                                        RoundedCornerShape(6.dp)
                                    )
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    for (row in 0..4) {
                                        Row(Modifier.weight(1f)) {
                                            for (col in 0..8) {
                                                val isSelected =
                                                    (row == selectedY && col == selectedX)
                                                val cellZombies =
                                                    moduleDataState.value.placements.filter {
                                                        it.gridX == col && it.gridY == row
                                                    }
                                                val firstZombie =
                                                    cellZombies.firstOrNull()
                                                var count = cellZombies.size

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .border(
                                                            0.5.dp,
                                                            Color(0xFF8581FA)
                                                        )
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
                                                    if (count > 0 && firstZombie != null) {
                                                        ZombieIconSmall(firstZombie.typeName)
                                                        if (count > 1) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .background(
                                                                        Color.Gray,
                                                                        RoundedCornerShape(
                                                                            bottomStart = 4.dp
                                                                        )
                                                                    )
                                                                    .padding(horizontal = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = "+$count",
                                                                    color = Color.White,
                                                                    fontSize = 8.sp,
                                                                    fontWeight = FontWeight.Bold
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
                    "僵尸分布列表 (行优先排序)",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            items(sortedPlacements) { item ->
                InitialZombieCard(
                    item = item,
                    isSelected = (item.gridX == selectedX && item.gridY == selectedY),
                    onClick = {
                        selectedX = item.gridX
                        selectedY = item.gridY
                        editingPlacement = item
                    }
                )
            }
        }
    }
}

@Composable
fun ZombieIconSmall(typeName: String) {
    val info = remember(typeName) { ZombieRepository.getZombieInfoById(typeName) }
    val cardShape = RoundedCornerShape(4.dp)

    if (info?.icon != null) {
        AssetImage(
            path = "images/zombies/${info.icon}",
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
                .fillMaxSize(0.9f)
                .background(Color.LightGray, cardShape)
        )
    }
}

@Composable
fun InitialZombieCard(
    item: InitialZombieData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val info = remember(item.typeName) {
        ZombieRepository.getZombieInfoById(item.typeName)
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFD9D7F6) else Color.White
        ),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFF8581FA)) else null,
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AssetImage(
                    path = if (info?.icon != null) "images/zombies/${info.icon}" else null,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEFEBE9))
                        .border(0.5.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                    placeholder = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Gray)
                        )
                    }
                )
            }

            Spacer(Modifier.width(8.dp))

            Column {
                Text(
                    text = "R${item.gridY + 1}:C${item.gridX + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF654B80)
                )
                Text(
                    text = item.condition,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}
