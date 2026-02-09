package com.example.z_editor.views.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * 创建一个防抖动的点击回调。
 * @param waitMs 防抖时间，默认 500ms，在此期间内的重复点击将被忽略。
 * @param onClick 实际要执行的点击逻辑。
 */
@Composable
fun rememberDebouncedClick(
    waitMs: Long = 300L,
    onClick: () -> Unit
): () -> Unit {
    val currentOnClick by rememberUpdatedState(onClick)
    val lastClickTime = remember { object { var value = 0L } }

    return remember {
        {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime.value > waitMs) {
                lastClickTime.value = currentTime
                currentOnClick()
            }
        }
    }
}