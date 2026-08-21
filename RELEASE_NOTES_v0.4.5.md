# Unictoos v0.4.5

Unictoos v0.4.5 is a streaming-path hardening release. It focuses on making the complete mobile broadcast lifecycle more deterministic and more honest about what the device and destination are doing: capture preparation, preview attachment, microphone readiness, camera switching, RTMP/RTMPS/SRT transport selection, bounded direct multistream, reconnects, watchdog timeouts, recording state, notifications, and readiness messaging.

## Streaming reliability

- Hardened the RootEncoder `MultiStream` adapter with a strict two-destination limit.
- Added per-slot callback aggregation so the service reports a successful broadcast only after every configured destination has connected.
- Added destination context to fan-out connection failures and suppressed duplicate failure, disconnect, and authentication callbacks.
- Reconnect attempts now clear partially active RTMP/SRT slots before restarting the complete configured endpoint set.
- Preserved generation-safe capture and graphics recovery behavior; a failed pipeline must reach terminal release before a new pipeline is created.
- Capture preparation failures now clear queued starts, cancel the capture timeout, and expose an actionable retry message instead of leaving the service stuck in preparation.

## Audio, camera, preview, and recording

- Kept microphone probing off the main thread and applied the persisted sample rate, bitrate, echo-canceler, and noise-suppressor settings to RootEncoder preparation.
- Camera switching remains available only for active camera capture and is not exposed for screen-only sessions.
- Fixed readiness display for scenes with no enabled camera or screen source.
- Made recording callbacks explicit for started, recording, paused, resumed, and stopped states.
- Recording start/finalization failures no longer convert a healthy live broadcast into a false stream error; the recording state reports the failure independently.
- Practice mode does not enter a false live state when its local recording cannot start.

## Destinations and UI truthfulness

- RTMP and RTMPS destinations continue to require an ingest URL and stream key.
- Complete `srt://` listener URLs are accepted without a separate stream-key field.
- Settings copy now explains the different RTMP/RTMPS and SRT configuration forms.
- Foreground notification actions reflect mute state, recording startup/recording/finalization state, and live controls.
- Existing YouTube, Twitch, Kick, custom RTMP/RTMPS, and compatible SRT paths remain local-only and do not require an OAuth server or paid backend.

## Validation

The source includes focused regression coverage for SRT destination configuration, bounded two-destination endpoint assembly, lifecycle policies, watchdog behavior, pipeline release semantics, and no-source readiness. The release gate is intended to include the static smoke suite, JVM unit tests, debug lint, debug and instrumentation APK assembly, and the minified release build.

This repository-side validation cannot prove ingest success on every phone, carrier, encoder, or streaming platform. Physical-device testing should still verify microphone capture, preview stability, camera switching, single-destination RTMP/RTMPS, SRT listener compatibility, two-destination fan-out, reconnect after network loss, recording finalization, and Android notification actions on the target device.
