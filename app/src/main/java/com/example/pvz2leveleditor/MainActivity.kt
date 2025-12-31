package com.example.pvz2leveleditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.pvz2leveleditor.ui.theme.PVZ2LevelEditorTheme
import com.example.pvz2leveleditor.views.screens.EditorScreen
import com.example.pvz2leveleditor.views.screens.LevelListScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PVZ2LevelEditorTheme {
                AppNavigation()
            }
        }
    }
}

enum class ScreenState {
    LevelList, // 列表页
    Editor     // 编辑页
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(ScreenState.LevelList) }
    var currentFileName by remember { mutableStateOf("") }

    AnimatedContent(
        targetState = currentScreen,
        label = "MainNavigationTransition",
        transitionSpec = {
            if (targetState == ScreenState.Editor) {
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
                    onLevelClick = { fileName ->
                        currentFileName = fileName
                        currentScreen = ScreenState.Editor
                    }
                )
            }

            ScreenState.Editor -> {
                EditorScreen(
                    fileName = currentFileName,
                    onBack = {
                        currentScreen = ScreenState.LevelList
                    }
                )
            }
        }
    }
}