# Unictoos v0.4.2

## Maximum-feasible professional feature release

Unictoos v0.4.2 extends the mobile-first broadcast workflow without claiming provider or hardware capabilities that cannot be verified inside the Android repository. The release keeps RootEncoder 2.8.0 and the generation-safe capture lifecycle while adding creator controls and integration-ready contracts around the existing pipeline.

## Implemented locally

The Studio now exposes live encoder telemetry with bitrate, FPS, dropped-frame count, and the most recent measured transport label. The bitrate/FPS badge is also visible over the live preview. Camera capture sessions have a real front/back camera switch command routed through the foreground service, and the persistent broadcast notification now exposes Open Studio, mute/unmute, record/stop recording, and stop controls.

Scenes now include one-tap Portrait Live, Gameplay + Camera, and Talk Show templates. Settings can import a safe configuration file through Android’s document picker. Import restores scene layouts only; destination URLs and stream keys are not imported. Library now includes a typed last-session recap with duration, bitrate, FPS, dropped frames, and completion time, while recordings remain app-private until the creator shares them.

## Integration-ready streaming changes

The existing RootEncoder `MultiStream` path is now configured for a bounded two-output shared encoder. The Studio builds an endpoint list from explicitly selected platform profiles and sends no more than two valid endpoints to the foreground service. The service validates every endpoint, starts each selected slot, stops both transport families during teardown, and reconnects the active endpoint list together as one aggregate session. Per-destination health and independent retry policy remain future work; a destination failure is therefore surfaced as an aggregate session failure rather than being reported as independently healthy.

The adapter also routes `srt://` endpoints through RootEncoder’s SRT transport. SRT is intended for a compatible SRT listener or relay and is not presented as a replacement for the YouTube, Twitch, or Kick dashboard ingest flows, which normally provide RTMP or RTMPS URLs. No SRT listener was available in the sandbox, so this path is integration-ready and requires real network validation.

## Truthful capability boundaries

Provider chat, events, moderation, metadata, scheduling, clips, and OAuth remain disconnected until the user supplies platform credentials and a backend or documented PKCE flow. Cloud backup, remote control, SRTLA/RIST bonding, and UVC/capture-card input now have explicit Kotlin contracts for future implementations, but no fake buttons or false success states were added.

> **Important:** A successful build does not prove a two-destination session or SRT listener interoperability. Those require a physical Android device, two valid destination credentials, a compatible SRT receiver where applicable, and extended network testing.

## Verification

The static feature smoke suite passes with 111/111 checks. The complete JVM unit-test suite passes. Debug, instrumentation APK assembly, lint, and minified release assembly are the remaining release gates for this build environment; physical-device capture, multistream, SRT, camera-switch, and notification-action testing remain required before production distribution.

Version: `0.4.2`  
Android version code: `43`  
Compile SDK: `37`  
Target SDK: `36`  
RootEncoder: `2.8.0`
