# Unictoos v0.1.0-alpha06

This stability release addresses the crash-prone paths identified during a deep sandbox audit of the alpha05 build.

## Stability fixes

The Android manifest now declares network-state access, foreground microphone service permission, optional camera hardware, and explicit backup-exclusion resources. The foreground service now selects MediaProjection and microphone service types deliberately, catches Android service-start security/configuration failures, and reports a controlled error instead of allowing an unhandled crash. The microphone readiness probe catches permission and invalid-configuration failures and verifies that `AudioRecord` is initialized before use.

The Compose startup and settings paths now recover from stale saved enum values and empty scene state. Scenes without an enabled screen or camera source no longer accidentally request camera capture. Home and Studio now show actionable error cards with the reported failure message and a direct Fix action.

## Validation

The following sandbox checks passed:

| Check | Result |
|---|---|
| Android lint | Passed with zero errors |
| JVM unit tests | Passed |
| Debug APK assembly | Passed |
| Release APK assembly | Passed |
| Static feature smoke suite | 33/33 checks passed |
| APK package and launchable-activity inspection | Passed |
| Manifest permission, service, backup-resource inspection | Passed |

The sandbox has no connected Android device and no installed emulator/AVD, so a process-level launch trace and physical capture test could not be executed here. The attached stability audit records this limitation and the exact physical-device test matrix still required.

## Build details

- Application ID: `com.unictoai.unictoos`
- Version name: `0.1.0-alpha06`
- Version code: `6`
- Build type: debug alpha
- Minimum Android version: Android 10 / API 29
- Target SDK: 35

This remains an alpha build. Install it on a physical device and test launch, every bottom tab, microphone denial/approval, screen capture, camera-only capture, recording, stop/restart, and private/unlisted platform ingest before an important broadcast.
