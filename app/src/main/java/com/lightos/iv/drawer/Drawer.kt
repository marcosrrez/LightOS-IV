package com.lightos.iv.drawer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.lightos.iv.LightOsViewModel
import com.lightos.iv.Sheet
import com.lightos.iv.command.AppSearch
import com.lightos.iv.data.AppEntry
import com.lightos.iv.design.Field
import com.lightos.iv.design.LText
import com.lightos.iv.design.Light
import com.lightos.iv.design.PressStyle
import com.lightos.iv.design.Space
import com.lightos.iv.design.Surface
import com.lightos.iv.design.pressable
import com.lightos.iv.design.rememberHaptics
import com.lightos.iv.home.toAndroid
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.roundToInt

private const val OTHER = '#'

private fun letterOf(name: String): Char {
    val c = AppSearch.normalize(name).firstOrNull()?.uppercaseChar() ?: return OTHER
    return if (c in 'A'..'Z') c else OTHER
}

private sealed interface DrawerItem {
    data class Header(val letter: Char) : DrawerItem
    data class App(val app: AppEntry) : DrawerItem
}

/**
 * The app drawer. Rides on `progress` from [com.lightos.iv.home.HomeLayer]. When the list
 * is at the top, pulling down hands the drag back to the drawer (nested scroll), so the
 * whole sheet follows the finger closed, exactly like opening it.
 */
@Composable
fun Drawer(
    vm: LightOsViewModel,
    progress: Float,
    fullHeight: Float,
    resetKey: Int,
    onDrag: (Float) -> Unit,
    onSettle: (Float) -> Unit,
) {
    val p = Light.palette
    val apps by vm.apps.collectAsState()
    var query by remember(resetKey) { mutableStateOf("") }
    val listState = remember(resetKey) { LazyListState() }
    val focus = LocalFocusManager.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // Hide keyboard and clear search when the drawer closes.
    LaunchedEffect(progress == 0f) {
        if (progress == 0f) { focus.clearFocus(); query = "" }
    }

    val grouped = remember(apps) {
        val out = ArrayList<DrawerItem>(apps.size + 27)
        val index = HashMap<Char, Int>()
        apps.groupBy { letterOf(it.name) }
            .toSortedMap(compareBy { if (it == OTHER) '{' else it })
            .forEach { (letter, list) ->
                index[letter] = out.size
                out += DrawerItem.Header(letter)
                list.forEach { out += DrawerItem.App(it) }
            }
        out to index
    }
    val ranked = remember(apps, query) { if (query.isBlank()) emptyList() else AppSearch.rank(query, apps) }

    val currentProgress by androidx.compose.runtime.rememberUpdatedState(progress)
    val connection = remember(fullHeight) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Re-opening a partly closed drawer takes priority over scrolling the list.
                if (available.y < 0f && currentProgress < 1f) {
                    onDrag(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // List is at the top and the finger keeps pulling down: close the drawer.
                if (available.y > 0f && source == NestedScrollSource.Drag) {
                    onDrag(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (currentProgress < 1f) {
                    onSettle(available.y)
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    val drawerDrag = rememberDraggableState { dy -> onDrag(dy) }
    val glass = p.surface == Surface.Glass
    val clamped = progress.coerceIn(0f, 1f)

    Column(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = (1f - clamped) * fullHeight
                alpha = if (clamped == 0f) 0f else 1f
            }
            .background(if (glass) Color.Black.copy(alpha = 0.62f) else p.ground)
            .draggable(Orientation.Vertical, drawerDrag, onDragStopped = { v -> onSettle(v) }),
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val (items, headerIndex) = grouped
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().nestedScroll(connection),
                contentPadding = PaddingValues(top = Space.s16, bottom = Space.s24, end = if (query.isBlank()) 40.dp else 0.dp),
            ) {
                if (query.isBlank()) {
                    items(items.size, key = { i ->
                        when (val it = items[i]) { is DrawerItem.Header -> "h${it.letter}"; is DrawerItem.App -> it.app.key }
                    }) { i ->
                        when (val item = items[i]) {
                            is DrawerItem.Header -> LetterHeader(item.letter)
                            is DrawerItem.App -> AppRow(item.app, vm, view, focus)
                        }
                    }
                } else if (ranked.isEmpty()) {
                    item {
                        LText(
                            "No apps match “${query.trim()}”.", Light.type.body, color = p.inkSoft,
                            modifier = Modifier.padding(horizontal = Space.gutter, vertical = Space.s16),
                        )
                    }
                } else {
                    items(ranked.size, key = { ranked[it].key }) { i -> AppRow(ranked[i], vm, view, focus) }
                }
            }

            if (query.isBlank() && headerIndex.isNotEmpty()) {
                Scrubber(
                    letters = headerIndex.keys.sortedBy { if (it == OTHER) '{' else it },
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(0.78f),
                    onLetter = { letter ->
                        headerIndex[letter]?.let { idx -> scope.launch { listState.scrollToItem(idx) } }
                    },
                )
            }
        }

        // Search sits at thumb height, above the keyboard.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(horizontal = Space.gutter, vertical = Space.s8),
        ) {
            Field(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search apps",
                style = Light.type.body,
                imeAction = ImeAction.Go,
                capitalization = KeyboardCapitalization.None,
                onSubmit = {
                    ranked.firstOrNull()?.let { vm.launch(it, view, null) }
                },
            )
        }
    }
}

@Composable
private fun LetterHeader(letter: Char) {
    LText(
        letter.toString(), Light.type.caption, color = Light.palette.inkFaint,
        modifier = Modifier.padding(start = Space.gutter, top = Space.s16, bottom = Space.s4),
    )
}

@Composable
private fun AppRow(app: AppEntry, vm: LightOsViewModel, view: android.view.View, focus: FocusManager) {
    val haptics = rememberHaptics()
    var bounds by remember { mutableStateOf<Rect?>(null) }
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Space.row)
            .pressable(
                style = PressStyle.Overlay,
                onLongPress = { vm.openSheet(Sheet.AppMenu(app)) },
            ) {
                haptics.commit()
                focus.clearFocus()
                vm.launch(app, view, bounds?.toAndroid())
            }
            .padding(horizontal = Space.gutter),
        contentAlignment = Alignment.CenterStart,
    ) {
        LText(
            app.name, Light.type.item, maxLines = 1,
            modifier = Modifier.wrapContentWidth().onGloballyPositioned { bounds = it.boundsInWindow() },
        )
    }
}

/**
 * The alphabet rail. Under the thumb, a Gaussian wave lifts letters toward the left
 * (σ = 40dp, up to 64dp), the active letter turns ink, a large preview follows the thumb,
 * and each new letter ticks. DESIGN.md §8.
 */
@Composable
private fun Scrubber(letters: List<Char>, modifier: Modifier, onLetter: (Char) -> Unit) {
    val p = Light.palette
    val t = Light.type
    val haptics = rememberHaptics()
    val density = LocalDensity.current
    val sigma = with(density) { 40.dp.toPx() }
    val maxShift = with(density) { 64.dp.toPx() }

    var touchY by remember { mutableStateOf<Float?>(null) }
    var active by remember { mutableStateOf<Char?>(null) }
    var height by remember { mutableStateOf(1) }

    fun pick(y: Float) {
        touchY = y
        val i = ((y / height) * letters.size).toInt().coerceIn(0, letters.lastIndex)
        val letter = letters[i]
        if (letter != active) {
            active = letter
            haptics.tick()
            onLetter(letter)
        }
    }

    Box(
        modifier
            .width(48.dp)
            .onSizeChanged { height = it.height.coerceAtLeast(1) }
            .pointerInput(letters) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    pick(down.position.y)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        change.consume()
                        pick(change.position.y)
                    }
                    touchY = null
                    active = null
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            letters.forEachIndexed { i, letter ->
                val centerY = (i + 0.5f) * height / letters.size
                val ty = touchY
                val g = if (ty == null) 0f else exp(-((centerY - ty) * (centerY - ty)) / (2f * sigma * sigma))
                val shift by animateFloatAsState(-maxShift * g, label = "wave")
                val isActive = letter == active
                Box(
                    Modifier.weight(1f).fillMaxWidth().graphicsLayer {
                        translationX = shift
                        val s = 1f + 0.5f * g
                        scaleX = s; scaleY = s
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    LText(
                        letter.toString(), t.caption,
                        color = if (isActive) p.ink else p.inkFaint,
                    )
                }
            }
        }
        // Large preview of the active letter, beside the thumb.
        val ty = touchY
        val letter = active
        if (ty != null && letter != null) {
            LText(
                letter.toString(), t.clock,
                modifier = Modifier.offset {
                    IntOffset(-(maxShift * 2.2f).roundToInt(), (ty - 32.dp.toPx()).roundToInt())
                },
            )
        }
    }
}
