package com.example.z_editor.datapack.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.z_editor.datapack.hotupdate.HotUpdateJSONConverter
import com.example.z_editor.datapack.rton.RtonConverter
import com.example.z_editor.ui.theme.PvzBluePrimary
import com.example.z_editor.views.components.rememberDebouncedClick
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

// ---- Data ----

private enum class SourceFormat(val label: String, val icon: ImageVector, val color: Color) {
    PLAIN_JSON("普通 JSON", Icons.Default.Code, PvzBluePrimary),
    PLAIN_RTON("普通 RTON", Icons.Default.Description, Color(0xFFFF9800)),
    ENCRYPTED_RTON("加密 RTON", Icons.Default.Lock, Color(0xFFE91E63)),
    HOTUPDATE_JSON("热更新 JSON", Icons.Default.Security, Color(0xFF9C27B0))
}

private enum class ConvertDirection(
    val label: String,
    val icon: ImageVector,
    val sourceFormat: SourceFormat,
    val outputExtension: String,
    val isCryptoAction: Boolean = false
) {
    JSON_TO_PLAIN_RTON(
        "JSON → 普通 RTON",
        Icons.Default.Description,
        SourceFormat.PLAIN_JSON,
        "rton"
    ),
    JSON_TO_HOTUPDATE(
        "JSON → 热更新 JSON",
        Icons.Default.Security,
        SourceFormat.PLAIN_JSON,
        "json",
        true
    ),
    PLAIN_RTON_TO_JSON("普通 RTON → JSON", Icons.Default.Code, SourceFormat.PLAIN_RTON, "json"),
    PLAIN_RTON_TO_ENCRYPTED(
        "普通 RTON → 加密 RTON",
        Icons.Default.Lock,
        SourceFormat.PLAIN_RTON,
        "rton",
        true
    ),
    ENCRYPTED_RTON_TO_PLAIN(
        "加密 RTON → 普通 RTON",
        Icons.Default.Description,
        SourceFormat.ENCRYPTED_RTON,
        "rton",
        true
    ),
    HOTUPDATE_TO_JSON(
        "热更新 JSON → 普通 JSON",
        Icons.Default.Code,
        SourceFormat.HOTUPDATE_JSON,
        "json",
        true
    )
}

private data class ScannedFile(val name: String, val path: String, val format: SourceFormat)

private data class BatchResult(
    val direction: ConvertDirection,
    val succeeded: Int,
    val failed: Int,
    val unrelated: Int,
    val outputDir: File?
)

// ---- Pure helpers ----

/**
 * 产物命名规则：扩展名改变时直接用新扩展名（如 foo.rton → foo.json）；
 * 扩展名不变或目标已存在（冲突）时，在文件名末尾追加 `~` 直到不冲突
 * （如 foo.rton → foo.rton~）。放弃原先 _enc/_plain/_decoded/_encoded 后缀。
 */
private fun resolveUniqueOutputName(dir: File?, baseName: String, d: ConvertDirection): String {
    var name = "$baseName.${d.outputExtension}"
    while (dir != null && File(dir, name).exists()) {
        name += "~"
    }
    return name
}

/** Format detection is content-driven (no extension gating); order is mutually exclusive. */
private fun detectFormat(bytes: ByteArray): SourceFormat? {
    if (bytes.size >= 2 && bytes[0] == 0x10.toByte() && bytes[1] == 0x00.toByte()) {
        return SourceFormat.ENCRYPTED_RTON
    }
    if (bytes.size >= 4 &&
        bytes[0] == 'R'.code.toByte() && bytes[1] == 'T'.code.toByte() &&
        bytes[2] == 'O'.code.toByte() && bytes[3] == 'N'.code.toByte()
    ) {
        return SourceFormat.PLAIN_RTON
    }
    val prefix = String(bytes, 0, minOf(bytes.size, 4096), Charsets.UTF_8)
    if (HotUpdateJSONConverter.isHotUpdateFormat(prefix)) return SourceFormat.HOTUPDATE_JSON
    val trimmed = prefix.trim()
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) return SourceFormat.PLAIN_JSON
    return null
}

private fun convertBytes(d: ConvertDirection, bytes: ByteArray, key: String): ByteArray = when (d) {
    ConvertDirection.JSON_TO_PLAIN_RTON ->
        RtonConverter.jsonTextToRtonBytes(bytes.toString(Charsets.UTF_8))

    ConvertDirection.PLAIN_RTON_TO_JSON ->
        RtonConverter.rtonBytesToJsonText(bytes).toByteArray(Charsets.UTF_8)

    ConvertDirection.PLAIN_RTON_TO_ENCRYPTED ->
        RtonConverter.encryptRtonBytes(bytes, key)

    ConvertDirection.ENCRYPTED_RTON_TO_PLAIN ->
        RtonConverter.decryptRtonBytes(bytes, key)

    ConvertDirection.HOTUPDATE_TO_JSON ->
        HotUpdateJSONConverter.decodeHotUpdateString(bytes.toString(Charsets.UTF_8), key)
            .toByteArray(Charsets.UTF_8)

    ConvertDirection.JSON_TO_HOTUPDATE ->
        HotUpdateJSONConverter.encodeHotUpdateString(bytes.toString(Charsets.UTF_8), key)
            .toByteArray(Charsets.UTF_8)
}

// ---- Main screen ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchConvertScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val handleBack = rememberDebouncedClick { onBack() }
    BackHandler(onBack = handleBack)

    val prefs = remember { context.getSharedPreferences("datapack_prefs", Context.MODE_PRIVATE) }

    var inputPath by remember { mutableStateOf("") }
    var pathError by remember { mutableStateOf<String?>(null) }
    var analyzedFile by remember { mutableStateOf<File?>(null) }
    var isDirectory by remember { mutableStateOf(false) }
    var singleFileFormat by remember { mutableStateOf<SourceFormat?>(null) }
    var scannedGroups by remember { mutableStateOf<Map<SourceFormat, List<ScannedFile>>>(emptyMap()) }
    var scannedTotal by remember { mutableIntStateOf(0) }

    var isConverting by remember { mutableStateOf(false) }
    var clearingDir by remember { mutableStateOf(false) }
    var progressDone by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(0) }
    var progressName by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<BatchResult?>(null) }

    var encryptionKey by remember { mutableStateOf(prefs.getString("encryption_key", "") ?: "") }
    var showKeyDialog by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf(encryptionKey) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val themeColor = PvzBluePrimary

    // ---- Permission gate (reuses SmfUnpackerScreen pattern) ----

    fun storagePermissionGranted(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()

    var hasManageStorage by remember { mutableStateOf(storagePermissionGranted()) }

    fun openManageAllFilesSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            .setData(Uri.parse("package:${context.packageName}"))
        runCatching { context.startActivity(intent) }
            .onFailure { context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasManageStorage = storagePermissionGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ---- Analysis ----

    fun resetAnalysis() {
        analyzedFile = null
        isDirectory = false
        singleFileFormat = null
        scannedGroups = emptyMap()
        scannedTotal = 0
        result = null
    }

    fun scanFolder(f: File) {
        scope.launch {
            val groups = withContext(Dispatchers.IO) {
                val map = mutableMapOf<SourceFormat, MutableList<ScannedFile>>()
                f.listFiles()?.forEach { child ->
                    if (child.isFile) {
                        try {
                            val head = FileInputStream(child).use { input ->
                                val buf = ByteArray(4096)
                                val n = input.read(buf)
                                if (n > 0) buf.copyOf(n) else ByteArray(0)
                            }
                            val fmt = detectFormat(head)
                            if (fmt != null) {
                                map.getOrPut(fmt) { mutableListOf() }
                                    .add(ScannedFile(child.name, child.absolutePath, fmt))
                            }
                        } catch (_: Exception) {
                            // 单个文件读取失败不影响整体扫描
                        }
                    }
                }
                map.mapValues { it.value.toList() }
            }
            scannedGroups = groups
            scannedTotal = groups.values.sumOf { it.size }
        }
    }

    fun analyzePath() {
        val path = inputPath.trim().removePrefix("file://").removePrefix("content://")
        if (path.isEmpty()) {
            pathError = "请输入文件或文件夹路径"
            resetAnalysis()
            return
        }
        val f = File(path)
        if (!f.exists()) {
            pathError = "路径不存在：$path"
            resetAnalysis()
            return
        }
        if (f.isFile) {
            pathError = null
            resetAnalysis()
            analyzedFile = f
            isDirectory = false
            scope.launch {
                val fmt = withContext(Dispatchers.IO) {
                    try {
                        val head = FileInputStream(f).use { input ->
                            val buf = ByteArray(4096)
                            val n = input.read(buf)
                            if (n > 0) buf.copyOf(n) else ByteArray(0)
                        }
                        detectFormat(head)
                    } catch (_: Exception) {
                        null
                    }
                }
                singleFileFormat = fmt
            }
            return
        }
        if (f.isDirectory) {
            pathError = null
            resetAnalysis()
            analyzedFile = f
            isDirectory = true
            scanFolder(f)
            return
        }
        pathError = "既不是文件也不是文件夹：$path"
        resetAnalysis()
    }

    // ---- Conversion ----

    fun isApplicable(d: ConvertDirection): Boolean {
        if (!hasManageStorage || isConverting || analyzedFile == null) return false
        return if (isDirectory) {
            scannedGroups[d.sourceFormat].orEmpty().isNotEmpty()
        } else {
            singleFileFormat == d.sourceFormat
        }
    }

    fun directionCount(d: ConvertDirection): Int {
        if (analyzedFile == null) return 0
        return if (isDirectory) scannedGroups[d.sourceFormat].orEmpty().size else 1
    }

    fun showCompletionToast(o: BatchResult) {
        val msg = when {
            o.failed > 0 -> "转换完成：成功写入 ${o.succeeded}，失败 ${o.failed}（已跳过）"
            else -> "转换完成：成功写入 ${o.succeeded} 个"
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun runFileConversion(file: File, d: ConvertDirection) {
        isConverting = true
        result = null
        progressTotal = 1
        progressDone = 0
        progressName = file.name
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                try {
                    val inputBytes = file.readBytes()
                    val outBytes = convertBytes(d, inputBytes, encryptionKey)
                    val outFile = File(
                        file.parentFile,
                        resolveUniqueOutputName(
                            file.parentFile,
                            file.name.substringBeforeLast('.'),
                            d
                        )
                    )
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { it.write(outBytes) }
                    BatchResult(d, 1, 0, 0, outFile.parentFile)
                } catch (e: Exception) {
                    BatchResult(d, 0, 1, 0, null)
                }
            }
            progressDone = 1
            isConverting = false
            result = outcome
            showCompletionToast(outcome)
        }
    }

    fun runFolderConversion(dir: File, d: ConvertDirection) {
        val files = scannedGroups[d.sourceFormat].orEmpty()
        if (files.isEmpty()) return
        val parent = dir.parentFile ?: return
        val outputDir = File(parent, dir.name.ifBlank { "converted" } + "~")

        isConverting = true
        clearingDir = true
        result = null
        progressDone = 0
        progressTotal = files.size
        progressName = null

        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                if (outputDir.exists()) outputDir.deleteRecursively()
                clearingDir = false
                var ok = 0
                var fail = 0
                for (file in files) {
                    progressName = file.name
                    try {
                        val inputBytes = File(file.path).readBytes()
                        val outBytes = convertBytes(d, inputBytes, encryptionKey)
                        val outFile = File(
                            outputDir,
                            resolveUniqueOutputName(
                                outputDir,
                                file.name.substringBeforeLast('.'),
                                d
                            )
                        )
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { it.write(outBytes) }
                        ok++
                    } catch (e: Exception) {
                        fail++
                    }
                    progressDone++
                }
                BatchResult(d, ok, fail, scannedTotal - files.size, outputDir)
            }
            isConverting = false
            result = outcome
            showCompletionToast(outcome)
        }
    }

    fun onDirectionClick(d: ConvertDirection) {
        val f = analyzedFile ?: return
        if (d.isCryptoAction && encryptionKey.isBlank()) {
            Toast.makeText(context, "加解密操作需要先设置密钥", Toast.LENGTH_SHORT).show()
            keyInput = encryptionKey
            showKeyDialog = true
            return
        }
        if (isDirectory) runFolderConversion(f, d) else runFileConversion(f, d)
    }

    // ---- UI ----

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "批量文件格式转换",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
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
                    IconButton(onClick = {
                        keyInput = encryptionKey
                        showKeyDialog = true
                    }) {
                        Icon(
                            Icons.Default.Key,
                            "密钥",
                            tint = if (encryptionKey.isNotBlank()) Color(0xFFFFD54F)
                            else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
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
                        body = "转换结果直接写入公共目录需要「所有文件访问」权限，仅 Android 11+ 支持。",
                        buttonLabel = null,
                        onButton = {}
                    )
                }
            } else if (!hasManageStorage) {
                item {
                    GateCard(
                        title = "需要「所有文件访问」权限",
                        body = "转换结果将直接写入源文件所在目录（原位写回 / 文件夹名~ 目录），该权限允许应用直接读写。" +
                                "请到系统设置中开启。",
                        buttonLabel = "去授权",
                        onButton = { openManageAllFilesSettings() }
                    )
                }
            }

            // ---- Path input ----
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    icon = Icons.Default.InsertDriveFile,
                    title = "输入路径",
                    subtitle = "填写文件或文件夹的完整路径，解析后自动判断类型"
                )
            }
            item {
                OutlinedTextField(
                    value = inputPath,
                    onValueChange = {
                        inputPath = it
                        pathError = null
                        resetAnalysis()
                    },
                    placeholder = { Text("输入待处理文件（夹）路径") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        focusedBorderColor = themeColor,
                        focusedLabelColor = themeColor,
                        cursorColor = themeColor
                    )
                )
            }
            item {
                Button(
                    onClick = { analyzePath() },
                    enabled = !isConverting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("解析路径", fontSize = 15.sp)
                }
            }

            // ---- Analysis result ----
            pathError?.let { err ->
                item {
                    ErrorBanner(title = "无法解析路径", body = err)
                }
            }

            analyzedFile?.let { f ->
                if (isDirectory) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        SectionHeader(
                            icon = Icons.Default.CheckCircle,
                            title = "扫描结果",
                            subtitle = "共识别 $scannedTotal 个可转换文件（不含子目录）"
                        )
                    }
                    val visible = SourceFormat.entries
                        .map { fmt -> fmt to scannedGroups[fmt].orEmpty() }
                        .filter { it.second.isNotEmpty() }
                    if (visible.isEmpty()) {
                        item {
                            EmptyHint(
                                icon = Icons.Default.FolderOff,
                                message = "该文件夹内没有可转换的文件",
                                path = "支持 json / rton / 热更新格式"
                            )
                        }
                    } else {
                        items(visible) { (fmt, files) ->
                            FormatCard(format = fmt, count = files.size, themeColor = themeColor)
                        }
                    }
                } else {
                    item {
                        Spacer(Modifier.height(8.dp))
                        SectionHeader(
                            icon = Icons.Default.CheckCircle,
                            title = "识别结果",
                            subtitle = "根据文件内容检测格式，不依赖扩展名"
                        )
                    }
                    if (singleFileFormat != null) {
                        item {
                            FormatCard(
                                format = singleFileFormat!!,
                                count = null,
                                themeColor = themeColor
                            )
                        }
                    } else {
                        item {
                            ErrorBanner(
                                title = "无法识别文件格式",
                                body = "该文件不是 JSON / RTON / 热更新 JSON，所有转换方向不可用。"
                            )
                        }
                    }
                }
            }

            // ---- Direction buttons (fixed 6) ----
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    icon = Icons.Default.SwapHoriz,
                    title = "转换方向",
                    subtitle = "固定 6 种处理，点击直接执行；源格式不匹配的方向会自动禁用"
                )
            }
            if (analyzedFile != null) {
                items(ConvertDirection.entries) { d ->
                    DirectionButton(
                        direction = d,
                        enabled = isApplicable(d),
                        count = directionCount(d),
                        onClick = { onDirectionClick(d) }
                    )
                }
            }

            // ---- Progress ----
            if (isConverting) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                if (clearingDir) "正在清除原有文件..." else "转换中...",
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
                                if (clearingDir) "正在删除上次转换的输出，文件较多时可能需要一些时间"
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
            result?.let { r ->
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
                                    if (r.failed == 0) Icons.Default.CheckCircle else Icons.Default.Error,
                                    null,
                                    tint = if (r.failed == 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    if (r.failed == 0) "转换完成" else "转换完成（有失败）",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            ResultRow("转换方向", r.direction.label)
                            ResultRow("成功", "${r.succeeded} 个")
                            if (r.failed > 0) ResultRow("失败（已跳过）", "${r.failed} 个")
                            if (r.unrelated > 0) ResultRow("无关文件", "${r.unrelated} 个")
                            if (r.outputDir != null) ResultRow("输出目录", r.outputDir.absolutePath)
                        }
                    }
                }
            }

            // ---- Info footer ----
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "单文件产物写到源文件同目录；文件夹模式写入 <文件夹名>~ 目录，原目录不动。" +
                            "加解密操作需要先在顶栏设置密钥。转换失败的文件会自动跳过并提示。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
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
                    }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showKeyDialog = false }) {
                    Text("取消", color = themeColor)
                }
            }
        )
    }

    // ---- Help dialog ----
    if (showHelpDialog) {
        EditorHelpDialog(
            title = "批量文件格式转换说明",
            onDismiss = { showHelpDialog = false },
            themeColor = themeColor
        ) {
            HelpSection(
                title = "功能介绍",
                body = "将游戏数据文件（JSON / 普通 RTON / 加密 RTON / 热更新 JSON）批量转换为目标格式。\n" +
                        "在输入栏填写完整路径并点击「解析路径」，应用自动判断是文件还是文件夹。"
            )
            HelpSection(
                title = "单文件",
                body = "解析后根据文件内容识别格式，产物写到源文件同目录（原位写回）。"
            )
            HelpSection(
                title = "文件夹",
                body = "扫描当前文件夹（不递归子目录），按源格式分组计数，产物写入 <文件夹名>~ 目录，原目录不被修改。"
            )
            HelpSection(
                title = "六个转换方向",
                body = "JSON→普通RTON / JSON→热更新JSON / 普通RTON→JSON / 普通RTON→加密RTON / 加密RTON→普通RTON / 热更新JSON→JSON。\n" +
                        "源格式不匹配的方向会自动禁用；转换失败的文件跳过并提示。"
            )
            HelpSection(
                title = "注意事项",
                body = "• 需要 Android 11（API 30）以上和「所有文件访问」权限\n" +
                        "• 格式识别基于文件内容，不依赖扩展名\n" +
                        "• 加解密 RTON 和热更新 JSON 需要正确密钥\n" +
                        "• 输入 USB / 应用私有目录等无法直接访问的路径可能读取失败\n" +
                        "• 产物命名：扩展名改变直接用新扩展名；扩展名不变或目标已存在时自动追加 ~ 去重"
            )
        }
    }
}

// ---- Display helpers ----

@Composable
private fun GateCard(
    title: String,
    body: String,
    buttonLabel: String?,
    onButton: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    title, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                body, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onError.copy(alpha = 0.85f)
            )
            if (buttonLabel != null) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onButton,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onError,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) { Text(buttonLabel) }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
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
    icon: ImageVector,
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
private fun ErrorBanner(title: String, body: String) {
    // 纯色 error 容器 + onError 内容，与 SmfUnpackerScreen 解包失败样式一致
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
                    title, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError, fontSize = 16.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(body, fontSize = 13.sp, color = MaterialTheme.colorScheme.onError)
        }
    }
}

@Composable
private fun FormatCard(
    format: SourceFormat,
    count: Int?,
    themeColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = format.color.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                format.icon,
                null,
                tint = format.color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                format.label,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (count != null) {
                Text(
                    "$count 个文件",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    "已识别",
                    tint = themeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DirectionButton(
    direction: ConvertDirection,
    enabled: Boolean,
    count: Int,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = direction.sourceFormat.color.copy(alpha = 0.12f),
            contentColor = direction.sourceFormat.color,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            direction.icon,
            null,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) direction.sourceFormat.color
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            direction.label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
        if (enabled && count > 0) {
            Text(
                "$count 个",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
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
