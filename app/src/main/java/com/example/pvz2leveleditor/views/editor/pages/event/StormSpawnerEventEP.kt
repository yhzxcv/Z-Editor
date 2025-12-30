package com.example.pvz2leveleditor.views.editor.pages.event

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.pvz2leveleditor.data.LevelParser
import com.example.pvz2leveleditor.data.PvzLevelFile
import com.example.pvz2leveleditor.data.RtidParser
import com.example.pvz2leveleditor.data.StormZombieData
import com.example.pvz2leveleditor.data.StormZombieSpawnerPropsData
import com.example.pvz2leveleditor.data.Repository.ZombiePropertiesRepository
import com.example.pvz2leveleditor.data.Repository.ZombieRepository
import com.example.pvz2leveleditor.data.Repository.ZombieTag
import com.example.pvz2leveleditor.views.components.AssetImage
import com.example.pvz2leveleditor.views.editor.EditorHelpDialog
import com.example.pvz2leveleditor.views.editor.HelpSection
import com.example.pvz2leveleditor.views.editor.NumberInputInt
import com.google.gson.Gson

private val gson = Gson()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StormZombieSpawnerPropsEP(
    rtid: String,
    rootLevelFile: PvzLevelFile,
    onBack: () -> Unit,
    onRequestZombieSelection: ((String) -> Unit) -> Unit,
    scrollState: ScrollState
) {
    val currentAlias = LevelParser.extractAlias(rtid)
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val stormDataState = remember {
        val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        val initialData = try {
            gson.fromJson(obj?.objData, StormZombieSpawnerPropsData::class.java)
        } catch (e: Exception) {
            StormZombieSpawnerPropsData()
        }
        mutableStateOf(initialData)
    }

    fun sync() {
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }?.let {
            it.objData = gson.toJsonTree(stormDataState.value)
        }
    }

    var listRefreshTrigger by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus() // 点击空白处清除焦点
            })
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("编辑 $currentAlias", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("事件类型：风暴突袭", fontSize = 15.sp, fontWeight = FontWeight.Normal)
                    }
                },
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
                    containerColor = Color(0xFFFF9800),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "风暴事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = Color(0xFFFF9800)
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "生成沙尘暴或暴风雪将僵尸快速传送到前线。游戏内出现在埃及和冰河。"
                )
                HelpSection(
                    title = "始末位置",
                    body = "从场地的左边开始计算位置，起始列数要小于结束列数，否则沙尘暴会不生成。"
                )
                HelpSection(
                    title = "生成逻辑",
                    body = "沙尘暴事件可以分组出现，不能独立设置风暴内僵尸的阶级和行号，阶级默认随地图阶级序列。"
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
            // === 区域 1: 风暴类型与范围 ===
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "类型:",
                            modifier = Modifier.width(60.dp),
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                stormDataState.value = stormDataState.value.copy(type = "sandstorm")
                                sync()
                            }
                        ) {
                            RadioButton(
                                selected = stormDataState.value.type == "sandstorm",
                                onClick = {
                                    stormDataState.value =
                                        stormDataState.value.copy(type = "sandstorm")
                                    sync()
                                }
                            )
                            Text("沙尘暴")
                        }
                        Spacer(Modifier.width(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                stormDataState.value = stormDataState.value.copy(type = "snowstorm")
                                sync()
                            }
                        ) {
                            RadioButton(
                                selected = stormDataState.value.type == "snowstorm",
                                onClick = {
                                    stormDataState.value =
                                        stormDataState.value.copy(type = "snowstorm")
                                    sync()
                                }
                            )
                            Text("暴风雪")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputInt(
                            value = stormDataState.value.columnStart,
                            onValueChange = {
                                stormDataState.value = stormDataState.value.copy(columnStart = it)
                                sync()
                            },
                            label = "起始列 (ColumnStart)",
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputInt(
                            value = stormDataState.value.columnEnd,
                            onValueChange = {
                                stormDataState.value = stormDataState.value.copy(columnEnd = it)
                                sync()
                            },
                            label = "结束列 (ColumnEnd)",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        "场地左边界为0列，右边界为9列，起始列要小于结束列",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("生成逻辑", fontWeight = FontWeight.Bold)

                    NumberInputInt(
                        value = stormDataState.value.groupSize,
                        onValueChange = { stormDataState.value.groupSize = it; sync() },
                        label = "每组数量 (GroupSize)"
                    )

                    NumberInputInt(
                        value = stormDataState.value.timeBetweenGroups,
                        onValueChange = { stormDataState.value.timeBetweenGroups = it; sync() },
                        label = "组间间隔 (TimeBetweenGroups)"
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("携带僵尸列表", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            onRequestZombieSelection { zombieId ->
                                val fullRtid = RtidParser.build(zombieId, "ZombieTypes")

                                val newList = stormDataState.value.zombies.toMutableList()
                                newList.add(StormZombieData(type = fullRtid))
                                stormDataState.value = stormDataState.value.copy(zombies = newList)

                                sync()
                                listRefreshTrigger++
                            }
                        }) {
                            Icon(Icons.Default.Add, null)
                            Text("添加僵尸")
                        }
                    }

                    if (stormDataState.value.zombies.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("列表中没有僵尸", color = Color.Gray)
                        }
                    } else {
                        key(listRefreshTrigger) {
                            stormDataState.value.zombies.forEachIndexed { index, zombie ->
                                val realTypeName = remember(zombie.type) {
                                    val alias = RtidParser.parse(zombie.type)?.alias ?: zombie.type
                                    ZombiePropertiesRepository.getTypeNameByAlias(alias)
                                }
                                val info = remember(realTypeName) {
                                    ZombieRepository.search(
                                        realTypeName,
                                        ZombieTag.All
                                    ).firstOrNull()
                                }
                                val displayName = info?.name ?: realTypeName
                                val placeholderContent = @Composable {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFFBDBDBD), CircleShape)
                                            .border(1.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = displayName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AssetImage(
                                        path = if (info?.icon != null) "images/zombies/${info.icon}" else null,
                                        contentDescription = displayName,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(1.dp, Color.LightGray, CircleShape),
                                        filterQuality = FilterQuality.Medium,
                                        placeholder = placeholderContent
                                    )
                                    Spacer(Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(displayName, fontWeight = FontWeight.Bold)
                                        Text(realTypeName, fontSize = 12.sp, color = Color.Gray)
                                    }

                                    IconButton(
                                        onClick = {
                                            val newList =
                                                stormDataState.value.zombies.toMutableList()
                                            newList.removeAt(index)
                                            stormDataState.value =
                                                stormDataState.value.copy(zombies = newList)
                                            sync()
                                            listRefreshTrigger++
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = Color.LightGray)
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