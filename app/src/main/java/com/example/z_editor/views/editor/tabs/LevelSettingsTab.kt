package com.example.z_editor.views.editor.tabs

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
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.LevelDefinitionData
import com.example.z_editor.data.ModuleMetadata
import com.example.z_editor.data.ModuleRegistry
import com.example.z_editor.data.PvzObject
import com.example.z_editor.data.repository.ReferenceRepository
import com.example.z_editor.data.RtidParser
import com.example.z_editor.views.editor.pages.others.SettingEntryCard

data class ModuleUIInfo(
    val rtid: String,
    val alias: String,
    val objClass: String,
    val friendlyName: String,
    val description: String,
    val icon: ImageVector,
    val isCore: Boolean
)

data class ModuleConflictRule(
    val conflictingClasses: Set<String>,
    val title: String = "模块逻辑冲突",
    val description: String? = null
)

object ConflictRegistry {
    val rules = listOf(
        ModuleConflictRule(
            conflictingClasses = setOf("SeedBankProperties", "ConveyorSeedBankProperties"),
            description = "种子库与传送带模块的ui会相互遮挡，而且有可能闪退，需要确保种子库处于预选模式。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("VaseBreakerPresetProperties", "StandardLevelIntroProperties"),
            description = "砸罐子模式下不需要添加开局转场动画。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("LastStandMinigameProperties", "StandardLevelIntroProperties"),
            description = "坚不可摧模式下不需要添加开局转场动画。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("EvilDaveProperties", "ZombiesDeadWinConProperties"),
            description = "我是僵尸模式下不能添加僵尸掉落模块。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("EvilDaveProperties", "ZombiesAteYourBrainsProperties"),
            description = "我是僵尸模式下不能添加僵尸胜利判定。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("ZombossBattleModuleProperties", "ZombiesDeadWinConProperties"),
            description = "僵王战模式下使用死亡掉落会导致无法正常结算。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("ZombossBattleIntroProperties", "StandardLevelIntroProperties"),
            description = "两种关卡开局转场不能同时出现，否则僵王血量无法正常显示。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("InitialPlantEntryProperties", "RoofProperties"),
            description = "在屋顶无法进行预置植物，会引发闪退。"
        ),
        ModuleConflictRule(
            conflictingClasses = setOf("ProtectThePlantChallengeProperties", "RoofProperties"),
            description = "在屋顶无法进行预置植物，会引发闪退。"
        )
    )
}

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
    if (levelDef == null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "未找到关卡定义",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "当前关卡内未找到关卡定义模块 (LevelDefinition)，这是关卡文件的基础节点，缺失表示当前关卡已被毁坏。请尝试手动添加关卡定义模块。",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
    return
}

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

    val activeConflicts = remember(existingObjClasses) {
        ConflictRegistry.rules.filter { rule ->
            existingObjClasses.containsAll(rule.conflictingClasses)
        }.map { rule ->
            val displayDesc = rule.description ?: run {
                val names = rule.conflictingClasses.map { cls ->
                    ModuleRegistry.getMetadata(cls).title
                }
                "${names.joinToString(" 与 ")} 发生逻辑冲突，建议只保留其中一个。"
            }
            rule to displayDesc
        }
    }

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
                }) { Text("确认移除", color = MaterialTheme.colorScheme.onError) }
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
                color = MaterialTheme.colorScheme.primary,
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
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .clickable { onNavigateToAddModule() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddCircleOutline, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("添加新模块", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(activeConflicts) { (rule, description) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error), // 浅红色背景
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.onError, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.onError)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = rule.title,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onError,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // --- “缺失必要模块”警告 ---
        if (missingEssentials.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary), // 浅黄色
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onTertiary, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onTertiary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "缺少必要模块",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "关卡可能无法正常运行。建议添加以下模块：",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                        missingEssentials.forEach { meta ->
                            Text(
                                text = "• ${meta.title}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiary,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(info.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(info.friendlyName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(info.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(info.alias, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Close,
                    "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(info.icon, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(info.friendlyName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(info.alias, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.RemoveCircleOutline,
                null,
                tint = MaterialTheme.colorScheme.onError.copy(0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}