---
name: SleepSense
description: A quiet, luminous instrument for reviewing a simulated night of sleep.
colors:
  canvas: "#030508"
  rail: "#05070b"
  surface: "#090c12"
  surface-raised: "#0e1219"
  surface-inset: "#05070a"
  line: "#252d38"
  text: "#f1f4f8"
  text-soft: "#bdc7d2"
  muted: "#929eac"
  dim: "#748190"
  mint: "#66d8df"
  ice: "#73d8e3"
  lilac: "#a493ff"
  rose: "#e58ad2"
  cyan: "#69d7e3"
  violet: "#b597ff"
  amber: "#d4af7c"
  awake: "#737d8b"
  light-sleep: "#8da9c7"
  deep-sleep: "#64d4df"
  rem-sleep: "#c779d4"
typography:
  display:
    fontFamily: "Sora, ui-sans-serif, system-ui, sans-serif"
    fontSize: "clamp(50px, 6.2vw, 72px)"
    fontWeight: 600
    lineHeight: 1
    letterSpacing: "-0.045em"
  body:
    fontFamily: "Sora, ui-sans-serif, system-ui, sans-serif"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: 1.6
  label:
    fontFamily: "Sora, ui-sans-serif, system-ui, sans-serif"
    fontSize: "9px"
    fontWeight: 600
    lineHeight: 1.4
rounded:
  sm: "8px"
  md: "12px"
  lg: "16px"
spacing:
  page-gutter: "clamp(22px, 6.5vw, 105px)"
components:
  button-primary:
    backgroundColor: "{colors.mint}"
    textColor: "#0c1715"
    rounded: "{rounded.sm}"
    height: "39px"
  signal-card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    rounded: "13px"
    padding: "22px"
  nav-active:
    backgroundColor: "rgba(57, 96, 168, 0.17)"
    textColor: "#e2ebff"
    rounded: "99px"
---

# Design System: SleepSense

## Overview

**Creative North Star: “Quiet Sleep Instrument”**

SleepSense should feel calm and precise in a dark room, with a dreamlike midnight atmosphere around information that remains easy to read. The interface is a personal review tool for simulated sleep data; it must never suggest clinical certainty or hide that readings are simulated.

Blue-violet light creates the night field. Rose marks sleep quality, cyan marks deep sleep and session timing, and violet marks REM and movement. Keep that color mapping stable so the charts, rings, and summary cards teach the same visual language.

**Key Characteristics:**

- Near-black OLED surfaces with a blue-violet aurora.
- Compact signal cards with fine outlines and restrained glow.
- Clear simulated-data labels and legible sleep-stage colors.
- Short, purposeful motion that never blocks scrolling or interaction.

## Colors

The palette pairs a nearly black canvas with cool signal colors; the warm amber notice is reserved for the prototype-data disclosure.

### Primary

- **Midnight canvas** (`#030508`): Main page field and the backdrop for charts.
- **Aurora blue** (`#2f82ff`): Atmospheric linework and low-opacity light behind the hero.
- **Sleep cyan** (`#69d7e3`): Deep sleep and session timing.

### Secondary

- **Signal rose** (`#e58ad2`): Sleep-quality score and its history.
- **REM violet** (`#b597ff`): REM sleep and movement.
- **Icy blue** (`#73d8e3`): Supporting sensor accents.

### Neutral

- **Primary text** (`#f1f4f8`): Main headings and important measurements.
- **Soft text** (`#bdc7d2`): Supporting copy on dark surfaces.
- **Muted text** (`#929eac`): Secondary labels and navigation.
- **Panel** (`#090c12`): Cards and chart panels.
- **Raised panel** (`#0e1219`): Interactive and selected surfaces.
- **Hairline** (`#252d38`): Card boundaries and structural dividers.
- **Prototype amber** (`#d4af7c`): Simulation disclosure only.

### Named Rules

**The Signal-Color Rule.** Reuse the established rose, cyan, and violet roles; do not assign a new accent to an existing sleep measure.

**The Simulation Rule.** Keep simulated status visible near the data and never describe prototype readings as measured health data.

## Typography

**Display Font:** Sora (with `ui-sans-serif`, `system-ui`, sans-serif fallbacks)  
**Body Font:** Sora  
**Label Font:** Sora

**Character:** Sora keeps the instrument modern and measured. Large headings are brief and composed; dense labels stay quiet without making measurements hard to scan.

### Hierarchy

- **Display** (600, `clamp(50px, 6.2vw, 72px)` desktop; `clamp(36px, 11.3vw, 46px)` mobile, 1): The dashboard welcome statement.
- **Section heading** (600, `clamp(21px, 2.7vw, 30px)`): Main groups such as the sleep signals.
- **Panel title** (600, 13–16px): Chart and session names.
- **Body** (400, 12px, 1.6): Short explanations and assistant guidance.
- **Label** (600, 8–10px): Stage, unit, simulation, and navigation labels.

Use tabular numerals for times and measured-looking values. Reserve wide tracking for compact metadata, never for the primary reading.

## Layout

The dashboard uses a full-width horizontal navigation bar and a centered content region capped at 1390px. Its welcome area is deliberately compact so the prototype notice and three sleep-signal cards appear near the first viewport. A full-width stage timeline follows, then a two-column sensor grid.

At 800px and below, the navigation becomes a compact top bar and the summary cards keep their three-column layout where space permits. At 600px and below, the cards and charts stack into one column, the navigation becomes a two-row mobile bar, and page gutters contract to 16px. Keep stage labels and simulation status visible at each size.

## Elevation & Depth

Depth comes from tonal steps between the canvas, panels, and raised controls, with a thin full outline around signal cards. Soft, offset shadows distinguish interactive emphasis; colored light is ambient atmosphere or a signal state, not a substitute for panel boundaries. Keep hover lifts small and brief.

## Shapes

Use 8px for controls, 12px for standard panels, and 16px for larger containers. Small status and navigation controls may use pill shapes. Cards use a full 1px outline; do not add a single colored edge to imply hierarchy.

## Components

### Buttons

- **Primary:** Cyan fill, dark text, 8px radius, and a 39px default minimum height (43px on full-width account actions).
- **Secondary:** Dark raised surface with a restrained blue outline; the dashboard’s generation action uses a small pill silhouette.
- **Hover / Focus:** Shift the border and surface tone slightly. Use the shared visible cyan focus ring; avoid large jumps or glow flashes.
- **Disabled:** Keep the control legible while reducing emphasis and preventing repeated submission.

### Sleep signal cards

- Use rose for sleep quality, cyan for deep sleep, and violet for REM.
- Pair the main value with a matching circular stage share and a slim meter where it clarifies proportion.
- Keep supporting copy short and label inferred stages as estimates.
- On small screens, stack the cards without shrinking the readings into microcopy.

### Sleep-stage timeline

- Use gray for awake, blue for light, cyan for deep, and violet-pink for REM.
- Keep start and end times, the proportional bar, and the text legend together.

### Charts and sensors

- Put a clear title and unit near every chart; retain the simulated label.
- Keep chart colors aligned with the summary-card roles.
- Show SpO₂ and skin temperature as prototype sensor values, not diagnostic conclusions.

### Prototype disclosure

- Use a compact, amber-tinted notice near the top of the dashboard.
- State plainly that all wearable readings are simulated.

### Motion

- The startup reveal appears once per browser session and respects reduced-motion settings.
- The page keeps native scrolling; section reveals settle quickly as content enters view.
- Night generation uses a brief sleep-signal sequence, then updates the existing cards and charts in place.

## Do's and Don'ts

- Do keep sleep values and their units visible at a glance.
- Do retain the midnight aurora while giving the data more visual weight than the atmosphere.
- Do keep simulated status beside readings and chart history.
- Do preserve keyboard focus, reduced-motion behavior, and touch-friendly controls.
- Don't use fitness labels or imply a medical diagnosis.
- Don't add decorative badges, extra accent colors, or large empty hero space that pushes the session below the fold.
- Don't use a section eyebrow where a clear heading already provides context.
