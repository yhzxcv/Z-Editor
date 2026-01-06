package com.example.z_editor.views.screens.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.z_editor.data.EditorSubScreen
import com.example.z_editor.data.EventMetadata
import com.example.z_editor.data.ModuleMetadata
import com.example.z_editor.data.SunDropperPropertiesData
import com.example.z_editor.data.repository.ChallengeTypeInfo

/**
 * 编辑器的一级 Tab 类型定义
 */
enum class EditorTabType(val title: String, val icon: ImageVector) {
    Settings("关卡设置", Icons.Default.Settings),
    Timeline("波次时间轴", Icons.Default.Timeline),
    IZombie("我是僵尸", Icons.Default.EmojiPeople),
    VaseBreaker("罐子布局", Icons.Default.Grid4x4),
    BossFight("僵王属性", Icons.Default.ShieldMoon),
}

/**
 * 定义所有编辑器操作的回调集合
 * 用于解耦 EditorScreen 的逻辑与 EditorContentRouter 的 UI
 */
data class EditorActions(
    val navigateTo: (EditorSubScreen) -> Unit,
    val navigateBack: () -> Unit,

    // 业务逻辑回调
    val onRemoveModule: (String) -> Unit,
    val onAddModule: (ModuleMetadata) -> Unit,
    val onAddEvent: (EventMetadata, Int) -> Unit,
    val onWavesChanged: () -> Unit,
    val onDeleteEventReference: (String) -> Unit,
    val onSaveWaveManager: () -> Unit,
    val onCreateWaveContainer: () -> Unit,
    val onStageSelected: (String) -> Unit,

    // 这里的参数必须明确指定完整包名或确保 import 正确
    val onAddChallenge: (ChallengeTypeInfo) -> Unit,

    // 选择器启动回调
    val onLaunchPlantSelector: ((String) -> Unit) -> Unit,
    val onLaunchZombieSelector: ((String) -> Unit) -> Unit,
    val onLaunchGridItemSelector: ((String) -> Unit) -> Unit,
    val onLaunchChallengeSelector: ((ChallengeTypeInfo) -> Unit) -> Unit,

    // 选择器结果处理
    val onSelectorResult: (Any) -> Unit,
    val onSelectorCancel: () -> Unit,

    val onToggleSunDropperMode: (Boolean, SunDropperPropertiesData) -> Unit = { _, _ -> },
    val onChallengeSelected: (ChallengeTypeInfo) -> Unit
)