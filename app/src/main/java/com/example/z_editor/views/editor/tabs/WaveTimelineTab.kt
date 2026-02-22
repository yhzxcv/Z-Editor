package com.example.z_editor.views.editor.tabs

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.R
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
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzPurpleDark
import com.example.z_editor.ui.theme.PvzPurpleLight
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
    val context = LocalContext.current

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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.wave_timeline_container_not_found),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.wave_timeline_container_missing_desc),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onCreateContainer,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.wave_timeline_btn_create_container))
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
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val locations = mutableListOf<String>()

            waveManager.waves.forEachIndexed { index, waveEvents ->
                waveEvents.forEach { eventRtid ->
                    val eventAlias = RtidParser.parse(eventRtid)?.alias
                    val eventObj = objectMap[eventAlias]
                    if (eventObj != null && eventObj.objData.toString().contains(rtid)) {
                        locations.add(
                            context.getString(
                                R.string.wave_timeline_location_format,
                                index + 1
                            )
                        )
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

    // 重命名
    fun performGlobalRename(oldRtid: String, newAlias: String) {
        if (newAlias.isBlank() || rootLevelFile == null) return

        val oldAlias = LevelParser.extractAlias(oldRtid)
        if (oldAlias == newAlias) return
        val isAliasTaken = rootLevelFile.objects.any { it.aliases?.contains(newAlias) == true }

        if (isAliasTaken) {
            Toast.makeText(
                context,
                context.getString(R.string.wave_timeline_toast_alias_exists, newAlias),
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
                context.getString(R.string.wave_timeline_toast_removed_ref, refCount - 1),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            waveManager.waves.forEach { wave -> wave.removeAll { it == rtid } }
            rootLevelFile.objects.removeAll { it.aliases?.contains(alias) == true }
            Toast.makeText(
                context,
                context.getString(R.string.wave_timeline_toast_deleted_entity),
                Toast.LENGTH_SHORT
            ).show()
        }
        onWavesChanged()
    }

    val deadLinks = remember(waveManager.waves, objectMap, refreshTrigger) {
        waveManager.waves.flatten().distinct().filter { rtid ->
            !objectMap.containsKey(LevelParser.extractAlias(rtid))
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
            Toast.makeText(
                context,
                context.getString(R.string.wave_timeline_toast_deleted_custom_zombie, info.alias),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    var selectedZombieInfo by remember { mutableStateOf<ZombieUsageInfo?>(null) }

    // ======================== 3. 弹窗与交互组件 ========================

    // --- A. 期望展示 ---
    if (showExpectationDialog != null) {
        val waveIdx = showExpectationDialog!!
        val isFlag = (waveIdx % interval == 0 || waveIdx == waveManager.waves.size)
        val currentPoints = calculatePoints(waveIdx, isFlag, waveModule)
        val expectationMap = remember(waveIdx, refreshTrigger) {
            val tempParsedData = ParsedLevelData(null, waveManager, waveModule, objectMap)
            WavePointAnalysis.calculateExpectation(currentPoints, tempParsedData)
        }

        val isDark = LocalDarkTheme.current
        val expColor = if (isDark) PvzPurpleDark else PvzPurpleLight

        AlertDialog(
            onDismissRequest = { showExpectationDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, null, tint = expColor)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.wave_timeline_dialog_exp_title, waveIdx))
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.wave_timeline_label_available_points),
                            fontWeight = FontWeight.Bold,
                            color = expColor
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            stringResource(R.string.wave_timeline_points_value, currentPoints),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (currentPoints > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onError
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.wave_timeline_label_spawn_exp),
                        fontWeight = FontWeight.Bold,
                        color = expColor
                    )
                    Spacer(Modifier.height(8.dp))

                    if (currentPoints <= 0) {
                        Text(
                            stringResource(R.string.wave_timeline_msg_no_points),
                            color = MaterialTheme.colorScheme.onError,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else if (expectationMap.isEmpty()) {
                        Text(
                            stringResource(R.string.wave_timeline_msg_empty_pool),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
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
                                    Row(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AssetImage(
                                            path = if (info?.icon != null) "images/zombies/${info.icon}" else "images/others/unknown.webp",
                                            contentDescription = displayName,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White),
                                            filterQuality = FilterQuality.Medium
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(displayName, fontSize = 14.sp)
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            stringResource(
                                                R.string.wave_timeline_zombie_count_approx,
                                                count
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            stringResource(R.string.wave_timeline_unit_zombie),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        if (!hasContent) {
                            Text(
                                stringResource(R.string.wave_timeline_msg_points_too_low),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showExpectationDialog = null
                }) { Text(stringResource(R.string.wave_timeline_btn_close)) }
            }
        )
    }

    // --- B. 复制事件 ---
    if (eventToCopy != null) {
        AlertDialog(
            onDismissRequest = { eventToCopy = null },
            title = { Text(stringResource(R.string.wave_timeline_dialog_copy_title)) },
            text = {
                OutlinedTextField(
                    value = copyTargetInput,
                    onValueChange = { copyTargetInput = it },
                    label = { Text(stringResource(R.string.wave_timeline_label_target_wave)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                TextButton(onClick = {
                    eventToCopy = null
                }) { Text(stringResource(R.string.wave_timeline_btn_cancel)) }
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
                        Toast.makeText(
                            context,
                            context.getString(R.string.wave_timeline_toast_invalid_wave),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) { Text(stringResource(R.string.wave_timeline_btn_copy)) }
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
            title = { Text(stringResource(R.string.wave_timeline_dialog_rename_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newAliasInput,
                        onValueChange = { newAliasInput = it },
                        label = { Text(stringResource(R.string.wave_timeline_label_new_alias)) },
                        isError = isConflict,
                        supportingText = { if (isConflict) Text(stringResource(R.string.wave_timeline_error_alias_taken)) },
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
                ) { Text(stringResource(R.string.wave_timeline_btn_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    eventToRename = null
                }) { Text(stringResource(R.string.wave_timeline_btn_cancel)) }
            }
        )
    }

    // --- D. 删除事件确认 ---
    var eventToDeleteSourceWave by remember { mutableStateOf<Int?>(null) }
    if (eventToDeleteRtid != null && eventToDeleteSourceWave != null) {
        val rtid = eventToDeleteRtid!!
        val count = waveManager.waves.flatten().count { it == rtid }
        AlertDialog(
            onDismissRequest = { eventToDeleteRtid = null },
            title = { Text(stringResource(if (count > 1) R.string.wave_timeline_dialog_remove_ref_title else R.string.wave_timeline_dialog_delete_entity_title)) },
            text = {
                Text(
                    if (count > 1)
                        stringResource(
                            R.string.wave_timeline_msg_remove_ref,
                            count,
                            eventToDeleteSourceWave!!
                        )
                    else stringResource(R.string.wave_timeline_msg_delete_entity)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        performSmartDelete(rtid, eventToDeleteSourceWave!!)
                        eventToDeleteRtid = null
                        eventToDeleteSourceWave = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (count > 1) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onError)
                ) {
                    Text(stringResource(if (count > 1) R.string.wave_timeline_btn_confirm_remove else R.string.wave_timeline_btn_confirm_delete_all))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    eventToDeleteRtid = null
                }) { Text(stringResource(R.string.wave_timeline_btn_cancel)) }
            }
        )
    }

    // --- E. 移动事件弹窗 ---
    if (eventToMove != null && moveSourceWaveIndex != null) {
        AlertDialog(
            onDismissRequest = { eventToMove = null; moveSourceWaveIndex = null },
            title = { Text(stringResource(R.string.wave_timeline_dialog_move_title)) },
            text = {
                OutlinedTextField(
                    value = moveTargetInput,
                    onValueChange = { moveTargetInput = it },
                    label = { Text(stringResource(R.string.wave_timeline_label_move_target)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            dismissButton = {
                TextButton(onClick = { eventToMove = null; moveSourceWaveIndex = null }) {
                    Text(
                        stringResource(R.string.wave_timeline_btn_cancel)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val target = moveTargetInput.toIntOrNull()
                    val sourceIdx = moveSourceWaveIndex!! - 1
                    if (target != null && target in 1..waveManager.waves.size) {
                        val targetIdx = target - 1
                        if (targetIdx == sourceIdx) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.wave_timeline_toast_same_wave),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val newSourceList = waveManager.waves[sourceIdx].toMutableList()
                            val newTargetList = waveManager.waves[targetIdx].toMutableList()
                            newSourceList.remove(eventToMove!!)
                            newTargetList.add(eventToMove!!)
                            waveManager.waves[sourceIdx] = newSourceList
                            waveManager.waves[targetIdx] = newTargetList
                            onWavesChanged()
                            Toast.makeText(
                                context,
                                context.getString(
                                    R.string.wave_timeline_toast_moved_success,
                                    target
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                            eventToMove = null
                            moveSourceWaveIndex = null
                        }
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.wave_timeline_toast_invalid_wave),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) { Text(stringResource(R.string.wave_timeline_btn_move)) }
            }
        )
    }

    // --- F. 删除波次确认 ---
    if (waveToDeleteIndex != null) {
        AlertDialog(
            onDismissRequest = { waveToDeleteIndex = null; confirmCheckbox = false },
            title = {
                Text(
                    stringResource(
                        R.string.wave_timeline_dialog_delete_wave_title,
                        waveToDeleteIndex!! + 1
                    )
                )
            },
            text = {
                Column {
                    val eventCount = waveManager.waves[waveToDeleteIndex!!].size
                    Text(
                        stringResource(R.string.wave_timeline_msg_delete_wave, eventCount),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f))
                            .clickable { confirmCheckbox = !confirmCheckbox }
                            .padding(8.dp)
                    ) {
                        Checkbox(
                            checked = confirmCheckbox,
                            onCheckedChange = { confirmCheckbox = it })
                        Text(
                            stringResource(R.string.wave_timeline_checkbox_confirm_permanent),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            eventsToRemove.forEach { rtid ->
                                if (!remainingRefs.contains(rtid)) {
                                    val alias = LevelParser.extractAlias(rtid)
                                    rootLevelFile.objects.removeAll { it.aliases?.contains(alias) == true }
                                }
                            }
                            Toast.makeText(
                                context,
                                context.getString(R.string.wave_timeline_toast_wave_deleted),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        onWavesChanged()
                        waveToDeleteIndex = null
                        confirmCheckbox = false
                    },
                    enabled = confirmCheckbox,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onError)
                ) { Text(stringResource(R.string.wave_timeline_btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { waveToDeleteIndex = null; confirmCheckbox = false }) {
                    Text(
                        stringResource(R.string.wave_timeline_btn_cancel)
                    )
                }
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
                            stringResource(R.string.wave_timeline_drawer_title, waveIdx),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            editingWaveIndex = null; onNavigateToAddEvent(waveIdx)
                        }) {
                            Icon(
                                Icons.Default.Add,
                                null
                            ); Text(stringResource(R.string.wave_timeline_btn_add_event))
                        }
                    }
                }
                itemsIndexed(currentWaveEvents) { _, rtid ->
                    DrawerEventItem(
                        rtid = rtid,
                        obj = objectMap[LevelParser.extractAlias(rtid)],
                        onEdit = { editingWaveIndex = null; onEditEvent(rtid, waveIdx) },
                        onCopy = { eventToCopy = rtid; copyTargetInput = waveIdx.toString() },
                        onRename = {
                            newAliasInput = LevelParser.extractAlias(rtid); eventToRename = rtid
                        },
                        onDelete = { eventToDeleteRtid = rtid; eventToDeleteSourceWave = waveIdx },
                        onMove = {
                            eventToMove = rtid; moveSourceWaveIndex = waveIdx; moveTargetInput =
                            waveIdx.toString()
                        }
                    )
                }
            }
        }
    }

    // --- H. 删除容器弹窗 ---
    if (showDeleteContainerDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteContainerDialog = false },
            title = { Text(stringResource(R.string.wave_timeline_dialog_delete_container_title)) },
            text = { Text(stringResource(R.string.wave_timeline_dialog_delete_container_desc)) },
            confirmButton = {
                TextButton(
                    onClick = { onDeleteContainer(); showDeleteContainerDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onError)
                ) {
                    Text(stringResource(R.string.wave_timeline_btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteContainerDialog = false
                }) { Text(stringResource(R.string.wave_timeline_btn_cancel)) }
            }
        )
    }

    if (selectedZombieInfo != null) {
        val info = selectedZombieInfo!!
        val realTypeName = ZombiePropertiesRepository.getTypeNameByAlias(info.baseTypeName)
        val displayName = ZombieRepository.getName(realTypeName)
        val iconInfo = remember(realTypeName) { ZombieRepository.getZombieInfoById(realTypeName) }

        AlertDialog(
            onDismissRequest = { selectedZombieInfo = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AssetImage(
                            path = if (iconInfo?.icon != null) "images/zombies/${iconInfo.icon}" else "images/others/unknown.webp",
                            contentDescription = null, modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(info.alias, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            stringResource(R.string.wave_timeline_label_prototype, displayName),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column {
                    if (info.isUnused) {
                        Text(
                            stringResource(R.string.wave_timeline_msg_zombie_unused),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            stringResource(R.string.wave_timeline_label_locations),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            info.locations.joinToString(", "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedZombieInfo = null; onEditCustomZombie(info.rtid) }) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.wave_timeline_btn_edit_props))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteCustomZombie(info); selectedZombieInfo = null },
                    enabled = info.isUnused,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onError)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.wave_timeline_btn_delete_entity_short))
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
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.outline),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lightbulb, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            stringResource(R.string.wave_timeline_guide_title),
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            stringResource(R.string.wave_timeline_guide_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onTertiary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Science, null); Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.wave_timeline_mgmt_custom_zombies),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (activeZombies.isEmpty() && unusedZombies.isEmpty()) {
                        Text(
                            stringResource(R.string.wave_timeline_no_custom_zombies),
                            fontSize = 12.sp
                        )
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
                                        Icon(
                                            if (info.isUnused) Icons.Default.Warning else Icons.Default.CheckCircle,
                                            null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }


        if (deadLinks.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onError)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row {
                            Icon(
                                Icons.Default.GppBad,
                                null,
                                tint = MaterialTheme.colorScheme.onError
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.wave_timeline_alert_dead_link),
                                color = MaterialTheme.colorScheme.onError,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        deadLinks.forEach {
                            Text(
                                it,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onError
                            )
                            Spacer(Modifier.height(2.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                waveManager.waves.forEach {
                                    it.removeAll { r -> deadLinks.contains(r) }
                                }; onWavesChanged()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onError),
                        ) { Text(stringResource(R.string.wave_timeline_btn_clean_dead_links)) }
                    }
                }
            }
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                SettingEntryCard(
                    title = stringResource(R.string.wave_timeline_global_params_title),
                    subtitle = stringResource(
                        R.string.wave_timeline_global_params_subtitle,
                        interval,
                        (waveManager.minNextWaveHealthPercentage * 100).toInt(),
                        (waveManager.maxNextWaveHealthPercentage * 100).toInt()
                    ),
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
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.wave_timeline_empty_list_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.wave_timeline_empty_list_desc),
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { showDeleteContainerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onError)
                        ) {
                            Icon(Icons.Default.DeleteForever, null); Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.wave_timeline_btn_delete_empty_container))
                        }
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                ) {
                    Text("#", modifier = Modifier.width(36.dp), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.wave_timeline_table_header_content),
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.wave_timeline_total_waves, waveManager.waves.size),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            itemsIndexed(
                items = waveManager.waves,
                key = { index, _ -> "wave_row_${index}_${refreshTrigger}" }) { index, waveEvents ->
                val waveIndex = index + 1
                val isFlagWave = (waveIndex % interval == 0 || waveIndex == waveManager.waves.size)
                val points = calculatePoints(waveIndex, isFlagWave, waveModule)
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        when (value) {
                            SwipeToDismissBoxValue.StartToEnd -> {
                                editingWaveIndex = waveIndex; false
                            }

                            SwipeToDismissBoxValue.EndToStart -> {
                                waveToDeleteIndex = index; false
                            }

                            else -> false
                        }
                    }
                )
                LaunchedEffect(waveToDeleteIndex, editingWaveIndex, refreshTrigger) {
                    if (waveToDeleteIndex == null && editingWaveIndex == null) dismissState.reset()
                }
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val color =
                            if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onError
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 24.dp),
                            contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                        ) {
                            Icon(
                                if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Settings else Icons.Default.Delete,
                                null,
                                tint = Color.White
                            )
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
                        onInfoClick = { showExpectationDialog = waveIndex })
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
                    waveManager.waves.add(mutableListOf()); waveManager.waveCount =
                    waveManager.waves.size; onWavesChanged()
                }) {
                    Icon(
                        Icons.Default.Add,
                        null
                    ); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.wave_timeline_btn_add_wave))
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
            .background(MaterialTheme.colorScheme.scrim)
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
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isFlagWave) Icon(
                        Icons.Default.Flag,
                        null,
                        tint = MaterialTheme.colorScheme.onError,
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
                    Text(
                        stringResource(R.string.wave_timeline_empty_wave_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)
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

    val themeColor = if (isInvalid) MaterialTheme.colorScheme.onError else (meta?.color
        ?: MaterialTheme.colorScheme.onSurfaceVariant)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(
            containerColor = if (isInvalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isInvalid) BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onError.copy(0.3f)
        ) else null
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(themeColor)
            )
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
                        color = if (isInvalid) themeColor else MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = when {
                        isInvalid -> stringResource(R.string.wave_timeline_error_invalid_ref)
                        meta != null -> stringResource(meta.title)
                        else -> stringResource(R.string.wave_timeline_label_unknown_type)
                    },
                    fontSize = 11.sp,
                    color = themeColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
            Row(
                modifier = Modifier.padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.DriveFileRenameOutline,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.ContentCopy,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onMove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
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

fun calculatePoints(waveIndex: Int, isFlag: Boolean, waveModule: WaveManagerModuleData?): Int {
    if (waveModule == null) return 0
    val zombiesList = waveModule.dynamicZombies
    if (zombiesList.isNullOrEmpty()) return 0
    val group = zombiesList[0]
    val startEffectWave = group.startingWave + 1
    if (waveIndex < startEffectWave) return 0
    var basePoints = group.startingPoints + (waveIndex - startEffectWave) * group.pointIncrement
    if (basePoints > 60000) basePoints = 60000
    return if (isFlag) (basePoints * 2.5).toInt() else basePoints
}