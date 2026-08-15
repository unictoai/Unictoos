# Autonomous graphics-stability validation

## Scope

This report records what can be tested without the user’s Infinix X6853, USB-connected computer, or cloud device. The reported Infinix evidence remains the authoritative device result: the graphics-resource error appeared after approximately five minutes, and after Fix/restart the replacement run failed again after approximately ten minutes.

## Sandbox capability inventory

The sandbox initially had no ADB devices, no emulator binary, and no Android system image. I installed the Android emulator package, Android API 35 Google APIs x86_64 system image, and created an AVD named `unictoos-api35`. The emulator could start and expose an ADB transport, but the Android framework did not complete boot: the package service was unavailable (`cmd: Can't find service: package`), and the emulator later disappeared from ADB before an APK could be installed. Therefore no emulator launch or capture test is counted as passed.

| Test category | Result | Evidence |
|---|---|---|
| Kotlin unit tests | Passed | `testDebugUnitTest` completed successfully during v0.2.5 validation |
| Lint | Passed | `lintDebug` completed successfully |
| Debug assembly | Passed | `assembleDebug` completed successfully |
| Static smoke suite before hardening | 48/48 passed | v0.2.5 post-commit gate |
| Static smoke suite after hardening | 51/51 passed | Added MultiStream owner and retryable-release guards |
| APK manifest | Passed | package `com.unictoai.unictoos`, versionName `0.2.5`, versionCode `31` |
| Android emulator boot | Not passed | ADB transport appeared, but package service never became available and device disappeared |
| Infinix X6853 GPU behavior | Not executable here | No physical device or remote Android device is connected |
| 15–20 minute stream | Not executable here | Requires real camera/GPU/network/destination ingest |
| Graphics exhaustion and Fix/retry | Not executable here | Requires the Infinix GPU driver and sustained capture |

## Autonomous hardening completed

The single-destination runtime continues to use RootEncoder `MultiStream` at slot zero. Its adapter release path now attempts all teardown operations, remembers the first failure, resets its closed state when any operation fails, and rethrows the failure so `PipelineReleasePolicy` can keep the pipeline retryable. The static smoke suite now verifies that the service uses the single-slot adapter, does not instantiate the old GenericStream transport, and preserves retryability after a failed release.

These changes are sandbox-validated but not physical-device-validated. The latest hardening commit is local and must not be described as a confirmed Infinix fix.

## Boundary

An Android emulator on a generic x86_64 software-graphics stack cannot certify the Infinix X6853’s proprietary GPU/EGL/MediaCodec behavior. No autonomous test available in this sandbox can truthfully replace the missing physical-device run. Multistream fan-out remains disabled until the single-destination pipeline passes on the actual target device.
