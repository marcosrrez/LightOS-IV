package com.lightos.iv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lightos.iv.command.CommandPanel
import com.lightos.iv.design.Motion
import com.lightos.iv.design.Space
import com.lightos.iv.design.Whisper
import com.lightos.iv.home.ConfirmSheet
import com.lightos.iv.home.HomeLayer
import com.lightos.iv.home.SheetLayer
import com.lightos.iv.settings.SettingsScreen
import com.lightos.iv.tools.FocusScreen
import com.lightos.iv.tools.NotesScreen
import com.lightos.iv.tools.TodayScreen

/**
 * Layer order, bottom to top: home (+ drawer) → tool screen → command panel → sheet →
 * confirm → whisper. BackHandlers are registered in the same order, so back always closes
 * the topmost open layer first, and on the bare home screen it does nothing.
 */
@Composable
fun LightOsRoot(vm: LightOsViewModel) {
    val screen by vm.screen.collectAsState()
    val whisper by vm.whisper.collectAsState()

    BackHandler(enabled = true) { vm.popScreen() }

    Box(Modifier.fillMaxSize()) {
        HomeLayer(vm)

        var lastTool by remember { mutableStateOf(Screen.Notes) }
        if (screen != Screen.Home) lastTool = screen
        AnimatedVisibility(
            visible = screen != Screen.Home,
            enter = fadeIn(Motion.fadeBase()) + slideInVertically(Motion.fadeBase()) { it / 32 },
            exit = fadeOut(Motion.fadeFast()) + slideOutVertically(Motion.fadeFast()) { it / 48 },
        ) {
            when (lastTool) {
                Screen.Notes -> NotesScreen(vm)
                Screen.Focus -> FocusScreen(vm)
                Screen.Today -> TodayScreen(vm)
                Screen.Settings -> SettingsScreen(vm)
                Screen.Home -> Unit
            }
        }

        CommandPanel(vm)
        SheetLayer(vm)
        ConfirmSheet(vm)

        Whisper(
            text = whisper?.text,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = Space.gutter)
                .padding(bottom = Space.row + Space.s24),
        )
    }
}
