# Unictoos v0.2.1 Rebuild Plan

## Objective

Unictoos v0.2.1 is a controlled rebuild of the existing Android application, not a feature expansion without boundaries. The release must provide a calmer mobile-first workspace, preserve the stable platform and credential flows, and use a conservative capture lifecycle that minimizes GPU/EGL ownership on the Infinix X6853.

## Product structure

The primary navigation will expose five creator tasks: **Home**, **Studio**, **Scenes**, **Library**, and **More**. Settings, platform credentials, diagnostics, and app information belong under More rather than competing with the five primary tasks. The Studio screen is the main task surface and will present, in order: session state, preview, one primary broadcast action, compact capture controls, destination readiness, and health details. Advanced controls remain available but are visually subordinate.

## Streaming boundary

The v0.2.1 rebuild will keep one video source per session. A scene may describe additional layers, but the app will clearly disclose that simultaneous camera-plus-screen composition is not yet implemented. The capture service will own exactly one GenericStream generation, one video source, one microphone source, one preview surface, and one MediaProjection at a time. The UI will never directly own encoder or EGL objects.

The service will retain generation-bound callbacks, serialized state mutation, idempotent release, asynchronous recording finalization, storage preflight, redacted diagnostics, and secure existing credential storage. The CredentialStore encryption format and existing authentication/reconnect policy are out of scope for this rebuild.

## Graphics-stability strategy

The device failure gate is treated as a hard release criterion. The rebuild will avoid periodic forced rendering, avoid preview reattachment during an active session, avoid scene-filter mutation while live, and avoid allocating a second capture pipeline for a retry. If the device reports a render failure, the service will transition once into a terminal failed-capture state, release the pipeline, and require a fresh capture permission flow before preparing again.

The initial v0.2.1 device profile will prefer a conservative 720p/30 FPS screen or camera path. The build will not claim that a software change is physically verified until the Infinix X6853 completes a controlled Twitch test without graphics-resource exhaustion.

## UI acceptance criteria

The new UI must have a single obvious primary action, consistent spacing, no clipped controls at 1080 x 2436 portrait dimensions, clear disabled states while a session is active, a destination setup call to action when credentials are absent, and an explicit error recovery card. Navigation must remain reachable with one hand and must not expose seven competing bottom navigation destinations.

## Release gates

Each implementation commit must pass `lintDebug`, `testDebugUnitTest`, `assembleDebug`, `git diff --check`, and the complete feature smoke suite. The release commit must include versionCode 27, versionName `0.2.1`, a reproducible debug APK checksum, release notes, and a physical-device testing procedure. The release must remain marked as an engineering/device-test build until the Infinix verification is complete.
