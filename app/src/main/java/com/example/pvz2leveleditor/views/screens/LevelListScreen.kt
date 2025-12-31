package com.example.pvz2leveleditor.views.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pvz2leveleditor.data.repository.LevelRepository
import androidx.core.content.edit
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelListScreen(
    onLevelClick: (String) -> Unit
) {
    val context = LocalContext.current

    // ======================== 1. 状态声明 ========================

    val files = remember { mutableStateListOf<String>() }

    var folderUri by remember {
        mutableStateOf(
            context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .getString("folder_uri", null)
        )
    }

    var showNoFolderDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    var showCopyDialog by remember { mutableStateOf(false) }

    var fileToCopy by remember { mutableStateOf("") }
    var copyNameInput by remember { mutableStateOf("") }

    var showTemplateDialog by remember { mutableStateOf(false) }
    var showCreateNameDialog by remember { mutableStateOf(false) }

    var templates by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTemplate by remember { mutableStateOf("") }
    var newLevelNameInput by remember { mutableStateOf("") }

    // ======================== 2. 逻辑方法 ========================

    fun reload() {
        if (folderUri == null) {
            showNoFolderDialog = true
        } else {
            files.clear()
            files.addAll(LevelRepository.getExternalLevelFiles(context))
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .edit { putString("folder_uri", uri.toString()) }
            folderUri = uri.toString()
            showNoFolderDialog = false
            reload()
        }
    }

    fun openTemplateSelector() {
        if (folderUri == null) {
            showNoFolderDialog = true
            return
        }
        // 获取模板列表
        templates = LevelRepository.getTemplateList(context)
        if (templates.isEmpty()) {
            Toast.makeText(context, "未找到模板文件 (assets/template)", Toast.LENGTH_SHORT).show()
        } else {
            showTemplateDialog = true
        }
    }

    fun handleTemplateSelected(templateName: String) {
        selectedTemplate = templateName
        newLevelNameInput = templateName
        showTemplateDialog = false
        showCreateNameDialog = true
    }

    fun handleCreateConfirm() {
        if (!newLevelNameInput.endsWith(".json", ignoreCase = true)) {
            newLevelNameInput += ".json"
        }

        val success =
            LevelRepository.createLevelFromTemplate(context, selectedTemplate, newLevelNameInput)
        if (success) {
            Toast.makeText(context, "创建成功", Toast.LENGTH_SHORT).show()
            showCreateNameDialog = false
            reload()
        } else {
            Toast.makeText(context, "创建失败或文件名已存在", Toast.LENGTH_SHORT).show()
        }
    }

    // 初次启动加载
    LaunchedEffect(Unit) { reload() }

    // ======================== 3. 各种弹窗组件 ========================

    // --- 弹窗 A: 提示选择文件夹 ---
    if (showNoFolderDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("需要设置目录") },
            text = { Text("请选择存放关卡 JSON 文件的文件夹以开始使用。") },
            confirmButton = {
                Button(onClick = { folderPickerLauncher.launch(null) }) { Text("去选择") }
            }
        )
    }

    // --- 弹窗 B: 删除确认 ---
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除 \"${pendingDelete}\" 吗？此操作将同时清除外部文件和本地缓存。") },
            confirmButton = {
                TextButton(onClick = {
                    LevelRepository.deleteLevelCompletely(context, pendingDelete!!)
                    pendingDelete = null
                    reload()
                }) { Text("删除", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }

    // --- 弹窗 C: 复制并重命名 ---
    if (showCopyDialog) {
        AlertDialog(
            onDismissRequest = { showCopyDialog = false },
            title = { Text("复制关卡") },
            text = {
                Column {
                    Text("请输入复制后的文件名：", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = copyNameInput,
                        onValueChange = { copyNameInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (copyNameInput.isNotBlank() && copyNameInput.endsWith(
                            ".json",
                            ignoreCase = true
                        )
                    ) {
                        if (LevelRepository.copyLevelToTarget(context, fileToCopy, copyNameInput)) {
                            showCopyDialog = false
                            reload()
                        } else {
                            Toast.makeText(context, "文件名已存在或复制失败", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(context, "必须以 .json 结尾", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showCopyDialog = false }) { Text("取消") }
            }
        )
    }

    // --- 弹窗 D: 选择模板 ---
    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("新建关卡 - 选择模板") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp), // 限制高度，防止太长
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(templates) { template ->
                        Card(
                            onClick = { handleTemplateSelected(template) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, null, tint = Color(0xFF4CAF50))
                                Spacer(Modifier.width(16.dp))
                                Text(template, fontSize = 16.sp)
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

    // --- 弹窗 E: 命名新关卡 ---
    if (showCreateNameDialog) {
        AlertDialog(
            onDismissRequest = { showCreateNameDialog = false },
            title = { Text("命名新关卡") },
            text = {
                Column {
                    Text("模板: $selectedTemplate", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newLevelNameInput,
                        onValueChange = { newLevelNameInput = it },
                        label = { Text("文件名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { handleCreateConfirm() }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateNameDialog = false }) { Text("取消") }
            }
        )
    }

    // ======================== 4. 主界面布局 ========================

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "我的关卡库",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        // 子标题显示路径
                        Text(
                            text = getReadablePath(folderUri),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Normal,
                            maxLines = 1 // 防止路径过长导致 UI 错乱
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { reload() }) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                    IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                        Icon(Icons.Default.FolderOpen, "选择目录")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openTemplateSelector() },
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "新建关卡")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            if (files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("当前目录没有 JSON 文件", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files) { fileName ->
                        FileItemCard(
                            fileName = fileName,
                            onEdit = {
                                if (LevelRepository.prepareInternalCache(context, fileName)) {
                                    onLevelClick(fileName)
                                }
                            },
                            onCopy = {
                                fileToCopy = fileName
                                val base = fileName.substringBeforeLast(".")
                                copyNameInput = "${base}_copy.json"
                                showCopyDialog = true
                            },
                            onDelete = { pendingDelete = fileName }
                        )
                    }
                }
            }
        }
    }
}

// ======================== 5. 单行卡片组件 ========================

@Composable
fun FileItemCard(
    fileName: String,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF4CAF50))
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = fileName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "JSON文件", fontSize = 12.sp, color = Color.Gray)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "编辑", tint = Color(0xFF388E3C))
                }
                IconButton(onClick = onCopy) {
                    Icon(
                        Icons.Default.ContentCopy,
                        "复制",
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "删除", tint = Color.Red)
                }
            }
        }
    }
}

private fun getReadablePath(uriString: String?): String {
    if (uriString == null) return "未选择目录"
    return try {
        val uri = uriString.toUri()
        val decodedPath = android.net.Uri.decode(uri.path)
        val segment = decodedPath.substringAfterLast(":")
        segment.replace("/", " > ")
    } catch (_: Exception) {
        "已同步目录"
    }
}