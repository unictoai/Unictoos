# Unictoos v0.2.1 — Mobile-first rebuild

## Overview

Unictoos v0.2.1 is a controlled rebuild of the mobile creator experience. The release reorganizes the Studio around the real broadcast workflow: check readiness, inspect the preview, start one destination, use compact live controls, and review health without navigating through a crowded control wall.

## Mobile-first Studio workspace

The Studio now uses a deliberate hierarchy with a branded header, scene and capture summary, preview card, one primary Go Live or Stop action, compact microphone/recording/marker controls, destination readiness, session health, and a collapsed setup section for secondary controls. Practice mode, scene editing, destination settings, and auto-stop remain available without competing with the primary broadcast action.

The preview surface now uses explicit, high-contrast status labels and clearer empty, preparing, live, and error states. Error recovery remains explicit: the Fix action releases the failed capture pipeline and requires a fresh capture preparation rather than retrying on top of stale resources.

## Capture stability changes

The local preview buffer is clamped to the active encoder profile while preserving the preview view’s aspect ratio. This prevents a large portrait SurfaceView from requesting a larger graphics buffer than the selected stream profile needs.

On the affected Infinix X6853 device family, the local preview surface is detached after the stream becomes live while the encoder and capture source continue running. This device-scoped compatibility path is intended to avoid the firmware failure mode in which an attached local preview exhausts graphics resources during an active broadcast. Other devices retain the normal live preview behavior.

The release retains the earlier lifecycle safeguards: no periodic ForceRenderer during source-driven capture, generation-bound callbacks, serialized service state changes, idempotent failed-pipeline teardown, synchronous GL stop on render failure, duplicate preview-callback suppression, bounded diagnostics, recording finalization, and stream-only storage preflight.

## Preserved functionality

Secure credential storage and existing authentication/reconnect policy are unchanged. YouTube, Twitch, Kick, custom RTMP destinations, scenes, screen capture, camera capture, microphone controls, local recording, markers, session history, practice mode, health telemetry, and configuration export remain part of the application. Simultaneous camera-plus-screen composition remains explicitly disclosed as a future GPU-compositor feature rather than being presented as supported.

## Automated validation

The following checks passed for the v0.2.1 source:

- `lintDebug`
- `testDebugUnitTest`, including the preview-buffer and device-isolation policy tests
- `assembleDebug`
- `tools/feature_smoke_test.py` with 47/47 checks passing
- `git diff --check`
- APK manifest verification for versionCode 27 and versionName 0.2.1

## Physical-device gate

This release is an engineering/device-test build. Install it on the Infinix X6853, use a disposable Twitch test destination, and verify that the app can prepare capture, transition live, and remain stable for at least five minutes before attempting a 30-minute run. On the affected device, the preview is expected to pause once the broadcast is live; this is intentional and does not mean the stream stopped.

If graphics exhaustion returns, record whether it happened before or after the preview paused, the elapsed time, and whether the Twitch broadcast remained connected. Do not use an important broadcast or an irreplaceable stream key until this gate passes.
