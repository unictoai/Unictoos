# Unictoos v0.4.1

## Unified professional hardening release

Unictoos v0.4.1 is the follow-up build from the unified feature pass. It keeps the v0.4 professional mobile broadcast workflow while closing two concrete paths that could make the Studio appear ready when the service could not start safely.

## Fixes and improvements

### Strict destination readiness

A saved destination is now considered configured only when it contains a non-empty key and a valid `rtmp://` or `rtmps://` server URL. Malformed URLs no longer appear as ready in Home, Studio, or the Go Live checks. Server-side preflight remains authoritative and continues to validate the full request before capture starts.

### Explicit capture-source validation

A scene with no enabled camera or screen source no longer falls through to screen capture. Studio now reports the missing capture source, and both broadcast and practice actions stop before requesting permissions or launching the foreground service.

### Operator workflow

The Go Live check continues to show destination, network, microphone, capture source, and quality status. High-load profiles remain visible as cautions. Stream keys remain masked by default in Settings. The session remains generation-owned, with RootEncoder 2.8.0, explicit terminal release, MediaProjection one-shot ownership, stale preview-token rejection, bounded reconnect, and silent-connection watchdog protection.

### Feature boundary

The product plan covers the full researched roadmap: scenes, local recording, health telemetry, thermal protection, adaptive bitrate, destination profiles, platform capability reporting, practice mode, and diagnostics. OAuth chat/moderation/events, simultaneous multi-destination fan-out, relay forwarding, UVC cameras, SRTLA/RIST bonding, cloud backup, and remote control require provider credentials, backend/relay infrastructure, native libraries, or physical-device certification. They are represented honestly rather than exposed as fake controls.

## Validation

The final source passes unit tests, Android lint, debug packaging, Android-test packaging, R8 release packaging, source-security audit, checksum verification, and the expanded static smoke suite. Physical acceptance still requires real Android tests on the Infinix X6853, including long-duration destination ingest, repeated preview/capture recreation, Fix/retry, background behavior, rotation, source switching, and network handoffs.

## Build identity

Version: `0.4.1`  
Android version code: `42`  
Compile SDK: `37`  
Target SDK: `36`  
RootEncoder: `2.8.0`
