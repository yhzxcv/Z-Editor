package com.example.pvz2leveleditor.views.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pvz2leveleditor.data.repository.PlantCategory
import com.example.pvz2leveleditor.data.repository.PlantInfo
import com.example.pvz2leveleditor.data.repository.PlantRepository
import com.example.pvz2leveleditor.data.repository.PlantTag
import com.example.pvz2leveleditor.views.components.AssetImage

@Composable
fun PlantSelectionScreen(
    onPlantSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf(PlantTag.All) }
    var selectedCategory by remember { mutableStateOf(PlantCategory.Quality) }

    val currentVisibleTags = remember(selectedCategory) {
        listOf(PlantTag.All) + PlantTag.entries.filter {
            it.category == selectedCategory && it != PlantTag.All
        }
    }

    val displayList = remember(searchQuery, selectedTag) {
        PlantRepository.search(searchQuery, selectedTag)
    }

    LaunchedEffect(selectedCategory) {
        if (!currentVisibleTags.contains(selectedTag)) {
            selectedTag = currentVisibleTags.first()
        }
    }

    val themeColor = Color(0xFF8BC34A)

    Scaffold(
        topBar = {
            Surface(
                color = themeColor,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(bottom = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        Spacer(Modifier.width(16.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "搜索植物名称或代码",
                                    fontSize = 16.sp,
                                    color = Color.Gray
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = themeColor
                            ),
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                    }) { Icon(Icons.Default.Clear, null, tint = Color.Gray) }
                                }
                            } else null,
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        PlantCategory.entries.forEach { category ->
                            val isSelected = selectedCategory == category

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        if (selectedCategory != category) {
                                            selectedCategory = category
                                            selectedTag = PlantTag.All
                                        }
                                    }
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent
                                    )
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = category.label,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    ScrollableTabRow(
                        selectedTabIndex = currentVisibleTags.indexOf(selectedTag).coerceAtLeast(0),
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        edgePadding = 16.dp,
                        indicator = { tabPositions ->
                            val index = currentVisibleTags.indexOf(selectedTag).coerceAtLeast(0)
                            if (index < tabPositions.size) {
                                SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[index]),
                                    color = Color.White,
                                    height = 3.dp
                                )
                            }
                        },
                        divider = {}
                    ) {
                        currentVisibleTags.forEach { tag ->
                            Tab(
                                selected = selectedTag == tag,
                                onClick = { selectedTag = tag },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (tag.iconName != null) {
                                            AssetImage(
                                                path = "images/tags/${tag.iconName}",
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                placeholder = {}
                                            )
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = tag.label,
                                            fontWeight = if (selectedTag == tag) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    }
                                },
                                unselectedContentColor = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
        ) {
            if (displayList.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Search,
                        null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("未找到相关植物", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 64.dp),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayList) { plant ->
                        PlantGridItem(plant = plant) {
                            onPlantSelected(plant.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlantGridItem(plant: PlantInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AssetImage(
            path = if (plant.icon != null) "images/plants/${plant.icon}" else null,
            contentDescription = plant.name,
            filterQuality = FilterQuality.High,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), CircleShape),
            placeholder = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = plant.name.take(1),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = plant.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            maxLines = 1,
            color = Color.Black,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Text(
            text = plant.id,
            fontSize = 8.sp,
            color = Color.Gray,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            lineHeight = 10.sp
        )
    }
}