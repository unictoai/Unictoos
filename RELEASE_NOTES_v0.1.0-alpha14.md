# Unictoos v0.1.0-alpha14

## Startup crash-fix release

Alpha14 hardens the alpha13 startup path against malformed, unavailable, or device-specific persistence failures. Stream-quality, audio, auto-stop, latency, thermal, scene, and credential reads now have safe startup fallbacks where appropriate; a credential-load failure no longer prevents the Studio shell from opening.

The patch does not alter Android Keystore encryption, credential write behavior, stream reconnect behavior, or authentication-error handling.

## Regression coverage

A new JVM regression test injects failing persistence repositories and verifies that StudioViewModel still starts with safe defaults: 720p/30 stream quality, Standard audio, no auto-stop, and Stable latency mode.

## Validation

Alpha14 passed `lintDebug`, `testDebugUnitTest`, and `assembleDebug` after the crash fix. The release build and 47-check feature smoke suite are being rerun for the published artifact.

## Installation

If alpha13 is installed, uninstall it or clear its app data only if the crash persists after installing alpha14. The alpha14 debug APK is intended for testing; the release APK is unsigned in this open-source build pipeline.
