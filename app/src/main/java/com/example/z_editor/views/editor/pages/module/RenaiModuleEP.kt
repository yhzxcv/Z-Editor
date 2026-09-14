package com.example.z_editor.views.editor.pages.module

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.LevelDefinitionData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RenaiModulePropertiesData
import com.example.z_editor.data.RenaiStatueData
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.repository.ReferenceRepository
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzGridHighLight
import com.example.z_editor.ui.theme.PvzLightPurpleDark
import com.example.z_editor.ui.theme.PvzLightPurpleLight
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorContentWindowInsets
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

// 贴图目录：assets/images/griditems/
private const val RENAI_STATUE_ICON_DIR = "images/griditems/"

// 四种原石像共用这一张贴图（美术上就是同一块石头）
private const val RENAI_HALF_ICON_NAME = "renai_statue_zombie1_half"

private const val DAY_BADGE_COLOR = 0xFF1E88E5
private const val NIGHT_BADGE_COLOR = 0xFF8E24AA

private data class RenaiStatueType(
    val typeName: String,
    val displayName: String,
    val shortLabel: String,
    val color: Long,
    /** assets/images/griditems/ 下的文件名（不含扩展名），默认与 TypeName 同名 */
    val iconName: String = typeName
)

// 本页专用雕像库：刻意不进 GridItemRepository，「预置障碍物」页不受影响
private val RENAI_STATUES = listOf(
    RenaiStatueType("renai_statue_zombie1", "复兴贵族僵尸雕像", "贵族", 0xFF8D6E63),
    RenaiStatueType("renai_statue_zombie_armor1", "复兴贵族路障雕像", "路障", 0xFF6D4C41),
    RenaiStatueType("renai_statue_zombie_armor2", "复兴贵族铁桶雕像", "铁桶", 0xFF546E7A),
    RenaiStatueType("renai_statue_zombie_carver", "复兴雕刻家小鬼雕像", "雕刻", 0xFF7E57C2),
    RenaiStatueType("renai_statue_zombie_perfumer", "复兴调香师雕像", "调香", 0xFFAB47BC),
    RenaiStatueType(
        "renai_statue_zombie1_half", "复兴原石像(召唤贵族僵尸)", "唤贵", 0xFFA1887F,
        iconName = RENAI_HALF_ICON_NAME
    ),
    RenaiStatueType(
        "renai_statue_zombie_armor1_half", "复兴原石像(召唤贵族路障)", "唤障", 0xFF8D6E63,
        iconName = RENAI_HALF_ICON_NAME
    ),
    RenaiStatueType(
        "renai_statue_zombie_armor2_half", "复兴原石像(召唤贵族铁桶)", "唤桶", 0xFF78909C,
        iconName = RENAI_HALF_ICON_NAME
    ),
    RenaiStatueType(
        "renai_statue_zombie_perfumer_half", "复兴原石像(召唤调香师)", "唤香", 0xFFBA68C8,
        iconName = RENAI_HALF_ICON_NAME
    )
)

// 手工写的文件可能带别的 TypeName，这里回退成灰色并原样显示，避免选择弹窗里选不到
private fun renaiStatueType(typeName: String): RenaiStatueType =
    RENAI_STATUES.firstOrNull { it.typeName == typeName }
        ?: RenaiStatueType(typeName, typeName, typeName.takeLast(4), 0xFF9E9E9E)

private fun statueDisplayName(typeName: String): String = renaiStatueType(typeName).displayName

private enum class StatueTarget { Day, Night }

private val StatueTarget.label: String
    get() = if (this == StatueTarget.Day) "昼" else "夜"

private data class PendingAdd(
    val target: StatueTarget,
    val type: RenaiStatueType,
    val gridX: Int,
    val gridY: Int,
    val occupied: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenaiModuleEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    levelDef: LevelDefinitionData
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    val currentAlias = RtidParser.parse(rtid)?.alias ?: "RenaiModule"

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzLightPurpleDark else PvzLightPurpleLight

    // 冷缓存下 ReferenceRepository 还没加载完，不设这个门就会在首帧误闪一次"地图不匹配"
    var refReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        ReferenceRepository.init(context)
        refReady = true
    }

    val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
    val syncManager = rememberJsonSync(obj, RenaiModulePropertiesData::class.java)
    val moduleDataState = syncManager.dataState

    fun sync() {
        syncManager.sync()
    }

    val stageModuleInfo = remember(levelDef.stageModule) {
        RtidParser.parse(levelDef.stageModule)
    }
    val stageObjClass = remember(stageModuleInfo) {
        stageModuleInfo?.alias?.let { alias -> ReferenceRepository.getObjClass(alias) }
    }
    val isRenaiStage = stageObjClass == "RenaiStageProperties"

    // 三个键只要有一个在，就算"已开启"：手工写的、只带 StatueInfos 的文件若被判成关闭，
    // 之后任意一次 sync() 都会把该列表静默删掉
    val isNightEnabled = moduleDataState.value.nightStartWaveNum != null ||
            moduleDataState.value.statueInfos != null ||
            moduleDataState.value.statueNightInfos != null

    val dayList = moduleDataState.value.statueInfos.orEmpty()
    val nightList = moduleDataState.value.statueNightInfos.orEmpty()

    val daySorted = remember(moduleDataState.value.statueInfos) {
        moduleDataState.value.statueInfos.orEmpty()
            .sortedWith(compareBy({ it.gridY }, { it.gridX }))
    }
    val nightSorted = remember(moduleDataState.value.statueNightInfos) {
        moduleDataState.value.statueNightInfos.orEmpty()
            .sortedWith(compareBy({ it.gridY }, { it.gridX }))
    }

    // key = row * 9 + col。两张表合起来数，才能抓出跨表的坐标重复
    val cellCounts =
        remember(moduleDataState.value.statueInfos, moduleDataState.value.statueNightInfos) {
            val counts = HashMap<Int, Int>()
            fun bump(x: Int, y: Int) {
                val key = y * 9 + x
                counts[key] = (counts[key] ?: 0) + 1
            }
            moduleDataState.value.statueInfos.orEmpty().forEach { bump(it.gridX, it.gridY) }
            moduleDataState.value.statueNightInfos.orEmpty().forEach { bump(it.gridX, it.gridY) }
            counts
        }
    val duplicatedCells = remember(cellCounts) { cellCounts.filterValues { it > 1 }.keys }

    // 已有雕像里的非标准 TypeName 也放进弹窗选项，免得手工写的类型被静默改写
    val typeOptions =
        remember(moduleDataState.value.statueInfos, moduleDataState.value.statueNightInfos) {
            val known = RENAI_STATUES.map { it.typeName }.toSet()
            val unknown =
                (moduleDataState.value.statueInfos.orEmpty() + moduleDataState.value.statueNightInfos.orEmpty())
                    .map { it.typeName }
                    .filter { it !in known }
                    .distinct()
            RENAI_STATUES + unknown.map { renaiStatueType(it) }
        }

    var selectedX by remember { mutableIntStateOf(0) }
    var selectedY by remember { mutableIntStateOf(0) }
    var addTarget by remember { mutableStateOf(StatueTarget.Day) }

    var showAddPicker by remember { mutableStateOf(false) }
    var showDisableConfirm by remember { mutableStateOf(false) }
    var pendingAdd by remember { mutableStateOf<PendingAdd?>(null) }
    var typeChangeTarget by remember { mutableStateOf<RenaiStatueData?>(null) }
    var typeChangeHolder by remember { mutableStateOf<StatueTarget?>(null) }
    var itemToDelete by remember { mutableStateOf<Pair<StatueTarget, RenaiStatueData>?>(null) }

    fun currentList(target: StatueTarget): List<RenaiStatueData> =
        if (target == StatueTarget.Day) moduleDataState.value.statueInfos.orEmpty()
        else moduleDataState.value.statueNightInfos.orEmpty()

    // 增删改的唯一出口：开启状态下顺手补齐另外两个键，避免删掉最后一个雕像就把键写没了
    fun writeList(target: StatueTarget, newList: List<RenaiStatueData>) {
        val current = moduleDataState.value
        moduleDataState.value = current.copy(
            nightStartWaveNum = current.nightStartWaveNum ?: 1,
            statueInfos = if (target == StatueTarget.Day) newList.toMutableList()
            else current.statueInfos ?: mutableListOf(),
            statueNightInfos = if (target == StatueTarget.Night) newList.toMutableList()
            else current.statueNightInfos ?: mutableListOf()
        )
        sync()
    }

    fun addStatue(target: StatueTarget, typeName: String, gridX: Int, gridY: Int) {
        val newList = currentList(target).toMutableList()
        newList.add(
            RenaiStatueData(gridX = gridX, gridY = gridY, waveNumber = 1, typeName = typeName)
        )
        writeList(target, newList)
    }

    // 同一格上的多条记录可能结构相等，必须按引用删除
    fun deleteStatue(target: StatueTarget, targetItem: RenaiStatueData) {
        val newList = currentList(target).toMutableList()
        val index = newList.indexOfFirst { it === targetItem }
        if (index != -1) {
            newList.removeAt(index)
            writeList(target, newList)
        }
    }

    fun updateWave(target: StatueTarget, targetItem: RenaiStatueData, wave: Int) {
        val newList = currentList(target).toMutableList()
        val index = newList.indexOfFirst { it === targetItem }
        if (index != -1) {
            newList[index] = targetItem.copy(waveNumber = wave)
            writeList(target, newList)
        }
    }

    fun updateType(target: StatueTarget, targetItem: RenaiStatueData, typeName: String) {
        val newList = currentList(target).toMutableList()
        val index = newList.indexOfFirst { it === targetItem }
        if (index != -1) {
            newList[index] = targetItem.copy(typeName = typeName)
            writeList(target, newList)
        }
    }

    // === 弹窗：移除确认 ===
    val deleteRef = itemToDelete
    if (deleteRef != null) {
        val deleteTarget = deleteRef.first
        val deleteItem = deleteRef.second
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("移除雕像", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "确定要移除 R${deleteItem.gridY + 1}:C${deleteItem.gridX + 1} 的" +
                            "${statueDisplayName(deleteItem.typeName)}（第 ${deleteItem.waveNumber} 波复活）吗？"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteStatue(deleteTarget, deleteItem)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onError)
                ) { Text("移除") }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("取消") }
            }
        )
    }

    // === 弹窗：种类选择（添加） ===
    if (showAddPicker) {
        StatueTypePickerDialog(
            title = "选择雕像种类",
            options = typeOptions,
            onDismiss = { showAddPicker = false },
            onPick = { typeName ->
                showAddPicker = false
                val occupied = cellCounts[selectedY * 9 + selectedX] ?: 0
                val pending = PendingAdd(
                    target = addTarget,
                    type = renaiStatueType(typeName),
                    gridX = selectedX,
                    gridY = selectedY,
                    occupied = occupied
                )
                if (occupied > 0) pendingAdd = pending
                else addStatue(pending.target, pending.type.typeName, pending.gridX, pending.gridY)
            }
        )
    }

    // === 弹窗：坐标重复确认 ===
    val pending = pendingAdd
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { pendingAdd = null },
            title = { Text("坐标重复", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "R${pending.gridY + 1}:C${pending.gridX + 1} 已经有 ${pending.occupied} 尊雕像了，" +
                            "坐标原则上不应重复。确定要再放一尊${pending.type.displayName}吗？"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        addStatue(
                            pending.target,
                            pending.type.typeName,
                            pending.gridX,
                            pending.gridY
                        )
                        pendingAdd = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onError)
                ) { Text("继续添加") }
            },
            dismissButton = {
                TextButton(onClick = { pendingAdd = null }) { Text("取消") }
            }
        )
    }

    // === 弹窗：种类选择（更换） ===
    val changeTarget = typeChangeTarget
    if (changeTarget != null) {
        StatueTypePickerDialog(
            title = "更换雕像种类",
            options = typeOptions,
            onDismiss = {
                typeChangeTarget = null
                typeChangeHolder = null
            },
            onPick = { typeName ->
                typeChangeHolder?.let { holder -> updateType(holder, changeTarget, typeName) }
                typeChangeTarget = null
                typeChangeHolder = null
            }
        )
    }

    // === 弹窗：关闭昼夜更替的二次确认 ===
    if (showDisableConfirm) {
        val statueTotal = dayList.size + nightList.size
        AlertDialog(
            onDismissRequest = { showDisableConfirm = false },
            title = { Text("关闭昼夜更替", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (statueTotal > 0)
                        "关闭后会清除已写入的起始波次，以及已放置的 $statueTotal 尊雕像" +
                                "（昼 ${dayList.size} 尊、夜 ${nightList.size} 尊），此操作无法撤销。确定继续吗？"
                    else
                        "关闭后会清除已写入的昼夜更替参数，此操作无法撤销。确定继续吗？"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 唯一会写 null 的地方：三个键一起清掉，objdata 回到 {}
                        moduleDataState.value = moduleDataState.value.copy(
                            nightStartWaveNum = null,
                            statueInfos = null,
                            statueNightInfos = null
                        )
                        sync()
                        showDisableConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onError)
                ) { Text("关闭并清除") }
            },
            dismissButton = {
                TextButton(onClick = { showDisableConfirm = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        contentWindowInsets = EditorContentWindowInsets(),
        topBar = {
            CommonEditorTopAppBar(
                title = "复兴雕像",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "复兴雕像模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "用于复兴地图的昼夜更替与雕像玩法：到达指定波次后地图在白天与黑夜之间切换，并在指定格点生成雕像，雕像被破坏后会在设定的波次复活。"
                )
                HelpSection(
                    title = "开关",
                    body = "关闭开关时不写入任何参数，仅作为圆环工作的依赖项，游戏内不进行昼夜更替；打开后才会写入起始波次与两张雕像表。"
                )
                HelpSection(
                    title = "雕像表",
                    body = "昼间雕像在关卡开始时就在场上，夜间雕像在昼夜更替开始后出现。同一个格点的坐标原则上不要重复，重复时格子会标红提醒。卡片上的图标就是雕像本身，点击即可更换种类。"
                )
                HelpSection(
                    title = "复活波次",
                    body = "每尊雕像单独设置复活波次，表示开战后经过多少波复活。"
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // === 区域 0: 地图类型警告 ===
            if (refReady && !isRenaiStage) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MapMismatchCard()
                }
            }

            // === 区域 1: 昼夜更替总开关 ===
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "启用昼夜更替",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = themeColor
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isNightEnabled)
                                    "随波次进行昼夜更替，需要复兴地图"
                                else
                                    "游戏内不进行昼夜更替",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = isNightEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    moduleDataState.value = moduleDataState.value.copy(
                                        nightStartWaveNum = moduleDataState.value.nightStartWaveNum
                                            ?: 1,
                                        statueInfos = moduleDataState.value.statueInfos
                                            ?: mutableListOf(),
                                        statueNightInfos = moduleDataState.value.statueNightInfos
                                            ?: mutableListOf()
                                    )
                                    sync()
                                } else {
                                    // 关掉会连已放的雕像一起清掉，先弹窗二次确认，确认前不动数据
                                    showDisableConfirm = true
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = themeColor,
                                checkedBorderColor = Color.Transparent,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            if (!isNightEnabled) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Info, null, tint = themeColor)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "关卡中的圆环必须添加此模块才能正常生效。",
                                    fontSize = 12.sp,
                                    color = themeColor,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            if (isNightEnabled) {
                // === 区域 2: 起始波次 ===
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "昼夜更替",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = themeColor
                            )
                            NumberInputInt(
                                value = moduleDataState.value.nightStartWaveNum ?: 1,
                                onValueChange = {
                                    moduleDataState.value =
                                        moduleDataState.value.copy(nightStartWaveNum = it)
                                    sync()
                                },
                                label = "开始昼夜更替的波次",
                                color = themeColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "到达该波次后地图开始在白天与黑夜之间切换。",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // === 区域 3: 共享网格选择器 ===
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(contentAlignment = Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.widthIn(max = 480.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(
                                            "选中位置",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "R${selectedY + 1} : C${selectedX + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = themeColor
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Button(
                                        onClick = { showAddPicker = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("添加雕像")
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "添加到",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    StatueTarget.values().forEach { target ->
                                        FilterChip(
                                            selected = addTarget == target,
                                            onClick = { addTarget = target },
                                            label = { Text(target.label, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = themeColor,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1.8f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isDark) Color(0xFF31383B) else Color(
                                                0xFFD7ECF1
                                            )
                                        )
                                        .border(1.dp, Color(0xFF6B899A), RoundedCornerShape(6.dp))
                                ) {
                                    Column(Modifier.fillMaxSize()) {
                                        for (row in 0..4) {
                                            Row(Modifier.weight(1f)) {
                                                for (col in 0..8) {
                                                    val isSelected =
                                                        (row == selectedY && col == selectedX)
                                                    val dayCount = dayList.count {
                                                        it.gridX == col && it.gridY == row
                                                    }
                                                    val nightCount = nightList.count {
                                                        it.gridX == col && it.gridY == row
                                                    }
                                                    val total = dayCount + nightCount
                                                    val isDuplicate = total > 1
                                                    val iconType =
                                                        dayList.firstOrNull {
                                                            it.gridX == col && it.gridY == row
                                                        }?.typeName
                                                            ?: nightList.firstOrNull {
                                                                it.gridX == col && it.gridY == row
                                                            }?.typeName

                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .fillMaxHeight()
                                                            .border(
                                                                width = if (isSelected || isDuplicate) 1.dp else 0.5.dp,
                                                                color = when {
                                                                    isSelected -> themeColor
                                                                    isDuplicate -> MaterialTheme.colorScheme.error
                                                                    else -> Color(0xFF6B899A)
                                                                }
                                                            )
                                                            .background(
                                                                if (isSelected) PvzGridHighLight
                                                                else Color.Transparent
                                                            )
                                                            .clickable {
                                                                selectedX = col
                                                                selectedY = row
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (total > 0 && iconType != null) {
                                                            RenaiStatueIcon(
                                                                typeName = iconType,
                                                                modifier = Modifier.fillMaxSize(
                                                                    0.86f
                                                                ),
                                                                cornerRadius = 4.dp,
                                                                fontSize = 9.sp
                                                            )
                                                        }
                                                        if (dayCount > 0) {
                                                            CellBadge(
                                                                text = "昼",
                                                                background = Color(DAY_BADGE_COLOR),
                                                                modifier = Modifier.align(Alignment.TopStart)
                                                            )
                                                        }
                                                        if (nightCount > 0) {
                                                            CellBadge(
                                                                text = "夜",
                                                                background = Color(NIGHT_BADGE_COLOR),
                                                                modifier = Modifier.align(Alignment.TopEnd)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (duplicatedCells.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    val cellsText = duplicatedCells
                                        .sortedWith(compareBy({ it / 9 }, { it % 9 }))
                                        .joinToString("、") { "R${it / 9 + 1}:C${it % 9 + 1}" }
                                    Text(
                                        text = "存在坐标重复：$cellsText（建议每个格点只放一尊雕像）",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onError,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // === 区域 4: 说明 ===
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Info, null, tint = themeColor)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "标「昼」的是昼间雕像（关卡开始就在场上），标「夜」的是夜间雕像（昼夜更替开始后出现）。点击卡片上的图标可以更换雕像种类。",
                                    fontSize = 12.sp,
                                    color = themeColor,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // === 区域 5: 昼间雕像列表 ===
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle("昼间雕像 (${daySorted.size})")
                }
                if (daySorted.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyHint("还没有昼间雕像。在上面的网格里选好位置，再点「添加雕像」。")
                    }
                } else {
                    items(daySorted) { item ->
                        StatueCard(
                            item = item,
                            isSelected = (item.gridX == selectedX && item.gridY == selectedY),
                            isDuplicate = (cellCounts[item.gridY * 9 + item.gridX] ?: 0) > 1,
                            isDark = isDark,
                            themeColor = themeColor,
                            onClick = {
                                selectedX = item.gridX
                                selectedY = item.gridY
                            },
                            onWaveChange = { wave -> updateWave(StatueTarget.Day, item, wave) },
                            onChangeType = {
                                typeChangeHolder = StatueTarget.Day
                                typeChangeTarget = item
                            },
                            onDelete = { itemToDelete = StatueTarget.Day to item }
                        )
                    }
                }

                // === 区域 6: 夜间雕像列表 ===
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle("夜间雕像 (${nightSorted.size})")
                }
                if (nightSorted.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyHint("还没有夜间雕像。把「添加到」切到夜再放置即可。")
                    }
                } else {
                    items(nightSorted) { item ->
                        StatueCard(
                            item = item,
                            isSelected = (item.gridX == selectedX && item.gridY == selectedY),
                            isDuplicate = (cellCounts[item.gridY * 9 + item.gridX] ?: 0) > 1,
                            isDark = isDark,
                            themeColor = themeColor,
                            onClick = {
                                selectedX = item.gridX
                                selectedY = item.gridY
                            },
                            onWaveChange = { wave -> updateWave(StatueTarget.Night, item, wave) },
                            onChangeType = {
                                typeChangeHolder = StatueTarget.Night
                                typeChangeTarget = item
                            },
                            onDelete = { itemToDelete = StatueTarget.Night to item }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(vertical = 8.dp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 14.sp
    )
}

@Composable
private fun EmptyHint(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun MapMismatchCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Red, RoundedCornerShape(8.dp)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                null,
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "地图类型不匹配",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "当前地图并非复兴地图，此模块在游戏中可能无法生效，甚至导致闪退",
                    color = MaterialTheme.colorScheme.onError,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun StatueCard(
    item: RenaiStatueData,
    isSelected: Boolean,
    isDuplicate: Boolean,
    isDark: Boolean,
    themeColor: Color,
    onClick: () -> Unit,
    onWaveChange: (Int) -> Unit,
    onChangeType: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) if (isDark) Color(0xFF31383B) else Color(0xFFD7ECF1)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(1.dp, themeColor) else null,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 图标即类型入口：点它换种类
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onChangeType)
                ) {
                    RenaiStatueIcon(
                        typeName = item.typeName,
                        modifier = Modifier.fillMaxSize(),
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "R${item.gridY + 1}:C${item.gridX + 1}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    if (isDuplicate) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "重复",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.onError)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "移除雕像",
                        tint = MaterialTheme.colorScheme.onError.copy(0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            NumberInputInt(
                value = item.waveNumber,
                onValueChange = onWaveChange,
                label = "复活波次",
                color = themeColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatueTypePickerDialog(
    title: String,
    options: List<RenaiStatueType>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPick(option.typeName) }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RenaiStatueIcon(
                            typeName = option.typeName,
                            modifier = Modifier.size(36.dp),
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = option.displayName,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = option.typeName,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/**
 * 雕像图标。贴图取自 assets/images/griditems/<iconName>.webp（四种原石像共用一张）；
 * 遇到手工写进来的未知 TypeName、或贴图缺失时才回落到带缩写的色块。
 */
@Composable
private fun RenaiStatueIcon(
    typeName: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 6.dp,
    fontSize: TextUnit = 11.sp
) {
    val type = renaiStatueType(typeName)
    AssetImage(
        path = RENAI_STATUE_ICON_DIR + type.iconName + ".webp",
        contentDescription = type.displayName,
        modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.Medium,
        placeholder = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color(type.color)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = type.shortLabel,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    )
}

@Composable
private fun BoxScope.CellBadge(text: String, background: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        maxLines = 1,
        modifier = modifier
            .padding(1.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(background)
            .padding(horizontal = 3.dp, vertical = 1.dp)
    )
}
