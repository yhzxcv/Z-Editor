package com.example.z_editor.datapack.ui

import android.content.Context
import android.os.Build
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.datapack.smf.AtlasSplitRunner
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

/** 拆分产物的固定子目录名，落在推导出的解包根下。 */
private const val IMAGE_DIR_NAME = "_images"

/**
 * 图集拆分（独立工具页）。
 *
 * 与「SMF 解包」页勾选开关的效果一样，区别是这个页面可以对着**任意已解包目录**用，
 * 不需要重新解包一次。两个入口共用 `AtlasSplitRunner`。
 *
 * 用户需要分别给出图集目录（`ATLASES/`）与 RTON 文件（`PROPERTIES/RESOURCES*.RTON`），
 * 输出根由两者推导（RTON 在 `PROPERTIES/` 下时取它的上一级），推导结果会显示在界面上。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtlasSplitScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val handleBack = rememberDebouncedClick { onBack() }
    BackHandler(onBack = handleBack)

    val prefs = remember { context.getSharedPreferences("datapack_prefs", Context.MODE_PRIVATE) }

    var atlasPath by remember { mutableStateOf("") }
    var rtonPath by remember { mutableStateOf("") }
    var pathError by remember { mutableStateOf<String?>(null) }
    var prepared by remember { mutableStateOf<AtlasSplitRunner.Prepared?>(null) }
    var imageDir by remember { mutableStateOf<File?>(null) }

    var isSplitting by remember { mutableStateOf(false) }
    var progressDone by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(0) }
    var progressName by remember { mutableStateOf<String?>(null) }
    var stats by remember { mutableStateOf<AtlasSplitRunner.Stats?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val themeColor = PvzBluePrimary
    val hasManageStorage = rememberManageStorageGranted()

    fun resetAnalysis() {
        prepared = null
        imageDir = null
        stats = null
    }

    fun analyze() {
        resetAnalysis()
        val atlasStr = atlasPath.trim().removePrefix("file://").removePrefix("content://")
        val rtonStr = rtonPath.trim().removePrefix("file://").removePrefix("content://")

        if (atlasStr.isEmpty() || rtonStr.isEmpty()) {
            pathError = "图集目录与 RTON 文件都要填"
            return
        }
        val atlasDir = File(atlasStr)
        val rtonFile = File(rtonStr)
        if (!atlasDir.isDirectory) {
            pathError = "图集目录不存在或不是目录：${atlasDir.absolutePath}"
            return
        }
        if (!rtonFile.isFile) {
            pathError = "RTON 文件不存在：${rtonFile.absolutePath}"
            return
        }

        // 图集拆分吃的是解包产物里的普通 RTON，本页不提供密钥配置入口。
        // 但独立页允许手填任意 RTON，万一碰上加密的，就借用「批量文件格式转换」
        // 页已经配好的那一份（同一个 datapack_prefs.encryption_key），不逼用户重配。
        val key = prefs.getString("encryption_key", "") ?: ""
        when (val r = AtlasSplitRunner.planFromFile(rtonFile, key)) {
            is AtlasSplitRunner.PlanResult.EncryptedNoKey -> {
                pathError = "该 RTON 已加密。密钥在「批量文件格式转换」页设置，设好后回到本页重试"
                return
            }

            is AtlasSplitRunner.PlanResult.Failed -> {
                pathError = r.message
                return
            }

            is AtlasSplitRunner.PlanResult.Ok -> {
                val dir = File(deriveOutputRoot(rtonFile, atlasDir), IMAGE_DIR_NAME)
                val p = AtlasSplitRunner.prepare(r.plan, atlasDir, dir)
                val blocking = p.blockingErrors()
                if (blocking.isNotEmpty()) {
                    pathError = blocking.joinToString("\n")
                    return
                }
                pathError = null
                prepared = p
                imageDir = dir
            }
        }
    }

    fun doSplit() {
        val p = prepared ?: return
        if (!hasManageStorage) {
            Toast.makeText(context, "需要「所有文件访问」权限", Toast.LENGTH_SHORT).show()
            return
        }
        isSplitting = true
        stats = null
        progressDone = 0
        progressTotal = 0
        progressName = null

        scope.launch {
            val s = withContext(Dispatchers.IO) {
                AtlasSplitRunner.run(p) { d, t, n ->
                    progressDone = d
                    progressTotal = t
                    progressName = n
                }
            }
            isSplitting = false
            stats = s
            Toast.makeText(context, "拆分完成，共 ${s.split} 张", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text("图集拆分", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
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
        // 本页有两个输入框：edge-to-edge 下不加这个键盘会直接盖住它们
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
                        body = "拆分结果写入公共目录需要「所有文件访问」权限，仅 Android 11+ 支持。",
                        buttonLabel = null,
                        onButton = {}
                    )
                }
            } else if (!hasManageStorage) {
                item {
                    GateCard(
                        title = "需要「所有文件访问」权限",
                        body = "拆分结果需要按真实路径写入本地目录，该权限允许应用直接写入。" +
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
                    title = "图集目录",
                    subtitle = "解包目录下的 ATLASES/，也可直接被转成 PNG 后的同名目录"
                )
            }
            item {
                OutlinedTextField(
                    value = atlasPath,
                    onValueChange = {
                        atlasPath = it
                        pathError = null
                        resetAnalysis()
                    },
                    placeholder = { Text("输入 ATLASES 目录的完整路径") },
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
                Spacer(Modifier.height(4.dp))
                SectionHeader(
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    title = "RTON 文件",
                    subtitle = "解包目录下的 PROPERTIES/RESOURCES*.RTON，记录每张图片的裁剪矩形"
                )
            }
            item {
                OutlinedTextField(
                    value = rtonPath,
                    onValueChange = {
                        rtonPath = it
                        pathError = null
                        resetAnalysis()
                    },
                    placeholder = { Text("输入 RESOURCES*.RTON 的完整路径") },
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
                // 与上面的 RTON 输入框拉开：LazyColumn 的 spacedBy 只有 8dp，按钮会显得贴在输入框上
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { analyze() },
                    enabled = !isSplitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("解析路径", fontSize = 15.sp)
                }
            }

            pathError?.let { err ->
                item {
                    ErrorBanner(title = "无法开始拆分", body = err)
                }
            }

            // ---- Analysis ----
            prepared?.let { p ->
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader(
                        icon = Icons.Default.Image,
                        title = "解析结果",
                        subtitle = "确认无误后开始拆分"
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
                            ResultRow("图集", "${p.resolvedAtlasCount} / ${p.plan.atlases.size} 张可用")
                            ResultRow("图片", "${p.plan.imageCount} 张")
                            ResultRow("将拆出", "${p.splittableCount(AtlasSplitRunner.Options())} 张")
                            if (p.missingAtlasNames.isNotEmpty()) ResultRow(
                                "缺少图集",
                                "${p.missingAtlasNames.size} 张（其图片将跳过）"
                            )
                            if (p.badRects.isNotEmpty()) ResultRow(
                                "越界矩形",
                                "${p.badRects.size} 个（将跳过）"
                            )
                            if (p.plan.orphans.isNotEmpty()) ResultRow(
                                "无主图片",
                                "${p.plan.orphans.size} 张（parent 不是图集，将跳过）"
                            )
                            ResultRow("输出目录", p.outputRoot.absolutePath)
                        }
                    }
                }

                // 缺图集是最常见的失败原因，单独列出来方便照名找文件
                if (p.missingAtlasNames.isNotEmpty()) {
                    item {
                        ErrorBanner(
                            title = "有 ${p.missingAtlasNames.size} 张图集在目录里找不到",
                            body = p.missingAtlasNames.take(8).joinToString("、") +
                                    if (p.missingAtlasNames.size > 8) " …" else ""
                        )
                    }
                }

                item {
                    Button(
                        onClick = { doSplit() },
                        enabled = !isSplitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCut, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("开始拆分", fontSize = 15.sp)
                    }
                }
            }

            // ---- Progress ----
            if (isSplitting) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "正在拆分图集...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
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
            stats?.let { s ->
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
                                    "拆分完成",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            ResultRow("拆出图片", "${s.split} 张")
                            ResultRow("使用图集", "${s.atlasUsed} 张")
                            if (s.fromPtx5x5 > 0) ResultRow("ASTC 5x5", "${s.fromPtx5x5} 张")
                            if (s.fromPtx6x6 > 0) ResultRow("ASTC 6x6", "${s.fromPtx6x6} 张")
                            if (s.fromPng > 0) ResultRow("来自 PNG", "${s.fromPng} 张")
                            // 刻意显示跳过数：否则用户会疑惑「为什么比预期少几张」
                            if (s.skippedPlaceholder > 0) ResultRow(
                                "跳过占位图",
                                "${s.skippedPlaceholder} 张（1×1 全透明）"
                            )
                            if (s.failed > 0) ResultRow("失败", "${s.failed} 张")
                            ResultRow("输出目录", s.outputDir.absolutePath)
                        }
                    }
                }

                if (s.failures.isNotEmpty()) {
                    item {
                        ErrorBanner(
                            title = "有 ${s.failed} 张未能拆出",
                            body = s.failures.joinToString("\n")
                        )
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        EditorHelpDialog(
            title = "图集拆分说明",
            onDismiss = { showHelpDialog = false },
            themeColor = themeColor
        ) {
            HelpSection(
                title = "功能介绍",
                body = "解包出来的 ATLASES/ 是一堆图片挤在一起的大图，每张图片该取哪一块记录在 " +
                        "PROPERTIES/RESOURCES*.RTON 里。本工具按这些记录把图片逐张拆出来。"
            )
            HelpSection(
                title = "使用步骤",
                body = "1. 先用「SMF 解包」把数据包解包到公共目录\n" +
                        "2. 图集目录填 <解包目录>/ATLASES\n" +
                        "3. RTON 文件填 <解包目录>/PROPERTIES/RESOURCES*.RTON\n" +
                        "4. 点「解析路径」确认张数，再点「开始拆分」"
            )
            HelpSection(
                title = "输出结构",
                body = "产物写入 <解包目录>/_images/，目录结构与 RTON 里记录的资源路径一致，" +
                        "例如 images/1200/dynamic/xxx.png。"
            )
            HelpSection(
                title = "加密 RTON",
                body = "解包产物里的 RESOURCES*.RTON 是普通 RTON，不加密、不需要密钥。" +
                        "只有手填其它来源的加密 RTON 时才用得上密钥，此时请先到" +
                        "「批量文件格式转换」页设置，本页会自动沿用。"
            )
            HelpSection(
                title = "其他入口",
                body = "「SMF 解包」页也有「图集拆分」开关，解包完直接拆，不必再走一遍本页。"
            )
        }
    }
}

/**
 * 推导输出根：RTON 在 `PROPERTIES/` 下时取它的上一级（即解包根），
 * 否则退到图集目录的上一级。推导结果会显示在界面上，不让用户猜。
 */
private fun deriveOutputRoot(rtonFile: File, atlasDir: File): File {
    val parent = rtonFile.parentFile
    if (parent != null && parent.name.equals("PROPERTIES", ignoreCase = true)) {
        parent.parentFile?.let { return it }
    }
    return atlasDir.parentFile ?: atlasDir
}

// ---- Components ----
// 与 SmfUnpackerScreen / BatchConvertScreen 同款：这几个小组件在本项目里是各屏私有、
// 复制粘贴的，不是共享组件，这里按同样的惯例自带一份。

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
        // 不 fillMaxWidth 的话，短标题（如「无法开始拆分」）会让卡片缩成一个窄块
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
