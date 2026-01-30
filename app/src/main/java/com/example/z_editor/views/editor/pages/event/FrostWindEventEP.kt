package com.example.z_editor.views.editor.pages.event

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.FrostWindData
import com.example.z_editor.data.FrostWindWaveActionPropsData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.StepperControl
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrostWindEventEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    val currentAlias = RtidParser.parse(rtid)?.alias ?: "FrostWindEvent"

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, FrostWindWaveActionPropsData::class.java)
    val eventDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    val themeColor = MaterialTheme.colorScheme.secondary

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            CommonEditorTopAppBar(
                title = "编辑 $currentAlias",
                subtitle = "事件类型：寒风侵袭",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "寒风侵袭事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "功能描述",
                    body = "冰河世界专属事件。在指定行生成寒风，寒风会携带冰冻效果，将植物冻结成冰块。"
                )
                HelpSection(
                    title = "参数详解",
                    body = "可以设置寒风来袭的方向，可以选择从左或从右。注意每一次寒风之间会会有一定间隔，若想要实现同时可以尝试添加多个寒风事件。"
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (eventDataState.value.winds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无寒风配置", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                eventDataState.value.winds.forEachIndexed { index, wind ->
                    WindCard(
                        wind = wind,
                        index = index,
                        themeColor = themeColor,
                        onUpdate = { updatedWind ->
                            val newList = eventDataState.value.winds.toMutableList()
                            newList[index] = updatedWind
                            eventDataState.value = eventDataState.value.copy(winds = newList)
                            sync()
                        },
                        onDelete = {
                            val newList = eventDataState.value.winds.toMutableList()
                            newList.removeAt(index)
                            eventDataState.value = eventDataState.value.copy(winds = newList)
                            sync()
                        }
                    )
                }
            }
            Button(
                onClick = {
                    val newList = eventDataState.value.winds.toMutableList()
                    newList.add(FrostWindData(row = 2, direction = "right"))
                    eventDataState.value = eventDataState.value.copy(winds = newList)
                    sync()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("添加寒风")
            }
        }
    }
}

@Composable
fun WindCard(
    wind: FrostWindData,
    index: Int,
    themeColor: Color,
    onUpdate: (FrostWindData) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AcUnit, null, tint = themeColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "寒风 #${index + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = themeColor
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color.LightGray)
                }
            }

            Spacer(Modifier.height(12.dp))

            StepperControl(
                label = "所在行 (Row ${wind.row + 1})",
                valueText = "${wind.row + 1}行",
                onMinus = {
                    val newRow = (wind.row - 1).coerceAtLeast(0)
                    onUpdate(wind.copy(row = newRow))
                },
                onPlus = {
                    val newRow = (wind.row + 1).coerceAtMost(4)
                    onUpdate(wind.copy(row = newRow))
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "风向",
                    modifier = Modifier.padding(start = 12.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))

                DirectionOption(
                    label = "Left",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    isSelected = wind.direction == "left",
                    color = themeColor,
                    onClick = { onUpdate(wind.copy(direction = "left")) }
                )

                Spacer(Modifier.width(8.dp))

                DirectionOption(
                    label = "Right",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    isSelected = wind.direction == "right" || wind.direction.isEmpty(),
                    color = themeColor,
                    onClick = { onUpdate(wind.copy(direction = "right")) }
                )
            }
        }
    }
}

@Composable
fun DirectionOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) color else Color.Transparent)
            .border(1.dp, if (isSelected) color else Color.LightGray, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}