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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.TidePropertiesData
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzGridBgDark
import com.example.z_editor.ui.theme.PvzGridBorder
import com.example.z_editor.ui.theme.PvzLightBlueDark
import com.example.z_editor.ui.theme.PvzLightBlueLight
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TidePropertiesEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    val currentAlias = RtidParser.parse(rtid)?.alias ?: "Tide"

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzLightBlueDark else PvzLightBlueLight

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, TidePropertiesData::class.java)
    val moduleDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    val startingLocation = moduleDataState.value.startingWaveLocation
    val isCellInWater: (Int) -> Boolean = remember(startingLocation) {
        { col: Int ->
            val waterStartCol = 9 - startingLocation
            col >= waterStartCol
        }
    }
    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            CommonEditorTopAppBar(
                title = "初始潮水设置",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "潮水模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "本模块用于开启关卡中的潮水系统，以便后续使用潮水更改事件。"
                )
                HelpSection(
                    title = "初始潮水位置",
                    body = "可以指定潮水的初始位置。场地最右边为0，最左边为9。允许输入负数在内的整数。"
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
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "初始潮水配置",
                        style = MaterialTheme.typography.titleMedium,
                        color = themeColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(16.dp))

                    NumberInputInt(
                        value = moduleDataState.value.startingWaveLocation,
                        onValueChange = {
                            moduleDataState.value =
                                moduleDataState.value.copy(startingWaveLocation = it)
                            sync()
                        },
                        color = themeColor,
                        label = "初始位置 (StartingWaveLocation)",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 使用 Box 包裹并居中，限制最大宽度
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
                            text = "潮水位置预览",
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
                                            val inWater = isCellInWater(col)

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .border(0.5.dp, PvzGridBorder)
                                                    .background(
                                                        if (inWater) themeColor.copy(alpha = 0.6f)
                                                        else if (isDark) PvzGridBgDark else Color.White
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

                        // 图例说明
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(20.dp)
                                    .background(themeColor.copy(alpha = 0.6f))
                                    .border(0.5.dp, PvzGridBorder)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "有潮水",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(24.dp))
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(20.dp)
                                    .background(if (isDark) PvzGridBgDark else Color.White)
                                    .border(0.5.dp, PvzGridBorder)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "无潮水",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            text = "场地最右边坐标为0，最左边为9，注意潮水模块需要最后添加否则可能导致闪退。",
                            color = themeColor,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}