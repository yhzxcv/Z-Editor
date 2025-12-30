package com.example.pvz2leveleditor.views.editor.pages.module

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pvz2leveleditor.data.PvzLevelFile
import com.example.pvz2leveleditor.data.RtidParser
import com.example.pvz2leveleditor.data.SunBombChallengeData
import com.example.pvz2leveleditor.views.editor.EditorHelpDialog
import com.example.pvz2leveleditor.views.editor.HelpSection
import com.example.pvz2leveleditor.views.editor.NumberInputInt
import com.google.gson.Gson

private val gson = Gson()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SunBombChallengePropertiesEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    val info = RtidParser.parse(rtid)
    var showHelpDialog by remember { mutableStateOf(false) }
    val currentAlias = info?.alias ?: "SunBombChallenge"

    // 1. 初始化数据状态
    val dataState = remember {
        val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        val data = try {
            if (obj != null) {
                gson.fromJson(obj.objData, SunBombChallengeData::class.java)
            } else {
                SunBombChallengeData()
            }
        } catch (e: Exception) {
            SunBombChallengeData()
        }
        mutableStateOf(data)
    }

    // 2. 同步函数
    fun sync() {
        val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        if (obj != null) {
            obj.objData = gson.toJsonTree(dataState.value)
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text("太阳炸弹配置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "帮助说明", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF9800),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "太阳炸弹模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = Color(0xFFFF9800)
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "本模块是未来世界小游戏阳光炸弹的必要模块，使用后天降阳光会变为紫色可引爆的阳光炸弹。"
                )
                HelpSection(
                    title = "参数配置",
                    body = "本页面可以直接配置阳光炸弹的详细参数，爆炸对不同阵营的杀伤可以区别填写。"
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Dangerous, null, tint = Color(0xFFFF9800))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "爆炸参数配置",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFFE65100)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // 第一组：爆炸半径
                    Text(
                        "爆炸半径 (ExplosionRadius)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumberInputInt(
                            value = dataState.value.plantBombExplosionRadius,
                            onValueChange = {
                                dataState.value =
                                    dataState.value.copy(plantBombExplosionRadius = it)
                                sync()
                            },
                            label = "植物爆炸半径",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputInt(
                            value = dataState.value.zombieBombExplosionRadius,
                            onValueChange = {
                                dataState.value =
                                    dataState.value.copy(zombieBombExplosionRadius = it)
                                sync()
                            },
                            label = "僵尸爆炸半径",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // 第二组：伤害数值
                    Text(
                        "爆炸伤害 (Damage)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumberInputInt(
                            value = dataState.value.plantDamage,
                            onValueChange = {
                                dataState.value = dataState.value.copy(plantDamage = it)
                                sync()
                            },
                            label = "对植物伤害",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputInt(
                            value = dataState.value.zombieDamage,
                            onValueChange = {
                                dataState.value = dataState.value.copy(zombieDamage = it)
                                sync()
                            },
                            label = "对僵尸伤害",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "爆炸半径单位为像素，一格约60像素",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}