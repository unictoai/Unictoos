# Unictoos v0.3.2 Directive Audit

## Scope

This audit compares the Updated Master Engineering Directive with the current `main` branch after v0.3.1. The repository already contains RootEncoder 2.8.0, explicit terminal release states, generation-bound encoder callbacks, single-flight graphics recovery, retryable failed release, preview-isolation for the affected Infinix path, preview-independent recording readiness, truthful provider capabilities, scene schema hardening, thermal debounce, secret redaction, and Compose shell recomposition improvements.

## Verified remaining gaps

| Severity | Area | Finding | v0.3.2 action |
|---|---|---|---|
| P0 | Preview replacement | Service reuse was based on `previewAttached`, dimensions, and validity. A new Surface object with identical dimensions could be treated as the existing attachment. | Add explicit Surface identity plus a monotonic preview token. Same-size replacement detaches the old preview output and attaches the new one without recreating capture or network state. |
| P0 | Stale preview destruction | MainActivity discarded the destroyed Surface identity, so a late Surface A destroy event could detach active Surface B. | Track the active Surface in MainActivity and ignore stale destroys; pass the token to the service for a second defensive check. |
| P1 | MediaProjection callback lifecycle | The intentional-release flag was not reset until after callback registration, leaving a lifecycle window and making per-generation intent less explicit. | Reset intentional-shutdown state before registering each new projection callback. |

## Existing protections retained

The release boundary remains authoritative: new capture creation requires `TERMINAL`, failed release remains observable and retryable, and generation mismatch prevents stale completion. Preview detachment still stops only the preview output and does not stop the underlying capture, encoder, microphone, camera, or network stream. The RootEncoder dependency is not upgraded again because the current 2.8.0 build is already present and source-compatible.

## Verification boundary

Pure unit tests can prove identity/replacement decisions and generation contracts. Android instrumentation tests can compile and exercise view-listener lifecycle, but no connected Android device is available in the sandbox. Therefore, physical 20+ capture cycles, 50+ preview recreation cycles, network switching, and long-running ingest remain required before declaring the real-device graphics exhaustion issue disproven.
