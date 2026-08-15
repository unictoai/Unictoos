# Unictoos v0.2.0-alpha6

## Consolidated engineering remediation build

Alpha6 combines the validated engineering hardening completed after the alpha5 graphics-resource failure report. It is intended for controlled physical-device testing on Android 15, especially the Infinix X6853.

## Included fixes

- Serializes RootEncoder, MediaProjection, network, recording, and lifecycle callbacks through the service main-thread queue.
- Adds generation guards so callbacks from released encoder pipelines cannot mutate a newer session.
- Tracks intentional MediaProjection teardown separately from Android revocation.
- Makes failed-capture cleanup explicit and release-once before retry.
- Keeps capture intent state across Activity recreation during permission and projection flows.
- Separates stream-only storage preflight from recording/practice storage requirements.
- Waits for recording output stabilization before MP4 validation and session finalization.
- Bounds scene source geometry to the normalized composition canvas and debounces continuous scene persistence.
- Prevents empty-scene reorder failures.
- Discloses that combined screen-and-camera scenes currently use single-source capture and disables scene editing while live.
- Locks next-session-only destination, quality, audio, and latency settings during an active session.
- Stops the active encoder and cancels reconnect behavior on authentication failure.
- Binds elapsed-time and auto-stop callbacks to the active session generation.
- Adds a bounded, redacted diagnostics buffer that excludes endpoint and secret-like values.

## Scope limits

This build does **not** implement multi-destination streaming, a GPU compositor, OAuth integrations, or a new credential-encryption format. Existing Keystore-backed credential encryption remains unchanged.

The graphics-resource exhaustion issue remains a real-device-gated item. Alpha6 contains explicit failed-pipeline release before retry plus the subsequent stale-callback and lifecycle hardening, but sandbox validation cannot substitute for the Infinix X6853 reproduction test.

## Automated validation

The following checks must pass before publication:

- `lintDebug`
- `testDebugUnitTest`
- `assembleDebug`
- `tools/feature_smoke_test.py` with 47/47 checks passing
- APK metadata verification for versionCode 24 and versionName 0.2.0-alpha6

## Required device test

Install alpha6, reproduce the graphics-exhaustion failure if possible, tap **Fix**, and confirm that the app reports that capture resources were released before starting capture again. Repeat the failure-and-retry sequence at least three times, then run camera capture and screen capture separately for at least 10 minutes each. Also test Activity recreation during a pending permission/projection flow, background/foreground during preview, authentication failure with an invalid test key, recording stop/finalization, and low-storage stream-only versus recording behavior.

If graphics exhaustion recurs, capture an Android bug report immediately after the failure and upload it without stream keys or other credentials.
