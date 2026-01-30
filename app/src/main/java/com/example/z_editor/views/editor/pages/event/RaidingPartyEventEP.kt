package com.example.z_editor.views.editor.pages.event

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.z_editor.data.LevelParser
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RaidingPartyEventData
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzLightOrangeDark
import com.example.z_editor.ui.theme.PvzLightOrangeLight
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

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

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, RaidingPartyEventData::class.java)
    val eventDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzLightOrangeDark else PvzLightOrangeLight

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
            })
        },
        topBar = {
            CommonEditorTopAppBar(
                title = "编辑 $currentAlias",
                subtitle = "事件类型：海盗登船",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "海盗登船事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
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
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "生成参数配置",
                        color = themeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    NumberInputInt(
                        value = eventDataState.value.groupSize,
                        onValueChange = {
                            eventDataState.value = eventDataState.value.copy(groupSize = it)
                            sync()
                        },
                        label = "每组的僵尸数 (GroupSize)",
                        modifier = Modifier.fillMaxWidth(),
                        color = themeColor
                    )
                    Text(
                        "每一组所包含的僵尸数量",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    NumberInputInt(
                        value = eventDataState.value.swashbucklerCount,
                        onValueChange = {
                            eventDataState.value = eventDataState.value.copy(swashbucklerCount = it)
                            sync()
                        },
                        label = "总僵尸数 (SwashbucklerCount)",
                        modifier = Modifier.fillMaxWidth(),
                        color = themeColor
                    )
                    Text(
                        "该事件总共生成的僵尸数量",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    NumberInputInt(
                        value = eventDataState.value.timeBetweenGroups,
                        onValueChange = {
                            eventDataState.value = eventDataState.value.copy(timeBetweenGroups = it)
                            sync()
                        },
                        label = "组间间隔 (TimeBetweenGroups)",
                        modifier = Modifier.fillMaxWidth(),
                        color = themeColor
                    )
                    Text(
                        "两批突袭之间的时间间隔",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}