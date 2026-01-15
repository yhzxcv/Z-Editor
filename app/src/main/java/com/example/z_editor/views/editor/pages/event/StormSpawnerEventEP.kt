package com.example.z_editor.views.editor.pages.event

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.LevelParser
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.StormZombieData
import com.example.z_editor.data.StormZombieSpawnerPropsData
import com.example.z_editor.data.repository.ZombieRepository
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import com.google.gson.Gson

private val gson = Gson()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StormZombieSpawnerPropsEP(
    rtid: String,
    rootLevelFile: PvzLevelFile,
    onBack: () -> Unit,
    onRequestZombieSelection: ((String) -> Unit) -> Unit,
    scrollState: LazyListState,
    onInjectZombie: (String) -> String?,
    onEditCustomZombie: (String) -> Unit
) {
    val currentAlias = LevelParser.extractAlias(rtid)
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    var localRefreshTrigger by remember { mutableIntStateOf(0) }

    val objectMap = remember(rootLevelFile, localRefreshTrigger) {
        rootLevelFile.objects.associateBy { it.aliases?.firstOrNull() ?: "unknown" }
    }
    val stormDataState = remember(localRefreshTrigger) {
        val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        val data = try {
            gson.fromJson(obj?.objData, StormZombieSpawnerPropsData::class.java)
        } catch (_: Exception) {
            StormZombieSpawnerPropsData()
        }
        mutableStateOf(data)
    }

    fun sync() {
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }?.let {
            it.objData = gson.toJsonTree(stormDataState.value)
        }
    }

    var zombieToCustomizeIndex by remember { mutableStateOf<Int?>(null) }

    if (zombieToCustomizeIndex != null) {
        val index = zombieToCustomizeIndex!!
        val zombieData = stormDataState.value.zombies[index]

        val currentIsCustom = RtidParser.parse(zombieData.type)?.source == "CurrentLevel"

        val currentBaseType = remember(zombieData.type) {
            val (base, _) = ZombieRepository.resolveZombieType(zombieData.type, objectMap)
            base
        }
        val displayName = ZombieRepository.getName(currentBaseType)

        val compatibleCustomZombies = remember(rootLevelFile.objects, currentBaseType) {
            rootLevelFile.objects
                .filter { it.objClass == "ZombieType" }
                .mapNotNull { obj ->
                    try {
                        val json = obj.objData.asJsonObject
                        if (json.has("TypeName") && json.get("TypeName").asString == currentBaseType) {
                            val alias = obj.aliases?.firstOrNull() ?: "Unknown"
                            val rtid = RtidParser.build(alias, "CurrentLevel")
                            if (rtid != zombieData.type) alias to rtid else null
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                }
        }

        AlertDialog(
            onDismissRequest = { zombieToCustomizeIndex = null },
            title = { Text(if (currentIsCustom) "配置自定义僵尸" else "创建自定义僵尸") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text("原型: $displayName", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    if (currentIsCustom) {
                        Button(
                            onClick = {
                                zombieToCustomizeIndex = null
                                onEditCustomZombie(zombieData.type)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("编辑当前僵尸属性")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                " 或 ",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }
                    }

                    Button(
                        onClick = {
                            val currentAlias =
                                RtidParser.parse(zombieData.type)?.alias ?: zombieData.type
                            val newRtid = onInjectZombie(currentAlias)
                            if (newRtid != null) {
                                val newList = stormDataState.value.zombies.toMutableList()
                                newList[index] = zombieData.copy(type = newRtid)
                                stormDataState.value = stormDataState.value.copy(zombies = newList)
                                sync()
                                localRefreshTrigger++
                                zombieToCustomizeIndex = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("创建并应用新的自定义")
                    }

                    if (compatibleCustomZombies.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("切换至已有的同类定义：", fontSize = 12.sp, color = Color.Gray)
                        compatibleCustomZombies.forEach { (alias, rtid) ->
                            Card(
                                onClick = {
                                    val newList = stormDataState.value.zombies.toMutableList()
                                    newList[index] = zombieData.copy(type = rtid)
                                    stormDataState.value =
                                        stormDataState.value.copy(zombies = newList)
                                    sync()
                                    zombieToCustomizeIndex = null
                                },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(alias, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(Modifier.weight(1f))
                                    if (rtid == zombieData.type) {
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text("暂无其他兼容的自定义僵尸", fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { zombieToCustomizeIndex = null }) { Text("取消") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
            })
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "编辑 $currentAlias",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                    body = "生成沙尘暴或暴风雪将僵尸快速传送到前线。极寒风暴出现于回忆之旅，可以冻结经过的植物。"
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
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === 区域 1: 风暴类型与范围 ===
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "生成参数",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFFF9800)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    stormDataState.value =
                                        stormDataState.value.copy(type = "sandstorm")
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
                                Text("沙尘暴", fontSize = 15.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    stormDataState.value =
                                        stormDataState.value.copy(type = "snowstorm")
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
                                Text("暴风雪", fontSize = 15.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    stormDataState.value =
                                        stormDataState.value.copy(type = "excoldstorm")
                                    sync()
                                }
                            ) {
                                RadioButton(
                                    selected = stormDataState.value.type == "excoldstorm",
                                    onClick = {
                                        stormDataState.value =
                                            stormDataState.value.copy(type = "excoldstorm")
                                        sync()
                                    }
                                )
                                Text("极寒风暴", fontSize = 15.sp)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberInputInt(
                                value = stormDataState.value.columnStart,
                                onValueChange = {
                                    stormDataState.value =
                                        stormDataState.value.copy(columnStart = it)
                                    sync()
                                },
                                label = "起始列 (ColumnStart)",
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFFF9800)
                            )
                            NumberInputInt(
                                value = stormDataState.value.columnEnd,
                                onValueChange = {
                                    stormDataState.value = stormDataState.value.copy(columnEnd = it)
                                    sync()
                                },
                                label = "结束列 (ColumnEnd)",
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFFF9800)
                            )
                        }
                        Text(
                            "场地左边界为0列，右边界为9列，起始列要小于结束列",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // === 区域 2: 生成逻辑 ===
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "生成逻辑",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFFF9800)
                        )

                        NumberInputInt(
                            value = stormDataState.value.groupSize,
                            onValueChange = { stormDataState.value.groupSize = it; sync() },
                            label = "每组数量 (GroupSize)",
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFF9800)
                        )

                        NumberInputInt(
                            value = stormDataState.value.timeBetweenGroups,
                            onValueChange = { stormDataState.value.timeBetweenGroups = it; sync() },
                            label = "组间间隔 (TimeBetweenGroups)",
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }

            // === 区域 3: 僵尸列表头 ===
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "携带僵尸 (Zombies)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFFFF9800)
                    )
                    Button(
                        onClick = {
                            onRequestZombieSelection { selectedId ->
                                val aliases = ZombieRepository.buildAliases(selectedId)
                                val fullRtid = RtidParser.build(aliases, "ZombieTypes")
                                val newList = stormDataState.value.zombies.toMutableList()
                                newList.add(StormZombieData(type = fullRtid))
                                stormDataState.value = stormDataState.value.copy(zombies = newList)
                                sync()
                                localRefreshTrigger++
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("添加僵尸", fontSize = 13.sp)
                    }
                }
            }

            itemsIndexed(stormDataState.value.zombies) { index, zombie ->
                val zombieData = stormDataState.value.zombies[index]
                val (baseTypeName, isValid) = ZombieRepository.resolveZombieType(
                    zombieData.type,
                    objectMap
                )
                val isElite = ZombieRepository.isElite(baseTypeName)

                val parsed = RtidParser.parse(zombieData.type)
                val isCustom = parsed?.source == "CurrentLevel"
                val alias = parsed?.alias ?: zombie.type
                val displayName = if (isCustom) alias else ZombieRepository.getName(baseTypeName)
                val info = remember(baseTypeName) {
                    ZombieRepository.getZombieInfoById(baseTypeName)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = if (!isValid) Color(0xFFF8F1F1) else Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = if (!isValid) androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.Red
                    ) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssetImage(
                                path = if (isValid && info?.icon != null) "images/zombies/${info.icon}" else null,
                                contentDescription = displayName,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isValid) Color(0xFFEEEEEE) else Color(0xFFFFEBEE))
                                    .border(
                                        0.5.dp,
                                        if (isValid) Color.Transparent else Color.Red,
                                        RoundedCornerShape(8.dp)
                                    ),
                                filterQuality = FilterQuality.Medium,
                                placeholder = {
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(displayName.take(1), fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (isValid) Color.Black else Color.Red
                                    )
                                    if (isCustom) {
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    Color(0xFFFF9800),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "自定义",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    else if (isElite) {
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    Color(0xFF673AB7),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "精英",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                if (!isValid) {
                                    Text(
                                        "引用对象不存在，请检查或删除",
                                        fontSize = 12.sp,
                                        color = Color.Red
                                    )
                                } else {
                                    Text(
                                        if (isCustom) "原型: $baseTypeName" else alias,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val newList = stormDataState.value.zombies.toMutableList()
                                    newList.add(index + 1, zombie.copy())
                                    stormDataState.value =
                                        stormDataState.value.copy(zombies = newList)
                                    sync()
                                    localRefreshTrigger++
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE3F2FD),
                                    contentColor = Color(0xFF1565C0)
                                ),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("复制", fontSize = 12.sp)
                            }

                            val customBtnColor =
                                if (isCustom) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                            val customContentColor =
                                if (isCustom) Color(0xFFE65100) else Color(0xFF2E7D32)
                            val customText = if (isCustom) "编辑属性" else "自定义"
                            val customIcon = Icons.Default.Build

                            Button(
                                onClick = {
                                    zombieToCustomizeIndex = index
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = customBtnColor,
                                    contentColor = customContentColor
                                ),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(customIcon, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(customText, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val newList = stormDataState.value.zombies.toMutableList()
                                    newList.removeAt(index)
                                    stormDataState.value =
                                        stormDataState.value.copy(zombies = newList)
                                    sync()
                                    localRefreshTrigger++
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFEBEE),
                                    contentColor = Color(0xFFC62828)
                                ),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("删除", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (stormDataState.value.zombies.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("列表中没有僵尸", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}