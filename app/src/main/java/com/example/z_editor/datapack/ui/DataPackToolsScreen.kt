package com.example.z_editor.datapack.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.views.components.rememberDebouncedClick
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection

/**
 * Sub-screens within the Data Pack Tools section.
 */
private enum class DataPackToolScreen {
    Main,
    FileManager,
    Smf,
    SmfUnpack,
    BatchConvert
}

/**
 * Main hub for experimental data pack tools.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataPackToolsScreen(
    onBack: () -> Unit
) {
    val handleBack = rememberDebouncedClick { onBack() }
    BackHandler(onBack = handleBack)

    var currentTool by remember { mutableStateOf(DataPackToolScreen.Main) }

    // ========== 免责声明弹窗（每次启动软件弹出一次） ==========
    var showDisclaimer by remember { mutableStateOf(true) }

    if (showDisclaimer) {
        DisclaimerDialog(onDismiss = { showDisclaimer = false })
    }

    AnimatedContent(
        targetState = currentTool,
        label = "DataPackNav",
        contentKey = { targetState -> targetState },
        transitionSpec = {
            val animSpec = tween<IntOffset>(durationMillis = 350, easing = FastOutSlowInEasing)
            val fadeSpec = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)
            if (initialState == targetState) {
                EnterTransition.None togetherWith ExitTransition.None
            } else if (targetState != DataPackToolScreen.Main && initialState == DataPackToolScreen.Main) {
                // 进入子页面：从右侧滑入
                (slideInHorizontally(animSpec) { it } + fadeIn(fadeSpec)).togetherWith(
                    slideOutHorizontally(animSpec) { -it / 3 } + fadeOut(fadeSpec)
                )
            } else {
                // 返回主页面 / 子页面间切换：从左侧滑入
                (slideInHorizontally(animSpec) { -it / 3 } + fadeIn(fadeSpec)).togetherWith(
                    slideOutHorizontally(animSpec) { it } + fadeOut(fadeSpec)
                )
            }
        }
    ) { targetState ->
        when (targetState) {
            DataPackToolScreen.Main -> {
                DataPackToolsMainContent(
                    onBack = handleBack,
                    onToolClick = { currentTool = it }
                )
            }

            DataPackToolScreen.FileManager -> {
                DataPackFileManagerScreen(
                    onBack = { currentTool = DataPackToolScreen.Main }
                )
            }

            DataPackToolScreen.Smf -> {
                SmfPackerScreen(
                    onBack = { currentTool = DataPackToolScreen.Main }
                )
            }

            DataPackToolScreen.SmfUnpack -> {
                SmfUnpackerScreen(
                    onBack = { currentTool = DataPackToolScreen.Main }
                )
            }

            DataPackToolScreen.BatchConvert -> {
                BatchConvertScreen(
                    onBack = { currentTool = DataPackToolScreen.Main }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataPackToolsMainContent(
    onBack: () -> Unit,
    onToolClick: (DataPackToolScreen) -> Unit
) {
    val themeColor = MaterialTheme.colorScheme.secondary
    var showHelpDialog by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "实验性功能",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDisclaimerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "风险提示",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "帮助",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColor,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Text(
                text = "数据包工具",
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ToolCard(
                        icon = Icons.Default.SwapHoriz,
                        title = "文件格式转换",
                        subtitle = "支持 json、rton 等格式相互转换",
                        themeColor = themeColor,
                        onClick = { onToolClick(DataPackToolScreen.FileManager) }
                    )
                }
                item {
                    ToolCard(
                        icon = Icons.Default.Autorenew,
                        title = "批量文件格式转换",
                        subtitle = "单文件或整个文件夹批量转换格式",
                        themeColor = themeColor,
                        onClick = { onToolClick(DataPackToolScreen.BatchConvert) }
                    )
                }
                item {
                    ToolCard(
                        icon = Icons.Default.Unarchive,
                        title = "SMF 解包",
                        subtitle = "解包 RSB/RSGP 数据包到公共目录",
                        themeColor = themeColor,
                        onClick = { onToolClick(DataPackToolScreen.SmfUnpack) }
                    )
                }
                item {
                    ToolCard(
                        icon = Icons.Default.FolderZip,
                        title = "数据包补丁",
                        subtitle = "制作 RSB 模式的数据包补丁",
                        themeColor = themeColor,
                        onClick = { onToolClick(DataPackToolScreen.Smf) }
                    )
                }

                item {
                    Text(
                        text = "此为实验性功能，请谨慎操作。操作前建议备份原文件。\n" +
                                "本软件不提供 Rton 加密密钥，需用户自行准备。",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // 帮助弹窗
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "实验性功能说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "功能介绍",
                    body = "这里提供 PvZ2 数据包相关的工具，包括文件格式转换和数据包补丁制作。这些功能属于实验性质，使用前建议备份原始文件。"
                )
                HelpSection(
                    title = "文件格式转换",
                    body = "支持 JSON、RTON、加密 RTON、热更新 JSON 等格式之间的相互转换。\n" +
                            "• JSON ↔ RTON：关卡文件编辑的核心流程\n" +
                            "• RTON ↔ 加密 RTON：游戏使用的加密格式\n" +
                            "• JSON ↔ 热更新JSON：热更新补丁的编解码"
                )
                HelpSection(
                    title = "数据包补丁",
                    body = "用于向 SMF/RSB 容器文件中注入修改后的文件，目前只支持 RSB 模式。\n" +
                            "使用步骤：\n" +
                            "1. 将原始数据包放入 packer/original/\n" +
                            "2. 将补丁文件放入 packer/patches/\n" +
                            "3. 选择模板，开始打包"
                )
                HelpSection(
                    title = "SMF 解包",
                    body = "将 RSB/RSGP 数据包解包为独立文件，结果写入公共目录 /storage/emulated/0/Z_editor/<模板名>/。\n" +
                            "需要「所有文件访问」权限（Android 11+），在系统设置中开启后即可使用。"
                )
                HelpSection(
                    title = "批量文件格式转换",
                    body = "批量转换 json / 普通 RTON / 加密 RTON / 热更新 JSON 格式。\n" +
                            "• 单文件模式：产物原位写到源文件同目录\n" +
                            "• 文件夹模式：产物写入 <文件夹名>~ 目录，原目录不动\n" +
                            "• 需要「所有文件访问」权限（Android 11+）"
                )
                HelpSection(
                    title = "注意事项",
                    body = "• 所有文件操作基于 SAF，首次使用需授权目录\n" +
                            "• 加密操作需要正确的 PvZ2 密钥\n" +
                            "• 补丁文件名需与数据包内文件名一致\n" +
                            "• 本功能处于实验阶段，请谨慎操作"
                )
            }
        }

        // 免责声明弹窗（手动二次查看）
        if (showDisclaimerDialog) {
            DisclaimerDialog(onDismiss = { showDisclaimerDialog = false })
        }
    }
}

@Composable
private fun DisclaimerDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* 不可点击外部关闭，必须点击按钮 */ },
        modifier = Modifier.padding(vertical = 24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        icon = {},
        title = {
            Text(
                text = "风险提示与免责声明",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "本工具涉及对《植物大战僵尸 2》游戏数据的直接修改操作。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• 使用本工具修改游戏数据可能违反游戏服务条款\n" +
                            "• 可能导致游戏账号被临时或永久封禁\n" +
                            "• 可能导致游戏存档损坏或数据丢失\n" +
                            "• 所有操作均为用户自行选择，风险自负",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
                Text(
                    text = "免责声明：",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
                Text(
                    text = "开发者在此明确声明：\n" +
                            "1. 本工具仅供学习研究使用，不鼓励任何形式的游戏作弊行为。\n" +
                            "2. 用户使用本工具所产生的一切后果，包括但不限于账号封禁、数据丢失、" +
                            "游戏体验受损等，均由用户自行承担，开发者不承担任何直接或间接责任。\n" +
                            "3. 用户在使用本工具前应充分了解相关风险，并自行决定是否承担这些风险。\n" +
                            "4. 继续使用即表示您已阅读、理解并同意本免责声明的全部条款。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "已知晓并同意",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    )
}

@Composable
private fun ToolCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    themeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = themeColor.copy(alpha = 0.1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}
