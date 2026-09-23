package com.example.z_editor.views.editor.pages.others

import android.widget.Toast
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
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.R
import com.example.z_editor.data.LevelParser
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.PvzObject
import com.example.z_editor.data.ZombieJitterOffsetsData
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzOrangeDark
import com.example.z_editor.ui.theme.PvzOrangeLight
import com.google.gson.Gson
import rememberJsonSync

private val gson = Gson()

/**
 * 出怪间隔偏移预设管理页。
 *
 * 预设是关卡内独立的 ZombieJitterOffsets 对象（不进 LevelDefinition.Modules），
 * 由自然出怪事件的 JitterOffsets 键用 RTID 引用。这里管理预设的增删改，
 * 以及「被哪些事件引用」的可见性 —— 被引用的预设不允许删除，必须先解绑。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZombieJitterOffsetsEP(
    rootLevelFile: PvzLevelFile,
    onBack: () -> Unit,
    onObjectsChanged: () -> Unit,
    scrollState: ScrollState
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    fun refresh() {
        refreshTrigger++
        onObjectsChanged()
    }

    val presets = remember(rootLevelFile.objects, refreshTrigger) {
        LevelParser.listJitterOffsetsPresets(rootLevelFile)
    }

    // 别名 → 引用它的事件对象；增删预设后要重算
    val referrersByAlias = remember(rootLevelFile.objects, refreshTrigger) {
        presets.associate { obj ->
            val alias = obj.aliases?.firstOrNull().orEmpty()
            alias to LevelParser.findJitterOffsetsReferrers(rootLevelFile, alias)
        }
    }

    var renaming by remember { mutableStateOf<PvzObject?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf<String?>(null) }

    var pendingDelete by remember { mutableStateOf<PvzObject?>(null) }
    var blockedDelete by remember { mutableStateOf<Pair<PvzObject, List<PvzObject>>?>(null) }

    /** 别名词自动生成：Offsets、Offsets2、Offsets3… 与 WaveManagerProps 的命名法一致。 */
    fun newAliasFor(): String {
        val taken = rootLevelFile.objects.flatMap { it.aliases ?: emptyList() }.toHashSet()
        var alias = "Offsets"
        var count = 0
        while (alias in taken) {
            count++
            alias = "Offsets$count"
        }
        return alias
    }

    fun addPreset() {
        val alias = newAliasFor()
        rootLevelFile.objects.add(
            PvzObject(
                aliases = listOf(alias),
                objClass = "ZombieJitterOffsets",
                objData = gson.toJsonTree(ZombieJitterOffsetsData())
            )
        )
        refresh()
        Toast.makeText(
            context,
            context.getString(R.string.jitter_msg_created, alias),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun requestDelete(obj: PvzObject) {
        val alias = obj.aliases?.firstOrNull().orEmpty()
        val referrers = LevelParser.findJitterOffsetsReferrers(rootLevelFile, alias)
        if (referrers.isNotEmpty()) {
            blockedDelete = obj to referrers
        } else {
            pendingDelete = obj
        }
    }

    fun confirmDelete(obj: PvzObject) {
        rootLevelFile.objects.removeAll { it === obj }
        pendingDelete = null
        refresh()
    }

    /** 返回错误信息；null 表示改名成功。 */
    fun applyRename(obj: PvzObject, rawInput: String): String? {
        val newAlias = rawInput.trim()
        val oldAlias = obj.aliases?.firstOrNull().orEmpty()
        if (newAlias.isEmpty()) return context.getString(R.string.jitter_rename_err_empty)
        if (newAlias.any { it == '@' || it == '(' || it == ')' }) {
            return context.getString(R.string.jitter_rename_err_bad_chars)
        }
        if (newAlias == oldAlias) return null
        val taken = rootLevelFile.objects
            .filter { it !== obj }
            .flatMap { it.aliases ?: emptyList() }
            .toHashSet()
        if (newAlias in taken) {
            return context.getString(R.string.jitter_rename_err_taken, newAlias)
        }

        obj.aliases = listOf(newAlias) + (obj.aliases?.drop(1) ?: emptyList())
        val synced = LevelParser.renameJitterOffsetsReferences(rootLevelFile, oldAlias, newAlias)
        refresh()
        if (synced > 0) {
            Toast.makeText(
                context,
                context.getString(R.string.jitter_msg_renamed, synced),
                Toast.LENGTH_SHORT
            ).show()
        }
        return null
    }

    val themeColor = if (LocalDarkTheme.current) PvzOrangeDark else PvzOrangeLight

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        contentWindowInsets = EditorContentWindowInsets(),
        topBar = {
            CommonEditorTopAppBar(
                title = stringResource(R.string.jitter_page_title),
                subtitle = stringResource(R.string.jitter_page_subtitle, presets.size),
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = stringResource(R.string.jitter_help_title),
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    stringResource(R.string.jitter_help_intro_title),
                    stringResource(R.string.jitter_help_intro_body)
                )
                HelpSection(
                    stringResource(R.string.jitter_help_jitter_title),
                    stringResource(R.string.jitter_help_jitter_body)
                )
                HelpSection(
                    stringResource(R.string.jitter_help_optional_title),
                    stringResource(R.string.jitter_help_optional_body)
                )
                HelpSection(
                    stringResource(R.string.jitter_help_ref_title),
                    stringResource(R.string.jitter_help_ref_body)
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (presets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.jitter_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }

            presets.forEach { preset ->
                val alias = preset.aliases?.firstOrNull().orEmpty()
                JitterOffsetsPresetCard(
                    preset = preset,
                    alias = alias,
                    referrers = referrersByAlias[alias].orEmpty(),
                    themeColor = themeColor,
                    onRenameClick = {
                        renaming = preset
                        renameInput = alias
                        renameError = null
                    },
                    onDeleteClick = { requestDelete(preset) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(1.dp, themeColor, RoundedCornerShape(8.dp))
                    .clickable { addPreset() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddCircleOutline, null, tint = themeColor)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.jitter_btn_add),
                        color = themeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // === 改名对话框 ===
    renaming?.let { target ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = {
                Text(
                    stringResource(R.string.jitter_rename_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        stringResource(R.string.jitter_rename_desc),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = {
                            renameInput = it
                            renameError = null
                        },
                        label = { Text(stringResource(R.string.jitter_rename_label)) },
                        singleLine = true,
                        isError = renameError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    renameError?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val error = applyRename(target, renameInput)
                    if (error == null) renaming = null else renameError = error
                }) { Text(stringResource(R.string.jitter_btn_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) {
                    Text(stringResource(R.string.jitter_btn_cancel))
                }
            }
        )
    }

    // === 删除确认（仅未被引用时） ===
    pendingDelete?.let { target ->
        val alias = target.aliases?.firstOrNull().orEmpty()
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = {
                Text(
                    stringResource(R.string.jitter_delete_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    stringResource(R.string.jitter_delete_desc, alias),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { confirmDelete(target) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.jitter_btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.jitter_btn_cancel))
                }
            }
        )
    }

    // === 删除被拒：还被引用着，先解绑 ===
    blockedDelete?.let { (target, referrers) ->
        val alias = target.aliases?.firstOrNull().orEmpty()
        AlertDialog(
            onDismissRequest = { blockedDelete = null },
            title = {
                Text(
                    stringResource(R.string.jitter_delete_blocked_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        stringResource(
                            R.string.jitter_delete_blocked_desc,
                            alias,
                            referrers.size
                        ),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.jitter_delete_blocked_hint),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    referrers.take(10).forEach {
                        Text(
                            "• ${it.aliases?.firstOrNull() ?: it.objClass}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (referrers.size > 10) {
                        Text(
                            "…",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { blockedDelete = null }) {
                    Text(stringResource(R.string.jitter_btn_got_it))
                }
            }
        )
    }
}

@Composable
private fun JitterOffsetsPresetCard(
    preset: PvzObject,
    alias: String,
    referrers: List<PvzObject>,
    themeColor: Color,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val syncManager = rememberJsonSync(preset, ZombieJitterOffsetsData::class.java)
    val data = syncManager.dataState.value

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        alias.ifEmpty { stringResource(R.string.jitter_label_no_alias) },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (alias.isEmpty()) MaterialTheme.colorScheme.error else themeColor
                    )
                    Spacer(Modifier.height(2.dp))
                    if (referrers.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.jitter_status_unused),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            stringResource(R.string.jitter_status_referenced, referrers.size),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onRenameClick, enabled = alias.isNotEmpty()) {
                    Icon(
                        Icons.Default.DriveFileRenameOutline,
                        contentDescription = stringResource(R.string.jitter_cd_rename),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.jitter_cd_delete),
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            }

            // 改动时从 dataState 现取现值再 copy：两个输入框若在同一帧内先后触发，
            // 用组合期捕获的 data 快照会把前一次的改动覆盖回去
            NumberInputInt(
                data.offsetIncrement,
                { value ->
                    syncManager.dataState.value =
                        syncManager.dataState.value.copy(offsetIncrement = value)
                    syncManager.sync()
                },
                stringResource(R.string.jitter_label_increment),
                color = themeColor,
                modifier = Modifier.fillMaxWidth()
            )

            NumberInputInt(
                data.randomJitter,
                { value ->
                    syncManager.dataState.value =
                        syncManager.dataState.value.copy(randomJitter = value)
                    syncManager.sync()
                },
                stringResource(R.string.jitter_label_jitter),
                color = themeColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
