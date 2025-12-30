package com.example.pvz2leveleditor.views.editor.pages.module

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
import com.example.pvz2leveleditor.data.LevelDefinitionData
import com.example.pvz2leveleditor.data.PvzLevelFile
import com.example.pvz2leveleditor.data.PvzObject
import com.example.pvz2leveleditor.data.Repository.ReferenceRepository
import com.example.pvz2leveleditor.data.RtidParser
import com.example.pvz2leveleditor.data.SunDropperPropertiesData
import com.example.pvz2leveleditor.views.editor.EditorHelpDialog
import com.example.pvz2leveleditor.views.editor.HelpSection
import com.google.gson.Gson

private val gson = Gson()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SunDropperPropertiesEP(
    rootLevelFile: PvzLevelFile,
    levelDef: LevelDefinitionData,
    onBack: () -> Unit,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    val sunModuleIndex = remember(levelDef.modules) {
        levelDef.modules.indexOfFirst { rtid ->
            val alias = RtidParser.parse(rtid)?.alias ?: ""
            ReferenceRepository.getObjClass(alias) == "SunDropperProperties" ||
                    rootLevelFile.objects.find { it.aliases?.contains(alias) == true }?.objClass == "SunDropperProperties"
        }
    }
    var isCustomMode by remember {
        val currentRtid = if (sunModuleIndex != -1) levelDef.modules[sunModuleIndex] else ""
        mutableStateOf(RtidParser.parse(currentRtid)?.source == "CurrentLevel")
    }
    val currentAlias =
        if (sunModuleIndex != -1) RtidParser.parse(levelDef.modules[sunModuleIndex])?.alias
            ?: "DefaultSunDropper" else "DefaultSunDropper"
    val sunDataState = remember {
        val localObj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        val data = if (localObj != null) {
            try {
                gson.fromJson(localObj.objData, SunDropperPropertiesData::class.java)
            } catch (e: Exception) {
                SunDropperPropertiesData()
            }
        } else {
            SunDropperPropertiesData()
        }
        mutableStateOf(data)
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
            })
        },
        topBar = {
            TopAppBar(
                title = { Text("阳光掉落配置") },
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
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "阳光掉落模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = Color(0xFF1976D2)
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "本模块用于控制天降阳光模块，黑夜地图或传送带等不需要掉落阳光的地图可用直接删除本模块。"
                )
                HelpSection(
                    title = "本地参数",
                    body = "常规关卡中掉落的参数写在LevelModules文件内，这里通过自定义注入的方式将属性参数改为CurrentLevel，即关卡内部。"
                )
                HelpSection(
                    title = "参数调节",
                    body = "两次阳光的掉落间隔会在当前间隔加上浮动范围内随机选择。常规关卡阳光掉落的间隔会随着游戏的推移越来越慢，是由单次增加间隔决定的。"
                )
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
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WbSunny, null, tint = Color(0xFF1976D2))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("自定义本地参数", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isCustomMode) "使用 @CurrentLevel (本地编辑)" else "使用 @LevelModules (系统默认)",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isCustomMode,
                        onCheckedChange = { checked ->
                            isCustomMode = checked
                            handleSunModeToggle(
                                checked, sunModuleIndex, levelDef, rootLevelFile, sunDataState.value
                            )
                        }
                    )
                }
            }

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
                    Text("参数调节", color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)

                    SunParamInput(
                        "首次掉落延迟 (InitialSunDropDelay)",
                        sunDataState.value.initialSunDropDelay
                    ) {
                        sunDataState.value = sunDataState.value.copy(initialSunDropDelay = it)
                        syncDataToRoot(rootLevelFile, currentAlias, sunDataState.value)
                    }
                    SunParamInput(
                        "初始掉落间隔 (SunCountDownBase)",
                        sunDataState.value.sunCountDownBase
                    ) {
                        sunDataState.value = sunDataState.value.copy(sunCountDownBase = it)
                        syncDataToRoot(rootLevelFile, currentAlias, sunDataState.value)
                    }
                    SunParamInput(
                        "最大掉落间隔 (SunCountDownMax)",
                        sunDataState.value.sunCountDownMax
                    ) {
                        sunDataState.value = sunDataState.value.copy(sunCountDownMax = it)
                        syncDataToRoot(rootLevelFile, currentAlias, sunDataState.value)
                    }
                    SunParamInput(
                        "间隔浮动范围 (SunCountDownRange)",
                        sunDataState.value.sunCountDownRange
                    ) {
                        sunDataState.value = sunDataState.value.copy(sunCountDownRange = it)
                        syncDataToRoot(rootLevelFile, currentAlias, sunDataState.value)
                    }
                    SunParamInput(
                        "单次增加间隔 (sunCountDownIncreasePerSun)",
                        sunDataState.value.sunCountDownIncreasePerSun
                    ) {
                        sunDataState.value =
                            sunDataState.value.copy(sunCountDownIncreasePerSun = it)
                        syncDataToRoot(rootLevelFile, currentAlias, sunDataState.value)
                    }
                }
            }

        }
    }
}

/**
 * 核心逻辑：处理开关切换
 */
private fun handleSunModeToggle(
    enableCustom: Boolean,
    moduleIndex: Int,
    levelDef: LevelDefinitionData,
    rootLevelFile: PvzLevelFile,
    data: SunDropperPropertiesData
) {
    if (enableCustom) {
        val alias = if (moduleIndex != -1) RtidParser.parse(levelDef.modules[moduleIndex])?.alias
            ?: "DefaultSunDropper" else "DefaultSunDropper"
        val newRtid = RtidParser.build(alias, "CurrentLevel")

        if (moduleIndex != -1) levelDef.modules[moduleIndex] = newRtid
        else levelDef.modules.add(newRtid)

        val existing = rootLevelFile.objects.find { it.aliases?.contains(alias) == true }
        if (existing == null) {
            val newObj = PvzObject(
                aliases = listOf(alias),
                objClass = "SunDropperProperties",
                objData = gson.toJsonTree(data)
            )
            rootLevelFile.objects.add(newObj)
        }
    } else {
        if (moduleIndex != -1) {
            val oldAlias = RtidParser.parse(levelDef.modules[moduleIndex])?.alias

            levelDef.modules[moduleIndex] = RtidParser.build("DefaultSunDropper", "LevelModules")

            if (oldAlias != null) {
                rootLevelFile.objects.removeAll { it.aliases?.contains(oldAlias) == true }
            }
        }
    }
}

/**
 * 将修改后的业务数据实时写回 PvzObject 的 JsonTree
 */
private fun syncDataToRoot(root: PvzLevelFile, alias: String, data: SunDropperPropertiesData) {
    root.objects.find { it.aliases?.contains(alias) == true }?.let {
        it.objData = gson.toJsonTree(data)
    }
}

/**
 * 封装的数字输入组件
 */
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
            focusedBorderColor = Color(0xFF388E3C),
            focusedLabelColor = Color(0xFF388E3C)
        )
    )
}