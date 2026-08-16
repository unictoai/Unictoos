# Unictoos remediation audit

**Scope.** This audit maps the Master Engineering Remediation Specification onto the current `main` branch at v0.2.7. The existing application must be modified in place; no unrelated streaming-stack rewrite is justified.

## Current boundary map

| Specification area | Current implementation | Gap or risk | Safe next action |
|---|---|---|---|
| Build and UI shell | v0.2.7 builds after the glass navigation repair; existing screens remain routed through `UnictoosApp` | No current build blocker | Preserve as a baseline and run the full required gate after each change |
| Credential migration | `CredentialStore` uses Android Keystore AES/GCM for new values, but legacy values are copied directly into new-format fields | A legacy plaintext value or a ciphertext from a different format can be treated as current ciphertext and become unreadable; migration does not explicitly classify format | Fix migration with explicit legacy-format handling and instrumentation coverage before touching streaming |
| Credential secrets | Credentials are kept in the credential repository and are not intentionally included in the diagnostics export | Future changes must avoid copying full keys into UI state, logs, or diagnostics | Add only redacted/masked assertions; do not change the encryption design |
| Global stream state | `StreamStatus` and `StreamSessionState` model one global stream with booleans for recording/capture/preview | The specification requires independent destination state, `DEGRADED`/`FAILED`, and a separate recording state | Introduce pure domain state contracts first; do not wire them into RootEncoder until tests define behavior |
| Destination runtime | `SingleDestinationMultiStreamAdapter` binds RootEncoder RTMP slot 0. `MultiStreamDestinationManager` currently owns bounded slots but is explicitly not wired into the service | No independent per-destination retries or failure isolation yet | Implement/test a provider-neutral destination session state machine before runtime fan-out |
| RootEncoder ownership | `StreamingForegroundService` owns the adapter, MediaProjection, sources, preview, recording, reconnect, thermal handling, and callbacks | Responsibility concentration makes future fan-out risky; generation handling already exists and must be preserved | Extract pure coordinator contracts incrementally; retain service ownership until a runtime testable replacement exists |
| MediaProjection | Callback is registered before source setup; external stop invalidates capture and stops the session; release is guarded | Physical lifecycle scenarios remain unverified in the sandbox | Add pure lifecycle tests and preserve generation/release ordering |
| Preview and recording | Preview-free streaming is supported for the Infinix policy, but `startRecording` still requires `previewAttached` | Recording can incorrectly depend on preview availability | First add a pure readiness policy and tests, then make the service check encoder/capture readiness instead of preview |
| Device policy | `CaptureCompatibilityPolicy` centralizes the Infinix preview rule and preview sizing | The report is useful but policy coverage remains narrower than the specification | Extend policy only through pure testable data, without adding device-specific branches to generic streaming classes |
| Encoder resilience | Graphics failures release the pipeline, increment generation, and require a new capture attempt; bounded reconnect and bitrate policies exist | Recovery and physical soak behavior are not proven; no broad encoder error model | Preserve the current release path and add pure recovery/state tests before further capture changes |
| Reconnect | Reconnect is currently one global endpoint with bounded backoff and jitter; auth errors stop retrying | Destination-level reconnect isolation is unavailable | Define retry classification/backoff contracts independently first |
| Provider integrations | Platform presets and generic RTMP configuration exist; no verified provider OAuth/API adapters are present | The specification forbids advertising unsupported provider behavior | Keep provider capabilities truthful; return a backend-required state rather than faking OAuth or chat |
| Recording | RootEncoder recording is validated asynchronously and stop is mostly idempotent | No explicit recording state machine; service still gates start on preview | Add a pure recording state machine and readiness tests |
| Scene persistence | `ScenePayloadCodec` and `SceneCompositionPlan` exist | Schema-versioned migration and malformed-data coverage require audit | Add tests before changing codec behavior |
| Telemetry and thermal policy | Health samples, diagnostics, adaptive bitrate, and thermal reduction exist | UI/state model remains mostly global and thermal debouncing is limited | Keep sampled telemetry; add policy tests before refactoring hot paths |
| Testing | Unit tests cover several policies and the global stream state; CredentialStore coverage is instrumentation-only and narrow | Key required scenarios are not yet represented | Start with migration tests and pure state/readiness tests; do not claim device soak completion |

## Recommended implementation order

The first coherent production task is **CredentialStore migration correctness**, because it is self-contained, directly required by the specification, and does not touch the capture or encoder path. It must be completed with instrumentation tests for plaintext legacy values, current-format legacy ciphertext, malformed values, partial values, pre-existing new values, and idempotence.

The second safe task is **pure destination/session state contracts**. This can establish independent destination states, global aggregation rules, bounded retry classification, and truthful UI data without changing RootEncoder behavior. Only after those contracts pass should runtime fan-out be integrated.

The third task is **preview/recording readiness separation**, followed by a pure recording state machine. This is a service behavior change and therefore needs device verification before being advertised as fully resolved.

Runtime multi-destination fan-out remains gated behind the existing single-destination physical soak requirement. The specification requires at least 30 minutes for PR validation and 2+ hours for a release candidate; the sandbox cannot provide that evidence for the Infinix X6853.

## Non-negotiable constraints carried forward

CredentialStore encryption must not be replaced. RootEncoder must not be replaced with an unrelated stack. The Infinix preview-free fallback must remain policy-driven. Multistream Stage B must not be declared stable based on compilation alone. No UI may claim `LIVE`, provider connectivity, OAuth, chat, or moderation capabilities that the runtime does not actually implement.
