# SleepSense dashboard

## Direction contract

**THESIS:** Adapt the user's screenshot of a wearable fitness companion into a premium SleepSense instrument: compact black signal cards that make the latest session readable without implying clinical certainty.

**OWN-WORLD:** Near-black OLED surfaces, a fine luminous outline around each readout, and three disciplined signal accents: rose for sleep quality, cyan for deep sleep and session timing, and violet for REM and movement. Each value gets a matching circular indicator and slim horizontal meter when the measure supports one.

**STORY:** On the first dashboard visit in a browser session, a branded sleep-signal reveal leads into the latest session. The user sees its simulated status, scans quality and stage duration, traces the stage timeline and stage distribution, then explores heart rate, EEG bands across recorded nights, movement, sleep-score history, SpO₂, skin temperature, and the AI assistant.

**ENTRY STATE:** The reveal runs once per browser session, can be skipped with a button or Escape, restores focus to the dashboard, and is omitted when reduced motion is enabled.

**SCROLL MOTION:** Keep native scrolling. As each SleepSense section enters the viewport, reveal its signal cards and charts once with a short upward settle, a soft focus-in, and a restrained cyan/violet lift. Stagger adjacent cards slightly; never hold scrolling or hide content when motion is reduced or JavaScript is unavailable.

**FIRST VIEWPORT:** The existing navigation stays familiar. The main column opens with the user's name, latest-session date, and a compact simulation notice. Three distinct signal cards sit in one row on desktop and stack on mobile: sleep score, deep sleep, and REM sleep. A full-width session card follows with start and end times, the color-coded stage bar, and the legend.

**FORM:** Pinned by the user: “make like i shared the screenshot … keep the parameters of the sleep app.” Reference: user-provided wearable app screenshot (fitness data is excluded; the visual language is adapted to SleepSense).

**FINISH:** unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
