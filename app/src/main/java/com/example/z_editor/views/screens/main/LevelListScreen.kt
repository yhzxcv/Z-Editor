package com.example.z_editor.views.screens.main

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.example.z_editor.data.repository.FileItem
import com.example.z_editor.data.repository.LevelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Breadcrumb(val name: String, val uri: Uri)

class OpenDocumentTreeFixed : ActivityResultContract<Uri?, Uri?>() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        if (input != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, input)
        } else {
            val primaryRootUri =
                Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A")
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, primaryRootUri)
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == android.app.Activity.RESULT_OK) intent?.data else null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelListScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    uiScale: Float,
    onUiScaleChange: (Float) -> Unit,
    onLevelClick: (String, Uri) -> Unit,
    onAboutClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ======================== 1. 状态声明 ========================

    // 当前目录的内容列表
    val fileItems = remember { mutableStateListOf<FileItem>() }
    var isLoading by remember { mutableStateOf(false) }

    // 路径栈：用于面包屑导航和返回上一级
    var pathStack by remember { mutableStateOf(listOf<Breadcrumb>()) }

    // 获取根目录 Uri (从 SharedPreferences)
    var rootFolderUri by remember {
        mutableStateOf(
            context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .getString("folder_uri", null)?.toUri()
        )
    }

    var itemToMove by remember { mutableStateOf<FileItem?>(null) }
    var moveSourceUri by remember { mutableStateOf<Uri?>(null) }
    val isMovingMode = itemToMove != null

    // 各种弹窗状态
    var showNoFolderDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<FileItem?>(null) }
    var itemToRename by remember { mutableStateOf<FileItem?>(null) }
    var itemToCopy by remember { mutableStateOf<FileItem?>(null) }

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }

    var showTemplateDialog by remember { mutableStateOf(false) }
    var showCreateNameDialog by remember { mutableStateOf(false) }

    var showMenu by remember { mutableStateOf(false) }
    var confirmCheckbox by remember { mutableStateOf(false) }

    // 输入框临时变量
    var renameInput by remember { mutableStateOf("") }
    var copyInput by remember { mutableStateOf("") }
    var newLevelNameInput by remember { mutableStateOf("") }

    var templates by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTemplate by remember { mutableStateOf("") }

    var showUiScaleDialog by remember { mutableStateOf(false) }

    // ======================== 2. 核心逻辑 ========================

    fun loadCurrentDirectory() {
        val currentUri = pathStack.lastOrNull()?.uri ?: rootFolderUri ?: return

        isLoading = true
        scope.launch {
            val items = withContext(Dispatchers.IO) {
                LevelRepository.getDirectoryContents(context, currentUri)
            }
            fileItems.clear()
            fileItems.addAll(items)
            isLoading = false
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentTreeFixed()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .edit { putString("folder_uri", uri.toString()) }

            rootFolderUri = uri

            val docFile = DocumentFile.fromTreeUri(context, uri)
            val rootName = docFile?.name ?: "根目录"
            pathStack = listOf(Breadcrumb(rootName, uri))

            showNoFolderDialog = false
            loadCurrentDirectory()
        }
    }

    LaunchedEffect(Unit) {
        if (rootFolderUri == null) {
            showNoFolderDialog = true
        } else {
            if (pathStack.isEmpty()) {
                val docFile = DocumentFile.fromTreeUri(context, rootFolderUri!!)
                val rootName = docFile?.name ?: "根目录"
                pathStack = listOf(Breadcrumb(rootName, rootFolderUri!!))
            }
            loadCurrentDirectory()
        }
    }

    // 返回键处理
    BackHandler(enabled = pathStack.size > 1) {
        pathStack = pathStack.dropLast(1)
        loadCurrentDirectory()
    }

    // --- 文件/文件夹 操作逻辑 ---
    fun navigateToFolder(folder: FileItem) {
        pathStack = pathStack + Breadcrumb(folder.name, folder.uri)
        loadCurrentDirectory()
    }

    fun handleRenameConfirm() {
        val target = itemToRename ?: return
        val currentUri = pathStack.last().uri
        var finalName = renameInput.trim()
        if (!target.isDirectory && !finalName.endsWith(".json", ignoreCase = true)) {
            finalName += ".json"
        }
        if (LevelRepository.renameItem(
                context,
                currentUri,
                target.name,
                finalName,
                target.isDirectory
            )
        ) {
            Toast.makeText(context, "重命名成功", Toast.LENGTH_SHORT).show()
            itemToRename = null
            loadCurrentDirectory()
        } else {
            Toast.makeText(context, "重命名失败，已有同名文件", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleDeleteConfirm() {
        val target = itemToDelete ?: return
        val currentUri = pathStack.last().uri

        LevelRepository.deleteItem(context, currentUri, target.name, target.isDirectory)
        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
        itemToDelete = null
        loadCurrentDirectory()
    }

    fun handleCopyConfirm() {
        val target = itemToCopy ?: return
        val currentUri = pathStack.last().uri

        var finalName = copyInput.trim()
        if (!finalName.endsWith(".json", ignoreCase = true)) {
            finalName += ".json"
        }
        if (LevelRepository.copyLevelToTarget(context, target.name, finalName, currentUri)) {
            Toast.makeText(context, "复制成功", Toast.LENGTH_SHORT).show()
            itemToCopy = null
            loadCurrentDirectory()
        } else {
            Toast.makeText(context, "复制失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleMoveConfirm() {
        val target = itemToMove ?: return
        val source = moveSourceUri ?: return
        val dest = pathStack.last().uri

        if (source == dest) {
            Toast.makeText(context, "源目录和目标目录相同", Toast.LENGTH_SHORT).show()
            itemToMove = null
            moveSourceUri = null
            return
        }

        isLoading = true
        scope.launch {
            val success = withContext(Dispatchers.IO) {
                LevelRepository.moveFile(context, source, target.name, dest)
            }
            isLoading = false
            if (success) {
                Toast.makeText(context, "移动成功", Toast.LENGTH_SHORT).show()
                itemToMove = null
                moveSourceUri = null
                loadCurrentDirectory()
            } else {
                Toast.makeText(context, "移动失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleNewFolder() {
        if (newFolderNameInput.isBlank()) return
        val currentUri = pathStack.last().uri
        if (LevelRepository.createDirectory(context, currentUri, newFolderNameInput)) {
            Toast.makeText(context, "文件夹创建成功", Toast.LENGTH_SHORT).show()
            showNewFolderDialog = false
            newFolderNameInput = ""
            loadCurrentDirectory()
        } else {
            Toast.makeText(context, "创建失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun openTemplateSelector() {
        templates = LevelRepository.getTemplateList(context)
        if (templates.isEmpty()) {
            Toast.makeText(context, "未找到模板", Toast.LENGTH_SHORT).show()
        } else {
            showTemplateDialog = true
        }
    }

    fun handleCreateLevelConfirm() {
        val currentUri = pathStack.lastOrNull()?.uri ?: return
        var name = newLevelNameInput
        if (!name.endsWith(".json", true)) name += ".json"

        if (LevelRepository.createLevelFromTemplate(context, currentUri, selectedTemplate, name)) {
            Toast.makeText(context, "创建成功", Toast.LENGTH_SHORT).show()
            showCreateNameDialog = false
            loadCurrentDirectory()
        } else {
            Toast.makeText(context, "创建失败，已有同名文件", Toast.LENGTH_SHORT).show()
        }
    }

    // ======================== 3. UI 渲染 ========================

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的关卡库", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                actions = {
                    IconButton(onClick = { loadCurrentDirectory() }) {
                        Icon(
                            Icons.Default.Refresh,
                            "刷新",
                            tint = MaterialTheme.colorScheme.background
                        )
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "切换主题",
                            tint = MaterialTheme.colorScheme.background
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert, "更多选项",
                                tint = MaterialTheme.colorScheme.background
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("切换目录") },
                                onClick = { folderPickerLauncher.launch(null) },
                                leadingIcon = {
                                    Icon(Icons.Default.FolderOpen, null, tint = Color.Gray)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("释放缓存") },
                                onClick = {
                                    showMenu = false
                                    val count = LevelRepository.clearAllInternalCache(context)
                                    Toast.makeText(
                                        context,
                                        "已清理 $count 个缓存文件",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, null, tint = Color.Gray)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("界面大小") },
                                onClick = {
                                    showMenu = false
                                    showUiScaleDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AspectRatio, null, tint = Color.Gray)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("关于软件") },
                                onClick = {
                                    showMenu = false
                                    onAboutClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, null, tint = Color.Gray)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
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
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = { showNewFolderDialog = true },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, "新建文件夹")
                    }

                    FloatingActionButton(
                        onClick = { openTemplateSelector() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(Icons.Default.Add, "新建关卡")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- 面包屑导航栏 ---
            BreadcrumbBar(
                pathStack = pathStack,
                onBreadcrumbClick = { index ->
                    pathStack = pathStack.take(index + 1)
                    loadCurrentDirectory()
                }
            )
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
                            Icons.Default.DriveFileMove,
                            null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "正在移动: ${itemToMove?.name}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 14.sp
                            )
                            Text(
                                "请导航至目标文件夹，然后点击右下角粘贴",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (pathStack.size > 1) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        pathStack = pathStack.dropLast(1)
                                        loadCurrentDirectory()
                                    },
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        null,
                                        tint = Color.Gray
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        "返回上一级",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    if (fileItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.FolderOpen,
                                        null,
                                        tint = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text("文件夹为空", color = Color.Gray)
                                }
                            }
                        }
                    } else {
                        // C. 文件列表
                        items(fileItems) { item ->
                            val isSelfMoving = isMovingMode && itemToMove == item
                            val isInteractionDisabled = isMovingMode && !item.isDirectory
                            val isActionButtonsDisabled = isMovingMode

                            val alpha = if (isInteractionDisabled || isSelfMoving) 0.5f else 1f

                            FileItemRow(
                                item = item,
                                modifier = Modifier.alpha(alpha),
                                onClick = {
                                    if (isMovingMode) {
                                        if (item.isDirectory) navigateToFolder(item)
                                    } else {
                                        if (item.isDirectory) {
                                            navigateToFolder(item)
                                        } else {
                                            if (LevelRepository.prepareInternalCache(
                                                    context,
                                                    item.uri,
                                                    item.name
                                                )
                                            ) {
                                                onLevelClick(item.name, item.uri)
                                            }
                                        }
                                    }
                                },
                                actionsEnabled = !isActionButtonsDisabled, onRename = {
                                    renameInput = if (item.isDirectory) {
                                        item.name
                                    } else {
                                        item.name.substringBeforeLast(".")
                                    }
                                    itemToRename = item
                                },
                                onDelete = { itemToDelete = item },
                                onCopy = {
                                    if (!item.isDirectory) {
                                        val base = item.name.substringBeforeLast(".")
                                        copyInput = "${base}_copy"
                                        itemToCopy = item
                                    }
                                },
                                onMove = {
                                    if (!item.isDirectory) {
                                        itemToMove = item
                                        moveSourceUri = pathStack.last().uri
                                    }
                                }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(160.dp)) }
                }
            }
        }
    }

    if (showNoFolderDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("初始化设置") },
            text = { Text("请选择一个文件夹作为关卡存储目录。") },
            confirmButton = { Button(onClick = { folderPickerLauncher.launch(null) }) { Text("选择文件夹") } }
        )
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("确认删除") },
            text = {
                Column {
                    Text("确定要删除 \"${itemToDelete?.name}\" 吗？\n${if (itemToDelete!!.isDirectory) "如果是文件夹，其内容也将被删除。" else "此操作不可恢复。"}")
                    Spacer(Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.05f))
                            .clickable { confirmCheckbox = !confirmCheckbox }
                            .padding(8.dp)
                    ) {
                        Checkbox(
                            checked = confirmCheckbox,
                            onCheckedChange = { confirmCheckbox = it }
                        )
                        Text(
                            if (itemToDelete!!.isDirectory) "我确定要永久删除此文件夹" else "我确定要永久删除此关卡",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        handleDeleteConfirm()
                        confirmCheckbox = false
                    },
                    enabled = confirmCheckbox,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onError)
                ) { Text("确认删除") }
            },
            dismissButton = {
                TextButton(onClick = {
                    itemToDelete = null
                    confirmCheckbox = false
                }) { Text("取消") }
            }
        )
    }

    if (itemToRename != null) {
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            title = { Text("重命名") },
            text = {
                OutlinedTextField(
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("新名称") },
                    singleLine = true
                )
            },
            confirmButton = { Button(onClick = { handleRenameConfirm() }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { itemToRename = null }) { Text("取消") } }
        )
    }

    if (itemToCopy != null) {
        AlertDialog(
            onDismissRequest = { itemToCopy = null },
            title = { Text("复制关卡") },
            text = {
                OutlinedTextField(
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    value = copyInput,
                    onValueChange = { copyInput = it },
                    label = { Text("新文件名") })
            },
            confirmButton = { Button(onClick = { handleCopyConfirm() }) { Text("复制") } },
            dismissButton = { TextButton(onClick = { itemToCopy = null }) { Text("取消") } }
        )
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("新建文件夹") },
            text = {
                OutlinedTextField(
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    value = newFolderNameInput,
                    onValueChange = { newFolderNameInput = it },
                    label = { Text("文件夹名称") })
            },
            confirmButton = { Button(onClick = { handleNewFolder() }) { Text("创建") } },
            dismissButton = {
                TextButton(onClick = {
                    showNewFolderDialog = false
                }) { Text("取消") }
            }
        )
    }

    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("新建关卡 - 选择模板") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(templates) { template ->
                        Card(
                            onClick = {
                                selectedTemplate = template
                                newLevelNameInput = template.substringBeforeLast(".")
                                showTemplateDialog = false
                                showCreateNameDialog = true
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    text = template.substringBeforeLast("."),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplateDialog = false }) { Text("取消") }
            }
        )
    }

    if (showCreateNameDialog) {
        AlertDialog(
            onDismissRequest = { showCreateNameDialog = false },
            title = { Text("命名关卡") },
            text = {
                OutlinedTextField(
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    value = newLevelNameInput,
                    onValueChange = { newLevelNameInput = it })
            },
            confirmButton = { Button(onClick = { handleCreateLevelConfirm() }) { Text("创建") } },
            dismissButton = {
                TextButton(onClick = {
                    showCreateNameDialog = false
                }) { Text("取消") }
            }
        )
    }
    if (showUiScaleDialog) {
        var tempScale by remember { mutableFloatStateOf(uiScale) }

        AlertDialog(
            onDismissRequest = { showUiScaleDialog = false },
            title = { Text("调整界面大小") },
            text = {
                Column {
                    Text(
                        text = "当前缩放: ${(tempScale * 100).toInt()}%",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(16.dp))
                    Slider(
                        value = tempScale,
                        onValueChange = { tempScale = it },
                        valueRange = 0.75f..1.25f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("小", style = MaterialTheme.typography.bodySmall)
                        Text("标准", style = MaterialTheme.typography.bodySmall)
                        Text("大", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showUiScaleDialog = false
                    onUiScaleChange(tempScale)
                }) {
                    Text("完成")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    tempScale = 1.0f
                }) {
                    Text("重置")
                }
            }
        )
    }
}

// === 自定义组件 ===

@Composable
fun BreadcrumbBar(
    pathStack: List<Breadcrumb>,
    onBreadcrumbClick: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 6.dp, horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(pathStack.size) { index ->
            val item = pathStack[index]
            val isLast = index == pathStack.size - 1

            Surface(
                color = if (isLast) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !isLast) { onBreadcrumbClick(index) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    if (index == 0) {
                        Icon(
                            Icons.Default.FolderOpen,
                            null,
                            tint = if (isLast) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }

                    Text(
                        text = item.name,
                        color = if (isLast) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }
            }

            if (!isLast) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun FileItemRow(
    item: FileItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    actionsEnabled: Boolean = true,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                tint = if (item.isDirectory) Color(0xFFFFC107) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (item.isDirectory) item.name else item.name.substringBeforeLast("."),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!item.isDirectory) {
                    Text("JSON 文件", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            if (actionsEnabled) {
                Row {
                    IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (!item.isDirectory) {
                        IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.ContentCopy,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onMove, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.AutoMirrored.Filled.DriveFileMove,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            null,
                            tint = MaterialTheme.colorScheme.onError.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}