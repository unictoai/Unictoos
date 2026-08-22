# Unictoos v0.4.10

## Focused live-streaming cleanup

Unictoos v0.4.10 removes inactive product surfaces and keeps the app focused on the capture-to-broadcast path. The release does not add new platform integrations or cosmetic feature work.

### Changes

- Removed the inactive Engage and More workspace surfaces from navigation.
- Removed placeholder engagement models, capability-matrix code, and disconnected platform-integration stubs that were not used by the active runtime.
- Removed the unused monetization policy, sponsor banner, and ad settings from the app.
- Removed unused multistream manager implementations and their tests; the active runtime remains the bounded RootEncoder `MultiStream` adapter with a two-destination cap.
- Removed stale tracked APK binaries from the source repository. Installable builds remain distributed through GitHub Releases.
- Made canceled Android screen-capture permission visibly report a retry message instead of silently returning.
- Cleared Activity-side capture metadata after handing an atomic request to the foreground service.
- Made the Studio action card display the service's current staged startup message during preparation.
- Updated repository documentation and smoke checks to describe the actual focused runtime.

### Core streaming path retained

The foreground service remains the owner of camera, MediaProjection, microphone, RootEncoder encoding, destination transport, notifications, reconnect handling, recording, and terminal cleanup. The v0.4.9 atomic capture/start handoff is retained: a single camera or screen request carries the start metadata and is queued before capture preparation completes.

### Validation

- JVM unit tests: passed.
- Debug lint: passed.
- Static smoke gate: 141/141 checks passed.
- Physical-device capture and platform-ingestion validation: not available in the build environment and therefore not claimed.

### Version

- Application ID: `com.unictoai.unictoos`
- Version name: `0.4.10`
- Version code: `51`
- RootEncoder: `2.8.0`

Use a disposable or rotatable test stream key when validating a development build. Before testing, uninstall the previous app or clear its app data, configure a destination in Settings, then open Go Live and approve the requested Android permissions.

## Artifact checksums

```text
445bb19a13f5326042da0a334b14e4af22a74155feb4b0e0a972b3b8e6691df8  Unictoos-v0.4.10-androidTest.apk
39241a84040568181f37a6461593cd9cc085667dcdbd32150c0bc26567bd8839  Unictoos-v0.4.10-debug.apk
17614dc9d1e3fe751c249a96ccaf95744b6561053b531c1e78c605a3fab06f54  Unictoos-v0.4.10-release-unsigned.apk
```
