package com.example.z_editor.views.editor.pages.module

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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.LevelDefinitionData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RoofPropertiesData
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.repository.ReferenceRepository
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzBrownDark
import com.example.z_editor.ui.theme.PvzBrownLight
import com.example.z_editor.ui.theme.PvzGridBgDark
import com.example.z_editor.ui.theme.PvzGridBorder
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.StepperControl
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoofPropertiesEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    levelDef: LevelDefinitionData,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    val currentAlias = RtidParser.parse(rtid)?.alias ?: "RoofProps"

    val stageModuleInfo = remember(levelDef.stageModule) {
        RtidParser.parse(levelDef.stageModule)
    }

    val stageObjClass = remember(stageModuleInfo) {
        stageModuleInfo?.alias?.let { alias ->
            ReferenceRepository.getObjClass(alias)
        }
    }

    val isRoofStage = stageObjClass == "RoofStageProperties"

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzBrownDark else PvzBrownLight

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, RoofPropertiesData::class.java)
    val moduleDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    val data = moduleDataState.value

    val minCol = data.flowerPotStartColumn
    val maxCol = data.flowerPotEndColumn


    val startCol = moduleDataState.value.flowerPotStartColumn
    val endCol = moduleDataState.value.flowerPotEndColumn

    val hasFlowerPot: (Int) -> Boolean = remember(startCol, endCol) {
        { col: Int ->
            col in startCol..endCol
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            CommonEditorTopAppBar(
                title = "屋顶花盆设置",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "屋顶花盆模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "用于在一代复刻屋顶地图中预置花盆。只有在地图为屋顶地图时才应使用此模块。"
                )
                HelpSection(
                    title = "列数范围",
                    body = "花盆将填满从起始列到终止列之间的所有格子。0代表最左侧第一列，8代表最右侧第九列。"
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
            if (!isRoofStage) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onError, RoundedCornerShape(8.dp)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.width(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "地图类型不匹配",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onError,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "当前地图类型并非屋顶地图，此模块在游戏中可能无法生效，甚至导致闪退",
                                color = MaterialTheme.colorScheme.onError,
                                fontSize = 14.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "花盆范围设置",
                        style = MaterialTheme.typography.titleMedium,
                        color = themeColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    StepperControl(
                        label = "起始列 (StartColumn)",
                        valueText = "${moduleDataState.value.flowerPotStartColumn}",
                        onMinus = {
                            val current = moduleDataState.value.flowerPotStartColumn
                            if (current > 0) {
                                moduleDataState.value =
                                    moduleDataState.value.copy(flowerPotStartColumn = current - 1)
                                sync()
                            }
                        },
                        onPlus = {
                            val current = moduleDataState.value.flowerPotStartColumn
                            if (minCol < maxCol) {
                                moduleDataState.value =
                                    moduleDataState.value.copy(flowerPotStartColumn = current + 1)
                                sync()
                            }
                        }
                    )

                    StepperControl(
                        label = "终止列 (EndColumn)",
                        valueText = "${moduleDataState.value.flowerPotEndColumn}",
                        onMinus = {
                            val current = moduleDataState.value.flowerPotEndColumn
                            if (minCol < maxCol) {
                                moduleDataState.value =
                                    moduleDataState.value.copy(flowerPotEndColumn = current - 1)
                                sync()
                            }
                        },
                        onPlus = {
                            val current = moduleDataState.value.flowerPotEndColumn
                            if (current < 8) {
                                moduleDataState.value =
                                    moduleDataState.value.copy(flowerPotEndColumn = current + 1)
                                sync()
                            }
                        }
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.widthIn(max = 480.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "花盆分布预览",
                            style = MaterialTheme.typography.titleMedium,
                            color = themeColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.8f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) PvzGridBgDark else Color.White)
                                .border(1.dp, PvzGridBorder, RoundedCornerShape(6.dp))
                        ) {
                            Column(Modifier.fillMaxSize()) {
                                for (row in 0..4) {
                                    Row(Modifier.weight(1f)) {
                                        for (col in 0..8) {
                                            val hasPot = hasFlowerPot(col)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .border(0.5.dp, PvzGridBorder)
                                                    .background(
                                                        if (hasPot) if (isDark) Color(0xFF6B4233) else Color(
                                                            0xFFC99380
                                                        )
                                                        else Color.Transparent
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
                                    .background(if (isDark) Color(0xFF6B4233) else Color(0xFFC99380))
                                    .border(0.5.dp, PvzGridBorder)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "有花盆",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.width(24.dp))

                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(if (isDark) PvzGridBgDark else Color.White)
                                    .border(0.5.dp, PvzGridBorder)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "无花盆",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}