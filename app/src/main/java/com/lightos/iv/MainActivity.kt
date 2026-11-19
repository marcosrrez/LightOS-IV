package com.lightos.iv

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.lightos.iv.design.Light
import com.lightos.iv.design.LightTheme

/**
 * The single HOME activity. Edge-to-edge with transparent bars (never immersive, so the
 * system home/back gestures work on the first swipe). Pressing Home while LightOS is in
 * front arrives here as [onNewIntent] and returns everything to the resting screen.
 */
class MainActivity : ComponentActivity() {
    private val vm: LightOsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            val surface by vm.surface.collectAsState()
            LightTheme(surface) {
                val light = Light.palette.isLight
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = light
                        isAppearanceLightNavigationBars = light
                    }
                }
                LightOsRoot(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) vm.goHome()
    }

    override fun onStart() {
        super.onStart()
        vm.onShown()
    }

    override fun onStop() {
        super.onStop()
        vm.onHidden()
    }
}
