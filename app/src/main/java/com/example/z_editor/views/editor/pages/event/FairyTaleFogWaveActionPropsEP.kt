package com.example.z_editor.views.editor.pages.event

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.FairyTaleFogWaveActionData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputDouble
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FairyTaleFogWaveActionPropsEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    val currentAlias = RtidParser.parse(rtid)?.alias ?: "FairyFogEvent"
    val themeColor = Color(0xFFBE5DBA)

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, FairyTaleFogWaveActionData::class.java)
    val actionDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    var typeExpanded by remember { mutableStateOf(false) }
    val fogOptions = listOf(
        "fairy_tale_fog_lvl1" to "一级迷雾 (Lvl 1)",
        "fairy_tale_fog_lvl2" to "二级迷雾 (Lvl 2)",
        "fairy_tale_fog_lvl3" to "三级迷雾 (Lvl 3)"
    )
    val currentFogLabel = fogOptions.find { it.first == actionDataState.value.fogType }?.second
        ?: actionDataState.value.fogType

    val mX = actionDataState.value.range.mX
    val mY = actionDataState.value.range.mY
    val mWidth = actionDataState.value.range.mWidth
    val mHeight = actionDataState.value.range.mHeight

    val isCellInFog: (Int, Int) -> Boolean = remember(mX, mY, mWidth, mHeight) {
        { col: Int, row: Int ->
            val inColRange = col >= mX && col < (mX + mWidth)
            val inRowRange = row >= mY && row < (mY + mHeight)
            inColRange && inRowRange
        }
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
                            "事件类型：童话迷雾",
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
                title = "童话迷雾事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "本事件用于生成覆盖场地、给僵尸提供护盾的迷雾，常用于童话森林关卡，只有微风事件才能吹散。"
                )
                HelpSection(
                    title = "范围参数",
                    body = "mX 和 mY 为计算中心点，mWidth 和 mHeight 分别表示含中心点向右和向下延伸的距离。"
                )
                HelpSection(
                    title = "迷雾等级",
                    body = "迷雾等级越高，僵尸获得的护盾及免控效果越强。等级从低到高依次为白色、蓝色、紫色。"
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
                        text = "迷雾参数配置",
                        style = MaterialTheme.typography.titleMedium,
                        color = themeColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = !typeExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentFogLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("迷雾等级 (FogType)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                focusedLabelColor = themeColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            fogOptions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        actionDataState.value =
                                            actionDataState.value.copy(fogType = value)
                                        sync()
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    NumberInputDouble(
                        value = actionDataState.value.movingTime,
                        onValueChange = { newVal ->
                            actionDataState.value = actionDataState.value.copy(movingTime = newVal)
                            sync()
                        },
                        label = "扩散时间 (MovingTime)",
                        modifier = Modifier.fillMaxWidth(),
                        color = themeColor
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "范围参数 (Range)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )

                    Spacer(Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberInputInt(
                                value = actionDataState.value.range.mX,
                                onValueChange = { newVal ->
                                    val currentRange = actionDataState.value.range
                                    actionDataState.value = actionDataState.value.copy(
                                        range = currentRange.copy(mX = newVal)
                                    )
                                    sync()
                                },
                                label = "起始列 (mX)",
                                modifier = Modifier.weight(1f),
                                color = themeColor
                            )

                            NumberInputInt(
                                value = actionDataState.value.range.mY,
                                onValueChange = { newVal ->
                                    val currentRange = actionDataState.value.range
                                    actionDataState.value = actionDataState.value.copy(
                                        range = currentRange.copy(mY = newVal)
                                    )
                                    sync()
                                },
                                label = "起始行 (mY)",
                                modifier = Modifier.weight(1f),
                                color = themeColor
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberInputInt(
                                value = actionDataState.value.range.mWidth,
                                onValueChange = { newVal ->
                                    val currentRange = actionDataState.value.range
                                    actionDataState.value = actionDataState.value.copy(
                                        range = currentRange.copy(mWidth = newVal)
                                    )
                                    sync()
                                },
                                label = "宽度 (mWidth)",
                                modifier = Modifier.weight(1f),
                                color = themeColor
                            )

                            NumberInputInt(
                                value = actionDataState.value.range.mHeight,
                                onValueChange = { newVal ->
                                    val currentRange = actionDataState.value.range
                                    actionDataState.value = actionDataState.value.copy(
                                        range = currentRange.copy(mHeight = newVal)
                                    )
                                    sync()
                                },
                                label = "高度 (mHeight)",
                                modifier = Modifier.weight(1f),
                                color = themeColor
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.widthIn(max = 480.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, null, tint = themeColor)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "迷雾覆盖预览",
                                style = MaterialTheme.typography.titleMedium,
                                color = themeColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }


                        Spacer(Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.8f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF5F5F5))
                                .border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(6.dp))
                        ) {
                            Column(Modifier.fillMaxSize()) {
                                for (row in 0..4) {
                                    Row(Modifier.weight(1f)) {
                                        for (col in 0..8) {
                                            val inFog = isCellInFog(col, row)

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .border(0.5.dp, Color(0xFFE0E0E0))
                                                    .background(
                                                        if (inFog) Color(0xFFE1BEE7).copy(alpha = 0.8f)
                                                        else Color.White
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color(0xFFE1BEE7).copy(alpha = 0.8f))
                                    .border(0.5.dp, Color.Gray)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("迷雾覆盖", fontSize = 12.sp, color = Color.Gray)

                            Spacer(Modifier.width(24.dp))

                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color.White)
                                    .border(0.5.dp, Color.Gray)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("无迷雾", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}