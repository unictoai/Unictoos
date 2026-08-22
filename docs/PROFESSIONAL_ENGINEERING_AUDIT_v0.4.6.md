# Unictoos v0.4.6 Professional Engineering Audit

## Scope and baseline

This audit reviewed the Android application as a complete product rather than treating the RootEncoder service in isolation. The review covered foreground-service lifecycle, MediaProjection and Camera2 preparation, preview-surface ownership, microphone probing and DSP configuration, RTMP/RTMPS/SRT endpoint handling, bounded multistream callbacks, reconnect and watchdog behavior, recording state, notification actions, scene persistence, configuration export/import, security-source scanning, launcher resources, UI readiness, build metadata, and repository documentation.

The restored baseline was Unictoos v0.4.5, package `com.unictoai.unictoos`, with the existing bounded two-destination architecture. No credentials were present in the repository. The audit intentionally preserved the existing evidence boundary: source and emulator-independent gates can establish contracts and build integrity, but they cannot prove end-to-end platform ingest on a physical phone.

## Findings and dispositions

| Area | Finding | Disposition in v0.4.6 |
|---|---|---|
| Reconnect lifecycle | Retry exhaustion could leave a service and UI in a reconnecting state without a terminal cleanup boundary. | Added terminal failure cleanup that stops RootEncoder, cancels watchdog/retry work, removes the foreground notification, publishes an error, and stops the service. |
| Camera orientation | Camera preparation discarded the scene payload and could initialize the default portrait profile before a landscape start request arrived. | Camera preparation now receives the scene payload directly and selects the normalized profile before RootEncoder initialization. |
| Scene format | The Studio UI showed 16:9/9:16 metadata, but the encoder profile and preview buffer could remain portrait. | Added `StreamQuality.forAspectRatio`, applied it to service preparation and preflight, and used it for Studio preview dimensions and labels. |
| Preview ownership | Compose detachment could clear the view listener before the service received a terminal surface event, leaving stale preview state and a retained surface. | Detachment now reports an outstanding surface exactly once before listener release; explicit listener release remains non-synthetic. |
| Practice mode | Practice could publish `LIVE` immediately after requesting recording, before RootEncoder confirmed recording startup. | Practice publishes `LIVE` only after an active recording callback; startup failure becomes an explicit error. |
| Foreground service | Android security, invalid-configuration, and illegal-state errors during foreground startup were not all handled. | Added a guarded illegal-state boundary with user-facing error state and service cleanup. |
| Destination validation | Adapter start validation occurred after tracker mutation and accepted scheme-only malformed values through some paths. | Endpoint lists are trimmed and fully validated before slot state is changed; URI hosts are required. |
| Readiness truthfulness | Studio treated any active network object as available, even without Internet capability. | Studio and health telemetry now use `NET_CAPABILITY_INTERNET`. |
| Scene editing performance | Opacity slider updates synchronously serialized and wrote the complete scene graph on every drag event. | Opacity uses the existing debounced background persistence path. |
| Local data bounds | Scene restore and payload serialization did not consistently bound scene graph size and text fields. | Added safe limits for scenes, groups, sources, identifiers, names, text, and URIs. |
| JSON correctness | Manual configuration export did not escape all control characters. | Added JSON escaping for backspace, form feed, and low ASCII control characters with regression coverage. |
| Android polish | The full-color bitmap was used as both launcher and notification icon, causing lint warnings and poor status-bar semantics. | Added adaptive launcher resources, vector fallback, and a monochrome notification status icon. |
| Security audit quality | The source scanner falsely classified the intentionally named legacy preference key as a credential literal. | Scanner now targets quoted credential values and bearer literals; current source reports zero suspicious credential literals and zero direct log calls. |

## Validation matrix

| Gate | Result |
|---|---|
| Repository static smoke suite | Passed after expanding the suite for v0.4.6 contracts. |
| Security-source audit | Passed with zero suspicious credential literals and zero direct log calls. |
| JVM unit tests | Passed with the constrained single-worker Gradle configuration. |
| Debug lint | Passed; remaining warnings are third-party trust-manager reports, intentional orientation policy, KTX style suggestions, and toolchain/dependency advisory notices. |
| Debug APK | Assembled successfully with package `com.unictoai.unictoos`, versionCode `47`, versionName `0.4.6`. |
| Android instrumentation APK | Assembled successfully. |
| Release APK | Assembled successfully as a minified, resource-shrunk v0.4.6 artifact and verified with the package metadata gate. |
| Physical-device ingest | Not provable in the sandbox; requires the target Android device and disposable platform credentials. |

## Evidence boundary

The repository is materially more defensive after this audit, but no responsible engineering review should label it platform-production-ready solely from source inspection. The required external validation remains a matrix of at least one long single-destination session, a deliberate reconnect, preview detach/reattach, microphone mute/unmute, camera switch, local recording start/stop/finalization, screen-capture revocation, SRT listener compatibility, and bounded two-destination RTMP/RTMPS/SRT fan-out on the target device. Results from that matrix should be added to the release record rather than inferred from unit tests.
