# LightOS IV — native + agentic

A from-scratch, **native Android** reimagining of the LightOS concept — not a WebView
wrapper. Kotlin + Jetpack Compose, registered as a real `HOME` launcher, with a
**genuine tool-calling AI agent** at its center instead of a regex matcher.

## What this is

The resting screen is six typographic words and a quiet clock. The real interface is
`⌘ ask anything`: type a natural request and an LLM agent reasons about it, then **acts
through real Android tools** — launching apps, starting a focus timer, taking a note,
reading your calendar, opening directions, calling or texting (with confirmation).

It's the same soul as the original LightOS line (Light Phone stillness, Jony-Ive daily
report, Commonplace notebook, focus timer, multi-surface theming) but built on the right
substrate: real system APIs, real haptics, live wallpaper for the Glass surface, 60fps
Compose, and an agent that does things instead of matching keywords.

## The agent (the point of this version)

- **ReAct-style tool-calling loop** (`agent/Agent.kt`): send system prompt + conversation
  + tool schemas to the model; if it returns tool calls, execute them against real
  Android capabilities, feed the results back, loop. Bounded to 6 steps.
- **17 tools** (`agent/tools/`): list/search/open apps, set timer, take/list notes, read
  today's calendar, open dialer, **make a direct call** (confirmed), **send SMS**
  (confirmed), route/navigation, web search, toggle torch, get device status, set the
  display surface, read text aloud.
- **Confirmation gate**: irreversible/charged actions (`make_direct_call`, `send_sms`)
  pop a real dialog before the agent executes them. The model is told not to ask — the
  app asks.
- **Live context**: every turn the system prompt is rebuilt with the current time, the
  active surface, installed-app count, battery/DND, and the next calendar event.

So "text Mom I'll be 20 min late, start a 20m focus timer, route me home" is one turn that
fires three tool calls.

### Provider-agnostic by design

The LLM client is OpenAI-compatible (`agent/LlmClient.kt`). Point the base URL + model +
key (Settings screen) at any of:

| Goal | Base URL | Model example |
| --- | --- | --- |
| Cloud quality | `https://api.openai.com/v1` | `gpt-4o-mini` |
| OpenRouter (many models) | `https://openrouter.ai/api/v1` | `anthropic/claude-3.5-sonnet` |
| Groq (fast) | `https://api.groq.com/openai/v1` | `llama-3.3-70b-versatile` |
| **On-device / private** | `http://localhost:11434/v1` (Ollama) | `llama3.1` |
| Local llama.cpp / vLLM | `http://localhost:8080/v1` | your model |

For local servers, leave the API key blank (the app only blocks a blank key for cloud
hosts). On-device inference gives you a fully private, offline agent.

## Architecture

```
app/src/main/java/com/lightos/iv/
  LightOsApp.kt            Application — owns the repositories + tool set
  MainActivity.kt          single HOME activity, edge-to-edge, system bars
  LightOsViewModel.kt       central state: nav, agent, timer, confirmation gate, ToolHost
  agent/
    Tool.kt                Tool interface + ToolHost (bridge to Android & UI)
    LlmClient.kt            OpenAI-compatible chat client (OkHttp)
    Agent.kt                the tool-calling loop + reasoning-trace events
    SystemPrompt.kt         persona + live DeviceContext injection
    JsonExt.kt              typed arg accessors
    tools/                  the 17 tool implementations + ToolRegistry
  data/
    NotesRepository.kt     persisted Commonplace notebook (DataStore)
    AppRepository.kt        installed apps (PackageManager)
    CalendarRepository.kt   today's events (CalendarContract)
    DeviceRepository.kt     battery + DND
    SettingsRepository.kt   surface, light mode, agent config (DataStore)
  ui/theme/                the 5 surfaces as a real Compose palette
  ui/                      Home, AgentSheet, AppDrawer (Niagara scrubber),
                           Notes, Timer, Report, Settings, root nav
```

Stack: Kotlin 2.0.21, AGP 8.7.3, Gradle 8.11.1, Compose BOM 2024.12, Material3,
DataStore, OkHttp, kotlinx.serialization. minSdk 26, target/compile 35.

## Build

**Android Studio (recommended):** open this folder, let it generate the Gradle wrapper
and download the SDK, run `app` on a device/emulator.

**CLI:** needs JDK 21 (Android Studio's bundled JBR works) — your system JDK 24 is too new
for Gradle 8.11. Set `org.gradle.java.home` in `~/.gradle/gradle.properties` to a JDK 21,
then:

```
gradle wrapper          # one-time, generates ./gradlew + the wrapper jar
./gradlew :app:assembleDebug
```

> The wrapper jar isn't committed (it's binary). `gradle wrapper` or Android Studio
> creates it from `gradle/wrapper/gradle-wrapper.properties`.

## First run

1. Press **Home** on the device and pick **LightOS** as the default launcher.
2. Open **Settings** (bottom rail) → enter your provider base URL, model, and API key,
   **save provider**. For Ollama: base URL `http://localhost:11434/v1`, blank key.
3. Tap **⌘ ask anything**, type a request, send.

Grant the calendar / call / SMS permissions in Settings → Permissions when you want the
agent to use them; it asks you to confirm before it calls or texts.

## Status & caveats

This is a coherent, buildable vertical slice, not a finished product:

- No voice input, no streaming (agent loop is request/response).
- No bundled fonts (system sans/serif/mono); no app icons in the drawer yet (name + package only).
- Daily Report shows calendar + a static reflection; habits/reader are not yet wired.
- Security model is strong where it counts (tool results aren't rendered as HTML, so the
  XSS class from the old web build can't happen here), but this is unreviewed prototype
  code — don't ship it to other people without an audit.

## Why this exists

The audited predecessor (`LightOS-V3`) was an HTML file in a WebView with a regex intent
engine and a hand-rolled binary-patching build. This version answers both: it's native
(the right tool for a launcher) and the assistant is a real agent (the right model for
"do what I mean").
