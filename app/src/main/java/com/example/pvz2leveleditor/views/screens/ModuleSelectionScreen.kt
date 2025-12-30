package com.example.pvz2leveleditor.views.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pvz2leveleditor.data.ModuleMetadata
import com.example.pvz2leveleditor.data.ModuleRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleSelectionScreen(
    existingObjClasses: Set<String>,
    onModuleSelected: (ModuleMetadata) -> Unit,
    onBack: () -> Unit
) {
    val allModules = remember { ModuleRegistry.getAllKnownModules() }
    val themeColor = Color(0xFF388E3C)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加关卡模块", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = themeColor)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allModules.entries.toList()) { (objClass, meta) ->
                    val isAlreadyAdded = existingObjClasses.contains(objClass)

                    ModuleSelectionCard(
                        meta = meta,
                        isAlreadyAdded = isAlreadyAdded,
                        onClick = { if (!isAlreadyAdded) onModuleSelected(meta) }
                    )
                }
            }
        }
    }
}

@Composable
fun ModuleSelectionCard(
    meta: ModuleMetadata,
    isAlreadyAdded: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isAlreadyAdded) 0.6f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isAlreadyAdded, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isAlreadyAdded) Color(0xFFEEEEEE) else Color.White
        ),
        elevation = CardDefaults.cardElevation(if (isAlreadyAdded) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isAlreadyAdded) Color.Gray.copy(alpha = 0.1f)
                        else Color(0xFF388E3C).copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = meta.icon,
                    contentDescription = null,
                    tint = if (isAlreadyAdded) Color.Gray else Color(0xFF388E3C),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = meta.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isAlreadyAdded) Color.Gray else Color.Black
                    )
                }
                Text(
                    text = meta.description,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
            }

            if (isAlreadyAdded) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "已添加",
                    tint = Color(0xFF388E3C),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}