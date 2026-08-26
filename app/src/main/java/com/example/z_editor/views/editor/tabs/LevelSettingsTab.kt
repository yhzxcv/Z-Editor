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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.foundation.text.selection.TextSelectionColors
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
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzGreenDarkTheme
import com.example.z_editor.ui.theme.PvzGreenPrimary
import com.example.z_editor.views.editor.pages.others.SettingEntryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSettingsTab(
    levelDef: LevelDefinitionData?,
    objectMap: Map<String, PvzObject>,
    missingModules: List<ModuleMetadata>,
    invalidLevelModuleRefs: List<String>,
    scrollState: LazyListState,
    onEditBasicInfo: () -> Unit,
    onEditModule: (String) -> Unit,
    onRemoveModule: (String) -> Unit,
    onRenameModule: (String, String) -> Unit,
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
    var pendingRenameRtid by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

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

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzGreenDarkTheme else PvzGreenPrimary

    // 重命名校验：仅 @CurrentLevel 且关卡中存在对应对象时允许改名
    val renameInfo = pendingRenameRtid?.let { rtid -> currentModulesList.find { it.rtid == rtid } }
    val renameParsed = renameInfo?.let { RtidParser.parse(it.rtid) }
    val canRename = renameParsed != null &&
        renameParsed.source == "CurrentLevel" &&
        objectMap.containsKey(renameParsed.alias)

    // 重名检查：排除被改名的模块自身与它对应的对象（objectMap 首别名为 key）
    val renameDuplicate = remember(renameText, pendingRenameRtid, objectMap, levelDef) {
        val parsed = renameParsed ?: return@remember false
        val newName = renameText.trim()
        if (newName.isBlank() || newName == parsed.alias) return@remember false

        val otherModuleAliases = levelDef.modules
            .filter { it != parsed.fullString }
            .mapNotNull { RtidParser.parse(it)?.alias }
        if (newName in otherModuleAliases) return@remember true

        val otherObjectAliases = objectMap.keys - parsed.alias
        newName in otherObjectAliases
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
            title = { Text(stringResource(id = R.string.level_settings_dialog_remove_title), fontSize = 18.sp) },
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

    if (pendingRenameRtid != null && renameInfo != null) {
        val parsed = renameParsed
        val renameName = renameText.trim()
        val renameConfirmEnabled = canRename &&
            renameName.isNotBlank() &&
            renameName != parsed.alias &&
            !renameDuplicate
        AlertDialog(
            onDismissRequest = { pendingRenameRtid = null },
            title = { Text(stringResource(id = R.string.level_settings_dialog_rename_title), fontSize = 18.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = {
                            Text(
                                stringResource(id = R.string.level_settings_dialog_rename_label),
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        isError = renameDuplicate,
                        supportingText = if (renameDuplicate) {
                            {
                                Text(
                                    stringResource(id = R.string.level_settings_dialog_rename_duplicate),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            cursorColor = themeColor,
                            selectionColors = TextSelectionColors(
                                handleColor = themeColor,
                                backgroundColor = themeColor.copy(alpha = 0.4f)
                            ),
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedBorderColor = themeColor,
                            focusedLabelColor = themeColor
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!canRename) {
                        Spacer(Modifier.height(8.dp))
                        val hintRes = if (parsed?.source == "LevelModules")
                            R.string.level_settings_dialog_rename_levelmodules_hint
                        else
                            R.string.level_settings_dialog_rename_invalid_hint
                        Text(
                            text = stringResource(id = hintRes),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = renameConfirmEnabled,
                    onClick = {
                        onRenameModule(pendingRenameRtid!!, renameName)
                        pendingRenameRtid = null
                    }
                ) {
                    Text(stringResource(id = R.string.level_settings_dialog_rename_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRenameRtid = null }) {
                    Text(stringResource(id = R.string.level_settings_dialog_rename_cancel))
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
                onRename = {
                    renameText = item.alias
                    pendingRenameRtid = item.rtid
                },
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
                MiscModuleRow(
                    info = item,
                    onRename = {
                        renameText = item.alias
                        pendingRenameRtid = item.rtid
                    },
                    onDelete = { pendingDeleteRtid = item.rtid })
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

        // --- “失效模块引用”警告（Modules 指向 @CurrentLevel 但文件里没有对应对象）---
        if (invalidLevelModuleRefs.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error), // 红色
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onError, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = MaterialTheme.colorScheme.onError
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.level_settings_invalid_module_title),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(id = R.string.level_settings_invalid_module_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(Modifier.height(4.dp))
                        invalidLevelModuleRefs.forEach { rtid ->
                            Text(
                                text = "• $rtid",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.padding(start = 12.dp, top = 6.dp)
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
fun ModuleCard(
    info: ModuleUIInfo,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
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
            IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    stringResource(id = R.string.level_settings_rename),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.RemoveCircleOutline,
                    stringResource(id = R.string.level_settings_delete),
                    tint = MaterialTheme.colorScheme.onError.copy(0.5f),
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
fun MiscModuleRow(
    info: ModuleUIInfo,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(info.icon, null, tint = Color.Gray, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                info.friendlyName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(info.alias, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Edit,
                stringResource(id = R.string.level_settings_rename),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.RemoveCircleOutline,
                stringResource(id = R.string.level_settings_delete),
                tint = MaterialTheme.colorScheme.onError.copy(0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}