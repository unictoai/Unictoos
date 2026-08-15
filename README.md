# Unictoos

**Unictoos** is a mobile-first, open-source Android live-streaming and recording studio for creators who want to broadcast from their phone to YouTube, Twitch, Kick, and compatible RTMP/RTMPS destinations.

The project is intentionally not a desktop OBS clone. It uses a touch-first workflow with a dashboard, scenes, a focused Studio screen, local recordings, secure destination management, and simple broadcast controls.

## Current status

The repository now contains the native Android alpha foundation and the benchmark-driven Creator Core Plus pass. The app includes Kotlin and Jetpack Compose screens, the uploaded Unictoos brand identity, Home, Scenes, Studio, Engage, Library, and Settings areas, scene source visibility controls, source creation for screen/camera/image/text/color layers, portrait/landscape scene creation, per-platform destination setup, Keystore-backed credential storage, MediaProjection screen capture, camera-only capture, microphone validation, RootEncoder RTMP/RTMPS transport, bounded reconnect attempts, preflight network and permission checks, mute control, local MP4 recording, recording indexing, and an opt-in app-only sponsor-space policy.

The current build remains an **alpha engineering milestone**. It compiles and packages successfully, but real-device validation against YouTube, Twitch, and Kick is still required before it should be treated as production-stable. The current engine can use screen capture or camera-only capture; a true screen-plus-camera picture-in-picture compositor is a planned next media milestone and should not be assumed from a scene containing both source types. Do not use an important broadcast or an irreplaceable stream key with an unvalidated development build.

## Latest test build

The latest device-test build is **Unictoos v0.2.2** with Android `versionCode 28`. It adds a polished first-launch onboarding flow, four branded 9:16 onboarding visuals, functional 16:9 and 9:16 scene formats, and retains the v0.2.1 graphics-stability path for the Infinix X6853. [Download the APK from GitHub Releases](https://github.com/unictoai/Unictoos/releases/tag/v0.2.2) or [download the APK directly](https://github.com/unictoai/Unictoos/releases/download/v0.2.2/unictoos-v0.2.2-debug.apk).

## Build locally

Requirements are Android SDK API 35 with build tools 35.0.0, JDK 21, and a physical Android device running Android 10 or later for capture and encoder testing.

```bash
./gradlew test assembleDebug
```

The debug artifact is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it on a connected device with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## First-use workflow

Open **Settings**, choose YouTube, Twitch, Kick, or Custom RTMP, and enter the current server URL and stream key supplied by that platform. Each destination has its own encrypted credential slot; switching platforms loads that platform’s saved values instead of overwriting another platform’s credentials. Save the destination, open **Scenes**, select a layout, and use the source switches to control which scene sources are enabled.

Open **Studio**, press **Go live**, approve Android’s screen-capture consent dialog, and allow microphone access. Unictoos checks that the microphone can be opened before it prepares the stream. Once live, the Studio surface exposes mute and local recording controls. Recording files are written to app-private storage under the `recordings` directory and are indexed by the Library screen.

If the connection drops, the service reports a reconnecting state and attempts up to three bounded retries using increasing delays. A failed authentication is not retried indefinitely; the UI asks the creator to check the selected platform and rotate or replace the stream key if necessary.

## Platform setup

Unictoos uses the platform-provided custom RTMP/RTMPS workflow for its reliable core path. This keeps platform credentials out of the media engine and allows a creator to stream without mandatory OAuth integration. YouTube and Twitch show common server hints in the app. Kick intentionally asks the creator to copy the current ingest URL from the Kick dashboard rather than relying on a stale hard-coded endpoint.

Platform OAuth, unified chat, alerts, scheduling, thumbnails, metadata publishing, moderation, guests, and multi-streaming remain separate integrations. The new Engage surface makes that boundary visible without pretending those provider APIs are already connected. They should not block the core path for starting a private test broadcast. See `mobile-streaming-benchmark.md` and `FEATURE_ROADMAP.md` for the staged plan.

## Creator features in this milestone

| Area | Current behavior |
|---|---|
| Scenes | Select scenes, create portrait or landscape scenes, and toggle individual sources |
| Studio | Branded preview surface, destination readiness, bitrate/FPS cards, microphone state, mute, recording, Go Live, and Stop |
| Destinations | Separate YouTube, Twitch, Kick, and Custom RTMP credential slots with secure local storage |
| Reliability | Permission checks, AudioRecord availability check, explicit capture readiness, bounded reconnects, authentication error handling, and cleanup |
| Recordings | Start/stop local MP4 recording from Studio and index saved files in Library |
| Advertising | Opt-in policy and provider-neutral app-only slot architecture; no advertising SDK or broadcast insertion is enabled in this alpha |

## Architecture

The application is organized around a Compose UI layer, shared domain models, a ViewModel state layer, a Keystore-backed credential store, an app-only ads policy, and a foreground media service. The service owns MediaProjection, microphone capture, hardware encoding, RootEncoder transport, recording, reconnect scheduling, notifications, and cleanup. The UI observes a process-local status bus for connection, bitrate, live, error, recording, mute, and disconnect states.

The project uses [RootEncoder](https://github.com/pedroSG94/RootEncoder) under its Apache-2.0 license for the open-source RTMP/RTMPS/media pipeline integration. See `THIRD_PARTY_NOTICES.md` for dependency attribution.

## Advertising policy

Unictoos does not place ads inside outgoing RTMP/RTMPS frames or local recordings. The current implementation provides a provider-neutral `AdProvider` contract, a persisted opt-in policy, and a clearly labeled non-live sponsor slot. The alpha build intentionally uses a no-op provider. A real ad network must only be enabled after privacy consent, region/age compliance, release-build configuration, frequency limits, and a review confirming that the media engine never receives ad content.

## Safety and privacy

Unictoos must never log stream keys, include them in exported scenes, or commit them to the repository. Use a disposable or rotatable test destination when validating the app. Screen capture always requires Android user consent. The foreground service exists because long-running capture and streaming work must remain visible to the user.

Before a public stable release, the project must complete a physical-device matrix, 30–120 minute reliability sessions, network interruption tests, thermal/battery tests, encoder capability checks, microphone-level validation, recording playback/share validation, and YouTube/Twitch/Kick ingest validation.

## License

Unictoos is free and open-source under Apache-2.0. Third-party dependencies remain subject to their own licenses and notices.
