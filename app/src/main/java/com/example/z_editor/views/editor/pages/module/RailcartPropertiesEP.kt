package com.example.z_editor.views.editor.pages.module

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.RailData
import com.example.z_editor.data.RailcartData
import com.example.z_editor.data.RailcartPropertiesData
import com.example.z_editor.data.RtidParser
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzBrownDark
import com.example.z_editor.ui.theme.PvzBrownLight
import com.example.z_editor.ui.theme.PvzGridBorder
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.CommonEditorTopAppBar
import com.example.z_editor.views.editor.pages.others.EditorHelpDialog
import com.example.z_editor.views.editor.pages.others.HelpSection
import com.google.gson.Gson

private val gson = Gson()

enum class RailEditMode {
    Rails,
    Carts
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RailcartPropertiesEP(
    rtid: String,
    onBack: () -> Unit,
    rootLevelFile: PvzLevelFile,
    scrollState: ScrollState
) {
    val currentAlias = RtidParser.parse(rtid)?.alias ?: ""
    var showHelpDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val moduleDataState = remember {
        val obj = rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }
        val data = try {
            gson.fromJson(obj?.objData, RailcartPropertiesData::class.java)
        } catch (_: Exception) {
            RailcartPropertiesData()
        }
        mutableStateOf(data)
    }

    var editMode by remember { mutableStateOf(RailEditMode.Rails) }

    val railsGrid = remember {
        val grid = Array(9) { BooleanArray(5) }
        moduleDataState.value.rails.forEach { rail ->
            for (r in rail.rowStart..rail.rowEnd) {
                if (rail.column in 0..8 && r in 0..4) {
                    grid[rail.column][r] = true
                }
            }
        }
        mutableStateListOf<BooleanArray>().apply {
            grid.forEach { add(it.clone()) }
        }
    }

    val cartSet = remember {
        mutableStateListOf<String>().apply {
            moduleDataState.value.railcarts.forEach { add("${it.column},${it.row}") }
        }
    }

    fun sync() {
        val newRails = mutableListOf<RailData>()
        for (c in 0..8) {
            var start: Int? = null
            for (r in 0..4) {
                val hasRail = railsGrid[c][r]
                if (hasRail) {
                    if (start == null) start = r
                } else {
                    if (start != null) {
                        newRails.add(RailData(column = c, rowStart = start, rowEnd = r - 1))
                        start = null
                    }
                }
            }
            if (start != null) {
                newRails.add(RailData(column = c, rowStart = start, rowEnd = 4))
            }
        }

        val newCarts = cartSet.map {
            val parts = it.split(",")
            RailcartData(column = parts[0].toInt(), row = parts[1].toInt())
        }.toMutableList()

        val newData = moduleDataState.value.copy(
            rails = newRails,
            railcarts = newCarts
        )
        moduleDataState.value = newData

        rootLevelFile.objects.find { it.aliases?.contains(currentAlias) == true }?.let {
            it.objData = gson.toJsonTree(newData)
        }
    }

    fun handleGridClick(col: Int, row: Int) {
        if (editMode == RailEditMode.Rails) {
            val newColArray = railsGrid[col].clone()
            newColArray[row] = !newColArray[row]
            railsGrid[col] = newColArray
            sync()
        } else {
            val key = "$col,$row"
            if (cartSet.contains(key)) {
                cartSet.remove(key)
            } else {
                cartSet.add(key)
            }
            sync()
        }
    }

    var typeExpanded by remember { mutableStateOf(false) }

    val cartTypeOptions = listOf(
        "railcart_cowboy" to "西部矿车 (railcart_cowboy)",
        "railcart_future" to "未来矿车 (railcart_future)",
        "railcart_egypt" to "埃及矿车 (railcart_egypt)",
        "railcart_pirate" to "海盗矿车 (railcart_pirate)",
        "railcart_worldcup" to "世界杯矿车 (railcart_worldcup)",
    )

    val currentTypeDisplay =
        cartTypeOptions.find { it.first == moduleDataState.value.railcartType }?.second
            ?: moduleDataState.value.railcartType

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzBrownDark else PvzBrownLight

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            CommonEditorTopAppBar(
                title = "矿车轨道设置",
                themeColor = themeColor,
                onBack = onBack,
                onHelpClick = { showHelpDialog = true }
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            EditorHelpDialog(
                title = "矿车轨道模块说明",
                onDismiss = { showHelpDialog = false },
                themeColor = themeColor
            ) {
                HelpSection(
                    title = "简要介绍",
                    body = "在此可以放置矿车与轨道的位置，设定矿车的款式。点击一次格点进行放置，再次点击进行删除。"
                )
                HelpSection(
                    title = "轨道铺设",
                    body = "在轨道铺设模式下点击网格铺设轨道。编辑器会自动将同一列连续的格子合并为一段轨道数据。"
                )
                HelpSection(
                    title = "矿车放置",
                    body = "点击网格放置或移除矿车。注意在同一段轨道上的矿车容易被堆叠起来。"
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = !typeExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentTypeDisplay,
                            onValueChange = {
                                moduleDataState.value =
                                    moduleDataState.value.copy(railcartType = it)
                                sync()
                            },
                            label = { Text("矿车类型 (RailcartType)") },
                            readOnly = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedBorderColor = themeColor,
                                focusedLabelColor = themeColor
                            ),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            cartTypeOptions.forEach { (code, display) ->
                                DropdownMenuItem(
                                    text = { Text(display) },
                                    onClick = {
                                        moduleDataState.value =
                                            moduleDataState.value.copy(railcartType = code)
                                        sync()
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (editMode == RailEditMode.Rails) themeColor else Color.Transparent)
                                .clickable { editMode = RailEditMode.Rails },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AddRoad,
                                    null,
                                    tint = if (editMode == RailEditMode.Rails) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "铺设轨道",
                                    color = if (editMode == RailEditMode.Rails) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (editMode == RailEditMode.Carts) themeColor else Color.Transparent)
                                .clickable { editMode = RailEditMode.Carts },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Inbox,
                                    null,
                                    tint = if (editMode == RailEditMode.Carts) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "放置矿车",
                                    color = if (editMode == RailEditMode.Carts) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.8f)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, PvzGridBorder, RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0xFF503C34) else Color(0xFFD7CCC8))
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.8f)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, PvzGridBorder, RoundedCornerShape(6.dp))
                        .background(if (isDark) Color(0xFF503C34) else Color(0xFFD7CCC8))
                ) {
                    Column(Modifier.fillMaxSize()) {
                        for (r in 0..4) {
                            Row(Modifier.weight(1f)) {
                                for (c in 0..8) {
                                    val hasRail = railsGrid[c][r]
                                    val hasCart = cartSet.contains("$c,$r")

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(
                                                0.5.dp,
                                                PvzGridBorder
                                            )
                                            .clickable { handleGridClick(c, r) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (hasRail) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.8f)
                                                    .fillMaxHeight()
                                                    .background(PvzBrownLight.copy(alpha = 0.3f))
                                            ) {
                                                AssetImage(
                                                    path = "images/others/rails.png",
                                                    contentDescription = "Railcart",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                )
                                            }
                                        }

                                        if (hasCart) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize(0.8f)
                                                    .clip(RoundedCornerShape(0.dp))
                                                    .background(Color.Transparent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AssetImage(
                                                    path = "images/others/railcarts.png",
                                                    contentDescription = "Railcart",
                                                    modifier = Modifier
                                                        .fillMaxSize()
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

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "轨道段数: ${moduleDataState.value.rails.size}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "矿车数量: ${moduleDataState.value.railcarts.size}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = {
                            for (c in 0..8) {
                                railsGrid[c] = BooleanArray(5)
                            }
                            cartSet.clear()
                            sync()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("清空所有配置", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}