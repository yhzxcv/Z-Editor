package com.example.z_editor.views.editor.pages.module

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RiftTimedSunData
import com.example.z_editor.data.RiftTimedSunModuleData
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.repository.ZombieRepository
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
fun LevelMutatorRiftTimedSunEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    onRequestZombieSelection: ((List<String>) -> Unit) -> Unit
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzPurpleDark else PvzPurpleLight

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, RiftTimedSunModuleData::class.java)
    val dataState = syncManager.dataState

    var showHelpDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<RiftTimedSunData?>(null) }

    Scaffold(
        topBar = {
            CommonEditorTopAppBar(
                title = "僵尸掉落阳光配置",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onRequestZombieSelection { selectedIds ->
                        if (selectedIds.isNotEmpty()) {
                            val newList = dataState.value.sunDrops.toMutableList()
                            selectedIds.forEach { id ->
                                val alias = ZombieRepository.buildZombieAliases(id)
                                if (newList.none { it.zombieTypeName == alias }) {
                                    newList.add(RiftTimedSunData(
                                        zombieTypeName = alias,
                                        sunDropValues = mutableListOf(0, 0, 0, 0, 0, 0)
                                    ))
                                }
                            }
                            dataState.value = dataState.value.copy(sunDrops = newList)
                            syncManager.sync()
                        }
                    }
                },
                containerColor = themeColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "僵尸掉落阳光说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "此模块用于设置特定僵尸在关卡中掉落的阳光数值，用于追击第五关。该模块的副作用是让阳光铲失效。"
                )
                HelpSection(
                    title = "数值设置",
                    body = "六个整数对应僵尸在一到六阶时的掉落的阳光，若阶级超过6则会默认使用一阶的数据。"
                )
            }
        }

        // 编辑对话框
        if (editingItem != null) {
            SunValuesEditDialog(
                item = editingItem!!,
                themeColor = themeColor,
                onDismiss = { editingItem = null },
                onConfirm = { updatedItem ->
                    val newList = dataState.value.sunDrops.toMutableList()
                    val index = newList.indexOf(editingItem)
                    if (index != -1) {
                        newList[index] = updatedItem
                        dataState.value = dataState.value.copy(sunDrops = newList)
                        syncManager.sync()
                    }
                    editingItem = null
                }
            )
        }

        if (dataState.value.sunDrops.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center
            ) {
                Text("暂无配置，点击右下角添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(dataState.value.sunDrops) { item ->
                    RiftSunItemCard(
                        item = item,
                        themeColor = themeColor,
                        onEdit = { editingItem = item },
                        onDelete = {
                            val newList = dataState.value.sunDrops.toMutableList()
                            newList.remove(item)
                            dataState.value = dataState.value.copy(sunDrops = newList)
                            syncManager.sync()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RiftSunItemCard(
    item: RiftTimedSunData,
    themeColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val info = remember(item.zombieTypeName) {
        ZombieRepository.getZombieInfoById(item.zombieTypeName)
    }

    Card(
        onClick = onEdit,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssetImage(
                path = if (info?.icon != null) "images/zombies/${info.icon}" else "images/others/unknown.webp",
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(info?.name ?: "未知僵尸", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "默认掉落: ${item.sunDropValues.getOrNull(0) ?: 0} 阳光",
                    fontSize = 12.sp,
                    color = themeColor
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}

@Composable
fun SunValuesEditDialog(
    item: RiftTimedSunData,
    themeColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (RiftTimedSunData) -> Unit
) {
    var tempValues by remember {
        mutableStateOf(
            if (item.sunDropValues.size >= 6) item.sunDropValues.toMutableList()
            else (item.sunDropValues + List(6 - item.sunDropValues.size) { 0 }).toMutableList()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑具体数值", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "配置该僵尸在不同阶级的阳光掉落量，若超过6阶则使用一阶数值",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                for (i in 0 until 6 step 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputInt(
                            value = tempValues[i],
                            onValueChange = { newValue ->
                                val updated = tempValues.toMutableList()
                                updated[i] = newValue
                                tempValues = updated
                            },
                            label = "${i + 1}阶",
                            color = themeColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (i + 1 < 6) {
                            NumberInputInt(
                                value = tempValues[i + 1],
                                onValueChange = { newValue ->
                                    val updated = tempValues.toMutableList()
                                    updated[i + 1] = newValue
                                    tempValues = updated
                                },
                                label = "${i + 2}阶",
                                color = themeColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(item.copy(sunDropValues = tempValues)) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}