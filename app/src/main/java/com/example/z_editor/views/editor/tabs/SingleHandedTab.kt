package com.example.z_editor.views.editor.tabs

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.DropWeaponData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.SingleHandedPropertiesData
import com.example.z_editor.data.SpecialWaveData
import com.example.z_editor.data.repository.PlantRepository
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzBlueDarkTheme
import com.example.z_editor.ui.theme.PvzBluePrimary
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.NumberInputDouble
import com.example.z_editor.views.editor.pages.others.NumberInputInt
import rememberJsonSync

/**
 * 单枪匹马小游戏专属 Tab
 */
@Composable
fun SingleHandedTab(
    rootLevelFile: PvzLevelFile?,
    onRequestPlantSelection: ((String) -> Unit) -> Unit
) {
    if (rootLevelFile == null) return

    val obj = remember(rootLevelFile.objects) {
        rootLevelFile.objects.find { it.objClass == "SingleHandedProperties" }
    }

    if (obj == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("数据异常：未找到单枪匹马配置模块", color = Color.Red)
        }
        return
    }

    val syncManager = rememberJsonSync(obj, SingleHandedPropertiesData::class.java)
    val themeColor = PvzBluePrimary
    val focusManager = LocalFocusManager.current

    fun onChange(newData: SingleHandedPropertiesData) {
        // TimeSpeed 无可视化编辑入口：新建模块时默认 1.5，外部手动改过的值保留原样
        syncManager.dataState.value = newData
        syncManager.sync()
    }

    Column(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BaseParamsCard(
            data = syncManager.dataState.value,
            themeColor = themeColor,
            onChange = { onChange(it) }
        )

        Spacer(Modifier.height(16.dp))

        WeaponCard(
            data = syncManager.dataState.value,
            themeColor = themeColor,
            onChange = { onChange(it) },
            onRequestPlantSelection = onRequestPlantSelection
        )

        Spacer(Modifier.height(16.dp))

        SpecialWaveCard(
            data = syncManager.dataState.value,
            themeColor = themeColor,
            onChange = { onChange(it) }
        )

        Spacer(Modifier.height(32.dp))
    }
}

// -----------------------------------------------------------
// 区域 1：基础参数
// -----------------------------------------------------------
@Composable
private fun BaseParamsCard(
    data: SingleHandedPropertiesData,
    themeColor: Color,
    onChange: (SingleHandedPropertiesData) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, null, tint = themeColor)
                Spacer(Modifier.width(12.dp))
                Text(
                    "基础参数",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = themeColor
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInputInt(
                    value = data.missileCount,
                    onValueChange = { onChange(data.copy(missileCount = it)) },
                    label = "单次导弹数量",
                    color = themeColor,
                    modifier = Modifier.weight(1f)
                )
                NumberInputInt(
                    value = data.missileInterval,
                    onValueChange = { onChange(data.copy(missileInterval = it)) },
                    label = "导弹发射间隔",
                    color = themeColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInputInt(
                    value = data.rocketHitTime,
                    onValueChange = { onChange(data.copy(rocketHitTime = it)) },
                    label = "预警时间",
                    color = themeColor,
                    modifier = Modifier.weight(1f)
                )
                NumberInputInt(
                    value = data.rocketSpeed,
                    onValueChange = { onChange(data.copy(rocketSpeed = it)) },
                    label = "导弹速度",
                    color = themeColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            NumberInputDouble(
                value = data.zombiesWalkSpeed,
                onValueChange = { onChange(data.copy(zombiesWalkSpeed = it)) },
                label = "僵尸速度倍率",
                color = themeColor,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            NumberInputDouble(
                value = data.zombiesHitpointsPercent,
                onValueChange = { onChange(data.copy(zombiesHitpointsPercent = it)) },
                label = "僵尸血量倍率",
                color = themeColor,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "特殊波次中的速度/血量会在该值基础上再乘系数",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
            )
        }
    }
}

// -----------------------------------------------------------
// 区域 2：武器配置（初始植物 + 升级武器）
// -----------------------------------------------------------
@Composable
private fun WeaponCard(
    data: SingleHandedPropertiesData,
    themeColor: Color,
    onChange: (SingleHandedPropertiesData) -> Unit,
    onRequestPlantSelection: ((String) -> Unit) -> Unit
) {
    val items = data.dropWeaponDatas
    var editing by remember { mutableStateOf<DropWeaponData?>(null) }
    val listKey = remember { mutableIntStateOf(0) }

    key(editing) {
        if (editing != null) {
            DropWeaponEditDialog(
                item = editing!!,
                themeColor = themeColor,
                onDismiss = { editing = null },
                onConfirm = { updated ->
                    val target = editing
                    editing = null
                    if (target != null) {
                        val idx = items.indexOf(target)
                        if (idx >= 0) {
                            val newList = items.toMutableList()
                            newList[idx] = updated
                            listKey.intValue++
                            onChange(data.copy(dropWeaponDatas = newList))
                        }
                    }
                }
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Yard, null, tint = themeColor)
                Spacer(Modifier.width(12.dp))
                Text(
                    "植物配置",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = themeColor
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "初始植物为开局武器，达到击杀数后自动升级植物。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            InitialWeaponRow(
                weapon = data.initWeapon,
                launchTimePercent = data.initWeaponLaunchTimePercent,
                themeColor = themeColor,
                onPick = {
                    onRequestPlantSelection { selectedId ->
                        onChange(data.copy(initWeapon = selectedId))
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            NumberInputDouble(
                value = data.initWeaponLaunchTimePercent,
                onValueChange = { onChange(data.copy(initWeaponLaunchTimePercent = it)) },
                label = "攻击间隔",
                color = themeColor,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "攻击间隔越小攻速越快",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    val newKillnum = 20 + 40 * items.size
                    onRequestPlantSelection { selectedId ->
                        val newList = items.toMutableList()
                        newList.add(DropWeaponData(weaponname = selectedId, killnum = newKillnum))
                        listKey.intValue++
                        onChange(data.copy(dropWeaponDatas = newList))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.outline,
                    contentColor = if (LocalDarkTheme.current) PvzBlueDarkTheme else themeColor
                ),
                contentPadding = PaddingValues(vertical = 0.dp, horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("添加升级植物", fontSize = 13.sp)
            }

            Spacer(Modifier.height(8.dp))

            if (items.isEmpty()) {
                Text(
                    "暂无升级植物，请添加",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                key(listKey.intValue) {
                    items.forEachIndexed { index, item ->
                        DropWeaponRow(
                            item = item,
                            themeColor = themeColor,
                            onEdit = { editing = item },
                            onDelete = {
                                val newList = items.toMutableList()
                                newList.removeAt(index)
                                listKey.intValue++
                                onChange(data.copy(dropWeaponDatas = newList))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialWeaponRow(
    weapon: String,
    launchTimePercent: Double,
    themeColor: Color,
    onPick: () -> Unit
) {
    val plantInfo = remember(weapon) { PlantRepository.getPlantInfoById(weapon) }
    val displayName = PlantRepository.getName(weapon)
    val iconPath = if (plantInfo?.icon != null) "images/plants/${plantInfo.icon}" else "images/others/unknown.webp"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .clickable { onPick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssetImage(
            path = iconPath,
            contentDescription = displayName,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray)
                .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
            filterQuality = FilterQuality.Medium,
            placeholder = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFBDBDBD), RoundedCornerShape(8.dp)),
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
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "初始植物 · 攻击间隔: $launchTimePercent",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(Icons.Default.Edit, "更换", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DropWeaponRow(
    item: DropWeaponData,
    themeColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val plantInfo = remember(item.weaponname) { PlantRepository.getPlantInfoById(item.weaponname) }
    val displayName = PlantRepository.getName(item.weaponname)
    val iconPath = if (plantInfo?.icon != null) "images/plants/${plantInfo.icon}" else "images/others/unknown.webp"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .clickable { onEdit() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssetImage(
            path = iconPath,
            contentDescription = displayName,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray)
                .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
            filterQuality = FilterQuality.Medium,
            placeholder = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFBDBDBD), RoundedCornerShape(8.dp)),
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
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "击杀: ${item.killnum} · 攻击间隔: ${item.launchtimepercent}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Default.Delete,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun DropWeaponEditDialog(
    item: DropWeaponData,
    themeColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (DropWeaponData) -> Unit
) {
    var tempKillnum by remember { mutableIntStateOf(item.killnum) }
    var tempLaunchTimePercent by remember { mutableDoubleStateOf(item.launchtimepercent) }
    val displayName = PlantRepository.getName(item.weaponname)
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "编辑: $displayName",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
            ) {
                NumberInputInt(
                    value = tempKillnum,
                    onValueChange = { tempKillnum = it },
                    label = "所需击杀数",
                    color = themeColor,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                NumberInputDouble(
                    value = tempLaunchTimePercent,
                    onValueChange = { tempLaunchTimePercent = it },
                    label = "攻击间隔",
                    color = themeColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        item.copy(
                            killnum = tempKillnum,
                            launchtimepercent = tempLaunchTimePercent
                        )
                    )
                }
            ) {
                Text("确定", color = themeColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// -----------------------------------------------------------
// 区域 3：特殊波次
// -----------------------------------------------------------
@Composable
private fun SpecialWaveCard(
    data: SingleHandedPropertiesData,
    themeColor: Color,
    onChange: (SingleHandedPropertiesData) -> Unit
) {
    val items = data.specialWaveDatas
    var editing by remember { mutableStateOf<SpecialWaveData?>(null) }
    val listKey = remember { mutableIntStateOf(0) }

    key(editing) {
        if (editing != null) {
            SpecialWaveEditDialog(
                item = editing!!,
                themeColor = themeColor,
                onDismiss = { editing = null },
                onConfirm = { updated ->
                    val target = editing
                    editing = null
                    if (target != null) {
                        val idx = items.indexOf(target)
                        if (idx >= 0) {
                            val newList = items.toMutableList()
                            newList[idx] = updated
                            listKey.intValue++
                            onChange(data.copy(specialWaveDatas = newList))
                        }
                    }
                }
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = themeColor)
                Spacer(Modifier.width(12.dp))
                Text(
                    "特殊波次",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = themeColor
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "用于放置 Boss 波次，速度/血量系数在此前基础值上再相乘。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    val nextWave = (items.maxOfOrNull { it.wave } ?: 0) + 5
                    val newList = items.toMutableList()
                    newList.add(SpecialWaveData(wave = nextWave))
                    listKey.intValue++
                    onChange(data.copy(specialWaveDatas = newList))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.outline,
                    contentColor = if (LocalDarkTheme.current) PvzBlueDarkTheme else themeColor
                ),
                contentPadding = PaddingValues(vertical = 0.dp, horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("添加特殊波次", fontSize = 13.sp)
            }

            Spacer(Modifier.height(8.dp))

            if (items.isEmpty()) {
                Text(
                    "暂无特殊波次，请添加",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                key(listKey.intValue) {
                    items.forEachIndexed { index, item ->
                        SpecialWaveRow(
                            item = item,
                            themeColor = themeColor,
                            onEdit = { editing = item },
                            onDelete = {
                                val newList = items.toMutableList()
                                newList.removeAt(index)
                                listKey.intValue++
                                onChange(data.copy(specialWaveDatas = newList))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecialWaveRow(
    item: SpecialWaveData,
    themeColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .clickable { onEdit() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("波次 ${item.wave}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (item.showHealthBar) "血条开启" else "血条关闭",
                    fontSize = 12.sp,
                    color = themeColor
                )
            }
            Text(
                "速度 x${item.zombiesWalkSpeed} · 血量 x${item.zombiesHitpointsPercent}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Default.Delete,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SpecialWaveEditDialog(
    item: SpecialWaveData,
    themeColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (SpecialWaveData) -> Unit
) {
    var tempWave by remember { mutableIntStateOf(item.wave) }
    var tempWalkSpeed by remember { mutableDoubleStateOf(item.zombiesWalkSpeed) }
    var tempHitpoints by remember { mutableDoubleStateOf(item.zombiesHitpointsPercent) }
    var tempShowHealthBar by remember { mutableStateOf(item.showHealthBar) }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "特殊波次",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
            ) {
                NumberInputInt(
                    value = tempWave,
                    onValueChange = { tempWave = it },
                    label = "波次",
                    color = themeColor,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputDouble(
                        value = tempWalkSpeed,
                        onValueChange = { tempWalkSpeed = it },
                        label = "速度系数",
                        color = themeColor,
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputDouble(
                        value = tempHitpoints,
                        onValueChange = { tempHitpoints = it },
                        label = "血量系数",
                        color = themeColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "显示血条",
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = tempShowHealthBar,
                        onCheckedChange = { tempShowHealthBar = it },
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
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        item.copy(
                            wave = tempWave,
                            zombiesWalkSpeed = tempWalkSpeed,
                            zombiesHitpointsPercent = tempHitpoints,
                            showHealthBar = tempShowHealthBar
                        )
                    )
                }
            ) {
                Text("确定", color = themeColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
