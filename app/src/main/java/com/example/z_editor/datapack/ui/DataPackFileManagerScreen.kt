package com.example.z_editor.datapack.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.example.z_editor.data.repository.FileItem
import com.example.z_editor.data.repository.LevelRepository
import com.example.z_editor.datapack.hotupdate.HotUpdateJSONConverter
import com.example.z_editor.datapack.rton.RtonConverter
import com.example.z_editor.views.components.rememberDebouncedClick
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---- Data ----
data class Breadcrumb(val name: String, val uri: Uri)

// 明确划分的四个核心操作
private enum class ConvertTarget(
    val extension: String,
    val label: String,
    val icon: ImageVector,
    val isCryptoAction: Boolean = false
) {
    // 目标格式: 图标 + 颜色 与文件列表中的格式图标一致
    JSON_TO_PLAIN_RTON("rton", "转换为 普通 RTON", Icons.Default.Description),
    PLAIN_RTON_TO_JSON("json", "解析为 JSON 文本", Icons.Default.Code),
    PLAIN_RTON_TO_ENCRYPTED("rton", "加密为 游戏 RTON", Icons.Default.Lock, true),
    ENCRYPTED_RTON_TO_PLAIN("rton", "解密为 普通 RTON", Icons.Default.Description, true),
    HOTUPDATE_TO_JSON("json", "解密热更新 JSON", Icons.Default.Code, true),
    JSON_TO_HOTUPDATE("json", "加密为热更新 JSON", Icons.Default.Security, true)
}

/** 目标格式对应的图标颜色，与文件列表中的格式颜色一致 */
private fun ConvertTarget.formatColor(themeColor: Color): Color = when (this) {
    ConvertTarget.JSON_TO_PLAIN_RTON -> Color(0xFFFF9800)   // RTON 橙色
    ConvertTarget.PLAIN_RTON_TO_JSON -> themeColor           // JSON 主题色
    ConvertTarget.PLAIN_RTON_TO_ENCRYPTED -> Color(0xFFE91E63) // 加密 RTON 红色
    ConvertTarget.ENCRYPTED_RTON_TO_PLAIN -> Color(0xFFFF9800) // RTON 橙色
    ConvertTarget.HOTUPDATE_TO_JSON -> themeColor               // JSON 主题色
    ConvertTarget.JSON_TO_HOTUPDATE -> Color(0xFF9C27B0)        // 热更新 JSON 紫色
}

// ---- SAF Launcher ----
private class OpenDocumentTreeFixed : ActivityResultContract<Uri?, Uri?>() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        if (input != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, input)
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data else null
    }
}

// ---- Main Screen ----
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataPackFileManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val handleBack = rememberDebouncedClick { onBack() }
    BackHandler(onBack = handleBack)

    val prefs = remember { context.getSharedPreferences("datapack_prefs", Context.MODE_PRIVATE) }
    val mainPrefs = remember { context.getSharedPreferences("prefs", Context.MODE_PRIVATE) }
    var encryptionKey by remember { mutableStateOf(prefs.getString("encryption_key", "") ?: "") }
    var showKeyDialog by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf(encryptionKey) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val fileItems = remember { mutableStateListOf<FileItem>() }
    var isLoading by remember { mutableStateOf(false) }
    var pathStack by remember { mutableStateOf(listOf<Breadcrumb>()) }

    // 复用主项目的 SAF 目录（LevelListScreen 的 "folder_uri"），无需重复授权
    var rootFolderUri by remember {
        mutableStateOf(
            mainPrefs.getString("folder_uri", null)?.toUri()
                ?: prefs.getString("datapack_folder_uri", null)?.toUri()
        )
    }
    var showNoFolderDialog by remember { mutableStateOf(false) }

    // 热更新 JSON 检测（纯内容检测，像 RTON 一样通过读取文件内容判断格式）
    fun isHotUpdateFile(uri: Uri): Boolean {
        val segment = uri.lastPathSegment ?: return false
        if (!segment.endsWith(".json", true)) return false
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = ByteArray(4096)
                val count = stream.read(bytes)
                if (count <= 0) return false
                HotUpdateJSONConverter.isHotUpdateFormat(String(bytes, 0, count, Charsets.UTF_8))
            } ?: false
        } catch (_: Exception) { false }
    }

    // 加密 RTON 检测（前 2 字节 = 0x1000）
    fun isEncryptedRton(uri: Uri): Boolean {
        val segment = uri.lastPathSegment ?: return false
        if (!segment.endsWith(".rton", true)) return false
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(2)
                stream.read(header) == 2 && header[0] == 0x10.toByte() && header[1] == 0x00.toByte()
            } ?: false
        } catch (_: Exception) { false }
    }

    // 操作菜单状态
    var convertTargetItem by remember { mutableStateOf<FileItem?>(null) }
    var availableTargets by remember { mutableStateOf<List<ConvertTarget>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }

    // 覆盖确认状态
    var showOverwriteDialog by remember { mutableStateOf(false) }
    var pendingOverwriteTarget by remember { mutableStateOf<ConvertTarget?>(null) }
    var pendingOverwriteItem by remember { mutableStateOf<FileItem?>(null) }
    var pendingOutputName by remember { mutableStateOf("") }

    // 文件操作状态（复制、删除、重命名、移动）
    var itemToDelete by remember { mutableStateOf<FileItem?>(null) }
    var itemToRename by remember { mutableStateOf<FileItem?>(null) }
    var itemToCopy by remember { mutableStateOf<FileItem?>(null) }
    var itemToMove by remember { mutableStateOf<FileItem?>(null) }
    var moveSourceUri by remember { mutableStateOf<Uri?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var copyInput by remember { mutableStateOf("") }
    val isMovingMode = itemToMove != null

    // 新建文件夹状态
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }

    // ---- Helpers ----
    fun loadCurrentDirectory() {
        val currentUri = pathStack.lastOrNull()?.uri ?: rootFolderUri ?: return
        isLoading = true
        scope.launch {
            val items = withContext(Dispatchers.IO) {
                val docDir = DocumentFile.fromTreeUri(context, currentUri) ?: return@withContext emptyList<FileItem>()
                docDir.listFiles().mapNotNull { file ->
                    val name = file.name ?: return@mapNotNull null
                    val isDir = file.isDirectory
                    if (isDir || !name.startsWith(".")) {
                        FileItem(name, file.uri, isDir, file.lastModified(), file.length())
                    } else null
                }.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
            }
            fileItems.clear()
            fileItems.addAll(items)
            isLoading = false
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(OpenDocumentTreeFixed()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            // 同时保存到主项目和 datapack prefs，保持一致
            mainPrefs.edit { putString("folder_uri", uri.toString()) }
            prefs.edit { putString("datapack_folder_uri", uri.toString()) }
            rootFolderUri = uri
            val rootName = DocumentFile.fromTreeUri(context, uri)?.name ?: "Root"
            pathStack = listOf(Breadcrumb(rootName, uri))
            showNoFolderDialog = false
            loadCurrentDirectory()
        }
    }

    LaunchedEffect(Unit) {
        if (rootFolderUri == null) showNoFolderDialog = true
        else {
            if (pathStack.isEmpty()) {
                val rootName = DocumentFile.fromTreeUri(context, rootFolderUri!!)?.name ?: "Root"
                pathStack = listOf(Breadcrumb(rootName, rootFolderUri!!))
            }
            loadCurrentDirectory()
        }
    }

    fun executeConversion(item: FileItem, target: ConvertTarget, dirUri: Uri, outputName: String) {
        isProcessing = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                when (target) {
                    ConvertTarget.JSON_TO_PLAIN_RTON -> RtonConverter.jsonToPlainRton(context, item.uri, dirUri, outputName)
                    ConvertTarget.PLAIN_RTON_TO_JSON -> RtonConverter.plainRtonToJson(context, item.uri, dirUri, outputName)
                    ConvertTarget.PLAIN_RTON_TO_ENCRYPTED -> RtonConverter.encryptRton(context, item.uri, dirUri, outputName, encryptionKey)
                    ConvertTarget.ENCRYPTED_RTON_TO_PLAIN -> RtonConverter.decryptRtonToPlain(context, item.uri, dirUri, outputName, encryptionKey)
                    ConvertTarget.HOTUPDATE_TO_JSON -> HotUpdateJSONConverter.convertToNormalJson(context, item.uri, dirUri, outputName, encryptionKey)
                    ConvertTarget.JSON_TO_HOTUPDATE -> HotUpdateJSONConverter.convertToHotUpdateJson(context, item.uri, dirUri, outputName, encryptionKey)
                }
            }
            isProcessing = false
            convertTargetItem = null

            result.fold(
                onSuccess = {
                    Toast.makeText(context, "成功保存为: $it", Toast.LENGTH_SHORT).show()
                    loadCurrentDirectory()
                },
                onFailure = {
                    Toast.makeText(context, "操作失败: ${it.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    fun prepareConversion(item: FileItem, target: ConvertTarget) {
        if (target.isCryptoAction && encryptionKey.isBlank()) {
            Toast.makeText(context, "加解密操作需要先设置密钥", Toast.LENGTH_SHORT).show()
            showKeyDialog = true
            return
        }

        val baseName = item.name.substringBeforeLast(".")

        // 为了防止覆盖测试时混淆，对加密解密产物加上后缀区分
        val outputName = when (target) {
            ConvertTarget.JSON_TO_PLAIN_RTON -> "$baseName.rton"
            ConvertTarget.PLAIN_RTON_TO_JSON -> "$baseName.json"
            ConvertTarget.PLAIN_RTON_TO_ENCRYPTED -> "${baseName}_enc.rton"
            ConvertTarget.ENCRYPTED_RTON_TO_PLAIN -> "${baseName}_plain.rton"
            ConvertTarget.HOTUPDATE_TO_JSON -> "${baseName}_decoded.json"
            ConvertTarget.JSON_TO_HOTUPDATE -> "${baseName}_encoded.json"
        }

        val currentDir = pathStack.lastOrNull()?.uri ?: return
        val docDir = DocumentFile.fromTreeUri(context, currentDir)
        if (docDir?.findFile(outputName) != null) {
            pendingOverwriteTarget = target
            pendingOverwriteItem = item
            pendingOutputName = outputName
            showOverwriteDialog = true
        } else {
            executeConversion(item, target, currentDir, outputName)
        }
    }

    fun detectAndShowMenu(item: FileItem) {
        val ext = item.name.substringAfterLast('.', "").lowercase()
        scope.launch {
            isProcessing = true
            val targets = withContext(Dispatchers.IO) {
                when (ext) {
                    "json" -> {
                        // 检测是否为 HotUpdate 格式（Base64 热更新）
                        val isHotUpdate = try {
                            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                                val bytes = ByteArray(4096)
                                val count = stream.read(bytes)
                                if (count > 0) HotUpdateJSONConverter.isHotUpdateFormat(String(bytes, 0, count, Charsets.UTF_8))
                                else false
                            } ?: false
                        } catch (_: Exception) { false }

                        if (isHotUpdate) {
                            listOf(ConvertTarget.JSON_TO_HOTUPDATE)
                        } else {
                            listOf(ConvertTarget.JSON_TO_PLAIN_RTON, ConvertTarget.JSON_TO_HOTUPDATE)
                        }
                    }
                    "rton" -> {
                        val isPlain = try {
                            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                                val bytes = ByteArray(4)
                                val readCount = stream.read(bytes)
                                if (readCount == 4) {
                                    bytes[0] == 'R'.code.toByte() && bytes[1] == 'T'.code.toByte() &&
                                            bytes[2] == 'O'.code.toByte() && bytes[3] == 'N'.code.toByte()
                                } else false
                            } ?: false
                        } catch (e: Exception) { false }

                        if (isPlain) {
                            listOf(ConvertTarget.PLAIN_RTON_TO_JSON, ConvertTarget.PLAIN_RTON_TO_ENCRYPTED)
                        } else {
                            listOf(ConvertTarget.ENCRYPTED_RTON_TO_PLAIN)
                        }
                    }
                    else -> emptyList()
                }
            }
            availableTargets = targets
            convertTargetItem = item
            isProcessing = false
        }
    }

    // ---- 文件操作处理 ----
    fun handleRenameConfirm() {
        val target = itemToRename ?: return
        val currentUri = pathStack.lastOrNull()?.uri ?: return
        val finalName = renameInput.trim()
        if (finalName.isEmpty()) return
        if (LevelRepository.renameItem(context, currentUri, target.name, finalName, target.isDirectory)) {
            Toast.makeText(context, "重命名成功", Toast.LENGTH_SHORT).show()
            itemToRename = null
            loadCurrentDirectory()
        } else {
            Toast.makeText(context, "重命名失败（可能目标已存在）", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleDeleteConfirm() {
        val target = itemToDelete ?: return
        val currentUri = pathStack.lastOrNull()?.uri ?: return
        LevelRepository.deleteItem(context, currentUri, target.name, target.isDirectory)
        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
        itemToDelete = null
        loadCurrentDirectory()
    }

    fun handleCopyConfirm() {
        val target = itemToCopy ?: return
        val currentUri = pathStack.lastOrNull()?.uri ?: return
        val finalName = copyInput.trim()
        if (finalName.isEmpty()) return
        if (LevelRepository.copyLevelToTarget(context, target.name, finalName, currentUri)) {
            Toast.makeText(context, "复制成功", Toast.LENGTH_SHORT).show()
            itemToCopy = null
            loadCurrentDirectory()
        } else {
            Toast.makeText(context, "复制失败（可能目标已存在）", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleMoveConfirm() {
        val target = itemToMove ?: return
        val source = moveSourceUri ?: return
        val dest = pathStack.lastOrNull()?.uri ?: return
        if (source == dest) {
            Toast.makeText(context, "源和目标相同，无需移动", Toast.LENGTH_SHORT).show()
            itemToMove = null; moveSourceUri = null; return
        }
        isLoading = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                LevelRepository.moveFile(context, source, target.name, dest)
            }
            isLoading = false
            if (ok) {
                Toast.makeText(context, "移动成功", Toast.LENGTH_SHORT).show()
                itemToMove = null; moveSourceUri = null
                loadCurrentDirectory()
            } else {
                Toast.makeText(context, "移动失败（可能目标已存在）", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleNewFolder() {
        if (newFolderNameInput.isBlank()) return
        val currentUri = pathStack.lastOrNull()?.uri ?: return
        if (LevelRepository.createDirectory(context, currentUri, newFolderNameInput.trim())) {
            Toast.makeText(context, "创建文件夹成功", Toast.LENGTH_SHORT).show()
            showNewFolderDialog = false
            newFolderNameInput = ""
            loadCurrentDirectory()
        } else {
            Toast.makeText(context, "创建文件夹失败（可能已存在）", Toast.LENGTH_SHORT).show()
        }
    }

    // ---- UI ----
    val themeColor = MaterialTheme.colorScheme.secondary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件格式转换", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = handleBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onPrimary) }
                },
                actions = {
                    IconButton(onClick = { loadCurrentDirectory() }) {
                        Icon(Icons.Default.Refresh, "刷新", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = { keyInput = encryptionKey; showKeyDialog = true }) {
                        Icon(Icons.Default.Key, "密钥", tint = if (encryptionKey.isNotBlank()) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onPrimary.copy(alpha=0.7f))
                    }
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "帮助", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = themeColor, titleContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        },
        floatingActionButton = {
            if (isMovingMode) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            itemToMove = null
                            moveSourceUri = null
                        },
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        icon = { Icon(Icons.Default.Close, null) },
                        text = { Text("取消") }
                    )
                    ExtendedFloatingActionButton(
                        onClick = { handleMoveConfirm() },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        icon = { Icon(Icons.Default.ContentPaste, null) },
                        text = { Text("粘贴") }
                    )
                }
            } else {
                FloatingActionButton(
                    onClick = { showNewFolderDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(
                        Icons.Default.CreateNewFolder,
                        "新建文件夹"
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {

            // 面包屑导航
            LazyRow(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(vertical = 6.dp, horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(pathStack.size) { index ->
                    val isLast = index == pathStack.size - 1
                    Surface(
                        color = if (isLast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = !isLast) {
                            pathStack = pathStack.take(index + 1); loadCurrentDirectory()
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            if (index == 0) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    null,
                                    tint = if (isLast) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                pathStack[index].name,
                                color = if (isLast) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                    }
                    if (!isLast) Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                }
            }

            // 移动模式提示条
            if (isMovingMode) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.DriveFileMove,
                            null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "移动: ${itemToMove?.name ?: ""}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 14.sp
                            )
                            Text(
                                "请导航到目标文件夹，然后点击粘贴按钮",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // 文件列表
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = themeColor) }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    if (pathStack.size > 1) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                elevation = CardDefaults.cardElevation(0.dp),
                                modifier = Modifier.fillMaxWidth().clickable { pathStack = pathStack.dropLast(1); loadCurrentDirectory() }) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(16.dp))
                                    Text("返回上级", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    if (fileItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.FolderOpen,
                                        null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "此文件夹为空",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(fileItems.size) { index ->
                            val item = fileItems[index]
                            val isSelfMoving = isMovingMode && itemToMove == item
                            val dimmed = isMovingMode && (!item.isDirectory || isSelfMoving)
                            val actionsEnabled = !isMovingMode

                            if (item.isDirectory) {
                                Card(modifier = Modifier.fillMaxWidth()
                                    .alpha(if (isSelfMoving) 0.5f else 1f)
                                    .clickable {
                                        pathStack = pathStack + Breadcrumb(item.name, item.uri); loadCurrentDirectory()
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(2.dp)) {
                                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Folder, null, tint = Color(0xFFFFC107), modifier = Modifier.size(28.dp))
                                        Spacer(Modifier.width(16.dp))
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                        if (actionsEnabled) {
                                            IconButton(onClick = {
                                                renameInput = item.name
                                                itemToRename = item
                                            }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Default.Edit, "重命名", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                            }
                                            IconButton(onClick = { itemToDelete = item }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.onError.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                val encrypted = remember(item.uri) { isEncryptedRton(item.uri) }
                                val isHotUpdate = remember(item.uri) { isHotUpdateFile(item.uri) }
                                Card(modifier = Modifier.fillMaxWidth()
                                    .alpha(if (dimmed) 0.5f else 1f)
                                    .clickable(enabled = !isMovingMode) { detectAndShowMenu(item) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(2.dp)) {
                                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        val isRton = item.name.endsWith(".rton", true)
                                        val isSmf = item.name.endsWith(".smf", true)
                                        val isJson = item.name.endsWith(".json", true)
                                        val (fileIcon, iconTint, fileLabel) = when {
                                            encrypted -> Triple(Icons.Default.Lock, Color(0xFFE91E63), "加密 RTON")
                                            isHotUpdate -> Triple(Icons.Default.Security, Color(0xFF9C27B0), "热更新 JSON")
                                            isRton -> Triple(Icons.Default.Description, Color(0xFFFF9800), "RTON 文件")
                                            isSmf -> Triple(Icons.Default.Inventory2, Color(0xFF00BCD4), "数据包 (SMF)")
                                            isJson -> Triple(Icons.Default.Code, themeColor, "JSON 文件")
                                            else -> Triple(Icons.Default.InsertDriveFile, Color(0xFF9E9E9E), "文件")
                                        }
                                        Icon(
                                            fileIcon, null,
                                            tint = iconTint, modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text(fileLabel,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (actionsEnabled) {
                                            IconButton(onClick = {
                                                val dot = item.name.lastIndexOf('.')
                                                copyInput = if (dot >= 0) item.name.substring(0, dot) + "_copy" + item.name.substring(dot)
                                                else item.name + "_copy"
                                                itemToCopy = item
                                            }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Default.ContentCopy, "复制", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                            }
                                            IconButton(onClick = {
                                                renameInput = item.name
                                                itemToRename = item
                                            }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Default.Edit, "重命名", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                            }
                                            IconButton(onClick = {
                                                itemToMove = item
                                                moveSourceUri = pathStack.lastOrNull()?.uri
                                            }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.AutoMirrored.Filled.DriveFileMove, "移动", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                            }
                                            IconButton(onClick = { itemToDelete = item }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.onError.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // ---- Dialogs ----

    // 无目录弹窗
    if (showNoFolderDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("选择数据包目录") },
            text = { Text("请选择一个包含游戏数据包的目录。") },
            confirmButton = {
                Button(onClick = { folderPickerLauncher.launch(null) }) { Text("选择目录") }
            }
        )
    }

    // 操作菜单弹窗
    if (convertTargetItem != null) {
        val item = convertTargetItem!!
        AlertDialog(
            onDismissRequest = { convertTargetItem = null },
            title = { Text("选择操作") },
            text = {
                Column {
                    Text("选中文件: ${item.name}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    availableTargets.forEach { target ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .clickable { convertTargetItem = null; prepareConversion(item, target) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(target.icon, null, tint = target.formatColor(themeColor))
                                Spacer(Modifier.width(12.dp))
                                Text(target.label, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { convertTargetItem = null }) { Text("取消", color = themeColor) } }
        )
    }

    // 覆盖提示弹窗
    if (showOverwriteDialog) {
        AlertDialog(
            onDismissRequest = { showOverwriteDialog = false },
            title = { Text("覆盖确认") },
            text = { Text("目标文件 [$pendingOutputName] 已存在，是否覆盖？") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    onClick = {
                    showOverwriteDialog = false
                    executeConversion(pendingOverwriteItem!!, pendingOverwriteTarget!!, pathStack.last().uri, pendingOutputName)
                }) { Text("覆盖") }
            },
            dismissButton = { TextButton(onClick = { showOverwriteDialog = false }) { Text("取消", color = themeColor) } }
        )
    }

    // 密钥配置弹窗
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
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showKeyDialog = false }) { Text("取消", color = themeColor) } }
        )
    }

    // ---- 文件操作弹窗 ----

    // 删除确认
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${itemToDelete!!.name}」吗？此操作不可恢复。") },
            confirmButton = {
                Button(onClick = { handleDeleteConfirm() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onError)) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("取消", color = themeColor) } }
        )
    }

    // 重命名
    if (itemToRename != null) {
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            title = { Text("重命名") },
            text = {
                OutlinedTextField(
                    value = renameInput, onValueChange = { renameInput = it },
                    label = { Text("新名称") }, singleLine = true,
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
                    onClick = { handleRenameConfirm() }) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { itemToRename = null }) { Text("取消", color = themeColor) } }
        )
    }

    // 复制
    if (itemToCopy != null) {
        AlertDialog(
            onDismissRequest = { itemToCopy = null },
            title = { Text("复制文件") },
            text = {
                OutlinedTextField(
                    value = copyInput, onValueChange = { copyInput = it },
                    label = { Text("副本名称") }, singleLine = true,
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
                    onClick = { handleCopyConfirm() }) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { itemToCopy = null }) { Text("取消", color = themeColor) } }
        )
    }

    // 新建文件夹
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("新建文件夹") },
            text = {
                OutlinedTextField(
                    value = newFolderNameInput,
                    onValueChange = { newFolderNameInput = it },
                    label = { Text("文件夹名称") },
                    singleLine = true,
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
                    onClick = { handleNewFolder() }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("取消", color = themeColor) }
            }
        )
    }

    // 加载动画
    if (isProcessing) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)).clickable(enabled = false){}, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = themeColor)
        }
    }

    // 帮助弹窗
    if (showHelpDialog) {
        EditorHelpDialog(
            title = "文件管理说明",
            onDismiss = { showHelpDialog = false },
            themeColor = themeColor
        ) {
            HelpSection(
                title = "功能介绍",
                body = "文件管理器用于浏览、转换和操作游戏数据包中的文件。支持 JSON、RTON（普通/加密）、热更新 JSON 等格式。"
            )
            HelpSection(
                title = "格式识别",
                body = "• JSON 文件（.json）— 蓝色图标，可直接查看和编辑\n" +
                    "• RTON 文件（.rton）— 橙色图标，游戏使用的二进制格式\n" +
                    "• 加密 RTON — 红色图标，需密钥才能解密查看\n" +
                    "• 热更新JSON — 紫色图标，Base64 编码的热更新格式\n" +
                    "• SMF 数据包 — 青色图标，游戏资源容器文件"
            )
            HelpSection(
                title = "加密操作",
                body = "加解密 RTON 和热更新 JSON 需要提前设置密钥，点击顶栏钥匙图标配置密钥。软件不提供密钥。"
            )
        }
    }
}
