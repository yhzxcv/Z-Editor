package com.example.z_editor.views.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract

/**
 * `ACTION_OPEN_DOCUMENT_TREE`，但允许指定初始位置。
 *
 * 没用框架自带的 `ActivityResultContracts.OpenDocumentTree`：那个只在 API 26+ 接受
 * initialUri，而这里还要显式带上读写与持久化标志。
 *
 * `input` 传 null 时退回 AOSP 的内部存储根。注意那是 **AOSP 的 authority**
 * （非 AOSP ROM 上初始位置会失效，不影响已授予的 tree URI）。
 *
 * 原先定义在 `LevelListScreen.kt` 里；数据包层也要用，就挪到了公共组件包，
 * 免得 `datapack.ui` 反向 import 一个 screen 包里的类。
 */
class OpenDocumentTreeFixed : ActivityResultContract<Uri?, Uri?>() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        if (input != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, input)
        } else {
            val primaryRootUri =
                Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A")
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, primaryRootUri)
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data else null
    }
}
