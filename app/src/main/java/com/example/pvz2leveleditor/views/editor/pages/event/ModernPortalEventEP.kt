package com.example.pvz2leveleditor.views.editor.pages.event

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pvz2leveleditor.data.PortalEventData
import com.example.pvz2leveleditor.data.PvzLevelFile
import com.example.pvz2leveleditor.data.repository.ZombieRepository
import com.example.pvz2leveleditor.data.repository.ZombieTag
import com.example.pvz2leveleditor.data.RtidParser
import com.example.pvz2leveleditor.views.components.AssetImage
import com.example.pvz2leveleditor.views.editor.EditorHelpDialog
import com.example.pvz2leveleditor.views.editor.HelpSection
import com.google.gson.Gson

private val gson = Gson()

// === 1. 重构裂缝数据定义 ===
data class PortalWorldDef(
    val typeCode: String,
    val name: String,
    val representativeZombies: List<String> // 硬编码的僵尸 ID
)

// === 2. 硬编码裂缝数据与对应的僵尸 ===
private val PORTAL_DEFINITIONS = listOf(
    PortalWorldDef("egypt", "主线埃及", listOf("ra", "tomb_raiser", "pharaoh")),
    PortalWorldDef("egypt_2", "埃及2号", listOf("explorer")),
    PortalWorldDef("pirate", "主线海盗", listOf("pirate_captain", "seagull", "barrelroller")),
    PortalWorldDef("cowboy", "主线西部", listOf("piano", "prospector", "poncho_plate")),
    PortalWorldDef("future", "主线未来", listOf("future_protector", "mech_cone", "football_mech")),
    PortalWorldDef("future_2", "未来2号", listOf("future_jetpack", "mech_cone", "future_armor1")),
    PortalWorldDef("dark", "主线沙滩", listOf("dark_juggler")),
    PortalWorldDef("beach", "主线沙滩", listOf("beach_octopus", "beach_surfer")),
    PortalWorldDef("iceage", "主线冰河", listOf("iceage_hunter", "iceage_weaselhoarder")),
    PortalWorldDef("lostcity", "主线失落", listOf("lostcity_excavator", "lostcity_jane")),
    PortalWorldDef("eighties", "主线摇滚", listOf("eighties_breakdancer", "eighties_mc")),
    PortalWorldDef("dino", "主线恐龙", listOf("dino_imp", "dino_bully"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpawnModernPortalsWaveActionPropsEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: ScrollState
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    var showHelpDialog by remember { mutableStateOf(false) }

    // 初始化数据
    val portalDataState = remember {
        val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        var data = try {
            if (obj != null) {
                gson.fromJson(obj.objData, PortalEventData::class.java)
            } else {
                PortalEventData()
            }
        } catch (_: Exception) {
            PortalEventData()
        }

        if (data == null) data = PortalEventData()
        // 默认值兜底
        if (data.portalRow < 0) data.portalRow = 2
        if (data.portalColumn < 0) data.portalColumn = 6
        if (data.portalType.isBlank()) data.portalType = "egypt"

        mutableStateOf(data)
    }

    var previewWorldDef by remember { mutableStateOf<PortalWorldDef?>(null) }

    fun sync() {
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }?.let {
            it.objData = gson.toJsonTree(portalDataState.value)
        }
    }

    if (previewWorldDef != null) {
        AlertDialog(
            onDismissRequest = { previewWorldDef = null },
            title = { Text(text = "${previewWorldDef!!.name} - 僵尸预览") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("该裂缝将生成以下僵尸:", fontSize = 14.sp, color = Color.Gray)
                    HorizontalDivider()
                    previewWorldDef!!.representativeZombies.forEach { typeName ->
                        val info = remember(typeName) {
                            ZombieRepository.search(typeName, ZombieTag.All).firstOrNull()
                        }
                        val displayName = ZombieRepository.getName(typeName)

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                            Column {
                                Text(displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(typeName, fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { previewWorldDef = null }) { Text("关闭") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("编辑 $currentAlias", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("事件类型：时空裂缝", fontSize = 15.sp, fontWeight = FontWeight.Normal)
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
                title = "时空裂缝事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = Color(0xFFFF9800)
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "在场地上刷新出固定种类的时空裂缝，常见于摩登世界和回忆之旅。"
                )
                HelpSection(
                    title = "世界类型",
                    body = "游戏内有非常多种裂缝类型，可以在此选择具体的裂缝种类，预览出怪。"
                )
                HelpSection(
                    title = "无视墓碑",
                    body = "开启此开关后，裂缝不会因为被墓碑冲浪板等障碍物挡住而不生成。"
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === 区域 1: 位置可视化 ===
            SimplePortalPositionCard(
                currentRow = portalDataState.value.portalRow,
                currentCol = portalDataState.value.portalColumn,
                onPositionSelected = { r, c ->
                    portalDataState.value =
                        portalDataState.value.copy(portalRow = r - 1, portalColumn = c - 1)
                    sync()
                }
            )

            // === 区域 2: 类型选择 (极简卡片 + 弹窗入口) ===
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "世界类型",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(12.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp), // 更小的宽度，一行能放3个左右
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(260.dp)
                        ) {
                            items(PORTAL_DEFINITIONS) { def ->
                                val isSelected = def.typeCode == portalDataState.value.portalType
                                MinimalPortalCard(
                                    def = def,
                                    isSelected = isSelected,
                                    onClick = {
                                        portalDataState.value =
                                            portalDataState.value.copy(portalType = def.typeCode)
                                        sync()
                                    },
                                    onInfoClick = {
                                        previewWorldDef = def
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // === 区域 3: 高级属性 ===
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Settings,
                                null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("高级属性", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(Modifier.height(16.dp))

                        // IgnoreGraveStone
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val current = portalDataState.value.ignoreGraveStone
                                    portalDataState.value =
                                        portalDataState.value.copy(ignoreGraveStone = !current)
                                    sync()
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("无视墓碑 (IgnoreGraveStone)", fontSize = 14.sp)
                                Text(
                                    "开启后裂缝可无视障碍物生成",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = portalDataState.value.ignoreGraveStone,
                                onCheckedChange = {
                                    portalDataState.value =
                                        portalDataState.value.copy(ignoreGraveStone = it)
                                    sync()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFFF9800)
                                ),
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MinimalPortalCard(
    def: PortalWorldDef,
    isSelected: Boolean,
    onClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFFF3E0) else Color(0xFFFAFAFA)
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFFFF9800) else Color.LightGray.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：名字 + 选中标记
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = def.name,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    color = if (isSelected) Color.Black else Color.DarkGray,
                    maxLines = 1
                )
            }

            // 右侧：信息按钮 (独立点击区域)
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Details",
                    tint = if (isSelected) Color(0xFFFF9800) else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SimplePortalPositionCard(
    currentRow: Int,
    currentCol: Int,
    onPositionSelected: (Int, Int) -> Unit
) {
    val displayRow = currentRow + 1
    val displayCol = currentCol + 1

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier
                .widthIn(max = 480.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row {
                    Text(
                        "生成位置",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "R$displayRow : C$displayCol",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFFF9800)
                    )
                }
                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 5f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFEEEEEE))
                ) {
                    for (r in 1..5) {
                        Row(modifier = Modifier.weight(1f)) {
                            for (c in 1..9) {
                                val isSelected = r == displayRow && c == displayCol
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(1.dp)
                                        .background(if (isSelected) Color(0xFFFF9800) else Color.White)
                                        .clickable { onPositionSelected(r, c) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
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

fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)