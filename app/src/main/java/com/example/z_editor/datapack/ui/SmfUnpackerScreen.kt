package com.example.z_editor.datapack.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.example.z_editor.data.repository.LevelRepository
import com.example.z_editor.datapack.smf.SmfUnpacker
import com.example.z_editor.ui.theme.PvzBluePrimary
import com.example.z_editor.views.components.GateCard
import com.example.z_editor.views.components.OpenDocumentTreeFixed
import com.example.z_editor.views.components.openManageAllFilesSettings
import com.example.z_editor.views.components.rememberDebouncedClick
import com.example.z_editor.views.components.rememberManageStorageGranted
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** 自定义输出目录在 `datapack_prefs` 里的键，存的是**真实路径字符串**。 */
private const val KEY_OUTPUT_DIR = "smf_unpack_output_dir"

/** 默认输出目录名，落在外部存储根下 —— 与加自定义目录之前的行为一致。 */
private const val DEFAULT_OUTPUT_DIR_NAME = "Z_editor"

/**
 * SMF/RSB Unpacker screen.
 *
 * Reads the template via SAF from packer/original/ (same input as the packer),
 * writes the extracted files with java.io.File into:
 *
 *   <输出目录>/<模板名>/
 *
 * 输出目录默认是 /storage/emulated/0/Z_editor/，可用「更改输出目录」换成任意可写的
 * 本地目录（存真实路径，不存 tree uri —— 写入根本不经过 provider）。
 *
 * Writes to a real public folder need MANAGE_EXTERNAL_STORAGE (Android 11+,
 * granted only through system Settings — no runtime dialog).  The permission
 * state is re-checked on every ON_RESUME because the settings activity does
 * not reliably return a result.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmfUnpackerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
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
    var originalDirUri by remember { mutableStateOf<Uri?>(null) }

    // Template files in packer/original/
    var templateFiles by remember { mutableStateOf<List<UnpackDisplayFile>>(emptyList()) }
    var selectedTemplate by remember { mutableStateOf<UnpackDisplayFile?>(null) }

    // State
    var isUnpacking by remember { mutableStateOf(false) }
    var clearingDir by remember { mutableStateOf(false) }
    var unpackResult by remember { mutableStateOf<SmfUnpacker.UnpackResult?>(null) }
    var unpackError by remember { mutableStateOf<String?>(null) }
    var progressDone by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(0) }
    var progressName by remember { mutableStateOf<String?>(null) }

    var showHelpDialog by remember { mutableStateOf(false) }

    val themeColor = PvzBluePrimary

    // ---- Permission ----
    val hasManageStorage = rememberManageStorageGranted()

    // ---- Output dir ----
    // 存的是**真实路径字符串**而不是 tree uri：解包本来就靠 java.io.File 直接写、不经过
    // provider，所以不需要 SAF 授权；而未持久化的 uri 重启后就是死的，存它反而是个陷阱。
    var customOutputBasePath by remember {
        mutableStateOf(prefs.getString(KEY_OUTPUT_DIR, null))
    }
    val outputBaseDir = customOutputBasePath?.let { File(it) }
        ?: File(Environment.getExternalStorageDirectory(), DEFAULT_OUTPUT_DIR_NAME)
    val baseName = selectedTemplate?.name?.substringBeforeLast('.')?.ifBlank { "unpacked" } ?: ""
    val outputDir = File(outputBaseDir, baseName)

    /**
     * 关卡库目录的真实路径 —— `rootFolderUri` 就是主界面存关卡的那个目录
     * （`mainPrefs.folder_uri`，回落 `datapack_folder_uri`）。解包产物堆进去会让主界面
     * 列表渲染失败，选目录时要挡住。换算不出来（云盘）时为 null，此时只剩文案提示。
     */
    val levelLibraryPath = rootFolderUri?.let { LevelRepository.realPathStringOf(it) }

    val outputDirPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentTreeFixed()
    ) { picked ->
        val path = picked?.let { LevelRepository.realPathStringOf(it) }
        when {
            picked == null -> Unit

            path == null || !LevelRepository.isRawPathUsable(picked) ->
                Toast.makeText(
                    context, "该目录无法换算为本地路径或不可写，请换一个", Toast.LENGTH_SHORT
                ).show()

            // 子目录同样要拦：产物写成 <输出目录>/<模板名>/，选在关卡库下面照样塞爆它
            levelLibraryPath != null && LevelRepository.isSameOrInsidePath(path, levelLibraryPath) ->
                Toast.makeText(
                    context,
                    "不能选关卡库目录（含其子目录）：大量解包文件堆积会让主界面列表渲染失败",
                    Toast.LENGTH_LONG
                ).show()

            else -> {
                prefs.edit { putString(KEY_OUTPUT_DIR, path) }
                customOutputBasePath = path
                Toast.makeText(context, "输出目录已更改", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---- Helpers ----

    /** Ensure packer/ subdirectories exist under the SAF root — same as the packer page. */
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
    }

    fun scanTemplates() {
        val dirUri = originalDirUri ?: return
        scope.launch {
            val files = withContext(Dispatchers.IO) {
                val dir = DocumentFile.fromTreeUri(context, dirUri)
                    ?: return@withContext emptyList<UnpackDisplayFile>()
                dir.listFiles()
                    .filter { it.isFile }
                    .mapNotNull { f ->
                        val name = f.name ?: return@mapNotNull null
                        UnpackDisplayFile(name, f.uri, f.length())
                    }
                    .sortedBy { it.name.lowercase() }
            }
            templateFiles = files
            if (selectedTemplate == null && files.isNotEmpty()) {
                selectedTemplate = files.first()
            }
        }
    }

    fun doUnpack() {
        val template = selectedTemplate ?: return
        if (!hasManageStorage) {
            Toast.makeText(context, "需要「所有文件访问」权限", Toast.LENGTH_SHORT).show()
            return
        }
        val targetDir =
            File(outputBaseDir, template.name.substringBeforeLast('.').ifBlank { "unpacked" })

        isUnpacking = true
        clearingDir = true
        unpackResult = null
        unpackError = null
        progressDone = 0
        progressTotal = 0
        progressName = null

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                // Clear stale files from a previous run of the same template
                // (deleting a big directory takes a while — surfaced in the UI).
                if (targetDir.exists()) targetDir.deleteRecursively()
                clearingDir = false
                SmfUnpacker.unpackSmf(
                    context = context,
                    inputUri = template.uri,
                    outputRootDir = targetDir,
                    options = SmfUnpacker.UnpackOptions()
                ) { d, t, n ->
                    progressDone = d
                    progressTotal = t
                    progressName = n
                }
            }

            isUnpacking = false

            result.fold(
                onSuccess = { r ->
                    unpackResult = r
                    Toast.makeText(
                        context,
                        "解包完成！已写入 ${r.fileCount} 个文件",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onFailure = { e ->
                    val msg = e.message ?: "未知错误"
                    unpackError = msg
                    Toast.makeText(context, "解包失败: $msg", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // ---- Init ----
    LaunchedEffect(Unit) {
        ensurePackerDirs()
        scanTemplates()
    }

    // ---- UI ----
    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text("SMF 解包", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "返回",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { scanTemplates() }) {
                        Icon(
                            Icons.Default.Refresh,
                            "刷新",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            "帮助",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
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
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ---- Permission gate ----
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                item {
                    GateCard(
                        title = "需要 Android 11（API 30）或更高版本",
                        body = "解包结果写入公共目录需要「所有文件访问」权限，仅 Android 11+ 支持。",
                        buttonLabel = null,
                        onButton = {}
                    )
                }
            } else if (!hasManageStorage) {
                item {
                    GateCard(
                        title = "需要「所有文件访问」权限",
                        body = "解包结果需要按真实路径写入本地目录，该权限允许应用直接写入。" +
                                "请到系统设置中开启。",
                        buttonLabel = "去授权",
                        onButton = { openManageAllFilesSettings(context) }
                    )
                }
            }

            // ---- Template selection ----
            item {
                SectionHeader(
                    icon = Icons.Default.InsertDriveFile, title = "选择数据包",
                    subtitle = "来自 packer/original/"
                )
            }

            if (templateFiles.isEmpty()) {
                item {
                    EmptyHint(
                        icon = Icons.Default.FolderOff,
                        message = "请将 .smf / .rsb 数据包放入",
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
                        onClick = { selectedTemplate = file }
                    )
                }
            }

            // ---- Output dir ----
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    icon = Icons.Default.Folder, title = "输出目录",
                    subtitle = if (customOutputBasePath == null) "默认目录，可自定义"
                    else "已自定义"
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            outputBaseDir.absolutePath,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "本次将写入 $baseName/ ，重复解包同一模板会先清空该子目录，其余内容不动。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Spacer(Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { outputDirPickerLauncher.launch(null) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                            ) { Text("更改输出目录") }
                            // 只有自定义过才给「恢复默认」，否则是个点了没反应的按钮
                            if (customOutputBasePath != null) {
                                OutlinedButton(onClick = {
                                    prefs.edit { remove(KEY_OUTPUT_DIR) }
                                    customOutputBasePath = null
                                }) { Text("恢复默认") }
                            }
                        }
                    }
                }
            }

            // ---- Progress ----
            if (isUnpacking) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                if (clearingDir) "正在清除原有文件..." else "解包中...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            if (clearingDir) {
                                LinearProgressIndicator(
                                    color = themeColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                val fraction =
                                    if (progressTotal > 0) progressDone.toFloat() / progressTotal else 0f
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    color = themeColor,
                                    strokeCap = StrokeCap.Butt,
                                    gapSize = 0.dp,
                                    drawStopIndicator = {},
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (clearingDir) "正在删除上次解包的文件，文件较多时可能需要一些时间"
                                else "$progressDone / $progressTotal" +
                                        (progressName?.let { " — $it" } ?: ""),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // ---- Result ----
            unpackResult?.let { result ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "解包完成",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            ResultRow("解包文件", "${result.fileCount} 个")
                            ResultRow("写入大小", formatSize(result.bytesWritten))
                            ResultRow("子组处理", "${result.subgroupsProcessed} 个")
                            if (result.skippedImages > 0) ResultRow(
                                "跳过图片",
                                "${result.skippedImages} 个"
                            )
                            if (result.skippedZeroLength > 0) ResultRow(
                                "跳过空文件",
                                "${result.skippedZeroLength} 个"
                            )
                            if (result.skippedOob > 0) ResultRow(
                                "越界跳过",
                                "${result.skippedOob} 个"
                            )
                            if (result.skippedUnsafePaths > 0) ResultRow(
                                "不安全路径",
                                "${result.skippedUnsafePaths} 个"
                            )
                            if (result.skippedInvalid > 0) ResultRow(
                                "无效条目",
                                "${result.skippedInvalid} 个"
                            )
                            if (result.sanitizedCount > 0) ResultRow(
                                "路径改写",
                                "${result.sanitizedCount} 个"
                            )
                            ResultRow("输出目录", result.outputDir.absolutePath)
                        }
                    }
                }
            }

            unpackError?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Error,
                                    null,
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "解包失败", fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onError, fontSize = 16.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(error, fontSize = 13.sp, color = MaterialTheme.colorScheme.onError)
                        }
                    }
                }
            }

            // ---- Unpack button ----
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { doUnpack() },
                    enabled = hasManageStorage && selectedTemplate != null && !isUnpacking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUnpacking) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (clearingDir) "正在清除原有文件..." else "解包中...",
                            fontSize = 16.sp
                        )
                    } else {
                        Icon(Icons.Default.Unarchive, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("开始解包", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ---- Info footer ----
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "解包会提取数据包内全部文件到公共目录。" +
                            "输入模板来自 packer/original/，解包过程请不要息屏或将应用置于后台。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // ---- Help dialog ----
    if (showHelpDialog) {
        EditorHelpDialog(
            title = "SMF 解包说明",
            onDismiss = { showHelpDialog = false },
            themeColor = themeColor
        ) {
            HelpSection(
                title = "功能介绍",
                body = "SMF 解包工具用于将游戏数据包（SMF/RSB 容器文件）解包为独立文件，便于查看和修改内部 RTON / 图片资源。"
            )
            HelpSection(
                title = "使用步骤",
                body = "1. 首次使用点击「去授权」，在系统设置中开启「所有文件访问」\n" +
                        "2. 将 .smf / .rsb 数据包放入 SAF 的 packer/original/ 目录\n" +
                        "3. 选择数据包模板，点击「开始解包」，等待进度完成\n" +
                        "4. 用系统文件管理器打开输出目录查看结果"
            )
            HelpSection(
                title = "输出目录",
                body = "解包结果写入 <输出目录>/<模板名>/，重复解包同一模板会先清空该子目录，" +
                        "输出目录下的其他内容不受影响。\n" +
                        "默认输出目录是 /storage/emulated/0/Z_editor/，点「更改输出目录」" +
                        "可以换成任意可写的本地文件夹。"
            )
            HelpSection(
                title = "不要选成存放关卡的目录",
                body = "输出目录不要选成主界面存放关卡的那个文件夹（也包括它的子目录）。\n" +
                        "一个数据包解包后会产生成千上万个文件，堆在关卡库里会让主界面的" +
                        "关卡列表渲染失败。\n" +
                        "本工具会直接拒绝这类选择并提示。若确实想放在附近，请选一个和关卡库" +
                        "平级、而不是嵌套在其中的目录。"
            )
            HelpSection(
                title = "注意事项",
                body = "• 需要 Android 11（API 30）或更高版本\n" +
                        "• 输出目录必须能换算成本地真实路径，云盘类目录选不了\n" +
                        "• 解包会提取数据包内全部文件，原样保留加密的 .rton 密文\n" +
                        "• 越界、空文件、非法路径的条目会被跳过并在结果卡中计数"
            )
        }
    }
}

// ---- Data ----

private data class UnpackDisplayFile(
    val name: String,
    val uri: Uri,
    val size: Long
)

// ---- Components ----

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                title, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 26.dp)
        )
    }
}

@Composable
private fun EmptyHint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    path: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                message, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                path, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TemplateCard(
    file: UnpackDisplayFile,
    isSelected: Boolean,
    themeColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                Text(
                    file.name, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    formatSize(file.size), fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    "已选择",
                    tint = themeColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
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
