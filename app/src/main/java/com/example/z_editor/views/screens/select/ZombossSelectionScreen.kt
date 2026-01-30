package com.example.z_editor.views.screens.select

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.repository.ZombossInfo
import com.example.z_editor.data.repository.ZombossRepository
import com.example.z_editor.data.repository.ZombossTag
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PvzPurpleDark
import com.example.z_editor.ui.theme.PvzPurpleLight
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.components.rememberDebouncedClick

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun ZombossSelectionScreen(
    onSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val handleBack = rememberDebouncedClick { onBack() }
    BackHandler(onBack = handleBack)

    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf(ZombossTag.All) }
    val focusManager = LocalFocusManager.current

    val displayList = remember(searchQuery, selectedTag) {
        ZombossRepository.search(searchQuery, selectedTag)
    }

    val isDark = LocalDarkTheme.current
    val themeColor = if (isDark) PvzPurpleDark else PvzPurpleLight

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            Surface(color = themeColor, shadowElevation = 4.dp) {
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
                        IconButton(onClick = handleBack, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.surface)
                        }
                        Spacer(Modifier.width(16.dp))

                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text("搜索僵王名称或代号", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(25.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = themeColor
                            ),
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                    }) { Icon(Icons.Default.Clear, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                            } else null,
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )
                    }

                    ScrollableTabRow(
                        selectedTabIndex = ZombossTag.entries.indexOf(selectedTag),
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.surface,
                        edgePadding = 16.dp,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[ZombossTag.entries.indexOf(selectedTag)]),
                                color = MaterialTheme.colorScheme.surface,
                                height = 3.dp
                            )
                        },
                        divider = {}
                    ) {
                        ZombossTag.entries.forEach { tag ->
                            val configuration = LocalConfiguration.current
                            val screenWidth = configuration.screenWidthDp.dp
                            Tab(
                                modifier = Modifier.width(screenWidth / 4),
                                selected = selectedTag == tag,
                                onClick = { selectedTag = tag },
                                text = {
                                    Text(
                                        text = tag.label,
                                        fontWeight = if (selectedTag == tag) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                },
                                unselectedContentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
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
                .background(MaterialTheme.colorScheme.surface)
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
                    Text("未找到匹配的僵王", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayList) { boss ->
                        ZombossItemCard(boss = boss) { onSelected(boss.id) }
                    }
                }
            }
        }
    }
}


@Composable
fun ZombossItemCard(boss: ZombossInfo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssetImage(
                path = "images/zombies/${boss.icon}",
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray)
                    .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(12.dp)),
                filterQuality = FilterQuality.Medium,
                placeholder = {
                    Box(Modifier.fillMaxSize().background(Color.LightGray)) {
                        Text(boss.name.take(1), Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.surface)
                    }
                }
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(boss.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(boss.id, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}