# Unictoos v0.2.6 — Supportability and creator confidence

Unictoos v0.2.6 adds the first production supportability batch without changing the unresolved capture or encoder graphics path. The mobile-first UI remains intact, CredentialStore encryption is unchanged, and simultaneous multistreaming remains disabled until the single-destination pipeline passes physical long-duration validation.

## Added

- **Redacted diagnostic export** from Settings. The exported JSON includes app version, Android/device compatibility checks, selected stream profile, session status, configured-destination count, and bounded lifecycle events. Stream keys, tokens, passwords, authorization values, and RTMP/RTMPS URLs are redacted or omitted.
- **Structured session timeline** in Library. Recent lifecycle events such as pipeline creation, preparation, connection, reconnect, release, and errors are displayed locally from the bounded diagnostics buffer.
- **Device compatibility report** in Settings. The report describes Android support, camera capability, profile load, memory posture, and the staged direct-destination cap. It distinguishes ready, caution, and blocked conditions.
- **Clearer preflight outcomes** on Home. Network, microphone, camera, destination, and stream-profile checks now show actionable values and explanations rather than generic readiness labels.
- **Pure regression tests** covering compatibility scoring, high-load profile warnings, missing network/destination explanations, and support-bundle redaction.

## Safety boundary

This release does not claim to fix the graphics-resource exhaustion reproduced on the Infinix X6853. It does not add new camera, screen, EGL, MediaCodec, or multistream fan-out behavior. The current v0.2.5 single-slot RootEncoder MultiStream migration remains unchanged. The supportability features are intentionally safe to ship while the graphics lifecycle requires further physical-device evidence.

## Validation

The required validation gate for this feature batch is:

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug --no-daemon --max-workers=1 -Dorg.gradle.jvmargs='-Xmx1024m -Dfile.encoding=UTF-8'
python3 tools/feature_smoke_test.py
```

Generic Android emulator execution remains unavailable in the sandbox because software emulation cannot complete framework boot without KVM acceleration. Therefore this build is not physical-device verified.
