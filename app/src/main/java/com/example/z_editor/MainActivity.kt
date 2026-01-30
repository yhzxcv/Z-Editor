package com.example.z_editor

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.z_editor.ui.theme.LocalDarkTheme
import com.example.z_editor.ui.theme.PVZ2LevelEditorTheme
import com.example.z_editor.views.screens.main.AboutScreen
import com.example.z_editor.views.screens.main.EditorScreen
import com.example.z_editor.views.screens.main.LevelListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val configuration = LocalConfiguration.current
            val systemDensity = LocalDensity.current

            val screenWidthDp = configuration.screenWidthDp
            val designWidthDp = 360f

            val targetDensity = (screenWidthDp * systemDensity.density) / (designWidthDp * 1.15f)

            var uiScale by rememberSaveable { mutableFloatStateOf(1.0f) }
            val appDensity = remember(targetDensity, uiScale) {
                Density(
                    density = targetDensity * uiScale,
                    fontScale = 1.0f
                )
            }
            var isDarkTheme by rememberSaveable { mutableStateOf(false) }
            CompositionLocalProvider(
                LocalDensity provides appDensity,
                LocalDarkTheme provides isDarkTheme
            ) {
                PVZ2LevelEditorTheme(darkTheme = isDarkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { isDarkTheme = !isDarkTheme },
                            uiScale = uiScale,
                            onUiScaleChange = { newScale -> uiScale = newScale }
                        )
                    }
                }
            }
        }
    }
}

enum class ScreenState {
    LevelList,
    Editor,
    About
}

@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    uiScale: Float,
    onUiScaleChange: (Float) -> Unit
) {
    var currentScreen by remember { mutableStateOf(ScreenState.LevelList) }
    var currentFileUri by remember { mutableStateOf<Uri?>(null) }
    var currentFileName by remember { mutableStateOf("") }

    AnimatedContent(
        targetState = currentScreen,
        label = "MainNavigationTransition",
        transitionSpec = {
            if (targetState == ScreenState.Editor || targetState == ScreenState.About) {
                (slideInHorizontally { width -> width } + fadeIn())
                    .togetherWith(
                        slideOutHorizontally { width -> -width / 3 } + fadeOut()
                    )
            } else {
                (slideInHorizontally { width -> -width } + fadeIn())
                    .togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
            }
        }
    ) { targetState ->
        when (targetState) {
            ScreenState.LevelList -> {
                LevelListScreen(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    uiScale = uiScale,
                    onUiScaleChange = onUiScaleChange,
                    onLevelClick = { fileName, fileUri ->
                        currentFileName = fileName
                        currentFileUri = fileUri // 保存 Uri
                        currentScreen = ScreenState.Editor
                    },
                    onAboutClick = {
                        currentScreen = ScreenState.About
                    }
                )
            }

            ScreenState.Editor -> {
                EditorScreen(
                    fileUri = currentFileUri,
                    fileName = currentFileName,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onBack = {
                        currentScreen = ScreenState.LevelList
                    }
                )
            }

            ScreenState.About -> {
                AboutScreen(
                    onBack = {
                        currentScreen = ScreenState.LevelList
                    }
                )
            }
        }
    }
}