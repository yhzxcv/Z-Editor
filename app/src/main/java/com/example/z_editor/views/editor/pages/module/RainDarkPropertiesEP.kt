package com.example.z_editor.views.editor.pages.module

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.LevelDefinitionData
import com.example.z_editor.data.RtidParser
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RainDarkPropertiesEP(
    currentRtid: String,
    levelDef: LevelDefinitionData,
    onBack: () -> Unit,
    onUpdate: () -> Unit
) {
    var showHelpDialog by remember { mutableStateOf(false) }
    var localRefreshTrigger by remember { mutableIntStateOf(0) }

    val themeColor = Color(0xFF607D8B)

    data class WeatherOption(
        val alias: String,
        val label: String,
        val description: String,
        val icon: ImageVector
    )

    val options = listOf(
        WeatherOption(
            "DefaultSnow",
            "冰河飞雪 (DefaultSnow)",
            "冰河再临秘境的下雪效果",
            Icons.Default.AcUnit
        ),
        WeatherOption(
            "LightningRain",
            "雷雨天气 (LightningRain)",
            "带闪电的的下雨效果",
            Icons.Default.Thunderstorm
        ),
        WeatherOption(
            "DefaultRainDark",
            "阴雨天气 (DefaultRainDark)",
            "场上会进入持续较长时间的黑暗状态",
            Icons.Default.DarkMode
        )
    )

    val targetAliases = remember { options.map { it.alias }.toSet() }

    val activeAlias = remember(localRefreshTrigger) {
        val foundRtid = levelDef.modules.find { rtid ->
            targetAliases.contains(RtidParser.parse(rtid)?.alias)
        }
        RtidParser.parse(foundRtid ?: currentRtid)?.alias ?: "DefaultSnow"
    }

    fun selectOption(newAlias: String) {
        if (newAlias == activeAlias) return

        val index = levelDef.modules.indexOfFirst { rtid ->
            targetAliases.contains(RtidParser.parse(rtid)?.alias)
        }

        if (index != -1) {
            val newRtid = RtidParser.build(newAlias, "LevelModules")
            levelDef.modules[index] = newRtid
            onUpdate()
            localRefreshTrigger++
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("环境天气设置", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "帮助说明", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColor,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "环境天气模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "本模块用于控制关卡中的全局环境特效，如雨雪天气。"
                )
                HelpSection(
                    title = "注意事项",
                    body = "这些模块通常直接引用自 LevelModules，无需在关卡内自定义参数。"
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("选择天气类型", fontWeight = FontWeight.Bold, color = themeColor)

            options.forEach { option ->
                val isSelected = option.alias == activeAlias

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFECEFF1) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectOption(option.alias) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectOption(option.alias) },
                            colors = RadioButtonDefaults.colors(selectedColor = themeColor)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(option.icon, null, tint = if (isSelected) themeColor else Color.Gray)
                                Spacer(Modifier.width(8.dp))
                                Text(option.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                            Text(option.description, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}