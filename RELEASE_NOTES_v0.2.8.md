# Unictoos v0.2.8 — Master engineering remediation

Unictoos v0.2.8 applies the highest-value production-safety changes from the Master Engineering Remediation Specification in place. The existing Compose application, RootEncoder stack, Android Keystore encryption, and current streaming policies remain intact.

## Remediation summary

| Engineering area | v0.2.8 result |
|---|---|
| Authoritative state | Added specification-aligned destination states, aggregate state reduction, explicit recording lifecycle, and structured streaming errors. |
| Destination isolation | Added a provider-neutral `DestinationSessionManager` foundation with independent destination failure state, bounded retries, and truthful degraded/failed aggregation. |
| Recording | Recording readiness now depends on capture and encoder readiness rather than preview attachment. |
| Lifecycle safety | Preserved generation-bound callbacks, deterministic RootEncoder teardown, MediaProjection callback handling, and the Infinix preview-isolation policy. |
| Scene persistence | Added schema versioning, bounded source loading, safe field limits, and backward-compatible malformed-data handling. |
| Provider truthfulness | The UI distinguishes stream-key streaming, manual RTMP/RTMPS configuration, and backend-required OAuth/chat/moderation/events. |
| Thermal control | Added sustained-pressure debounce and recovery-window reset logic before reducing quality. |
| Diagnostics | Hardened redaction for RTMP endpoints, key-value secrets, JSON-style credentials, and Bearer authorization values. |
| Credential security | Preserved Android Keystore AES/GCM and the corrected legacy migration behavior from the previous remediation slice. |

## Validation

The v0.2.8 gate passed `lintDebug`, `testDebugUnitTest`, `assembleDebug`, `assembleAndroidTest`, R8-backed `assembleRelease`, source diff hygiene, the bounded source security audit, and the expanded static smoke suite. The smoke suite reports **68/68 checks passed**. Release minification required a bounded 1024 MiB Gradle heap in this sandbox because the default 768 MiB setting is insufficient for R8.

Instrumentation tests were compiled into an Android test APK, but no connected Android device was available for execution. Physical streaming soak tests therefore remain an explicit release risk. This release does not claim that the Infinix X6853 graphics-resource exhaustion issue is resolved, and it does not claim production-ready simultaneous multi-destination capture fan-out. Those claims remain gated by physical long-duration validation and a later runtime integration task.
