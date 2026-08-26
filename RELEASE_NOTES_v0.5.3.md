# Unictoos v0.5.3

Unictoos v0.5.3 is a professional UI redesign for the mobile broadcasting workflow. The update keeps the existing capture, RootEncoder transport, destination, permission, and recovery behavior intact while making the application feel more like a focused mobile production workspace.

## Interface redesign

The app shell now uses a floating glass control deck with a clearer top workspace bar, a compact bottom navigation surface, stronger active-tab hierarchy, and better safe-area spacing. The Black Hole visual system is carried through the complete shell with event-horizon backgrounds, blue-violet action surfaces, photon-cyan live signals, warm orange caution states, refined borders, and a consistent Material 3 shape system.

A lightweight animated black-hole backdrop now sits behind routed screens. It uses a small Canvas drawing with an orbiting accretion ring and restrained photon particles rather than a heavy asset or third-party animation engine. Home now has a breathing broadcast-readiness hero and clearer primary action. The Studio preview has a framed broadcast-monitor treatment with animated signal emphasis, a clearer LIVE/PREVIEW badge, and more legible telemetry. Shared scene cards now animate selection emphasis, and section headings use consistent signal markers throughout Home, Scenes, Studio, Library, and Settings.

## Reliability boundary

This release is UI-only. It does not add a backend, cloud relay, Firebase, Supabase, OAuth redirect server, paid API, desktop OBS server, WHIP, WebRTC, or new streaming transport. Existing RootEncoder 2.8.0 and Compose BOM 2026.08.00 dependencies remain verified and unchanged after the aborted 2.10.0 experiment. The streaming and capture paths were not redesigned for cosmetic changes.

## Validation

| Gate | Result |
|---|---|
| JVM unit tests | Passed after the redesign. |
| Android lint | Passed after the redesign. |
| Debug APK assembly | Passed after the redesign. |
| Static smoke gate | Passed with 157 assertions, including palette and motion-graphics checks. |
| Unsigned release APK assembly | Not completed in this sandbox run; the R8 process was killed by the sandbox memory limit. |
| Source security audit | Previously clean; no runtime service or credential-handling code was added in this UI update. |

These checks do not prove physical-device appearance, Go Live success, PiP operation, platform ingest, Android 15 OEM behavior, or long-duration streaming stability. Those still require testing on the target Android phone.

The package remains `com.unictoai.unictoos`, with version name `0.5.3` and versionCode `63`. The successful debug APK is 87,255,394 bytes with SHA-256 `3d03209aceb3cd6c4e0dc8102e814a8da36bb2b1547f7201bb26936bab22d819`. The debug APK is the installable test artifact for this UI release; the unsigned release shrinker was not completed because the sandbox terminated R8 under memory pressure.
