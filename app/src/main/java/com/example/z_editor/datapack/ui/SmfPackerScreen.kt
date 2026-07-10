package com.example.z_editor.datapack.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.example.z_editor.datapack.smf.SmfPacker
import com.example.z_editor.ui.theme.PvzBluePrimary
import com.example.z_editor.views.components.rememberDebouncedClick
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SMF/RSB Packer screen.
 *
 * Directory structure (auto-created under SAF root):
 *   packer/original/  — user places template .rsb/.smf files here
 *   packer/patches/   — user places modified patch files here (flat, basename match)
 *   packer/output/    — packed output files land here
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmfPackerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val handleBack = rememberDebouncedClick { onBack() }
    BackHandler(onBack = handleBack)

    val prefs = remember { context.getSharedPreferences("datapack_prefs", Context.MODE_PRIVATE) }
    val mainPrefs = remember { context.getSharedPreferences("prefs", Context.MODE_PRIVATE) }

    // SAF root
    var rootFolderUri by remember {
        mutableStateOf(
            mainPrefs.getString("folder_uri", null)?.toUri()
                ?: prefs.getString("datapack_folder_uri", null)?.toUri()
        )
    }

    // Sub-directory URIs
    var originalDirUri by remember { mutableStateOf<Uri?>(null) }
    var patchesDirUri by remember { mutableStateOf<Uri?>(null) }
    var outputDirUri by remember { mutableStateOf<Uri?>(null) }

    // Template files in packer/original/
    var templateFiles by remember { mutableStateOf<List<DisplayFile>>(emptyList()) }
    var selectedTemplate by remember { mutableStateOf<DisplayFile?>(null) }

    // Patch files in packer/patches/
    var patchFiles by remember { mutableStateOf<List<DisplayFile>>(emptyList()) }
    var patchCount by remember { mutableStateOf(0) }

    // Output name
    var outputName by remember { mutableStateOf("") }

    // State
    var isLoading by remember { mutableStateOf(true) }
    var isPacking by remember { mutableStateOf(false) }
    var packResult by remember { mutableStateOf<SmfPacker.PackResult?>(null) }
    var packError by remember { mutableStateOf<String?>(null) }
    var conflictErrors by remember { mutableStateOf<List<SmfPacker.ConflictError>>(emptyList()) }

    // Key dialog
    var encryptionKey by remember { mutableStateOf(prefs.getString("encryption_key", "") ?: "") }
    var showKeyDialog by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf(encryptionKey) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val themeColor = PvzBluePrimary

    // ---- Helpers ----

    /** Ensure packer/ subdirectories exist, create if missing */
    fun ensurePackerDirs() {
        val root = rootFolderUri ?: return
        val rootDoc = DocumentFile.fromTreeUri(context, root) ?: return

        fun getOrCreateDir(parent: DocumentFile, name: String): DocumentFile? {
            val existing = parent.findFile(name)
            if (existing != null && existing.isDirectory) return existing
            return parent.createDirectory(name)
        }

        val packerDir = getOrCreateDir(rootDoc, "packer") ?: return
        originalDirUri = getOrCreateDir(packerDir, "original")?.uri
        patchesDirUri = getOrCreateDir(packerDir, "patches")?.uri
        outputDirUri = getOrCreateDir(packerDir, "output")?.uri
    }

    fun scanTemplates() {
        val dirUri = originalDirUri ?: return
        isLoading = true
        scope.launch {
            val files = withContext(Dispatchers.IO) {
                val dir = DocumentFile.fromTreeUri(context, dirUri) ?: return@withContext emptyList<DisplayFile>()
                dir.listFiles()
                    .filter { it.isFile }
                    .mapNotNull { f ->
                        val name = f.name ?: return@mapNotNull null
                        DisplayFile(name, f.uri, f.length())
                    }
                    .sortedBy { it.name.lowercase() }
            }
            templateFiles = files
            // Auto-select first template if none selected
            if (selectedTemplate == null && files.isNotEmpty()) {
                selectedTemplate = files.first()
                // Output same name as template
                outputName = files.first().name
            }
            isLoading = false
        }
    }

    fun scanPatches() {
        val dirUri = patchesDirUri ?: return
        scope.launch {
            val files = withContext(Dispatchers.IO) {
                val dir = DocumentFile.fromTreeUri(context, dirUri) ?: return@withContext emptyList<DisplayFile>()
                dir.listFiles()
                    .filter { it.isFile }
                    .mapNotNull { f ->
                        val name = f.name ?: return@mapNotNull null
                        DisplayFile(name, f.uri, f.length())
                    }
                    .sortedBy { it.name.lowercase() }
            }
            patchFiles = files
            patchCount = files.size
        }
    }

    fun doPack() {
        val template = selectedTemplate ?: return
        val outDir = outputDirUri ?: return
        if (outputName.isBlank()) {
            Toast.makeText(context, "请输入输出文件名", Toast.LENGTH_SHORT).show()
            return
        }
        // Ensure .smf extension
        val finalName = if (outputName.endsWith(".smf", true) || outputName.endsWith(".rsb", true))
            outputName else "$outputName.smf"

        isPacking = true
        packResult = null
        packError = null
        conflictErrors = emptyList()

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                SmfPacker.packSmf(
                    context = context,
                    templateUri = template.uri,
                    patchDirUri = patchesDirUri!!,
                    outputUri = outDir,
                    outputName = finalName
                )
            }

            isPacking = false

            result.fold(
                onSuccess = { r ->
                    packResult = r
                    if (r.patchesApplied == 0 && r.subgroupsModified == 0) {
                        Toast.makeText(context, "未检测到匹配的补丁文件，已保存原文件", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context,
                            "打包完成！${r.subgroupsModified} 个子组, ${r.patchesApplied} 个文件补丁已注入", Toast.LENGTH_LONG).show()
                    }
                    // Refresh patches (in case user wants to pack again with different patches)
                    scanPatches()
                },
                onFailure = { e ->
                    val msg = e.message ?: "未知错误"
                    packError = msg
                    Toast.makeText(context, "打包失败: $msg", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // ---- Init ----
    LaunchedEffect(Unit) {
        ensurePackerDirs()
        scanTemplates()
        scanPatches()
    }

    // ---- UI ----
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据包补丁", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scanTemplates()
                        scanPatches()
                    }) {
                        Icon(Icons.Default.Refresh, "刷新", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = { keyInput = encryptionKey; showKeyDialog = true }) {
                        Icon(Icons.Default.Key, "密钥",
                            tint = if (encryptionKey.isNotBlank()) MaterialTheme.colorScheme.tertiary
                                   else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                    }
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "帮助", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColor,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ---- Template selection ----
            item {
                SectionHeader(icon = Icons.Default.InsertDriveFile, title = "选择数据包",
                    subtitle = "来自 packer/original/")
            }

            if (templateFiles.isEmpty()) {
                item {
                    EmptyHint(
                        icon = Icons.Default.FolderOff,
                        message = "请将 .rsb.smf 数据包放入",
                        path = "packer/original/"
                    )
                }
            } else {
                items(templateFiles) { file ->
                    val isSelected = selectedTemplate == file
                    TemplateCard(
                        file = file,
                        isSelected = isSelected,
                        themeColor = themeColor,
                        onClick = {
                            selectedTemplate = file
                            outputName = file.name
                        }
                    )
                }
            }

            // ---- Patches ----
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(icon = Icons.Default.Folder, title = "补丁文件",
                    subtitle = "来自 packer/patches/")
            }

            if (patchFiles.isEmpty()) {
                item {
                    EmptyHint(
                        icon = Icons.Default.NoteAdd,
                        message = "请将修改后的补丁文件放入",
                        path = "packer/patches/"
                    )
                }
            } else {
                // List individual patch files
                items(patchFiles.take(50)) { file ->
                    PatchFileRow(file)
                }
                if (patchFiles.size > 50) {
                    item {
                        Text("... 还有 ${patchFiles.size - 50} 个文件未显示",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp))
                    }
                }
                item {
                    Text(
                        "$patchCount 个补丁文件就绪，按文件名自动匹配",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }

            // ---- Output name ----
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(icon = Icons.Default.SaveAlt, title = "输出文件名",
                    subtitle = "将保存到 packer/output/")
            }

            item {
                OutlinedTextField(
                    value = outputName,
                    onValueChange = { outputName = it },
                    label = { Text("文件名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColor,
                        focusedLabelColor = themeColor,
                        cursorColor = themeColor
                    )
                )
            }

            // ---- Conflict warnings ----
            if (conflictErrors.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null,
                                    tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("文件名冲突", fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiary)
                            }
                            Spacer(Modifier.height(8.dp))
                            conflictErrors.forEach { c ->
                                Text(
                                    "· '${c.basename}' 同时存在于: ${c.subgroups.joinToString(", ")}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // ---- Result ----
            packResult?.let { result ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null,
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("打包成功", fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 16.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            ResultRow("输出文件", result.outputName)
                            ResultRow("补丁注入", "${result.patchesApplied} 个文件")
                            ResultRow("子组修改", "${result.subgroupsModified} 个")
                            ResultRow("输出大小", formatSize(result.outputSize))
                        }
                    }
                }
            }

            packError?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, null,
                                    tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("打包失败", fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onError, fontSize = 16.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(error, fontSize = 13.sp, color = MaterialTheme.colorScheme.onError)
                        }
                    }
                }
            }

            // ---- Pack button ----
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { doPack() },
                    enabled = selectedTemplate != null && !isPacking,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isPacking) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("打包中...", fontSize = 16.sp)
                    } else {
                        Icon(Icons.Default.Archive, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("开始打包", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ---- Info footer ----
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "补丁文件按文件名 (basename) 自动匹配数据包内文件。\n" +
                        "请确保补丁文件名与数据包内文件同名。\n" +
                        "输出文件将保存到 packer/output/ 目录。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = themeColor, strokeWidth = 4.dp)
            }
        }
    }

    // ---- Help dialog ----
    if (showHelpDialog) {
        EditorHelpDialog(
            title = "数据包补丁说明",
            onDismiss = { showHelpDialog = false },
            themeColor = themeColor
        ) {
            HelpSection(
                title = "功能介绍",
                body = "数据包补丁工具用于向游戏数据包（SMF/RSB 容器文件）中注入修改后的文件。你不需要解包整个容器，只需提供要替换的文件即可。"
            )
            HelpSection(
                title = "使用步骤",
                body = "1. 将原始数据包文件（.smf 或 .rsb）放入 packer/original/ 目录\n" +
                    "2. 将修改后的补丁文件放入 packer/patches/ 目录\n" +
                    "3. 补丁文件名需与数据包内文件同名（按 basename 自动匹配）\n" +
                    "4. 选择数据包模板，确认输出文件名，点击「开始打包」"
            )
            HelpSection(
                title = "目录结构",
                body = "packer/original/ — 存放原始数据包模板\n" +
                    "packer/patches/ — 存放修改后的补丁文件\n" +
                    "packer/output/ — 打包输出目录"
            )
            HelpSection(
                title = "注意事项",
                body = "• 补丁按文件名自动匹配，不区分大小写\n" +
                    "• 输出文件默认与模板同名，可手动修改\n" +
                    "• 如多个子组中存在同名文件，将报告冲突\n" +
                    "• 操作前建议备份原始文件"
            )
        }
    }

    // ---- Key dialog ----
    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = { Text("设置加密密钥") },
            text = {
                OutlinedTextField(
                    value = keyInput, onValueChange = { keyInput = it },
                    label = { Text("PvZ2 密钥") }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = themeColor,
                        focusedLabelColor = themeColor,
                        cursorColor = themeColor
                    )
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    onClick = {
                        encryptionKey = keyInput.trim()
                        prefs.edit { putString("encryption_key", encryptionKey) }
                        showKeyDialog = false
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showKeyDialog = false }) {
                    Text("取消", color = themeColor)
                }
            }
        )
    }
}

// ---- Data ----

private data class DisplayFile(
    val name: String,
    val uri: Uri,
    val size: Long
)

// ---- Components ----

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(2.dp))
        Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 26.dp))
    }
}

@Composable
private fun EmptyHint(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String, path: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text(message, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(path, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TemplateCard(
    file: DisplayFile,
    isSelected: Boolean,
    themeColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) themeColor.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.InsertDriveFile,
                null,
                tint = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurface)
                Text(formatSize(file.size), fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, "已选择", tint = themeColor, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun PatchFileRow(file: DisplayFile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.InsertDriveFile, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(file.name, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        Text(formatSize(file.size), fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

// ---- Utilities ----

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))} GB"
    }
}
