# Unictoos

**Unictoos** is a mobile-first, open-source Android live-streaming and recording studio for creators who want to broadcast from their phone to YouTube, Twitch, Kick, and compatible RTMP/RTMPS destinations.

The project is intentionally not a desktop OBS clone. It uses a touch-first workflow with a dashboard, scenes, a focused Studio screen, local recordings, secure destination management, and simple broadcast controls.

## Current status

The repository contains the native Android alpha foundation for focused mobile broadcasting. The app includes Kotlin and Jetpack Compose screens, Home, Scenes, Go Live, Library, and Settings areas, scene source controls, portrait/landscape layouts, per-platform destination setup, Keystore-backed credential storage, MediaProjection screen capture, camera capture with front/back switching, RootEncoder RTMP/RTMPS/SRT transport routing, bounded two-output fan-out, reconnect attempts, preflight checks, actionable notifications, live encoder telemetry, local MP4 recording, scene templates, safe scene-only configuration import, and post-session recap.

The current build remains an **alpha engineering milestone**. It compiles and packages successfully, but real-device validation against YouTube, Twitch, and Kick is still required before it should be treated as production-stable. The current engine can use screen capture or camera-only capture; a true screen-plus-camera picture-in-picture compositor is a planned next media milestone and should not be assumed from a scene containing both source types. Do not use an important broadcast or an irreplaceable stream key with an unvalidated development build.

## Latest test build

The latest supportability build is **Unictoos v0.4.11** with Android `versionCode 52`. It retains the atomic camera/screen-capture-to-start handoff and staged startup diagnostics, and replaces the four large onboarding PNGs with WebP assets. The unsigned release APK is approximately **8.5 MB**, down from approximately 21.3 MB in v0.4.10, while the live-streaming code path is unchanged. True screen-plus-camera PiP, multi-track media export, live DSP processing, and provider OAuth remain explicitly gated until separately verified implementations and physical-device validation are available. Review `RELEASE_NOTES_v0.4.11.md` for the current evidence boundary.

## Build locally

Requirements are Android SDK Platform 37, build tools 35.0.0, JDK 21, and a physical Android device running Android 10 or later for capture and encoder testing.

```bash
./gradlew test assembleDebug
```

The debug artifact is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it on a connected device with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## First-use workflow

Open **Settings**, choose YouTube, Twitch, Kick, or Custom transport, and enter the current server URL and stream key supplied by that platform. For SRT, choose Custom transport, enter the complete `srt://` listener URL, and leave Stream key blank. Each destination has its own encrypted credential slot; switching platforms loads that platform’s saved values instead of overwriting another platform’s credentials. The Direct multistream selector can include up to two selected profiles, but only configured destinations are started.

Open **Go Live**, press **Go Live**, approve Android’s screen-capture consent dialog when using screen capture, and allow microphone/camera access when Android requests it. The foreground service owns the RootEncoder microphone startup and reports a specific error if capture cannot be prepared. Once connected, the Studio surface exposes mute and local recording controls. Recording files are written to app-private storage under the `recordings` directory and are indexed by the Library screen.

If the connection drops, the service reports a reconnecting state and attempts up to three bounded retries using increasing delays. A failed authentication is not retried indefinitely; the UI asks the creator to check the selected platform and rotate or replace the stream key if necessary.

## Platform setup

Unictoos uses the platform-provided custom RTMP/RTMPS workflow for its reliable core path. This keeps platform credentials out of the media engine and allows a creator to stream without mandatory OAuth integration. YouTube and Twitch show common server hints in the app. Kick intentionally asks the creator to copy the current ingest URL from the Kick dashboard rather than relying on a stale hard-coded endpoint.

Platform OAuth, unified chat, alerts, scheduling, thumbnails, metadata publishing, moderation, guests, cloud backup, remote control, bonding, and USB capture remain separate integrations. Direct two-output fan-out is wired through the shared RootEncoder path, while independent per-destination health and retry policy remain future work. The active product surface intentionally stays focused on destination setup, scenes, capture, and broadcast control.

## Creator features in this milestone

| Area | Current behavior |
|---|---|
| Scenes | Select scenes, create portrait or landscape scenes, toggle sources, save source groups, and choose local transition metadata |
| Studio | Branded preview surface, destination readiness, bitrate/FPS cards, microphone state, mute, recording, Go Live, Stop, camera switching, and actionable notification controls |
| Destinations | Separate YouTube, Twitch, Kick, and Custom RTMP credential slots with secure local storage |
| Reliability | Permission checks, RootEncoder-owned microphone startup, explicit capture readiness, bounded reconnects, authentication error handling, cleanup, clearer preflight outcomes, and support diagnostics |
| Recordings | Start/stop local MP4 recording, index saved files in Library, retain chapter-style markers, validate trim plans, and export trimmed MP4 copies locally |

## Architecture

The application is organized around a Compose UI layer, shared domain models, a ViewModel state layer, a Keystore-backed credential store, local SQLite analytics, a Media3 recording editor, and a foreground media service. The service owns MediaProjection, microphone capture, hardware encoding, RootEncoder transport, recording, reconnect scheduling, notifications, and cleanup. The UI observes a process-local status bus for connection, bitrate, live, error, recording, mute, and disconnect states. Settings can export a redacted support bundle containing compatibility checks, the selected profile, session status, and bounded lifecycle diagnostics; Library exposes the recent session event timeline and local trim/export workflow.

The project uses [RootEncoder](https://github.com/pedroSG94/RootEncoder) under its Apache-2.0 license for the open-source RTMP/RTMPS/SRT/media pipeline integration. v0.4.9 keeps the bounded two-slot shared-encoder adapter around RootEncoder’s `MultiStream` API, aggregates callbacks across configured destinations, and clears partial slots before a complete reconnect attempt. The service deliberately exposes aggregate session state; independent per-destination dashboards and retry policy remain future work. See `RELEASE_NOTES_v0.4.9.md`, `docs/V0.4.4_OPEN_SOURCE_EXPANSION.md`, and `THIRD_PARTY_NOTICES.md` for the current migration boundary and dependency attribution.

## Safety and privacy

Unictoos must never log stream keys, include them in exported scenes, or commit them to the repository. Use a disposable or rotatable test destination when validating the app. Screen capture always requires Android user consent. The foreground service exists because long-running capture and streaming work must remain visible to the user.

Before a public stable release, the project must complete a physical-device matrix, 30–120 minute reliability sessions, network interruption tests, thermal/battery tests, encoder capability checks, microphone-level validation, recording playback/share validation, and YouTube/Twitch/Kick ingest validation.

## License

Unictoos is free and open-source under Apache-2.0. Third-party dependencies remain subject to their own licenses and notices.
