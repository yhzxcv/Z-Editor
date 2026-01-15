package com.example.z_editor.views.editor.pages.others

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.Point2D
import com.example.z_editor.data.Point3DDouble
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RectData
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.ZombiePropertySheetData
import com.example.z_editor.data.ZombieTypeData
import com.example.z_editor.views.components.AssetImage
import rememberJsonSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomZombiePropertiesEP(
    rtid: String,
    rootLevelFile: PvzLevelFile,
    onBack: () -> Unit,
    scrollState: ScrollState
) {
    val focusManager = LocalFocusManager.current
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    var showHelpDialog by remember { mutableStateOf(false) }


    val typeObj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }

    val typeSyncManager = rememberJsonSync(typeObj, ZombieTypeData::class.java)
    val typeDataState = typeSyncManager.dataState

    val propsAlias = RtidParser.parse(typeDataState.value.properties)?.alias
    val propsObj = rootLevelFile.objects.find { it.aliases?.contains(propsAlias) == true }

    val propsSyncManager = rememberJsonSync(propsObj, ZombiePropertySheetData::class.java)
    val propsDataState = propsSyncManager.dataState

    val resistanceState = remember { mutableStateListOf<Double>() }
    val inputTexts = remember { mutableStateMapOf<Int, String>() }

    LaunchedEffect(Unit) {
        val currentList = typeDataState.value.resistences

        val initialList = if (currentList.isNullOrEmpty()) {
            List(7) { 0.0 }
        } else {
            currentList + List(maxOf(0, 7 - currentList.size)) { 0.0 }
        }
        resistanceState.clear()
        resistanceState.addAll(initialList)

        initialList.forEachIndexed { index, d ->
            inputTexts[index] = d.toString()
        }
    }

    fun sync() {
        val allZero = resistanceState.all { it == 0.0 }
        val newResistances = if (allZero) { null } else {
            ArrayList(resistanceState.map { it.coerceIn(0.0, 1.0) })
        }
        typeDataState.value = typeDataState.value.copy(resistences = newResistances)
        propsSyncManager.sync()
        typeSyncManager.sync()
    }

    var showHitRectDialog by remember { mutableStateOf(false) }
    var showAttackRectDialog by remember { mutableStateOf(false) }
    var showArtCenterDialog by remember { mutableStateOf(false) }
    var showShadowDialog by remember { mutableStateOf(false) }
    var showSizeDialog by remember { mutableStateOf(false) }

    val themeColor = Color(0xFFFF9800)

    if (showHitRectDialog) {
        val current = propsDataState.value.hitRect ?: RectData(10, 10, 32, 95)
        RectEditDialog(
            title = "编辑受击判定 (HitRect)",
            initialData = current,
            onDismiss = { showHitRectDialog = false },
            onConfirm = { newData ->
                propsDataState.value = propsDataState.value.copy(hitRect = newData)
                sync()
                showHitRectDialog = false
            }
        )
    }

    if (showAttackRectDialog) {
        val current = propsDataState.value.attackRect ?: RectData(15, 0, 20, 95)
        RectEditDialog(
            title = "编辑攻击判定 (AttackRect)",
            initialData = current,
            onDismiss = { showAttackRectDialog = false },
            onConfirm = { newData ->
                propsDataState.value = propsDataState.value.copy(attackRect = newData)
                sync()
                showAttackRectDialog = false
            }
        )
    }

    if (showArtCenterDialog) {
        val current = propsDataState.value.artCenter ?: Point2D(90, 125)
        Point2DEditDialog(
            title = "编辑贴图中心 (ArtCenter)",
            initialData = current,
            onDismiss = { showArtCenterDialog = false },
            onConfirm = { newData ->
                propsDataState.value = propsDataState.value.copy(artCenter = newData)
                sync()
                showArtCenterDialog = false
            }
        )
    }

    if (showShadowDialog) {
        val current = propsDataState.value.shadowOffset ?: Point3DDouble(5.0, 0.0, 1.2)
        Point3DEditDialog(
            title = "编辑阴影偏移 (ShadowOffset)",
            initialData = current,
            onDismiss = { showShadowDialog = false },
            onConfirm = { newData ->
                propsDataState.value = propsDataState.value.copy(shadowOffset = newData)
                sync()
                showShadowDialog = false
            }
        )
    }

    if (showSizeDialog) {
        SizeTypeDialog(
            currentValue = propsDataState.value.sizeType,
            onDismiss = { showSizeDialog = false },
            onConfirm = { newVal ->
                propsDataState.value = propsDataState.value.copy(sizeType = newVal)
                sync()
                showSizeDialog = false
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
                    Text(
                        "自定义僵尸通用属性",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
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
                    containerColor = themeColor,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "自定义僵尸基础说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "这里通过关卡内注入自定义的方式，修改僵尸数据的主要参数。不同种僵尸所含有的特殊属性非常多，软件只对一些通用的属性提供解析。"
                )
                HelpSection(
                    title = "基础属性",
                    body = "自定义僵尸可以自由修改其基础属性，包括血量，移动速度和啃食伤害等。自定义的僵尸不会出现在关卡开头的预览池中。"
                )
                HelpSection(
                    title = "部分功能介绍",
                    body = "判定部分中X和Y表示偏移量，W和H分别表示宽度和高度。通过偏移僵尸的贴图中心可以实现隐藏僵尸。在地面轨迹部分可以留空可以让僵尸原地踏步。"
                )
                HelpSection(
                    title = "手动修改",
                    body = "软件实现自定义注入时会从对应的游戏文件中自动填入原僵尸的所有属性，可以在此基础上手动修改 json 文件。"
                )
            }
        }
        if (propsObj == null) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    null,
                    tint = themeColor,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("找不到属性对象", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "在关卡中未找到该自定义僵尸关联的属性对象 ($propsAlias)，表示自定义僵尸的属性定义没有明确指向关卡内部，无法在关卡内更改。",
                    color = Color.Gray,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )
                return@Scaffold
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "基础数值",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = themeColor
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NumberInputDouble(
                        value = propsDataState.value.hitpoints,
                        onValueChange = {
                            propsDataState.value = propsDataState.value.copy(hitpoints = it)
                            sync()
                        },
                        label = "生命值 (Hitpoints)",
                        color = themeColor,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumberInputDouble(
                            value = propsDataState.value.speed,
                            onValueChange = {
                                propsDataState.value = propsDataState.value.copy(speed = it)
                                sync()
                            },
                            label = "移动速度 (Speed)",
                            color = themeColor,
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputDouble(
                            value = propsDataState.value.speedVariance ?: 0.1,
                            onValueChange = {
                                propsDataState.value = propsDataState.value.copy(speedVariance = it)
                                sync()
                            },
                            label = "移速方差 (Variance)",
                            color = themeColor,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    NumberInputDouble(
                        value = propsDataState.value.eatDPS,
                        onValueChange = {
                            propsDataState.value = propsDataState.value.copy(eatDPS = it)
                            sync()
                        },
                        label = "啃食伤害 (EatDPS)",
                        color = themeColor,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val isFractionEnabled = propsDataState.value.armDropFraction != null
                            || propsDataState.value.headDropFraction != null
                    val canBeLaunchedEnabled = propsDataState.value.canBeLaunchedByPlants != null
                            || propsDataState.value.canBePlantTossedStrong != null
                            || propsDataState.value.canBePlantTossedweak != null

                    SwitchRow(
                        title = "关闭临界值 (headDropFraction)",
                        checked = isFractionEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                propsDataState.value = propsDataState.value.copy(
                                    armDropFraction = -1,
                                    headDropFraction = 0
                                )
                            } else {
                                propsDataState.value = propsDataState.value.copy(
                                    armDropFraction = null,
                                    headDropFraction = null
                                )
                            }
                            sync()
                        }
                    )
                    SwitchRow(
                        title = "免疫植物击退 (CanBeLaunchedByPlants)",
                        checked = canBeLaunchedEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                propsDataState.value = propsDataState.value.copy(
                                    canBeLaunchedByPlants = false,
                                    canBePlantTossedweak = false,
                                    canBePlantTossedStrong = false
                                )
                            } else {
                                propsDataState.value = propsDataState.value.copy(
                                    canBeLaunchedByPlants = null,
                                    canBePlantTossedweak = null,
                                    canBePlantTossedStrong = null
                                )
                            }
                            sync()
                        }
                    )
                }
            }

            Text(
                "判定与位置参数",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = themeColor
            )

            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    EditButtonRow(
                        title = "受击判定 (HitRect)",
                        subtitle = formatRect(propsDataState.value.hitRect),
                        icon = Icons.Default.AspectRatio,
                        onClick = { showHitRectDialog = true }
                    )
                    HorizontalDivider()
                    EditButtonRow(
                        title = "攻击判定 (AttackRect)",
                        subtitle = formatRect(propsDataState.value.attackRect),
                        icon = Icons.Default.AspectRatio,
                        onClick = { showAttackRectDialog = true }
                    )
                    HorizontalDivider()
                    EditButtonRow(
                        title = "贴图中心 (ArtCenter)",
                        subtitle = formatPoint(propsDataState.value.artCenter),
                        icon = Icons.Default.CenterFocusStrong,
                        onClick = { showArtCenterDialog = true }
                    )
                    HorizontalDivider()
                    EditButtonRow(
                        title = "阴影偏移 (ShadowOffset)",
                        subtitle = formatPoint3D(propsDataState.value.shadowOffset),
                        icon = Icons.Default.Layers,
                        onClick = { showShadowDialog = true }
                    )
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    var groundExpanded by remember { mutableStateOf(false) }
                    val groundOptions = listOf("ground_swatch", "")

                    val currentGroundLabel = when (propsDataState.value.groundTrackName) {
                        "ground_swatch" -> "普通地面 (ground_swatch)"
                        "" -> "无 (Empty)"
                        else -> propsDataState.value.groundTrackName
                    }
                    ExposedDropdownMenuBox(
                        expanded = groundExpanded,
                        onExpandedChange = { groundExpanded = !groundExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentGroundLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("行进轨迹 (GroundTrackName)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groundExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                focusedLabelColor = themeColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = groundExpanded,
                            onDismissRequest = { groundExpanded = false }
                        ) {
                            groundOptions.forEach { option ->
                                val label =
                                    if (option == "ground_swatch") "普通地面 (ground_swatch)" else "无 (null)"
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        propsDataState.value =
                                            propsDataState.value.copy(groundTrackName = option)
                                        sync()
                                        groundExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Text(
                "外观与行为设置",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = themeColor
            )

            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSizeDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "体型大小 (SizeType)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                propsDataState.value.sizeType ?: "null",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                        Icon(Icons.Default.Edit, null, tint = Color.Gray)
                    }
                    HorizontalDivider()

                    SwitchRow(
                        title = "受伤显示血条 (EnableShowHealthBar)",
                        checked = propsDataState.value.enableShowHealthBarByDamage == true,
                        onCheckedChange = { checked ->
                            val newVal = if (checked) true else null
                            val newTime = if (checked) (propsDataState.value.drawHealthBarTime
                                ?: 4.0) else null
                            propsDataState.value = propsDataState.value.copy(
                                enableShowHealthBarByDamage = newVal,
                                drawHealthBarTime = newTime
                            )
                            sync()
                        }
                    )

                    AnimatedVisibility(
                        visible = propsDataState.value.enableShowHealthBarByDamage == true,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        NumberInputDouble(
                            value = propsDataState.value.drawHealthBarTime ?: 4.0,
                            onValueChange = {
                                propsDataState.value =
                                    propsDataState.value.copy(drawHealthBarTime = it)
                                sync()
                            },
                            label = "血条显示时间 (DrawHealthBarTime)",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp)
                        )
                    }

                    SwitchRow(
                        title = "开启精英缩放 (EnableEliteScale)",
                        checked = propsDataState.value.enableEliteScale == true,
                        onCheckedChange = { checked ->
                            val newVal = if (checked) true else null
                            val newScale =
                                if (checked) (propsDataState.value.eliteScale ?: 1.2) else null
                            propsDataState.value = propsDataState.value.copy(
                                enableEliteScale = newVal,
                                eliteScale = newScale
                            )
                            sync()
                        }
                    )

                    AnimatedVisibility(
                        visible = propsDataState.value.enableEliteScale == true,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        NumberInputDouble(
                            value = propsDataState.value.eliteScale ?: 1.2,
                            onValueChange = {
                                propsDataState.value = propsDataState.value.copy(eliteScale = it)
                                sync()
                            },
                            label = "缩放比例 (EliteScale)",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp)
                        )
                    }

                    SwitchRow(
                        title = "开启精英免疫 (EnableEliteImmunities)",
                        checked = propsDataState.value.enableEliteImmunities == true,
                        onCheckedChange = { checked ->
                            val newVal = if (checked) true else null
                            propsDataState.value =
                                propsDataState.value.copy(enableEliteImmunities = newVal)
                            sync()
                        }
                    )

                    SwitchRow(
                        title = "能否携带能量豆 (CanSpawnPlantFood)",
                        checked = propsDataState.value.canSpawnPlantFood,
                        onCheckedChange = {
                            propsDataState.value = propsDataState.value.copy(canSpawnPlantFood = it)
                            sync()
                        }
                    )

                    SwitchRow(
                        title = "可在游戏结束时投降 (CanSurrender)",
                        checked = propsDataState.value.canSurrender == true,
                        onCheckedChange = { checked ->
                            val newVal = if (checked) true else null
                            propsDataState.value = propsDataState.value.copy(canSurrender = newVal)
                            sync()
                        }
                    )

                    SwitchRow(
                        title = "可食脑判负 (CanTriggerZombieWin)",
                        checked = propsDataState.value.canTriggerZombieWin != false,
                        onCheckedChange = { checked ->
                            val newVal = if (checked) null else false
                            propsDataState.value =
                                propsDataState.value.copy(canTriggerZombieWin = newVal)
                            sync()
                        }
                    )
                }
            }

            Text(
                "僵尸抗性设置 (Resistences)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = themeColor
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    @Composable
                    fun ResistanceInput(
                        index: Int,
                        label: String,
                        iconPath: String?,
                        modifier: Modifier = Modifier
                    ) {
                        val textValue = inputTexts.getOrElse(index) {
                            resistanceState.getOrElse(index) { 0.0 }.toString()
                        }
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { str ->
                                inputTexts[index] = str
                                val num = str.toDoubleOrNull()
                                if (num != null) {
                                    val clamped = num.coerceIn(0.0, 1.0)
                                    resistanceState[index] = clamped
                                    sync()
                                }
                            },

                            label = { Text(label, fontSize = 11.sp) },
                            leadingIcon = if (iconPath != null) {
                                {
                                    AssetImage(
                                        path = iconPath,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else null,
                            modifier = modifier,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColor,
                                focusedLabelColor = themeColor
                            )
                        )
                    }
                    ResistanceInput(
                        index = 0,
                        label = "即死抗性 (受到秒杀攻击的免疫概率)",
                        iconPath = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val labels = listOf(
                        "物理抗性 (Physics)", "毒液抗性 (Poison)",
                        "电能抗性 (Electric)", "魔法抗性 (Magic)",
                        "寒冰抗性 (Ice)", "火焰抗性 (Fire)"
                    )
                    val icons = listOf(
                        "images/tags/Plant_Physics.png", "images/tags/Plant_Poison.png",
                        "images/tags/Plant_Electric.png", "images/tags/Plant_Magic.png",
                        "images/tags/Plant_Ice.png", "images/tags/Plant_Fire.png"
                    )

                    for (i in 0 until 3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val idx1 = 1 + i * 2
                            val idx2 = 1 + i * 2 + 1

                            ResistanceInput(
                                index = idx1,
                                label = labels.getOrElse(i * 2) { "Res $idx1" },
                                iconPath = icons.getOrElse(i * 2) { null },
                                modifier = Modifier.weight(1f)
                            )

                            ResistanceInput(
                                index = idx2,
                                label = labels.getOrElse(i * 2 + 1) { "Res $idx2" },
                                iconPath = icons.getOrElse(i * 2 + 1) { null },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text(
                        "数值范围 0.0 - 1.0，0.0 表示无影响，1.0 表示完全免疫",
                        fontSize = 11.sp, color = Color.Gray
                    )
                }
            }

            Text(
                "僵尸所属种类: ${typeDataState.value.typeName}",
                fontSize = 14.sp, color = Color.Gray
            )
            Text(
                "属性链接代号: $propsAlias",
                fontSize = 14.sp, color = Color.Gray
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// === 辅助组件 ===
@Composable
fun EditButtonRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFFFF9800))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Icon(Icons.Default.Edit, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// === 弹窗实现 ===

@Composable
fun RectEditDialog(
    title: String,
    initialData: RectData,
    onDismiss: () -> Unit,
    onConfirm: (RectData) -> Unit
) {
    var mX by remember { mutableIntStateOf(initialData.mX) }
    var mY by remember { mutableIntStateOf(initialData.mY) }
    var mWidth by remember { mutableIntStateOf(initialData.mWidth) }
    var mHeight by remember { mutableIntStateOf(initialData.mHeight) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputInt(mX, { mX = it }, "X", modifier = Modifier.weight(1f))
                    NumberInputInt(mY, { mY = it }, "Y", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputInt(mWidth, { mWidth = it }, "Width", modifier = Modifier.weight(1f))
                    NumberInputInt(
                        mHeight,
                        { mHeight = it },
                        "Height",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(RectData(mX, mY, mWidth, mHeight)) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun Point2DEditDialog(
    title: String,
    initialData: Point2D,
    onDismiss: () -> Unit,
    onConfirm: (Point2D) -> Unit
) {
    var x by remember { mutableIntStateOf(initialData.x) }
    var y by remember { mutableIntStateOf(initialData.y) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInputInt(x, { x = it }, "X", modifier = Modifier.weight(1f))
                NumberInputInt(y, { y = it }, "Y", modifier = Modifier.weight(1f))
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(Point2D(x, y)) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun Point3DEditDialog(
    title: String,
    initialData: Point3DDouble,
    onDismiss: () -> Unit,
    onConfirm: (Point3DDouble) -> Unit
) {
    var x by remember { mutableDoubleStateOf(initialData.x) }
    var y by remember { mutableDoubleStateOf(initialData.y) }
    var z by remember { mutableDoubleStateOf(initialData.z) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInputDouble(x, { x = it }, "X")
                NumberInputDouble(y, { y = it }, "Y")
                NumberInputDouble(z, { z = it }, "Z")
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(Point3DDouble(x, y, z)) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun SizeTypeDialog(
    currentValue: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    val options = listOf(null, "small", "mid", "large")
    var selected by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择体型") },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = option }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == selected),
                            onClick = { selected = option }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(option ?: "null", fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

fun formatRect(rect: RectData?) =
    if (rect == null) "默认" else "X:${rect.mX}, Y:${rect.mY}, W:${rect.mWidth}, H:${rect.mHeight}"

fun formatPoint(pt: Point2D?) = if (pt == null) "默认" else "X:${pt.x}, Y:${pt.y}"
fun formatPoint3D(pt: Point3DDouble?) =
    if (pt == null) "默认" else "X:${pt.x}, Y:${pt.y}, Z:${pt.z}"
