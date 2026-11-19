# LightOS design system

LightOS is a quiet instrument: a home screen that rests until touched, responds instantly,
and does real work when asked. This document is the source of truth. Code implements it in
`app/src/main/java/com/lightos/iv/design/`. If the code and this document disagree, one of
them is a bug.

---

## 1. Principles

1. **Still at rest.** Nothing moves, blinks, badges or counts on the home screen unless the
   owner asked for it (a running focus timer is the only live element).
2. **Words, not icons.** Apps and tools are named, never pictured. Type does the work color
   and imagery do elsewhere.
3. **Instant before intelligent.** Anything that can be answered on the device is answered
   in the same frame. The assistant handles only what needs reasoning, and says when it is
   thinking.
4. **One thumb.** Everything you touch often lives in the lower 60% of the screen. The top
   is for reading (time, date, status), not for tapping.
5. **Physically honest.** Anything you drag follows your finger 1:1 and settles with a
   spring that keeps your release velocity. Nothing teleports. Every gesture can be
   abandoned halfway.
6. **True.** Never show sample or invented data as the owner's. Empty states say what will
   appear and how to add it.
7. **Asks before it acts.** Calls, texts, and anything that costs money or can't be undone
   need a confirmation with the resolved person, number and message shown in full.

---

## 2. Surfaces (color)

A surface is a complete palette. Components never use literal colors; they read roles.

### Roles

| Role | Use |
|---|---|
| `ground` | Window background. |
| `raised` | Sheets and the command panel, one level above ground. |
| `ink` | Primary text and the clock. |
| `inkSoft` | Secondary text: dates, metadata, inactive words. ≥ 4.5:1 on ground. |
| `inkFaint` | Hints and placeholders. ≥ 3:1 on ground. |
| `rule` | Hairlines and dividers. |
| `press` | Overlay while a control is held. |
| `scrim` | Behind sheets. |
| `signal` | The single accent. Only for *live* state: a running timer, the assistant working, a listening microphone. Never decorative. |
| `danger` | Destructive confirmations only. |

### The five surfaces

| Surface | ground | raised | ink | inkSoft | inkFaint | signal |
|---|---|---|---|---|---|---|
| **OLED** (default) | `#000000` | `#121211` | `#F2EFE8` | `#A3A09A` | `#6B6965` | `#FFB547` |
| **Glass** | wallpaper + 40% black scrim | `#141413` @ 88% | `#FFFFFF` | `#FFFFFF` @ 72% | `#FFFFFF` @ 48% | `#FFC266` |
| **Paper** | `#F2F0EB` | `#FBFAF7` | `#141413` | `#5E5C57` | `#85827B` | `#A34A07` |
| **Amber** (night reading) | `#EFE0C0` | `#F6EAD0` | `#1F1810` | `#6A5A43` | `#857560` | `#9A3E0A` |
| **Matte** | `#161615` | `#20201E` | `#E6E2D8` | `#9C988F` | `#6C6962` | `#E8A33D` |

`rule` is `ink` at 12%; `press` is `ink` at 8%; `scrim` is ground-tinted at 60% (black at
60% on dark surfaces, `#2A2416` at 35% on Paper and Amber), so a sheet never drops a black
curtain over cream.

All pairs are verified: `ink` ≥ 13:1, `inkSoft` ≥ 5:1, `inkFaint` ≥ 3.3:1 and `signal` ≥ 5:1 on
`ground` in every solid surface. Glass is checked against a mid-grey wallpaper under its
scrim (the worst common case).

Glass text gets a soft shadow (`0, 1dp, 3dp, black @ 35%`) so it stays legible on any
wallpaper. No other surface uses shadows.

---

## 3. Type

Two families, bundled in the APK (never loaded from the network):

- **Public Sans** (variable, 100–900): everything in the interface.
- **Newsreader** (variable, optical size): reading only, in notes and the Today greeting.

There is no monospace face. Numbers that change in place (clock, timer, counts) use Public
Sans with tabular figures (`tnum`) so digits don't jitter.

### Scale

| Token | Family / weight | Size / line | Tracking | Use |
|---|---|---|---|---|
| `clock` | Sans 300, tnum | 60 / 64 | −0.035em | Home clock. |
| `word` | Sans 400 | 32 / 40 | −0.022em | Home words. |
| `title` | Sans 500 | 24 / 30 | −0.015em | Screen titles. |
| `item` | Sans 400 | 19 / 26 | −0.01em | Drawer rows, list rows. |
| `body` | Sans 400 | 16 / 24 | −0.005em | Running text, assistant replies. |
| `label` | Sans 500 | 14 / 20 | 0 | Actions, field text, metadata. |
| `caption` | Sans 400, tnum | 12.5 / 16 | +0.01em | Dates, timestamps, hints. |
| `display` | Newsreader 400, opsz 48 | 36 / 42 | −0.01em | Today greeting. |
| `reading` | Newsreader 400, opsz 16 | 18 / 28 | 0 | Note bodies. |

Rules:
- Sentence case everywhere. No ALL CAPS, no letter-spaced labels.
- At most two sizes visible in any one region.
- Weight contrast comes from 300/400/500 only; 600+ is not used.
- Respect the system font scale; layouts must survive 1.3× without clipping.

---

## 4. Space and shape

**Grid:** 4dp base. Steps: `4, 8, 12, 16, 24, 32, 48, 64`.

| Token | Value | Use |
|---|---|---|
| `gutter` | 28dp | Side margin on home and drawer. |
| `gutterTool` | 24dp | Side margin on tool screens. |
| `rowHeight` | 56dp | Minimum tap row (home words, drawer rows, actions). |
| `wordGap` | 4dp | Extra space between home words (on top of the 56dp row). |

**Shape:** only two radii.
- `sheet`: 28dp, top corners of bottom sheets.
- `pill`: fully round, for the one filled primary action in a sheet.

Everything else is square or uses no container at all. Separation comes from space first,
then a `rule`. Cards are not used.

---

## 5. Motion

All motion comes from four tokens. Nothing animates for decoration.

| Token | Definition | Use |
|---|---|---|
| `spring.move` | damping 0.86, stiffness 380 | Drawer, sheets, anything dragged and released. |
| `spring.snap` | damping 0.92, stiffness 900 | Small state changes, press release. |
| `fade.fast` | 120ms, standard decelerate | Hints, placeholders, whispers out. |
| `fade.base` | 220ms, emphasized decelerate `(0.05, 0.7, 0.1, 1)` | Screen content in. |

Behaviors:
- **Press:** on touch-down, scale to 0.97 and ink to 64% within 60ms; on release, return
  with `spring.snap`. A press that turns into a scroll cancels without firing.
- **Drag:** content tracks the finger 1:1. On release, the target is chosen by position
  *and* velocity (a quick flick commits even from 10% travel); the spring starts at the
  release velocity.
- **Arrive home:** words fade in with a 6dp rise, 18ms stagger, on return to home only.
- **Reduced motion** (system setting): springs become 120ms fades; no rise, no stagger.

---

## 6. Haptics

Three moments only, using the system's own effects (which honor the owner's haptic setting):

| Moment | Effect | When |
|---|---|---|
| **Tick** | `CLOCK_TICK` (`SEGMENT_FREQUENT_TICK` on Android 14+) | Scrubber crosses a letter. |
| **Commit** | `CONFIRM` (Android 11+) / `VIRTUAL_KEY` | An app launches, a drag commits, an action completes. |
| **Hold** | `LONG_PRESS` | A long-press opens a sheet. |

Taps that only navigate inside LightOS get no haptic. Silence is the default.

---

## 7. Layout of the home screen

```
┌──────────────────────────────┐
│ 9:41                         │  clock (top-left, gutter)
│ Wednesday, November 18       │  caption, inkSoft
│ Focus · 18 min left          │  live line (signal), only when something is live
│                              │
│                              │
│ Phone                        │  six words, left-aligned at gutter,
│ Messages                     │  anchored to the lower-middle of the
│ Notes                        │  screen for one-thumb reach
│ Focus                        │
│ Maps                         │
│ Music                        │
│                              │
│ Search or ask                │  command line, inkFaint
└──────────────────────────────┘
```

Gestures on home:
- **Swipe up** anywhere: the app drawer follows the finger.
- **Swipe down** anywhere: the system notification shade.
- **Tap "Search or ask"**: the command panel opens with the keyboard.
- **Long-press a word**: its sheet (change, remove).
- **Long-press empty space**: the home sheet (surface, words, settings).
- **Home button**: closes everything and returns here.

---

## 8. Components

| Component | Spec |
|---|---|
| **Word** | `word` type, 56dp row spanning the full width, left-aligned text. Press per §5. Long-press = Hold haptic + sheet. |
| **Row** | `item` type, optional `caption` secondary line, 56dp min. Full-width press overlay (`press`), no ripple. |
| **Action** | Text action: `label`, `ink`. Primary action: `label` on a `pill` filled with `ink`, text `ground`, 48dp tall. One primary per sheet at most. |
| **Field** | No box. `label` text on a single `rule` underline that becomes `ink` while focused. Placeholder in `inkFaint`. |
| **Sheet** | `raised`, `sheet` radius, drag to dismiss, `scrim` behind, closes on back. Content padding 24dp, 16dp between rows. |
| **Whisper** | Feedback after an action ("Note saved"). `caption`, `inkSoft`, sits above the command line for 1.6s then fades out. No box, no button. |
| **Confirm** | A sheet: title says the action ("Call Mom?"), body shows the resolved number and any message verbatim, primary action names the verb ("Call"), secondary is "Cancel". |
| **Scrubber** | Letters in `caption` at `inkFaint` on the right edge; under the thumb a Gaussian wave lifts and pushes letters left (σ = 40dp, max offset 64dp); the active letter is `ink`. Tick haptic per letter. The real list scrolls under it. |

---

## 9. Voice

- Plain names: "Notes", "Focus", "Today", "Maps". No brand names, no internal jargon.
- Buttons are verbs that say what happens: "Call", "Save", "Start focus".
- Whispers confirm in past tense: "Saved", "Timer set for 25 minutes".
- Errors say what happened and what to do: "No app matches 'spotfy'. Try the full name."
- The assistant answers in one calm sentence and never pretends to have done something it
  didn't.

---

## 10. Performance budgets

| Measure | Budget |
|---|---|
| Cold start to first frame | < 400ms (release build, baseline profile) |
| Home → drawer | first frame of motion within 16ms of the drag |
| Dropped frames while dragging | 0 at 120Hz |
| Local command resolution | < 16ms |
| Memory at rest | < 60MB |
