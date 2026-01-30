package com.example.z_editor.views.editor.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.z_editor.data.LocationData
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.ZombossBattleIntroData
import com.example.z_editor.data.ZombossBattleModuleData
import com.example.z_editor.data.repository.ZombossRepository
import com.example.z_editor.views.components.AssetImage
import com.example.z_editor.views.editor.pages.others.StepperControl
import com.google.gson.Gson

private val gson = Gson()

@Composable
fun ZombossBattleTab(
    rootLevelFile: PvzLevelFile?,
    onLaunchZombossSelector: ((String) -> Unit) -> Unit
) {
    if (rootLevelFile == null) return

    val battleObj = remember(rootLevelFile.objects) {
        rootLevelFile.objects.find { it.objClass == "ZombossBattleModuleProperties" }
    }
    val introObj = remember(rootLevelFile.objects) {
        rootLevelFile.objects.find { it.objClass == "ZombossBattleIntroProperties" }
    }

    if (battleObj == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("数据异常：未找到僵王战配置模块", color = Color.Red)
        }
        return
    }

    val battleDataState = remember(battleObj) {
        val initialData = try {
            gson.fromJson(battleObj.objData, ZombossBattleModuleData::class.java)
        } catch (_: Exception) {
            ZombossBattleModuleData()
        }
        mutableStateOf(initialData)
    }
    val introDataState = remember(introObj) {
        val initialData = if (introObj != null) {
            try {
                gson.fromJson(introObj.objData, ZombossBattleIntroData::class.java)
            } catch (_: Exception) {
                ZombossBattleIntroData()
            }
        } else null
        mutableStateOf(initialData)
    }

    fun sync() {
        battleObj.objData = gson.toJsonTree(battleDataState.value)
        if (introObj != null && introDataState.value != null) {
            introObj.objData = gson.toJsonTree(introDataState.value)
        }
    }

    val currentBossInfo = ZombossRepository.get(battleDataState.value.zombossMechType)
    val themeColor = MaterialTheme.colorScheme.onSurfaceVariant

    fun onZombossChanged(newType: String) {
        val newInfo = ZombossRepository.get(newType) ?: return
        val newPhaseCount = newInfo.defaultPhaseCount
        val newSpawnPosition: LocationData? = when (newType) {
            "zombossmech_pvz1_robot_hard", "zombossmech_pvz1_robot_normal", "zombossmech_pvz1_robot_1",
            "zombossmech_pvz1_robot_2", "zombossmech_pvz1_robot_3", "zombossmech_pvz1_robot_4",
            "zombossmech_pvz1_robot_5", "zombossmech_pvz1_robot_6", "zombossmech_pvz1_robot_7",
            "zombossmech_pvz1_robot_8", "zombossmech_pvz1_robot_9" -> null

            "zombossmech_iceage", "zombossmech_eighties", "zombossmech_renai", "zombossmech_modern_iceage",
            "zombossmech_modern_eighties", "zombossmech_iceage_vacation", "zombossmech_eighties_vacation",
            "zombossmech_iceage_12th", "zombossmech_eighties_12th", "zombossmech_renai_12th" -> LocationData(
                6,
                4
            )

            else -> LocationData(6, 3)
        }

        battleDataState.value = battleDataState.value.copy(
            zombossMechType = newType,
            zombossStageCount = newPhaseCount,
            zombossSpawnGridPosition = newSpawnPosition
        )

        if (introDataState.value != null) {
            introDataState.value = introDataState.value!!.copy(
                zombossPhaseCount = newPhaseCount
            )
        }
        sync()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (introObj == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onError),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "模块缺失警告",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "关卡未检测到僵王战转场模块，请添加僵王战转场模块后重新选择僵王。",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onError,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Text(
            "僵王类型",
            fontWeight = FontWeight.Bold,
            color = themeColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable {
                    onLaunchZombossSelector { newType ->
                        onZombossChanged(newType)
                    }
                },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssetImage(
                    path = if (currentBossInfo != null) "images/zombies/${currentBossInfo.icon}" else null,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(12.dp)),
                    filterQuality = FilterQuality.Medium,
                    placeholder = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = "?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentBossInfo?.name ?: "未知僵王",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = battleDataState.value.zombossMechType,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(Icons.Default.Edit, "更改", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "关卡参数",
            fontWeight = FontWeight.Bold,
            color = themeColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        StepperControl(
            label = "植物预留列",
            valueText = "${battleDataState.value.reservedColumnCount} 列",
            onMinus = {
                val newVal = (battleDataState.value.reservedColumnCount - 1).coerceAtLeast(0)
                battleDataState.value = battleDataState.value.copy(reservedColumnCount = newVal)
                sync()
            },
            onPlus = {
                val newVal = (battleDataState.value.reservedColumnCount + 1).coerceAtMost(9)
                battleDataState.value = battleDataState.value.copy(reservedColumnCount = newVal)
                sync()
            }
        )
        Text(
            "表示右边预留不能种植植物的列数，通常预留两列以上",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
        )
    }
}