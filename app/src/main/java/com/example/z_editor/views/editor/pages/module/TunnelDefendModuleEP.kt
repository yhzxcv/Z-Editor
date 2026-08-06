package com.example.z_editor.views.editor.pages.module

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Foundation
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.TunnelDefendModuleData
import com.example.z_editor.data.TunnelRoadData
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzBrownDark
import com.example.z_editor.ui.theme.PvzBrownLight
import com.example.z_editor.ui.theme.PvzGridBorder
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputDouble
import com.google.gson.Gson

private val gson = Gson()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunnelDefendModuleEP(
    rtid: String,
    onBack: () -> Unit,
    onToggleMode: (Boolean, TunnelDefendModuleData) -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val rtidInfo = remember(rtid) { RtidParser.parse(rtid) }
    val currentAlias = rtidInfo?.alias ?: "DefaultTunnel"
    val isCustomMode = rtidInfo?.source == "CurrentLevel"

    val moduleDataState = remember(rtid, rootLevelFile) {
        val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        val data = try {
            if (obj != null) {
                gson.fromJson(obj.objData, TunnelDefendModuleData::class.java)
            } else {
                TunnelDefendModuleData()
            }
        } catch (_: Exception) {
            TunnelDefendModuleData()
        }
        mutableStateOf(data)
    }

    val gridState = remember(moduleDataState.value) {
        val matrix = Array(9) { arrayOfNulls<String>(5) }
        moduleDataState.value.roads.forEach { road ->
            if (road.gridX in 0..8 && road.gridY in 0..4) {
                matrix[road.gridX][road.gridY] = road.img
            }
        }
        mutableStateListOf<Array<String?>>().apply {
            matrix.forEach { add(it.clone()) }
        }
    }

    val availableAssets = listOf(
        "IMAGE_UI_MAUSOLEUM_TUNNEL_DOWN",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_DOWN_2",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_DOWN_3",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_DOWN_LEFT",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_DOWN_LEFT_2",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_DOWN_LEFT_3",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_LEFT",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_LEFT_2",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_LEFT_3",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_LEFT_4",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_LEFT_5",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_LEFT_6",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_LEFT_7",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_UP",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_2",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_3",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_LEFT",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_LEFT_2",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_LEFT_3",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_DOWN",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_DOWN_2",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_DOWN_LEFT",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_DOWN_LEFT_2",
        ""  // 无贴图坑道
    )
    var selectedImg by remember { mutableStateOf(availableAssets[0]) }

    fun sync() {
        if (!isCustomMode) return

        val newRoads = mutableListOf<TunnelRoadData>()
        for (x in 0..8) {
            for (y in 0..4) {
                gridState[x][y]?.let { imgName ->
                    newRoads.add(TunnelRoadData(gridX = x, gridY = y, img = imgName))
                }
            }
        }
        val newData = moduleDataState.value.copy(roads = newRoads)
        moduleDataState.value = newData
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }?.let {
            val jsonTree = gson.toJsonTree(newData)
            // reportError: 默认true，仅为false时写入
            if (newData.reportError != false) {
                jsonTree.asJsonObject.remove("reportError")
            }
            // BrickMapIndex: 默认null，仅在2或3时写入
            if (newData.BrickMapIndex == null) {
                jsonTree.asJsonObject.remove("BrickMapIndex")
            }
            // TunnelSequenceInterval: 仅在BrickMapIndex为2时写入
            if (newData.BrickMapIndex != 2 || newData.TunnelSequenceInterval == null) {
                jsonTree.asJsonObject.remove("TunnelSequenceInterval")
            }
            it.objData = jsonTree
        }
    }

    fun handleGridClick(x: Int, y: Int) {
        if (!isCustomMode) return
        val newColumn = gridState[x].clone()
        newColumn[y] = if (gridState[x][y] == selectedImg) null else selectedImg
        gridState[x] = newColumn
        sync()
    }

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzBrownDark else PvzBrownLight

    Scaffold(
        modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
        topBar = {
            CommonEditorTopAppBar(
                title = "地宫坑道设置",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "地宫坑道模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "使用本模块在关卡里添加地宫秘境的地道，部分僵尸和植物的交互会被地道影响。"
                )
                HelpSection(
                    title = "使用说明",
                    body = "先在下方列表中选择一个地道组件，在上方网格中点击即可放置。 点击已有的相同组件可将其移除，点击不同的组件可直接替换。"
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.8f)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, PvzGridBorder, RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0xFF3E2723) else Color(0xFFEFEBE9))
            ) {
                Column(Modifier.fillMaxSize()) {
                    for (y in 0..4) {
                        Row(Modifier.weight(1f)) {
                            for (x in 0..8) {
                                val imgName = gridState[x][y]
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(0.5.dp, PvzGridBorder.copy(alpha = 0.5f))
                                        .clickable { handleGridClick(x, y) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        imgName == "" -> {
                                            // 无贴图坑道：显示空心标识
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize(0.4f)
                                                    .border(
                                                        2.dp,
                                                        themeColor.copy(alpha = 0.6f),
                                                        RoundedCornerShape(2.dp)
                                                    )
                                            )
                                        }
                                        imgName != null -> {
                                            AssetImage(
                                                path = "images/tunnels/$imgName.webp",
                                                contentDescription = imgName,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "选择组件",
                        fontWeight = FontWeight.Bold,
                        color = themeColor,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(10.dp))

                    Box(modifier = Modifier.height(320.dp)) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 56.dp),
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(availableAssets) { asset ->
                                val isSelected = selectedImg == asset
                                val isEmptyTunnel = asset == ""
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) themeColor.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(
                                            2.dp,
                                            if (isSelected) themeColor else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedImg = asset }
                                        .padding(vertical = 12.dp, horizontal = 4.dp)
                                ) {
                                    if (isEmptyTunnel) {
                                        // 无贴图坑道选项
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .border(
                                                    2.dp,
                                                    themeColor.copy(alpha = 0.5f),
                                                    RoundedCornerShape(4.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "无",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = themeColor.copy(alpha = 0.6f)
                                            )
                                        }
                                    } else {
                                        AssetImage(
                                            path = "images/tunnels/$asset.webp",
                                            contentDescription = asset,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .border(
                                                    0.5.dp, MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                        )
                                    }
                                    Text(
                                        text = if (isEmptyTunnel) "无贴图" else asset.replace("IMAGE_UI_MAUSOLEUM_TUNNEL_", ""),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // === reportError 开关 ===
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "种植草垛提示",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "种植植物是否提示需要草垛，默认开启",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = moduleDataState.value.reportError != false,
                        onCheckedChange = { checked ->
                            val newReportError = if (checked) null else false
                            moduleDataState.value = moduleDataState.value.copy(reportError = newReportError)
                            sync()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = themeColor,
                            checkedBorderColor = Color.Transparent,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    )
                }
            }

            // === BrickMapIndex 选择器 ===
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        "地图索引",
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "选择坑道对应的游戏模式",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val options = listOf(
                            null to "默认",
                            2 to "僵王战",
                            3 to "搜打撤"
                        )
                        options.forEach { (value, label) ->
                            val isSelected = moduleDataState.value.BrickMapIndex == value
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) themeColor.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        1.5.dp,
                                        if (isSelected) themeColor else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        val newIndex = if (moduleDataState.value.BrickMapIndex == value) null else value
                                        val newInterval = if (newIndex == 2) (moduleDataState.value.TunnelSequenceInterval ?: 0.4) else null
                                        moduleDataState.value = moduleDataState.value.copy(
                                            BrickMapIndex = newIndex,
                                            TunnelSequenceInterval = newInterval
                                        )
                                        sync()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // 仅在僵王战(index=2)时显示间隔输入
                    if (moduleDataState.value.BrickMapIndex == 2) {
                        Spacer(Modifier.height(12.dp))
                        NumberInputDouble(
                            value = moduleDataState.value.TunnelSequenceInterval ?: 0.4,
                            onValueChange = { newVal ->
                                moduleDataState.value = moduleDataState.value.copy(TunnelSequenceInterval = newVal)
                                sync()
                            },
                            label = "隧道序列间隔",
                            color = themeColor
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "已放置组件: ${moduleDataState.value.roads.size}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(
                        onClick = {
                            for (i in 0..8) gridState[i] = arrayOfNulls(5)
                            sync()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("清空全部", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}