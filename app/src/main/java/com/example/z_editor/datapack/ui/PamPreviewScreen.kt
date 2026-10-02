package com.example.z_editor.datapack.ui

import android.content.Context
import android.os.Build
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Save
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
import com.example.z_editor.datapack.pam.PamGifExporter
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

/**
 * 忙碌提示的默认文案（解析）。扫描、识别各有一句，都写进 `busyText`，
 * 因为三者共用 `isLoading` 这一个闸门。
 */
private const val LOADING_TEXT = "正在解析..."

/** 「解包产物目录」在 `datapack_prefs` 里的键。下次进页面直接回填，省得再翻一遍文件管理器。 */
private const val PREFS_UNPACK_ROOT = "pam_preview_unpack_root"

/** 导出最长边候选：null = 同取景框（原始尺寸，不缩放）。 */
private val GIF_LONG_EDGES: List<Pair<Int?, String>> = listOf(
    360 to "360",
    480 to "480",
    720 to "720",
    1080 to "1080",
    null to "同取景框",
)

/** 导出背景色候选。透明是默认（GIF 的透明是二值的，软边会被硬切）。 */
private val GIF_BACKGROUNDS: List<Pair<PamGifExporter.Background, String>> = listOf(
    PamGifExporter.Background.TRANSPARENT to "透明",
    PamGifExporter.Background.WHITE to "白",
    PamGifExporter.Background.BLACK to "黑",
)

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
 * 并且按 `PamAssets.previewScale` 给部件限了分辨率（4096² 图集下这是不 OOM 的关键）。
 * 位图用完必须 `close()`（见下面的 `DisposableEffect`）。
 *
 * 限了分辨率之后**位图比 `pam.image[].size` 小**，所以 [PreviewCanvas] 画之前要按
 * 「声明 ÷ 实际」把矩阵补回来，否则所有部件会一起缩到左上角。
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

    // ---- GIF 导出 ----
    // 导出遵循**当前时间线选择与当前隐藏图层**（所见即所得），并以载入时那个 PAM 的所在目录
    // 作为落盘位置 —— 所以载入成功时要把目录留下来（pamPath 之后可能被用户改掉）。
    var loadedPamDir by remember { mutableStateOf<File?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    // 进度与结果：只有导出卡片里的 item 读它们，重组范围被限制在那一张卡内
    var exportPhase by remember { mutableIntStateOf(1) }
    var exportDone by remember { mutableIntStateOf(0) }
    var exportTotal by remember { mutableIntStateOf(0) }
    var exportResult by remember { mutableStateOf<PamGifExporter.Result.Ok?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }

    // 导出选项
    var gifRange by remember { mutableIntStateOf(-1) } // -1 = 全部；否则是 labelSpans 的下标
    var gifLongEdge by remember { mutableStateOf<Int?>(720) } // null = 同取景框（原始尺寸）
    var gifBackground by remember { mutableStateOf(PamGifExporter.Background.TRANSPARENT) }
    var gifDither by remember { mutableStateOf(false) }
    var gifLoop by remember { mutableStateOf(true) }

    // 导出中也算忙碌 —— 这是**正确性**要求，不只是 UX：路径输入框的 onValueChange 会把
    // preview 置空，DisposableEffect 随即 close() 回收位图，而导出协程正在往那张位图上画。
    val busy = isLoading || isExporting

    // 放在 isLoading 声明之后：这里和顶部箭头置灰读的是同一个标志
    BusyBackHandler(
        busy = busy,
        busyMessage = if (isExporting) "导出中，完成前无法返回" else "解析中，完成前无法返回",
        onBack = handleBack,
    )

    val themeColor = PvzBluePrimary
    val hasManageStorage = rememberManageStorageGranted()

    // 换了 PAM 就把上一份位图还掉，否则连点几次「加载」会攒下几十 MB
    DisposableEffect(preview) {
        val held = preview
        onDispose { held?.close() }
    }

    // 求值：切换 PAM 或切换精灵时，把它这条时间线的每一帧都先算好。
    // 纯算术（几十万次浮点），但帧数可达 2200+，所以放 Default 而不是主线程。
    //
    // 走 `evaluateAll` 而不是 `(0 until frameCount).map { evaluate(...) }`：后者每帧都从第 0 帧
    // 重放，是 O(帧数²)，实测最坏一例（`ZOMBIE_DINO_STEGOSAURUS` 的 1936 帧）要 6.2 秒 ——
    // 这正是实机上"切个精灵就卡很久"的来源。切精灵本来就会重跑这一整段，所以换个精灵卡一次。
    LaunchedEffect(preview, spriteSel) {
        val p = preview ?: return@LaunchedEffect
        val ref = p.timelines.getOrNull(spriteSel) ?: return@LaunchedEffect
        // 错误随结果一起带回主线程再落 state，不在 Default 线程上写 Compose 状态
        val computed = withContext(Dispatchers.Default) {
            try {
                TimelineData(
                    frames = PamTimeline.evaluateAll(p.pam, ref.index),
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

    // 换 PAM 或换时间线：上一次的导出结果与范围选择都失效 —— 帧号与标签段都是**那条**时间线的
    LaunchedEffect(preview, spriteSel) {
        exportResult = null
        exportError = null
        gifRange = -1
    }

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

    /**
     * 导出 GIF。产物写在 PAM 源文件旁，重名加 `~`。
     *
     * 范围、图层、时间线都取自**当前选择**（所见即所得）；帧数据用当前已求值好的 `frames`，
     * 不重算。真正干活在 `Dispatchers.Default` 上（渲染两遍 + 量化 + LZW，中端机上秒级到分钟级）。
     */
    fun startExport() {
        val p = preview ?: return
        val dir = loadedPamDir ?: run {
            exportError = "拿不到 PAM 所在目录，无法确定写到哪里"
            return
        }
        val total = frames.size
        if (total == 0) return
        val span = labelSpans.getOrNull(gifRange)
        val start = (span?.start ?: 0).coerceIn(0, total - 1)
        val end = (span?.endInclusive ?: total - 1).coerceIn(start, total - 1)

        playing = false
        isExporting = true
        exportResult = null
        exportError = null
        exportPhase = 1
        exportDone = 0
        exportTotal = end - start + 1

        val options = PamGifExporter.Options(
            longEdge = gifLongEdge,
            background = gifBackground,
            dither = gifDither,
            loopForever = gifLoop,
        )
        val timelineName = p.timelines.getOrNull(spriteSel)?.name.orEmpty()
        val hidden = hiddenLayers
        val ops = frames
        val sel = spriteSel
        val fps = rateOf(p, spriteSel)
        // pamPath 此刻已被 !busy 闸住不可编辑，直接读就是"载入时那一份"
        val base = pamPath.trim()
            .removePrefix("file://").removePrefix("content://")
            .substringAfterLast('/').substringBeforeLast('.')

        scope.launch {
            val result = withContext(Dispatchers.Default) {
                val name = resolveUniqueOutputName(dir, gifBaseName(base, timelineName, span?.label), "gif")
                PamGifExporter.export(
                    preview = p,
                    timelineIndex = sel,
                    frames = ops,
                    hiddenLayers = hidden,
                    startFrame = start,
                    endInclusive = end,
                    fps = fps,
                    options = options,
                    output = File(dir, name),
                ) { phase, done, tot ->
                    // 与 SmfUnpackerScreen 同一个写法：回调在工作线程上直接落 state
                    exportPhase = phase
                    exportDone = done
                    exportTotal = tot
                }
            }
            isExporting = false
            when (result) {
                is PamGifExporter.Result.Ok -> {
                    exportResult = result
                    Toast.makeText(context, "已导出 ${result.file.name}", Toast.LENGTH_SHORT).show()
                }

                is PamGifExporter.Result.Failed -> {
                    exportError = result.message
                    Toast.makeText(context, "导出失败：${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
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
                    // 导出产物写在 PAM 源文件旁边：记下这一份的目录，之后用户改 pamPath 也不影响
                    loadedPamDir = pamFile.parentFile
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
            // 读写都是 /sdcard 下的真实路径（读 PAM/图集/RTON，**导出 GIF 时要写盘**），
            // 所以仍然需要这个权限 —— 文案里"只读"的说法随导出功能一起去掉了。
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                item {
                    GateCard(
                        title = "需要 Android 11（API 30）或更高版本",
                        body = "预览与导出需要按真实路径读写解包产物，仅 Android 11+ 支持。",
                        buttonLabel = null,
                        onButton = {}
                    )
                }
            } else if (!hasManageStorage) {
                item {
                    GateCard(
                        title = "需要「所有文件访问」权限",
                        body = "预览需要按真实路径读取 PAM、图集与 RTON；导出 GIF 时还要把产物写在 " +
                                "PAM 源文件旁边。请到系统设置中开启。",
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
                    enabled = !busy
                )
            }
            item {
                Button(
                    onClick = { scanForPams() },
                    enabled = !busy,
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
                    enabled = !busy
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
                    enabled = !busy
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
                    enabled = !busy
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { load() },
                    enabled = !busy,
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
                                        // 导出中不许切：切精灵会触发重量级的 evaluateAll 抢 CPU，
                                        // 而且会把导出脚下的 frames 换掉
                                        onClick = { spriteSel = i; playing = false },
                                        enabled = !busy,
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
                                    // 导出中不转播放：播放循环每帧改 state，会和导出抢渲染
                                    enabled = frames.isNotEmpty() && !busy
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

                // ---- Export GIF ----
                // 范围与帧数：范围下拉选「全部」或某个标签段。带标签切分后动画通常很短
                // （见帮助文案），"固定全帧不抽帧"才不会导出成几百帧的巨物。
                val gifSpan = labelSpans.getOrNull(gifRange)
                val gifStart = (gifSpan?.start ?: 0).coerceIn(0, (frames.size - 1).coerceAtLeast(0))
                val gifEnd = (gifSpan?.endInclusive ?: (frames.size - 1))
                    .coerceIn(gifStart, (frames.size - 1).coerceAtLeast(0))
                val gifFrameCount = if (frames.isEmpty()) 0 else gifEnd - gifStart + 1

                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader(
                        icon = Icons.Default.Save,
                        title = "导出 GIF",
                        subtitle = "按当前时间线与当前隐藏图层导出，产物写在 PAM 源文件旁边"
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("范围", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.width(8.dp))
                                GifRangeMenu(
                                    labels = labelSpans,
                                    frameCount = frames.size,
                                    selected = gifRange,
                                    themeColor = themeColor,
                                    enabled = !busy,
                                    onSelect = { gifRange = it },
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            Text("最长边", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GIF_LONG_EDGES.forEach { (edge, label) ->
                                    FilterChip(
                                        selected = gifLongEdge == edge,
                                        onClick = { gifLongEdge = edge },
                                        enabled = !busy,
                                        label = { Text(label, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = themeColor,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Text("背景", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GIF_BACKGROUNDS.forEach { (bg, label) ->
                                    FilterChip(
                                        selected = gifBackground == bg,
                                        onClick = { gifBackground = bg },
                                        enabled = !busy,
                                        label = { Text(label, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = themeColor,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = gifDither,
                                    onCheckedChange = { gifDither = it },
                                    enabled = !busy
                                )
                                Text("抖动（渐变更平滑，文件略大）", fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = gifLoop,
                                    onCheckedChange = { gifLoop = it },
                                    enabled = !busy
                                )
                                Text("无限循环", fontSize = 12.sp)
                            }

                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { startExport() },
                                enabled = !busy && frames.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (gifFrameCount == frames.size) "导出 GIF（全部 $gifFrameCount 帧）"
                                    else "导出 GIF（$gifFrameCount 帧）",
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                if (isExporting) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            // 进度只在这一张卡里读 —— 重组范围被限制在这，不惊动整屏
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    if (exportPhase == 1) "正在分析调色板…" else "正在写入 GIF…",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = {
                                        if (exportTotal <= 0) 0f
                                        else ((exportPhase - 1) + exportDone.toFloat() / exportTotal) / 2f
                                    },
                                    color = themeColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "第 $exportDone / $exportTotal 帧",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                exportResult?.let { r ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("导出完成", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(Modifier.height(6.dp))
                                ResultRow("文件", r.file.name)
                                ResultRow("大小", formatSize(r.byteSize))
                                ResultRow("帧数", "${r.frames} 帧")
                                ResultRow("尺寸", "${r.width} × ${r.height}")
                                ResultRow("每帧", "${r.delayCs * 10} 毫秒")
                            }
                        }
                    }
                }

                exportError?.let { err ->
                    item { ErrorBanner(title = "导出失败", body = err) }
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
            HelpSection(
                title = "导出 GIF",
                body = "把当前这条时间线导成 GIF 动图，画面与预览所见一致（含当前隐藏的图层）。\n" +
                        "• 范围：选「全部」或某个标签段。整条时间线可能上千帧，按标签切开后一段通常很短。\n" +
                        "• 固定全帧导出，不做抽帧 —— 帧数与播放完全一致。\n" +
                        "• 最长边决定画幅大小（同取景框 = 不缩放）。文件写在 PAM 源文件旁边，重名会自动加 `~`。\n" +
                        "GIF 格式本身的限制：最多 256 色，且**透明只有开或关两档** —— " +
                        "部件边缘的半透明会被切成硬边，需要干净边缘时请选白底或黑底。\n" +
                        "导出过程要渲染两遍（先分析调色板再写盘），画面大、帧数多时会花上一些时间，期间不能返回。"
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
 * 坐标链：位图像素 →（限分辨率时按 [imageFit] 放大回声明尺寸）→ 帧变换（[PamTimeline.DrawOp]）
 * → 整个 PAM 空间等比缩放到控件、居中。
 */
@Composable
private fun PreviewCanvas(
    preview: PamPreviewRunner.Preview,
    timelineIndex: Int,
    ops: List<PamTimeline.DrawOp>,
) {
    val view = remember(preview, timelineIndex) {
        PreviewTransform.paddedView(preview.bounds.getOrNull(timelineIndex))
    }
    val viewW = view?.width?.takeIf { it > 0.0 } ?: FALLBACK_CANVAS
    val viewH = view?.height?.takeIf { it > 0.0 } ?: FALLBACK_CANVAS
    val ratio = (viewW / viewH).coerceIn(0.25, 4.0)

    // 预览与导出共用同一个绘制器（见 [PamFramePainter] 类注释）：导出另建一个自己的实例。
    val painter = remember(preview) { PamFramePainter(preview) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio.toFloat())
    ) {
        drawIntoCanvas { canvas ->
            painter.draw(canvas.nativeCanvas, ops, size.width, size.height, view)
        }
    }
}

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

// ---- 导出辅助 ----

/**
 * 导出范围下拉：「全部（N 帧）」+ 各标签段。形态照 [LabelMenu] 的 TextButton+DropdownMenu。
 *
 * 带标签切分是有实际意义的：整条时间线可能有上千帧（最长实测 1936 帧），而按标签切开后
 * 一段通常只有几十帧 —— 导出的是"某段动作"而不是"整个动画"。
 */
@Composable
private fun GifRangeMenu(
    labels: List<PamTimeline.LabelSpan>,
    frameCount: Int,
    selected: Int,
    themeColor: Color,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val span = labels.getOrNull(selected)

    Box {
        TextButton(
            onClick = { menuOpen = true },
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text(
                if (span == null) "全部（$frameCount 帧）"
                else "${span.label ?: "（未命名段）"}（${span.endInclusive - span.start + 1} 帧）",
                fontSize = 12.sp,
                color = themeColor,
                fontWeight = FontWeight.Medium
            )
            Icon(
                Icons.Default.ArrowDropDown, "选择导出范围",
                tint = themeColor, modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("全部（$frameCount 帧）", fontSize = 13.sp) },
                onClick = {
                    menuOpen = false
                    onSelect(-1)
                }
            )
            labels.forEachIndexed { i, s ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${s.label ?: "（未命名段）"}    ${s.start}–${s.endInclusive}",
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onSelect(i)
                    }
                )
            }
        }
    }
}

/**
 * 目标文件名：换扩展名，已被占用就一路加 `~`。
 *
 * 本屏私有拷贝 —— 与「动画转换」页同款、同惯例（这几屏的这种小工具都是各自一份，
 * 不是共享组件）。走直写而不是 SAF，理由与那页相同：产物要和源文件放在一起。
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
