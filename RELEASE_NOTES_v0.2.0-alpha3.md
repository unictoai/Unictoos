# Unictoos v0.2.0-alpha3 — Priority 1 GL Fix Test Build

This prerelease contains the Priority 1 capture-pipeline hardening work for the confirmed RootEncoder `GL_OUT_OF_MEMORY` failure and the idle FPS telemetry correction.

The capture service now releases the active preview and MediaProjection before replacing the capture pipeline, recreates the RootEncoder `GenericStream` after repeated camera or screen preparation, allows a short settle period for RootEncoder’s asynchronous EGL executor teardown, and resets source references and capture state between attempts. FPS callbacks are exposed only for an active broadcast or practice session, and terminal/error paths reset FPS, bitrate, elapsed time, dropped-frame, audio, and recording telemetry. Studio also hides historical FPS while the session is not live.

The APK passed `lintDebug`, `testDebugUnitTest`, `assembleDebug`, and all 47 feature smoke checks. This is a **device-test build**, not a claim that Priority 1 is complete: the remaining gate is real testing on the Infinix X6853 running Android 15.

## Required test

Run at least 20 camera preview start/stop cycles, 20 screen-capture start/stop cycles, repeated camera/screen alternation, a 15-minute active preview, and a 10-minute Go Live attempt. Confirm that idle Studio shows FPS as `—` and that graphics exhaustion does not recur. If it does recur, capture logcat or an Android bug report during the controlled reproduction.

Foundation hardening, multistreaming, chat overlays, chroma key, and camera filters remain intentionally gated until this device verification is complete.
