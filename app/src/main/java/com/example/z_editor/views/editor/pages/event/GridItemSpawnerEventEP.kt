package com.example.z_editor.views.editor.pages.event

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.SpawnZombiesFromGridItemData
import com.example.z_editor.data.ZombieSpawnData
import com.example.z_editor.data.repository.GridItemRepository
import com.example.z_editor.data.repository.ZombieRepository
import com.example.z_editor.data.repository.ZombieTag
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import com.example.z_editor.views.editor.pages.others.StepperControl
import com.google.gson.Gson

private val gson = Gson()

// ======================== 编辑器界面 ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpawnZombiesFromGridItemSpawnerEventEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    onRequestGridItemSelection: ((String) -> Unit) -> Unit,
    onRequestZombieSelection: ((String) -> Unit) -> Unit,
    scrollState: LazyListState,
    onInjectZombie: (String) -> String?,
    onEditCustomZombie: (String) -> Unit
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    var localRefreshTrigger by remember { mutableIntStateOf(0) }

    val themeColor = Color(0xFF607D8B)

    val objectMap = remember(rootLevelFile, localRefreshTrigger) {
        rootLevelFile.objects.associateBy { it.aliases?.firstOrNull() ?: "unknown" }
    }

    val actionDataState = remember(localRefreshTrigger) {
        val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        val data = try {
            gson.fromJson(obj?.objData, SpawnZombiesFromGridItemData::class.java)
        } catch (_: Exception) {
            SpawnZombiesFromGridItemData()
        }

        data.zombies.forEach { zombie ->
            val (baseTypeName, isValid) = ZombieRepository.resolveZombieType(zombie.type, objectMap)
            zombie.isElite = ZombieRepository.isElite(baseTypeName)

            if (zombie.isElite) {
                zombie.level = null
            } else if ((zombie.level ?: 1) < 1) {
                zombie.level = 1
            }
        }
        mutableStateOf(data)
    }

    val internalObjectAliases = remember(rootLevelFile.objects.size, rootLevelFile.hashCode(), localRefreshTrigger) {
        rootLevelFile.objects.flatMap { it.aliases ?: emptyList() }.toSet()
    }

    fun sync() {
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }?.let {
            it.objData = gson.toJsonTree(actionDataState.value)
        }
    }

    fun addGridType() {
        onRequestGridItemSelection { selectedId ->
            val newList = actionDataState.value.gridTypes.toMutableList()
            newList.add(RtidParser.build(selectedId, "GridItemTypes"))
            actionDataState.value = actionDataState.value.copy(gridTypes = newList)
            sync()
        }
    }

    fun addZombie() {
        onRequestZombieSelection { selectedId ->
            val aliases = ZombieRepository.buildAliases(selectedId)
            val isElite = ZombieRepository.isElite(selectedId)
            val newList = actionDataState.value.zombies.toMutableList()
            newList.add(
                ZombieSpawnData(
                    type = RtidParser.build(aliases, "ZombieTypes"),
                    level = if (isElite) null else 1,
                    row = null
                )
            )
            actionDataState.value = actionDataState.value.copy(zombies = newList)
            sync()
        }
    }
    var zombieToCustomizeIndex by remember { mutableStateOf<Int?>(null) }

    if (zombieToCustomizeIndex != null) {
        val index = zombieToCustomizeIndex!!
        val zombieData = actionDataState.value.zombies[index]

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
                    } catch (e: Exception) { null }
                }
        }

        AlertDialog(
            onDismissRequest = { zombieToCustomizeIndex = null },
            title = { Text("自定义僵尸配置") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text("原型: $displayName", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Button(
                        onClick = {
                            val currentAlias = RtidParser.parse(zombieData.type)?.alias ?: zombieData.type
                            val newRtid = onInjectZombie(currentAlias)
                            if (newRtid != null) {
                                val newList = actionDataState.value.zombies.toMutableList()
                                newList[index] = zombieData.copy(type = newRtid)
                                actionDataState.value = actionDataState.value.copy(zombies = newList)
                                sync()
                                localRefreshTrigger++
                                zombieToCustomizeIndex = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("创建新的自定义属性")
                    }

                    if (compatibleCustomZombies.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("或切换至已有的同类定义：", fontSize = 12.sp, color = Color.Gray)
                        compatibleCustomZombies.forEach { (alias, rtid) ->
                            Card(
                                onClick = {
                                    val newList = actionDataState.value.zombies.toMutableList()
                                    newList[index] = zombieData.copy(type = rtid)
                                    actionDataState.value = actionDataState.value.copy(zombies = newList)
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
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
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
            detectTapGestures(onTap = { focusManager.clearFocus() })
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
                        Text(
                            "事件类型：障碍物出怪",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "帮助", tint = Color.White)
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
                title = "障碍物出怪事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "此事件可以在特定的障碍物类型上进行出怪，常用于黑暗时代的亡灵返乡。"
                )
                HelpSection(
                    title = "生成事件",
                    body = "僵尸会从障碍物所在的格子生成。每个障碍物只能出现1只僵尸，若需要出现多只可以尝试重复引用事件。"
                )
                HelpSection(
                    title = "延迟时间",
                    body = "从波次开始到僵尸生成之间的时间间隔，如果计时尚未结束已经进入下一波则不会进行出怪。"
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
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "基础参数",
                            color = themeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = actionDataState.value.waveStartMessage ?: "",
                            onValueChange = {
                                val newVal = if (it.isBlank()) null else it
                                actionDataState.value =
                                    actionDataState.value.copy(waveStartMessage = newVal)
                                sync()
                            },
                            label = { Text("提示信息 (WaveStartMessage)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                focusedLabelColor = themeColor
                            )
                        )
                        Text(
                            "事件开始时在屏幕中央显示的红字警告，不支持输入中文",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        NumberInputInt(
                            value = actionDataState.value.zombieSpawnWaitTime,
                            onValueChange = {
                                actionDataState.value =
                                    actionDataState.value.copy(zombieSpawnWaitTime = it)
                                sync()
                            },
                            color = themeColor,
                            label = "生成延迟 (秒)",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "僵尸生成前的等待时间，若已进入下一波将不生成僵尸",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "触发源障碍物 (GridTypes)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = themeColor
                    )
                    Button(
                        onClick = { addGridType() },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("添加类型", fontSize = 13.sp)
                    }
                }
            }

            items(actionDataState.value.gridTypes.size) { index ->
                val rtidStr = actionDataState.value.gridTypes[index]

                val parsed = RtidParser.parse(rtidStr)
                val alias = parsed?.alias ?: rtidStr
                val source = parsed?.source

                val isValid = if (source == "CurrentLevel") {
                    internalObjectAliases.contains(alias)
                } else {
                    GridItemRepository.isValid(alias)
                }

                val displayName =
                    if (source == "CurrentLevel") alias else GridItemRepository.getName(alias)

                Card(
                    colors = CardDefaults.cardColors(containerColor = if (!isValid) Color(0xFFF8F1F1) else Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                    border = if (!isValid) androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.Red
                    ) else null
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssetImage(
                            path = GridItemRepository.getIconPath(alias),
                            contentDescription = alias,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEEEEEE)),
                            filterQuality = FilterQuality.Medium,
                            placeholder = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Widgets, null, tint = Color.Gray)
                                }
                            }
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                            Text(
                                text = alias,
                                fontSize = 10.sp,
                                color = if (!isValid) Color.Red else Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                val newList = actionDataState.value.gridTypes.toMutableList()
                                newList.removeAt(index)
                                actionDataState.value =
                                    actionDataState.value.copy(gridTypes = newList)
                                sync()
                            }
                        ) {
                            Icon(Icons.Default.Delete, null, tint = Color.LightGray)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "生成僵尸 (Zombies)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = themeColor
                    )
                    Button(
                        onClick = { addZombie() },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("添加僵尸", fontSize = 13.sp)
                    }
                }
            }

            items(actionDataState.value.zombies.size) { index ->
                val zombieData = actionDataState.value.zombies[index]
                val (baseTypeName, isValid) = ZombieRepository.resolveZombieType(zombieData.type, objectMap)

                val parsed = RtidParser.parse(zombieData.type)
                val isCustom = parsed?.source == "CurrentLevel"
                val alias = parsed?.alias ?: zombieData.type
                val displayName = if (isCustom) alias else ZombieRepository.getName(baseTypeName)
                val info = remember(baseTypeName) {
                    ZombieRepository.getZombieInfoById(baseTypeName)
                }
                val isElite = zombieData.isElite

                Card(
                    colors = CardDefaults.cardColors(containerColor = if (!isValid) Color(0xFFF8F1F1) else Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = if (!isValid) androidx.compose.foundation.BorderStroke(1.dp, Color.Red) else null,
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
                                    .border(0.5.dp, if(isValid) Color.Transparent else Color.Red, RoundedCornerShape(8.dp)),
                                filterQuality = FilterQuality.Medium,
                                placeholder = {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                                                .background(Color(0xFFFF9800), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("自定义", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    else if (isElite) {
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF673AB7), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("精英", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                if (!isValid) {
                                    Text("引用对象不存在，请检查或删除", fontSize = 12.sp, color = Color.Red)
                                } else {
                                    Text(if(isCustom) "原型: $baseTypeName" else alias, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (isValid) {
                            if (isElite) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("僵尸等级", fontSize = 14.sp, color = Color.Gray)
                                    Spacer(Modifier.weight(1f))
                                    Text("精英", fontWeight = FontWeight.Bold, color = Color(0xFF673AB7))
                                }
                            } else {
                                StepperControl(
                                    label = "僵尸等级",
                                    valueText = "${zombieData.level}",
                                    onMinus = {
                                        val current = zombieData.level
                                        if (current != null && current > 1) {
                                            val newList = actionDataState.value.zombies.toMutableList()
                                            newList[index] = zombieData.copy(level = current - 1)
                                            actionDataState.value = actionDataState.value.copy(zombies = newList)
                                            sync()
                                        }
                                    },
                                    onPlus = {
                                        val current = zombieData.level
                                        if (current != null && current < 10) {
                                            val newList = actionDataState.value.zombies.toMutableList()
                                            newList[index] = zombieData.copy(level = current + 1)
                                            actionDataState.value = actionDataState.value.copy(zombies = newList)
                                            sync()
                                        }
                                    }
                                )
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
                                    val newList = actionDataState.value.zombies.toMutableList()
                                    newList.add(index + 1, zombieData.copy())
                                    actionDataState.value = actionDataState.value.copy(zombies = newList)
                                    sync()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE3F2FD),
                                    contentColor = Color(0xFF1565C0)
                                ),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("复制", fontSize = 12.sp)
                            }

                            val customBtnColor = if (isCustom) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                            val customContentColor = if (isCustom) Color(0xFFE65100) else Color(0xFF2E7D32)
                            val customText = if (isCustom) "编辑属性" else "自定义"
                            val customIcon = if (isCustom) Icons.Default.Edit else Icons.Default.Science

                            Button(
                                onClick = {
                                    if (isCustom) {
                                        onEditCustomZombie(zombieData.type)
                                    } else {
                                        zombieToCustomizeIndex = index
                                    }
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
                                    val newList = actionDataState.value.zombies.toMutableList()
                                    newList.removeAt(index)
                                    actionDataState.value = actionDataState.value.copy(zombies = newList)
                                    sync()
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

            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}