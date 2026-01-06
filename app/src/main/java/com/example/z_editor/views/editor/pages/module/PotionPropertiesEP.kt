package com.example.z_editor.views.editor.pages.module

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.PotionSpawnTimerData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.ZombiePotionModulePropertiesData
import com.example.z_editor.data.repository.GridItemRepository
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import com.google.gson.Gson

private val gson = Gson()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZombiePotionModulePropertiesEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    onRequestGridItemSelection: ((String) -> Unit) -> Unit,
    scrollState: ScrollState
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    // 主题色：深紫色 (代表药水/黑暗时代)
    val themeColor = Color(0xFF673AB7)

    // 1. 初始化数据
    val moduleDataState = remember {
        val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        val data = try {
            gson.fromJson(obj?.objData, ZombiePotionModulePropertiesData::class.java)
        } catch (_: Exception) {
            ZombiePotionModulePropertiesData()
        }
        mutableStateOf(data)
    }

    // 2. 同步函数
    fun sync() {
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }?.let {
            it.objData = gson.toJsonTree(moduleDataState.value)
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text("僵尸药水配置", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
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
                title = "僵尸药水模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "此模块会按一定的时间间隔从右往左在随机行生成指定类型的障碍物。"
                )
                HelpSection(
                    title = "生成机制",
                    body = "障碍物会在指定的时间间隔区间内随机生成。如果场上指定障碍物数量达到上限，则不会继续生成。"
                )
                HelpSection(
                    title = "药水类型",
                    body = "会在指定的种类中随机选取，如果想间隔固定时间同时生成多个，可以尝试在关卡里添加多次此模块。"
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === 卡片 1: 数量控制 ===
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "数量控制",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = themeColor
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        NumberInputInt(
                            value = moduleDataState.value.initialPotionCount,
                            onValueChange = {
                                moduleDataState.value = moduleDataState.value.copy(initialPotionCount = it)
                                sync()
                            },
                            label = "初始数量 (Initial)",
                            color = themeColor,
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputInt(
                            value = moduleDataState.value.maxPotionCount,
                            onValueChange = {
                                moduleDataState.value = moduleDataState.value.copy(maxPotionCount = it)
                                sync()
                            },
                            label = "最大数量 (MaxCount)",
                            color = themeColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // === 卡片 2: 生成计时器 ===
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "生成时间间隔 (PotionSpawnTimer)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = themeColor
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        NumberInputInt(
                            value = moduleDataState.value.potionSpawnTimer.min,
                            onValueChange = {
                                val newTimer = moduleDataState.value.potionSpawnTimer.copy(min = it)
                                moduleDataState.value = moduleDataState.value.copy(potionSpawnTimer = newTimer)
                                sync()
                            },
                            label = "最小间隔 (秒)",
                            color = themeColor,
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputInt(
                            value = moduleDataState.value.potionSpawnTimer.max,
                            onValueChange = {
                                val newTimer = moduleDataState.value.potionSpawnTimer.copy(max = it)
                                moduleDataState.value = moduleDataState.value.copy(potionSpawnTimer = newTimer)
                                sync()
                            },
                            label = "最大间隔 (秒)",
                            color = themeColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // === 卡片 3: 药水类型列表 ===
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "药水种类列表 (PotionTypes)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = themeColor
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            onRequestGridItemSelection { selectedType ->
                                val newList = moduleDataState.value.potionTypes.toMutableList()
                                if (!newList.contains(selectedType)) {
                                    newList.add(selectedType)
                                    moduleDataState.value = moduleDataState.value.copy(potionTypes = newList)
                                    sync()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Add, null)
                            Text("添加")
                        }
                    }

                    if (moduleDataState.value.potionTypes.isEmpty()) {
                        Text("暂无配置，请添加药水类型", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            moduleDataState.value.potionTypes.forEachIndexed { index, typeName ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 图标
                                    AssetImage(
                                        path = GridItemRepository.getIconPath(typeName),
                                        contentDescription = typeName,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        filterQuality = FilterQuality.Low,
                                        placeholder = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFFE0E0E0)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Science, null, tint = Color.Gray)
                                            }
                                        }
                                    )

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            GridItemRepository.getName(typeName),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(typeName, fontSize = 10.sp, color = Color.Gray)
                                    }

                                    IconButton(onClick = {
                                        val newList = moduleDataState.value.potionTypes.toMutableList()
                                        newList.removeAt(index)
                                        moduleDataState.value = moduleDataState.value.copy(potionTypes = newList)
                                        sync()
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            null,
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(20.dp)
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
}