# Contributing to Unictoos

Thank you for contributing to Unictoos, a mobile-first Android live-streaming and recording studio. The project is an alpha engineering codebase, so reliability, privacy, and reproducible testing matter more than adding a large feature quickly.

## Development environment

Local builds require Android SDK API 35, Android build tools 35.0.0, JDK 21, and the Gradle wrapper included in the repository. Capture and encoder behavior requires a physical Android device running Android 10 or later; a successful JVM build cannot prove that MediaProjection, Camera2, AudioRecord, hardware encoding, or platform ingest work on a device.

Before opening a pull request, run the relevant checks from the repository root:

```bash
./gradlew test
./gradlew lintDebug
./gradlew assembleDebug
python3 tools/feature_smoke_test.py
```

For a full local validation pass, use:

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug assembleRelease
```

Instrumented tests require an emulator or physical Android device. When one is available, run the relevant connected test task, for example:

```bash
./gradlew connectedDebugAndroidTest
```

Do not treat the absence of a connected device as a passing instrumented test run. Report it explicitly in the pull request.

## Compose file organization

The Compose UI follows a one-file-per-screen convention. The activity and navigation host live in `app/src/main/java/com/unictoai/unictoos/ui/MainActivity.kt`. Screen-level composables belong in `ui/screens/`, using one file for each major screen: Home, Scenes, Studio, Engage, Library, More, and Settings. Shared composables used by two or more screens belong in `ui/components/`.

Screen composables should receive state through parameters and communicate user actions through lambda callbacks. Deeply nested composables should not reach directly into `StudioViewModel`. Keep the existing unidirectional flow: `StateFlow` is collected near the app shell, and mutations are sent through explicit callbacks. Preserve the mobile-first hierarchy and do not introduce desktop-style control grids or crowded horizontal action rows without a documented accessibility reason.

## Tests and review expectations

Add tests alongside new ViewModel, domain, repository, credential, and service-boundary logic. JVM tests should use injected fakes for Android-backed stores and should assert both final state and meaningful `StateFlow` emissions where applicable. Turbine is available for Flow assertions. Android Keystore behavior belongs in instrumented tests because it requires an Android runtime or emulator.

Every pull request should explain what was tested, which commands were run, whether an emulator or physical device was available, and which device-only behaviors remain unverified. Changes to capture, foreground services, RootEncoder setup, credential storage, exported components, network transport, or platform integrations require focused review for lifecycle, privacy, and failure behavior.

## Credentials and private data

Never place stream keys, OAuth access tokens, OAuth refresh tokens, private ingest URLs, personal channel identifiers, or real broadcast data in source files, test fixtures, screenshots, logs, issue comments, or commits. Use fake or disposable credentials for local validation, and redact secrets from all diagnostic artifacts. Do not add credential fields to scene exports or telemetry payloads.

The encoder must not receive platform OAuth tokens, chat payloads, moderation data, advertising content, or raw credentials. Platform features that require OAuth, webhooks, client secrets, moderation scopes, chat relays, or persistent event subscriptions must remain behind explicit provider adapters with a documented PKCE or secure-backend strategy. A generic RTMP stream key does not provide those capabilities.

## Media and battery discipline

The foreground service owns long-running capture, encoding, transport, recording, reconnect scheduling, notifications, and cleanup. UI work must not poll the encoder at a high frequency or create continuous idle animations that compete with capture and battery. Audio probes and other potentially blocking device operations must run off the service main-thread path. Any coroutine scope introduced in a service must be canceled during teardown.

Do not change RootEncoder source classes, `GenericStream`, or connection behavior as part of a UI-only change. If a media-pipeline change is necessary, isolate it in its own reviewable commit and include device-test evidence or a clearly documented limitation.

## Pull requests

Keep commits focused and reviewable. Separate structural refactors, test additions, media-threading changes, and user-facing feature work rather than combining unrelated modifications. Describe user-visible behavior changes, migration concerns, security implications, and rollback considerations. Update release notes or roadmap documentation when a change affects alpha scope or changes what is safe to claim as implemented.
