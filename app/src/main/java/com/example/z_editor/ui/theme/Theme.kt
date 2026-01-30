package com.example.z_editor.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 定义暗色方案
private val DarkColorScheme = darkColorScheme(
    primary = PvzGreenDarkTheme,
    onPrimary = Color.Black,
    primaryContainer = PvzGreenDark,
    onPrimaryContainer = Color.White,

    secondary = PvzBlueDarkTheme,
    onSecondary = Color.Black,

    background = DarkBackground,
    onBackground = DarkOnSurface,

    surface = DarkSurface,
    onSurface = DarkOnSurface,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,

    error = DarkErrorSurface,
    onError = DarkErrorOnSurface,
    tertiary = DarkWarningSurface,
    onTertiary = DarkWarningOnSurface,
    outline = DarkBlueBg,
    outlineVariant = DarkGreenBg,
    scrim = PvzDarkScrim,
    surfaceTint = DarkPurpleTag,
)

// 定义亮色方案
private val LightColorScheme = lightColorScheme(
    primary = PvzGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = PvzGreenContainer,
    onPrimaryContainer = PvzGreenDark,

    secondary = PvzBluePrimary,
    onSecondary = Color.White,

    background = LightBackground,
    onBackground = LightOnSurface,

    surface = LightSurface,
    onSurface = LightOnSurface,

    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,

    error = LightErrorSurface,
    onError = LightErrorOnSurface,
    tertiary = LightWarningSurface,
    onTertiary = LightWarningOnSurface,
    outline = LightBlueBg,
    outlineVariant = LightGreenBg,
    scrim = Color.White,
    surfaceTint = LightPurpleTag,
)

val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun PVZ2LevelEditorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
            content = content
        )
    }
}