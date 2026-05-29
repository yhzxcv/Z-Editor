package com.example.z_editor.views.editor.pages.module

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RiftThemeDemoModuleData
import com.example.z_editor.data.RtidParser
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzCyanDark
import com.example.z_editor.ui.theme.PvzCyanLight
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import rememberJsonSync

data class RiftThemeOption(
    val id: String,
    val label: String
)

val riftThemeOptions = listOf(
    RiftThemeOption("zombie", "全副武装"),
    RiftThemeOption("projectile", "反弹子弹"),
    RiftThemeOption("rusher", "小小大僵尸"),
    RiftThemeOption("nuke", "禁止防御"),
    RiftThemeOption("gravity", "万有引力"),
    RiftThemeOption("rift", "僵尸加速"),
    RiftThemeOption("spawn_offset", "加速进场"),
    RiftThemeOption("fire_reduce", "火焰减免"),
    RiftThemeOption("lighting_reduce", "雷电减免"),
    RiftThemeOption("cold_reduce", "寒冰减免"),
    RiftThemeOption("miner_cheating", "矿工快速挖掘"),
    RiftThemeOption("mage_cheating", "法师增益"),
    RiftThemeOption("knight_cheating", "冲锋无敌"),
    RiftThemeOption("invisible", "隐形战争"),
    RiftThemeOption("sun", "阳光飞逝"),
    RiftThemeOption("dark", "打雷天"),
    RiftThemeOption("blizzard", "冰雪节"),
    RiftThemeOption("gravestone", "死亡墓碑"),
    RiftThemeOption("plant_exploder", "两败俱伤"),
    RiftThemeOption("plant_aoe", "炸翻天"),
    RiftThemeOption("plant_fastcd", "加速防御"),
    RiftThemeOption("plant_melee", "近战之光"),
    RiftThemeOption("lemon", "腐蚀酸雨"),
    RiftThemeOption("seed_rain", "种子雨"),
    RiftThemeOption("balloon", "气球满天飞"),
    RiftThemeOption("plant_seed", "斩草不除根"),
    RiftThemeOption("piggy_bank", "碎碎平安"),
    RiftThemeOption("energy_fly", "能量飞逝"),
    RiftThemeOption("watering", "浇水壶"),
    RiftThemeOption("disable_boost", "回归基本功"),
    RiftThemeOption("printer", "打印机"),
    RiftThemeOption("cleaner", "扫地机器人"),
    RiftThemeOption("pea_rain", "豌豆雨"),
    RiftThemeOption("sun_disabled", "死亡阳光"),
    RiftThemeOption("zombie_sun", "星期日"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiftThemeDemoModuleEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile
) {
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val currentAlias = RtidParser.parse(rtid)?.alias ?: "RiftThemeDemo"

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, RiftThemeDemoModuleData::class.java)
    val dataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    val predefinedIds = remember { riftThemeOptions.map { it.id }.toSet() }

    val selectedPredefinedCount = remember(dataState.value.demoRiftThemeName) {
        dataState.value.demoRiftThemeName.count { it in predefinedIds }
    }

    val unknownThemes = remember(dataState.value.demoRiftThemeName) {
        dataState.value.demoRiftThemeName.filter { it !in predefinedIds }
    }

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzCyanDark else PvzCyanLight

    fun toggleTheme(id: String) {
        val current = dataState.value.demoRiftThemeName.toMutableList()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        dataState.value = dataState.value.copy(demoRiftThemeName = current)
        sync()
    }

    fun addCustomTheme(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val current = dataState.value.demoRiftThemeName.toMutableList()
        if (!current.contains(trimmed)) {
            current.add(trimmed)
        }
        dataState.value = dataState.value.copy(demoRiftThemeName = current)
        sync()
    }

    fun removeCustomTheme(name: String) {
        val current = dataState.value.demoRiftThemeName.toMutableList()
        current.remove(name)
        dataState.value = dataState.value.copy(demoRiftThemeName = current)
        sync()
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            CommonEditorTopAppBar(
                title = "关卡主题设置",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "关卡主题模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "本模块用于配置潘妮追击或回忆之旅关卡中的主题效果。勾选需要激活的主题即可。"
                )
                HelpSection(
                    title = "自定义主题",
                    body = "如果需要使用的主题不在预定义列表中，可以在下方\"自定义主题\"区域手动输入主题代号进行添加。"
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === 预定义主题选择 ===
            Text(
                text = "选择关卡主题",
                fontWeight = FontWeight.Bold,
                color = themeColor,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
            )

            // 超过3个主题时的警告
            if (selectedPredefinedCount > 2) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "已选择 $selectedPredefinedCount 个主题，过多主题可能导致关卡闪退",
                            color = MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    riftThemeOptions.forEachIndexed { index, option ->
                        val isChecked = dataState.value.demoRiftThemeName.contains(option.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toggleTheme(option.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { toggleTheme(option.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = themeColor,
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.label,
                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = option.id,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (index < riftThemeOptions.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // === 自定义主题区域（兜底处理） ===
            Text(
                text = "自定义主题",
                fontWeight = FontWeight.Bold,
                color = themeColor,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp)
            )

            // 未知主题提示
            if (unknownThemes.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "以下主题不在预定义列表中，可能来自新版游戏或其他来源。你可以保留或删除它们。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onTertiary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    var customInput by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customInput,
                            onValueChange = { customInput = it },
                            placeholder = { Text("输入主题代号", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                cursorColor = themeColor
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    addCustomTheme(customInput)
                                    customInput = ""
                                    focusManager.clearFocus()
                                }
                            ),
                            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                                fontSize = 14.sp
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                addCustomTheme(customInput)
                                customInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("添加")
                        }
                    }

                    // 显示当前所有不在预定义列表中的主题
                    val displayList = unknownThemes.ifEmpty {
                        // 没有任何未知主题时显示空状态
                        emptyList()
                    }
                    if (displayList.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "暂无自定义主题，你可以在上方输入框中添加",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        displayList.forEach { themeName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            themeColor.copy(alpha = 0.1f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = themeName,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { removeCustomTheme(themeName) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        "删除",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
