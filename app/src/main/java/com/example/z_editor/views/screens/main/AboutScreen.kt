package com.example.z_editor.views.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.views.components.rememberDebouncedClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val themeColor = MaterialTheme.colorScheme.primary
    val handleBack = rememberDebouncedClick { onBack() }
    BackHandler(onBack = handleBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "软件介绍",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColor,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Z-Editor",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = themeColor
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "PVZ2 关卡可视化编辑器",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(18.dp))

            InfoSectionCard(title = "简介") {
                Text(
                    "\tZ-Editor 是一款专为《植物大战僵尸2》设计的可视化关卡编辑工具。它旨在解决直接修改 JSON 文件繁琐、易错的问题，提供直观的图形界面来管理关卡配置。",
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            InfoSectionCard(title = "核心功能") {
                BulletPoint("模块化编辑：对关卡模块和事件进行模块化管理，实现快速配置。")
                BulletPoint("多模式支持：支持编辑我是僵尸、砸罐子、坚不可摧、僵王战等多种模式。")
                BulletPoint("自定义注入：实现在关卡内注入并管理自定义僵尸、编辑其基础属性。")
                BulletPoint("智能校验：自动检测模块依赖缺失、引用失效等问题，有效预防关卡闪退。")
                BulletPoint("资源预览：内置植物、僵尸、障碍物图标，优化编辑体验，所见即所得。")
            }

            InfoSectionCard(title = "使用说明") {
                Text(
                    "1. 目录设置：首次进入请点击右上角文件夹图标，选择存放 JSON 关卡文件的目录。\n" +
                            "2. 导入/新建：可以直接点击列表项编辑现有关卡，或使用右下角按钮基于模板新建。\n" +
                            "3. 模块管理：在编辑器中，可以通过“添加新模块”来扩展关卡功能。\n" +
                            "4. 保存关卡：编辑完成后点击右上角保存按钮，文件将自动回写到原 JSON 文件。\n" +
                            "如果有使用上的疑问或反馈建议，欢迎加入交流 QQ 群 562251204 进行交流。",
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            InfoSectionCard(title = "致谢名单") {
                BulletPoint("软件作者：")
                Text(
                    "降维打击",
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                BulletPoint("特别鸣谢：")
                Text(
                    "星寻、metal海枣、超越自我3333、桃酱、凉沈、小小师、顾小言、PhiLia093、咖啡、不留名",
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(20.dp))

            Text(
                text = "穿越时空 创造无穷可能",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 18.sp
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Version 1.1.5",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color.LightGray,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun InfoSectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp,
                color = Color.LightGray.copy(alpha = 0.5f)
            )
            content()
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text, lineHeight = 24.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}