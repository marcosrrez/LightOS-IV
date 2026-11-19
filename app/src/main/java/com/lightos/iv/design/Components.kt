@file:OptIn(ExperimentalFoundationApi::class)

package com.lightos.iv.design

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------
// Text
// ---------------------------------------------------------------------------------------

/** All text goes through here so the surface's text shadow (Glass) is applied uniformly. */
@Composable
fun LText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Light.palette.ink,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val shadow = Light.palette.textShadow
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(TextStyle(color = color, shadow = shadow)),
        maxLines = maxLines,
        overflow = overflow,
    )
}

// ---------------------------------------------------------------------------------------
// Press feedback (DESIGN.md §5): scale + dim, or a full-width overlay for rows. No ripple.
// ---------------------------------------------------------------------------------------

enum class PressStyle { Scale, Overlay }

fun Modifier.pressable(
    style: PressStyle = PressStyle.Scale,
    origin: TransformOrigin = TransformOrigin.Center,
    enabled: Boolean = true,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduced = Light.reduceMotion
    val haptics = rememberHaptics()
    val progress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = if (pressed) tween(60) else Motion.snap(reduced),
        label = "press",
    )
    val pressColor = Light.palette.press
    val base = when (style) {
        PressStyle.Scale -> Modifier.graphicsLayer {
            val s = 1f - 0.03f * progress
            scaleX = s; scaleY = s
            alpha = 1f - 0.36f * progress
            transformOrigin = origin
        }
        PressStyle.Overlay -> Modifier.background(pressColor.copy(alpha = pressColor.alpha * progress))
    }
    base.combinedClickable(
        interactionSource = interaction,
        indication = null,
        enabled = enabled,
        onLongClick = onLongPress?.let { lp -> { haptics.hold(); lp() } },
        onClick = onClick,
    )
}

// ---------------------------------------------------------------------------------------
// Rows & actions
// ---------------------------------------------------------------------------------------

@Composable
fun Rule(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Light.palette.rule))
}

/** A list row: `item` title, optional caption, optional trailing text. 56dp minimum. */
@Composable
fun LRow(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    trailing: String? = null,
    titleColor: Color = Light.palette.ink,
    horizontalPadding: androidx.compose.ui.unit.Dp = Space.gutterTool,
    onLongPress: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val p = Light.palette
    val t = Light.type
    val clickMod = if (onClick != null) Modifier.pressable(PressStyle.Overlay, onLongPress = onLongPress, onClick = onClick) else Modifier
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = Space.row)
            .then(clickMod)
            .padding(horizontal = horizontalPadding, vertical = Space.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            LText(title, t.item, color = titleColor, maxLines = 1)
            if (detail != null) LText(detail, t.caption, color = p.inkSoft, maxLines = 2)
        }
        if (trailing != null) {
            Spacer(Modifier.padding(start = Space.s16))
            LText(trailing, t.label, color = p.inkSoft, maxLines = 1)
        }
    }
}

/** Text action: a verb in `label`, ink. */
@Composable
fun TextAction(text: String, modifier: Modifier = Modifier, color: Color = Light.palette.ink, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = Space.action).pressable(onClick = onClick).padding(horizontal = Space.s12),
        contentAlignment = Alignment.Center,
    ) { LText(text, Light.type.label, color = color, maxLines = 1) }
}

/** The one filled action per sheet: ink pill, ground text. */
@Composable
fun PrimaryAction(text: String, modifier: Modifier = Modifier, color: Color = Light.palette.ink, onClick: () -> Unit) {
    val p = Light.palette
    Box(
        modifier
            .heightIn(min = Space.action)
            .pressable(onClick = onClick)
            .background(color, Shapes.pill)
            .padding(horizontal = Space.s24),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = Light.type.label.merge(TextStyle(color = if (p.isLight) p.raised else Color.Black)), maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------
// Field: no box, a single rule that turns ink while focused.
// ---------------------------------------------------------------------------------------

@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Light.type.label,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    onSubmit: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    secret: Boolean = false,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    minLines: Int = 1,
) {
    val p = Light.palette
    var focused by remember { mutableStateOf(false) }
    val ruleColor = if (focused) p.ink else p.rule
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused },
        textStyle = style.merge(TextStyle(color = p.ink)),
        cursorBrush = SolidColor(p.ink),
        singleLine = singleLine,
        minLines = minLines,
        visualTransformation = if (secret) androidx.compose.ui.text.input.PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = if (secret) KeyboardType.Password else KeyboardType.Text,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onAny = { onSubmit?.invoke() }),
        decorationBox = { inner ->
            Column {
                Box(Modifier.fillMaxWidth().heightIn(min = Space.action).padding(vertical = Space.s12), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) LText(placeholder, style, color = p.inkFaint, maxLines = 1)
                    inner()
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(ruleColor))
            }
        },
    )
}

// ---------------------------------------------------------------------------------------
// Sheet: raised, 28dp top radius, drag to dismiss, scrim, back closes (caller's BackHandler).
// ---------------------------------------------------------------------------------------

/**
 * Shows [content] for a non-null [item]; animates out (keeping the last item) when it
 * becomes null. Dismisses on scrim tap, downward drag past 30% or a downward flick.
 */
@Composable
fun <T : Any> SheetHost(
    item: T?,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(T) -> Unit,
) {
    var last by remember { mutableStateOf(item) }
    if (item != null) last = item
    val shown = last ?: return

    val reduced = Light.reduceMotion
    val progress = remember { Animatable(0f) }
    val drag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var sheetHeight by remember { mutableStateOf(1) }
    val density = LocalDensity.current
    val flick = with(density) { Motion.FLICK_DP_PER_S.dp.toPx() }

    LaunchedEffect(item != null) {
        if (item != null) {
            drag.snapTo(0f)
            progress.animateTo(1f, Motion.move(reduced))
        } else {
            progress.animateTo(0f, Motion.move(reduced))
            last = null
        }
    }

    val p = Light.palette
    Box(Modifier.fillMaxSize()) {
        // Scrim
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
                .background(p.scrim)
                .pointerInput(Unit) { detectTapOnly { onDismiss() } },
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { sheetHeight = it.height.coerceAtLeast(1) }
                .graphicsLayer {
                    translationY = (1f - progress.value) * sheetHeight + drag.value
                }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { d ->
                        scope.launch { drag.snapTo((drag.value + d).coerceAtLeast(0f)) }
                    },
                    onDragStopped = { v ->
                        if (drag.value > sheetHeight * 0.3f || v > flick) onDismiss()
                        else drag.animateTo(0f, Motion.snap(reduced), initialVelocity = v)
                    },
                )
                .background(p.raised, Shapes.sheet)
                .pointerInput(Unit) { detectTapOnly { } } // swallow taps so they don't hit the scrim
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(top = Space.s12, bottom = Space.s16),
            verticalArrangement = Arrangement.spacedBy(Space.s4),
        ) {
            // Grabber
            Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = Space.s8)) {
                Box(Modifier.height(4.dp).fillMaxWidth(0.1f).background(p.rule, Shapes.pill))
            }
            content(shown)
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTapOnly(onTap: () -> Unit) {
    detectTapGestures(onTap = { onTap() })
}

// ---------------------------------------------------------------------------------------
// Whisper: past-tense feedback above the command line. No box, no button.
// ---------------------------------------------------------------------------------------

@Composable
fun Whisper(text: String?, modifier: Modifier = Modifier) {
    var last by remember { mutableStateOf(text) }
    if (text != null) last = text
    AnimatedVisibility(
        visible = text != null,
        modifier = modifier,
        enter = fadeIn(Motion.fadeBase()),
        exit = fadeOut(Motion.fadeFast()),
    ) {
        LText(last.orEmpty(), Light.type.caption, color = Light.palette.inkSoft, maxLines = 2)
    }
}

/** Section label inside tool screens and sheets. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    LText(
        text, Light.type.caption, color = Light.palette.inkSoft,
        modifier = modifier.padding(horizontal = Space.gutterTool).padding(top = Space.s24, bottom = Space.s4),
    )
}

/** A full-screen tool frame: inset-safe, title, back on the left edge via system back. */
@Composable
fun ToolFrame(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .background(Light.palette.ground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        LText(
            title, Light.type.title,
            modifier = Modifier.padding(horizontal = Space.gutterTool).padding(top = Space.s24, bottom = Space.s16),
        )
        content()
    }
}
