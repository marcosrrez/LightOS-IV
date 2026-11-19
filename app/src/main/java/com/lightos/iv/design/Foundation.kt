package com.lightos.iv.design

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

/** 4dp grid and layout tokens. DESIGN.md §4. */
object Space {
    val s4 = 4.dp
    val s8 = 8.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s24 = 24.dp
    val s32 = 32.dp
    val s48 = 48.dp
    val s64 = 64.dp

    val gutter = 28.dp
    val gutterTool = 24.dp
    val row = 56.dp
    val wordGap = 4.dp
    val action = 48.dp
}

/** The only two radii in the system. */
object Shapes {
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val pill = RoundedCornerShape(percent = 50)
}

/** Every animation in the app comes from these four tokens. DESIGN.md §5. */
object Motion {
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val StandardDecelerate = CubicBezierEasing(0f, 0f, 0f, 1f)

    fun <T> move(reduced: Boolean = false): AnimationSpec<T> =
        if (reduced) tween(120) else spring(dampingRatio = 0.86f, stiffness = 380f)

    fun <T> snap(reduced: Boolean = false): AnimationSpec<T> =
        if (reduced) tween(80) else spring(dampingRatio = 0.92f, stiffness = 900f)

    fun <T> fadeFast(): AnimationSpec<T> = tween(120, easing = StandardDecelerate)
    fun <T> fadeBase(): AnimationSpec<T> = tween(220, easing = EmphasizedDecelerate)

    /** Release velocity (dp/s) above which a flick commits regardless of distance. */
    const val FLICK_DP_PER_S = 700f
}

/**
 * Three haptic moments, using system effects that honor the owner's haptic setting.
 * DESIGN.md §6. Navigation inside LightOS stays silent.
 */
class Haptics(private val view: View) {
    fun tick() {
        val c = if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        else HapticFeedbackConstants.CLOCK_TICK
        view.performHapticFeedback(c)
    }

    fun commit() {
        val c = if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.VIRTUAL_KEY
        view.performHapticFeedback(c)
    }

    fun hold() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun reject() {
        val c = if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT
        else HapticFeedbackConstants.LONG_PRESS
        view.performHapticFeedback(c)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
