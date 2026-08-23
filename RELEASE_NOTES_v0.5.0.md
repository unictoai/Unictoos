# Unictoos v0.5.0

Unictoos v0.5.0 is a bounded local-device streaming update for Android. It keeps the previously hardened camera, microphone, MediaProjection, RootEncoder, RTMP/RTMPS/SRT, recording, and focused mobile Studio paths intact while adding opt-in screen-plus-camera composition, per-destination telemetry, adaptive bitrate controls, camera-only background audio behavior, and text overlay rendering.

This release is intentionally **not represented as a device-certified production release**. The sandbox has no Android device, emulator, ADB, logcat, platform ingest account, or 60-minute endurance-test environment. PiP is therefore exposed as an experimental RootEncoder-supported path with a screen-only fallback, and the release should be validated on the target Infinix X6853 Android 15 device before relying on it for a public broadcast.

## What changed

| Area | v0.5.0 behavior | Evidence boundary |
|---|---|---|
| Screen plus camera | Explicitly enabled scene PiP routes screen capture as the primary source and attaches a bounded camera layer through RootEncoder `SurfaceFilterRender`. Corner and size presets are persisted and exported. | JVM geometry and routing tests pass. No physical-device GL/camera proof is available. Dragging, rounded corners, border, shadow, and a custom two-input EGL compositor are not claimed. |
| PiP fallback | If the secondary camera/filter cannot initialize, the service records `pip_fallback_screen_only` and reports that it is continuing screen-only. | Static source and build checks pass; runtime fallback requires device validation. |
| Destination health | RootEncoder slot callbacks expose healthy, reconnecting, bitrate, failed, disconnect, and authentication states for up to two direct destinations. Studio shows per-slot status, bitrate, error text, and observed retry count. | This is per-destination observation over one shared MultiStream pipeline. Reconnect remains coordinated; independent slot stop/retry controls are not claimed. |
| Adaptive bitrate | The default-on policy steps down after 15 seconds below 80% of target and steps up after 60 seconds at or above 95% of target. Changes use the existing encoded stream and do not restart capture or resolution. A Settings toggle and quality-tier badge are included. | Pure policy, ViewModel, UI, and compile gates pass. Network behavior still requires real ingest testing. |
| Background audio | Camera-only scenes can keep the broadcast audio path active when the screen turns off. Video is muted through RootEncoder and restored on user-present. A partial wake lock is acquired for active broadcast/practice sessions and released on terminal paths. Screen capture is excluded from this mode. | Pure policy and source checks pass. OEM background limits and target-device behavior require device validation. |
| Text overlays | Existing text scene sources render through RootEncoder `TextObjectFilterRender`. The overlay contract includes text, timer, and chat-style data types for bounded future expansion. | Text burn-in is implemented through the existing scene editor. Image overlay decoding and remote chat providers are not claimed. |
| Lifecycle hardening | Null-intent service restarts return `START_NOT_STICKY` and do not resurrect capture. Startup failures, practice-recording failures, graphics recovery, terminal reconnect failures, stop completion, and service destruction release the broadcast wake lock. | Static, JVM, lint, and build gates pass; Android lifecycle behavior still requires device validation. |

## Scope deliberately deferred

No backend, server, cloud relay, Firebase, Supabase, OAuth redirect server, paid API, desktop OBS remote server, WHIP, or WebRTC service was added. YouTube API-key chat, Twitch IRC/WebSocket chat, Kick chat, remote provider authentication, and image overlay texture decoding remain deferred rather than implemented with guessed or unverified networking. Direct multistream remains capped at two destinations and is device fan-out, not cloud relay.

The PiP path uses only APIs already present in the RootEncoder dependency. It should be treated as an experimental fallback-capable feature until the target device proves camera initialization, positioning, preview, long-running encoding, and correct ingest output. The app continues to prefer the existing camera-first behavior for legacy mixed scenes unless PiP is explicitly enabled.

## Validation performed in the sandbox

| Gate | Result |
|---|---|
| JVM unit tests | Passed after the final v0.5 source changes. |
| Android lint | Passed. |
| Debug APK assembly | Passed. |
| Instrumentation APK assembly | Passed. |
| Release APK assembly | Passed before and after the final metadata bump when the final artifact is produced. |
| Static feature smoke gate | Passed with v0.5-specific PiP, health, adaptive bitrate, background-audio, overlay, wake-lock, and restart assertions. |
| Source security audit | Passed with zero suspicious credential literals and zero direct logging calls. |

The checks above are repository and build evidence only. They do **not** prove successful YouTube, Twitch, Kick, custom RTMP/RTMPS, or SRT ingestion, PiP operation on the Infinix X6853, graphics-resource stability for 60 minutes, or background execution under Android 15 OEM policy.

## Installation artifacts

The final release directory contains the unsigned release APK, debug APK, instrumentation APK, and SHA-256 checksums. The measured sizes are 8,549,243 bytes for the unsigned release APK, 87,239,010 bytes for the debug APK, and 2,434,801 bytes for the instrumentation APK. The release APK is intentionally unsigned because no signing key is stored in the repository or sandbox. Install the debug APK for device validation unless you have your own signing and distribution process.

| Artifact | SHA-256 |
|---|---|
| `Unictoos-v0.5.0-debug.apk` | `c1fb7f1fc567ba0cb51798da4ff8b486c77c1e67109a044e7bb253681047a1f3` |
| `Unictoos-v0.5.0-release-unsigned.apk` | `f9f28a22b4f1627ff20c435b2d05429844a88dafc8da23591aa6b11f04395f02` |
| `Unictoos-v0.5.0-androidTest.apk` | `030b2608bccda2a91058cd798bd7365faf1391840eed32f4082047aa7174b722` |

## Security and privacy

Stream keys remain handled through the existing Android Keystore-backed credential path and are not included in scene/config exports, diagnostics, release notes, or source literals. The app does not transmit data to a Unictoos backend. Users should continue to treat device build details, stream keys, and platform credentials as sensitive.
