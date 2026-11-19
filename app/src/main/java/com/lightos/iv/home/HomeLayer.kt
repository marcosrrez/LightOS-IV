package com.lightos.iv.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.lightos.iv.HomeSignal
import com.lightos.iv.LightOsViewModel
import com.lightos.iv.Screen
import com.lightos.iv.Sheet
import com.lightos.iv.data.HomeSlot
import com.lightos.iv.drawer.Drawer
import com.lightos.iv.design.LText
import com.lightos.iv.design.Light
import com.lightos.iv.design.Motion
import com.lightos.iv.design.Space
import com.lightos.iv.design.pressable
import com.lightos.iv.design.rememberHaptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

/** Window-space bounds → android.graphics.Rect for the clip-reveal launch animation. */
fun Rect.toAndroid() = android.graphics.Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())

/**
 * Home + drawer as one physical surface. `progress` is 0 at rest and 1 with the drawer
 * fully up; every drag writes it directly (1:1) and every release settles it with
 * `spring.move`, starting from the release velocity.
 */
@Composable
fun HomeLayer(vm: LightOsViewModel) {
    val density = LocalDensity.current
    val reduced = Light.reduceMotion
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fullH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val flick = with(density) { Motion.FLICK_DP_PER_S.dp.toPx() }
        val shadePull = with(density) { 56.dp.toPx() }

        var progress by remember { mutableFloatStateOf(0f) }
        var settleJob by remember { mutableStateOf<Job?>(null) }
        var drawerReset by remember { mutableIntStateOf(0) }
        var downPull by remember { mutableFloatStateOf(0f) }

        fun dragBy(dyDown: Float) {
            settleJob?.cancel()
            progress = (progress - dyDown / fullH).coerceIn(0f, 1f)
        }

        fun settle(velocityDown: Float, force: Float? = null) {
            val target = force ?: when {
                velocityDown < -flick -> 1f
                velocityDown > flick -> 0f
                progress > 0.4f -> 1f
                else -> 0f
            }
            settleJob?.cancel()
            settleJob = scope.launch {
                animate(progress, target, -velocityDown / fullH, Motion.move(reduced)) { v, _ -> progress = v }
            }
        }

        LaunchedEffect(Unit) {
            vm.homeSignals.collect { signal ->
                when (signal) {
                    HomeSignal.GoHome -> settle(0f, force = 0f)
                    HomeSignal.Hidden -> {
                        settleJob?.cancel()
                        progress = 0f
                        drawerReset++
                    }
                }
            }
        }

        BackHandler(enabled = progress > 0.001f) { settle(0f, force = 0f) }

        val homeDrag = rememberDraggableState { dy ->
            if (progress == 0f && dy > 0f) downPull += dy else dragBy(dy)
        }

        // ---- Home ----
        Box(
            Modifier
                .fillMaxSize()
                .draggable(
                    orientation = Orientation.Vertical,
                    state = homeDrag,
                    onDragStarted = { settleJob?.cancel(); downPull = 0f },
                    onDragStopped = { v ->
                        if (progress == 0f && (downPull > shadePull || (downPull > 0f && v > flick))) {
                            expandNotificationShade(context)
                        } else settle(v)
                        downPull = 0f
                    },
                )
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { vm.openSheet(Sheet.HomeMenu) })
                }
                .graphicsLayer {
                    val p = progress.coerceIn(0f, 1f)
                    alpha = (1f - p * 1.6f).coerceIn(0f, 1f)
                    translationY = -p * 48.dp.toPx()
                    val s = 1f - 0.04f * p
                    scaleX = s; scaleY = s
                },
        ) {
            HomeContent(vm)
        }

        // ---- Drawer ----
        Drawer(
            vm = vm,
            progress = progress,
            fullHeight = fullH,
            resetKey = drawerReset,
            onDrag = { dragBy(it) },
            onSettle = { v -> settle(v) },
        )
    }
}

@Composable
private fun HomeContent(vm: LightOsViewModel) {
    val p = Light.palette
    val t = Light.type
    val slots by vm.slots.collectAsState()
    val focus by vm.focus.collectAsState()
    val arrival by vm.arrival.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Space.gutter),
    ) {
        Spacer(Modifier.height(Space.s24))
        Clock(onShowFocus = { vm.open(Screen.Focus) }, focusMinutes = if (focus.running) focus.remainingMinutes else null)

        Spacer(Modifier.weight(1f))

        slots.forEachIndexed { index, slot ->
            Word(
                slot = slot,
                index = index,
                arrival = arrival,
                onOpen = { view, bounds -> vm.launchSlot(slot, view, bounds) },
                onHold = { vm.openSheet(Sheet.Word(index)) },
            )
        }
        if (slots.isEmpty()) {
            LText("Long-press here to choose your words.", t.body, color = p.inkFaint)
        }

        Spacer(Modifier.weight(0.42f))

        // The command line. Quiet until touched.
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Space.row)
                .pressable(origin = TransformOrigin(0f, 0.5f)) { vm.openCommand() },
            contentAlignment = Alignment.CenterStart,
        ) {
            LText("Search or ask", t.body, color = p.inkFaint, maxLines = 1)
        }
        Spacer(Modifier.height(Space.s8))
    }
}

@Composable
private fun Word(
    slot: HomeSlot,
    index: Int,
    arrival: Int,
    onOpen: (android.view.View, android.graphics.Rect?) -> Unit,
    onHold: () -> Unit,
) {
    val reduced = Light.reduceMotion
    val view = LocalView.current
    val haptics = rememberHaptics()
    var bounds by remember { mutableStateOf<Rect?>(null) }
    val enter = remember { Animatable(1f) }
    val rise = with(LocalDensity.current) { 6.dp.toPx() }

    LaunchedEffect(arrival) {
        if (!reduced && arrival > 0) {
            enter.snapTo(0f)
            delay(index * 18L)
            enter.animateTo(1f, Motion.fadeBase())
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Space.row)
            .padding(vertical = Space.wordGap / 2)
            .graphicsLayer {
                alpha = enter.value
                translationY = (1f - enter.value) * rise
            }
            .pressable(origin = TransformOrigin(0f, 0.5f), onLongPress = onHold) {
                haptics.commit()
                onOpen(view, bounds?.toAndroid())
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        LText(
            slot.label,
            Light.type.word,
            maxLines = 1,
            modifier = Modifier
                .wrapContentWidth()
                .onGloballyPositioned { bounds = it.boundsInWindow() },
        )
    }
}

@Composable
private fun Clock(onShowFocus: () -> Unit, focusMinutes: Int?) {
    val p = Light.palette
    val t = Light.type
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Tick exactly on the minute boundary.
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000L - (now % 60_000L) + 20L)
        }
    }

    val is24 = DateFormat.is24HourFormat(context)
    val locale = Locale.getDefault()
    val time = remember(now, is24) {
        DateFormat.format(if (is24) "H:mm" else "h:mm", Date(now)).toString()
    }
    val date = remember(now) {
        DateFormat.format(DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd"), Date(now)).toString()
    }

    Column {
        LText(
            time, t.clock, maxLines = 1,
            modifier = Modifier.pressable(origin = TransformOrigin(0f, 0.5f)) { openClock(context) },
        )
        LText(
            date, t.caption, color = p.inkSoft, maxLines = 1,
            modifier = Modifier
                .padding(top = Space.s4)
                .pressable(origin = TransformOrigin(0f, 0.5f)) { openCalendar(context) },
        )
        if (focusMinutes != null) {
            LText(
                "Focus · $focusMinutes min left", t.caption, color = p.signal, maxLines = 1,
                modifier = Modifier
                    .padding(top = Space.s8)
                    .pressable(origin = TransformOrigin(0f, 0.5f), onClick = onShowFocus),
            )
        }
    }
}

private fun openClock(context: Context) {
    runCatching {
        context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun openCalendar(context: Context) {
    runCatching {
        val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
            .appendPath(System.currentTimeMillis().toString()).build()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Pull down on home → the system notification shade (needs EXPAND_STATUS_BAR). */
@SuppressLint("WrongConstant")
fun expandNotificationShade(context: Context) {
    runCatching {
        val service = context.getSystemService("statusbar")
        Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(service)
    }
}
