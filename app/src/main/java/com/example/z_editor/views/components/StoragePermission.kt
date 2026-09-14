package com.example.z_editor.views.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.z_editor.data.repository.LevelRepository

/**
 * 「所有文件访问」（MANAGE_EXTERNAL_STORAGE）是否已授予，并在每次 ON_RESUME 重新判定
 * —— 系统设置页不像 Activity 那样返回结果，只能靠回前台重查。
 *
 * 判定口径统一走 [LevelRepository.hasRawStoragePermission]，避免出现界面说"已开启"、
 * 实际写入却走不通的错位。
 */
@Composable
fun rememberManageStorageGranted(): Boolean {
    var granted by remember { mutableStateOf(LevelRepository.hasRawStoragePermission()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = LevelRepository.hasRawStoragePermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return granted
}

/**
 * 打开「所有文件访问」的系统设置页。返回 false 表示三个入口都打不开，调用方应改用
 * 文案提示用户手动去系统设置里开启。
 *
 * 依次尝试厂商实现的应用级页面、系统全局列表页，最后退回应用详情页。
 * 原来的实现只把第一个 startActivity 包在 runCatching 里，**回退那一句是裸的** ——
 * 两个 activity 都不存在的 ROM（部分第三方 ROM、鸿蒙）上点一下就崩。
 */
fun openManageAllFilesSettings(context: Context): Boolean {
    val packageUri = Uri.parse("package:${context.packageName}")
    val candidates = listOf(
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).setData(packageUri),
        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(packageUri)
    )
    return candidates.any { intent ->
        runCatching { context.startActivity(intent) }.isSuccess
    }
}
