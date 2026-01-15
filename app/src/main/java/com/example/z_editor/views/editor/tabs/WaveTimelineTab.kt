package com.example.z_editor.views.editor.tabs


import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.EventRegistry
import com.example.z_editor.data.LevelParser
import com.example.z_editor.data.ParsedLevelData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.PvzObject
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.WaveManagerData
import com.example.z_editor.data.WaveManagerModuleData
import com.example.z_editor.data.WavePointAnalysis
import com.example.z_editor.data.repository.ZombiePropertiesRepository
import com.example.z_editor.data.repository.ZombieRepository
import com.example.z_editor.data.repository.ZombieTag
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.EventChip
import com.example.z_editor.views.editor.pages.others.SettingEntryCard

data class ZombieUsageInfo(
    val rtid: String,
    val alias: String,
    val baseTypeName: String,
    val locations: List<String>
) {
    val isUnused: Boolean get() = locations.isEmpty()
}

/**
 * 波次时间轴 Tab 页面内容
 * 交互：右滑管理，左滑删除，点数实时计算，引用校验
 */
@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaveTimelineTab(
    rootLevelFile: PvzLevelFile?,
    waveManager: WaveManagerData?,
    waveModule: WaveManagerModuleData?,
    objectMap: Map<String, PvzObject>,
    scrollState: LazyListState,
    refreshTrigger: Int,
    onEditEvent: (String, Int) -> Unit,
    onEditSettings: () -> Unit,
    onNavigateToAddEvent: (Int) -> Unit,
    onWavesChanged: () -> Unit,
    onCreateContainer: () -> Unit,
    onDeleteContainer: () -> Unit,
    parsedData: ParsedLevelData?,
    onEditCustomZombie: (String) -> Unit
) {
    if (waveManager == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inbox,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "未找到波次容器",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "当前关卡启用了波次管理模块，但缺少存储波次数据的实体对象 (WaveManagerProperties)。",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onCreateContainer,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("创建空波次容器")
                }
            }
        }
        return
    }

    // ======================== 1. 状态声明 ========================
    var confirmCheckbox by remember { mutableStateOf(false) }
    var showDeleteContainerDialog by remember { mutableStateOf(false) }

    var showExpectationDialog by remember { mutableStateOf<Int?>(null) }
    var waveToDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var editingWaveIndex by remember { mutableStateOf<Int?>(null) }

    var eventToRename by remember { mutableStateOf<String?>(null) }
    var newAliasInput by remember { mutableStateOf("") }
    var eventToCopy by remember { mutableStateOf<String?>(null) }
    var copyTargetInput by remember { mutableStateOf("") }

    var eventToDeleteRtid by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    var eventToMove by remember { mutableStateOf<String?>(null) }
    var moveSourceWaveIndex by remember { mutableStateOf<Int?>(null) }
    var moveTargetInput by remember { mutableStateOf("") }

    val interval = if (waveManager.flagWaveInterval <= 0) 10 else waveManager.flagWaveInterval


    val customZombies = remember(rootLevelFile?.objects, refreshTrigger) {
        rootLevelFile?.objects?.filter {
            it.objClass == "ZombieType"
        } ?: emptyList()
    }

    val customZombieUsageList = remember(customZombies, waveManager, refreshTrigger) {
        customZombies.map { typeObj ->
            val alias = typeObj.aliases?.firstOrNull() ?: "Unknown"
            val rtid = RtidParser.build(alias, "CurrentLevel")
            var baseType = "unknown"
            try {
                val json = typeObj.objData.asJsonObject
                if (json.has("TypeName")) {
                    baseType = json.get("TypeName").asString
                }
            } catch (e: Exception) { e.printStackTrace() }

            val locations = mutableListOf<String>()

            waveManager.waves.forEachIndexed { index, waveEvents ->
                waveEvents.forEach { eventRtid ->
                    val eventAlias = RtidParser.parse(eventRtid)?.alias
                    val eventObj = objectMap[eventAlias]
                    if (eventObj != null && eventObj.objData.toString().contains(rtid)) {
                        locations.add("第 ${index + 1} 波")
                    }
                }
            }
            ZombieUsageInfo(rtid, alias, baseType, locations.distinct())
        }
    }

    val zombieReferenceCounts = remember(customZombies, parsedData, refreshTrigger) {
        val counts = mutableMapOf<String, Int>()

        customZombies.forEach { obj ->
            obj.aliases?.firstOrNull()?.let { alias ->
                val rtid = RtidParser.build(alias, "CurrentLevel")
                counts[rtid] = 0
            }
        }

        waveManager.waves.flatten().forEach { eventRtid ->
            val eventAlias = RtidParser.parse(eventRtid)?.alias
            val eventObj = objectMap[eventAlias]
            if (eventObj != null) {
                val jsonStr = eventObj.objData.toString()
                counts.keys.forEach { zombieRtid ->
                    if (jsonStr.contains(zombieRtid)) {
                        counts[zombieRtid] = (counts[zombieRtid] ?: 0) + 1
                    }
                }
            }
        }
        counts
    }
    val unusedZombies = zombieReferenceCounts.filter { it.value == 0 }.keys
    val activeZombies = zombieReferenceCounts.filter { it.value > 0 }.keys

    // ======================== 2. 核心业务逻辑 ========================

    // 点数计算
    fun calculatePoints(waveIndex: Int, isFlag: Boolean): Int {
        if (waveModule == null || waveModule.dynamicZombies.isEmpty()) return 0
        val group = waveModule.dynamicZombies[0]
        val startEffectWave = group.startingWave + 1
        if (waveIndex < startEffectWave) return 0
        var basePoints = group.startingPoints + (waveIndex - startEffectWave) * group.pointIncrement
        if (basePoints > 60000) basePoints = 60000
        return if (isFlag) (basePoints * 2.5).toInt() else basePoints
    }

    // 重命名
    fun performGlobalRename(oldRtid: String, newAlias: String) {
        if (newAlias.isBlank() || rootLevelFile == null) return

        val oldAlias = LevelParser.extractAlias(oldRtid)

        if (oldAlias == newAlias) return
        val isAliasTaken = rootLevelFile.objects.any { it.aliases?.contains(newAlias) == true }

        if (isAliasTaken) {
            Toast.makeText(
                context,
                "错误：代号 \"$newAlias\" 已存在，请使用其他名称",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val newRtid = RtidParser.build(newAlias, "CurrentLevel")

        val objectIndex =
            rootLevelFile.objects.indexOfFirst { it.aliases?.contains(oldAlias) == true }
        if (objectIndex != -1) {
            val oldObj = rootLevelFile.objects[objectIndex]
            val updatedObj = oldObj.copy(aliases = listOf(newAlias))
            rootLevelFile.objects[objectIndex] = updatedObj
        }

        waveManager.waves.forEach { wave ->
            val iterator = wave.listIterator()
            while (iterator.hasNext()) {
                val rtid = iterator.next()
                if (rtid == oldRtid) {
                    iterator.set(newRtid)
                }
            }
        }

        onWavesChanged()
    }

    // 全局删除实体
    fun performSmartDelete(rtid: String, waveIdx: Int) {
        if (rootLevelFile == null) return

        val alias = LevelParser.extractAlias(rtid)
        val allReferences = waveManager.waves.flatten()
        val refCount = allReferences.count { it == rtid }

        if (refCount > 1) {
            val currentWave = waveManager.waves.getOrNull(waveIdx - 1)
            currentWave?.remove(rtid)
            Toast.makeText(
                context,
                "已移除当前波次的引用 (剩余引用: ${refCount - 1})",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            waveManager.waves.forEach { wave -> wave.removeAll { it == rtid } }
            rootLevelFile.objects.removeAll { it.aliases?.contains(alias) == true }
            Toast.makeText(
                context,
                "已彻底删除实体数据",
                Toast.LENGTH_SHORT
            ).show()
        }
        onWavesChanged()
    }

    // 校验失效引用
    val deadLinks = remember(waveManager.waves, objectMap, refreshTrigger) {
        waveManager.waves.flatten().distinct().filter { rtid ->
            !objectMap.containsKey(LevelParser.extractAlias(rtid))
        }
    }

    // 清除僵尸引用
    fun cleanUnusedZombies() {
        if (rootLevelFile == null) return
        var deletedCount = 0
        unusedZombies.forEach { zombieRtid ->
            val alias = RtidParser.parse(zombieRtid)?.alias ?: return@forEach

            val typeObj = rootLevelFile.objects.find { it.aliases?.contains(alias) == true }
            if (typeObj != null) {
                try {
                    val typeJson = typeObj.objData.asJsonObject
                    if (typeJson.has("Properties")) {
                        val propsRtid = typeJson.get("Properties").asString
                        val propsAlias = RtidParser.parse(propsRtid)?.alias
                        if (propsAlias != null && RtidParser.parse(propsRtid)?.source == "CurrentLevel") {
                            rootLevelFile.objects.removeAll { it.aliases?.contains(propsAlias) == true }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                rootLevelFile.objects.remove(typeObj)
                deletedCount++
            }
        }
        if (deletedCount > 0) {
            Toast.makeText(context, "清理了 $deletedCount 个闲置僵尸数据", Toast.LENGTH_SHORT)
                .show()
            onWavesChanged()
        }
    }

    fun deleteCustomZombie(info: ZombieUsageInfo) {
        if (rootLevelFile == null) return
        val typeObj = rootLevelFile.objects.find { it.aliases?.contains(info.alias) == true }
        if (typeObj != null) {
            try {
                val typeJson = typeObj.objData.asJsonObject
                if (typeJson.has("Properties")) {
                    val propsRtid = typeJson.get("Properties").asString
                    val propsInfo = RtidParser.parse(propsRtid)

                    if (propsInfo?.source == "CurrentLevel") {
                        rootLevelFile.objects.removeAll { it.aliases?.contains(propsInfo.alias) == true }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            rootLevelFile.objects.remove(typeObj)

            onWavesChanged()
            Toast.makeText(context, "已删除 ${info.alias} 及其属性数据", Toast.LENGTH_SHORT).show()
        }
    }

    var selectedZombieInfo by remember { mutableStateOf<ZombieUsageInfo?>(null) }

    // ======================== 3. 弹窗与交互组件 ========================

    // --- A. 期望展示 ---
    if (showExpectationDialog != null) {
        val waveIdx = showExpectationDialog!!
        val isFlag = (waveIdx % interval == 0 || waveIdx == waveManager.waves.size)
        val currentPoints = calculatePoints(waveIdx, isFlag)
        val expectationMap = remember(waveIdx, refreshTrigger) {
            val tempParsedData = ParsedLevelData(null, waveManager, waveModule, objectMap)
            WavePointAnalysis.calculateExpectation(currentPoints, tempParsedData)
        }

        AlertDialog(
            onDismissRequest = { showExpectationDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, null, tint = Color(0xFF673AB7))
                    Spacer(Modifier.width(8.dp))
                    Text("第 $waveIdx 波期望分析")
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "当前可用点数：",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF673AB7)
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "$currentPoints pt",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (currentPoints > 0) Color(0xFF388E3C) else Color.Red
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("点数出怪期望：", fontWeight = FontWeight.Bold, color = Color(0xFF673AB7))

                    Spacer(Modifier.height(8.dp))

                    if (currentPoints <= 0) {
                        Text(
                            "点数不足或为负，无随机出怪。",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else if (expectationMap.isEmpty()) {
                        Text("无 (僵尸池为空或数据缺失)", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        var hasContent = false
                        expectationMap.entries
                            .sortedByDescending { it.value }
                            .forEach { (typeName, count) ->
                                if (count > 0.05) {
                                    hasContent = true
                                    val info = remember(typeName) {
                                        ZombieRepository.getZombieInfoById(typeName)
                                    }
                                    val displayName = ZombieRepository.getName(typeName)
                                    val placeholderContent = @Composable {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Color(0xFFEEEEEE),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    Color.LightGray,
                                                    RoundedCornerShape(12.dp)
                                                ),
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
                                    Row(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AssetImage(
                                            path = if (info?.icon != null) "images/zombies/${info.icon}" else null,
                                            contentDescription = displayName,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White)
                                                .border(
                                                    1.dp,
                                                    Color.LightGray,
                                                    RoundedCornerShape(12.dp)
                                                ),
                                            filterQuality = FilterQuality.Medium,
                                            placeholder = placeholderContent
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(displayName, fontSize = 14.sp)
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            "~${String.format("%.2f", count)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1565C0)
                                        )
                                        Text(" 只", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        if (!hasContent) {
                            Text("点数过低，无法生成任何僵尸", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showExpectationDialog = null
                }) { Text("关闭") }
            }
        )
    }

    // --- B. 复制事件 ---
    if (eventToCopy != null) {
        AlertDialog(
            onDismissRequest = { eventToCopy = null },
            title = { Text("复制事件引用") },
            text = {
                OutlinedTextField(
                    value = copyTargetInput,
                    onValueChange = { copyTargetInput = it },
                    label = { Text("目标波次序号") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                TextButton(onClick = { eventToCopy = null }) { Text("取消") }
            },
            confirmButton = {
                Button(onClick = {
                    val target = copyTargetInput.toIntOrNull()
                    if (target != null && target in 1..waveManager.waves.size) {
                        val targetIdx = target - 1
                        val newEventList = waveManager.waves[targetIdx].toMutableList()
                        newEventList.add(eventToCopy!!)
                        waveManager.waves[targetIdx] = newEventList
                        onWavesChanged()
                        eventToCopy = null
                    } else {
                        Toast.makeText(context, "无效波次序号", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("复制") }
            }
        )
    }

    // --- C. 重命名 ---
    if (eventToRename != null) {
        val oldAlias = LevelParser.extractAlias(eventToRename!!)

        val isConflict = remember(newAliasInput) {
            newAliasInput != oldAlias && rootLevelFile?.objects?.any {
                it.aliases?.contains(
                    newAliasInput
                ) == true
            } == true
        }

        AlertDialog(
            onDismissRequest = { eventToRename = null },
            title = { Text("全局重命名事件") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newAliasInput,
                        onValueChange = { newAliasInput = it },
                        label = { Text("新代号(Alias)") },
                        isError = isConflict,
                        supportingText = {
                            if (isConflict) {
                                Text("该代号已被其他事件占用", color = Color.Red)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        performGlobalRename(eventToRename!!, newAliasInput)
                        eventToRename = null
                    },
                    enabled = !isConflict && newAliasInput.isNotBlank()
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { eventToRename = null }) { Text("取消") }
            }
        )
    }

    // --- D. 删除事件确认 ---
    var eventToDeleteSourceWave by remember { mutableStateOf<Int?>(null) }

    if (eventToDeleteRtid != null && eventToDeleteSourceWave != null) {
        val rtid = eventToDeleteRtid!!
        val allRefs = waveManager.waves.flatten()
        val count = allRefs.count { it == rtid }

        AlertDialog(
            onDismissRequest = { eventToDeleteRtid = null },
            title = { Text(if (count > 1) "移除事件引用" else "彻底删除事件") },
            text = {
                if (count > 1) {
                    Text("该事件在关卡中被引用了 $count 次。当前操作仅会从第 $eventToDeleteSourceWave 波中移除此引用，不会删除事件实体数据。")
                } else {
                    Text("这是该事件的最后一份引用。删除后将同时从文件中彻底抹除该事件的实体配置，不可恢复。")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        performSmartDelete(rtid, eventToDeleteSourceWave!!)
                        eventToDeleteRtid = null
                        eventToDeleteSourceWave = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (count > 1) Color.Gray else Color(
                            0xFFD32F2F
                        )
                    )
                ) {
                    Text(if (count > 1) "确认移除" else "确认彻底删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDeleteRtid = null }) { Text("取消") }
            }
        )
    }

    // --- E. 移动事件弹窗 ---
    if (eventToMove != null && moveSourceWaveIndex != null) {
        AlertDialog(
            onDismissRequest = { eventToMove = null; moveSourceWaveIndex = null },
            title = { Text("移动事件") },
            text = {
                OutlinedTextField(
                    value = moveTargetInput,
                    onValueChange = { moveTargetInput = it },
                    label = { Text("移动至波次序号") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            dismissButton = {
                TextButton(onClick = {
                    eventToMove = null; moveSourceWaveIndex = null
                }) { Text("取消") }
            },
            confirmButton = {
                Button(onClick = {
                    val target = moveTargetInput.toIntOrNull()
                    val sourceIdx = moveSourceWaveIndex!! - 1 // 注意：这里需要确保非空

                    if (target != null && target in 1..waveManager.waves.size) {
                        val targetIdx = target - 1

                        if (targetIdx == sourceIdx) {
                            Toast.makeText(context, "目标波次与当前波次相同", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            val newSourceList = waveManager.waves[sourceIdx].toMutableList()
                            val newTargetList = waveManager.waves[targetIdx].toMutableList()

                            newSourceList.remove(eventToMove!!)
                            newTargetList.add(eventToMove!!)

                            waveManager.waves[sourceIdx] = newSourceList
                            waveManager.waves[targetIdx] = newTargetList

                            onWavesChanged()
                            Toast.makeText(context, "已移动至第 $target 波", Toast.LENGTH_SHORT)
                                .show()

                            eventToMove = null
                            moveSourceWaveIndex = null
                        }
                    } else {
                        Toast.makeText(context, "无效波次序号", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("移动") }
            }
        )
    }

    // --- F. 删除波次确认 ---
    if (waveToDeleteIndex != null) {
        AlertDialog(
            onDismissRequest = {
                waveToDeleteIndex = null
                confirmCheckbox = false // 重置状态
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("删除波次 ${waveToDeleteIndex!! + 1}")
                }
            },
            text = {
                Column {
                    val eventCount = waveManager.waves[waveToDeleteIndex!!].size
                    Text(
                        "该操作将移除此波次及其内部的 $eventCount 个事件引用，确定要删除该波次？",
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(16.dp))

                    // 逻辑锁：勾选框
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.05f))
                            .clickable { confirmCheckbox = !confirmCheckbox }
                            .padding(8.dp)
                    ) {
                        Checkbox(
                            checked = confirmCheckbox,
                            onCheckedChange = { confirmCheckbox = it }
                        )
                        Text("我确定要永久删除此波次", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val index = waveToDeleteIndex!!
                        val eventsToRemove = waveManager.waves[index].toList()

                        waveManager.waves.removeAt(index)
                        waveManager.waveCount = waveManager.waves.size

                        if (rootLevelFile != null) {
                            val remainingRefs = waveManager.waves.flatten().toSet()
                            var cleanedCount = 0

                            eventsToRemove.forEach { rtid ->
                                if (!remainingRefs.contains(rtid)) {
                                    val alias = LevelParser.extractAlias(rtid)
                                    val wasRemoved =
                                        rootLevelFile.objects.removeAll { it.aliases?.contains(alias) == true }
                                    if (wasRemoved) cleanedCount++
                                }
                            }
                            Toast.makeText(context, "已删除波次", Toast.LENGTH_SHORT).show()
                        }
                        onWavesChanged()
                        waveToDeleteIndex = null
                        confirmCheckbox = false
                    },
                    enabled = confirmCheckbox,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("确认删除") }
            },
            dismissButton = {
                TextButton(onClick = {
                    waveToDeleteIndex = null
                    confirmCheckbox = false
                }) { Text("取消") }
            }
        )
    }

    // --- G. 管理抽屉 ---
    if (editingWaveIndex != null) {
        val waveIdx = editingWaveIndex!!
        val currentWaveEvents = waveManager.waves.getOrNull(waveIdx - 1) ?: mutableListOf()
        ModalBottomSheet(onDismissRequest = { editingWaveIndex = null }) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            "第 $waveIdx 波事件管理",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            editingWaveIndex = null; onNavigateToAddEvent(waveIdx)
                        }) {
                            Icon(Icons.Default.Add, null); Text("新增事件")
                        }
                    }
                }
                itemsIndexed(currentWaveEvents) { index, rtid ->
                    val alias = LevelParser.extractAlias(rtid)
                    val obj = objectMap[alias]

                    DrawerEventItem(
                        rtid = rtid,
                        obj = obj,
                        onEdit = {
                            editingWaveIndex = null
                            onEditEvent(rtid, waveIdx)
                        },
                        onCopy = {
                            eventToCopy = rtid
                            copyTargetInput = waveIdx.toString()
                        },
                        onRename = {
                            newAliasInput = alias
                            eventToRename = rtid
                        },
                        onDelete = {
                            eventToDeleteRtid = rtid
                            eventToDeleteSourceWave = waveIdx
                        },
                        onMove = {
                            eventToMove = rtid
                            moveSourceWaveIndex = waveIdx
                            moveTargetInput = waveIdx.toString()
                        }
                    )
                }
                item {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    // --- H. 删除容器弹窗 ---
    if (showDeleteContainerDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteContainerDialog = false },
            title = { Text("删除波次容器") },
            text = { Text("确定要删除空的波次容器吗？\n删除后您可以重新创建一个新的容器。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteContainer()
                        showDeleteContainerDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteContainerDialog = false }) { Text("取消") }
            }
        )
    }

    if (selectedZombieInfo != null) {
        val info = selectedZombieInfo!!
        val realTypeName = ZombiePropertiesRepository.getTypeNameByAlias(info.baseTypeName)
        val displayName = ZombieRepository.getName(realTypeName)
        val iconInfo = remember(realTypeName) {
            ZombieRepository.getZombieInfoById(realTypeName)
        }

        val placeholderContent = @Composable {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
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

        AlertDialog(
            onDismissRequest = { selectedZombieInfo = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
                            .background(Color.LightGray)
                    ) {
                        AssetImage(
                            path = if (iconInfo?.icon != null) "images/zombies/${iconInfo.icon}" else null,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            filterQuality = FilterQuality.Medium,
                            placeholder = placeholderContent
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.fillMaxWidth()){
                        Text(info.alias, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("原型: $displayName", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            },
            text = {
                Column {
                    if (info.isUnused) {
                        Text("此自定义僵尸当前未被任何波次或模块使用", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        Text("出现位置:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(info.locations.joinToString(", "), color = Color.Gray, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedZombieInfo = null
                        onEditCustomZombie(info.rtid)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("编辑属性")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteCustomZombie(info)
                        selectedZombieInfo = null
                    },
                    enabled = info.isUnused,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("删除实体")
                }
            }
        )
    }

    // ======================== 4. 主界面布局 ========================

    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                border = BorderStroke(1.dp, Color(0xFF1976D2))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lightbulb, null, tint = Color(0xFF1976D2))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "操作指引",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1976D2)
                        )
                        Text(
                            "右滑：管理波次事件\n左滑：删除该波次\n点击pt：查看期望",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        if (deadLinks.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color.Red)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row {
                            Icon(
                                Icons.Default.GppBad,
                                null,
                                tint = Color.Red
                            ); Spacer(Modifier.width(8.dp)); Text(
                            "引用失效报警",
                            color = Color.Red,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        }
                        Spacer(Modifier.height(8.dp))

                        deadLinks.forEach { Text(it, fontSize = 14.sp, color = Color.Red) }

                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                waveManager.waves.forEach {
                                    it.removeAll { r -> deadLinks.contains(r) }
                                }; onWavesChanged()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("一键清理失效波次", fontSize = 14.sp) }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF3E3)),
                border = BorderStroke(1.dp, Color(0xFFFF9800)),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Science, null, tint = Color(0xFFFF9800))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "自定义僵尸管理",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFFF9800)
                        )
                        Spacer(Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(12.dp))

                    if (activeZombies.isEmpty() && unusedZombies.isEmpty()) {
                        Text("暂无自定义僵尸数据", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            customZombieUsageList.forEach { info ->
                                AssistChip(
                                    onClick = { selectedZombieInfo = info },
                                    label = { Text(info.alias) },
                                    leadingIcon = {
                                        if (info.isUnused) {
                                            Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                                        } else {
                                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    border = null,
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (info.isUnused) Color(0xFFFFF3E0) else Color(0xFFF1F8E9),
                                        labelColor = if (info.isUnused) Color(0xFFEF6C00) else Color(0xFF33691E)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingEntryCard(
                    title = "波次管理器全局参数",
                    subtitle = "旗帜间隔: $interval, 刷新血线: ${(waveManager.minNextWaveHealthPercentage * 100).toInt()}% - ${(waveManager.maxNextWaveHealthPercentage * 100).toInt()}%",
                    icon = Icons.Default.Tune,
                    onClick = onEditSettings
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        if (waveManager.waves.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "当前波次列表为空",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "您可以添加第一个波次，或者删除这个空的容器。",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = { showDeleteContainerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Icon(Icons.Default.DeleteForever, null)
                            Spacer(Modifier.width(8.dp))
                            Text("删除空容器")
                        }
                    }
                }
            }
        } else {

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEEEEEE))
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                ) {
                    Text(
                        "#",
                        modifier = Modifier.width(36.dp),
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        "内容及点数预览",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        "Total: ${waveManager.waves.size}",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }

            itemsIndexed(
                items = waveManager.waves,
                key = { index, _ -> "wave_row_${index}_${refreshTrigger}" }
            ) { index, waveEvents ->
                val waveIndex = index + 1
                val isFlagWave = (waveIndex % interval == 0 || waveIndex == waveManager.waves.size)
                val points = calculatePoints(waveIndex, isFlagWave)

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        when (value) {
                            SwipeToDismissBoxValue.StartToEnd -> {
                                editingWaveIndex = waveIndex
                                false
                            }

                            SwipeToDismissBoxValue.EndToStart -> {
                                waveToDeleteIndex = index
                                false
                            }

                            else -> false
                        }
                    },
                    positionalThreshold = { totalDistance ->
                        totalDistance * 0.5f
                    }
                )

                LaunchedEffect(waveToDeleteIndex, editingWaveIndex, refreshTrigger) {
                    if (waveToDeleteIndex == null && editingWaveIndex == null) {
                        dismissState.reset()
                    }
                }

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val direction = dismissState.dismissDirection
                        val color = when (direction) {
                            SwipeToDismissBoxValue.StartToEnd -> Color(0xFF388E3C)
                            SwipeToDismissBoxValue.EndToStart -> Color.Red
                            else -> Color.Transparent
                        }
                        val alignment =
                            if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                        val icon =
                            if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Settings else Icons.Default.Delete

                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 24.dp),
                            contentAlignment = alignment
                        ) {
                            Icon(icon, null, tint = Color.White)
                        }
                    }
                ) {
                    WaveRowItem(
                        waveIndex = waveIndex,
                        isFlagWave = isFlagWave,
                        rtidList = waveEvents,
                        objectMap = objectMap,
                        points = points,
                        onEditEvent = onEditEvent,
                        onInfoClick = { showExpectationDialog = waveIndex }
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = {
                    waveManager.waves.add(mutableListOf())
                    waveManager.waveCount = waveManager.waves.size
                    onWavesChanged()
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("添加空波次")
                }
            }
        }
    }
}

@Composable
fun WaveRowItem(
    waveIndex: Int,
    isFlagWave: Boolean,
    rtidList: List<String>,
    objectMap: Map<String, PvzObject>,
    points: Int,
    onEditEvent: (String, Int) -> Unit,
    onInfoClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$waveIndex",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF388E3C)
                    )
                    if (isFlagWave) Icon(
                        Icons.Default.Flag,
                        null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier
                            .size(12.dp)
                            .padding(start = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (rtidList.isEmpty()) {
                    Text("空波次 (左右划操作)", color = Color.LightGray, fontSize = 11.sp)
                } else {
                    rtidList.forEach { rtid ->
                        EventChip(rtid, objectMap) { onEditEvent(rtid, waveIndex) }
                    }
                }
            }

            if (points != 0) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onInfoClick() }
                        .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${points}pt",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = Color.LightGray.copy(0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(0.2f))
    }
}


@Composable
fun DrawerEventItem(
    rtid: String,
    obj: PvzObject?,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val alias = LevelParser.extractAlias(rtid)
    val isInvalid = obj == null
    val meta = EventRegistry.getMetadata(obj?.objClass ?: "")

    // 主题色判定
    val themeColor = if (isInvalid) Color(0xFFD32F2F) else (meta?.color ?: Color.Gray)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(
            containerColor = if (isInvalid) Color(0xFFFFEBEE) else Color(0xFFF8F9FA)
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isInvalid) BorderStroke(1.dp, Color.Red.copy(0.3f)) else null
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min) // 使左侧条高度撑满
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 左侧主题色条
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(themeColor)
            )

            // 2. 内容区
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isInvalid) {
                        Icon(
                            Icons.Default.Error,
                            null,
                            tint = themeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = alias,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isInvalid) themeColor else Color.Black
                    )
                }
                Text(
                    text = if (isInvalid) "引用失效：找不到实体对象" else (meta?.title
                        ?: "未知类型"),
                    fontSize = 11.sp,
                    color = themeColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }

            // 3. 按钮组
            Row(
                modifier = Modifier.padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 重命名
                IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.DriveFileRenameOutline,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                // 复制
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.ContentCopy,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                // 移动
                IconButton(onClick = onMove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                // 删除
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        null,
                        tint = themeColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}