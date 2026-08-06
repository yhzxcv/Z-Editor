package com.example.z_editor.views.editor.pages.others

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.ui.theme.DarkBlueBg
import com.example.z_editor.ui.theme.LightBlueBg
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzBlueDarkTheme
import com.example.z_editor.ui.theme.PvzBlueLight
import com.example.z_editor.ui.theme.PvzBluePrimary
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_UNDO_DEPTH = 100

/** 字体大小范围与默认值（顶部精简条 − / + 步进调整，步长 0.5）。 */
private const val MIN_FONT_SIZE = 6f
private const val MAX_FONT_SIZE = 12f
private const val DEFAULT_FONT_SIZE = 9f
private const val FONT_SIZE_STEP = 0.5f

/** 双击进入/退出编辑模式：两次点击的最大时间间隔。手写检测略放宽到 400ms，兼容慢速双击。 */
private const val DOUBLE_TAP_TIMEOUT_MS = 400L

/** 搜索输入防抖延迟。输入即时反馈到输入框，搜索逻辑用此延迟后的稳定值。 */
private const val SEARCH_DEBOUNCE_MS = 250L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonCodeViewerScreen(
    fileName: String,
    levelFile: PvzLevelFile?,
    onBack: () -> Unit,
    onPersistLevel: () -> Unit = {}
) {
    if (levelFile == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("数据为空")
        }
        return
    }

    val context = LocalContext.current
    val gson = remember { GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create() }

    // === 状态定义 ===
    var fontSize by remember { mutableFloatStateOf(DEFAULT_FONT_SIZE) }
    // 查看态（高亮 Text）↔ 编辑态（纯文本 BasicTextField）两态切换
    var isEditing by remember { mutableStateOf(false) }
    var editingValue by remember { mutableStateOf(TextFieldValue("")) }
    var syntaxError by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // 搜索状态（防抖：输入即时反馈到 searchQuery，搜索逻辑用 250ms 后的稳定值 debouncedSearchQuery）
    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }
    var searchCaseSensitive by remember { mutableStateOf(false) }
    var searchRegex by remember { mutableStateOf(false) }
    // 替换栏：初始隐藏，点一次「替换」展开；替换内容 + 是否已展开
    var replaceQuery by remember { mutableStateOf("") }
    var showReplaceBar by remember { mutableStateOf(false) }
    var activeMatchIndex by remember { mutableIntStateOf(-1) }
    // 编辑态文本含语法错误时，尝试退出弹出的确认弹窗；discardErrorPosition 保存出错位置（如 "第 3 行 第 12 列"）
    var showDiscardWarning by remember { mutableStateOf(false) }
    var discardErrorPosition by remember { mutableStateOf<String?>(null) }
    // 当前激活文本域的布局结果（查看态高亮 Text / 编辑态 BasicTextField 经 onTextLayout 写入）。
    // 换行后行号栏、进入编辑的光标定位、搜索命中跳转都需要它按可视行对齐。
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    // 缩放锚点（待应用）：字号变化前记录视口顶部的可视行（小数），字号变化并重新布局后把滚动位置
    // 恢复到 锚点×新行高，保证缩放时文本相对位置不变（行号不跳动）。
    // 关键：只在「没有待应用锚点」时捕获一次，且只在 scrollTo 成功后清空 —— 连续捏合/快速连点时
    // LaunchedEffect(fontSize) 每帧被取消、scroll 从未更新，若每个事件都重算锚点，
    // 最终只会按最后一个事件的微小缩放比例换算，导致欠调（字体翻倍 → 顶部行减半）。
    var zoomAnchorVisualLine by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isEmpty()) debouncedSearchQuery = ""
        else {
            delay(SEARCH_DEBOUNCE_MS)
            debouncedSearchQuery = searchQuery
        }
    }

    // 撤销/重做栈
    val undoStack = remember { ArrayDeque<TextFieldValue>() }
    val redoStack = remember { ArrayDeque<TextFieldValue>() }

    val focusRequester = remember { FocusRequester() }
    val commonVerticalScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // 序列化一次作为初始文本；refreshTrigger 变化（保存后）时重算
    val serializedJson = remember(refreshTrigger) { gson.toJson(levelFile) }
    // 文本只在 serializedJson 变化时重置（初始进入 / 保存后），编辑过程中不覆盖用户输入
    LaunchedEffect(serializedJson) {
        if (editingValue.text != serializedJson) {
            editingValue = TextFieldValue(serializedJson, TextRange(0))
            undoStack.clear()
            redoStack.clear()
        }
    }

    val searchActive = debouncedSearchQuery.isNotBlank()
    val searchOptions = JsonSearchOptions(caseSensitive = searchCaseSensitive, regex = searchRegex)

    // 搜索命中（基于当前编辑文本全文扫描）
    val editMatches = remember(searchVisible, debouncedSearchQuery, searchCaseSensitive, searchRegex, editingValue.text) {
        if (!searchVisible || debouncedSearchQuery.isBlank()) emptyList()
        else JsonSearchLogic.findMatches(editingValue.text, debouncedSearchQuery, searchOptions)
    }

    // palette 缓存：defaultPalette 每次重组都返回新实例，不缓存会让高亮 remember 每次都失效重算。
    // 配色已硬编码亮/暗两套（键=黄、字符串=绿），不再依赖 colorScheme，只随暗色开关切换。
    val colorScheme = MaterialTheme.colorScheme
    val darkTheme = LocalDarkTheme.current
    val palette = remember(darkTheme) { defaultPalette(darkTheme) }

    // 编辑态蓝色 scheme：仅在 isEditing 时通过嵌套 MaterialTheme 注入。
    // baseScheme 读在嵌套 theme 之外（保持绿色），查看态高亮配色不受影响。
    // 仅顶栏/光标/行号栏等随主题变蓝，背景保持原色（编辑态不再整体变色）。
    val editScheme = remember(colorScheme, darkTheme) {
        if (darkTheme) colorScheme.copy(
            primary = PvzBlueDarkTheme,
            onPrimary = Color.Black,
            primaryContainer = DarkBlueBg,
            onPrimaryContainer = Color.White
        ) else colorScheme.copy(
            primary = PvzBluePrimary,
            onPrimary = Color.White,
            primaryContainer = LightBlueBg,
            onPrimaryContainer = PvzBlueLight
        )
    }

    // 查看态才做语法高亮；编辑态用纯文本（BasicTextField 不支持 AnnotatedString）
    val baseHighlighted = remember(isEditing, editingValue.text, palette) {
        if (isEditing) {
            AnnotatedString(editingValue.text)
        } else {
            JsonSyntaxHighlighter.buildHighlightedJson(editingValue.text, palette)
        }
    }
    val highlighted = remember(baseHighlighted, editMatches, activeMatchIndex) {
        if (isEditing) {
            baseHighlighted
        } else {
            JsonSyntaxHighlighter.overlayMatchBackgrounds(baseHighlighted, editMatches, activeMatchIndex, palette)
        }
    }

    // === 业务操作 ===
    // density 提出来供 changeFontSize 复用（普通函数内不能调 LocalDensity.current）；密度对设备恒定，跨重组安全。
    val density = LocalDensity.current
    val lineHeightPx = with(density) { (fontSize * 1.3f).sp.toPx() }

    /**
     * 统一字号修改入口：−/+ 按钮与双指捏合都走这里。
     * 修改前用当前行高把滚动位置换算成「视口顶部可视行」（小数），字号变化后由
     * LaunchedEffect(fontSize) 据此恢复滚动位置 —— 缩放时文本相对位置不变、行号不跳动。
     * 注意用局部 density 而非 lineHeightPx：本函数被捏合手势的 pointerInput(Unit)
     * 闭包以首次组合的实例捕获，若引用重组的 lineHeightPx 会拿到过期的旧字号行高。
     */
    fun changeFontSize(newSize: Float) {
        if (newSize == fontSize) return
        // 只有没有待应用锚点时（上一次缩放已应用完/首帧）才捕获：
        // 一批连续变化（捏合事件或连点快于重组，effect 被不断取消）共用一个锚点 = 最初的 scroll/行高。
        if (zoomAnchorVisualLine == null) {
            val lh = with(density) { (fontSize * 1.3f).sp.toPx() }
            zoomAnchorVisualLine = if (lh > 0f) commonVerticalScrollState.value / lh else null
        }
        fontSize = newSize
    }

    // 字号变化后：等一帧让新字号完成布局（maxValue 更新），再把滚动位置恢复到 锚点可视行×新行高。
    // 键变化会取消上一个 effect；被取消时锚点保留（不清空），后续 effect 用同一锚点 + 更新的行高重算，
    // 恰好按「初始 scroll → 最终字号」的总缩放比例换算，不受中间帧取消影响。
    LaunchedEffect(fontSize) {
        val anchor = zoomAnchorVisualLine
        if (anchor != null) {
            withFrameNanos { }
            commonVerticalScrollState.scrollTo((anchor * lineHeightPx).roundToInt())
            // 成功应用后才清空；若在此之前的字号变化已让 effect 被取消，这里不会执行，锚点保留给下一次
            zoomAnchorVisualLine = null
        }
    }

    fun onEditTextChange(newValue: TextFieldValue) {
        val prev = editingValue
        editingValue = newValue
        if (newValue.text != prev.text) {
            undoStack.addLast(prev)
            if (undoStack.size > MAX_UNDO_DEPTH) undoStack.removeFirst()
            redoStack.clear()
        }
    }

    fun undoEdit() {
        if (undoStack.isNotEmpty()) {
            redoStack.addLast(editingValue)
            editingValue = undoStack.removeLast()
        }
    }

    fun redoEdit() {
        if (redoStack.isNotEmpty()) {
            undoStack.addLast(editingValue)
            editingValue = redoStack.removeLast()
        }
    }

    /** 查看态点击进入编辑：把光标放到点击处，避免聚焦后 auto-scroll 跳回顶部。 */
    fun enterEditAt(offset: Int) {
        editingValue = editingValue.copy(selection = TextRange(offset))
        isEditing = true
    }

    /** 顶栏"编辑"按钮进入：把光标放到当前可见区顶部（可视行）处，避免跳回顶部。 */
    fun enterEditAtTopLine() {
        val layout = layoutResult.value
        val offset = if (layout != null) {
            val visualLine = (commonVerticalScrollState.value / lineHeightPx).toInt()
                .coerceIn(0, (layout.lineCount - 1).coerceAtLeast(0))
            layout.getLineStart(visualLine)
        } else 0
        editingValue = editingValue.copy(selection = TextRange(offset))
        isEditing = true
    }

    /**
     * 校验当前编辑文本是否为合法 JSON；返回错误描述，合法则 null。
     * 错误描述优先用可读的出错位置（"第 N 行 第 M 列"），提取不到才退回完整消息。
     */
    fun validateJson(): String? = try {
        gson.fromJson(editingValue.text, PvzLevelFile::class.java)
        null
    } catch (e: com.google.gson.JsonSyntaxException) {
        discardErrorPosition = extractErrorPosition(e.localizedMessage)
        "JSON 格式错误：${discardErrorPosition ?: e.localizedMessage?.substringAfterLast("Caused by: ")}"
    } catch (e: Exception) {
        discardErrorPosition = null
        "JSON 解析失败: ${e.message}"
    }

    fun handleSave() {
        val error = validateJson()
        if (error != null) {
            syntaxError = error
            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
            return
        }
        val newLevelData = gson.fromJson(editingValue.text, PvzLevelFile::class.java)
        levelFile.objects.clear()
        levelFile.objects.addAll(newLevelData.objects)

        onPersistLevel()
        refreshTrigger++
        syntaxError = null
        // 不在此重复弹 toast：onPersistLevel（编辑器 performSave）已弹"保存成功"，再弹就是双 toast。
    }

    /** 退出编辑态：若当前文本有语法错误，弹窗提示修改会丢失，不直接退回预览；否则直接退出。 */
    fun exitEditMode() {
        if (validateJson() != null) {
            showDiscardWarning = true
        } else {
            isEditing = false
        }
    }

    fun currentMatchCount(): Int = editMatches.size

    fun currentMatchLabel(): String {
        val total = currentMatchCount()
        if (total == 0) return if (searchActive) "0" else ""
        return "${if (activeMatchIndex < 0) 0 else activeMatchIndex + 1}/$total"
    }

    fun stepMatch(delta: Int) {
        val total = currentMatchCount()
        if (total == 0 || !searchActive) return
        activeMatchIndex = if (activeMatchIndex < 0) {
            if (delta > 0) 0 else total - 1
        } else {
            ((activeMatchIndex + delta) % total + total) % total
        }
    }

    /** 替换当前命中。仅编辑态可用；替换后 activeMatchIndex 复位（matches 随文本变化自动重算）。 */
    fun replaceCurrentMatch() {
        if (!isEditing || activeMatchIndex < 0 || activeMatchIndex >= editMatches.size) return
        val match = editMatches[activeMatchIndex]
        val newText = JsonSearchLogic.replaceSingle(
            editingValue.text, match, replaceQuery, debouncedSearchQuery, searchOptions
        )
        if (newText == editingValue.text) return
        // 复用 onEditTextChange 的 undo/redo 记录，光标停在替换内容处
        onEditTextChange(TextFieldValue(newText, TextRange(match.start)))
        activeMatchIndex = -1
    }

    /** 全部替换。仅编辑态可用；替换后 activeMatchIndex 复位。 */
    fun replaceAllMatches() {
        if (!isEditing) return
        val newText = JsonSearchLogic.replaceAll(
            editingValue.text, debouncedSearchQuery, replaceQuery, searchOptions
        )
        if (newText == editingValue.text) return
        onEditTextChange(TextFieldValue(newText, TextRange(0)))
        activeMatchIndex = -1
    }

    /** 「替换」按钮：替换栏隐藏时展开，否则替换当前命中。 */
    fun onToggleReplace() {
        if (!showReplaceBar) showReplaceBar = true
        else replaceCurrentMatch()
    }

    // 搜索导航：编辑态定位光标，查看态只滚动到命中行
    LaunchedEffect(isEditing, activeMatchIndex, editingValue.text) {
        if (activeMatchIndex < 0) return@LaunchedEffect
        if (activeMatchIndex < editMatches.size) {
            val match = editMatches[activeMatchIndex]
            if (isEditing) {
                editingValue = editingValue.copy(selection = TextRange(match.start, match.end))
            }
            // 换行后 match.lineIndex 是逻辑行，滚动位置要用命中起点所在的可视行
            val visualLine = layoutResult.value?.getLineForOffset(match.start) ?: match.lineIndex
            commonVerticalScrollState.scrollTo(max(0, (visualLine * lineHeightPx - lineHeightPx * 2).toInt()))
        }
    }

    BackHandler {
        when {
            showDiscardWarning -> showDiscardWarning = false
            searchVisible -> searchVisible = false
            isEditing -> exitEditMode()
            else -> onBack()
        }
    }

    MaterialTheme(colorScheme = if (isEditing) editScheme else colorScheme) {
        Scaffold(
            // 底部内边距统一走 contentWindowInsets：系统栏与键盘取并集（大者优先）。
            // 之前用 Modifier.imePadding() 会与 Scaffold 自带的 contentWindowInsets 双重加底部内边距，
            // 键盘弹出时在搜索栏（Column 底部）下方留下一条空白遮罩。
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            when {
                                searchVisible -> searchVisible = false
                                isEditing -> exitEditMode()
                                else -> onBack()
                            }
                        }) {
                            Icon(
                                if (searchVisible || isEditing) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                null,
                                tint = MaterialTheme.colorScheme.surface
                            )
                        }
                    },
                    actions = {
                        // 搜索入口（所有状态可用）
                        IconButton(onClick = { searchVisible = !searchVisible; searchQuery = "" }) {
                            Icon(
                                Icons.Default.Search,
                                "搜索",
                                tint = MaterialTheme.colorScheme.surface
                            )
                        }
                        if (isEditing) {
                            IconButton(onClick = { undoEdit() }, enabled = undoStack.isNotEmpty()) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Undo,
                                    "撤销",
                                    tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.38f)
                                )
                            }
                            IconButton(onClick = { redoEdit() }, enabled = redoStack.isNotEmpty()) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Redo,
                                    "重做",
                                    tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.38f)
                                )
                            }
                        } else {
                            IconButton(onClick = { enterEditAtTopLine() }) {
                                Icon(Icons.Default.Edit, "编辑", tint = MaterialTheme.colorScheme.surface)
                            }
                        }
                        IconButton(onClick = { handleSave() }) {
                            Icon(Icons.Default.Save, "保存", tint = MaterialTheme.colorScheme.surface)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.surface,
                        actionIconContentColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(0.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    // 仍占满全宽；上下做薄：Row 无垂直 padding，−/+ 用紧凑可点图标（去掉 IconButton 的 48dp 最小高度）
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FormatSize, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${fontSize}sp", fontSize = 14.sp)
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(enabled = fontSize > MIN_FONT_SIZE) {
                                    changeFontSize((fontSize - FONT_SIZE_STEP).coerceAtLeast(MIN_FONT_SIZE))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                "减小字号",
                                modifier = Modifier.size(20.dp),
                                tint = if (fontSize > MIN_FONT_SIZE) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(enabled = fontSize < MAX_FONT_SIZE) {
                                    changeFontSize((fontSize + FONT_SIZE_STEP).coerceAtMost(MAX_FONT_SIZE))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                "增大字号",
                                modifier = Modifier.size(20.dp),
                                tint = if (fontSize < MAX_FONT_SIZE) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        // 双指捏合缩放字号（结果对齐到 0.5 步长）。≥2 指时消费事件，单指滚动/点击/选择完全放行。
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                var prevSpan = 0f
                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val pressed = event.changes.filter { it.pressed }
                                    if (pressed.size >= 2) {
                                        // 双指间距的变化即缩放因子；首帧 prevSpan=0 只建立基准，不缩放
                                        val span = (pressed[0].position - pressed[1].position).getDistance()
                                        if (prevSpan > 0f && span > 0f) {
                                            val zoom = span / prevSpan
                                            if (zoom.isFinite() && zoom != 1f) {
                                                val scaled = (fontSize * zoom).coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
                                                changeFontSize((scaled * 2f).roundToInt() / 2f)
                                            }
                                        }
                                        prevSpan = span
                                        event.changes.forEach { it.consume() }
                                    } else {
                                        prevSpan = 0f
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                ) {
                    EditZone(
                        value = editingValue,
                        onValueChange = { onEditTextChange(it) },
                        fontSize = fontSize,
                        syntaxError = syntaxError,
                        onDismissSyntaxError = { syntaxError = null },
                        isEditing = isEditing,
                        highlighted = highlighted,
                        layoutResultState = layoutResult,
                        onTextDoubleTap = { offset -> enterEditAt(offset) },
                        onGutterDoubleTap = {
                            if (isEditing) exitEditMode()
                            else enterEditAtTopLine()
                        },
                        verticalScrollState = commonVerticalScrollState,
                        focusRequester = focusRequester
                    )
                }

                // 搜索/替换条（页面底部、键盘上方）
                if (searchVisible) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it; activeMatchIndex = -1 },
                        replaceQuery = replaceQuery,
                        onReplaceQueryChange = { replaceQuery = it },
                        showReplaceBar = showReplaceBar,
                        onToggleReplace = { onToggleReplace() },
                        onReplaceAll = {
                            // 与「替换」一致：替换栏隐藏时点击先展开，展开后再执行全部替换
                            if (!showReplaceBar) showReplaceBar = true
                            else replaceAllMatches()
                        },
                        caseSensitive = searchCaseSensitive,
                        onCaseSensitiveChange = { searchCaseSensitive = it; activeMatchIndex = -1 },
                        regex = searchRegex,
                        onRegexChange = { searchRegex = it; activeMatchIndex = -1 },
                        matchLabel = currentMatchLabel(),
                        onStep = { stepMatch(it) },
                        canReplace = isEditing,
                        onClose = {
                            searchVisible = false
                            searchQuery = ""
                            replaceQuery = ""
                            showReplaceBar = false
                            activeMatchIndex = -1
                        }
                    )
                }
            }
        }

        // 编辑态含语法错误时尝试退出：提示修改会丢失，确认后才放弃并退出预览
        if (showDiscardWarning) {
            AlertDialog(
                onDismissRequest = { showDiscardWarning = false },
                title = { Text("无法保存修改") },
                text = {
                    Text(
                        if (discardErrorPosition != null) {
                            "JSON 格式错误：${discardErrorPosition}，无法保存。\n退出编辑将丢失这些修改。"
                        } else {
                            "当前 JSON 有语法错误，无法保存。\n退出编辑将丢失这些修改。"
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        // 放弃修改：还原到最近一次有效内容，不保存带语法错误的修改
                        editingValue = TextFieldValue(serializedJson, TextRange(0))
                        undoStack.clear()
                        redoStack.clear()
                        syntaxError = null
                        discardErrorPosition = null
                        showDiscardWarning = false
                        isEditing = false
                    }) {
                        Text("放弃并退出")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardWarning = false }) {
                        Text("继续编辑")
                    }
                }
            )
        }
    }
}

/**
 * 搜索/替换条：页面底部、键盘上方。
 * 三栏紧凑布局：第一栏搜索输入 + 匹配计数；第二栏替换输入（初始隐藏）；第三栏功能按钮。
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    replaceQuery: String,
    onReplaceQueryChange: (String) -> Unit,
    showReplaceBar: Boolean,
    onToggleReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    caseSensitive: Boolean,
    onCaseSensitiveChange: (Boolean) -> Unit,
    regex: Boolean,
    onRegexChange: (Boolean) -> Unit,
    matchLabel: String,
    onStep: (Int) -> Unit,
    // 仅编辑态可替换；查看态（只读）替换/全部替换禁用
    canReplace: Boolean,
    onClose: () -> Unit
) {
    val hasMatch = matchLabel.isNotEmpty() && matchLabel != "0"
    // 查看态完全禁用；编辑态下，栏隐藏时点击=展开（不要求有匹配），栏显示时按「有匹配」执行
    val replaceEnabled = canReplace && (!showReplaceBar || hasMatch)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // 第一栏：搜索输入 + 匹配计数
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("搜索 JSON…") },
                // 多行搜索：最多 2 行，内容超出时框内上下拖动滚动；回车插入换行（不再直接收起键盘）
                maxLines = 2,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                } else null,
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            Text(matchLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // 第二栏：替换输入（初始隐藏，点「替换」后出现）
        if (showReplaceBar) {
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = replaceQuery,
                    onValueChange = onReplaceQueryChange,
                    placeholder = { Text("替换为…") },
                    // 与搜索框一致：最多 2 行，超出时框内上下拖动滚动；回车插入换行
                    maxLines = 2,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 第三栏：功能按钮（紧凑排列）
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onStep(-1) }, enabled = hasMatch) {
                Icon(
                    Icons.Default.ArrowUpward,
                    "上一个",
                    tint = if (hasMatch) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            IconButton(onClick = { onStep(1) }, enabled = hasMatch) {
                Icon(
                    Icons.Default.ArrowDownward,
                    "下一个",
                    tint = if (hasMatch) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            TextButton(onClick = { onCaseSensitiveChange(!caseSensitive) }) {
                Text(
                    if (caseSensitive) "Aa" else "aa",
                    fontWeight = FontWeight.Bold,
                    color = if (caseSensitive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { onRegexChange(!regex) }) {
                Text(
                    ".*",
                    fontWeight = FontWeight.Bold,
                    color = if (regex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onToggleReplace,
                enabled = replaceEnabled
            ) {
                // 不显式指定颜色：跟随 TextButton 内容色，enabled=false（只读/无匹配）时自动置灰
                Text("替换")
            }
            TextButton(
                onClick = onReplaceAll,
                enabled = replaceEnabled
            ) {
                Text("全部替换")
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "关闭", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * 编辑区：行号栏 + 内容。
 * 查看态 = 高亮只读 Text（长按可选词，双击进入编辑，单击无反应）；
 * 编辑态 = 纯文本 BasicTextField（文本内双击=原生选词，双击行号栏退出）。
 * 两态共用同一文本、字号与滚动状态。
 */
@Composable
fun EditZone(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fontSize: Float,
    syntaxError: String?,
    onDismissSyntaxError: () -> Unit,
    isEditing: Boolean,
    highlighted: AnnotatedString,
    layoutResultState: MutableState<TextLayoutResult?>,
    onTextDoubleTap: (Int) -> Unit,
    onGutterDoubleTap: () -> Unit,
    verticalScrollState: ScrollState,
    focusRequester: FocusRequester
) {
    val lineCount = value.text.count { it == '\n' } + 1
    val lineNumbersWidth = remember(lineCount, fontSize) {
        val digits = minOf(lineCount.toString().length, 5)
        (digits * fontSize * 0.7f).dp + 12.dp
    }

    // 查看/编辑两态共用的代码文字样式：显式 letterSpacing=0，
    // 避免查看态 Text 继承 LocalTextStyle（M3 bodyLarge letterSpacing=0.5sp）导致两态字距不一致
    val codeTextStyle = remember(fontSize) {
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.3).sp,
            letterSpacing = 0.sp
        )
    }

    // 进入编辑态时聚焦（聚焦 auto-scroll 到 selection 位置，selection 已在 enterEdit* 中放到合理位置）
    LaunchedEffect(isEditing) {
        if (isEditing) focusRequester.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (syntaxError != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error)
                        .padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = syntaxError,
                        color = MaterialTheme.colorScheme.onError,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissSyntaxError) {
                        Icon(
                            Icons.Default.Close,
                            "关闭",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .width(lineNumbersWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .verticalScroll(verticalScrollState)
                        // 双击行号栏 = 切换查看/编辑态（编辑态文本内双击是原生选词，故退出放这里）
                        .pointerInput(Unit) {
                            awaitDoubleTapGesture { onGutterDoubleTap() }
                        }
                        .padding(top = 0.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    val lineNumbersText = remember(layoutResultState.value, value.text) {
                        buildLineNumberGutter(layoutResultState.value, value.text)
                    }
                    Text(
                        text = lineNumbersText,
                        style = codeTextStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // 无水平滚动：文本按 Box 宽度原地换行，行号栏按可视行对齐
                        .verticalScroll(verticalScrollState)
                        .padding(start = 8.dp, top = 0.dp, end = 8.dp, bottom = 8.dp)
                ) {
                    if (isEditing) {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = codeTextStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            onTextLayout = { layoutResultState.value = it }
                        )
                    } else {
                        // 查看态：高亮只读文本；双击进入编辑（光标放到双击处），长按可选中复制，单击无反应
                        SelectionContainer {
                            Text(
                                text = highlighted,
                                onTextLayout = { layoutResultState.value = it },
                                style = codeTextStyle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        awaitDoubleTapGesture { pos ->
                                            layoutResultState.value?.let { onTextDoubleTap(it.getOffsetForPosition(pos)) }
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
        // 右侧可拖动滚动条，快速翻页
        ScrollStateDragScrubber(
            state = verticalScrollState,
            scope = rememberCoroutineScope(),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }
}

/**
 * 双击手势检测：不消费任何事件，长按选择/滚动等手势完全放行给内部处理。
 * 命中时用第二次点击抬起的位置（即完成双击时的落点）回调；单击、长按或超时不回调。
 */
private suspend fun PointerInputScope.awaitDoubleTapGesture(onDoubleTap: (Offset) -> Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        val firstUp = waitForUpOrCancellation() ?: return@awaitEachGesture
        // 第二击检测，与 Compose 内部 awaitSecondDown 同构：
        // awaitFirstDown 是非受限顶层扩展，可在外层 AwaitPointerEventScope 隐式 receiver 上解析
        val secondDown = withTimeoutOrNull(DOUBLE_TAP_TIMEOUT_MS) {
            awaitFirstDown(requireUnconsumed = false)
        } ?: return@awaitEachGesture
        val secondUp = waitForUpOrCancellation() ?: return@awaitEachGesture
        onDoubleTap(secondUp.position)
    }
}

/**
 * 右侧可拖动快速翻页条：点按/拖动条上任意位置，按比例跳转滚动位置。
 * 拇指为固定统一长度（不再随内容比例压缩）；位移只在可见轨道内，两端不越界。
 * 修正点：trackHeight 含上下 padding，若直接按 (trackHeight - thumbPx) 算位移，
 * 接近底部时拇指会滑出可见轨道底边（progress=1 时拇指底到 trackHeight+8dp），看起来被"压缩"。
 */
@Composable
private fun DragScrubber(
    progress: Float,
    onScrub: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val thumbColor = MaterialTheme.colorScheme.primary
    val trackVPad = 8.dp
    val thumbHeight = 40.dp
    val padPx = with(LocalDensity.current) { trackVPad.toPx() }
    val thumbPx = with(LocalDensity.current) { thumbHeight.toPx() }
    var trackHeight by remember { mutableIntStateOf(0) } // 含上下 padding 的整体高度
    // 可见轨道高 = 整体高 - 上下 padding；拇指行程 = 可见轨道高 - 拇指高（在可见轨道内移动，两端贴边）
    val travel = (trackHeight - padPx * 2 - thumbPx).coerceAtLeast(0f)
    val thumbOffsetY = if (trackHeight > 0) (travel * progress).toInt() else 0

    Box(
        modifier = modifier
            .width(10.dp)
            .padding(vertical = trackVPad)
            .clip(RoundedCornerShape(5.dp))
            .background(trackColor)
            .onSizeChanged { trackHeight = it.height }
            .pointerInput(trackHeight) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val y = event.changes.first().position.y
                        // 触摸只映射到可见轨道内（去掉上下 padding），与拇指行程一致
                        val content = trackHeight - padPx * 2
                        if (content > 0) onScrub(((y - padPx) / content).coerceIn(0f, 1f))
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, thumbOffsetY) }
                .height(thumbHeight)
                .background(thumbColor, RoundedCornerShape(4.dp))
        )
    }
}

/** ScrollState（编辑区）的翻页条。 */
@Composable
private fun ScrollStateDragScrubber(
    state: ScrollState,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    val maxScroll = state.maxValue
    DragScrubber(
        progress = if (maxScroll > 0) state.value.toFloat() / maxScroll else 0f,
        onScrub = { f -> scope.launch { state.scrollTo((f * maxScroll).toInt()) } },
        modifier = modifier
    )
}

/**
 * 生成行号栏文本：每个可视行一行。逻辑行首行显示逻辑行号，换行的续行留空。
 * 换行后一个逻辑行占多个可视行，行号栏与内容区共享 verticalScrollState，
 * 必须逐可视行输出，否则行数与内容行高不匹配导致滚动错位。
 * layout 为空（首帧/无布局）时降级为逻辑行编号。
 */
private fun buildLineNumberGutter(layout: TextLayoutResult?, text: String): String {
    if (layout == null) {
        val count = text.count { it == '\n' } + 1
        return (1..count).joinToString("\n")
    }
    val sb = StringBuilder()
    var logicalNo = 0 // 已扫过的逻辑行数（每跨过一个 '\n' +1）
    var prevStart = 0
    for (i in 0 until layout.lineCount) {
        if (i > 0) sb.append('\n')
        val start = layout.getLineStart(i)
        // 累计上一可视行起点到本行起点之间的换行数，推进逻辑行号（O(n) 总量）
        for (k in prevStart until start) if (text[k] == '\n') logicalNo++
        prevStart = start
        if (start == 0 || text[start - 1] == '\n') {
            sb.append(logicalNo + 1)
        }
        // else：续行留空
    }
    return sb.toString()
}

/**
 * 从 Gson 错误消息中提取出错位置（"line N column M" → "第 N 行 第 M 列"），
 * 提取不到返回 null。用于报错提示只显示位置，不暴露冗长的完整异常消息。
 */
private fun extractErrorPosition(message: String?): String? {
    if (message.isNullOrBlank()) return null
    val match = Regex("""line\s+(\d+)\s+column\s+(\d+)""").find(message) ?: return null
    val line = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
    val col = match.groupValues.getOrNull(2)?.toIntOrNull() ?: return null
    return "第 $line 行 第 $col 列"
}
