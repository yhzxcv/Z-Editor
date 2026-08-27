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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.HamsterZombieItem
import com.example.z_editor.data.HamsterZombieSpawnerData
import com.example.z_editor.data.LevelParser
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.repository.ZombieRepository
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzGrayDark
import com.example.z_editor.ui.theme.PvzGrayLight
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputDouble
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

private const val HAMSTER_BALL_TYPE = "RTID(hamster_ball@ZombieTypes)"

private val BEHAVIOR_OPTIONS = listOf(
    Triple(0, "匀速运动", "0 = 匀速运动"),
    Triple(1, "先快后慢", "1 = 初始快，碰到植物后变慢"),
    Triple(2, "碰撞换行", "2 = 碰到植物会换行")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HamsterBallEventEP(
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
    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, HamsterZombieSpawnerData::class.java)
    val actionDataState = syncManager.dataState

    fun sync() {
        val v = actionDataState.value
        var updated = v
        // Type 固定写入：仅 hamster_ball
        val forced = v.zombies.map {
            if (it.type != HAMSTER_BALL_TYPE) it.copy(type = HAMSTER_BALL_TYPE) else it
        }.toMutableList()
        if (forced != v.zombies) {
            updated = updated.copy(zombies = forced)
        }
        // 起始/结束列不影响内容，静默固定写入 0 和 8
        if (updated.columnStart != 0 || updated.columnEnd != 8) {
            updated = updated.copy(columnStart = 0, columnEnd = 8)
        }
        if (updated != v) {
            actionDataState.value = updated
        }
        syncManager.sync()
    }

    fun updateItem(index: Int, transform: (HamsterZombieItem) -> HamsterZombieItem) {
        val list = actionDataState.value.zombies.toMutableList()
        list[index] = transform(list[index])
        actionDataState.value = actionDataState.value.copy(zombies = list)
        sync()
    }

    var zombieToCustomizeIndex by remember { mutableStateOf<Int?>(null) }

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzGrayDark else PvzGrayLight

    if (zombieToCustomizeIndex != null) {
        val index = zombieToCustomizeIndex!!
        val zombieData = actionDataState.value.zombies[index]
        val insideType = zombieData.zombieInsideBallType

        if (insideType.isBlank()) {
            AlertDialog(
                onDismissRequest = { zombieToCustomizeIndex = null },
                title = { Text("无法自定义", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                text = { Text("请先为仓鼠球选择内部僵尸，再使用自定义功能。") },
                confirmButton = {
                    TextButton(onClick = { zombieToCustomizeIndex = null }) { Text("确定") }
                }
            )
        } else {
            val currentIsCustom = RtidParser.parse(insideType)?.source == "CurrentLevel"

            val currentBaseType = remember(insideType) {
                ZombieRepository.resolveZombieType(insideType, objectMap).first
            }
            val displayName = ZombieRepository.getName(currentBaseType)

            val compatibleCustomZombies = remember(rootLevelFile.objects, currentBaseType) {
                rootLevelFile.objects
                    .filter { it.objClass == "ZombieType" }
                    .mapNotNull { objItem ->
                        try {
                            val json = objItem.objData.asJsonObject
                            if (json.has("TypeName") && json.get("TypeName").asString == currentBaseType) {
                                val alias = objItem.aliases?.firstOrNull() ?: "Unknown"
                                val rtid = RtidParser.build(alias, "CurrentLevel")
                                if (rtid != insideType) alias to rtid else null
                            } else null
                        } catch (_: Exception) {
                            null
                        }
                    }
            }

            AlertDialog(
                onDismissRequest = { zombieToCustomizeIndex = null },
                title = {
                    Text(
                        if (currentIsCustom) "配置自定义僵尸" else "创建自定义僵尸",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
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
                                    onEditCustomZombie(insideType)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = themeColor,
                                    contentColor = MaterialTheme.colorScheme.surface
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
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    " 或 ",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val currentAlias = RtidParser.parse(insideType)?.alias ?: insideType
                                val newRtid = onInjectZombie(currentAlias)
                                if (newRtid != null) {
                                    updateItem(index) { it.copy(zombieInsideBallType = newRtid) }
                                    localRefreshTrigger++
                                    zombieToCustomizeIndex = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("创建并应用新的自定义")
                        }

                        if (compatibleCustomZombies.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "切换至已有的同类定义：",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            compatibleCustomZombies.forEach { (alias, rtidValue) ->
                                Card(
                                    onClick = {
                                        updateItem(index) { it.copy(zombieInsideBallType = rtidValue) }
                                        localRefreshTrigger++
                                        zombieToCustomizeIndex = null
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(alias, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(Modifier.weight(1f))
                                        if (rtidValue == insideType) {
                                            Icon(
                                                Icons.Default.Check,
                                                null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowForward,
                                                null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "暂无其他兼容的自定义僵尸",
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { zombieToCustomizeIndex = null }) { Text("取消") }
                }
            )
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        // 底部内边距统一走 contentWindowInsets：键盘弹出时自动在底部留出空间，避免遮挡输入框
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        topBar = {
            CommonEditorTopAppBar(
                title = "编辑 $currentAlias",
                subtitle = "事件类型：僵尸仓鼠球",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "仓鼠球事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "滚动仓鼠球将僵尸带入场地，在波次内编写即可生效。"
                )
                HelpSection(
                    title = "滚动范围",
                    body = "仓鼠球滚动范围固定为从0列到8列，该字段不影响内容，由软件静默写入。"
                )
                HelpSection(
                    title = "生成逻辑",
                    body = "每组数量表述一组里出现几个仓鼠球，组间间隔为相邻组的间隔时间，达到最大生成时间后不会进行额外分组直接全部生成。"
                )
            }
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === 区域 1: 生成逻辑 ===
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            color = themeColor
                        )

                        NumberInputInt(
                            value = actionDataState.value.groupSize,
                            onValueChange = {
                                actionDataState.value = actionDataState.value.copy(groupSize = it)
                                sync()
                            },
                            label = "每组数量 (GroupSize)",
                            modifier = Modifier.fillMaxWidth(),
                            color = themeColor
                        )

                        NumberInputInt(
                            value = actionDataState.value.timeBetweenGroups,
                            onValueChange = {
                                actionDataState.value =
                                    actionDataState.value.copy(timeBetweenGroups = it)
                                sync()
                            },
                            label = "组间间隔 (TimeBetweenGroups)",
                            modifier = Modifier.fillMaxWidth(),
                            color = themeColor
                        )

                        NumberInputInt(
                            value = actionDataState.value.timeBeforeFullSpawn,
                            onValueChange = {
                                actionDataState.value =
                                    actionDataState.value.copy(timeBeforeFullSpawn = it)
                                sync()
                            },
                            label = "全部生成所需时间 (TimeBeforeFullSpawn)",
                            modifier = Modifier.fillMaxWidth(),
                            color = themeColor
                        )
                    }
                }
            }

            // === 区域 2: 僵尸列表头 ===
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "仓鼠球内僵尸 (Zombies)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = themeColor,
                    )
                    Button(
                        onClick = {
                            onRequestZombieSelection { selectedId ->
                                val aliases = ZombieRepository.buildZombieAliases(selectedId)
                                val fullRtid = RtidParser.build(aliases, "ZombieTypes")
                                val newList = actionDataState.value.zombies.toMutableList()
                                newList.add(HamsterZombieItem(zombieInsideBallType = fullRtid))
                                actionDataState.value =
                                    actionDataState.value.copy(zombies = newList)
                                sync()
                                localRefreshTrigger++
                            }
                        },
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

            itemsIndexed(actionDataState.value.zombies) { index, zombie ->
                val insideType = zombie.zombieInsideBallType
                val parsed = RtidParser.parse(insideType)
                val isCustom = parsed?.source == "CurrentLevel"
                val (baseTypeName, isValid) = if (insideType.isBlank()) {
                    "" to false
                } else {
                    ZombieRepository.resolveZombieType(insideType, objectMap)
                }
                val displayName = when {
                    insideType.isBlank() -> "未选择仓鼠球内僵尸"
                    isCustom -> parsed?.alias ?: insideType
                    else -> ZombieRepository.getName(baseTypeName)
                }
                val isElite = ZombieRepository.isElite(baseTypeName)
                val info = remember(baseTypeName) {
                    ZombieRepository.getZombieInfoById(baseTypeName)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssetImage(
                                path = if (isValid && info?.icon != null) "images/zombies/${info.icon}" else "images/others/unknown.webp",
                                contentDescription = displayName,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isValid) Color(0xFFEEEEEE) else Color(0xFFFFEBEE))
                                    .border(
                                        0.5.dp,
                                        if (isValid) Color.Transparent else MaterialTheme.colorScheme.onError,
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
                                        color = if (isValid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onError
                                    )
                                    if (isCustom) {
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.onTertiary,
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
                                    } else if (isElite) {
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceTint,
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
                                Text(
                                    if (insideType.isBlank()) "点击「自定义」前请先添加内部僵尸"
                                    else if (!isValid) "引用对象不存在，请检查或删除"
                                    else if (isCustom) "原型: $baseTypeName"
                                    else baseTypeName,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))

                        // === 单个仓鼠球条目的属性 ===
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberInputInt(
                                value = zombie.level,
                                onValueChange = { newVal -> updateItem(index) { it.copy(level = newVal) } },
                                label = "僵尸等级",
                                modifier = Modifier.weight(1f),
                                color = themeColor
                            )
                            NumberInputDouble(
                                value = zombie.speedBeforeImpact,
                                onValueChange = { newVal ->
                                    updateItem(index) {
                                        it.copy(
                                            speedBeforeImpact = newVal
                                        )
                                    }
                                },
                                label = "初始速度",
                                modifier = Modifier.weight(1f),
                                color = themeColor
                            )
                        }
                        Spacer(Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BEHAVIOR_OPTIONS.forEach { (value, label, _) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        updateItem(index) { it.copy(behavior = value) }
                                    }
                                ) {
                                    RadioButton(
                                        colors = RadioButtonDefaults.colors(selectedColor = themeColor),
                                        selected = zombie.behavior == value,
                                        onClick = {
                                            updateItem(index) { it.copy(behavior = value) }
                                        }
                                    )
                                    Text(label, fontSize = 13.sp)
                                }
                                Spacer(Modifier.width(2.dp))
                            }
                        }
                        Text(
                            "行为 (Behavior)：${BEHAVIOR_OPTIONS.firstOrNull { it.first == zombie.behavior }?.third ?: "未知"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "携带能量豆 (HasPlantfood)",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Switch(
                                checked = zombie.hasPlantfood,
                                onCheckedChange = { newVal ->
                                    updateItem(index) {
                                        it.copy(
                                            hasPlantfood = newVal
                                        )
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = themeColor,
                                    checkedBorderColor = Color.Transparent,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val newList = actionDataState.value.zombies.toMutableList()
                                    newList.add(index + 1, zombie.copy())
                                    actionDataState.value =
                                        actionDataState.value.copy(zombies = newList)
                                    sync()
                                    localRefreshTrigger++
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.outline,
                                    contentColor = MaterialTheme.colorScheme.secondary
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
                                if (isCustom) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
                            val customContentColor =
                                if (isCustom) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.primary
                            val customText = if (isCustom) "编辑属性" else "自定义"

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
                                Icon(Icons.Default.Build, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(customText, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val newList = actionDataState.value.zombies.toMutableList()
                                    newList.removeAt(index)
                                    actionDataState.value =
                                        actionDataState.value.copy(zombies = newList)
                                    sync()
                                    localRefreshTrigger++
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
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

            if (actionDataState.value.zombies.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "列表中没有僵尸",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
