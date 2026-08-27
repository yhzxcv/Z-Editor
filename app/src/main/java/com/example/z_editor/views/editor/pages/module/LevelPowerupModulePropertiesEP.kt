package com.example.z_editor.views.editor.pages.module

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.LevelPowerupData
import com.example.z_editor.data.LevelPowerupModulePropertiesData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzYellowDark
import com.example.z_editor.ui.theme.PvzYellowLight
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorContentWindowInsets
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

// TypeName → 中文名（对应 JSON 中间注释），技能类型固定三种
private val POWERUP_TYPE_OPTIONS = listOf(
    "powerupflickzombie" to "浮空指",
    "powerupwizardfinger" to "电击指",
    "poweruppinchzombie" to "剪刀指"
)

private const val DEFAULT_FREE_USE_COUNT = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelPowerupModulePropertiesEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, LevelPowerupModulePropertiesData::class.java)
    val moduleDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzYellowDark else PvzYellowLight

    fun powerupCount(typeName: String): Int =
        moduleDataState.value.powerups.firstOrNull { it.typeName == typeName }?.freeUseCount
            ?: DEFAULT_FREE_USE_COUNT

    fun updatePowerupCount(typeName: String, count: Int) {
        val newList = moduleDataState.value.powerups.toMutableList()
        val index = newList.indexOfFirst { it.typeName == typeName }
        if (index != -1) {
            newList[index] = newList[index].copy(freeUseCount = count)
        } else {
            newList.add(LevelPowerupData(typeName = typeName, freeUseCount = count))
        }
        moduleDataState.value = moduleDataState.value.copy(powerups = newList)
        sync()
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        contentWindowInsets = EditorContentWindowInsets(),
        topBar = {
            CommonEditorTopAppBar(
                title = "金手指设置",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "金手指模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "关卡里使用金手指写法，配置玩家在关卡内可免费使用的手势技能。中文版没有冰雹指和火焰指，可选的有浮空指、电击指、剪刀指。"
                )
                HelpSection(
                    title = "使用方法",
                    body = "将本模块放置于开头关卡定义的 Modules 中即可生效。"
                )
                HelpSection(
                    title = "参数说明",
                    body = "免费使用次数（FreeUseCount）为该技能可免费使用的次数，超出后需要消耗其他资源。"
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "金手指技能",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = themeColor
                    )
                    POWERUP_TYPE_OPTIONS.forEach { (code, display) ->
                        NumberInputInt(
                            value = powerupCount(code),
                            onValueChange = { count -> updatePowerupCount(code, count) },
                            label = "$display (免费使用次数)",
                            color = themeColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
