package com.example.z_editor.views.editor.pages.others

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.PvzObject
import com.google.gson.GsonBuilder

private enum class JsonViewMode {
    Structured,
    RawText
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonCodeViewerScreen(
    fileName: String,
    levelFile: PvzLevelFile?,
    onBack: () -> Unit
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
    var viewMode by remember { mutableStateOf(JsonViewMode.RawText) }
    var fontSize by remember { mutableFloatStateOf(12f) }

    var isEditing by remember { mutableStateOf(false) }
    var editingValue by remember { mutableStateOf(TextFieldValue("")) }
    var syntaxError by remember { mutableStateOf<String?>(null) }

    val focusRequester = remember { FocusRequester() }
    val expandedStates = remember { mutableStateMapOf<Int, Boolean>() }

    var refreshTrigger by remember { mutableIntStateOf(0) }
    var itemToDeleteIndex by remember { mutableStateOf<Int?>(null) }

    val fullJsonContent by remember(viewMode, refreshTrigger) {
        derivedStateOf {
            if (viewMode == JsonViewMode.RawText || isEditing) {
                gson.toJson(levelFile)
            } else ""
        }
    }

    val lazyListState = rememberLazyListState()
    val commonVerticalScrollState = rememberScrollState()
    val commonHorizontalScrollState = rememberScrollState()

    fun persistToFile() {
        try {
            val json = gson.toJson(levelFile)
            context.openFileOutput(fileName, android.content.Context.MODE_PRIVATE).use {
                it.write(json.toByteArray())
            }
            refreshTrigger++
        } catch (e: Exception) {
            Toast.makeText(context, "物理写入失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleSave() {
        try {
            val newLevelData = gson.fromJson(editingValue.text, PvzLevelFile::class.java)
            levelFile.objects.clear()
            levelFile.objects.addAll(newLevelData.objects)

            persistToFile()

            Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
            syntaxError = null
        } catch (e: com.google.gson.JsonSyntaxException) {
            syntaxError = "JSON格式错误: " + (e.localizedMessage?.substringAfterLast("Caused by: "))
            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            syntaxError = "保存失败: ${e.message}"
        }
    }

    BackHandler {
        if (isEditing) isEditing = false else onBack()
    }

    // 删除确认弹窗
    if (itemToDeleteIndex != null) {
        AlertDialog(
            onDismissRequest = { itemToDeleteIndex = null },
            title = { Text("确认删除") },
            text = { Text("确定要完全移除第 ${itemToDeleteIndex!! + 1} 个对象吗？此操作将立即同步到 JSON 文件。") },
            confirmButton = {
                TextButton(onClick = {
                    val index = itemToDeleteIndex!!
                    levelFile.objects.removeAt(index)
                    persistToFile()
                    Toast.makeText(context, "已删除并同步", Toast.LENGTH_SHORT).show()
                    itemToDeleteIndex = null
                }) {
                    Text("确认删除", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeleteIndex = null }) { Text("取消") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    val modeTitle = when {
                        isEditing -> "编辑模式"
                        viewMode == JsonViewMode.Structured -> "结构化视图"
                        else -> "纯文本视图"
                    }
                    Text(modeTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                },
                navigationIcon = {
                    IconButton(onClick = { if (isEditing) isEditing = false else onBack() }) {
                        Icon(
                            if (isEditing) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            null,
                            tint = MaterialTheme.colorScheme.surface
                        )
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { handleSave() }) {
                            Icon(
                                Icons.Default.Save,
                                "保存",
                                tint = MaterialTheme.colorScheme.surface
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            viewMode = if (viewMode == JsonViewMode.Structured)
                                JsonViewMode.RawText else JsonViewMode.Structured
                        }) {
                            Icon(
                                imageVector = if (viewMode == JsonViewMode.Structured)
                                    Icons.Default.DataObject else Icons.AutoMirrored.Filled.List,
                                contentDescription = "切换视图",
                                tint = MaterialTheme.colorScheme.surface
                            )
                        }
                        IconButton(onClick = {
                            editingValue = TextFieldValue(
                                text = gson.toJson(levelFile),
                                selection = TextRange(0)
                            )
                            isEditing = true
                        }) {
                            Icon(
                                Icons.Default.Edit,
                                "编辑",
                                tint = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isEditing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FormatSize, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("${fontSize.toInt()}", fontSize = 14.sp)
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 6f..18f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = if (isEditing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            activeTrackColor = if (isEditing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        )
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (isEditing) {
                    EditZone(
                        value = editingValue,
                        onValueChange = { editingValue = it },
                        fontSize = fontSize,
                        syntaxError = syntaxError,
                        verticalScrollState = commonVerticalScrollState,
                        horizontalScrollState = commonHorizontalScrollState,
                        focusRequester = focusRequester
                    )
                } else {
                    when (viewMode) {
                        JsonViewMode.Structured -> {
                            val objectsSnapshot =
                                remember(refreshTrigger) { levelFile.objects.toList() }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = lazyListState
                            ) {
                                itemsIndexed(
                                    items = objectsSnapshot,
                                    key = { _, obj -> obj.hashCode() }
                                ) { index, obj ->
                                    val isExpanded = expandedStates[index] != false
                                    ObjectCodeCard(
                                        index = index,
                                        obj = obj,
                                        fontSize = fontSize,
                                        expanded = isExpanded,
                                        onToggle = { expandedStates[index] = !isExpanded },
                                        onDelete = { itemToDeleteIndex = index },
                                        jsonFormatter = { element -> gson.toJson(element) }
                                    )
                                }
                            }
                        }

                        JsonViewMode.RawText -> {
                            SelectionContainer(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background)
                                        .verticalScroll(commonVerticalScrollState)
                                        .horizontalScroll(commonHorizontalScrollState)
                                ) {
                                    Text(
                                        text = fullJsonContent,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = fontSize.sp,
                                        lineHeight = (fontSize * 1.3).sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditZone(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fontSize: Float,
    syntaxError: String?,
    verticalScrollState: ScrollState,
    horizontalScrollState: ScrollState,
    focusRequester: FocusRequester
) {
    val lineCount = value.text.count { it == '\n' } + 1
    val lineNumbersWidth = remember(lineCount, fontSize) {
        val digits = lineCount.toString().length
        (digits * fontSize * 0.7f).dp + 20.dp
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (syntaxError != null) {
            Text(
                text = syntaxError,
                color = MaterialTheme.colorScheme.onError,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.error)
                    .padding(8.dp)
            )
        }
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(lineNumbersWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .verticalScroll(verticalScrollState)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.End
            ) {
                val lineNumbersText = remember(lineCount) { (1..lineCount).joinToString("\n") }
                Text(
                    text = lineNumbersText,
                    fontSize = fontSize.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    lineHeight = (fontSize * 1.3).sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState)
                    .padding(horizontal = 8.dp, vertical = 16.dp)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.3).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
fun ObjectCodeCard(
    index: Int,
    obj: PvzObject,
    fontSize: Float,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    jsonFormatter: (com.google.gson.JsonElement) -> String
) {
    val isLevelDef = obj.objClass == "LevelDefinition"
    val jsonContent by remember(obj.objData) { derivedStateOf { jsonFormatter(obj.objData) } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                .clickable { onToggle() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${index + 1}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (!isLevelDef && !obj.aliases.isNullOrEmpty()) {
                    Text(
                        "Aliases: ${(obj.aliases as Iterable<Any?>).joinToString(", ")}",
                        fontSize = 12.sp
                    )
                }
                Text(
                    "ObjClass: ${obj.objClass}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.DeleteForever,
                    null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(20.dp)
                )
            }

            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
        }

        if (expanded) {
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            SelectionContainer {
                Text(
                    text = jsonContent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.3).sp
                )
            }
        }
    }
}