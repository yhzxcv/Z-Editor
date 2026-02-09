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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.google.gson.Gson

private val gson = Gson()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunnelDefendModuleEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: ScrollState
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    var showHelpDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val moduleDataState = remember {
        val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        val data = try {
            gson.fromJson(obj?.objData, TunnelDefendModuleData::class.java)
        } catch (_: Exception) {
            TunnelDefendModuleData()
        }
        mutableStateOf(data)
    }

    val gridState = remember {
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
        "IMAGE_UI_MAUSOLEUM_TUNNEL_DOWN", "IMAGE_UI_MAUSOLEUM_TUNNEL_DOWN_2", "IMAGE_UI_MAUSOLEUM_TUNNEL_DOWN_3",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_DOWN_LEFT", "IMAGE_UI_MAUSOLEUM_TUNNEL_DOWN_LEFT_2", "IMAGE_UI_MAUSOLEUM_TUNNEL_DOWN_LEFT_3",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_LEFT", "IMAGE_UI_MAUSOLEUM_TUNNEL_LEFT_2", "IMAGE_UI_MAUSOLEUM_TUNNEL_LEFT_3",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_LEFT_4", "IMAGE_UI_MAUSOLEUM_TUNNEL_LEFT_5", "IMAGE_UI_MAUSOLEUM_TUNNEL_LEFT_6",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_LEFT_7",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_UP", "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_2", "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_3",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_LEFT", "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_LEFT_2", "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_LEFT_3",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_DOWN", "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_DOWN_2",
        "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_DOWN_LEFT", "IMAGE_UI_MAUSOLEUM_TUNNEL_UP_DOWN_LEFT_2"
    )
    var selectedImg by remember { mutableStateOf(availableAssets[0]) }

    fun sync() {
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
            it.objData = gson.toJsonTree(newData)
        }
    }

    fun handleGridClick(x: Int, y: Int) {
        val currentInCell = gridState[x][y]
        val newColumn = gridState[x].clone()

        if (currentInCell == selectedImg) {
            newColumn[y] = null
        } else {
            newColumn[y] = selectedImg
        }

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
                                    if (imgName != null) {
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

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("选择组件", fontWeight = FontWeight.Bold, color = themeColor, fontSize = 16.sp)
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
                                    AssetImage(
                                        path = "images/tunnels/$asset.webp",
                                        contentDescription = asset,
                                        modifier = Modifier.size(48.dp)
                                            .border(
                                            0.5.dp, MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Text(
                                        text = asset.replace("IMAGE_UI_MAUSOLEUM_TUNNEL_", ""),
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