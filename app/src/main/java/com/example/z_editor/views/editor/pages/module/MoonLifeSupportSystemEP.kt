package com.example.z_editor.views.editor.pages.module

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.MoonLifeSupportSystemPropertiesData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.ui.theme.PvzBluePrimary
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorContentWindowInsets
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputDouble
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoonLifeSupportSystemEP(
    rtid: String,
    rootLevelFile: PvzLevelFile,
    onBack: () -> Unit,
    onRequestPlantSelection: ((List<String>) -> Unit) -> Unit,
    scrollState: ScrollState
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, MoonLifeSupportSystemPropertiesData::class.java)
    val moduleDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    val themeColor = PvzBluePrimary

    Scaffold(
        modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
        contentWindowInsets = EditorContentWindowInsets(),
        topBar = {
            CommonEditorTopAppBar(
                title = "月球电力维持系统设置",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "月球电力维持系统模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "月球地图使用电力系统维持植物运作，与阳光收集是两个不同的体系。电力不足时植物会进入超负荷状态，持续超负荷一段时间后植物死机。"
                )
                HelpSection(
                    title = "电力参数",
                    body = "初始电力容量为关卡开始时的可用电力，超负荷比例为过载发生时的比例，用电量超过容量的一定比例后开始计时，计时结束后植物死机。"
                )
                HelpSection(
                    title = "免疫植物",
                    body = "免疫名单内的植物不受电力系统影响，即便电力耗尽也能正常工作。"
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "电力参数",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = themeColor
                    )
                    NumberInputInt(
                        value = moduleDataState.value.initialCapacity,
                        onValueChange = {
                            moduleDataState.value =
                                moduleDataState.value.copy(initialCapacity = it); sync()
                        },
                        label = "初始电力容量",
                        color = themeColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                    NumberInputDouble(
                        value = moduleDataState.value.bufferOverloadRatio,
                        onValueChange = {
                            moduleDataState.value =
                                moduleDataState.value.copy(bufferOverloadRatio = it); sync()
                        },
                        label = "超负荷比例",
                        color = themeColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                    NumberInputDouble(
                        value = moduleDataState.value.penaltyCountdown,
                        onValueChange = {
                            moduleDataState.value =
                                moduleDataState.value.copy(penaltyCountdown = it); sync()
                        },
                        label = "死机倒计时 (秒)",
                        color = themeColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            ResourceListEditor(
                title = "免疫植物名单",
                description = "不受电力系统影响的植物",
                items = moduleDataState.value.plantImmunityList.list,
                accentColor = themeColor,
                isZombie = false,
                onListChanged = { newList ->
                    moduleDataState.value = moduleDataState.value.copy(
                        plantImmunityList = moduleDataState.value.plantImmunityList.copy(list = newList)
                    )
                    sync()
                },
                onAddRequest = onRequestPlantSelection
            )
        }
    }
}
