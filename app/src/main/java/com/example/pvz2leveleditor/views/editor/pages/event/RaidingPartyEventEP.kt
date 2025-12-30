package com.example.pvz2leveleditor.views.editor.pages.event

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pvz2leveleditor.data.LevelParser
import com.example.pvz2leveleditor.data.PvzLevelFile
import com.example.pvz2leveleditor.data.RaidingPartyEventData
import com.example.pvz2leveleditor.views.editor.EditorHelpDialog
import com.example.pvz2leveleditor.views.editor.HelpSection
import com.example.pvz2leveleditor.views.editor.NumberInputInt
import com.google.gson.Gson

private val gson = Gson()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaidingPartyEventEP(
    rtid: String,
    rootLevelFile: PvzLevelFile,
    onBack: () -> Unit,
    scrollState: ScrollState
) {
    val currentAlias = LevelParser.extractAlias(rtid)
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val eventDataState = remember {
        val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        val initialData = try {
            gson.fromJson(obj?.objData, RaidingPartyEventData::class.java)
        } catch (e: Exception) {
            RaidingPartyEventData()
        }
        mutableStateOf(initialData)
    }

    fun sync() {
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }?.let {
            it.objData = gson.toJsonTree(eventDataState.value)
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus() // 点击空白处清除焦点
            })
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("编辑 $currentAlias", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("事件类型：海盗登船", fontSize = 15.sp, fontWeight = FontWeight.Normal)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
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
            EditorHelpDialog(
                title = "海盗登船事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = Color(0xFFFF9800)
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "常见于海盗港湾的事件，能分批依次生成若干只飞索僵尸进攻。"
                )
                HelpSection(
                    title = "僵尸设置",
                    body = "本事件修改自由度较低，僵尸强制写死为海盗飞索，阶级也只能默认随地图阶级序列。"
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF0E1)),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()) {
                    Text("事件说明", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "此事件控制海盗港湾的突袭，生成飞索僵尸。",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("生成参数配置", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    NumberInputInt(
                        value = eventDataState.value.groupSize,
                        onValueChange = {
                            eventDataState.value = eventDataState.value.copy(groupSize = it)
                            sync()
                        },
                        label = "每组的僵尸数 (GroupSize)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "每一组所包含的僵尸数量",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    NumberInputInt(
                        value = eventDataState.value.swashbucklerCount,
                        onValueChange = {
                            eventDataState.value = eventDataState.value.copy(swashbucklerCount = it)
                            sync()
                        },
                        label = "总僵尸数 (SwashbucklerCount)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "该事件总共生成的僵尸数量",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    NumberInputInt(
                        value = eventDataState.value.timeBetweenGroups,
                        onValueChange = {
                            eventDataState.value = eventDataState.value.copy(timeBetweenGroups = it)
                            sync()
                        },
                        label = "组间间隔 (TimeBetweenGroups)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "两批突袭之间的时间间隔",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}