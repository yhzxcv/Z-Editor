package com.example.z_editor.views.editor.pages.event

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.LevelParser
import com.example.z_editor.data.MagicMirrorArrayData
import com.example.z_editor.data.MagicMirrorWaveActionData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzPurpleDark
import com.example.z_editor.ui.theme.PvzPurpleLight
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagicMirrorEventEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile
) {
    val currentAlias = LevelParser.extractAlias(rtid)
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }


    var selectedIndex by remember { mutableIntStateOf(0) }
    var isEditingMirror2 by remember { mutableStateOf(false) }

    var typeExpanded by remember { mutableStateOf(false) }
    val typeOptions = listOf(1, 2, 3)

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, MagicMirrorWaveActionData::class.java)
    val eventDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    LaunchedEffect(Unit) {
        if (eventDataState.value.arrays.isEmpty()) {
            val newList = mutableListOf(MagicMirrorArrayData(2, 2, 6, 2, 1, 300))
            eventDataState.value = eventDataState.value.copy(arrays = newList)
            sync()
        }
    }

    val currentArray = eventDataState.value.arrays.getOrNull(selectedIndex)

    fun updateCurrentArray(newData: MagicMirrorArrayData) {
        if (selectedIndex in eventDataState.value.arrays.indices) {
            val newList = eventDataState.value.arrays.toMutableList()
            newList[selectedIndex] = newData
            eventDataState.value = eventDataState.value.copy(arrays = newList)
            sync()
        }
    }

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzPurpleDark else PvzPurpleLight

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            CommonEditorTopAppBar(
                title = "编辑 $currentAlias",
                subtitle = "事件类型：召唤魔镜",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "魔镜事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "基本概念",
                    body = "魔镜事件会在场地上生成成对的传送门。每对传送门包含入口和出口，二者外观相同。"
                )
                HelpSection(
                    title = "外观类型",
                    body = "可以更改镜子的外观样式用于区分，该事件中共有3种不同形态的魔镜。"
                )
                HelpSection(
                    title = "网格操作",
                    body = "网格上显示了所有组别的镜子。灰色为非当前编辑组，紫色高亮为当前编辑组。点击网格空白处可放置当前选中的镜子。"
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // === 区域 1: 数组列表管理 ===
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(eventDataState.value.arrays) { index, _ ->
                    FilterChip(
                        selected = index == selectedIndex,
                        onClick = { selectedIndex = index },
                        label = { Text("第 ${index + 1} 组") },
                        leadingIcon = {
                            if (index == selectedIndex) Icon(
                                Icons.Default.CompareArrows,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (eventDataState.value.arrays.size > 1) {
                                Icon(
                                    Icons.Default.Close,
                                    "删除",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            val newList =
                                                eventDataState.value.arrays.toMutableList()
                                            newList.removeAt(index)
                                            eventDataState.value =
                                                eventDataState.value.copy(arrays = newList)
                                            if (selectedIndex >= newList.size) selectedIndex =
                                                (newList.size - 1).coerceAtLeast(0)
                                            sync()
                                        }
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeColor.copy(alpha = 0.2f),
                            selectedLabelColor = themeColor
                        )
                    )
                }
                item {
                    IconButton(
                        onClick = {
                            val newList = eventDataState.value.arrays.toMutableList()
                            newList.add(MagicMirrorArrayData(2, 2, 6, 2, 1, 300))
                            eventDataState.value = eventDataState.value.copy(arrays = newList)
                            selectedIndex = newList.lastIndex
                            sync()
                        }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            "添加组",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (currentArray != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "第 ${selectedIndex + 1} 组配置",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = themeColor
                        )
                        Spacer(Modifier.height(12.dp))

                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = !typeExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = "样式 ${currentArray.typeIndex}",
                                onValueChange = {},
                                label = { Text("镜子外观 (TypeIndex)") },
                                readOnly = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedBorderColor = themeColor,
                                    focusedLabelColor = themeColor
                                ),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false }
                            ) {
                                typeOptions.forEach { typeVal ->
                                    DropdownMenuItem(
                                        text = { Text("样式 $typeVal") },
                                        onClick = {
                                            updateCurrentArray(currentArray.copy(typeIndex = typeVal))
                                            typeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        NumberInputInt(
                            value = currentArray.mirrorExistDuration,
                            onValueChange = {
                                updateCurrentArray(currentArray.copy(mirrorExistDuration = it))
                            },
                            label = "存在持续时间 (秒)",
                            modifier = Modifier.fillMaxWidth(),
                            color = themeColor
                        )

                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (!isEditingMirror2) themeColor else Color.Transparent)
                                    .clickable { isEditingMirror2 = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Login,
                                        null,
                                        tint = if (!isEditingMirror2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "放置镜子 1",
                                        color = if (!isEditingMirror2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isEditingMirror2) themeColor else Color.Transparent)
                                    .clickable { isEditingMirror2 = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Logout,
                                        null,
                                        tint = if (isEditingMirror2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "放置镜子 2",
                                        color = if (isEditingMirror2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.widthIn(max = 480.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (!isEditingMirror2) "当前编辑: 镜子 1" else "当前编辑: 镜子 2",
                                    color = themeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "M1: R${currentArray.mirror1GridY + 1}:C${currentArray.mirror1GridX + 1}  |  M2: R${currentArray.mirror2GridY + 1}:C${currentArray.mirror2GridX + 1}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.8f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0xFF413B49) else Color(0xFFEDE7F6))
                                    .border(1.dp, Color(0xFFD1C4E9), RoundedCornerShape(6.dp))
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    for (row in 0..4) {
                                        Row(Modifier.weight(1f)) {
                                            for (col in 0..8) {
                                                val mirrorsInCell =
                                                    eventDataState.value.arrays.mapIndexedNotNull { idx, data ->
                                                        if (data.mirror1GridX == col && data.mirror1GridY == row) Triple(
                                                            idx,
                                                            1,
                                                            data.typeIndex
                                                        )
                                                        else if (data.mirror2GridX == col && data.mirror2GridY == row) Triple(
                                                            idx,
                                                            2,
                                                            data.typeIndex
                                                        )
                                                        else null
                                                    }
                                                val isTargetCell =
                                                    (!isEditingMirror2 && currentArray.mirror1GridX == col && currentArray.mirror1GridY == row)
                                                            || (isEditingMirror2 && currentArray.mirror2GridX == col && currentArray.mirror2GridY == row)

                                                val mirrorToShow =
                                                    mirrorsInCell.find { it.first == selectedIndex }
                                                        ?: mirrorsInCell.lastOrNull()

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .border(
                                                            width = if (isTargetCell) 1.5.dp else 0.5.dp,
                                                            color = if (isTargetCell) themeColor else Color(
                                                                0xFFD1C4E9
                                                            )
                                                        )
                                                        .background(
                                                            if (isTargetCell) Color(0xFFB0B0B0).copy(
                                                                alpha = 0.6f
                                                            ) else Color.Transparent
                                                        )
                                                        .clickable {
                                                            if (isEditingMirror2) {
                                                                updateCurrentArray(
                                                                    currentArray.copy(
                                                                        mirror2GridX = col,
                                                                        mirror2GridY = row
                                                                    )
                                                                )
                                                            } else {
                                                                updateCurrentArray(
                                                                    currentArray.copy(
                                                                        mirror1GridX = col,
                                                                        mirror1GridY = row
                                                                    )
                                                                )
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (mirrorToShow != null) {
                                                        val (groupIndex, mirrorNum, typeIdx) = mirrorToShow
                                                        val isSelected = groupIndex == selectedIndex

                                                        val imageName = "magic_mirror$typeIdx.png"

                                                        AssetImage(
                                                            path = "images/griditems/$imageName",
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .fillMaxSize(0.85f)
                                                                .clip(RoundedCornerShape(4.dp)),
                                                            filterQuality = FilterQuality.Medium
                                                        )

                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(2.dp)
                                                                .size(18.dp)
                                                                .background(
                                                                    color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                        alpha = 0.8f
                                                                    ),
                                                                    shape = CircleShape
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "${groupIndex + 1}",
                                                                color = Color.White,
                                                                fontSize = 10.sp,
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
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "请点击上方 + 号添加一组魔镜",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}