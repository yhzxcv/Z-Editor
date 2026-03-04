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
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.BungeeWaveActionData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.repository.ZombieRepository
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzGridHighLight
import com.example.z_editor.ui.theme.PvzLightOrangeDark
import com.example.z_editor.ui.theme.PvzLightOrangeLight
import com.example.z_editor.ui.theme.PvzPurpleDark
import com.example.z_editor.ui.theme.PvzPurpleLight
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BungeeWaveActionEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    onRequestZombieSelection: ((String) -> Unit) -> Unit
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzLightOrangeDark else PvzLightOrangeLight

    var showHelpDialog by remember { mutableStateOf(false) }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, BungeeWaveActionData::class.java)
    val dataState = syncManager.dataState

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            CommonEditorTopAppBar(
                title = "蹦极投放事件",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "蹦极投放事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "在关卡中设置蹦极僵尸投放僵尸的种类与位置，单个事件只能投放一只僵尸。"
                )
                HelpSection(
                    title = "坐标说明",
                    body = "在下方网格中点击，即可设置蹦极僵尸落下的草坪格子位置。"
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "当前目标: Col ${dataState.value.target.mX + 1}, Row ${dataState.value.target.mY + 1}",
                            fontWeight = FontWeight.Bold,
                            color = themeColor,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "(X: ${dataState.value.target.mX}, Y: ${dataState.value.target.mY})",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.8f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF403A33) else Color(0xFFFFF9EF))
                            .border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            for (row in 0..4) {
                                Row(Modifier.weight(1f)) {
                                    for (col in 0..8) {
                                        val isSelected =
                                            (dataState.value.target.mX == col && dataState.value.target.mY == row)

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .border(
                                                    0.5.dp,
                                                    if (isSelected) themeColor else themeColor.copy(alpha = 0.3f)
                                                )
                                                .background(if (isSelected) PvzGridHighLight else Color.Transparent)
                                                .clickable {
                                                    dataState.value = dataState.value.copy(
                                                        target = dataState.value.target.copy(
                                                            mX = col,
                                                            mY = row
                                                        )
                                                    )
                                                    syncManager.sync()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                val info =
                                                    ZombieRepository.getZombieInfoById(dataState.value.zombieName)
                                                AssetImage(
                                                    path = if (info?.icon != null) "images/zombies/${info.icon}" else "images/others/unknown.webp",
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .fillMaxSize(0.9f)
                                                        .clip(RoundedCornerShape(4.dp)),
                                                    contentScale = ContentScale.Crop
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    val zombieInfo = remember(dataState.value.zombieName) {
                        ZombieRepository.getZombieInfoById(dataState.value.zombieName)
                    }

                    Column {
                        Text("属性配置", fontWeight = FontWeight.Bold, color = themeColor, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    onRequestZombieSelection { selectedId ->
                                        val alias = ZombieRepository.buildZombieAliases(selectedId)
                                        dataState.value = dataState.value.copy(zombieName = alias)
                                        syncManager.sync()
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssetImage(
                                path = if (zombieInfo?.icon != null) "images/zombies/${zombieInfo.icon}" else "images/others/unknown.webp",
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    zombieInfo?.name ?: "点击选择僵尸",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    dataState.value.zombieName,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    NumberInputInt(
                        value = dataState.value.level,
                        onValueChange = {
                            dataState.value = dataState.value.copy(level = it)
                            syncManager.sync()
                        },
                        label = "僵尸等级 (Level)",
                        color = themeColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        null,
                        tint = themeColor,
                        modifier = Modifier.width(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "注意在屋顶地图中蹦极投放事件被保护伞拦截后有可能直接触发食脑，请谨慎使用。",
                            color = themeColor,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}