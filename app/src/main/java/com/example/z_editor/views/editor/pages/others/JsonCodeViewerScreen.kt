package com.example.z_editor.views.editor.pages.others

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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

    val expandedStates = remember { mutableStateMapOf<Int, Boolean>() }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val fullJsonContent by remember(viewMode, refreshTrigger) {
        derivedStateOf {
            if (viewMode == JsonViewMode.RawText || isEditing) {
                gson.toJson(levelFile)
            } else ""
        }
    }

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

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "保存失败: JSON 格式错误\n${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
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
                            Icon(Icons.Default.Close, "取消", tint = Color.White)
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                        }
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { handleSave() }) {
                            Icon(Icons.Default.Save, "保存", tint = Color.White)
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
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            isEditing = true
                        }) {
                            Icon(Icons.Default.Edit, "编辑", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isEditing) Color(0xFF1472E8) else Color(0xFF455A64),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFECEFF1))
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(0.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FormatSize, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("${fontSize.toInt()}", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 6f..24f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        BasicTextField(
                            value = editingText,
                            onValueChange = { editingText = it },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * 1.3).sp,
                                color = Color(0xFF263238)
                            ),
                            cursorBrush = SolidColor(Color(0xFF1472E8)),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    }
                } else {
                    when (viewMode) {
                        JsonViewMode.Structured -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                            ) {
                                itemsIndexed(levelFile.objects) { index, obj ->
                                    val isExpanded = expandedStates[index] ?: true
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
                                        .background(Color.White)
                                        .verticalScroll(rememberScrollState())
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = fullJsonContent,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = fontSize.sp,
                                        lineHeight = (fontSize * 1.3).sp,
                                        color = Color(0xFF263238),
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFCFD8DC).copy(alpha = 0.3f))
                .clickable { onToggle() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFF455A64), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    color = Color.White,
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
                        color = Color(0xFF546E7A)
                    )
                }

                Text(
                    text = "ObjClass: $objClass",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )
            }

            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                tint = Color.Gray
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
                    color = Color(0xFF37474F)
                )
            }
        }
    }
}