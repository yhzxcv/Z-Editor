package com.example.z_editor.views.editor.pages.module

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableDoubleStateOf
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
import com.example.z_editor.data.LinkedTileData
import com.example.z_editor.data.PowerTilePropertiesData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.TileLocationData
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzCyanDark
import com.example.z_editor.ui.theme.PvzCyanLight
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputDouble
import rememberJsonSync

enum class PowerTileGroup(
    val value: String,
    val label: String,
    val color: Color,
    val imageName: String
) {
    Alpha("alpha", "Alpha (绿)", Color(0xFF41FF4B), "tool_powertile_alpha.webp"),
    Beta("beta", "Beta (红)", Color(0xFFFF493A), "tool_powertile_beta.webp"),
    Gamma("gamma", "Gamma (蓝)", Color(0xFF3CFFFF), "tool_powertile_gamma.webp"),
    Delta("delta", "Delta (黄)", Color(0xFFFFE837), "tool_powertile_delta.webp")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PowerTilePropertiesEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: ScrollState
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    var showHelpDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    var selectedGroup by remember { mutableStateOf(PowerTileGroup.Alpha) }
    var globalDelayInput by remember { mutableDoubleStateOf(1.5) }

    var tileToEdit by remember { mutableStateOf<LinkedTileData?>(null) }
    var tileEditDelay by remember { mutableDoubleStateOf(0.0) }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, PowerTilePropertiesData::class.java)
    val moduleDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    fun getTileAt(mx: Int, my: Int): LinkedTileData? {
        return moduleDataState.value.linkedTiles.find { it.location.mx == mx && it.location.my == my }
    }

    fun handleGridClick(mx: Int, my: Int) {
        val existingTile = getTileAt(mx, my)
        val newList = moduleDataState.value.linkedTiles.toMutableList()

        if (existingTile != null) {
            if (existingTile.group == selectedGroup.value) {
                newList.remove(existingTile)
            } else {
                val index = newList.indexOf(existingTile)
                if (index != -1) {
                    newList[index] = existingTile.copy(
                        group = selectedGroup.value,
                        propagationDelay = globalDelayInput
                    )
                }
            }
        } else {
            newList.add(
                LinkedTileData(
                    group = selectedGroup.value,
                    propagationDelay = globalDelayInput,
                    location = TileLocationData(mx = mx, my = my)
                )
            )
        }

        moduleDataState.value = moduleDataState.value.copy(linkedTiles = newList)
        sync()
    }

    fun handleUpdateSingleTile() {
        if (tileToEdit == null) return
        val newList = moduleDataState.value.linkedTiles.toMutableList()
        val index = newList.indexOf(tileToEdit!!)
        if (index != -1) {
            newList[index] = tileToEdit!!.copy(propagationDelay = tileEditDelay)
            moduleDataState.value = moduleDataState.value.copy(linkedTiles = newList)
            sync()
        }
        tileToEdit = null
    }

    if (tileToEdit != null) {
        AlertDialog(
            onDismissRequest = { tileToEdit = null },
            title = {
                Text("编辑 R${tileToEdit!!.location.my + 1}:C${tileToEdit!!.location.mx + 1} 瓷砖")
            },
            text = {
                Column {
                    Text(
                        "当前组: ${tileToEdit!!.group}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    NumberInputDouble(
                        value = tileEditDelay,
                        onValueChange = { tileEditDelay = it },
                        label = "传导延迟 (秒)"
                    )
                }
            },
            confirmButton = {
                Button(onClick = { handleUpdateSingleTile() }) { Text("保存修改") }
            },
            dismissButton = {
                TextButton(onClick = { tileToEdit = null }) { Text("取消") }
            }
        )
    }

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzCyanDark else PvzCyanLight

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            CommonEditorTopAppBar(
                title = "能量瓷砖设置",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "能量瓷砖模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "瓷砖分为四组。同一组的瓷砖在触发能量豆时会联动。点击一次格点进行放置，再次点击进行删除。"
                )
                HelpSection(
                    title = "延迟设置",
                    body = "可以设置能量开始传导的前摇时间，注意不包含传输时间。这个值默认是1.5秒。放置后长按单个瓷砖可单独修改其延迟。"
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
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PowerTileGroup.entries.forEach { group ->
                            val isSelected = selectedGroup == group
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) group.color.copy(alpha = 0.8f) else group.color.copy(
                                            alpha = 0.2f
                                        )
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedGroup = group },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = group.label.substringBefore(" "),
                                    color = Color.Black.copy(0.6f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NumberInputDouble(
                            color = themeColor,
                            value = globalDelayInput,
                            onValueChange = { globalDelayInput = it },
                            label = "新放置瓷砖的默认延迟 (Delay)",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.8f) // 9:5
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF00796B), RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0xFF3D4947) else Color(0xFFE0F2F1))
            ) {
                Column(Modifier.fillMaxSize()) {
                    for (row in 0..4) {
                        Row(Modifier.weight(1f)) {
                            for (col in 0..8) {
                                val tile = getTileAt(col, row)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(0.5.dp, Color(0xFFB2DFDB))
                                        .combinedClickable(
                                            onClick = { handleGridClick(col, row) },
                                            onLongClick = {
                                                if (tile != null) {
                                                    tileToEdit = tile
                                                    tileEditDelay = tile.propagationDelay
                                                }
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (tile != null) {
                                        val groupConfig =
                                            PowerTileGroup.entries.find { it.value == tile.group }
                                                ?: PowerTileGroup.Alpha

                                        Box(
                                            modifier = Modifier.fillMaxSize(0.95f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AssetImage(
                                                path = "images/tools/${groupConfig.imageName}",
                                                contentDescription = groupConfig.label,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .padding(bottom = 2.dp)
                                                    .background(
                                                        Color.Transparent
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "${tile.propagationDelay}s",
                                                    fontSize = 10.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
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
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    PowerTileGroup.entries.forEach { group ->
                        val count =
                            moduleDataState.value.linkedTiles.count { it.group == group.value }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AssetImage(
                                    path = "images/tools/${group.imageName}",
                                    contentDescription = group.label,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text("$count", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    TextButton(
                        onClick = {
                            moduleDataState.value =
                                moduleDataState.value.copy(linkedTiles = mutableListOf())
                            sync()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Text("清空所有配置", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}