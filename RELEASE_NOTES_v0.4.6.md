# Unictoos v0.4.6

Unictoos v0.4.6 is a repository-wide professional hardening release. It addresses defects found across the live-streaming lifecycle, capture handoff, preview ownership, destination validation, scene persistence, configuration serialization, notifications, launcher resources, and engineering quality gates.

## Resolved defects

### Streaming and capture lifecycle

- Terminal reconnect failures now stop the active stream, remove the foreground notification, and stop the service instead of leaving the UI in a permanently reconnecting state.
- Foreground-service startup now handles security, invalid-configuration, and illegal-state failures without allowing an uncaught Android lifecycle exception to crash the app.
- Camera preparation receives the scene payload directly. The selected scene orientation is therefore applied before RootEncoder preparation rather than relying on a race with the later start request.
- Capture preparation continues to use the serialized release boundary and now uses the scene-normalized quality profile for both camera and screen capture.
- Practice mode publishes `LIVE` only after the recording callback confirms that the local recording is active. A recording startup failure remains a recording error instead of a false live session.
- Pending practice state is cleared across stop, graphics recovery, reconnect exhaustion, and service destruction paths.

### Preview and readiness

- A preview surface that is still outstanding when Compose detaches now sends the service a terminal detach notification. The explicit listener-release API remains non-synthetic, so Android’s real `surfaceDestroyed` callback is not duplicated.
- Studio readiness now evaluates Internet capability rather than treating any non-null active network as usable.
- Studio preview dimensions and quality labels follow the selected 16:9 or 9:16 scene orientation.
- Strict URI validation rejects incomplete RTMP, RTMPS, and SRT destination values before adapter slot state is mutated.

### Destinations and multistream

- The bounded two-destination RootEncoder adapter validates and normalizes the entire endpoint list before changing active-slot state.
- Complete SRT listener URLs remain valid without a separate stream key; RTMP and RTMPS continue to require a key through the destination model.
- Fan-out callbacks remain aggregate and session-safe: all configured slots must connect before the service reports `LIVE`, while stale or partial slot state is cleared before a retry.

### Data integrity and performance

- High-frequency opacity edits use the existing debounced background scene persistence path instead of serializing and writing the complete scene graph synchronously on every slider event.
- Scene restoration and serialization are bounded by safe scene, group, source, identifier, name, text, and URI limits.
- Configuration export now escapes all JSON control characters, including backspace, form feed, and low ASCII values, so scene text cannot generate malformed JSON.
- The static security audit now distinguishes quoted credential literals from ordinary variable assignments and reports zero suspicious credential literals and zero direct log calls in the current source.

### Android polish

- The manifest now uses adaptive launcher resources with a vector fallback, and the foreground notification uses a dedicated monochrome status icon instead of full-color launcher art.
- README build instructions, SDK requirements, architecture claims, and release references now match v0.4.6 and its actual evidence boundary.
- The test-only `org.json` dependency is updated to the current repository-validated release identified by the Android lint gate.

## Validation

The v0.4.6 gate includes the repository smoke suite, security-source audit, JVM unit tests, debug lint, debug APK assembly, Android instrumentation APK assembly, minified release assembly, `git diff --check`, APK package/version inspection, and checksum verification.

Repository-side validation cannot prove successful ingest on every device, carrier, or platform. A physical-device matrix is still required for YouTube, Twitch, Kick, compatible SRT listeners, microphone capture, preview stability, camera switching, network interruption, thermal throttling, notification actions, recording finalization, and two-destination fan-out.
