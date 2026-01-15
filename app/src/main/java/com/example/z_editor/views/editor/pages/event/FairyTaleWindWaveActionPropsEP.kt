package com.example.z_editor.views.editor.pages.event

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Air
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.FairyTaleWindWaveActionData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputDouble
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FairyTaleWindWaveActionPropsEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    val currentAlias = RtidParser.parse(rtid)?.alias ?: "WindEvent"
    val themeColor = Color(0xFFBE5DBA)

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, FairyTaleWindWaveActionData::class.java)
    val actionDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "编辑 $currentAlias",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "事件类型：童话微风",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
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
                    containerColor = themeColor,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "童话微风事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "本事件会产生一阵持续的微风，用于将童话迷雾吹散，常见于童话森林。"
                )
                HelpSection(
                    title = "参数说明",
                    body = "该事件作用时可以改变抛射物的速度，1.0表示原速，数值越大子弹运动越快。"
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
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "微风参数配置",
                        style = MaterialTheme.typography.titleMedium,
                        color = themeColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(16.dp))

                    NumberInputDouble(
                        value = actionDataState.value.duration,
                        onValueChange = { newVal ->
                            actionDataState.value = actionDataState.value.copy(duration = newVal)
                            sync()
                        },
                        label = "持续时间 (Duration)",
                        modifier = Modifier.fillMaxWidth(),
                        color = themeColor
                    )

                    Spacer(Modifier.height(12.dp))

                    NumberInputDouble(
                        value = actionDataState.value.velocityScale,
                        onValueChange = { newVal ->
                            actionDataState.value = actionDataState.value.copy(velocityScale = newVal)
                            sync()
                        },
                        label = "速度倍率 (VelocityScale)",
                        modifier = Modifier.fillMaxWidth(),
                        color = themeColor
                    )
                }
            }
        }
    }
}