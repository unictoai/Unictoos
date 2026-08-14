# Unictoos

**Unictoos** is a mobile-first, open-source Android live-streaming and recording studio for creators who want to broadcast from their phone to YouTube, Twitch, Kick, and compatible RTMP/RTMPS destinations.

The project is intentionally not a desktop OBS clone. It uses a touch-first workflow with a dashboard, scenes, a focused Studio screen, local recordings, and simple broadcast controls.

## Current status

This repository contains the first native Android milestone:

- Kotlin and Jetpack Compose application shell.
- Mobile creator UI with Home, Scenes, Studio, Recordings, and Settings areas.
- Scene and source domain models.
- Destination configuration for custom RTMP/RTMPS server URLs and stream keys.
- Android Keystore-backed local encryption for destination credentials.
- MediaProjection consent flow and foreground-service boundary.
- RootEncoder-backed screen capture, microphone capture, hardware encoding, recording/streaming service integration.
- Gradle Wrapper and JVM unit tests.

The current build is an **alpha engineering milestone**. The app can build and install, but real-device validation against YouTube, Twitch, and Kick is still required before it should be treated as production-stable. Do not use a live stream key in a development build until the device and destination workflow has been tested safely.

## Build locally

Requirements:

- Android SDK with API 35 and build tools 35.0.0.
- JDK 21.
- A physical Android device running Android 10 or later is strongly recommended for capture testing.

Build the debug APK:

```bash
./gradlew assembleDebug
```

Run JVM unit tests:

```bash
./gradlew test
```

The debug artifact is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device with Android Debug Bridge:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## First-use workflow

1. Open **Settings** and enter the destination server URL and stream key. Use the ingest URL and key provided by YouTube, Twitch, Kick, or another compatible service.
2. Save the destination. Credentials are stored locally through an Android Keystore-backed encryption key and are not part of scene exports.
3. Open **Scenes** and select a scene.
4. Open **Studio**, press **Go live**, approve Android’s screen-capture consent dialog, and monitor the live status.
5. Stop the broadcast from Studio. The foreground service is responsible for releasing capture and encoder resources.

## Platform setup

Unictoos initially uses the common custom RTMP/RTMPS workflow. This keeps platform credentials out of the core engine and lets creators use the app with multiple destinations without mandatory OAuth accounts.

The destination adapter and documentation work will be validated separately for:

- YouTube Live ingest settings.
- Twitch ingest settings and stream-key rotation.
- Kick ingest settings and stream-key rotation.

Platform OAuth, chat, scheduling, thumbnails, and metadata publishing are intentionally separate follow-up integrations. They must not block the reliable core broadcast path.

## Architecture

The application is organized around a Compose UI layer, a shared domain model, a ViewModel state layer, a Keystore-backed credential store, and a foreground media service. The service owns MediaProjection, microphone capture, hardware encoding, RootEncoder transport, and cleanup. The UI observes a process-local status bus for connection, bitrate, live, error, and disconnect states.

The project uses [RootEncoder](https://github.com/pedroSG94/RootEncoder) under its Apache-2.0 license for the initial open-source RTMP/RTMPS/media pipeline integration. See `THIRD_PARTY_NOTICES` for dependency attribution.

## Safety and privacy

Unictoos must never log stream keys, include them in exported scenes, or commit them to the repository. Use a disposable test destination when validating the app. Screen capture always requires Android user consent. The foreground service exists because Android requires long-running capture and streaming work to be visible to the user.

Before a public stable release, the project must complete a physical-device matrix, 30–120 minute reliability sessions, network interruption tests, thermal/battery tests, encoder capability checks, and YouTube/Twitch/Kick ingest validation.

## License

Unictoos is intended to be free and open-source. The final license will be committed after the dependency and codec audit confirms compatibility; the default project direction is Apache-2.0.
