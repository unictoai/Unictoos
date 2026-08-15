## Live-render scheduling hardening

Alpha8 is a Twitch-focused Priority 1 device-test build following an alpha7 stream that remained live for approximately 28 seconds before returning a graphics-resource exhaustion error on the Infinix X6853 running Android 15.

The investigation found that Unictoos enabled RootEncoder’s periodic `ForceRenderer` while Camera2Source and ScreenSource already delivered SurfaceTexture frame callbacks. That created two render producers for the same GL pipeline. RootEncoder’s encoder FPS limiter reduced some work, but the extra periodic producer could still enqueue redundant render tasks on slower encoder/EGL drivers.

Alpha8 disables the periodic ForceRenderer for camera and screen capture. RootEncoder’s normal SurfaceTexture callbacks remain responsible for rendering, and `prepareVideo` continues to enable the configured encoder FPS limit. The synchronous render-error callback and idempotent failed-pipeline teardown from alpha7 remain included.

## Included fixes

The release retains RootEncoder 2.5.9, synchronous GL stop on render failure, serialized generation-bound recovery, late-failure race protection, and exactly-once preview-surface callback registration. The new change specifically removes the redundant periodic live render loop while preserving preview, stream, recording, microphone, and source lifecycle behavior.

## Scope limits

This build does not implement multi-destination streaming, a GPU compositor, OAuth integrations, or a new credential-encryption format. Keystore-backed credential encryption and existing reconnect/authentication policy are unchanged.

The graphics-resource exhaustion issue remains physical-device gated. Alpha8 is the next controlled test candidate and must not be considered fixed until the Infinix X6853 completes a sustained stream without the error.

## Automated validation

The following checks passed for the alpha8 source:

- `lintDebug`
- `testDebugUnitTest`
- `assembleDebug`
- `tools/feature_smoke_test.py` with 47/47 checks passing
- APK metadata verification for versionCode 26 and versionName 0.2.0-alpha8

## Required device test

Install alpha8, use a disposable Twitch test destination, and run the same screen-capture stream that failed after 28 seconds. Test for at least five minutes first, then repeat for at least 30 minutes if stable. Also test the Fix action by forcing or reproducing the preview failure, confirm that the release message appears, and verify that a fresh capture can be prepared afterward.

Record the elapsed time if the graphics error returns, whether the stream was still connected, whether the preview remained visible, and whether the error occurred before or after any bitrate or thermal notification.
