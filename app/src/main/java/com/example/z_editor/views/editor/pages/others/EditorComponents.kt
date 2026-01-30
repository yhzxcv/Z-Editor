package com.example.z_editor.views.editor.pages.others

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.EventRegistry
import com.example.z_editor.data.LevelParser
import com.example.z_editor.data.PvzObject
import com.example.z_editor.data.RtidParser
import com.example.z_editor.data.ZombieSpawnData
import com.example.z_editor.data.repository.ZombieRepository
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.views.components.AssetImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonEditorTopAppBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    themeColor: Color = MaterialTheme.colorScheme.primary,
    onHelpClick: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit) = {}
) {
    TopAppBar(
        title = {
            if (subtitle == null) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        actions = {
            if (onHelpClick != null) {
                IconButton(onClick = onHelpClick) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "帮助说明",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = themeColor,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        )
    )
}

@Composable
fun EventChip(rtid: String, objectMap: Map<String, PvzObject>, onClick: () -> Unit) {
    val alias = LevelParser.extractAlias(rtid)
    val obj = objectMap[alias]
    val isInvalid = obj == null

    val isDark = LocalDarkTheme.current
    val meta = EventRegistry.getMetadata(obj?.objClass)

    val bgColor = when {
        isInvalid -> MaterialTheme.colorScheme.onError
        meta != null -> if (isDark) meta.darkColor else meta.color
        else -> Color(0xFF9E9E9E)
    }

    val summaryText = if (!isInvalid) {
        meta?.summaryProvider?.invoke(obj)
    } else null

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isInvalid) {
                Icon(
                    Icons.Default.ErrorOutline,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
            }

            Text(
                text = alias,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (summaryText != null) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = summaryText,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun SettingEntryCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.rotation(180f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun Modifier.rotation(degrees: Float) = this.then(Modifier.graphicsLayer(rotationZ = degrees))

@Composable
fun NumberInputInt(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    color: Color = Color.Blue,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() || it == '-' }
            text = filtered
            val num = filtered.toIntOrNull()
            if (num != null) {
                onValueChange(num)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            cursorColor = color,
            selectionColors = TextSelectionColors(
                handleColor = color,
                backgroundColor = color.copy(alpha = 0.4f)
            ),
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedBorderColor = color,
            focusedLabelColor = color
        ),
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
    )
}


@Composable
fun NumberInputDouble(
    value: Double,
    onValueChange: (Double) -> Unit,
    label: String,
    color: Color = Color.Blue,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) {
        val parsed = text.toDoubleOrNull()
        if (parsed != value) {
            val isEditingSpecialChar = text.isEmpty() || text == "-" || text == "."
            if (!isEditingSpecialChar) {
                text = value.toString()
            }
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input
            val num = input.toDoubleOrNull()
            if (num != null) {
                onValueChange(num)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            cursorColor = color,
            selectionColors = TextSelectionColors(
                handleColor = color,
                backgroundColor = color.copy(alpha = 0.4f)
            ),
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedBorderColor = color,
            focusedLabelColor = color
        ),
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
    )
}

@Composable
fun LaneRow(
    laneLabel: String,
    laneColor: Color,
    zombies: List<ZombieSpawnData>,
    objectMap: Map<String, PvzObject>,
    onAddClick: () -> Unit,
    onZombieClick: (ZombieSpawnData) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(14.dp)
                    .background(laneColor, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(8.dp))
            Text(laneLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Text("${zombies.size} 只", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(zombies) { zombie ->
                CompactZombieCard(
                    zombie = zombie,
                    objectMap = objectMap,
                    onClick = { onZombieClick(zombie) }
                )
            }
            item {
                CompactAddButton(onClick = onAddClick, color = laneColor)
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(0.2f))
    }
}

@Composable
fun CompactZombieCard(
    zombie: ZombieSpawnData,
    objectMap: Map<String, PvzObject>,
    onClick: () -> Unit
) {
    val resolverResult = remember(zombie.type, objectMap) {
        ZombieRepository.resolveZombieType(zombie.type, objectMap)
    }
    val realTypeName = resolverResult.first
    val isValid = resolverResult.second

    val parsedRtid = remember(zombie.type) { RtidParser.parse(zombie.type) }
    val isCustom = parsedRtid?.source == "CurrentLevel"
    val alias = parsedRtid?.alias ?: "Unknown"

    val displayName = if (isCustom) alias else ZombieRepository.getName(realTypeName)

    val info = remember(realTypeName) {
        ZombieRepository.getZombieInfoById(realTypeName)
    }

    val isElite = zombie.isElite
    val level = zombie.level ?: 0

    val placeholderContent = @Composable {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isValid) Color(0xFFE0E0E0) else Color(0xFFFFEBEE)),
            contentAlignment = Alignment.Center
        ) {
            if (!isValid) {
                Icon(
                    Icons.Default.ErrorOutline,
                    null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = displayName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isValid) Color.White else MaterialTheme.colorScheme.error)
            .border(if (!isValid) 0.5.dp else 0.dp, MaterialTheme.colorScheme.onError, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        if (isValid) {
            AssetImage(
                path = if (info?.icon != null) "images/zombies/${info.icon}" else null,
                contentDescription = displayName,
                modifier = Modifier.fillMaxSize(),
                filterQuality = FilterQuality.Medium,
                placeholder = placeholderContent
            )
        } else {
            placeholderContent()
        }
        if (isValid) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(
                        color = when {
                            isCustom -> MaterialTheme.colorScheme.onTertiary
                            isElite -> MaterialTheme.colorScheme.surfaceTint
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        shape = RoundedCornerShape(bottomStart = 6.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = when {
                        isElite -> "E"
                        isCustom -> "C"
                        else -> "$level"
                    },
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CompactAddButton(onClick: () -> Unit, color: Color) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, "添加", tint = color, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun ZombieEditSheetContent(
    originalZombie: ZombieSpawnData,
    objectMap: Map<String, PvzObject>,
    onValueChange: (ZombieSpawnData) -> Unit,
    onCopy: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onInjectCustom: (String) -> Unit,
    onEditCustom: (String) -> Unit,
    compatibleCustomZombies: List<Pair<String, String>> = emptyList(),
    onSelectExistingCustom: ((String) -> Unit)? = null
) {
    // === 核心解析逻辑 ===
    val resolverResult = remember(originalZombie.type, objectMap) {
        ZombieRepository.resolveZombieType(originalZombie.type, objectMap)
    }
    val baseTypeName = resolverResult.first
    val isValid = resolverResult.second
    val rtidInfo = remember(originalZombie.type) { RtidParser.parse(originalZombie.type) }
    val isCustom = rtidInfo?.source == "CurrentLevel"
    var showSwapDialog by remember { mutableStateOf(false) }

    val alias = rtidInfo?.alias ?: originalZombie.type
    val displayName = if (isCustom) alias else ZombieRepository.getName(baseTypeName)
    val subtitle = if (isCustom) "基于: $baseTypeName" else baseTypeName
    val info = remember(baseTypeName) {
        ZombieRepository.getZombieInfoById(baseTypeName)
    }

    val isElite = originalZombie.isElite

    val placeholderContent = @Composable {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isValid) Color(0xFFEEEEEE) else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isValid) {
                Text(
                    text = displayName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 24.sp
                )
            } else {
                Icon(
                    Icons.Default.ErrorOutline,
                    null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetImage(
                path = if (isValid && info?.icon != null) "images/zombies/${info.icon}" else null,
                contentDescription = displayName,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(
                        if (!isValid) 1.dp else 0.dp,
                        MaterialTheme.colorScheme.onError,
                        RoundedCornerShape(8.dp)
                    ),
                filterQuality = FilterQuality.Medium,
                placeholder = placeholderContent
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isValid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onError
                    )
                    if (isCustom) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.onTertiary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                "自定义",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (isElite) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceTint, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
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

                if (!isValid) {
                    Text("引用失效 (找不到定义)", fontSize = 12.sp, color =  MaterialTheme.colorScheme.onError)
                } else {
                    Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onCopy != null) {
                Button(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.outline,
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("复制单位")
                }
            }

            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("删除单位")
            }
        }

        if (isValid) {
            if (isElite) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("僵尸等级", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text("Elite", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.surfaceTint)
                }
            } else {
                StepperControl(
                    label = "僵尸等级",
                    valueText = "${originalZombie.level ?: 0}",
                    onMinus = {
                        val current = originalZombie.level ?: 0
                        val newLevel = (current - 1).coerceAtLeast(0)
                        onValueChange(originalZombie.copy(level = if (newLevel == 0) null else newLevel))
                    },
                    onPlus = {
                        val current = originalZombie.level ?: 0
                        val newLevel = (current + 1).coerceAtMost(10)
                        onValueChange(originalZombie.copy(level = if (newLevel == 0) null else newLevel))
                    }
                )
            }
        }
        val currentRow = originalZombie.row ?: 0
        StepperControl(
            label = "所在行",
            valueText = if (currentRow == 0) "随机行" else "第 $currentRow 行",
            onMinus = {
                val newRow = when (currentRow) {
                    0 -> 5
                    1 -> 0
                    else -> currentRow - 1
                }
                onValueChange(originalZombie.copy(row = if (newRow == 0) null else newRow))
            },
            onPlus = {
                val newRow = when (currentRow) {
                    0 -> 1
                    5 -> 0
                    else -> currentRow + 1
                }
                onValueChange(originalZombie.copy(row = if (newRow == 0) null else newRow))
            }
        )
        if (compatibleCustomZombies.isNotEmpty() && onSelectExistingCustom != null) {
            Button(
                onClick = { showSwapDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("切换至已定义的同类僵尸 (${compatibleCustomZombies.size})")
            }
        }

        if (isCustom) {
            Button(
                onClick = { onEditCustom(originalZombie.type) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onTertiary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Build, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("编辑自定义属性数值")
            }
        } else {
            Button(
                onClick = { onInjectCustom(alias) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onTertiary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Build, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("注入为自定义僵尸")
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showSwapDialog) {
        AlertDialog(
            onDismissRequest = { showSwapDialog = false },
            title = { Text("选择自定义僵尸") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "检测到关卡内已有基于 \"$displayName\" 的自定义僵尸，点击即可替换：",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    compatibleCustomZombies.forEach { (alias, rtid) ->
                        Card(
                            onClick = {
                                onSelectExistingCustom?.invoke(rtid)
                                showSwapDialog = false
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(alias, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.weight(1f))
                                if (originalZombie.type == rtid) {
                                    Text("当前使用", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSwapDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
fun StepperControl(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 12.dp)
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onMinus) {
            Icon(
                Icons.Default.Remove,
                null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Box(modifier = Modifier.width(60.dp), contentAlignment = Alignment.Center) {
            Text(
                text = valueText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onPlus) {
            Icon(
                Icons.Default.Add,
                null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 通用的帮助信息弹窗组件
 */
@Composable
fun EditorHelpDialog(
    title: String,
    onDismiss: () -> Unit,
    themeColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(vertical = 24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        titleContentColor = themeColor,
        iconContentColor = themeColor,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, null)
                Spacer(Modifier.width(8.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("我明白了", color = themeColor)
            }
        }
    )
}

/**
 * 帮助弹窗中的一个小章节组件
 */
@Composable
fun HelpSection(title: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "• $title",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}