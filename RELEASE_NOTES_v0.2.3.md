# Unictoos v0.2.3 — Premium onboarding correction

## Visual correction

This release replaces the previous onboarding artwork with a new four-slide 9:16 set designed around a protected inner safe area. Each slide uses a restrained charcoal and cobalt Unictoos visual system, full-frame product composition, complete objects, and a short exact quote rendered in the artwork:

- “Turn every moment into a broadcast.”
- “Make your stream look like you.”
- “Capture. Connect. Create.”
- “Your channels. Your control.”

The onboarding screen now preserves the complete vertical artwork with a fit presentation instead of cropping it to the device viewport. The app shows a compact step indicator and supporting copy while the quote remains the visual focus.

## Replayable tour

More now includes **Replay the Unictoos tour**, allowing an existing creator to review onboarding without clearing app data. This is the preferred way to inspect the new artwork after upgrading. The action does not erase scenes, destinations, or stored credentials.

## Production roadmap

The repository now includes `PRODUCTION_FEATURE_ROADMAP.md`, a researched 1,140-slot capability inventory organized across capture, transport, scenes, audio, recording, destinations, creator workflow, engagement, branding, analytics, accessibility, privacy, monetization, and automation. The inventory is a staged backlog, not a claim that every feature is implemented in one release. Reliability and privacy remain gates for every batch.

## Preserved capabilities and stability

The v0.2.3 build retains the v0.2.2 portrait/landscape scene formats and the v0.2.1 graphics-stability path for the Infinix X6853. Secure credential storage, YouTube/Twitch/Kick/custom RTMP destinations, screen and camera capture, recording, scenes, markers, practice mode, health telemetry, configuration export, and the device-specific live-preview fallback remain included.

## Validation

The source passed `lintDebug`, `testDebugUnitTest`, `assembleDebug`, `git diff --check`, and the complete feature smoke suite with 47/47 checks passing. The release APK is versionCode 29 and versionName 0.2.3.

## Device-test note

The artwork and Replay tour can be tested without starting a broadcast. For stream testing, use a disposable destination and preserve the previously validated capture path. The graphics issue should continue to be evaluated on the Infinix X6853 with short controlled streams before longer broadcasts.
