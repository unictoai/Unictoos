# Unictoos v0.1.0-alpha18

## Production hardening release

Alpha18 is the first implementation pass from the Unictoos production-improvement review. It focuses on preventing false-ready sessions, making lifecycle behavior authoritative, and making diagnostics honest about what the Android streaming engine actually measures.

## Implemented

- Added an authoritative, testable stream lifecycle model with explicit `CONNECTING` and `STOPPED` states.
- Rejected duplicate start requests and prevented stale RootEncoder callbacks from resurrecting a stopped session.
- Added capped exponential reconnect backoff with bounded jitter and failure classification for network, timeout, authentication, server rejection, encoder, and configuration failures.
- Added Android network callbacks and MediaProjection stop handling, with cleanup of projections, preview surfaces, reconnect callbacks, and service resources.
- Added preflight checks for RTMP/RTMPS endpoint format, stream profile bounds, storage, network availability, battery state, and critical thermal state.
- Changed health presentation so FPS is sourced from RootEncoder’s measured FPS callback, while unavailable audio and dropped-frame telemetry is shown as `—` instead of a fabricated zero.
- Added recording storage checks and post-recording MP4 validation. Local recording quality still follows the active RootEncoder stream profile.
- Preserved corrupted scene JSON locally before falling back to defaults, preventing silent scene-data loss.
- Added persisted normalized source geometry controls for X, Y, width, and height in the Scenes editor.
- Added the first real GPU scene-output milestone: enabled TEXT sources are rendered through RootEncoder’s GL filter path into preview, broadcast, and recording output.
- Threaded the selected scene payload from the Compose editor to the foreground service.
- Added regression tests for lifecycle transitions, failure policy, preflight, scene composition planning, configuration export, and existing behavior.

## Important limitations

Alpha18 does **not** claim full multi-source compositing. RootEncoder still supplies one active camera or screen video source. TEXT overlays are rendered in the GPU output path, but simultaneous screen-plus-camera PiP, image layers, color backgrounds behind video, transitions, independent recording quality, multi-destination streaming, official platform APIs, chat, and moderation remain separate follow-up work.

The platform adapters remain disconnected boundaries for YouTube, Twitch, and Kick. Users still provide destination URLs and stream keys, which remain encrypted on-device by the existing credential store. No stream key is included in configuration export.

## Validation target

The release candidate must pass `lintDebug`, `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, and the repository smoke suite. Physical-device testing must include the confirmed Infinix X6853 / Android 15 device, camera practice, screen practice, text overlay visibility, network interruption, Android capture revocation, stop/restart, and an unlisted/private platform stream.
