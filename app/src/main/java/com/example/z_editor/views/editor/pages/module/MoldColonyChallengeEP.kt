package com.example.z_editor.views.editor.pages.module

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.BoardGridMapPropsData
import com.example.z_editor.data.MoldColonyChallengePropsData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.PvzObject
import com.example.z_editor.data.RtidParser
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzBlueDark
import com.example.z_editor.ui.theme.PvzBlueLight
import com.example.z_editor.ui.theme.PvzGridHighLight
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.google.gson.Gson
import rememberJsonSync

private val gson = Gson()
private const val MOLD_ALIAS = "Mold"
private const val MOLD_IMAGE_PATH = "images/griditems/fake_mold.webp"
private const val GRID_ROWS = 5
private const val GRID_COLS = 9

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoldColonyChallengeEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    onUpdate: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val rtidInfo = remember(rtid) { RtidParser.parse(rtid) }
    val moldColonyAlias = rtidInfo?.alias ?: "DoNotPlantBeforeLine"

    val moldColonyObj = rootLevelFile.objects.find { it.aliases?.contains(moldColonyAlias) == true }

    val colonySyncManager =
        rememberJsonSync(moldColonyObj, MoldColonyChallengePropsData::class.java)
    val colonyData = colonySyncManager.dataState.value

    fun colonySync() = colonySyncManager.sync()

    val locationsRtid = colonyData.locations
    val locationsInfo = remember(locationsRtid) { RtidParser.parse(locationsRtid) }

    val needsFix = locationsInfo == null ||
            locationsInfo.source != "CurrentLevel" ||
            rootLevelFile.objects.find { it.aliases?.contains(locationsInfo.alias) == true } == null

    // Find any existing BoardGridMapProps in the level (for auto-fix target)
    val existingMoldObj = rootLevelFile.objects.find { it.objClass == "BoardGridMapProps" }
    val existingMoldAlias = existingMoldObj?.aliases?.firstOrNull()

    val moldObj = if (!needsFix && locationsInfo != null) {
        rootLevelFile.objects.find { it.aliases?.contains(locationsInfo.alias) == true }
    } else null

    val moldSyncManager = rememberJsonSync(moldObj, BoardGridMapPropsData::class.java)
    val moldData = moldSyncManager.dataState.value

    fun moldSync() = moldSyncManager.sync()

    var selectedRow by remember { mutableIntStateOf(0) }
    var selectedCol by remember { mutableIntStateOf(0) }

    fun toggleCell(row: Int, col: Int) {
        if (needsFix) return
        selectedRow = row
        selectedCol = col
        val newValues = moldData.values.map { it.toMutableList() }.toMutableList()
        newValues[row][col] = if (newValues[row][col] == 1) 0 else 1
        moldSyncManager.dataState.value = moldData.copy(values = newValues)
        moldSync()
    }

    fun autoFix() {
        val targetAlias: String
        if (existingMoldObj != null) {
            targetAlias = existingMoldAlias ?: MOLD_ALIAS
        } else {
            targetAlias = MOLD_ALIAS
            val newObj = PvzObject(
                aliases = listOf(targetAlias),
                objClass = "BoardGridMapProps",
                objData = gson.toJsonTree(BoardGridMapPropsData())
            )
            rootLevelFile.objects.add(newObj)
        }

        val moldRtid = RtidParser.build(targetAlias, "CurrentLevel")
        colonySyncManager.dataState.value = colonyData.copy(locations = moldRtid)
        colonySync()
        onUpdate()
    }

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzBlueDark else PvzBlueLight

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            CommonEditorTopAppBar(
                title = "霉菌区域编辑",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "霉菌区域说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "设置关卡中的霉菌覆盖区域。霉菌会阻止玩家在对应格子上种植植物。"
                )
                HelpSection(
                    title = "网格操作",
                    body = "点击格子切换状态：空地（可种植）↔ 霉菌（不可种植）。网格上方会显示当前选中位置。"
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Warning card when RTID is misconfigured (same style as WaveManagerModulePropertiesEP)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (needsFix) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.outlineVariant
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (needsFix) Icons.Default.Warning else Icons.Default.CheckCircle,
                            null,
                            tint = if (needsFix) MaterialTheme.colorScheme.onError
                            else MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("关联霉菌布局 (Locations)", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "当前值: $locationsRtid",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    if (needsFix) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when {
                                locationsInfo == null -> "错误：Locations 字段无法解析"
                                locationsInfo.source != "CurrentLevel" ->
                                    "错误：引用源为 ${locationsInfo.source}，需切换为关卡自定义"
                                else -> "错误：找不到代号为 \"${locationsInfo.alias}\" 的 BoardGridMapProps 对象"
                            },
                            color = MaterialTheme.colorScheme.onError,
                            fontSize = 12.sp
                        )
                        if (existingMoldObj != null && locationsInfo?.alias != existingMoldAlias) {
                            Text(
                                "关卡中已存在 BoardGridMapProps (代号: $existingMoldAlias)，将关联至该对象",
                                color = MaterialTheme.colorScheme.onError,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Button(
                            onClick = { autoFix() },
                            modifier = Modifier.padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(
                                "一键修复关联至: ${existingMoldAlias ?: MOLD_ALIAS}",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Grid card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "选中位置",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "R${selectedRow + 1} : C${selectedCol + 1}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = themeColor
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.8f)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF6B899A), RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF31383B) else Color(0xFFD7ECF1))
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            for (row in 0 until GRID_ROWS) {
                                Row(Modifier.weight(1f)) {
                                    for (col in 0 until GRID_COLS) {
                                        val cellValue =
                                            moldData.values.getOrNull(row)?.getOrNull(col) ?: 0
                                        val isMold = cellValue == 1
                                        val isSelected = row == selectedRow && col == selectedCol

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .border(
                                                    0.5.dp,
                                                    if (isSelected) themeColor else Color(0xFF6B899A)
                                                )
                                                .background(
                                                    if (isSelected) PvzGridHighLight
                                                    else Color.Transparent
                                                )
                                                .then(
                                                    if (!needsFix) Modifier.clickable {
                                                        toggleCell(
                                                            row,
                                                            col
                                                        )
                                                    }
                                                    else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isMold) {
                                                AssetImage(
                                                    path = MOLD_IMAGE_PATH,
                                                    contentDescription = "霉菌",
                                                    modifier = Modifier.fillMaxSize(0.9f),
                                                    contentScale = ContentScale.Fit,
                                                    filterQuality = FilterQuality.Low
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color.Transparent, RoundedCornerShape(2.dp))
                                .border(0.5.dp, Color(0xFF6B899A), RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "空地",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(20.dp))
                        AssetImage(
                            path = MOLD_IMAGE_PATH,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.Low
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "霉菌",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
