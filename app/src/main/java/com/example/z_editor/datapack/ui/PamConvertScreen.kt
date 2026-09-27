package com.example.z_editor.datapack.ui

import android.os.Build
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.datapack.pam.PamBinaryReader
import com.example.z_editor.datapack.pam.PamConverter
import com.example.z_editor.ui.theme.PvzBluePrimary
import com.example.z_editor.views.components.GateCard
import com.example.z_editor.views.components.openManageAllFilesSettings
import com.example.z_editor.views.components.rememberDebouncedClick
import com.example.z_editor.views.components.rememberManageStorageGranted
import com.example.z_editor.views.editor.pages.others.EditorContentWindowInsets
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val SNIFF_BYTES = 64
private const val MAX_SCAN_DEPTH = 32
private const val MAX_REPORTED_FAILURES = 20

private const val MAX_PAM_INPUT = 8L * 1024 * 1024

/** `JSON → PAM` 的输入上限。方向反过来，输入本来就是啰嗦的那一侧。 */
private const val MAX_JSON_INPUT = 64L * 1024 * 1024

/**
 * PAM ↔ JSON（独立工具页）。

 *
 * 产物一律写在**源文件旁边**（重名加 `~`），本页不管回包 —— 要打进包里请用
 * 「数据包文件管理」页把产物搬进补丁目录并改成与包内条目一致的文件名。
 *
 * 只做格式转换，不渲染、不拆图 —— 渲染成帧另开（需要接 RTON 图集链）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PamConvertScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val handleBack = rememberDebouncedClick { onBack() }

    var path by remember { mutableStateOf("") }
    var pathError by remember { mutableStateOf<String?>(null) }
    var scan by remember { mutableStateOf<Scan?>(null) }
    var direction by remember { mutableStateOf<PamConverter.Direction?>(null) }

    var isScanning by remember { mutableStateOf(false) }
    var isConverting by remember { mutableStateOf(false) }
    var progressDone by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(0) }
    var progressName by remember { mutableStateOf<String?>(null) }
    var stats by remember { mutableStateOf<ConvertStats?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val busy = isScanning || isConverting

    // 跑动中不给走：任务跑在页面的 scope 上，走了就既没结果也没提示
    BusyBackHandler(
        busy = busy,
        busyMessage = if (isConverting) "转换中，完成前无法返回" else "扫描中，完成前无法返回",
        onBack = handleBack
    )

    val themeColor = PvzBluePrimary
    val hasManageStorage = rememberManageStorageGranted()

    fun resetScan() {
        scan = null
        direction = null
        stats = null
    }

    fun analyze() {
        val raw = path.trim().removePrefix("file://").removePrefix("content://")
        if (raw.isEmpty()) {
            pathError = "请输入路径"
            return
        }
        val root = File(raw)
        if (!root.exists()) {
            pathError = "路径不存在：${root.absolutePath}"
            return
        }
        if (!root.isFile && !root.isDirectory) {
            pathError = "既不是文件也不是目录：${root.absolutePath}"
            return
        }

        resetScan()
        isScanning = true
        scope.launch {
            // 目录可能要递归几万个文件，绝不能在主线程走 —— 那是现成的 ANR
            val result = withContext(Dispatchers.IO) { runCatching { scanPath(root) } }
            isScanning = false
            result.fold(
                onSuccess = {
                    scan = it
                    // 与「批量文件格式转换」页「方向永不自动选」不同：这里的嗅探是
                    // 内容级的（magic / `{`），可靠，所以预选；仍允许手动改。
                    direction = it.suggestedDirection()
                    pathError = null
                },
                onFailure = { pathError = it.message ?: it.javaClass.simpleName }
            )
        }
    }

    fun doConvert() {
        val s = scan ?: return
        val dir = direction ?: return
        if (!hasManageStorage) {
            Toast.makeText(context, "需要「所有文件访问」权限", Toast.LENGTH_SHORT).show()
            return
        }
        val targets = s.targetsFor(dir)
        if (targets.isEmpty()) {
            Toast.makeText(context, "没有可转换的文件", Toast.LENGTH_SHORT).show()
            return
        }

        isConverting = true
        stats = null
        progressDone = 0
        progressTotal = targets.size
        progressName = null

        scope.launch {
            val r = withContext(Dispatchers.IO) {
                convertAll(targets, dir) { d, t, n ->
                    progressDone = d
                    progressTotal = t
                    progressName = n
                }
            }
            isConverting = false
            stats = r
            Toast.makeText(
                context,
                if (r.failed > 0) "转换完成：成功 ${r.succeeded}，失败 ${r.failed}"
                else "转换完成，共 ${r.succeeded} 个文件",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text("动画转换", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = handleBack,
                        // 跟下面的 enabled 一起压 alpha 才看得出灰：Icon 写死了 tint，
                        // 光靠 enabled=false 压不动它。
                        modifier = Modifier.alpha(if (busy) DISABLED_ALPHA else 1f),
                        enabled = !busy
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "返回",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
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
        // 本页有输入框：edge-to-edge 下不加这个键盘会直接盖住它
        contentWindowInsets = EditorContentWindowInsets(),
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
                        body = "转换结果写入公共目录需要「所有文件访问」权限，仅 Android 11+ 支持。",
                        buttonLabel = null,
                        onButton = {}
                    )
                }
            } else if (!hasManageStorage) {
                item {
                    GateCard(
                        title = "需要「所有文件访问」权限",
                        body = "转换结果需要按真实路径写入本地目录，该权限允许应用直接写入。" +
                                "请到系统设置中开启。",
                        buttonLabel = "去授权",
                        onButton = { openManageAllFilesSettings(context) }
                    )
                }
            }

            // ---- Path input ----
            item {
                SectionHeader(
                    icon = Icons.Default.Folder,
                    title = "输入路径",
                    subtitle = "可以是一个 .PAM / .json 文件，也可以是一个目录（递归处理里面所有文件）"
                )
            }
            item {
                PathInputField(
                    value = path,
                    onValueChange = {
                        path = it
                        pathError = null
                        resetScan()
                    },
                    placeholder = "输入文件或目录的完整路径",
                    themeColor = themeColor,
                    pick = PathPick.FileOrDirectory,
                    enabled = !busy
                )
            }
            item {
                // 与上面的输入框拉开：LazyColumn 的 spacedBy 只有 8dp，按钮会显得贴在输入框上
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { analyze() },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isScanning) "扫描中…" else "扫描路径", fontSize = 15.sp)
                }
            }

            pathError?.let { err ->
                item {
                    ErrorBanner(title = "无法开始转换", body = err)
                }
            }

            // ---- Scan result ----
            scan?.let { s ->
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader(
                        icon = Icons.Default.Description,
                        title = "识别结果",
                        subtitle = if (s.isDirectory) "按内容嗅探，不认扩展名" else "以下是要转换的文件"
                    )
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (s.isDirectory) {
                                ResultRow("动画文件（PAM）", "${s.pamFiles.size} 个")
                                ResultRow("JSON 文件", "${s.jsonFiles.size} 个")
                                if (s.unknownFiles > 0) ResultRow(
                                    "不认识的文件",
                                    "${s.unknownFiles} 个（将跳过）"
                                )
                                ResultRow("产物位置", "各源文件所在目录")
                            } else {
                                val f = s.pamFiles.firstOrNull() ?: s.jsonFiles.firstOrNull()
                                ResultRow("文件", f?.name ?: "")
                                ResultRow("识别为", s.formatLabel())
                                s.summary?.let { m ->
                                    ResultRow("PAM 版本", "v${m.version}")
                                    ResultRow("全局帧率", "${m.frameRate}")
                                    ResultRow("图集", "${m.imageCount} 张")
                                    ResultRow("精灵", "${m.spriteCount} 个")
                                    ResultRow("总帧数", "${m.frameCount}")
                                }
                            }
                        }
                    }
                }
            }

            // ---- Direction ----
            scan?.let { s ->
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader(
                        icon = Icons.AutoMirrored.Filled.DriveFileMove,
                        title = "转换方向",
                        subtitle = "已按识别结果预选，可以改。" +
                                "JSON 只是旁边多出来的一份，原来的文件不会被替换"
                    )
                }
                item {
                    // 单文件模式下即使嗅探失败也两个都可点 —— 用户有权强制指定方向；
                    // 目录模式下没有对应源文件的方向就没意义，直接置灰。
                    val pamOk = !s.isDirectory || s.pamFiles.isNotEmpty()
                    val jsonOk = !s.isDirectory || s.jsonFiles.isNotEmpty()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DirectionButton(
                            label = "PAM → JSON",
                            selected = direction == PamConverter.Direction.PamToJson,
                            enabled = !busy && pamOk,
                            themeColor = themeColor,
                            modifier = Modifier.weight(1f),
                            onClick = { direction = PamConverter.Direction.PamToJson }
                        )
                        DirectionButton(
                            label = "JSON → PAM",
                            selected = direction == PamConverter.Direction.JsonToPam,
                            enabled = !busy && jsonOk,
                            themeColor = themeColor,
                            modifier = Modifier.weight(1f),
                            onClick = { direction = PamConverter.Direction.JsonToPam }
                        )
                    }
                }

                direction?.let { d ->
                    val n = s.targetsFor(d).size
                    item {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { doConvert() },
                            enabled = !busy && n > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("开始转换（$n 个文件）", fontSize = 15.sp)
                        }
                    }
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
                            Text("正在转换...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(Modifier.height(8.dp))
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
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "$progressDone / $progressTotal" +
                                        (progressName?.takeIf { it.isNotEmpty() }?.let { " — $it" } ?: ""),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // ---- Result ----
            stats?.let { r ->
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
                                    "转换完成",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            ResultRow("方向", r.directionLabel())
                            ResultRow("成功", "${r.succeeded} 个文件")
                            if (r.failed > 0) ResultRow("失败", "${r.failed} 个文件")
                            ResultRow("写出", formatSize(r.bytesWritten))
                            r.firstOutput?.let { out ->
                                if (scan?.isDirectory == false) {
                                    ResultRow("输出文件", out.name)
                                } else {
                                    ResultRow("输出位置", out.parentFile?.absolutePath ?: "")
                                }
                            }
                        }
                    }
                }

                if (r.failures.isNotEmpty()) {
                    item {
                        ErrorBanner(
                            title = "有 ${r.failed} 个文件未能转换",
                            body = r.failures.joinToString("\n")
                        )
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        EditorHelpDialog(
            title = "动画转换说明",
            onDismiss = { showHelpDialog = false },
            themeColor = themeColor
        ) {
            HelpSection(
                title = "格式介绍",
                body = "PAM 是宝开自家的动画格式（PopCap Animation）。数据包解出来的 .PAM " +
                        "是二进制、变长记录，直接用文本编辑器打不开；转成 JSON 之后帧、精灵、" +
                        "命令结构都是可读可改的。"
            )
            HelpSection(
                title = "输入路径",
                body = "填一个文件，或者填一个目录（会递归处理里面所有文件）。\n" +
                        "文件类型按内容判断而不是扩展名。" +
                        "目录模式下产物写到各自源文件所在目录。"
            )
            HelpSection(
                title = "回包：把改好的 PAM 放回数据包",
                body = "1. 转换得到 JSON，改好\n" +
                        "2. 方向选 JSON → PAM，开始转换。产物写在源文件旁边，" +
                        "与源文件重名就加 `~`（`AKEE~.PAM`）\n" +
                        "3. 用「数据包文件管理」页把产物复制/移动进补丁目录，并改成与包内条目" +
                        "一致的文件名（去掉 `~`）\n" +
                        "4. 到「数据包补丁」页把该目录选为补丁目录，打包\n" +
                        "文件名必须与包内条目一致才匹配得上。"
            )
            HelpSection(
                title = "限制",
                body = "• 只支持 PAM v4–v6；v1–v3 的图集旋转编码是另一套，会明确报错\n" +
                        "• 单个 PAM 超过 8MB、单个 JSON 超过 64MB 会跳过（JSON 展开后太大，防止内存爆掉）\n" +
                        "• 本页只做格式转换；想验收效果，用「动画预览」页"
            )
        }
    }
}

// ---- 扫描与转换（纯 File，不碰 Compose） ----

/** 一次扫描的结果。文件按格式分成两堆，方向按钮直接照着点。 */
private class Scan(
    val isDirectory: Boolean,
    val pamFiles: List<File>,
    val jsonFiles: List<File>,
    /** 目录里既不像 PAM 也不像 JSON 的文件数。单文件模式下只会是 0 或 1。 */
    val unknownFiles: Int,
    /** 单文件模式下的摘要；目录模式不解码（逐个解一遍只为展示太贵），为 null。 */
    val summary: PamConverter.Summary?,
) {
    fun targetsFor(direction: PamConverter.Direction): List<File> =
        if (direction == PamConverter.Direction.PamToJson) pamFiles else jsonFiles

    /**
     * 预选方向。目录模式按「谁多听谁的」；一样多或都是 0 时不给建议，
     * 免得替用户瞎猜一个。
     */
    fun suggestedDirection(): PamConverter.Direction? = when {
        pamFiles.size > jsonFiles.size -> PamConverter.Direction.PamToJson
        jsonFiles.size > pamFiles.size -> PamConverter.Direction.JsonToPam
        else -> null
    }

    fun formatLabel(): String = when {
        pamFiles.isNotEmpty() -> "PAM 动画"
        jsonFiles.isNotEmpty() -> "JSON"
        else -> "不认识（不是 PAM 也不像 JSON）"
    }
}

private class ConvertStats(
    val direction: PamConverter.Direction,
    val succeeded: Int,
    val failed: Int,
    val bytesWritten: Long,
    /** 第一个写出的文件，用来在结果卡里指明产物落在哪。全部失败时为 null。 */
    val firstOutput: File?,
    /** 上限 [MAX_REPORTED_FAILURES] 条。 */
    val failures: List<String>,
) {
    fun directionLabel(): String = when (direction) {
        PamConverter.Direction.PamToJson -> "PAM → JSON"
        PamConverter.Direction.JsonToPam -> "JSON → PAM"
    }
}

private fun scanPath(root: File): Scan {
    if (root.isFile) {
        val format = PamConverter.detect(sniff(root))
        val isPam = format == PamConverter.SourceFormat.Pam
        val isJson = format == PamConverter.SourceFormat.Json
        // 解码只为取摘要 —— 让用户在点转换之前确认「转的是不是这个东西」。
        // 解不开不算扫描失败：转换那一步会给出更具体的报错，扔在失败明细里。
        val summary = if (isPam) {
            runCatching { PamConverter.summarize(PamBinaryReader.decode(root.readBytes())) }.getOrNull()
        } else {
            null
        }
        return Scan(
            isDirectory = false,
            pamFiles = if (isPam) listOf(root) else emptyList(),
            jsonFiles = if (isJson) listOf(root) else emptyList(),
            unknownFiles = if (isPam || isJson) 0 else 1,
            summary = summary,
        )
    }

    val pam = ArrayList<File>()
    val json = ArrayList<File>()
    var unknown = 0
    // walkTopDown 会跟符号链接，给个深度上限兜底
    root.walkTopDown().maxDepth(MAX_SCAN_DEPTH).forEach { f ->
        if (!f.isFile) return@forEach
        when (PamConverter.detect(sniff(f))) {
            PamConverter.SourceFormat.Pam -> pam += f
            PamConverter.SourceFormat.Json -> json += f
            PamConverter.SourceFormat.Unknown -> unknown++
        }
    }
    // 排序只为让进度条上的文件名顺序稳定，跟转换结果无关
    return Scan(
        isDirectory = true,
        pamFiles = pam.sortedBy { it.absolutePath },
        jsonFiles = json.sortedBy { it.absolutePath },
        unknownFiles = unknown,
        summary = null,
    )
}

/**
 * 读文件头用于嗅探。
 *
 * 单文件模式下 PAM 走的是 `looksLikePam`（只看 4 字节 magic），头 64 字节足够；
 * 读失败（权限、目录项损坏）当「不认识」处理，不在这里报错。
 */
private fun sniff(f: File): ByteArray = try {
    f.inputStream().use { input ->
        val buf = ByteArray(SNIFF_BYTES)
        val n = input.read(buf)
        if (n <= 0) ByteArray(0) else buf.copyOf(n)
    }
} catch (e: Throwable) {
    ByteArray(0)
}

private fun inputCapFor(direction: PamConverter.Direction): Long =
    if (direction == PamConverter.Direction.PamToJson) MAX_PAM_INPUT else MAX_JSON_INPUT

/**
 * 逐个转换。产物写在源文件同目录，重名加 `~`。
 *
 * 单个文件的失败不影响其余文件（与其余转换页一致），错误收进
 * [ConvertStats.failures] 一并展示。
 *
 * 早先这里还有个「回包输出目录」参数：填了就按**原名**写进指定目录，好让
 * `SmfPacker` 的扁平回退（按 basename 匹配）能认出产物。**已去掉** ——
 * 现在是把产物用「数据包文件管理」页搬进 `patch/` 文件夹，那条路不需要转换时就写对名字。
 */
private fun convertAll(
    targets: List<File>,
    direction: PamConverter.Direction,
    onProgress: (Int, Int, String) -> Unit,
): ConvertStats {
    val cap = inputCapFor(direction)
    var succeeded = 0
    var bytesWritten = 0L
    var firstOutput: File? = null
    val failures = ArrayList<String>()

    targets.forEachIndexed { i, src ->
        onProgress(i, targets.size, src.name)
        try {
            val length = src.length()
            if (length > cap) {
                throw IllegalArgumentException(
                    "${formatSize(length)} 超过 ${formatSize(cap)} 上限，跳过"
                )
            }
            val output = PamConverter.convert(src.readBytes(), direction)

            val base = src.name.substringBeforeLast('.', src.name)
            val dir = src.parentFile ?: throw IllegalArgumentException("拿不到所在目录")
            // resolveUniqueOutputName 见到已存在的名字就加 `~`，而源文件必然存在，
            // 所以这里不可能把产物写回源文件本身。
            val target = File(dir, resolveUniqueOutputName(dir, base, direction.outputExtension))
            target.writeBytes(output)

            succeeded++
            bytesWritten += output.size.toLong()
            if (firstOutput == null) firstOutput = target
        } catch (e: Throwable) {
            // 连 OutOfMemoryError 一起接：一个超大文件不该把整批转换带走
            failures += "${src.name}：${e.message ?: e.javaClass.simpleName}"
        }
    }
    onProgress(targets.size, targets.size, "")

    return ConvertStats(
        direction = direction,
        succeeded = succeeded,
        failed = targets.size - succeeded,
        bytesWritten = bytesWritten,
        firstOutput = firstOutput,
        failures = failures.take(MAX_REPORTED_FAILURES),
    )
}

/**
 * 目标文件名：换扩展名，已被占用就一路加 `~`。
 *
 * 只换扩展名、不动目录 —— 与「批量文件格式转换」的文件夹模式（产物收进 `<目录名>~`）
 * 不同，本页要求产物能一眼看出是哪个文件的对应物。
 */
private fun resolveUniqueOutputName(dir: File, baseName: String, extension: String): String {
    var name = withTargetExtension(baseName, extension)
    while (File(dir, name).exists()) name += "~"
    return name
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))} GB"
    }
}

// ---- Components ----
// 与 SmfUnpackerScreen / AtlasSplitScreen 同款：这几个小组件在本项目里是各屏私有、
// 复制粘贴的，不是共享组件，这里按同样的惯例自带一份。

@Composable
private fun DirectionButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    themeColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) themeColor else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

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
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun ErrorBanner(title: String, body: String) {
    Card(
        // 不 fillMaxWidth 的话，短标题会让卡片缩成一个窄块
        modifier = Modifier.fillMaxWidth(),
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
