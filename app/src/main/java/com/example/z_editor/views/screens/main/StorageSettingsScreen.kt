package com.example.z_editor.views.screens.main

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import com.example.z_editor.R
import com.example.z_editor.data.repository.LevelRepository
import com.example.z_editor.data.repository.StorageMode
import com.example.z_editor.views.components.GateCard
import com.example.z_editor.views.components.OpenDocumentTreeFixed
import com.example.z_editor.views.components.openManageAllFilesSettings
import com.example.z_editor.views.components.rememberDebouncedClick
import com.example.z_editor.views.components.rememberManageStorageGranted

/**
 * 存储设置页：显式选择走 SAF 还是直接写入，并给「所有文件访问」权限一个入口。
 *
 * 背景：部分第三方 provider（鸿蒙实机已确认）能列目录却拒绝一切写入。应用已声明
 * MANAGE_EXTERNAL_STORAGE，直写模式会把 SAF 的 document id 换算回真实路径、用
 * `java.io.File` 直接读写，完全绕开 provider。
 *
 * **不变量：`folder_uri` 永远存 `content://` 树 URI。** 直写只是运行时的换算，切模式
 * 不需要重选目录；一旦把 `file://` 写进 prefs，`takePersistableUriPermission` 就失去
 * 意义，四个数据包页面读同一个键也会被带坏。
 *
 * `context.getSharedPreferences` 直读直写 —— 项目没有 DI，沿用 LevelListScreen 的约定。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val themeColor = MaterialTheme.colorScheme.primary
    val handleBack = rememberDebouncedClick { onBack() }
    BackHandler(onBack = handleBack)

    val prefs = remember {
        context.getSharedPreferences(LevelRepository.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var storedUriString by remember {
        mutableStateOf(prefs.getString(KEY_FOLDER_URI, null))
    }
    val storedUri = storedUriString?.toUri()

    // 系统设置页不返回结果，权限靠回前台重查（与两个数据包页面同一套实现）。
    val hasPermission = rememberManageStorageGranted()

    var mode by remember { mutableStateOf(LevelRepository.readStorageMode(context)) }
    // 权限被撤销时把持久化模式归一回 SAF，否则开关会停在"勾选但不可用"的错位状态。
    LaunchedEffect(hasPermission) {
        if (!hasPermission && LevelRepository.readStorageMode(context) == StorageMode.Raw) {
            LevelRepository.writeStorageMode(context, StorageMode.Saf)
            mode = StorageMode.Saf
        }
    }

    val isUnsupported = Build.VERSION.SDK_INT < Build.VERSION_CODES.R
    val effectiveMode = LevelRepository.normalizeStorageMode(mode, hasPermission)

    fun toast(resId: Int) {
        Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentTreeFixed()
    ) { uri ->
        if (uri != null) {
            // 先要读写，拿不到就退而求其次只读 —— 只授读的 ROM 上这一步会抛。
            // 都失败也必须 toast 而不是崩溃，且**不能**把 uri 写进 prefs：
            // 没有持久化权限的 uri 重启后就是死的。
            val taken = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }.isSuccess || runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }.isSuccess

            if (taken) {
                prefs.edit { putString(KEY_FOLDER_URI, uri.toString()) }
                storedUriString = uri.toString()
                toast(R.string.storage_settings_toast_folder_changed)
            } else {
                toast(R.string.storage_settings_toast_pick_failed)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.storage_settings_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.storage_settings_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColor,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 权限卡片排在开关上方：没有权限时开关是灰的，得先看到原因才知道该做什么。
            if (isUnsupported) {
                GateCard(
                    title = stringResource(R.string.storage_settings_unsupported_title),
                    body = stringResource(R.string.storage_settings_unsupported_body),
                    buttonLabel = null,
                    onButton = {}
                )
                Spacer(Modifier.height(12.dp))
            } else if (!hasPermission) {
                GateCard(
                    title = stringResource(R.string.storage_settings_permission_title),
                    body = stringResource(R.string.storage_settings_permission_body),
                    buttonLabel = stringResource(R.string.storage_settings_permission_button),
                    onButton = {
                        if (!openManageAllFilesSettings(context)) {
                            toast(R.string.storage_settings_toast_no_settings_app)
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
            }

            InfoSectionCard(title = stringResource(R.string.storage_settings_mode_title)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                if (effectiveMode == StorageMode.Saf) {
                                    R.string.storage_settings_mode_saf_title
                                } else {
                                    R.string.storage_settings_mode_raw_title
                                }
                            ),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                if (effectiveMode == StorageMode.Saf) {
                                    R.string.storage_settings_mode_saf_summary
                                } else {
                                    R.string.storage_settings_mode_raw_summary
                                }
                            ),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = effectiveMode == StorageMode.Saf,
                        enabled = hasPermission,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = themeColor,
                            checkedBorderColor = Color.Transparent,

                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        onCheckedChange = { useSaf ->
                            if (useSaf) {
                                LevelRepository.writeStorageMode(context, StorageMode.Saf)
                                mode = StorageMode.Saf
                            } else {
                                val target = storedUri
                                if (target == null || LevelRepository.isRawPathUsable(target)) {
                                    LevelRepository.writeStorageMode(context, StorageMode.Raw)
                                    mode = StorageMode.Raw
                                } else {
                                    toast(R.string.storage_settings_toast_raw_unmappable)
                                }
                            }
                        }
                    )
                }

                if (isUnsupported || !hasPermission) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.storage_settings_mode_disabled_hint),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            InfoSectionCard(title = stringResource(R.string.storage_settings_folder_title)) {
                val displayName = storedUri
                    ?.let { LevelRepository.directoryDisplayName(context, it) }
                    ?.takeIf { it.isNotBlank() }
                Text(
                    text = displayName ?: stringResource(R.string.storage_settings_folder_none),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (effectiveMode == StorageMode.Raw) {
                    val rawPath =
                        storedUri?.let { LevelRepository.rawDirectoryUri(context, it)?.path }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(
                            R.string.storage_settings_raw_path_label,
                            rawPath ?: stringResource(R.string.storage_settings_raw_path_unknown)
                        ),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        // 只把 content:// 交给选择器当初始位置 —— file:// 会让它打不开。
                        folderPickerLauncher.launch(
                            storedUri?.takeIf { it.scheme == LevelRepository.SCHEME_CONTENT }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.storage_settings_folder_reselect))
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

/** `folder_uri` 这个键在 LevelListScreen 与四个数据包页面里都是字面量，此处沿用同一个。 */
private const val KEY_FOLDER_URI = "folder_uri"
