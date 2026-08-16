# Unictoos v0.3.0 — RootEncoder graphics-lifecycle remediation

Unictoos v0.3.0 upgrades the streaming engine to RootEncoder 2.8.0 and hardens the preview/capture lifecycle around the recurring graphics-resource exhaustion path.

## Remediation changes

| Area | Change |
|---|---|
| RootEncoder | Upgraded `com.github.pedroSG94.RootEncoder:library` from 2.5.9 to 2.8.0. The project now compiles against Android API 37 with the compatible AGP 8.13.2, Gradle 8.13, and Kotlin 2.3.21 toolchain. |
| Surface ownership | `PreviewSurfaceView` registers one holder callback only when attached, unregisters on detach, and no longer emits a synthetic `surfaceDestroyed` event from listener release. Android’s real holder lifecycle remains authoritative. |
| Release boundary | Removed the fixed 150 ms graphics settle delay. A pipeline now reaches a `TERMINAL` release state only after the underlying RootEncoder release call completes successfully. |
| Failed recovery | Failed releases remain retryable and block creation of a new capture pipeline. A new generation cannot start on top of an incompletely released previous generation. |
| Generation safety | Graphics-failure recovery carries and validates the pipeline generation, preventing stale crash callbacks from tearing down a newer pipeline. |
| Infinix policy | The existing Infinix preview-isolation policy remains enabled and unchanged. |
| Recording API | Updated the adapter for RootEncoder 2.8.0’s `startRecord` track-selection API. |

## Automated validation

The clean debug gate passed `lintDebug`, `testDebugUnitTest`, `assembleDebug`, and `assembleAndroidTest`. The expanded static smoke suite passed **77/77 checks**. Unit coverage includes a 50-cycle terminal release/recreate model test, generation mismatch protection, failed-release retryability, and Android-runtime preview listener tests covering 50 repeated listener recreate cycles.

The R8 release gate passed with a bounded 1024 MiB Gradle heap after the initial 768 MiB attempt reported Java heap space and an earlier 1024 MiB attempt was terminated under sandbox memory pressure. The final retry completed successfully.

## Validation boundary

The sandbox has no connected Android device. Therefore, instrumentation tests were compiled but not executed on hardware, and no physical 20–50 cycle capture/preview soak or Infinix X6853 streaming soak can be claimed from this environment. The release is a lifecycle-hardening build with automated contract coverage; physical graphics-stability confirmation remains required before declaring the device-specific bug disproven.
