package com.example.z_editor.views.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * 创建一个防抖动的点击回调。
 * @param waitMs 防抖时间，默认 500ms，在此期间内的重复点击将被忽略。
 * @param onClick 实际要执行的点击逻辑。
 */
@Composable
fun rememberDebouncedClick(
    waitMs: Long = 500L,
    onClick: () -> Unit
): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }

    return remember(onClick, waitMs) {
        {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > waitMs) {
                lastClickTime = currentTime
                onClick()
            }
        }
    }
}