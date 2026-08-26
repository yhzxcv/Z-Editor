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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.EventRegistry
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.ThunderItem
import com.example.z_editor.data.ThunderWaveActionPropsData
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzGrayDark
import com.example.z_editor.ui.theme.PvzGrayLight
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import rememberJsonSync

// ======================== 编辑器界面 ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThunderEventEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: LazyListState
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, ThunderWaveActionPropsData::class.java)
    val actionDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    val isDark = LocalDarkTheme.current
    // 主题色跟随事件注册表的金色闪电配色，与事件列表页保持一致
    val eventMeta = remember { EventRegistry.getMetadata("ThunderWaveActionProps") }
    val themeColor = if (isDark) eventMeta?.darkColor ?: PvzGrayDark else eventMeta?.color ?: PvzGrayLight

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        // 底部内边距统一走 contentWindowInsets：键盘弹出时自动在底部留出空间，避免遮挡输入框
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        topBar = {
            CommonEditorTopAppBar(
                title = "编辑 $currentAlias",
                subtitle = "事件类型：雷暴",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "雷暴事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "此事件用于在关卡中产生随机雷暴，闪电会随机劈向场地。Thunders 中每一组代表一次闪电，填几组就产生几次。"
                )
                HelpSection(
                    title = "闪电极性",
                    body = "每组的 Type 表示闪电极性：positive（正极）或 negative（负极），可自由组合。"
                )
            }
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (actionDataState.value.thunders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无闪电配置", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(actionDataState.value.thunders.size) { index ->
                    val thunder = actionDataState.value.thunders[index]
                    ThunderCard(
                        thunder = thunder,
                        index = index,
                        themeColor = themeColor,
                        onUpdate = { updatedThunder ->
                            val newList = actionDataState.value.thunders.toMutableList()
                            newList[index] = updatedThunder
                            actionDataState.value = actionDataState.value.copy(thunders = newList)
                            sync()
                        },
                        onDelete = {
                            val newList = actionDataState.value.thunders.toMutableList()
                            newList.removeAt(index)
                            actionDataState.value = actionDataState.value.copy(thunders = newList)
                            sync()
                        }
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val newList = actionDataState.value.thunders.toMutableList()
                        newList.add(ThunderItem(type = "positive"))
                        actionDataState.value = actionDataState.value.copy(thunders = newList)
                        sync()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("添加闪电")
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
fun ThunderCard(
    thunder: ThunderItem,
    index: Int,
    themeColor: Color,
    onUpdate: (ThunderItem) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, null, tint = themeColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "闪电 #${index + 1}",
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "闪电极性",
                    modifier = Modifier.padding(start = 12.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))

                PolarityOption(
                    label = "positive",
                    icon = Icons.Default.Add,
                    isSelected = thunder.type == "positive",
                    color = themeColor,
                    onClick = { onUpdate(thunder.copy(type = "positive")) }
                )

                Spacer(Modifier.width(8.dp))

                PolarityOption(
                    label = "negative",
                    icon = Icons.Default.Remove,
                    isSelected = thunder.type == "negative",
                    color = themeColor,
                    onClick = { onUpdate(thunder.copy(type = "negative")) }
                )
            }
        }
    }
}

@Composable
fun PolarityOption(
    label: String,
    icon: ImageVector,
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
