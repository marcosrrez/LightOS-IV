package com.lightos.iv.command

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lightos.iv.LightOsViewModel
import com.lightos.iv.agent.AgentEvent
import com.lightos.iv.design.Field
import com.lightos.iv.design.LText
import com.lightos.iv.design.Light
import com.lightos.iv.design.Motion
import com.lightos.iv.design.PressStyle
import com.lightos.iv.design.Space
import com.lightos.iv.design.TextAction
import com.lightos.iv.design.pressable
import com.lightos.iv.design.rememberHaptics
import com.lightos.iv.home.toAndroid

/**
 * "Search or ask". The instant layer ([Resolver]) answers on every keystroke; suggestions
 * sit just above the keyboard (closest to the thumb), best first. Enter runs the top one.
 * Requests that need reasoning go to the assistant, whose conversation reads above.
 */
@Composable
fun CommandPanel(vm: LightOsViewModel) {
    val open by vm.commandOpen.collectAsState()

    BackHandler(enabled = open) { vm.closeCommand() }

    AnimatedVisibility(
        visible = open,
        enter = fadeIn(Motion.fadeBase()) + slideInVertically(Motion.fadeBase()) { it / 16 },
        exit = fadeOut(Motion.fadeFast()) + slideOutVertically(Motion.fadeFast()) { it / 24 },
    ) {
        PanelContent(vm)
    }
}

@Composable
private fun PanelContent(vm: LightOsViewModel) {
    val p = Light.palette
    val t = Light.type
    val apps by vm.apps.collectAsState()
    val cfg by vm.agentConfig.collectAsState()
    val events by vm.events.collectAsState()
    val thinking by vm.thinking.collectAsState()
    val view = LocalView.current
    val haptics = rememberHaptics()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var input by remember { mutableStateOf("") }
    val suggestions = remember(input, apps, cfg) { Resolver.resolve(input, apps, cfg.isReady) }
    val bounds = remember { mutableMapOf<Int, Rect>() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    fun run(index: Int) {
        val s = suggestions.getOrNull(index) ?: return
        if (s is Suggestion.OpenApp) haptics.commit()
        vm.run(s, view, bounds[index]?.toAndroid())
        if (s is Suggestion.Ask || s is Suggestion.Calculate) input = ""
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(p.raised)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = Space.gutter, end = Space.s12, top = Space.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LText(if (events.isEmpty()) "Search or ask" else "Assistant", t.label, color = p.inkSoft, modifier = Modifier.weight(1f))
            if (thinking) TextAction("Stop", color = p.inkSoft) { vm.stopAsking() }
            else if (events.isNotEmpty()) TextAction("Clear", color = p.inkSoft) { vm.resetConversation() }
            TextAction("Close", color = p.inkSoft) { vm.closeCommand() }
        }

        // Conversation (newest at the bottom, nearest the input).
        val convo = rememberLazyListState()
        LaunchedEffect(events.size, thinking) {
            val last = events.size + (if (thinking) 1 else 0) - 1
            if (last >= 0) convo.animateScrollToItem(last)
        }
        LazyColumn(
            state = convo,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Space.s12, Alignment.Bottom),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Space.gutter, vertical = Space.s16),
        ) {
            items(events.size) { i -> EventLine(events[i]) }
            if (thinking) item { Thinking() }
        }

        // Suggestions, best first, directly above the field.
        if (input.isNotBlank()) {
            Column(Modifier.fillMaxWidth().padding(bottom = Space.s4)) {
                suggestions.take(6).forEachIndexed { i, s ->
                    SuggestionRow(
                        s, primary = i == 0,
                        onBounds = { bounds[i] = it },
                        onClick = { run(i) },
                    )
                }
            }
        }

        Box(Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.s8)) {
            Field(
                value = input,
                onValueChange = { input = it },
                placeholder = if (cfg.isReady) "Open, note, focus, calculate or ask" else "Open, note, focus or calculate",
                style = t.body,
                imeAction = ImeAction.Go,
                onSubmit = { run(0) },
                focusRequester = focusRequester,
            )
        }
    }
}

@Composable
private fun SuggestionRow(s: Suggestion, primary: Boolean, onBounds: (Rect) -> Unit, onClick: () -> Unit) {
    val p = Light.palette
    val t = Light.type
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Space.row)
            .pressable(PressStyle.Overlay, onClick = onClick)
            .padding(horizontal = Space.gutter)
            .onGloballyPositioned { onBounds(it.boundsInWindow()) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            LText(s.title, t.item, color = if (primary) p.ink else p.inkSoft, maxLines = 1)
            s.detail?.let { LText(it, t.caption, color = p.inkFaint, maxLines = 1) }
        }
        if (primary) LText("Go", t.label, color = p.inkFaint)
    }
}

@Composable
private fun EventLine(ev: AgentEvent) {
    val p = Light.palette
    val t = Light.type
    when (ev.kind) {
        AgentEvent.Kind.Thought -> LText(ev.text, t.body, color = p.inkSoft)
        AgentEvent.Kind.Tool -> LText("↳ ${ev.text}", t.caption, color = p.inkFaint)
        AgentEvent.Kind.Result -> Unit
        AgentEvent.Kind.Answer -> LText(ev.text, t.body)
        AgentEvent.Kind.Error -> LText(ev.text, t.caption, color = p.danger)
    }
}

@Composable
private fun Thinking() {
    val p = Light.palette
    val pulse = rememberInfiniteTransition(label = "thinking")
    val a by pulse.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).graphicsLayer { alpha = a }.background(p.signal, CircleShape))
        Spacer(Modifier.size(Space.s8))
        LText("Thinking", Light.type.caption, color = p.inkSoft)
    }
}
