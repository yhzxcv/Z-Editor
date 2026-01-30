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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.SunDropperPropertiesData
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzLightOrangeDark
import com.example.z_editor.ui.theme.PvzLightOrangeLight
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputDouble
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

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzLightOrangeDark else PvzLightOrangeLight

    fun sync() {
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
                            "帮助说明",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColor,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "阳光掉落模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "本模块用于配置关卡中的天降阳光参数，若是黑夜地图可考虑不添加此模块。"
                )
                HelpSection(
                    title = "参数配置",
                    body = "常规情况下，本模块使用在游戏文件里的定义，也可以选择打开自定义开关对详细参数进行编辑。"
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === 模式切换卡片 ===
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WbSunny, null, tint = themeColor)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("自定义本地参数", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isCustomMode) "当前: 本地编辑 (@CurrentLevel)" else "当前: 系统默认 (@LevelModules)",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isCustomMode,
                        onCheckedChange = { checked ->
                            onToggleMode(checked, sunDataState.value)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = themeColor,
                            checkedBorderColor = Color.Transparent,

                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
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
                        .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("参数调节", color = themeColor, fontWeight = FontWeight.Bold)
                    NumberInputDouble(
                        value = sunDataState.value.initialSunDropDelay,
                        onValueChange = {
                            sunDataState.value = sunDataState.value.copy(initialSunDropDelay = it)
                            sync()
                        },
                        label = "首次掉落延迟",
                        modifier = Modifier.fillMaxWidth(),
                        color = themeColor
                    )
                    NumberInputDouble(
                        value = sunDataState.value.sunCountdownBase,
                        onValueChange = {
                            sunDataState.value = sunDataState.value.copy(sunCountdownBase = it)
                            sync()
                        },
                        label = "初始掉落间隔",
                        modifier = Modifier.fillMaxWidth(),
                        color = themeColor
                    )
                    NumberInputDouble(
                        value = sunDataState.value.sunCountdownMax,
                        onValueChange = {
                            sunDataState.value = sunDataState.value.copy(sunCountdownMax = it)
                            sync()
                        },
                        label = "最大掉落间隔",
                        modifier = Modifier.fillMaxWidth(),
                        color = themeColor
                    )
                    NumberInputDouble(
                        value = sunDataState.value.sunCountdownRange,
                        onValueChange = {
                            sunDataState.value = sunDataState.value.copy(sunCountdownRange = it)
                            sync()
                        },
                        label = "间隔浮动范围",
                        modifier = Modifier.fillMaxWidth(),
                        color = themeColor
                    )
                    NumberInputDouble(
                        value = sunDataState.value.sunCountdownIncreasePerSun,
                        onValueChange = {
                            sunDataState.value =
                                sunDataState.value.copy(sunCountdownIncreasePerSun = it)
                            sync()
                        },
                        label = "单次增加间隔",
                        modifier = Modifier.fillMaxWidth(),
                        color = themeColor
                    )
                }
            }
        }
    }
}