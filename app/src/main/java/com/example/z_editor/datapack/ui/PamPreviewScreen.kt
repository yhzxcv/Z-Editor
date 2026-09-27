package com.example.z_editor.datapack.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.datapack.pam.PamPreviewRunner
import com.example.z_editor.datapack.pam.PamTimeline
import com.example.z_editor.datapack.pam.PamUnpackScan
import com.example.z_editor.ui.theme.PvzBluePrimary
import com.example.z_editor.views.components.GateCard
import com.example.z_editor.views.components.openManageAllFilesSettings
import com.example.z_editor.views.components.rememberDebouncedClick
import com.example.z_editor.views.components.rememberManageStorageGranted
import com.example.z_editor.views.editor.pages.others.EditorContentWindowInsets
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** 原色的 tint，与 `PamTimeline` 里同一个约定：等于它就不必挂颜色滤镜。 */
private const val NO_TINT = 0xFFFFFF

/**
 * 忙碌提示的默认文案（解析）。扫描、识别各有一句，都写进 `busyText`，
 * 因为三者共用 `isLoading` 这一个闸门。
 */
private const val LOADING_TEXT = "正在解析..."

/** 「解包产物目录」在 `datapack_prefs` 里的键。下次进页面直接回填，省得再翻一遍文件管理器。 */
private const val PREFS_UNPACK_ROOT = "pam_preview_unpack_root"

/**
 * 连一条可用取景框都拿不到时的兜底画布边长（时间线为空、PAM 既没声明尺寸又一帧画不出东西）。
 * 正常路径不会走到这里 —— 取景框由 `PamTimeline.bounds` 按内容并集算，见 [PreviewCanvas]。
 */
private const val FALLBACK_CANVAS = 768.0

/**
 * 一条时间线的三份求值产物，一起算、一起落 state。
 *
 * 合成一个类是为了让它们在**同一个 try** 里算完：三者都依赖 `applyFrame`，
 * 一条损坏的时间线会让三个都抛，分开写只会得到三份不一致的半成品。
 */
private class TimelineData(
    val frames: List<List<PamTimeline.DrawOp>>,
    val labels: List<PamTimeline.LabelSpan>,
    val layers: List<PamTimeline.LayerRef>,
    val error: String?,
)

/**
 * PAM 动画预览（独立工具页）。
 *
 * 「动画转换」页能把 `.PAM` 转成 JSON 改，但**改完看不见效果**——只能解包进游戏才知道。
 * 本页接上 RTON 图集链，把动画按帧率播出来，让"改得对不对"当场可见。
 *
 * 需要三份输入，**两条路子**：一是用户手填 PAM 文件、图集目录（`ATLASES/`）、RTON 清单
 * （`RESOURCES*.RTON`），正对应 [PamPreviewRunner.Input] 的三项；二是指定一个解包产物根目录，
 * 由 [PamUnpackScan] 自动认出这三样再填进上面三个框。解析与求值在 [PamPreviewRunner] /
 * [PamTimeline]。
 *
 * **内存**：一张图集整张解码是 64 MB 级，所以 [PamPreviewRunner.load] 逐张解码、裁完即弃，
 * 位图用完必须 `close()`（见下面的 `DisposableEffect`）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PamPreviewScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val handleBack = rememberDebouncedClick { onBack() }

    val prefs = remember { context.getSharedPreferences("datapack_prefs", Context.MODE_PRIVATE) }

    var pamPath by remember { mutableStateOf("") }
    var atlasPath by remember { mutableStateOf("") }
    var rtonPath by remember { mutableStateOf("") }
    var pathError by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<PamPreviewRunner.Preview?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // 自动识别那条路子的状态。`unpackRoot` 回填上次用过的目录（只记扫成功的，见 scanForPams）。
    var unpackRoot by remember {
        mutableStateOf(prefs.getString(PREFS_UNPACK_ROOT, "").orEmpty())
    }
    var pamEntries by remember { mutableStateOf<List<PamUnpackScan.Entry>>(emptyList()) }
    var pamListTotal by remember { mutableIntStateOf(0) }
    var showPamPicker by remember { mutableStateOf(false) }
    var scanNotes by remember { mutableStateOf<List<String>>(emptyList()) }

    // 扫描 / 识别 / 解析三件事共用 isLoading 这一个闸门，所以只有文案是分开的
    var busyText by remember { mutableStateOf(LOADING_TEXT) }

    // 播放状态
    var spriteSel by remember { mutableIntStateOf(0) }
    var frame by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var frames by remember { mutableStateOf<List<List<PamTimeline.DrawOp>>>(emptyList()) }
    var framesError by remember { mutableStateOf<String?>(null) }

    // 标签与图层，同样随时间线重算
    var labelSpans by remember { mutableStateOf<List<PamTimeline.LabelSpan>>(emptyList()) }
    var layerRefs by remember { mutableStateOf<List<PamTimeline.LayerRef>>(emptyList()) }
    var showLayers by remember { mutableStateOf(false) }

    // 隐藏集合**按时间线各记一份**（键 = `SpriteRef.index`，主时间线是 -1）：切到别的精灵
    // 再切回来得还原成原样，所以不能只留一份。只有换了 PAM 才清空 —— 图层号只对同一份
    // PAM 里的同一条时间线有意义，跨 PAM 沿用会张冠李戴。
    var hiddenByTimeline by remember { mutableStateOf<Map<Int, Set<Int>>>(emptyMap()) }

    // 放在 isLoading 声明之后：这里和顶部箭头置灰读的是同一个标志
    BusyBackHandler(busy = isLoading, busyMessage = "解析中，完成前无法返回", onBack = handleBack)

    val themeColor = PvzBluePrimary
    val hasManageStorage = rememberManageStorageGranted()

    // 换了 PAM 就把上一份位图还掉，否则连点几次「加载」会攒下几十 MB
    DisposableEffect(preview) {
        val held = preview
        onDispose { held?.close() }
    }

    // 求值：切换 PAM 或切换精灵时，把它这条时间线的每一帧都先算好。
    // 纯算术（几十万次浮点），但帧数可达 2200+，所以放 Default 而不是主线程。
    LaunchedEffect(preview, spriteSel) {
        val p = preview ?: return@LaunchedEffect
        val ref = p.timelines.getOrNull(spriteSel) ?: return@LaunchedEffect
        // 错误随结果一起带回主线程再落 state，不在 Default 线程上写 Compose 状态
        val computed = withContext(Dispatchers.Default) {
            try {
                TimelineData(
                    frames = (0 until ref.frameCount).map { PamTimeline.evaluate(p.pam, ref.index, it) },
                    labels = PamTimeline.labels(p.pam, ref.index),
                    layers = PamTimeline.layers(p.pam, ref.index),
                    error = null,
                )
            } catch (ex: Exception) {
                TimelineData(
                    emptyList(), emptyList(), emptyList(),
                    "第 ${ref.name} 条时间线求值失败：${ex.message ?: ex.javaClass.simpleName}"
                )
            }
        }
        frames = computed.frames
        framesError = computed.error
        labelSpans = computed.labels
        layerRefs = computed.layers
        frame = 0
    }

    // 换 PAM 才清空隐藏记录（切精灵不清，见 hiddenByTimeline 的说明）
    LaunchedEffect(preview) { hiddenByTimeline = emptyMap() }

    // 当前这条时间线的隐藏集合 + 两个写入口。写的时候只动当前这条的记录，别的原样留着。
    val currentTimelineIndex = preview?.timelines?.getOrNull(spriteSel)?.index
    val hiddenLayers = currentTimelineIndex?.let { hiddenByTimeline[it] } ?: emptySet()

    fun setLayerHidden(layer: Int, hide: Boolean) {
        val key = currentTimelineIndex ?: return
        val cur = hiddenByTimeline[key] ?: emptySet()
        hiddenByTimeline = hiddenByTimeline + (key to if (hide) cur + layer else cur - layer)
    }

    // 三个批量按钮的落点：整个集合由弹窗算好（它才知道搜索过滤掉了哪些）再交过来
    fun setHiddenSet(next: Set<Int>) {
        val key = currentTimelineIndex ?: return
        hiddenByTimeline = hiddenByTimeline + (key to next)
    }

    // 按精灵自己的帧率推进。playing / frames 一变就重启，所以暂停即取消。
    val rate = preview?.let { rateOf(it, spriteSel) } ?: 30.0
    LaunchedEffect(playing, frames, rate) {
        if (!playing || frames.isEmpty()) return@LaunchedEffect
        val period = (1000.0 / rate).toLong().coerceAtLeast(16L)
        while (true) {
            delay(period)
            frame = (frame + 1) % frames.size
        }
    }

    fun load() {
        val pamStr = pamPath.trim().removePrefix("file://").removePrefix("content://")
        val atlasStr = atlasPath.trim().removePrefix("file://").removePrefix("content://")
        val rtonStr = rtonPath.trim().removePrefix("file://").removePrefix("content://")

        if (pamStr.isEmpty() || atlasStr.isEmpty() || rtonStr.isEmpty()) {
            pathError = "PAM 文件、图集目录、RTON 文件三项都要填"
            return
        }
        val pamFile = File(pamStr)
        val atlasDir = File(atlasStr)
        val rtonFile = File(rtonStr)
        if (!pamFile.isFile) {
            pathError = "PAM 文件不存在：${pamFile.absolutePath}"
            return
        }
        if (!atlasDir.isDirectory) {
            pathError = "图集目录不存在或不是目录：${atlasDir.absolutePath}"
            return
        }
        if (!rtonFile.isFile) {
            pathError = "RTON 文件不存在：${rtonFile.absolutePath}"
            return
        }

        val key = prefs.getString("encryption_key", "") ?: ""

        isLoading = true
        busyText = LOADING_TEXT
        pathError = null
        playing = false
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                PamPreviewRunner.load(
                    PamPreviewRunner.Input(pamFile, atlasDir, rtonFile),
                    key,
                )
            }
            isLoading = false
            when (result) {
                is PamPreviewRunner.LoadResult.Ok -> {
                    spriteSel = 0
                    frame = 0
                    preview = result.preview
                }

                is PamPreviewRunner.LoadResult.Failed -> {
                    preview = null
                    pathError = result.message
                }
            }
        }
    }

    /**
     * 扫「解包产物目录」，把里面的 .PAM 列进弹窗让用户挑。
     *
     * 扫描放 IO 线程：真产物上万个文件，在主线程走一遍会掉帧。**注意这不是"选文件"** ——
     * 页面拿不到系统目录树里的相对结构，所以自己走一遍文件系统，见 [PamUnpackScan.listPams]。
     */
    fun scanForPams() {
        val typed = unpackRoot.trim()
        val root = File(typed.removePrefix("file://").removePrefix("content://"))
        if (typed.isEmpty()) {
            pathError = "先填解包产物目录（解包出来的那个根目录）"
            return
        }

        isLoading = true
        busyText = "正在扫描 PAM…"
        pathError = null
        scanNotes = emptyList()
        scope.launch {
            val result = withContext(Dispatchers.IO) { PamUnpackScan.listPams(root) }
            isLoading = false
            when (result) {
                is PamUnpackScan.ListResult.Failed -> {
                    pamEntries = emptyList()
                    pamListTotal = 0
                    pathError = result.message
                }

                is PamUnpackScan.ListResult.Ok -> {
                    pamEntries = result.entries
                    pamListTotal = result.total
                    if (result.entries.isEmpty()) {
                        pathError = "这个目录下没有 .PAM 文件：${root.absolutePath}"
                    } else {
                        showPamPicker = true
                        // 只记扫成功的路径 —— 打错的那次不该覆盖掉上次好用的那个
                        prefs.edit().putString(PREFS_UNPACK_ROOT, typed).apply()
                    }
                }
            }
        }
    }

    /**
     * 选中一份 PAM：认出图集目录与 RTON，填进三个输入框，认全了就立刻加载。
     *
     * 认不全时**不加载**，只把缺什么说出来：塞个空目录进去只会让下游报
     * 「图集目录里没有 .PTX / .png」这种看不出根因的错（[PamUnpackScan.locate] 的注释同理）。
     */
    fun adoptPam(pam: File) {
        val root = File(unpackRoot.trim().removePrefix("file://").removePrefix("content://"))
        showPamPicker = false
        isLoading = true
        busyText = "正在识别图集与 RTON…"
        pathError = null
        scope.launch {
            val layout = withContext(Dispatchers.IO) { PamUnpackScan.locate(root, pam) }
            isLoading = false

            // 三个框都写上新值（认不出的留空），再走一遍正常加载 —— 用户随后还能手改任一项
            pamPath = layout.pam.absolutePath
            atlasPath = layout.atlasDir?.absolutePath.orEmpty()
            rtonPath = layout.rtonFile?.absolutePath.orEmpty()
            scanNotes = layout.notes
            // 换 PAM 了，旧动画与它无关；留着会让人以为下面报的错是这份旧动画的
            preview = null

            if (layout.complete) {
                load()
            } else {
                pathError = "在这份 PAM 附近没认全要用的东西：\n" +
                        layout.notes.joinToString("\n") { "• $it" } +
                        "\n可以照下面三项手动指定。"
            }
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text("动画预览", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = handleBack,
                        modifier = Modifier.alpha(if (isLoading) DISABLED_ALPHA else 1f),
                        enabled = !isLoading
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
            // 本页只读文件、不写盘，但读的是 /sdcard 下的真实路径，仍需该权限
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                item {
                    GateCard(
                        title = "需要 Android 11（API 30）或更高版本",
                        body = "预览需要按真实路径读取解包产物，仅 Android 11+ 支持。",
                        buttonLabel = null,
                        onButton = {}
                    )
                }
            } else if (!hasManageStorage) {
                item {
                    GateCard(
                        title = "需要「所有文件访问」权限",
                        body = "预览需要按真实路径读取 PAM、图集与 RTON。请到系统设置中开启。",
                        buttonLabel = "去授权",
                        onButton = { openManageAllFilesSettings(context) }
                    )
                }
            }

            // ---- Unpack root：第二条入口，选目录自动认 ----
            item {
                SectionHeader(
                    icon = Icons.Default.Folder,
                    title = "解包产物目录",
                    subtitle = "指到解包出来的那个根目录，本页列出里面的 .PAM 并自动识别图集与资源 RTON"
                )
            }
            item {
                PathInputField(
                    value = unpackRoot,
                    onValueChange = {
                        unpackRoot = it
                        pathError = null
                        pamEntries = emptyList()
                        scanNotes = emptyList()
                    },
                    placeholder = "输入解包产物根目录的完整路径",
                    themeColor = themeColor,
                    pick = PathPick.Directory,
                    enabled = !isLoading
                )
            }
            item {
                Button(
                    onClick = { scanForPams() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("浏览并选择 PAM…", fontSize = 15.sp)
                }
            }

            // 识别说明：挑哪份 RTON、往上找到了哪层根，这类"我替你决定了"的事都要摆出来
            if (scanNotes.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("自动识别", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.height(4.dp))
                            scanNotes.forEach { Text("• $it", fontSize = 12.sp) }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "—— 可选择手动指定下面三项填入 ——",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ---- Path inputs ----
            item {
                SectionHeader(
                    icon = Icons.Default.Movie,
                    title = "PAM 文件",
                    subtitle = "要预览的 .PAM，可以是「动画转换」页改完转回来的那一份"
                )
            }
            item {
                PathInputField(
                    value = pamPath,
                    onValueChange = { pamPath = it; pathError = null; preview = null },
                    placeholder = "输入 .PAM 文件的完整路径",
                    themeColor = themeColor,
                    pick = PathPick.File,
                    enabled = !isLoading
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                SectionHeader(
                    icon = Icons.Default.Folder,
                    title = "图集目录",
                    subtitle = "解包目录下的 ATLASES/。档位要与 PAM 一致（768 的 PAM 配 768 的图集）"
                )
            }
            item {
                PathInputField(
                    value = atlasPath,
                    onValueChange = { atlasPath = it; pathError = null; preview = null },
                    placeholder = "输入 ATLASES 目录的完整路径",
                    themeColor = themeColor,
                    pick = PathPick.Directory,
                    enabled = !isLoading
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                SectionHeader(
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    title = "资源 RTON",
                    subtitle = "解包目录下的 PROPERTIES/RESOURCES*.RTON，记录每张图片的裁剪矩形"
                )
            }
            item {
                PathInputField(
                    value = rtonPath,
                    onValueChange = { rtonPath = it; pathError = null; preview = null },
                    placeholder = "输入 RESOURCES*.RTON 的完整路径",
                    themeColor = themeColor,
                    pick = PathPick.File,
                    enabled = !isLoading
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { load() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (preview == null) "加载并预览" else "重新加载", fontSize = 15.sp)
                }
            }

            pathError?.let { err ->
                item { ErrorBanner(title = "无法预览", body = err) }
            }

            if (isLoading) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(busyText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                color = themeColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // ---- Loaded ----
            preview?.let { p ->
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader(
                        icon = Icons.Default.Movie,
                        title = "解析结果",
                        subtitle = "图片已按 RTON 的矩形从图集里裁出"
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
                            ResultRow("PAM 版本", "v${p.pam.version}")
                            ResultRow("图片", "${p.pam.image.size} 张")
                            ResultRow("精灵", "${p.pam.sprite.size} 个")
                            ResultRow("解析成功", "${p.images.size} / ${p.pam.image.size} 张")
                            ResultRow("时间线", "${p.timelines.size} 条")
                            if (p.skippedPlaceholders > 0) ResultRow(
                                "跳过占位图",
                                "${p.skippedPlaceholders} 张（1×1）"
                            )
                            p.note?.let { ResultRow("说明", it) }
                        }
                    }
                }

                if (p.failures.isNotEmpty()) {
                    item {
                        ErrorBanner(
                            title = "有 ${p.failures.size} 项没能解析",
                            body = p.failures.joinToString("\n")
                        )
                    }
                }

                // ---- Player ----
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader(
                        icon = Icons.Default.PlayArrow,
                        title = "播放",
                        subtitle = "选一条时间线，按它的帧率播放"
                    )
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // 时间线可能几十条，横向滚动挑
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                p.timelines.forEachIndexed { i, ref ->
                                    FilterChip(
                                        selected = i == spriteSel,
                                        onClick = { spriteSel = i; playing = false },
                                        label = { Text("${ref.name} (${ref.frameCount})", fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = themeColor,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // 图层管理里关掉的层不画。嵌套子树展开出来的 op 记的是**父槽位号**
                            // （见 `PamTimeline.expand`），所以关一层就是连它整棵子树一起关。
                            val visibleOps = remember(frames, frame, hiddenLayers) {
                                frames.getOrNull(frame).orEmpty().filter { it.layer !in hiddenLayers }
                            }
                            PreviewCanvas(
                                preview = p,
                                timelineIndex = spriteSel,
                                ops = visibleOps,
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { playing = !playing },
                                    enabled = frames.isNotEmpty()
                                ) {
                                    Icon(
                                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        if (playing) "暂停" else "播放",
                                        tint = themeColor
                                    )
                                }
                                Text(
                                    if (frames.isEmpty()) "无可播放的帧"
                                    else "${frame + 1} / ${frames.size}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.width(12.dp))
                                // 帧率只对"当前这条时间线"有意义，写出来免得用户以为是全局的
                                Text(
                                    "${"%.0f".format(rate)} fps",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.weight(1f))
                                // 一条标签都没有的时间线（实测 565 个 PAM 里只有 1 个）不必给入口，
                                // 那时 labels() 依然会给一个 label=null 的整段，光看 isNotEmpty 是骗人的
                                if (labelSpans.any { it.label != null }) {
                                    LabelMenu(
                                        labels = labelSpans,
                                        current = frame,
                                        themeColor = themeColor,
                                        onJump = { target ->
                                            playing = false
                                            // 标签段是按整条时间线的帧号算的，
                                            // 而 frames 可能因求值失败为空
                                            frame = target.coerceIn(0, (frames.size - 1).coerceAtLeast(0))
                                        },
                                    )
                                }
                            }

                            if (frames.size > 1) {
                                Slider(
                                    value = frame.toFloat(),
                                    onValueChange = {
                                        playing = false
                                        frame = it.toInt().coerceIn(0, frames.size - 1)
                                    },
                                    valueRange = 0f..(frames.size - 1).toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = themeColor,
                                        activeTrackColor = themeColor
                                    )
                                )
                            }

                            // 图层管理的入口放在播放卡里（开关图层要一边看画面一边点，
                            // 弹窗盖住画布也没关系，因为弹窗外的画布还在实时重绘）。
                            // 只有一层（实测中位数就是 1）就没有可管理的东西，不给入口。
                            if (layerRefs.size > 1) {
                                Spacer(Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { showLayers = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColor)
                                ) {
                                    Icon(
                                        Icons.Default.Layers, null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("图层管理", fontSize = 13.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (hiddenLayers.isEmpty()) "${layerRefs.size} 层"
                                        else "已隐藏 ${hiddenLayers.size} / ${layerRefs.size} 层",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                framesError?.let { err ->
                    item { ErrorBanner(title = "求值失败", body = err) }
                }
            }
        }
    }

    if (showLayers) {
        LayerDialog(
            layers = layerRefs,
            hidden = hiddenLayers,
            themeColor = themeColor,
            onToggle = { idx -> setLayerHidden(idx, idx !in hiddenLayers) },
            onSetHidden = { next -> setHiddenSet(next) },
            onDismiss = { showLayers = false },
        )
    }

    if (showPamPicker) {
        PamPickerDialog(
            entries = pamEntries,
            total = pamListTotal,
            themeColor = themeColor,
            onPick = { adoptPam(it) },
            onDismiss = { showPamPicker = false },
        )
    }

    if (showHelpDialog) {
        EditorHelpDialog(
            title = "动画预览说明",
            onDismiss = { showHelpDialog = false },
            themeColor = themeColor
        ) {
            HelpSection(
                title = "功能介绍",
                body = "把 PAM 动画在屏幕上播出来。改完 PAM 转成 JSON、改好再转回去之后，" +
                        "可以用这里确认改动是否如愿，不必解包进游戏。"
            )
            HelpSection(
                title = "配置方法",
                body = "PAM 只记「第几帧第几个槽位摆在哪」，不含图片本身、也不含从图集哪里裁。" +
                        "所以需要以下配置才能播放：\n" +
                        "• 图集目录（ATLASES/）提供图片像素\n" +
                        "• RTON 清单提供每张图片的裁剪矩形\n" +
                        "三者配齐才能画出画面。"
            )
            HelpSection(
                title = "标签",
                body = "有些 PAM 会给帧打标签，一个标签覆盖连续的一段帧。\n" +
                        "播放控件右边按钮显示当前落在哪个标签。点击下拉框内的标签会从该段的第一帧开始播。\n" +
                        "开头或结尾没有标签的帧会列成 `（未命名段）`。"
            )
            HelpSection(
                title = "图层",
                body = "图层数超过 1 时，播放卡底部会出现「图层管理」按钮，" +
                        "勾掉某一层就不画它，方便看清被挡住的部分。"
            )
        }
    }
}

/** 取一条时间线的帧率：精灵自报的优先，退回 PAM 的全局帧率。 */
private fun rateOf(preview: PamPreviewRunner.Preview, sel: Int): Double {
    val ref = preview.timelines.getOrNull(sel) ?: return preview.pam.frameRate.toDouble()
    val sprite = if (ref.index == PamTimeline.MAIN_SPRITE) {
        preview.pam.mainSprite
    } else {
        preview.pam.sprite.getOrNull(ref.index)
    }
    val r = sprite?.frameRate ?: 0.0
    return if (r > 0.0) r else preview.pam.frameRate.toDouble()
}

/**
 * 把一帧的绘制指令画出来。
 *
 * **必须用 `nativeCanvas.drawBitmap(bmp, Matrix, Paint)`**：真样本里 move 的矩阵位
 * 534,009 个、单位矩阵 0 个，`DrawScope.drawImage` 那种只支持平移缩放的 API 不够用。
 *
 * 坐标链：位图像素 → 帧变换（[PamTimeline.DrawOp]）→ 整个 PAM 空间等比缩放到控件、居中。
 */
@Composable
private fun PreviewCanvas(
    preview: PamPreviewRunner.Preview,
    timelineIndex: Int,
    ops: List<PamTimeline.DrawOp>,
) {
    val view = remember(preview, timelineIndex) {
        val b = preview.bounds.getOrNull(timelineIndex) ?: return@remember null
        val pad = maxOf(b.width, b.height) * 0.02
        PamTimeline.Bounds(b.minX - pad, b.minY - pad, b.maxX + pad, b.maxY + pad)
    }
    val viewW = view?.width?.takeIf { it > 0.0 } ?: FALLBACK_CANVAS
    val viewH = view?.height?.takeIf { it > 0.0 } ?: FALLBACK_CANVAS
    val ratio = (viewW / viewH).coerceIn(0.25, 4.0)

    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    }
    val matrix = remember { Matrix() }
    val values = remember { FloatArray(9) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio.toFloat())
    ) {
        if (ops.isEmpty()) return@Canvas

        val scale = minOf(size.width / viewW, size.height / viewH)
        val offsetX = (size.width - viewW * scale) / 2.0
        val offsetY = (size.height - viewH * scale) / 2.0
        val originX = view?.minX ?: 0.0
        val originY = view?.minY ?: 0.0

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            for (op in ops) {
                val bmp: Bitmap = preview.images[op.imageIndex] ?: continue

                values[0] = op.m0.toFloat(); values[1] = op.m1.toFloat(); values[2] = op.tx.toFloat()
                values[3] = op.m2.toFloat(); values[4] = op.m3.toFloat(); values[5] = op.ty.toFloat()
                values[6] = 0f; values[7] = 0f; values[8] = 1f
                matrix.setValues(values)
                matrix.postScale(scale.toFloat(), scale.toFloat())
                matrix.postTranslate(
                    (offsetX - originX * scale).toFloat(),
                    (offsetY - originY * scale).toFloat(),
                )

                paint.alpha = op.alpha
                paint.colorFilter =
                    if (op.tint == NO_TINT) null else tintFilter(op.tint)

                native.drawBitmap(bmp, matrix, paint)
            }
        }
    }
}

/** 逐通道乘算的染色滤镜。只有非原色时才建（真样本里约 0.3% 的帧会用到）。 */
private fun tintFilter(tint: Int): ColorMatrixColorFilter = ColorMatrixColorFilter(
    ColorMatrix(
        floatArrayOf(
            (tint shr 16 and 0xFF) / 255f, 0f, 0f, 0f, 0f,
            0f, (tint shr 8 and 0xFF) / 255f, 0f, 0f, 0f,
            0f, 0f, (tint and 0xFF) / 255f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

// ---- 标签菜单 ----

/**
 * 标签下拉菜单：显示"当前落在哪个标签"，点开按名字跳到该段的第一帧。
 *
 * **只留菜单、不留色块条**：色块条的宽度分配在只有一两个标签时毫无信息量（就一条铺满整行），
 * 在几十个标签（实测最长的一条时间线有 65 个）时又要横向滑半天才找得到目标。
 * 标签本质是**跳转入口**，不是时间刻度尺 —— 刻度尺旁边已经有滑块了。
 * 每段占的帧号范围在菜单里写出来，不影响跳转的精度。
 */
@Composable
private fun LabelMenu(
    labels: List<PamTimeline.LabelSpan>,
    current: Int,
    themeColor: Color,
    onJump: (Int) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val now = labels.firstOrNull { current in it.start..it.endInclusive }

    Box {
        TextButton(
            onClick = { menuOpen = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text(
                now?.label ?: "（未命名段）",
                fontSize = 12.sp,
                color = themeColor,
                fontWeight = FontWeight.Medium
            )
            Icon(
                Icons.Default.ArrowDropDown, "选择标签",
                tint = themeColor, modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            labels.forEach { span ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${span.label ?: "（未命名段）"}    ${span.start}–${span.endInclusive}",
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onJump(span.start)
                    }
                )
            }
        }
    }
}

// ---- 图层管理弹窗 ----

/**
 * 图层管理弹窗：一条时间线上出现过的所有绘制槽位，勾掉就不画，顶上带搜索框。
 *
 * 做成**弹窗**而不是内嵌卡片，是因为实测最多的一条时间线有 273 层
 * （`BACKGROUND_EIGHTIES_..._TOP.PAM`），内嵌的话这一块能撑满十几屏，
 * 把下面的帮助区挤到看不见；弹窗能有自己的固定高度和滚动。
 *
 * 搜索匹配**名字子串（不分大小写）或序号前缀** —— 名字是资源名（`akee_52x24` 这种），
 * 又长又不像人话，光靠眼睛在几百行里找某一层很痛苦，序号能直接搜。
 *
 * 图层号就是**槽位下标**（实测 144274 条 append 里槽位号一次都没被复用成别的东西，
 * 所以一个下标在整条时间线里是恒定身份）。嵌套 sprite 展开出来的 op 记的是父槽位号，
 * 勾掉一层等于连整棵子树一起关 —— 详见 `PamTimeline.LayerRef`。
 */
@Composable
private fun LayerDialog(
    layers: List<PamTimeline.LayerRef>,
    hidden: Set<Int>,
    themeColor: Color,
    onToggle: (Int) -> Unit,
    /** 整个隐藏集合的新值（三个批量按钮在弹窗里算好再交出来，见下）。 */
    onSetHidden: (Set<Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val shown = remember(layers, query) {
        val q = query.trim()
        if (q.isEmpty()) layers
        else layers.filter {
            it.name.contains(q, ignoreCase = true) || it.index.toString().startsWith(q)
        }
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    /**
     * 三个批量按钮**只作用于搜索出来的那些层**（用户 2026-09-27 定的）——
     * 搜出 3 条再点「全不选」，藏掉的只有那 3 条，否则搜索就白搜了。
     *
     * 集合运算本身在 [applyLayerBatch]（纯函数、有单测），这里只负责把搜索结果交进去。
     */
    val shownIndices = shown.map { it.index }.toSet()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("图层管理", fontSize = 18.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜名字或序号", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                    },
                    colors = datapackFieldColors(themeColor)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            onSetHidden(applyLayerBatch(hidden, shownIndices, LayerBatch.Show))
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) { Text("全选", fontSize = 13.sp) }
                    TextButton(
                        onClick = {
                            onSetHidden(applyLayerBatch(hidden, shownIndices, LayerBatch.Hide))
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) { Text("全不选", fontSize = 13.sp) }
                    TextButton(
                        onClick = {
                            onSetHidden(applyLayerBatch(hidden, shownIndices, LayerBatch.Invert))
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) { Text("反选", fontSize = 13.sp) }
                    if (query.isNotBlank()) {
                        // 有搜索时按钮的作用范围变了，不写出来会让人以为点错了
                        Spacer(Modifier.width(4.dp))
                        Text("共${shown.size}个结果", fontSize = 11.sp, color = muted)
                    }
                }
                // 普通 Column + 滚动，不用 LazyColumn：弹窗内容槽本身已在滚动容器里，
                // 再嵌一个同向懒列表要跟它抢测量约束，不值得。层数上限实测 273，铺得开。
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (shown.isEmpty()) {
                        Text("没有匹配的图层", fontSize = 13.sp, color = muted)
                    }
                    shown.forEach { layer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(layer.index) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = layer.index !in hidden,
                                onCheckedChange = { onToggle(layer.index) }
                            )
                            Text(
                                "${layer.index}",
                                fontSize = 12.sp,
                                color = muted,
                                modifier = Modifier.width(30.dp)
                            )
                            Text(
                                layer.name,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${layer.firstFrame}..${layer.lastFrame}",
                                fontSize = 11.sp,
                                color = muted
                            )
                        }
                    }
                }
            }
        },
        // 只有一个按钮：原来 dismissButton 那个「全部显示」和「全选」是同一件事，
        // 两个按钮挨着却做一样的事只会让人犹豫点哪个。
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
    )
}

/**
 * 从解包产物里挑一份 PAM。
 *
 * 列表可能上千条（真产物一个包组就有几百份动画），所以**两条都得有**：一次只列
 * [PamUnpackScan.MAX_LISTED_PAMS] 条 + 搜索框。搜索按**相对路径**匹配而不是文件名 ——
 * `AKEE.PAM` 在 384/768/1536 三档、好几个包组下都会重名，只搜文件名等于没搜。
 *
 * 用普通 `Column` + 滚动，**不用 `LazyColumn`**：理由同 [LayerDialog]（AlertDialog 的
 * 内容槽本身已在滚动容器里，同向嵌套要抢测量约束）。正因为不懒加载才必须配上限截断。
 */
@Composable
private fun PamPickerDialog(
    entries: List<PamUnpackScan.Entry>,
    total: Int,
    themeColor: Color,
    onPick: (File) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val shown = remember(entries, query) {
        val q = query.trim()
        if (q.isEmpty()) entries else entries.filter { it.relativePath.contains(q, ignoreCase = true) }
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择要预览的 PAM", fontSize = 18.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜相对路径", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                    },
                    colors = datapackFieldColors(themeColor)
                )
                if (total > entries.size) {
                    // 截断了就得说出来，否则用户会以为整个产物里就这么多
                    Text(
                        "共 $total 个，只列出前 ${entries.size} 个 —— 用搜索缩小范围",
                        fontSize = 11.sp,
                        color = muted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (shown.isEmpty()) {
                        Text("没有匹配的 PAM", fontSize = 13.sp, color = muted)
                    }
                    shown.forEach { entry ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(entry.file) }
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                entry.file.name,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                entry.relativePath,
                                fontSize = 11.sp,
                                color = muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        // 选中即生效，所以只剩一个「取消」
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

// ---- Components ----
// 与 SmfUnpackerScreen / AtlasSplitScreen 同款：这几个小组件在本项目里是各屏私有、
// 复制粘贴的，不是共享组件，这里按同样的惯例自带一份。
//
// 例外是路径输入框：它从 `PathInputField.kt` 来（那一份是共享的）—— 因为"点按钮选文件"
// 那套 launcher + uri→真实路径换算有十几行且**必须一致**，各屏各抄一份迟早抄歪。

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
