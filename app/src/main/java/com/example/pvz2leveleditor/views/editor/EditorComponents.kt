package com.example.pvz2leveleditor.views.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pvz2leveleditor.data.EventRegistry
import com.example.pvz2leveleditor.data.LevelParser
import com.example.pvz2leveleditor.data.PvzObject
import com.example.pvz2leveleditor.data.RtidParser
import com.example.pvz2leveleditor.data.repository.ZombiePropertiesRepository
import com.example.pvz2leveleditor.data.repository.ZombieRepository
import com.example.pvz2leveleditor.data.ZombieSpawnData
import com.example.pvz2leveleditor.data.repository.ZombieTag
import com.example.pvz2leveleditor.views.components.AssetImage

@Composable
fun EventChip(rtid: String, objectMap: Map<String, PvzObject>, onClick: () -> Unit) {
    val alias = LevelParser.extractAlias(rtid)
    val obj = objectMap[alias]
    val isInvalid = obj == null

    // 1. 直接获取元数据
    val meta = EventRegistry.getMetadata(obj?.objClass)

    // 2. 决定颜色：失效红，未知灰，已知取配置
    val bgColor = when {
        isInvalid -> Color(0xFFD32F2F)
        meta != null -> meta.color
        else -> Color(0xFF9E9E9E) // 未知类型的默认色
    }

    // 3. 获取摘要文字 (例如 "5 僵尸")
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

            Text(alias, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

            // 如果有摘要，显示摘要胶囊
            if (summaryText != null) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(summaryText, color = Color.White, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun SettingEntryCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF2E7D32))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.rotation(180f),
                tint = Color.LightGray
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
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            // 过滤非数字字符
            val filtered = input.filter { it.isDigit() || it == '-' }
            text = filtered
            val num = filtered.toIntOrNull()
            if (num != null) {
                onValueChange(num)
            }
        },
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
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input
            val num = input.toDoubleOrNull()
            if (num != null) {
                onValueChange(num)
            }
        },
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
    onAddClick: () -> Unit,
    onZombieClick: (ZombieSpawnData) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
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
            Text(laneLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
            Spacer(Modifier.weight(1f))
            Text("${zombies.size} 只", fontSize = 11.sp, color = Color.Gray)
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(zombies) { zombie ->
                CompactZombieCard(zombie = zombie, onClick = { onZombieClick(zombie) })
            }
            item {
                CompactAddButton(onClick = onAddClick, color = laneColor)
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
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
fun CompactZombieCard(zombie: ZombieSpawnData, onClick: () -> Unit) {
    val realTypeName = remember(zombie.type) {
        val alias = RtidParser.parse(zombie.type)?.alias ?: zombie.type
        ZombiePropertiesRepository.getTypeNameByAlias(alias)
    }
    val info = remember(realTypeName) {
        ZombieRepository.search(realTypeName, ZombieTag.All).firstOrNull()
    }
    val displayName = info?.name ?: realTypeName
    val isElite = zombie.isElite
    val level = zombie.level ?: 1

    val placeholderContent = @Composable {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayName.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(0.5.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        AssetImage(
            path = if (info?.icon != null) "images/zombies/${info.icon}" else null,
            contentDescription = displayName,
            modifier = Modifier.fillMaxSize(),
            filterQuality = FilterQuality.Medium,
            placeholder = placeholderContent
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(
                    color = if (isElite) Color(0xFF673AB7)
                    else Color.Gray,
                    shape = RoundedCornerShape(bottomStart = 6.dp)
                )
                .padding(horizontal = 5.dp)
        ) {
            Text(
                text = if (isElite) "E" else "$level",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ZombieEditSheetContent(
    originalZombie: ZombieSpawnData,
    onValueChange: (ZombieSpawnData) -> Unit,
    onCopy: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    val realTypeName = remember(originalZombie.type) {
        val alias = RtidParser.parse(originalZombie.type)?.alias ?: originalZombie.type
        ZombiePropertiesRepository.getTypeNameByAlias(alias)
    }
    val displayName = ZombieRepository.getName(realTypeName)
    val info = remember(realTypeName) {
        ZombieRepository.search(realTypeName, ZombieTag.All).firstOrNull()
    }
    val isElite = originalZombie.isElite

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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
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
            Spacer(Modifier.width(12.dp))
            Column {
                Text(displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (isElite) {
                    Text(
                        "精英僵尸",
                        fontSize = 14.sp,
                        color = Color(0xFF673AB7),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(realTypeName, fontSize = 14.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.weight(1f))
            if (onCopy != null) {
                Button(
                    onClick = onCopy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE3F2FD), // 浅蓝色背景
                        contentColor = Color(0xFF1976D2)    // 深蓝色文字/图标
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("复制")
                }
            }
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFEBEE),
                    contentColor = Color.Red
                ),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("删除")
            }
        }

        HorizontalDivider()

        if (isElite) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("僵尸等级", fontSize = 16.sp, color = Color.Gray)
                Spacer(Modifier.weight(1f))
                Text("Elite", fontWeight = FontWeight.Bold, color = Color(0xFF673AB7))
            }
        } else {
            StepperControl(
                label = "僵尸等级",
                valueText = "${originalZombie.level ?: 1}",
                onMinus = {
                    val current = originalZombie.level ?: 1
                    val newLevel = (current - 1).coerceAtLeast(1)
                    onValueChange(originalZombie.copy(level = newLevel))
                },
                onPlus = {
                    val current = originalZombie.level ?: 1
                    val newLevel = (current + 1).coerceAtMost(10)
                    onValueChange(originalZombie.copy(level = newLevel))
                }
            )
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

        Spacer(Modifier.height(16.dp))
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
            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 12.dp)
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onMinus) { Icon(Icons.Default.Remove, null, tint = Color.Black) }
        Box(modifier = Modifier.width(60.dp), contentAlignment = Alignment.Center) {
            Text(
                text = valueText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF5D4037)
            )
        }
        IconButton(onClick = onPlus) { Icon(Icons.Default.Add, null, tint = Color.Black) }
    }
}

/**
 * 通用的帮助信息弹窗组件
 */
@Composable
fun EditorHelpDialog(
    title: String,
    onDismiss: () -> Unit,
    themeColor: Color = Color(0xFF1976D2),
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(vertical = 24.dp), // 防止太高
        containerColor = Color.White,
        titleContentColor = themeColor,
        iconContentColor = themeColor,
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
                // 这里渲染传入的具体内容
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
            color = Color.Black.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            fontSize = 13.sp,
            color = Color.Gray,
            lineHeight = 18.sp,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}