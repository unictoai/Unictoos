# Unictoos stability audit

## Scope

This audit covers the alpha05 launch path, Compose startup state, Android manifest, foreground service entry points, microphone and camera permission flows, screen capture, local recording, secure destination setup, scene source controls, Engage routing, backup behavior, and APK packaging.

## Sandbox constraints

The sandbox does not have a connected Android device or an installed emulator binary. `adb devices -l` returned no devices, and no AVD was available. Therefore, a true process-level launch trace and physical microphone/camera/MediaProjection test cannot be completed in this environment. The connected Android test task completed without running a device test; the result is treated as package/build validation rather than proof of runtime behavior.

## Initial findings

The first deep lint run found four release-blocking issues that could produce runtime failures: the preflight network check lacked `ACCESS_NETWORK_STATE`; the microphone probe did not explicitly guard the `AudioRecord` constructor against permission and argument failures; the foreground service declared microphone capture without `FOREGROUND_SERVICE_MICROPHONE`; and the camera permission did not declare camera hardware as optional for large-screen devices.

The source audit also found crash-prone state assumptions: selecting the first scene unconditionally, parsing saved platform/source/aspect-ratio enum names with `valueOf`, and requesting camera capture for scenes that had neither an enabled screen nor camera source.

## Fixes applied

The manifest now declares network state, foreground microphone, optional camera hardware, Android 12+ data extraction rules, and legacy backup exclusions. The microphone probe now catches `SecurityException` and `IllegalArgumentException`, verifies `AudioRecord.STATE_INITIALIZED`, and returns a controlled failure. Foreground-service startup now selects MediaProjection versus microphone service types deliberately and catches Android `SecurityException` and `IllegalArgumentException` instead of allowing an unhandled crash.

The Compose shell now has a safe fallback scene, safe enum recovery for restored state, and a safer capture-mode selection policy. The smoke-test tool checks all implemented service actions, UI tabs, permissions, capture paths, secure credentials, recording library, preflight, fallback guards, backup rules, APK identity, and launchable activity.

## Validation results

| Check | Result |
|---|---|
| `./gradlew lintDebug` | Passed with no errors |
| `./gradlew testDebugUnitTest` | Passed |
| `./gradlew assembleDebug` | Passed |
| `./gradlew assembleRelease` | Passed |
| `python3 tools/feature_smoke_test.py` | 33/33 checks passed |
| APK package and launchable activity inspection | Passed |
| Manifest permission/service/resource inspection | Passed |
| Physical Android launch and capture test | Not available in sandbox; requires a device or emulator |

The remaining physical-device test must install the release APK, launch Home, navigate every bottom tab, create a scene/source, set a test destination, test microphone permission denial and approval, run screen and camera-only capture, record locally, stop/restart the service, and verify private/unlisted platform ingestion.
