package com.example.pvz2leveleditor.views.editor.pages.module

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pvz2leveleditor.data.PvzLevelFile
import com.example.pvz2leveleditor.data.PvzObject
import com.example.pvz2leveleditor.data.repository.ChallengeRepository
import com.example.pvz2leveleditor.data.repository.ReferenceRepository
import com.example.pvz2leveleditor.data.RtidParser
import com.example.pvz2leveleditor.data.StarChallengeBeatTheLevelData
import com.example.pvz2leveleditor.data.StarChallengeBlowZombieData
import com.example.pvz2leveleditor.data.StarChallengeKillZombiesInTimeData
import com.example.pvz2leveleditor.data.StarChallengeModuleData
import com.example.pvz2leveleditor.data.StarChallengePlantSurviveData
import com.example.pvz2leveleditor.data.StarChallengePlantsLostData
import com.example.pvz2leveleditor.data.StarChallengeSimultaneousPlantsData
import com.example.pvz2leveleditor.data.StarChallengeSpendSunHoldoutData
import com.example.pvz2leveleditor.data.StarChallengeSunProducedData
import com.example.pvz2leveleditor.data.StarChallengeSunReducedData
import com.example.pvz2leveleditor.data.StarChallengeSunUsedData
import com.example.pvz2leveleditor.data.StarChallengeUnfreezePlantsData
import com.example.pvz2leveleditor.data.StarChallengeZombieDistanceData
import com.example.pvz2leveleditor.data.StarChallengeZombieSpeedData
import com.example.pvz2leveleditor.views.editor.EditorHelpDialog
import com.example.pvz2leveleditor.views.editor.HelpSection
import com.example.pvz2leveleditor.views.editor.NumberInputInt
import com.google.gson.Gson

private val gson = Gson()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarChallengeModulePropertiesEP(
    rtid: String,
    rootLevelFile: PvzLevelFile,
    objectMap: Map<String, PvzObject>,
    onBack: () -> Unit,
    onNavigateToAddChallenge: () -> Unit,
    scrollState: ScrollState
) {
    val context = LocalContext.current
    val currentAlias = RtidParser.parse(rtid)?.alias ?: "ChallengeModule"
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }

    // === 1. 数据状态 ===
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val challengeDataState = remember {
        val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        val data = try {
            gson.fromJson(obj?.objData, StarChallengeModuleData::class.java)
        } catch (_: Exception) {
            StarChallengeModuleData()
        }
        if (data.challenges.isEmpty()) {
            data.challenges.add(mutableListOf())
        }

        mutableStateOf(data)
    }

    // === 2. 弹窗编辑状态 ===
    var editingChallenge by remember { mutableStateOf<Pair<Int, String>?>(null) }

    fun syncMainModule() {
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }?.let {
            it.objData = gson.toJsonTree(challengeDataState.value)
        }
    }

    fun handleDeleteChallenge(index: Int, challengeRtid: String) {
        val innerList = challengeDataState.value.challenges.getOrNull(0)

        if (innerList != null && index in innerList.indices) {
            innerList.removeAt(index)
            syncMainModule()

            val rtidInfo = RtidParser.parse(challengeRtid)
            if (rtidInfo?.source == "CurrentLevel") {
                val alias = rtidInfo.alias
                val removed =
                    rootLevelFile.objects.removeAll { it.aliases?.contains(alias) == true }
                if (removed) {
                    Toast.makeText(context, "已删除本地对象 $alias", Toast.LENGTH_SHORT).show()
                }
            }
            refreshTrigger++
        }
    }

    // === 3. 处理子对象保存 ===
    fun handleSaveSubObject(originalRtid: String, newData: Any) {
        val info = RtidParser.parse(originalRtid)
        val alias = info?.alias ?: return

        val targetObj = rootLevelFile.objects.find { it.aliases?.contains(alias) == true }

        if (targetObj != null) {
            targetObj.objData = gson.toJsonTree(newData)
            Toast.makeText(context, "参数已更新", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "错误：找不到本地对象 $alias", Toast.LENGTH_SHORT).show()
        }
        editingChallenge = null
    }

    // === 4. 弹窗组件 ===
    if (editingChallenge != null) {
        val (index, rtidStr) = editingChallenge!!
        ChallengeEditDialog(
            rtid = rtidStr,
            rootLevelFile = rootLevelFile,
            onDismiss = { editingChallenge = null },
            onSave = { newData -> handleSaveSubObject(rtidStr, newData) }
        )
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text("挑战模块配置", fontWeight = FontWeight.Bold) },
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
                    containerColor = Color(0xFFE8A000),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "挑战模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = Color(0xFFE8A000)
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "这里可用选择关卡使用的各项挑战模块。可以同时设置多项挑战目标以及使用多次同种挑战。"
                )
                HelpSection(
                    title = "优化建议",
                    body = "部分挑战在游戏内有统计框记录进度，当挑战模块过多时统计数据框可能会被遮挡。"
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                key(refreshTrigger) {
                    val activeList = challengeDataState.value.challenges.firstOrNull() ?: emptyList()
                    if (activeList.isEmpty()) {
                        Text("暂无挑战", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                    } else {
                        activeList.forEachIndexed { index, challengeRtid ->
                            ChallengeItemCard(
                                rtid = challengeRtid,
                                objectMap = objectMap,
                                onClick = { editingChallenge = index to challengeRtid },
                                onDelete = { handleDeleteChallenge(index, challengeRtid) }
                            )
                        }
                    }
                }

                AddChallengeButton(onClick = onNavigateToAddChallenge)

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F0E5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Info, null, tint = Color(0xFFE8A000))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "同类型的挑战可设置多个，注意模块过多时挑战的统计数据框可能会被遮挡。",
                                fontSize = 12.sp,
                                color = Color(0xFFE8A000),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// === 列表项卡片 (更新了Icon逻辑) ===
@Composable
fun ChallengeItemCard(
    rtid: String,
    objectMap: Map<String, PvzObject>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val info = RtidParser.parse(rtid)
    val alias = info?.alias ?: rtid
    val source = info?.source ?: "Unknown"

    // 尝试从 objectMap 查找
    val rawObjClass = if (source == "CurrentLevel") {
        objectMap[alias]?.objClass
    } else {
        ReferenceRepository.getObjClass(alias)
    }

    val isMissing = rawObjClass == null
    val objClass = rawObjClass ?: "Unknown Object"

    val displayName = getChallengeDisplayName(objClass)
    val isEditable = source == "CurrentLevel" && !isMissing

    // 视觉状态区分
    val themeColor = if (isMissing) Color.Red else Color(0xFFE8A000)
    val containerColor = if (isMissing) Color(0xFFFFEBEE) else Color.White // 错误时浅红背景

    // 图标区分
    val challengeInfo = ChallengeRepository.getInfo(objClass)
    val icon = if (isMissing) Icons.Default.BrokenImage else (challengeInfo?.icon ?: Icons.Default.Extension)

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(2.dp),
        border = if (isMissing) androidx.compose.foundation.BorderStroke(1.dp, Color.Red) else null, // 错误时加红框
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(themeColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = themeColor)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMissing) "引用失效: $alias" else displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isMissing) Color.Red.copy(0.7f) else Color.Black
                )
                Text(
                    text = if (isMissing) "找不到本地对象 (Object Missing)" else alias,
                    fontSize = 12.sp,
                    color = if (isMissing) Color.Red.copy(0.7f) else Color.Gray
                )
                if (!isEditable && !isMissing) {
                    Text(text = "引用对象 (只读)", fontSize = 10.sp, color = Color.Red.copy(0.7f))
                }
            }

            Icon(Icons.Default.Edit, "编辑", tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, "移除", tint = if (isMissing) Color.Red else Color.Red.copy(alpha = 0.6f))
            }
        }
    }
}

// === 新增：底部横向添加按钮 ===
@Composable
fun AddChallengeButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, Color(0xFFE8A000), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AddCircleOutline, null, tint = Color(0xFFE8A000))
            Spacer(Modifier.width(8.dp))
            Text("添加新挑战", color = Color(0xFFE8A000), fontWeight = FontWeight.Bold)
        }
    }
}

// === 核心：挑战参数编辑弹窗 (保持不变) ===
@Composable
fun ChallengeEditDialog(
    rtid: String,
    rootLevelFile: PvzLevelFile,
    onDismiss: () -> Unit,
    onSave: (Any) -> Unit
) {
    val info = RtidParser.parse(rtid) ?: return
    val alias = info.alias

    val obj = rootLevelFile.objects.find { it.aliases?.contains(alias) == true }

    if (obj == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("错误", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth()) },
            text = { Text("找不到本地对象: $alias \n\n该挑战引用的实体数据在关卡文件中不存在，这可能是因为手动修改了JSON，请直接点击列表右侧的删除按钮移除它", fontSize = 14.sp, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
        )
        return
    }

    when (obj.objClass) {
        "StarChallengeSaveMowerProps" -> {
            InfoOnlyDialog(
                title = "不丢车挑战",
                message = "该挑战没有可配置的参数\n\n通关时所有小推车必须完好无损，注意庭院模块下默认没有小推车",
                onDismiss = onDismiss
            )
        }

        "StarChallengePlantFoodNonuseProps" -> {
            InfoOnlyDialog(
                title = "禁用能量豆挑战",
                message = "该挑战没有可配置的参数\n\n禁止玩家使用能量豆",
                onDismiss = onDismiss
            )
        }

        "StarChallengeBeatTheLevelProps" -> {
            BeatTheLevelEditDialog(
                initialData = gson.fromJson(
                    obj.objData,
                    StarChallengeBeatTheLevelData::class.java
                ),
                onDismiss = onDismiss,
                onConfirm = onSave
            )
        }

        "StarChallengePlantSurviveProps" -> {
            PlantSurviveEditDialog(
                initialData = gson.fromJson(obj.objData, StarChallengePlantSurviveData::class.java),
                onDismiss = onDismiss,
                onConfirm = onSave
            )
        }

        "StarChallengeZombieDistanceProps" -> {
            ZombieDistanceEditDialog(
                initialData = gson.fromJson(
                    obj.objData,
                    StarChallengeZombieDistanceData::class.java
                ),
                onDismiss = onDismiss,
                onConfirm = onSave
            )
        }

        "StarChallengeSunProducedProps" -> {
            SunProducedEditDialog(
                initialData = gson.fromJson(
                    obj.objData,
                    StarChallengeSunProducedData::class.java
                ),
                onDismiss = onDismiss,
                onConfirm = onSave
            )
        }

        "StarChallengeSunUsedProps" -> {
            SunUsedEditDialog(
                initialData = gson.fromJson(
                    obj.objData,
                    StarChallengeSunUsedData::class.java
                ),
                onDismiss = onDismiss,
                onConfirm = onSave
            )
        }

        "StarChallengeSpendSunHoldoutProps" -> {
            SpendSunHoldoutEditDialog(
                initialData = gson.fromJson(
                    obj.objData,
                    StarChallengeSpendSunHoldoutData::class.java
                ),
                onDismiss = onDismiss,
                onConfirm = onSave
            )
        }

        "StarChallengeKillZombiesInTimeProps" -> {
            KillZombiesEditDialog(
                initialData = gson.fromJson(
                    obj.objData,
                    StarChallengeKillZombiesInTimeData::class.java
                ),
                onDismiss = onDismiss,
                onConfirm = onSave
            )
        }

        "StarChallengeZombieSpeedProps" -> {
            ZombieSpeedEditDialog(
                initialData = gson.fromJson(
                    obj.objData,
                    StarChallengeZombieSpeedData::class.java
                ),
                onDismiss = onDismiss,
                onConfirm = onSave
            )
        }

        "StarChallengeSunReducedProps" -> {
            SunReducedEditDialog(
                initialData = gson.fromJson(
                    obj.objData,
                    StarChallengeSunReducedData::class.java
                ),
                onDismiss = onDismiss,
                onConfirm = onSave
            )
        }

        "StarChallengePlantsLostProps" -> {
            PlantsLostEditDialog(
                initialData = gson.fromJson(
                    obj.objData,
                    StarChallengePlantsLostData::class.java
                ),
                onDismiss = onDismiss,
                onConfirm = onSave
            )
        }

        "StarChallengeSimultaneousPlantsProps" -> {
            SimultaneousPlantsEditDialog(
                initialData = gson.fromJson(
                    obj.objData,
                    StarChallengeSimultaneousPlantsData::class.java
                ),
                onDismiss = onDismiss,
                onConfirm = onSave
            )
        }

        "StarChallengeUnfreezePlantsProps" -> {
            UnfreezePlantsEditDialog(
                initialData = gson.fromJson(
                    obj.objData,
                    StarChallengeUnfreezePlantsData::class.java
                ),
                onDismiss = onDismiss,
                onConfirm = onSave
            )
        }

        "StarChallengeBlowZombieProps" -> {
            BlowZombieEditDialog(
                initialData = gson.fromJson(
                    obj.objData,
                    StarChallengeBlowZombieData::class.java
                ),
                onDismiss = onDismiss,
                onConfirm = onSave
            )
        }

        else -> {
            InfoOnlyDialog(
                title = "未知挑战类型",
                message = "编辑器暂不支持修改此类型模块的参数",
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
fun InfoOnlyDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                message,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8A000))
            ) {
                Text("确定")
            }
        }
    )
}

@Composable
fun PlantSurviveEditDialog(
    initialData: StarChallengePlantSurviveData,
    onDismiss: () -> Unit,
    onConfirm: (StarChallengePlantSurviveData) -> Unit
) {
    var count by remember { mutableIntStateOf(initialData.count) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "幸存植物挑战",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text("需要指定数量的植物在游戏结束时存活", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                NumberInputInt(
                    value = count,
                    onValueChange = { count = it },
                    label = "存活数量 (Count)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(initialData.copy(count = count))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8A000))
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun ZombieDistanceEditDialog(
    initialData: StarChallengeZombieDistanceData,
    onDismiss: () -> Unit,
    onConfirm: (StarChallengeZombieDistanceData) -> Unit
) {
    var distance by remember { mutableDoubleStateOf(initialData.targetDistance) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "花坛线挑战",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text(
                    text = "不能让僵尸踩踏到花坛线",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(16.dp))
                com.example.pvz2leveleditor.views.editor.NumberInputDouble(
                    value = distance,
                    onValueChange = { distance = it },
                    label = "花坛距离 (TargetDistance)",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "数值代表从左边线起列数，数值越大离房屋越远，可输入小数",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(initialData.copy(targetDistance = distance)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8A000))
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}


@Composable
fun BeatTheLevelEditDialog(
    initialData: StarChallengeBeatTheLevelData,
    onDismiss: () -> Unit,
    onConfirm: (StarChallengeBeatTheLevelData) -> Unit
) {
    var description by remember { mutableStateOf(initialData.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "关卡提示文字",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text(
                    text = "在关卡开头用弹窗显示提示文字",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("文字显示 (Description)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    singleLine = false,
                    maxLines = 10,
                    minLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Default
                    )
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "支持显示中文，多行文字需直接输入回车，无需使用转义序列\\n，注意ios端庭院内无法查看提示内容",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(initialData.copy(description = description)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8A000))
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun SunProducedEditDialog(
    initialData: StarChallengeSunProducedData,
    onDismiss: () -> Unit,
    onConfirm: (StarChallengeSunProducedData) -> Unit
) {
    var targetSun by remember { mutableIntStateOf(initialData.targetSun) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "生产阳光挑战",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text("关卡结束前生产一定数量阳光", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                NumberInputInt(
                    value = targetSun,
                    onValueChange = { targetSun = it },
                    label = "目标阳光 (TargetSun)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(initialData.copy(targetSun = targetSun))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8A000))
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun SunUsedEditDialog(
    initialData: StarChallengeSunUsedData,
    onDismiss: () -> Unit,
    onConfirm: (StarChallengeSunUsedData) -> Unit
) {
    var maximumSun by remember { mutableIntStateOf(initialData.maximumSun) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "阳光限额挑战",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text("关卡中阳光的限额使用", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                NumberInputInt(
                    value = maximumSun,
                    onValueChange = { maximumSun = it },
                    label = "阳光限额 (MaximumSun)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(initialData.copy(maximumSun = maximumSun))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8A000))
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun SpendSunHoldoutEditDialog(
    initialData: StarChallengeSpendSunHoldoutData,
    onDismiss: () -> Unit,
    onConfirm: (StarChallengeSpendSunHoldoutData) -> Unit
) {
    var holdoutSeconds by remember { mutableIntStateOf(initialData.holdoutSeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "保持阳光挑战",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text("保持一段时间不使用阳光", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                NumberInputInt(
                    value = holdoutSeconds,
                    onValueChange = { holdoutSeconds = it },
                    label = "保持时间 (HoldoutSeconds)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(initialData.copy(holdoutSeconds = holdoutSeconds))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8A000))
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun KillZombiesEditDialog(
    initialData: StarChallengeKillZombiesInTimeData,
    onDismiss: () -> Unit,
    onConfirm: (StarChallengeKillZombiesInTimeData) -> Unit
) {
    var zombiesToKill by remember { mutableIntStateOf(initialData.zombiesToKill) }
    var time by remember { mutableIntStateOf(initialData.time) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "消灭僵尸挑战",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text("在一定时间内消灭指定数量僵尸", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                NumberInputInt(
                    value = zombiesToKill,
                    onValueChange = { zombiesToKill = it },
                    label = "击杀个数 (ZombiesToKill)",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                NumberInputInt(
                    value = time,
                    onValueChange = { time = it },
                    label = "时间限制 (Time)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(initialData.copy(zombiesToKill = zombiesToKill, time = time))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8A000))
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun ZombieSpeedEditDialog(
    initialData: StarChallengeZombieSpeedData,
    onDismiss: () -> Unit,
    onConfirm: (StarChallengeZombieSpeedData) -> Unit
) {
    var speedModifier by remember { mutableDoubleStateOf(initialData.speedModifier) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "僵尸提速挑战",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text(
                    text = "所有僵尸的速度获得一定的增幅",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(16.dp))
                com.example.pvz2leveleditor.views.editor.NumberInputDouble(
                    value = speedModifier,
                    onValueChange = { speedModifier = it },
                    label = "增幅倍率 (SpeedModifier)",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "填入0.5代表僵尸移速获得百分之50的增幅",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(initialData.copy(speedModifier = speedModifier)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8A000))
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun SunReducedEditDialog(
    initialData: StarChallengeSunReducedData,
    onDismiss: () -> Unit,
    onConfirm: (StarChallengeSunReducedData) -> Unit
) {
    var sunModifier by remember { mutableDoubleStateOf(initialData.sunModifier) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "阳光减收挑战",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text(
                    text = "获取的阳光会被按照一定比例减少",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(16.dp))
                com.example.pvz2leveleditor.views.editor.NumberInputDouble(
                    value = sunModifier,
                    onValueChange = { sunModifier = it },
                    label = "降低倍率 (SunModifier)",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "填入0.2表示阳光获取降低百分之20",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(initialData.copy(sunModifier = sunModifier)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8A000))
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun PlantsLostEditDialog(
    initialData: StarChallengePlantsLostData,
    onDismiss: () -> Unit,
    onConfirm: (StarChallengePlantsLostData) -> Unit
) {
    var maximumPlantsLost by remember { mutableIntStateOf(initialData.maximumPlantsLost) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "植物限损挑战",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text("损失的植物不能超过一定限额", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                NumberInputInt(
                    value = maximumPlantsLost,
                    onValueChange = { maximumPlantsLost = it },
                    label = "损失上限 (MaximumPlantsLost)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(initialData.copy(maximumPlantsLost = maximumPlantsLost))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8A000))
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun SimultaneousPlantsEditDialog(
    initialData: StarChallengeSimultaneousPlantsData,
    onDismiss: () -> Unit,
    onConfirm: (StarChallengeSimultaneousPlantsData) -> Unit
) {
    var maximumPlants by remember { mutableIntStateOf(initialData.maximumPlants) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "限制种植数挑战",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text("限制所有植物同时在场的数量", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                NumberInputInt(
                    value = maximumPlants,
                    onValueChange = { maximumPlants = it },
                    label = "种植上限 (MaximumPlants)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(initialData.copy(maximumPlants = maximumPlants))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8A000))
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun UnfreezePlantsEditDialog(
    initialData: StarChallengeUnfreezePlantsData,
    onDismiss: () -> Unit,
    onConfirm: (StarChallengeUnfreezePlantsData) -> Unit
) {
    var count by remember { mutableIntStateOf(initialData.count) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "解冻植物挑战",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text("解冻一定数量的植物", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                NumberInputInt(
                    value = count,
                    onValueChange = { count = it },
                    label = "解冻数量 (Count)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(initialData.copy(count = count))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8A000))
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun BlowZombieEditDialog(
    initialData: StarChallengeBlowZombieData,
    onDismiss: () -> Unit,
    onConfirm: (StarChallengeBlowZombieData) -> Unit
) {
    var count by remember { mutableIntStateOf(initialData.count) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "吹飞僵尸挑战",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text("吹飞一定数量的僵尸", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                NumberInputInt(
                    value = count,
                    onValueChange = { count = it },
                    label = "吹飞数量 (Count)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(initialData.copy(count = count))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8A000))
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}


private fun getChallengeDisplayName(objClass: String): String {
    return ChallengeRepository.getInfo(objClass)?.title
        ?: when (objClass) {
            "StarChallengeBeatTheLevelProps" -> "关卡提示文字"
            "StarChallengeSaveMowerProps" -> "不丢车挑战"
            "StarChallengePlantFoodNonuseProps" -> "禁用能量豆挑战"
            "StarChallengePlantSurviveProps" -> "幸存植物挑战"
            "StarChallengeZombieDistanceProps" -> "花坛线挑战"
            "StarChallengeSunProducedProps" -> "生产阳光挑战"
            "StarChallengeSunUsedProps" -> "阳光限额挑战"
            "StarChallengeSpendSunHoldoutProps" -> "保持阳光挑战"
            "StarChallengeKillZombiesInTimeProps" -> "消灭僵尸挑战"
            "StarChallengeZombieSpeedProps" -> "僵尸提速挑战"
            "StarChallengeSunReducedProps" -> "阳光减收挑战"
            "StarChallengePlantsLostProps" -> "植物限损挑战"
            "StarChallengeSimultaneousPlantsProps" -> "限制种植数挑战"
            "StarChallengeUnfreezePlantsProps" -> "解冻植物挑战"
            "StarChallengeBlowZombieProps" -> "吹飞僵尸挑战"
            else -> objClass.replace("StarChallenge", "").replace("Props", "")
        }
}