# Unictoos v0.3.3 — Deep-dive stability and capture correctness

Unictoos v0.3.3 is a corrective release following a repository-wide audit of the streaming service, Android lifecycle bridge, destination state, foreground-service declarations, preview identity policy, and automated validation.

## Verified fixes

| Area | Issue found | Correction |
|---|---|---|
| Camera capture startup | The service used camera capture but the manifest declared only media-projection and microphone foreground-service types. | Added `FOREGROUND_SERVICE_CAMERA` and declared the service as `mediaProjection|microphone|camera`; camera preparation now requests the camera service type explicitly. |
| Capture preparation races | Projection and camera preparation requests could overlap while both were releasing and recreating RootEncoder resources. | Added a main-service preparation mutex so only one preparation/release transaction can run at a time. |
| Preview detach compatibility | A detach command with a missing or legacy token could detach an active preview surface. | Treat any tokenless detach as stale whenever an active preview token exists. |
| Destination readiness | Persisted Twitch, Kick, and custom credentials were not reflected in the destination list after a cold start, causing incorrect configured counts and misleading support diagnostics. | Hydrate every destination from the secure credential repository during ViewModel initialization and refresh the selected platform state. |
| Credential input hygiene | Server URLs and stream keys were saved with accidental leading or trailing whitespace. | Normalize both values before persistence and endpoint construction. |
| Capture-mode intent crossover | A normal broadcast request could inherit a previous practice-mode flag while permissions or screen-capture approval were pending. | Clear practice mode at the start of every ordinary broadcast request. |

## Validation

The v0.3.3 validation sequence completed with the following results:

| Gate | Result |
|---|---:|
| Static smoke suite | **88/88 passed** |
| Unit tests | Passed |
| Android lint | Passed with existing non-blocking warnings |
| Debug APK assembly | Passed |
| Android-test APK compilation | Passed |
| R8 release assembly | Passed with a 1024 MiB heap |
| APK version | `0.3.3`, version code `39` |
| Package | `com.unictoai.unictoos` |

The existing RootEncoder 2.8.0 migration, terminal release boundary, generation-safe graphics recovery, Infinix preview-isolation policy, duplicate callback protection, and no-synthetic-destroy contract remain in place.

## Physical validation boundary

The sandbox has no connected Android device or GPU-accelerated emulator. Physical validation is still required on the Infinix X6853: repeated camera and screen capture preparation, at least 20 start/stop cycles, at least 50 preview recreation cycles, long-duration broadcasts, graphics-exhaustion Fix/retry recovery, Activity recreation, microphone/camera switching, and network transitions. This release does not claim those hardware-only tests were executed in the sandbox.

## Installation guidance

Use the installable debug APK from the GitHub release for device testing. The R8 artifact is unsigned and is provided for build verification, not direct installation.
