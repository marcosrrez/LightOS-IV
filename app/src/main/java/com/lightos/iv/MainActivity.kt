package com.lightos.iv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.lightos.iv.ui.LightOsRoot
import com.lightos.iv.ui.theme.LightOsTheme
import com.lightos.iv.ui.theme.Surface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val vm = ViewModelProvider(this)[LightOsViewModel::class.java]

        setContent {
            val surface by vm.surface.collectAsState()
            LightOsTheme(surface) {
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    val lightBars = surface == Surface.Paper || surface == Surface.Amber
                    controller.isAppearanceLightStatusBars = lightBars
                    controller.isAppearanceLightNavigationBars = lightBars
                }
                LightOsRoot(vm)
            }
        }

        // A launcher never exits on back; it pops its own screen stack instead.
        onBackPressedDispatcher.addCallback(this) { vm.onBack() }
    }
}
