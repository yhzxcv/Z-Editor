package com.example.z_editor.views.editor.pages.module

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.z_editor.data.LevelDefinitionData
import com.example.z_editor.data.RtidParser
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzLightGreenDark
import com.example.z_editor.ui.theme.PvzLightGreenLight
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawnMowerPropertiesEP(
    currentRtid: String,
    levelDef: LevelDefinitionData,
    onBack: () -> Unit,
    onUpdate: () -> Unit
) {
    var showHelpDialog by remember { mutableStateOf(false) }

    var localRefreshTrigger by remember { mutableIntStateOf(0) }

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzLightGreenDark else PvzLightGreenLight

    data class MowerOption(
        val alias: String,
        val label: String
    )

    val options = listOf(
        MowerOption("FrontLawnMowers", "前院小推车 (FrontLawnMowers)"),
        MowerOption("EgyptMowers", "埃及小推车 (EgyptMowers)"),
        MowerOption("PirateMowers", "海盗小推车 (PirateMowers)"),
        MowerOption("WestMowers", "西部小推车 (WestMowers)"),
        MowerOption("KongFuMowers", "功夫小推车 (KongFuMowers)"),
        MowerOption("FutureMowers", "未来小推车 (FutureMowers)"),
        MowerOption("DarkMowers", "黑暗小推车 (DarkMowers)"),
        MowerOption("BeachMowers", "沙滩小推车 (BeachMowers)"),
        MowerOption("IceageMowers", "冰河小推车 (IceageMowers)"),
        MowerOption("IceageZombossMowers", "冰河僵王小推车 (IceageZombossMowers)"),
        MowerOption("LostCityMowers", "失落小推车 (LostCityMowers)"),
        MowerOption("EightiesMowers", "摇滚小推车 (EightiesMowers)"),
        MowerOption("EightiesZombossMowers", "摇滚僵王小推车 (EightiesZombossMowers)"),
        MowerOption("DinoMowers", "恐龙小推车 (DinoMowers)"),
        MowerOption("ModernMowers", "摩登小推车 (ModernMowers)"),
        MowerOption("SteamMowers", "蒸汽小推车 (SteamMowers)"),
        MowerOption("RenaiMowers", "复兴小推车 (RenaiMowers)"),
        MowerOption("HeianMowers", "平安小推车 (HeianMowers)"),
        MowerOption("FairyTaleMowers", "童话小推车 (FairyTaleMowers)"),
        MowerOption("ZCorpMowers", "Z公司小推车 (ZCorpMowers)"),
        MowerOption("RunningSubwayMowers", "跑酷小推车 (RunningSubwayMowers)"),
        MowerOption("MausoleumMowers", "地宫小推车 (MausoleumMowers)"),
    )

    val targetAliases = remember { options.map { it.alias }.toSet() }

    val activeAlias = remember(levelDef.modules, localRefreshTrigger) {
        val foundRtid = levelDef.modules.find { rtid ->
            targetAliases.contains(RtidParser.parse(rtid)?.alias)
        }
        if (foundRtid != null) {
            RtidParser.parse(foundRtid)?.alias ?: "LawnMower"
        } else {
            RtidParser.parse(currentRtid)?.alias ?: "LawnMower"
        }
    }

    fun selectOption(newAlias: String) {
        if (newAlias == activeAlias) return

        var index = levelDef.modules.indexOfFirst { rtid ->
            targetAliases.contains(RtidParser.parse(rtid)?.alias)
        }
        if (index == -1) {
            index = levelDef.modules.indexOf(currentRtid)
        }
        if (index != -1) {
            val newRtid = RtidParser.build(newAlias, "LevelModules")
            levelDef.modules[index] = newRtid

            localRefreshTrigger++

            onUpdate()
        }
    }

    Scaffold(
        topBar = {
            CommonEditorTopAppBar(
                title = "小推车样式设置",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "小推车模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "本模块用于控制关卡中小推车的样式外观，注意在庭院框架下小推车模块无效。"
                )
                HelpSection(
                    title = "注意事项",
                    body = "小推车模块通常直接引用自 LevelModules，无需在关卡内自定义参数。"
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Text(
                text = "选择小推车类型",
                fontWeight = FontWeight.Bold,
                color = themeColor,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(options) { option ->
                    val isSelected = option.alias == activeAlias

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 2.dp),
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
                                    Icon(
                                        Icons.Default.CleaningServices,
                                        null,
                                        tint = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        option.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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