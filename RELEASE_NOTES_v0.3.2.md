# Unictoos v0.3.2 — Updated Master Engineering Directive remediation

Unictoos v0.3.2 applies the remaining verified preview-lifecycle gaps identified during the Updated Master Engineering Directive audit. It does not repeat the already completed RootEncoder 2.8.0 and terminal release-boundary work.

## Correctness changes

| Severity | Area | Root cause | Fix |
|---|---|---|---|
| P0 | Preview Surface replacement | The service previously reused an attached preview when dimensions matched, without proving that the incoming Android `Surface` was the same object. | Added object-identity and monotonic token validation. A same-size replacement is treated as a replacement and only the preview output is reattached; capture, encoder, microphone, camera, and network ownership are preserved. |
| P0 | Stale preview destruction | MainActivity discarded the destroyed Surface identity, allowing a late Surface A destroy event to risk detaching active Surface B. | MainActivity tracks the active Surface and ignores stale destroys. The service also rejects stale detach tokens defensively. |
| P1 | MediaProjection callback lifecycle | Intentional-shutdown state was not reset explicitly before each new projection callback registration. | Reset intentional-release state before registering each projection callback, preserving unexpected revocation reporting for the new generation. |
| P1 | Stop/reconnect contracts | Existing state rules lacked explicit repeated-stop and reconnect recovery regression coverage. | Added repeated-stop admission checks across active/error states and reconnect-to-live/reconnect-to-stop transition coverage. |

The Infinix X6853 preview-isolation policy, RootEncoder 2.8.0 dependency, explicit terminal release boundary, generation-safe graphics recovery, recording readiness decoupling, credential encryption, provider capability truthfulness, scene persistence, thermal debounce, and diagnostics redaction remain unchanged and protected.

## Validation

The final v0.3.2 gate includes clean lint, unit tests, debug APK assembly, instrumentation-test APK compilation, R8 release assembly, source diff hygiene, and the expanded static smoke suite. The smoke suite includes explicit preview identity, stale detach, projection reset, terminal release, generation-safety, and repeated lifecycle guards.

Physical Android-device testing is mandatory for the directive’s final graphics claims. The sandbox has no connected device, so 20+ capture cycles, 50+ preview recreation cycles, camera/audio stress, network switching, Activity recreation, and long-running platform ingest were not executed on hardware and are not claimed here.
