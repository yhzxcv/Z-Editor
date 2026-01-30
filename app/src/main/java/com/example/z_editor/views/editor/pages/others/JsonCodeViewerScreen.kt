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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    var editingText by remember { mutableStateOf("") }
    var syntaxError by remember { mutableStateOf<String?>(null) }

    val expandedStates = remember { mutableStateMapOf<Int, Boolean>() }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val fullJsonContent by remember(viewMode, refreshTrigger) {
        derivedStateOf {
            if (viewMode == JsonViewMode.RawText || isEditing) {
                gson.toJson(levelFile)
            } else ""
        }
    }

    val commonVerticalScrollState = rememberScrollState()
    val commonHorizontalScrollState = rememberScrollState()

    LaunchedEffect(isEditing) {
        if (isEditing) {
            editingText = gson.toJson(levelFile)
        }
    }

    BackHandler {
        if (isEditing) {
            isEditing = false
        } else {
            onBack()
        }
    }

    fun handleSave() {
        try {
            val newLevelData = gson.fromJson(editingText, PvzLevelFile::class.java)

            levelFile.objects.clear()
            levelFile.objects.addAll(newLevelData.objects)

            context.openFileOutput(fileName, android.content.Context.MODE_PRIVATE).use {
                it.write(editingText.toByteArray())
            }
            Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
            refreshTrigger++

            syntaxError = null
        } catch (e: com.google.gson.JsonSyntaxException) {
            syntaxError = "JSON格式错误: " + (e.localizedMessage?.substringAfterLast("Caused by: "))
            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            syntaxError = "保存失败: ${e.message}"
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val modeTitle = when {
                            isEditing -> "编辑模式"
                            viewMode == JsonViewMode.Structured -> "结构化视图"
                            else -> "纯文本视图"
                        }
                        Text(modeTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    if (isEditing) {
                        IconButton(onClick = { isEditing = false }) {
                            Icon(
                                Icons.Default.Close,
                                "取消",
                                tint = MaterialTheme.colorScheme.surface
                            )
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "返回",
                                tint = MaterialTheme.colorScheme.surface
                            )
                        }
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
                    Icon(
                        Icons.Default.FormatSize,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "${fontSize.toInt()}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        colors = SliderDefaults.colors(
                            thumbColor = if (isEditing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            activeTrackColor = if (isEditing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        ),
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 6f..18f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isEditing) {
                    EditZone(
                        text = editingText,
                        onValueChange = { editingText = it },
                        fontSize = fontSize,
                        syntaxError = syntaxError,
                        verticalScrollState = commonVerticalScrollState,
                        horizontalScrollState = commonHorizontalScrollState
                    )
                } else {
                    when (viewMode) {
                        JsonViewMode.Structured -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    vertical = 8.dp
                                )
                            ) {
                                itemsIndexed(levelFile.objects) { index, obj ->
                                    val isExpanded = expandedStates[index] != false
                                    ObjectCodeCard(
                                        index = index,
                                        obj = obj,
                                        fontSize = fontSize,
                                        expanded = isExpanded,
                                        onToggle = { expandedStates[index] = !isExpanded },
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
                                        .background(MaterialTheme.colorScheme.surface)
                                        .verticalScroll(commonVerticalScrollState)
                                        .horizontalScroll(commonHorizontalScrollState)
                                ) {
                                    Text(
                                        text = fullJsonContent,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = fontSize.sp,
                                        lineHeight = (fontSize * 1.3).sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun EditZone(
    text: String, onValueChange: (String) -> Unit, fontSize: Float, syntaxError: String?,
    verticalScrollState: ScrollState, horizontalScrollState: ScrollState
) {
    val lines = text.split("\n")
    val lineCount = lines.size

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
        Row(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .width(45.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .verticalScroll(verticalScrollState)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    Text(
                        text = "$i ",
                        fontSize = fontSize.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = (fontSize * 1.3).sp
                    )
                }
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
                    value = text,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.3).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
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
    jsonFormatter: (com.google.gson.JsonElement) -> String
) {
    val aliases = obj.aliases
    val objClass = obj.objClass
    val isLevelDef = objClass == "LevelDefinition"

    val jsonContent by remember(obj.objData) {
        derivedStateOf { jsonFormatter(obj.objData) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { onToggle() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    color = MaterialTheme.colorScheme.surface,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (!isLevelDef && !aliases.isNullOrEmpty()) {
                    Text(
                        text = "Aliases: ${aliases.joinToString(", ")}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "ObjClass: $objClass",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

        if (expanded) {
            SelectionContainer {
                Text(
                    text = jsonContent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.3).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}