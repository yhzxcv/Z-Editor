package com.example.z_editor.views.editor.pages.module

import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.PennyClassroomModuleData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.repository.PlantRepository
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzCyanDark
import com.example.z_editor.ui.theme.PvzCyanLight
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import rememberJsonSync
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PennyClassroomModulePropertiesEP(
    rtid: String,
    rootLevelFile: PvzLevelFile,
    onBack: () -> Unit,
    onRequestPlantSelection: ((List<String>) -> Unit) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""

    // 使用 rememberJsonSync 处理数据同步
    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, PennyClassroomModuleData::class.java)
    val dataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    var batchLevelFloat by remember { mutableFloatStateOf(1f) }
    var showBatchConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzCyanDark else PvzCyanLight

    fun executeBatchUpdate() {
        val targetLevel = batchLevelFloat.roundToInt()
        val newMap = LinkedHashMap(dataState.value.plantMap)
        for (key in newMap.keys) {
            newMap[key] = targetLevel
        }
        dataState.value = dataState.value.copy(plantMap = newMap)
        sync()
        Toast.makeText(
            context,
            "已将所有植物设为 $targetLevel 阶",
            Toast.LENGTH_SHORT
        ).show()
        showBatchConfirmDialog = false
    }

    if (showBatchConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBatchConfirmDialog = false },
            icon = { Icon(Icons.Default.Check, null) },
            title = { Text("确认批量设置") },
            text = {
                val level = batchLevelFloat.roundToInt()
                Text("此操作将把列表中已添加的所有 ${dataState.value.plantMap.size} 种植物的等级统一设置为 $level 阶。")
            },
            confirmButton = {
                Button(
                    onClick = { executeBatchUpdate() },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    Text("确认覆盖")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchConfirmDialog = false }) {
                    Text(
                        "取消",
                        color = themeColor
                    )
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            CommonEditorTopAppBar(
                title = "阶级定义设置",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "阶级定义模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "此模块用于定义植物的全局等级。它通常优于种子库中的等级设置，且可以针对特定植物单独设置等级。"
                )
                HelpSection(
                    title = "生效范围",
                    body = "设置的等级将应用于关卡内玩家使用的该种植物，包括保护植物、种子掉落等。"
                )
                HelpSection(
                    title = "列表管理",
                    body = "只有添加到列表中的植物才会应用特定等级。未添加的植物将使用关卡默认等级或玩家自身存档等级。"
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // === 顶部：批量控制与添加按钮 ===
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Layers,
                            null,
                            tint = themeColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "批量设置: ${batchLevelFloat.roundToInt()} 阶",
                            fontWeight = FontWeight.Bold,
                            color = themeColor
                        )
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { showBatchConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(36.dp),
                            enabled = dataState.value.plantMap.isNotEmpty()
                        ) {
                            Text("应用批量设置")
                        }
                    }

                    Slider(
                        value = batchLevelFloat,
                        onValueChange = { batchLevelFloat = it },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = themeColor,
                            activeTrackColor = themeColor,
                        ),
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            onRequestPlantSelection { selectedIds ->
                                val newMap = LinkedHashMap(dataState.value.plantMap)
                                var addedCount = 0
                                selectedIds.forEach { id ->
                                    if (!newMap.containsKey(id)) {
                                        newMap[id] = batchLevelFloat.roundToInt()
                                        addedCount++
                                    }
                                }
                                if (addedCount > 0) {
                                    dataState.value = dataState.value.copy(plantMap = newMap)
                                    sync()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = themeColor
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            themeColor.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("添加植物到列表")
                    }
                }
            }

            // === 列表区域 ===
            val plantList = dataState.value.plantMap.entries.toList()

            if (plantList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Layers,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "暂无配置，请添加植物",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = plantList,
                        key = { it.key }
                    ) { (plantId, level) ->
                        PlantLevelRow(
                            plantId = plantId,
                            level = level,
                            onLevelChange = { newLevel ->
                                val newMap = LinkedHashMap(dataState.value.plantMap)
                                newMap[plantId] = newLevel
                                dataState.value = dataState.value.copy(plantMap = newMap)
                                sync()
                            },
                            onDelete = {
                                val newMap = LinkedHashMap(dataState.value.plantMap)
                                newMap.remove(plantId)
                                dataState.value = dataState.value.copy(plantMap = newMap)
                                sync()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlantLevelRow(
    plantId: String,
    level: Int,
    onLevelChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    val plantName = remember(plantId) { PlantRepository.getName(plantId) }
    val info = remember(plantId) { PlantRepository.getPlantInfoById(plantId) }
    val iconPath = if (info?.icon != null) "images/plants/${info.icon}" else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (iconPath != null) {
                    AssetImage(
                        path = iconPath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        filterQuality = FilterQuality.Medium,
                        placeholder = {
                            Text(
                                plantName.take(1),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plantName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Text(
                    text = plantId,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Stepper(
                value = level,
                onChange = { newValue ->
                    if (newValue in 1..5) {
                        onLevelChange(newValue)
                    }
                }
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    null,
                    tint = Color.LightGray.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .border(1.dp, Color.LightGray.copy(0.5f), RoundedCornerShape(6.dp))
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(32.dp)
                .clickable { onChange(value - 1) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "-",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "$value 阶",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(32.dp)
                .clickable { onChange(value + 1) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "+",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}