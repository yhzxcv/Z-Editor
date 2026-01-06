package com.example.z_editor.views.editor.pages.module

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.SunDropperPropertiesData
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.google.gson.Gson

private val gson = Gson()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SunDropperPropertiesEP(
    rtid: String,
    rootLevelFile: PvzLevelFile,
    onToggleMode: (Boolean, SunDropperPropertiesData) -> Unit,
    onBack: () -> Unit,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val rtidInfo = remember(rtid) { RtidParser.parse(rtid) }
    val currentAlias = rtidInfo?.alias ?: "DefaultSunDropper"
    val isCustomMode = rtidInfo?.source == "CurrentLevel"

    val sunDataState = remember(rtid) {
        val obj = if (isCustomMode) {
            rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        } else null

        val data = try {
            if (obj != null) gson.fromJson(obj.objData, SunDropperPropertiesData::class.java)
            else SunDropperPropertiesData()
        } catch (_: Exception) {
            SunDropperPropertiesData()
        }
        mutableStateOf(data)
    }

    fun syncData() {
        if (isCustomMode) {
            rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }?.let {
                it.objData = gson.toJsonTree(sunDataState.value)
            }
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text("阳光掉落配置", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "帮助说明", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF9800),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(title = "阳光掉落说明", onDismiss = { showHelpDialog = false }) {
                HelpSection("模式说明", "默认模式引用系统配置；自定义模式会在关卡内生成独立配置对象。")
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === 模式切换卡片 ===
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WbSunny, null, tint = Color(0xFFFF9800))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("自定义本地参数", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isCustomMode) "当前: 本地编辑 (@CurrentLevel)" else "当前: 系统默认 (@LevelModules)",
                            fontSize = 12.sp, color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isCustomMode,
                        onCheckedChange = { checked ->
                            // [关键修复] 直接调用回调，不在此处修改数据结构
                            onToggleMode(checked, sunDataState.value)
                        }
                    )
                }
            }

            // === 参数编辑区域 ===
            AnimatedVisibility(
                visible = isCustomMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.White, shape = MaterialTheme.shapes.medium)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("参数调节", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)

                    SunParamInput("首次掉落延迟", sunDataState.value.initialSunDropDelay) {
                        sunDataState.value = sunDataState.value.copy(initialSunDropDelay = it)
                        syncData()
                    }
                    SunParamInput("初始掉落间隔", sunDataState.value.sunCountdownBase) {
                        sunDataState.value = sunDataState.value.copy(sunCountdownBase = it)
                        syncData()
                    }
                    SunParamInput("最大掉落间隔", sunDataState.value.sunCountdownMax) {
                        sunDataState.value = sunDataState.value.copy(sunCountdownMax = it)
                        syncData()
                    }
                    SunParamInput("间隔浮动范围", sunDataState.value.sunCountdownRange) {
                        sunDataState.value = sunDataState.value.copy(sunCountdownRange = it)
                        syncData()
                    }
                    SunParamInput("单次增加间隔", sunDataState.value.sunCountdownIncreasePerSun) {
                        sunDataState.value = sunDataState.value.copy(sunCountdownIncreasePerSun = it)
                        syncData()
                    }
                }
            }
        }
    }
}

@Composable
fun SunParamInput(label: String, value: Double, onValueChange: (Double) -> Unit) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = textValue,
        onValueChange = {
            textValue = it
            it.toDoubleOrNull()?.let { num -> onValueChange(num) }
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFFF9800),
            focusedLabelColor = Color(0xFFFF9800)
        )
    )
}