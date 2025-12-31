package com.example.pvz2leveleditor.views.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pvz2leveleditor.data.LevelDefinitionData
import com.example.pvz2leveleditor.data.ModuleMetadata
import com.example.pvz2leveleditor.data.ModuleRegistry
import com.example.pvz2leveleditor.data.PvzObject
import com.example.pvz2leveleditor.data.repository.ReferenceRepository
import com.example.pvz2leveleditor.data.RtidParser

// ModuleUIInfo 保持不变
data class ModuleUIInfo(
    val rtid: String,
    val alias: String,
    val objClass: String,
    val friendlyName: String,
    val description: String,
    val icon: ImageVector,
    val isCore: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSettingsTab(
    levelDef: LevelDefinitionData?,
    objectMap: Map<String, PvzObject>,
    missingModules: List<ModuleMetadata>,
    scrollState: LazyListState,
    onEditBasicInfo: () -> Unit,
    onEditModule: (String) -> Unit,
    onRemoveModule: (String) -> Unit,
    onNavigateToAddModule: () -> Unit
) {
    if (levelDef == null) return

    var pendingDeleteRtid by remember { mutableStateOf<String?>(null) }

    val currentModulesList = remember(levelDef.modules) {
        levelDef.modules.map { rtid ->
            val info = RtidParser.parse(rtid)
            val alias = info?.alias ?: "Unknown"
            val objClass = if (info?.source == "CurrentLevel") {
                objectMap[alias]?.objClass
            } else {
                ReferenceRepository.getObjClass(alias)
            } ?: "UnknownObject"

            val metadata = ModuleRegistry.getMetadata(objClass)

            ModuleUIInfo(
                rtid = rtid,
                alias = alias,
                objClass = objClass,
                friendlyName = metadata.title,
                description = metadata.description,
                icon = metadata.icon,
                isCore = metadata.isCore
            )
        }
    }

    val coreModules = currentModulesList.filter { it.isCore }
    val miscModules = currentModulesList.filter { !it.isCore }

    val existingObjClasses = remember(currentModulesList) {
        currentModulesList.map { it.objClass }.toSet()
    }

    val hasSeedBank = existingObjClasses.contains("SeedBankProperties")
    val hasConveyor = existingObjClasses.contains("ConveyorSeedBankProperties")
    val hasModuleConflict = hasSeedBank && hasConveyor

    val missingEssentials = missingModules

    if (pendingDeleteRtid != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteRtid = null },
            title = { Text("移除模块") },
            text = {
                Text("确定要移除该模块吗？\n\n如果是本地自定义模块(@CurrentLevel)，其关联的数据配置也会被一并删除，且不可恢复。")
            },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveModule(pendingDeleteRtid!!)
                    pendingDeleteRtid = null
                }) { Text("确认移除", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRtid = null }) { Text("取消") }
            }
        )
    }

    // --- 主界面列表 ---
    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingEntryCard(
                title = "关卡基本信息",
                subtitle = "名称、序号、描述、地图背景",
                icon = Icons.Default.EditNote,
                onClick = onEditBasicInfo
            )
        }

        item { Spacer(Modifier.height(8.dp)) }

        // 核心模块区域
        item {
            Text(
                "可用编辑模块",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF388E3C)
            )
        }
        items(coreModules) { item ->
            ModuleCard(
                info = item,
                onClick = { onEditModule(item.rtid) },
                onDelete = { pendingDeleteRtid = item.rtid })
        }

        item { Spacer(Modifier.height(8.dp)) }

        // 杂项模块区域
        if (miscModules.isNotEmpty()) {
            item {
                Text(
                    "默认参数模块",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Gray
                )
            }
            items(miscModules) { item ->
                MiscModuleRow(info = item, onDelete = { pendingDeleteRtid = item.rtid })
            }
        }

        // --- “添加模块”按钮 ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(1.dp, Color(0xFF388E3C), RoundedCornerShape(8.dp))
                    .clickable { onNavigateToAddModule() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddCircleOutline, null, tint = Color(0xFF388E3C))
                    Spacer(Modifier.width(8.dp))
                    Text("添加新模块", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
                }
            }
        }

        if (hasModuleConflict) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), // 浅红色背景
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFEF5350), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = Color(0xFFD32F2F))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "模块逻辑冲突",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "种子库与传送带模块同时存在，在游戏中会导致 UI 显示异常或逻辑错误，可能引发闪退。",
                            fontSize = 12.sp,
                            color = Color(0xFFB71C1C),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // --- “缺失必要模块”警告 ---
        if (missingEssentials.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)), // 浅黄色
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF57F17), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFFBC02D))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "缺少必要模块",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF57F17)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "关卡可能无法正常运行。建议添加以下模块：",
                            fontSize = 12.sp,
                            color = Color(0xFFF57F17)
                        )
                        missingEssentials.forEach { meta ->
                            Text(
                                text = "• ${meta.title}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF57F17),
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 核心模块大卡片
 */
@Composable
fun ModuleCard(info: ModuleUIInfo, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(info.icon, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(info.friendlyName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(info.description, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                Text(info.alias, fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Close,
                    "删除",
                    tint = Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 次要模块小行
 */
@Composable
fun MiscModuleRow(info: ModuleUIInfo, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(info.icon, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(info.friendlyName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(info.alias, fontSize = 10.sp, color = Color.LightGray)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.RemoveCircleOutline,
                null,
                tint = Color(0xFFFFCDD2),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}