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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.R
import com.example.z_editor.data.LevelDefinitionData
import com.example.z_editor.data.ModuleMetadata
import com.example.z_editor.data.ModuleRegistry
import com.example.z_editor.data.PvzObject
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.repository.ConflictRegistry
import com.example.z_editor.data.repository.ModuleUIInfo
import com.example.z_editor.data.repository.ReferenceRepository
import com.example.z_editor.views.editor.pages.others.SettingEntryCard

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
    val conflictSeparator = stringResource(id = R.string.level_settings_conflict_separator)
    val conflictSuffix = stringResource(id = R.string.level_settings_conflict_suffix)
    val context = LocalContext.current

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
                    text = stringResource(id = R.string.level_settings_not_found_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.level_settings_not_found_desc),
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
                friendlyName = context.getString(metadata.titleRes),
                description = context.getString(metadata.descriptionRes),
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
                    context.getString(ModuleRegistry.getMetadata(cls).titleRes)
                }
                names.joinToString(conflictSeparator) + conflictSuffix
            }
            rule to displayDesc
        }
    }

    val missingEssentials = missingModules

    if (pendingDeleteRtid != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteRtid = null },
            title = { Text(stringResource(id = R.string.level_settings_dialog_remove_title)) },
            text = {
                Text(stringResource(id = R.string.level_settings_dialog_remove_msg))
            },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveModule(pendingDeleteRtid!!)
                    pendingDeleteRtid = null
                }) {
                    Text(
                        stringResource(id = R.string.level_settings_dialog_remove_confirm),
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRtid = null }) {
                    Text(stringResource(id = R.string.level_settings_dialog_remove_cancel))
                }
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
                title = stringResource(id = R.string.level_settings_basic_info_title),
                subtitle = stringResource(id = R.string.level_settings_basic_info_subtitle),
                icon = Icons.Default.EditNote,
                onClick = onEditBasicInfo
            )
        }

        item { Spacer(Modifier.height(8.dp)) }

        // 核心模块区域
        item {
            Text(
                text = stringResource(id = R.string.level_settings_header_editable_modules),
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
                    text = stringResource(id = R.string.level_settings_header_misc_modules),
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
                    Icon(
                        Icons.Default.AddCircleOutline,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.level_settings_add_module),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
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

        // --- “缺少必要模块”警告 ---
        if (missingEssentials.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary), // 浅黄色
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onTertiary,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = MaterialTheme.colorScheme.onTertiary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.level_settings_missing_essentials_title),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.level_settings_missing_essentials_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                        missingEssentials.forEach { meta ->
                            Text(
                                text = "• ${stringResource(id = meta.titleRes)}",
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
            Icon(
                info.icon,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    info.friendlyName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    info.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    info.alias,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Close,
                    stringResource(id = R.string.level_settings_delete),
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
            Text(
                info.friendlyName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(info.alias, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.RemoveCircleOutline,
                stringResource(id = R.string.level_settings_delete),
                tint = MaterialTheme.colorScheme.onError.copy(0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}