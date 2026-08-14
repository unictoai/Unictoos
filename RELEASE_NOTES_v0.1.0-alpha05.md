## Unictoos v0.1.0-alpha05

This release begins the benchmark-driven Creator Core Plus expansion based on a review of Streamlabs Mobile, PRISM Live Studio, Larix Broadcaster, YouTube mobile, Twitch creator APIs, Kick mobile guidance, Restream Chat, and StreamElements.

### New creator workflow features

- Added a real camera-only capture mode with runtime camera permission handling.
- Added preflight checks for network presence, microphone permission, camera permission, and Android screen-capture consent expectations.
- Added a dedicated **Engage** workspace for future unified chat, alerts, moderation, emotes, clips, and platform account integrations.
- Added transparent integration cards that distinguish current stream-key support from future OAuth-backed creator tools.
- Added scene-source creation for screen, camera, image, text, and color layers.
- Preserved source visibility switches and portrait/landscape scene creation.
- Added benchmark and feature-roadmap documentation to the repository.

### Reliability and privacy

The existing foreground-service reliability work remains included: microphone readiness validation, bounded reconnects, authentication-specific errors, local MP4 recording, secure per-platform credentials, and no ad injection into streams or recordings. The security documentation now defines OAuth scope, chat-data, provider, and monetization boundaries.

### Important current limitation

A scene containing both screen and camera sources is currently a scene-model declaration, not yet a fully composited camera-picture-in-picture output. The current service selects screen capture when an enabled screen source exists; otherwise it uses camera-only capture. The next media-engine milestone is a tested GL compositor with draggable picture-in-picture layers.

### Build details

- Application ID: `com.unictoai.unictoos`
- Version name: `0.1.0-alpha05`
- Build type: debug alpha
- Minimum Android version: Android 10 / API 29
- Target SDK: 35

### Testing note

The APK passed the JVM test suite and debug assembly in the sandbox. Physical-device testing is still required for camera capture, microphone audio, screen capture, recording, reconnect behavior, and private/unlisted YouTube, Twitch, and Kick ingest.
