package com.example.z_editor.views.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 权限或运行环境不满足时的红色提示卡片。
 *
 * 原本 SmfUnpackerScreen 与 BatchConvertScreen 各有一份逐字节相同的私有实现，
 * 存储设置页会是第三份，所以抽到这里。
 *
 * [buttonLabel] 为 null 时只展示说明、不渲染按钮 —— 用于"系统版本太低"这类
 * 用户无法通过点击解决的情况，调用方传 `onButton = {}` 即可。
 */
@Composable
fun GateCard(
    title: String,
    body: String,
    buttonLabel: String?,
    onButton: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    title, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                body, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onError.copy(alpha = 0.85f)
            )
            if (buttonLabel != null) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onButton,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onError,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) { Text(buttonLabel) }
            }
        }
    }
}
