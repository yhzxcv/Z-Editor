package com.example.pvz2leveleditor.views.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

@Composable
fun AssetImage(
    path: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    filterQuality: FilterQuality = FilterQuality.High,
    placeholder: @Composable (() -> Unit) = {
        Image(
            painter = rememberVectorPainter(Icons.Default.QuestionMark),
            contentDescription = "Placeholder"
        )
    }
) {
    val context = LocalContext.current

    // 使用 remember 缓存 Bitmap，避免重组时重复解码
    val imageBitmap: ImageBitmap? = remember(path) {
        if (path.isNullOrBlank()) {
            null
        } else {
            try {
                context.assets.open(path).use { inputStream ->
                    val options = BitmapFactory.Options().apply {
                        inScaled = false

                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    }

                    val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    bitmap?.asImageBitmap()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            filterQuality = filterQuality
        )
    } else {
        Box(modifier = modifier) {
            placeholder()
        }
    }
}