package com.example.z_editor.datapack.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
internal const val DISABLED_ALPHA = 0.38f
@Composable
internal fun BusyBackHandler(
    busy: Boolean,
    busyMessage: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    BackHandler {
        if (busy) {
            Toast.makeText(context, busyMessage, Toast.LENGTH_SHORT).show()
        } else {
            onBack()
        }
    }
}
