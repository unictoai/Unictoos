# Contributing to Unictoos

Thank you for helping build a serious mobile creator studio. Unictoos is a native Android project focused on reliable capture, composition, encoding, and streaming rather than a desktop UI clone.

## Before opening a change

Read the README, SECURITY.md, and THIRD_PARTY_NOTICES.md. Do not commit stream keys, OAuth credentials, private RTMP URLs, recordings containing personal data, or device logs containing sensitive information.

## Local checks

Run the following from the repository root:

```bash
./gradlew test
./gradlew assembleDebug
```

For media changes, test on a physical Android device. Emulator-only validation is not sufficient for screen capture, microphone routing, hardware encoders, thermal behavior, or long-duration streaming.

## Pull requests

Describe the user problem, affected screens or media subsystems, Android versions/devices tested, and any new permissions or third-party libraries. Include screenshots for UI changes and a short failure/recovery description for streaming changes.

Changes to the following areas require extra care: MediaProjection consent, foreground services, MediaCodec configuration, audio capture, RTMP/RTMPS transport, stream-key handling, exported components, and release signing.

## Project principles

Unictoos should remain mobile-first, transparent, privacy-conscious, and free for core streaming. Prefer small testable interfaces, capability detection over device assumptions, clear error recovery, and explicit documentation of platform limitations.
