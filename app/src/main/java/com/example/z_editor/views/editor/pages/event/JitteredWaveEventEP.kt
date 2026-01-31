package com.example.z_editor.views.editor.pages.event

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.WaveActionData
import com.example.z_editor.data.ZombieSpawnData
import com.example.z_editor.data.repository.PlantRepository
import com.example.z_editor.data.repository.ZombiePropertiesRepository
import com.example.z_editor.data.repository.ZombieRepository
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.example.z_editor.views.editor.pages.others.LaneRow
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import com.example.z_editor.views.editor.pages.others.ZombieEditSheetContent
import com.google.gson.Gson
import kotlin.math.roundToInt

private val gson = Gson()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpawnZombiesJitteredWaveActionPropsEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    onRequestZombieSelection: ((String) -> Unit) -> Unit,
    onRequestPlantSelection: ((String) -> Unit) -> Unit,
    scrollState: LazyListState,
    onInjectZombie: (String) -> String?,
    onEditCustomZombie: (String) -> Unit
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    val focusManager = LocalFocusManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var batchLevelFloat by remember { mutableFloatStateOf(1f) }
    var showBatchConfirmDialog by remember { mutableStateOf(false) }
    var localRefreshTrigger by remember { mutableIntStateOf(0) }

    val objectMap = remember(rootLevelFile, localRefreshTrigger) {
        rootLevelFile.objects.associateBy { it.aliases?.firstOrNull() ?: "unknown" }
    }

    val actionDataState = remember(localRefreshTrigger) {
        val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        val data = try {
            gson.fromJson(obj?.objData, WaveActionData::class.java)
        } catch (_: Exception) {
            WaveActionData()
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

    var addingToRowIndex by remember { mutableStateOf<Int?>(null) }
    var editingZombie by remember { mutableStateOf<ZombieSpawnData?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    fun sync(newData: WaveActionData) {
        actionDataState.value = newData
        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }?.let {
            it.objData = gson.toJsonTree(newData)
        }
    }

    fun handleAddZombie() {
        onRequestZombieSelection { selectedId ->
            val isElite = ZombieRepository.isElite(selectedId)
            val aliases = ZombieRepository.buildZombieAliases(selectedId)
            val newZombie = ZombieSpawnData(
                type = RtidParser.build(aliases, "ZombieTypes"),
                row = addingToRowIndex,
                level = null,
                isElite = isElite
            )

            val newList = actionDataState.value.zombies.toMutableList()
            newList.add(newZombie)
            sync(actionDataState.value.copy(zombies = newList))
        }
    }

    fun executeBatchUpdate() {
        val targetLevel = batchLevelFloat.roundToInt()
        val currentZombies = actionDataState.value.zombies.toList()
        var changeCount = 0

        val newZombies = currentZombies.map { zombie ->
            if (!zombie.isElite) {
                changeCount++
                zombie.copy(level = if (targetLevel == 0) null else targetLevel)
            } else {
                zombie
            }
        }.toMutableList()

        sync(actionDataState.value.copy(zombies = newZombies))
        showBatchConfirmDialog = false
        Toast.makeText(
            context,
            "已将 $changeCount 只僵尸设为 $targetLevel 阶",
            Toast.LENGTH_SHORT
        ).show()
    }

    val themeColor = MaterialTheme.colorScheme.secondary

    if (showBottomSheet && editingZombie != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false; editingZombie = null },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            val currentBaseType = remember(editingZombie!!.type) {
                val rtidInfo = RtidParser.parse(editingZombie!!.type)
                val alias = rtidInfo?.alias ?: editingZombie!!.type

                val obj = objectMap[alias]
                if (obj != null && obj.objClass == "ZombieType") {
                    try {
                        obj.objData.asJsonObject.get("TypeName").asString
                    } catch (_: Exception) {
                        ZombiePropertiesRepository.getTypeNameByAlias(alias)
                    }
                } else {
                    ZombiePropertiesRepository.getTypeNameByAlias(alias)
                }
            }
            val compatibleCustomZombies = remember(rootLevelFile.objects, currentBaseType) {
                rootLevelFile.objects
                    .filter { it.objClass == "ZombieType" }
                    .mapNotNull { obj ->
                        try {
                            val json = obj.objData.asJsonObject
                            if (json.has("TypeName") && json.get("TypeName").asString == currentBaseType) {
                                val alias = obj.aliases?.firstOrNull() ?: "Unknown"
                                val rtid = RtidParser.build(alias, "CurrentLevel")
                                alias to rtid
                            } else {
                                null
                            }
                        } catch (_: Exception) {
                            null
                        }
                    }
            }
            ZombieEditSheetContent(
                originalZombie = editingZombie!!,
                objectMap = objectMap,
                compatibleCustomZombies = compatibleCustomZombies,
                onValueChange = { updatedZombie ->
                    val currentList = actionDataState.value.zombies.toMutableList()
                    val index = currentList.indexOf(editingZombie!!)
                    if (index != -1) {
                        currentList[index] = updatedZombie
                        editingZombie = updatedZombie
                        sync(actionDataState.value.copy(zombies = currentList))
                    }
                },
                onCopy = {
                    val newZombie = editingZombie!!.copy()
                    val currentList = actionDataState.value.zombies.toMutableList()
                    currentList.add(newZombie)
                    sync(actionDataState.value.copy(zombies = currentList))
                    showBottomSheet = false
                    editingZombie = null
                },
                onDelete = {
                    val currentList = actionDataState.value.zombies.toMutableList()
                    currentList.remove(editingZombie!!)
                    sync(actionDataState.value.copy(zombies = currentList))
                    showBottomSheet = false
                    editingZombie = null
                },
                onInjectCustom = { oldAlias ->
                    val newRtid = onInjectZombie(oldAlias)
                    if (newRtid != null) {
                        val updatedZombie = editingZombie!!.copy(type = newRtid)
                        val currentList = actionDataState.value.zombies.toMutableList()
                        val index = currentList.indexOf(editingZombie!!)
                        if (index != -1) {
                            currentList[index] = updatedZombie
                            editingZombie = updatedZombie
                            sync(actionDataState.value.copy(zombies = currentList))
                            showBottomSheet = false
                            localRefreshTrigger++
                        }
                    }
                },
                onEditCustom = { rtid ->
                    showBottomSheet = false
                    editingZombie = null
                    onEditCustomZombie(rtid)
                },
                onSelectExistingCustom = { selectedRtid ->
                    val updatedZombie = editingZombie!!.copy(type = selectedRtid)
                    val currentList = actionDataState.value.zombies.toMutableList()
                    val index = currentList.indexOf(editingZombie!!)
                    if (index != -1) {
                        currentList[index] = updatedZombie
                        editingZombie = updatedZombie
                        sync(actionDataState.value.copy(zombies = currentList))
                    }
                }
            )
        }
    }

    if (showBatchConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBatchConfirmDialog = false },
            icon = { Icon(Icons.Default.Check, null) },
            title = { Text("确认批量应用？") },
            text = {
                val level = batchLevelFloat.roundToInt()
                Text("此操作将把当前波次内的所有僵尸等级统一设置为 $level 阶。")
            },
            confirmButton = {
                Button(
                    onClick = { executeBatchUpdate() },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    Text("确认覆盖")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchConfirmDialog = false }) {
                    Text(
                        "取消",
                        color = themeColor
                    )
                }
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
            CommonEditorTopAppBar(
                title = "编辑 $currentAlias",
                subtitle = "事件类型：自然出怪",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "自然出怪事件说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "最基础的生成僵尸事件。可以配置每一只僵尸的阶级和行号，0阶表示随地图阶级，庭院模式下即为1阶。"
                )
                HelpSection(
                    title = "等级设置",
                    body = "精英级僵尸不能更改等级，在非庭院模式下，僵尸的阶级序列定义一般只到5阶。可以通过逐个调整或者批量设置配置当前事件内的僵尸阶级。"
                )
                HelpSection(
                    title = "掉落物配置",
                    body = "默认情况下配置的是携带能量豆的僵尸个数，启用掉落植物功能后会随机从配置的植物库里掉落植物卡片。"
                )
            }
        }
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                val jamOptions = listOf(
                    null to "默认/无 (None)",
                    "jam_pop" to "流行 (Pop)",
                    "jam_rap" to "说唱 (Rap)",
                    "jam_metal" to "重金属 (Metal)",
                    "jam_punk" to "朋克 (Punk)",
                    "jam_8bit" to "街机 (8-Bit)"
                )
                val currentJamCode = actionDataState.value.notificationEvents?.firstOrNull()
                val currentJamLabel =
                    jamOptions.find { it.first == currentJamCode }?.second ?: "默认/无 (None)"
                var jamExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, null, tint = themeColor)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "魔音音乐切换",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = themeColor
                            )
                        }
                        Spacer(Modifier.height(12.dp))

                        ExposedDropdownMenuBox(
                            expanded = jamExpanded,
                            onExpandedChange = { jamExpanded = !jamExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = currentJamLabel,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = jamExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = themeColor,
                                    focusedLabelColor = themeColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = jamExpanded,
                                onDismissRequest = { jamExpanded = false }
                            ) {
                                jamOptions.forEach { (code, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            val newList =
                                                if (code == null) null else mutableListOf(code)
                                            sync(actionDataState.value.copy(notificationEvents = newList))
                                            jamExpanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "此事件触发时切换背景音乐，仅对摇滚年代地图有效。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(5) { index ->
                val rowNum = index + 1
                val zombiesInRow = actionDataState.value.zombies.filter { it.row == rowNum }

                LaneRow(
                    laneLabel = "第 $rowNum 行",
                    laneColor = MaterialTheme.colorScheme.primary,
                    zombies = zombiesInRow,
                    onAddClick = {
                        addingToRowIndex = rowNum
                        handleAddZombie()
                    },
                    objectMap = objectMap,
                    onZombieClick = { zombie ->
                        editingZombie = zombie
                        showBottomSheet = true
                    }
                )
            }

            item {
                val randomZombies =
                    actionDataState.value.zombies.filter { it.row == null || it.row == 0 }
                LaneRow(
                    laneLabel = "随机行",
                    laneColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    zombies = randomZombies,
                    onAddClick = {
                        addingToRowIndex = null
                        handleAddZombie()
                    },
                    objectMap = objectMap,
                    onZombieClick = { zombie ->
                        editingZombie = zombie
                        showBottomSheet = true
                    }
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Layers,
                                null,
                                tint = themeColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "批量设置等级",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = themeColor
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "${batchLevelFloat.roundToInt()} 阶",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = themeColor
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Slider(
                                colors = SliderDefaults.colors(
                                    inactiveTickColor = themeColor,
                                    thumbColor = themeColor,
                                    activeTrackColor = themeColor,
                                ),
                                value = batchLevelFloat,
                                onValueChange = { batchLevelFloat = it },
                                valueRange = 1f..10f,
                                steps = 8,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(Modifier.width(16.dp))

                            Button(
                                onClick = { showBatchConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("一键应用", fontSize = 13.sp)
                            }
                        }

                        Text(
                            text = "将本波次所有僵尸设为指定等级（精英不受影响）",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                }
            }

            item {
                val count = actionDataState.value.additionalPlantFood ?: 0
                val spawnPlantList = actionDataState.value.spawnPlantName ?: mutableListOf()
                val isDroppingPlants = (spawnPlantList.size == count && spawnPlantList.isNotEmpty())
                val cardTitle = if (isDroppingPlants) "掉落物配置 (植物)" else "掉落物配置 (能量豆)"
                val cardColor =
                    if (isDroppingPlants) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.primary

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Eco,
                                null,
                                tint = cardColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                cardTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = cardColor
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // --- 区域 1: SpawnPlantName 列表管理 ---
                        Text(
                            "指定掉落植物 (SpawnPlantName)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))

                        // 植物列表展示 (FlowRow 或 换行布局)
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            spawnPlantList.forEachIndexed { index, plantType ->
                                InputChip(
                                    selected = true,
                                    onClick = {},
                                    label = { Text(PlantRepository.getName(plantType)) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "删除",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    val newList = spawnPlantList.toMutableList()
                                                    newList.removeAt(index)
                                                    val finalData =
                                                        if (newList.isEmpty()) null else newList
                                                    sync(actionDataState.value.copy(spawnPlantName = finalData))
                                                }
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                                    )
                                )
                            }

                            InputChip(
                                selected = false,
                                onClick = {
                                    onRequestPlantSelection { selectedId ->
                                        val newList = spawnPlantList.toMutableList()
                                        newList.add(selectedId)
                                        sync(actionDataState.value.copy(spawnPlantName = newList))
                                    }
                                },
                                label = { Text("添加植物") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AddCircleOutline,
                                        null,
                                        Modifier.size(18.dp)
                                    )
                                }
                            )
                        }

                        Text(
                            "当掉落植物列表的植物数等于能量豆数量时会变为掉落植物卡片",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val countLabel = if (isDroppingPlants) {
                            "携带上述植物的僵尸数量 (AdditionalPlantFood)"
                        } else {
                            "携带能量豆的僵尸数量 (AdditionalPlantFood)"
                        }

                        NumberInputInt(
                            value = actionDataState.value.additionalPlantFood ?: 0,
                            onValueChange = { newVal ->
                                val finalVal = if (newVal <= 0) null else newVal
                                sync(actionDataState.value.copy(additionalPlantFood = finalVal))
                            },
                            label = countLabel,
                            modifier = Modifier.fillMaxWidth(),
                            color = themeColor
                        )

                        val explainText = if (count > 0) {
                            if (isDroppingPlants)
                                "本波次将有 $count 只僵尸掉落列表中的植物"
                            else
                                "本波次将有 $count 只僵尸携带能量豆"
                        } else {
                            "无额外掉落"
                        }

                        Text(
                            text = explainText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}