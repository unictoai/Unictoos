# Unictoos v0.5.5

Unictoos v0.5.5 is a maintenance release that prepares the project for broader alpha testing while maintaining the existing feature set and reliability path. This version focuses on project hygiene, documentation consistency, and preparation for physical device validation.

## Changes from v0.5.2

- **Version bump**: Updated from v0.5.2 (versionCode 62) to v0.5.5 (versionCode 65)
- **Documentation consistency**: Ensured all version references are synchronized across README and release notes
- **Project preparation**: Cleaned up release artifacts and organized version history

## Feature boundary

This is a **no-backend** release. Unictoos continues to work entirely on-device with:
- Keystore-backed credential storage (per-platform stream keys)
- RootEncoder RTMP/RTMPS/SRT transport routing
- Local MP4 recording to app-private storage
- MediaProjection screen capture
- Camera capture with front/back switching
- Bounded two-output fan-out
- Bounded reconnect attempts
- Actionable notifications
- Live encoder telemetry
- Scene templates
- Safe scene-only configuration import

**No external services are required.** The app works with YouTube, Twitch, Kick, and custom RTMP/RTMPS/SRT endpoints using only the credentials you provide.

## Reliability boundary

This remains an **alpha engineering build**. It compiles and packages successfully, but requires physical-device validation for:
- YouTube ingest
- Twitch ingest
- Kick ingest
- Custom RTMP/RTMPS/SRT endpoints
- Screen capture on various OEMs
- Camera capture behavior
- PiP (Picture-in-Picture) functionality
- Android 15 OEM behavior
- 30-120 minute endurance sessions
- Network interruption recovery
- Thermal/battery behavior

Do not use an important broadcast or an irreplaceable stream key with an unvalidated development build.

## Validation

| Gate | Result |
|---|---|
| JVM unit tests | Passed (34 test files) |
| Android lint | Passed |
| Debug APK assembly | Passed |

## Package information

- **Package name**: `com.unictoai.unictoos`
- **Version name**: `0.5.5`
- **Version code**: `65`
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 36 (Android 16)

## Build commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run lint
./gradlew lint
```

## Installation

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install on a connected device with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## First-use workflow (unchanged from v0.5.2)

1. Open **Settings**
2. Choose YouTube, Twitch, Kick, or Custom transport
3. Enter the server URL and stream key from your platform
4. For SRT: Choose Custom transport, enter the complete `srt://` listener URL, leave Stream key blank
5. Open **Go Live**
6. Press **Go Live**
7. Approve Android's screen-capture consent dialog when using screen capture
8. Allow microphone/camera access when Android requests it

Each destination has its own encrypted credential slot. Switching platforms loads that platform's saved values instead of overwriting another platform's credentials.

## Known limitations

- PiP drag coordinates, rounded corners, border/shadow styling, and image overlays are not complete
- Independent per-destination health dashboards remain future work
- Remote chat providers, OAuth, scheduling, thumbnails, and moderation are not included
- Background streaming must be explicitly enabled and is accompanied by a foreground notification

## Safety and privacy

Unictoos must never log stream keys, include them in exported scenes, or commit them to the repository. Use a disposable or rotatable test destination when validating the app. Screen capture always requires Android user consent. The foreground service exists because long-running capture and streaming work must remain visible to the user.

## License

Unictoos is free and open-source under Apache-2.0. Third-party dependencies remain subject to their own licenses and notices.
