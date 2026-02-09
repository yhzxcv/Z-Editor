package com.example.z_editor.views.editor.pages.module

import android.widget.Toast
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.DynamicZombieGroup
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.WaveManagerModuleData
import com.example.z_editor.data.repository.ZombiePropertiesRepository
import com.example.z_editor.data.repository.ZombieRepository
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzPurpleDark
import com.example.z_editor.ui.theme.PvzPurpleLight
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaveManagerModulePropertiesEP(
    rootLevelFile: PvzLevelFile,
    rtid: String,
    onBack: () -> Unit,
    onRequestZombieSelection: ((List<String>) -> Unit) -> Unit,
    scrollState: ScrollState
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val obj = remember(rootLevelFile) {
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    }
    val syncManager = rememberJsonSync(obj, WaveManagerModuleData::class.java)
    val moduleData = syncManager.dataState.value

    fun sync() {
        syncManager.sync()
    }

    LaunchedEffect(Unit) {
        var needsUpdate = false
        var currentData = moduleData
        if (currentData.dynamicZombies != null) {
            if (currentData.dynamicZombies!!.isEmpty()) {
                currentData.dynamicZombies!!.add(DynamicZombieGroup())
                needsUpdate = true
            } else {
                currentData.dynamicZombies!!.forEach { group ->
                    if (group.zombieLevel == null) group.zombieLevel = mutableListOf()
                    if (group.zombiePool == null) group.zombiePool = mutableListOf()

                    while (group.zombieLevel.size < group.zombiePool.size) {
                        group.zombieLevel.add(1)
                        needsUpdate = true
                    }
                }
            }
        }

        if (needsUpdate) {
            syncManager.dataState.value = currentData
            sync()
        }
    }

    var isDynamicZombiesEnabled by remember(moduleData.dynamicZombies) {
        mutableStateOf(moduleData.dynamicZombies != null)
    }

    var refreshTrigger by remember { mutableIntStateOf(0) }

    val actualWaveMgrAlias = remember(rootLevelFile.objects) {
        rootLevelFile.objects.find { it.objClass == "WaveManagerProperties" }?.aliases?.firstOrNull()
    }

    val currentPropsAlias = RtidParser.parse(moduleData.waveManagerProps ?: "")?.alias
    val isPropsValid = actualWaveMgrAlias != null && currentPropsAlias == actualWaveMgrAlias

    val hasLastStandModule = remember(rootLevelFile.objects) {
        rootLevelFile.objects.any { it.objClass == "LastStandMinigameProperties" }
    }

    LaunchedEffect(hasLastStandModule) {
        if (hasLastStandModule && moduleData.manualStartup != true) {
            syncManager.dataState.value = moduleData.copy(manualStartup = true)
            sync()
        }
    }

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzPurpleDark else PvzPurpleLight

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
            })
        },
        topBar = {
            CommonEditorTopAppBar(
                title = "波次管理器设置",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "波次管理器模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "波次管理器模块是波次事件容器的前置定义。只有添加了波次管理器模块软件才会开放波次事件编辑入口。"
                )
                HelpSection(
                    title = "关联波次参数",
                    body = "波次管理器用一个Rtid语句块指向波次事件容器。这里会自动校验该语句是否正确地指向容器。"
                )
                HelpSection(
                    title = "点数分配设置",
                    body = "此处原为国际版旧版本的动态难度机制，在中文版和新版本用于实现点数出怪。这里可以修改关卡点数配置情况。"
                )
                HelpSection(
                    title = "点数出怪介绍",
                    body = "点数出怪会根据僵尸消耗的点数在有效波次中额外刷新僵尸。常规波次点数上限为60000，旗帜波点数会变为2.5倍。"
                )
                HelpSection(
                    title = "僵尸池设置",
                    body = "点数为正数时，出怪使用的僵尸从僵尸池中选取。在波次容器编辑页面内可查看每种僵尸的出现期望。点数为负数时，会从自然出怪事件中扣除相应点数的僵尸。注意点数出怪池不应该写精英怪以及自定义僵尸。"
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isPropsValid) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.error
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isPropsValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                            null,
                            tint = if (isPropsValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onError
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("关联波次参数 (WaveManagerProps)", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "当前值: ${moduleData.waveManagerProps ?: "null"}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    if (actualWaveMgrAlias == null) {
                        Text(
                            "错误：当前关卡不存在波次容器，该模块无法正常工作",
                            color = MaterialTheme.colorScheme.onError,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (!isPropsValid && actualWaveMgrAlias != null) {
                        Text(
                            "错误：当前指向无效。这会导致波次无法正确加载。",
                            color = MaterialTheme.colorScheme.onError,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Button(
                            onClick = {
                                val newProps = RtidParser.build(actualWaveMgrAlias, "CurrentLevel")
                                syncManager.dataState.value =
                                    moduleData.copy(waveManagerProps = newProps)
                                sync()
                            },
                            modifier = Modifier.padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onError)
                        ) {
                            Text("自动修正至: $actualWaveMgrAlias", fontSize = 12.sp)
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timeline,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "启用点数出怪",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isDynamicZombiesEnabled) "已启用 (使用额外点数出怪)" else "未启用 (仅使用波次事件)",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isDynamicZombiesEnabled,
                        onCheckedChange = { checked ->
                            isDynamicZombiesEnabled = checked
                            if (checked) {
                                if (moduleData.dynamicZombies == null) {
                                    val newGroup = DynamicZombieGroup(
                                        startingWave = 3,
                                        startingPoints = 100,
                                        pointIncrement = 40,
                                        zombiePool = mutableListOf(),
                                        zombieLevel = mutableListOf()
                                    )
                                    syncManager.dataState.value = moduleData.copy(
                                        dynamicZombies = mutableListOf(newGroup)
                                    )
                                }
                            } else {
                                syncManager.dataState.value = moduleData.copy(
                                    dynamicZombies = null
                                )
                            }
                            sync()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = themeColor,
                            checkedBorderColor = Color.Transparent,

                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    )
                }
            }

            // === 仅当开关开启时显示详细配置 ===
            if (isDynamicZombiesEnabled) {

                if (isDynamicZombiesEnabled) {
                    val firstGroup = moduleData.dynamicZombies?.firstOrNull()

                    if (firstGroup != null) {
                        Text(
                            "点数分配设置",
                            fontWeight = FontWeight.Bold,
                            color = themeColor,
                            fontSize = 16.sp
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                NumberInputInt(
                                    firstGroup.startingWave,
                                    {
                                        firstGroup.startingWave = it
                                        sync()
                                    },
                                    "起始波次 (StartingWave)",
                                    color = themeColor
                                )
                                NumberInputInt(
                                    firstGroup.startingPoints,
                                    {
                                        firstGroup.startingPoints = it
                                        sync()
                                    },
                                    "起始点数 (StartingPoints)",
                                    color = themeColor
                                )
                                NumberInputInt(
                                    firstGroup.pointIncrement,
                                    {
                                        firstGroup.pointIncrement = it
                                        sync()
                                    },
                                    "每波点数增量 (PointIncrement)",
                                    color = themeColor
                                )
                            }
                        }

                        Text(
                            "僵尸池 (ZombiePool)",
                            fontWeight = FontWeight.Bold,
                            color = themeColor,
                            fontSize = 16.sp
                        )

                        key(refreshTrigger) {
                            val pool = firstGroup.zombiePool
                            val levels = firstGroup.zombieLevel

                            ZombiePoolEditor(
                                zombiePool = pool,
                                zombieLevel = levels,
                                onAdd = {
                                    onRequestZombieSelection { selectedIds ->
                                        var addedCount = 0
                                        selectedIds.forEach { selectedId ->
                                            val isElite = ZombieRepository.isElite(selectedId)
                                            if (!isElite) {
                                                val aliases = ZombieRepository.buildZombieAliases(selectedId)
                                                firstGroup.zombiePool.add(RtidParser.build(aliases, "ZombieTypes"))
                                                firstGroup.zombieLevel.add(1)
                                                addedCount++
                                            }
                                        }

                                        if (addedCount > 0) {
                                            sync()
                                            refreshTrigger++
                                            Toast.makeText(context, "已添加 $addedCount 种僵尸", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "未添加任何僵尸", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onRemove = { index ->
                                    if (index < pool.size) {
                                        pool.removeAt(index)
                                        if (index < levels.size) levels.removeAt(index)
                                        sync()
                                        refreshTrigger++
                                    }
                                },
                                onLevelChange = { index, newLevel ->
                                    if (index < levels.size) {
                                        val newLevels = levels.toMutableList()
                                        newLevels[index] = newLevel
                                        levels.clear()
                                        levels.addAll(newLevels)
                                        sync()
                                        refreshTrigger++
                                    }
                                }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "点数出怪已关闭，将仅使用波次事件列表中的配置。",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ZombiePoolEditor(
    zombiePool: List<String>,
    zombieLevel: List<Int>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onLevelChange: (Int, Int) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzPurpleDark else PvzPurpleLight
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        zombiePool.forEachIndexed { index, rtid ->
            val alias = RtidParser.parse(rtid)?.alias ?: rtid
            val typeName = ZombiePropertiesRepository.getTypeNameByAlias(alias)
            val displayName = ZombieRepository.getName(typeName)
            val level = zombieLevel.getOrNull(index) ?: 1
            val info = remember(typeName) {
                ZombieRepository.getZombieInfoById(typeName)
            }
            val placeholderContent = @Composable {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        fontSize = 24.sp
                    )
                }
            }

            Card(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssetImage(
                        path = if (info?.icon != null) "images/zombies/${info.icon}" else "images/others/unknown.webp",
                        contentDescription = displayName,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(16.dp)
                            ),
                        filterQuality = FilterQuality.Medium,
                        placeholder = placeholderContent
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            typeName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "等级: $level",
                            fontSize = 12.sp,
                            color = if (level >= 6) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { if (level > 1) onLevelChange(index, level - 1) }) {
                        Icon(
                            Icons.Default.Remove,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { if (level < 10) onLevelChange(index, level + 1) },
                        enabled = level < 10
                    ) {
                        Icon(
                            Icons.Default.Add, null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { onRemove(index) }) {
                        Icon(
                            Icons.Default.Delete,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, themeColor, RoundedCornerShape(8.dp))
                .clickable { onAdd() }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddCircleOutline, null, tint = themeColor)
                Spacer(Modifier.width(8.dp))
                Text(
                    "添加新僵尸",
                    color = themeColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}