package com.example.z_editor.views.editor.pages.event

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.DinoWaveActionPropsData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzLightGreenDark
import com.example.z_editor.ui.theme.PvzLightGreenLight
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import com.example.z_editor.views.editor.pages.others.StepperControl
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DinoEventEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    val currentAlias = RtidParser.parse(rtid)?.alias ?: "DinoWaveEvent"

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, DinoWaveActionPropsData::class.java)
    val eventDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzLightGreenDark else PvzLightGreenLight

    val dinoOptions = listOf(
        "raptor" to "迅猛龙 (raptor)",
        "stego" to "剑龙 (stego)",
        "ptero" to "翼龙 (ptero)",
        "tyranno" to "霸王龙 (tyranno)",
        "ankylo" to "甲龙 (ankylo)"
    )

    var dinoExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            CommonEditorTopAppBar(
                title = "编辑 $currentAlias",
                subtitle = "事件类型：召唤恐龙",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "恐龙召唤事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "恐龙危机专属事件。在指定的行召唤一只指定的恐龙进入场地，恐龙会协助僵尸进攻。"
                )
                HelpSection(
                    title = "恐龙配置",
                    body = "一次事件只能配置一只恐龙，若需要同时出现多只恐龙需要在波次内添加多次恐龙事件。"
                )
                HelpSection(
                    title = "持续时间",
                    body = "恐龙在场上停留的时间，单位为波次。时间结束或与足够量的僵尸互动后恐龙会离开场地。"
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Pets, null, tint = themeColor)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "恐龙种类 (DinoType)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = themeColor
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    val currentDinoType = eventDataState.value.dinoType
                    val imagePath = "images/others/dino_${currentDinoType}.png"

                    // === 下拉选择框 ===
                    ExposedDropdownMenuBox(
                        expanded = dinoExpanded,
                        onExpandedChange = { dinoExpanded = !dinoExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val selectedLabel =
                            dinoOptions.find { it.first == eventDataState.value.dinoType }?.second
                                ?: eventDataState.value.dinoType

                        OutlinedTextField(
                            value = selectedLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dinoExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedBorderColor = themeColor,
                                focusedLabelColor = themeColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = dinoExpanded,
                            onDismissRequest = { dinoExpanded = false }
                        ) {
                            dinoOptions.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        eventDataState.value =
                                            eventDataState.value.copy(dinoType = code)
                                        sync()
                                        dinoExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AssetImage(
                            path = imagePath,
                            contentDescription = currentDinoType,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.Medium,
                            placeholder = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Pets,
                                        null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = currentDinoType.uppercase(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        "无预览图",
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "位置与持续时间",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = themeColor
                    )
                    Spacer(Modifier.height(16.dp))

                    StepperControl(
                        label = "所在行 (DinoRow)",
                        valueText = "${eventDataState.value.dinoRow + 1} 行",
                        onMinus = {
                            val current = eventDataState.value.dinoRow
                            val newRow = (current - 1).coerceAtLeast(0)
                            eventDataState.value = eventDataState.value.copy(dinoRow = newRow)
                            sync()
                        },
                        onPlus = {
                            val current = eventDataState.value.dinoRow
                            val newRow = (current + 1).coerceAtMost(4)
                            eventDataState.value = eventDataState.value.copy(dinoRow = newRow)
                            sync()
                        },
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    NumberInputInt(
                        value = eventDataState.value.dinoWaveDuration,
                        onValueChange = {
                            eventDataState.value = eventDataState.value.copy(dinoWaveDuration = it)
                            sync()
                        },
                        label = "持续波次数 (DinoWaveDuration)",
                        modifier = Modifier.fillMaxWidth(),
                        color = themeColor
                    )
                }
            }
        }
    }
}