# Unictoos — Architecture Report

> Read-only deep read of the codebase at `~/workspace/projects/Unictoos`
> (git remote: `github.com/unictoai/Unictoos`). Generated 2026-09-26 as a
> working reference for taking over development. Covers the tree as of
> **v0.5.3 (versionCode 63)**.

## 0. TL;DR

- Single-module Android app (`:app`), Kotlin + Jetpack Compose, minSdk 29 /
  targetSdk 36 / compileSdk 37. RootEncoder **2.8.0** (via JitPack) owns
  capture → encode → RTMP/RTMPS/SRT transport. No backend, no cloud, no chat
  integrations — everything is on-device.
- The whole media pipeline lives in one 1975-line
  `streaming/StreamingForegroundService.kt`. Stateless `*Policy` objects hold
  the decision logic; the service orchestrates.
- Encoding is **one shared encoder fanned out to ≤2 destinations** via
  RootEncoder's `MultiStream` API (`SingleDestinationMultiStreamAdapter`).
- State: one God `StudioViewModel` (576 lines, 13 StateFlows) + a
  process-local `StreamingStatusBus` (the engine→UI bridge). Manual DI
  (constructor injection, no Hilt/Koin).
- Persistence: plain `SharedPreferences` per concern (no DataStore, no Room);
  one manual `SQLiteOpenHelper` for local analytics; credentials in
  SharedPreferences encrypted with a manual AES/GCM AndroidKeyStore key
  (`unictoos_stream_credentials`).
- UI: single activity, hand-rolled 5-tab navigation (no Navigation Compose),
  Material 3 "Black Hole" theme. PiP, adaptive bitrate, per-destination health,
  text overlays are all v0.5.0 additions — all **experimental / not
  device-validated** (no physical device, emulator, or ingest testing was ever
  available in the build environment; see §14).

---

## 1. Build & module facts

| Fact | Value |
|---|---|
| Modules | Single `:app` module (`settings.gradle.kts` → `include(":app")`) |
| Package / applicationId | `com.unictoai.unictoos` |
| Version | `0.5.3`, versionCode 63 (`VERSION` file + `app/build.gradle.kts`) |
| SDKs | minSdk 29, targetSdk 36, compileSdk 37; JDK 17 target |
| Streaming engine | `com.github.pedroSG94.RootEncoder:library:2.8.0` (JitPack) |
| UI | Compose BOM `2026.08.00`, Material 3, `navigation-compose:2.9.8` (dependency present but **unused** — see §9), lifecycle 2.11.0 |
| Media post-processing | Media3 `transformer`/`effect`/`common` 1.11.0 (recording trim/export) |
| Persistence libs | None — no DataStore, no Room (see §8) |
| Tests | 34 JVM unit tests (`app/src/test`), 3 instrumented (`app/src/androidTest`); JUnit4 + Turbine + kotlinx-coroutines-test |
| Release build | R8 minify+shrink; release APK unsigned (no signing key in repo) |
| Kotlin files | 113 main-source files, ~13k LOC |

`app/build.gradle.kts` plugins: `com.android.application`,
`org.jetbrains.kotlin.plugin.compose`. Note: v0.5.3 release notes mention an
**aborted RootEncoder 2.10.0 upgrade experiment** — 2.8.0 was kept.

---

## 2. Package / file layout

```
com.unictoai.unictoos/
├── UnictoosApplication.kt          # 53 lines: global uncaught-exception handler only
├── StudioViewModel.kt              # 576 lines: THE screen-state holder
├── StudioViewModelFactory.kt       # 26 lines: manual ViewModelProvider.Factory
├── data/            # 14 files: SharedPreferences stores + export/import + Media3 editor
│   ├── SceneStore, CredentialStore, StreamQualityStore, AudioSettingsStore,
│   │   AutoStopStore, LatencyModeStore, ThermalProtectionStore,
│   │   AdaptiveBitrateStore, MultistreamSelectionStore
│   ├── CreatorHistoryStore, LocalAnalyticsStore (SQLite), ConfigExporter,
│   │   ConfigImporter, Media3RecordingEditor
├── domain/          # 14 files: pure models (no Android deps mostly)
│   ├── StudioModels (Scene, SceneSource, StreamDestination…),
│   │   ScenePresentation, SceneGeometryPolicy, PipConfig (+PipGeometryPolicy),
│   │   StreamQuality, LatencyMode, AudioSettings, AutoStopDuration,
│   │   MultistreamModels, PlatformCapabilities, RecordingState,
│   │   StreamUsageEstimate, CreatorHistoryModels
├── health/          # DestinationHealth, DestinationSlotEvent
├── integrations/    # ExternalFeatureContracts.kt — unimplemented boundary interfaces
├── overlay/         # StreamOverlay.kt (model), OverlayRenderer.kt (GL filters)
├── stream/          # CompositorSurface.kt (PiP), QualityTier.kt (display ladder)
├── streaming/       # 27 files: THE engine — service + policies + state machines
├── ui/
│   ├── MainActivity.kt, UnictoosApp.kt (nav shell), OnboardingScreen.kt,
│   │   PreviewSurfaceView.kt
│   ├── screens/     # Home, Scenes, Studio, Library, Settings (+Onboarding)
│   ├── components/  # SharedComponents.kt
│   └── theme/       # Theme.kt (tokens + Material3 schemes)
```

Cross-cutting rule the code follows: anything that *can* be a pure decision is
a stateless `*Policy` object in `streaming/`; anything stateful about the live
pipeline lives in `StreamingForegroundService`.

---

## 3. Streaming pipeline end-to-end

### 3.1 Ownership & threading

`streaming/StreamingForegroundService.kt` (1975 lines) owns the entire
pipeline: it creates and holds the `SingleDestinationMultiStreamAdapter`
(field name `genericStream`), owns `MediaProjection`, camera/mic sources,
reconnect scheduling, recording, wake lock, and notifications, and publishes
snapshots to `StreamingStatusBus`.

Threading model (verified in source):
- A main-thread `Handler` serializes **all** state changes (`postSerialized`).
  RootEncoder `ConnectChecker` callbacks are re-wrapped in
  `GenerationConnectChecker` and posted to it. Projection callbacks, network
  callbacks, GL render-error callbacks, reconnect watchdog, 30 s capture
  timeout, and the 1 s elapsed ticker all run here.
- `serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`
  for `ACTION_PREPARE_*`, STOP, RELEASE_CAPTURE; a kotlinx `Mutex`
  (`capturePreparationMutex`) prevents prepare/stop races.
- `Dispatchers.IO` is used only for a mic-availability check and
  `RecordingValidator.validateWhenStable` (a blocking `Thread.sleep` loop,
  8×150 ms).
- `sessionGeneration: AtomicLong` is the stale-callback fence: every async
  callback is stamped, and callbacks from a released pipeline generation are
  dropped. `AtomicBoolean`s guard `closed` / `graphicsFailureRequested`;
  `synchronized(trackerLock)` guards slot bookkeeping in the adapter.

### 3.2 Capture sources

No dedicated capture classes — the service owns sources directly
(`stream/CompositorSurface.kt` is the only capture-adjacent class, for PiP).

- **Camera:** `prepareCamera()` builds RootEncoder `Camera2Source(context)` and
  hot-swaps it via `genericStream.changeVideoSource(cameraSource)`. Audio is
  always `MicrophoneSource` via `changeAudioSource`. `ACTION_SWITCH_CAMERA`
  flips front/back (including the PiP secondary camera).
- **Screen:** `prepareProjectionFromIntent()` →
  `prepareProjection(resultCode, data, pipConfig)` builds an Android
  `MediaProjection` from the activity's consent intent and injects RootEncoder
  `ScreenSource(context, projection)`. A `MediaProjection.Callback` on the main
  handler converts an OS-initiated `onStop()` into an ERROR publish +
  `stopStreaming`, unless `manualStop` / `intentionallyReleasingProjection`.
- **Capture-mode selection:** `streaming/CaptureModePolicy.forScene(scene)` —
  `"screen"` if PiP enabled and both screen+camera sources are on; else
  `"camera"` if any camera source is on (mixed scenes are camera-first, which
  protects legacy saved scenes); else `"screen"`; else `"none"`. The service
  receives `ACTION_PREPARE_PROJECTION` or `ACTION_PREPARE_CAMERA` accordingly.
- **Capture-start handshake:** `streaming/CaptureStartPolicy` gates queueing
  (`canQueue` requires a state that `acceptsQueuedStart`), staleness by
  generation (`STALE_REQUEST`), and readiness (`WAIT_FOR_CAPTURE` → `START_NOW`
  when `prepared && captureReady`). A 30 s `captureTimeout` fails the request
  with ERROR if capture (or the Studio preview surface) never arrives.
- **Background audio (v0.5.0):** `streaming/BackgroundAudioPolicy` — camera-only
  scenes may keep the broadcast audio path alive when the screen turns off;
  video is muted through RootEncoder and restored on user-present. A
  `PARTIAL_WAKE_LOCK` (`"Unictoos:Broadcast"`) is held for active
  broadcast/practice sessions and released on every terminal path. Screen
  capture is excluded. Receiver registration uses Android 13+
  `RECEIVER_NOT_EXPORTED`.

### 3.3 Encoder (RootEncoder)

Not `RtmpCamera1/2` or `SrtCamera`. The adapter instantiates
**`com.pedro.library.multiple.MultiStream`** with `rtmpCheckers` /
`srtCheckers` (two `SlotConnectChecker` arrays) and placeholder
`NoVideoSource()`/`NoAudioSource()`, then hot-swaps real sources. MultiStream
= one shared GL pipeline + encoder, fanning encoded packets to transport slots.

- `prepareVideo(width, height, bitrate, rotation=0)` /
  `prepareAudio(sampleRate, stereo=false, bitrate, echoCanceler,
  noiseSuppressor)` are called in `prepareGenericStream()` from
  `StreamQualityStore`'s `StreamQuality` profile and `AudioSettingsStore`'s
  `AudioSettings`.
- Recording is RootEncoder's **transport-level recorder**
  (`startRecord(path, RecordTracks.ALL, listener)`) — no separate recording
  encoder, so MP4 quality = live profile. Recordings land in app-private
  `<filesDir>/recordings/`.
- `setVideoBitrateOnFly()` is the mechanism for both adaptive bitrate and
  thermal throttling.
- GL: `getGlInterface().setForceRender(false)` (comment notes ForceRenderer
  double-renders on slow EGL drivers); `setRenderErrorCallback` feeds the GL-OOM
  safety net; `setFpsListener` feeds FPS telemetry.
- **Bitrate ladder** (`stream/QualityTier.kt`, display/telemetry only): 1080p60
  / 8 Mbps, 1080p30 / 6 Mbps, 720p60 / 6 Mbps, 720p30 / 4.5 Mbps, 480p30 /
  2 Mbps, 360p30 / 1.2 Mbps. `QualityTier.toQualityTier()` maps
  `StreamQualityPreset`; the service's `qualityTierForBitrate` renders the
  nearest-by-bitrate label into `StreamSessionState.qualityTier`. The encoder
  itself is configured from the `StreamQuality` store profile, not from
  `QualityTier`.

### 3.4 Transport: RTMP vs SRT

- `streaming/StreamEndpointPolicy.transport()` /
  `SingleDestinationMultiStreamAdapter.transportFor()`: `srt://` → SRT path;
  `rtmp://` / `rtmps://` → RTMP path; anything else rejected. `isSupported()`
  also requires a valid URI host and no whitespace. Multiple endpoints are
  joined with `\u001F` in intents and deduped/capped by `decodeEndpoints()`.
- Stream keys travel as part of the URL string. Keys are **never** stored in
  `StreamingDiagnostics` — a regex redacts `stream_key` / `token` / `secret` /
  `rtmp(s)://…` values.
- `genericStream.startStream(endpoints)` starts each endpoint on its
  per-protocol slot (`multiStream.startStream(transport, index, endpoint)`).

### 3.5 Multi-destination fan-out

`streaming/SingleDestinationMultiStreamAdapter.kt` wraps MultiStream and caps it
at **`MAX_DESTINATIONS = 2`** ("bounded two-output fan-out"); the service
independently enforces `MAX_DIRECT_DESTINATIONS = 2`
(`domain/MultistreamModels.kt: MultistreamDefaults.DIRECT_DESTINATION_CAP = 2`,
with a feature-gated 3-destination cap keyed to
`THREE_DESTINATION_DEVICE_GATE = "Infinix X6853"`).

Aggregation rules (important for future work):
- Per-slot `SlotConnectChecker` emits `health/DestinationSlotEvent(index,
  HealthState)` → service updates `DestinationHealth`.
- The *aggregate* delegate callbacks (which drive `StreamStatus` LIVE /
  RECONNECTING) are forwarded **only when all active slots agree**
  (`successfulSlots.containsAll(activeSlots)` for success,
  `authenticatedSlots` for auth). First-slot-wins latches
  (`failureReported` / `disconnectReported`) report failure/disconnect once.
- `onNewBitrate` deliberately uses **slot 0 only** so the second destination
  can't double the adaptive-bitrate computation.
- `streaming/MultistreamStateReducer.aggregate()` turns per-destination states
  into IDLE / STOPPING / STOPPED / STARTING / LIVE / **DEGRADED** / FAILED.
- Release notes are explicit: this is per-destination *observation* over one
  shared encoder. Independent slot stop/retry controls do **not** exist.

### 3.6 Reconnect logic

`streaming/StreamFailurePolicy.kt`:
- `classify(reason)` is keyword-based. Non-retryable → `terminateStreamingFailure`:
  AUTH (`auth`/`key`/`credential`/`unauthorized`), CONFIGURATION
  (`unsupported`/`invalid url`/`malformed`/`protocol`), SERVER_REJECTION,
  ENCODER (`encoder`/`codec`/`media format`). Retryable: TIMEOUT, NETWORK
  (`socket`/`connect`/`disconnect`/`broken pipe`).
- `reconnectDelayMs(attempt, jitter)`: `2 s × 2^(attempt-1)`, capped at 30 s,
  clamped to [1 s, 35 s]; jitter ≤ 500 ms.
- **`MAX_RECONNECT_ATTEMPTS = 3`** (`StreamingForegroundService.kt:1967`);
  exceeding it gives up with "Reconnect limit reached".
- 45 s connection watchdog (`StreamStartupPolicy.CONNECTION_TIMEOUT_MS`,
  `shouldTimeout`) kills CONNECTING stalls; `onAuthError` hard-stops
  immediately and never retries (UI tells the user to rotate the stream key).

### 3.7 State machines

Three state machines exist (all pure `canTransition` objects; illegal
transitions are silently dropped by `publish()`):

**`streaming/StreamStateMachine.kt`** over `StreamStatus`
(IDLE, PREPARING, CONNECTING, LIVE, RECONNECTING, STOPPING, STOPPED, ERROR):
- IDLE → PREPARING, STOPPED, ERROR
- PREPARING → PREPARING, IDLE, CONNECTING, LIVE, RECONNECTING, STOPPING, STOPPED, ERROR
- CONNECTING → CONNECTING, LIVE, RECONNECTING, STOPPING, STOPPED, ERROR
- LIVE → LIVE, RECONNECTING, STOPPING, STOPPED, ERROR
- RECONNECTING → RECONNECTING, CONNECTING, LIVE, STOPPING, STOPPED, ERROR
- STOPPING → STOPPING, STOPPED, IDLE, ERROR
- STOPPED → STOPPED, IDLE, PREPARING, ERROR
- ERROR → ERROR, IDLE, PREPARING, STOPPED
- `acceptsStart` = IDLE/STOPPED/ERROR; `acceptsQueuedStart` adds PREPARING
  (queued Go-Live); `acceptsStop` = anything except IDLE/STOPPED.
- Triggers: `ACTION_PREPARE_*` → PREPARING; pending request ready → CONNECTING
  (practice → LIVE via recording); `GenerationConnectChecker.onConnectionSuccess`
  → LIVE; failure → RECONNECTING via `scheduleReconnect`; `ACTION_STOP` →
  STOPPING → `completeStop` → STOPPED; preflight/capture/auth/GL failures →
  ERROR.

**`streaming/StreamingStateContracts.kt`** also holds:
- `DestinationStateMachine` over `domain/DestinationState`
  (DISABLED → CONFIGURING → CONNECTING → LIVE → RECONNECTING → FAILED →
  STOPPING → STOPPED), plus `acceptsRetry` (FAILED/RECONNECTING).
- `RecordingStateMachine` over `domain/RecordingState`
  (IDLE → STARTING → RECORDING/STOPPING/FAILED …).
- `StreamingError` sealed interface: PermissionDenied, ProjectionUnavailable,
  EncoderUnavailable, UnsupportedConfiguration, NetworkUnavailable,
  AuthenticationFailed, DestinationRejected, ThermalLimit, StorageUnavailable,
  Unknown — each with `userMessage`, `developerMessage`, `retryable`,
  `destinationId`. This is the structured-error contract between engine and UI.

### 3.8 Preflight (before Go Live)

- `streaming/StreamPreflight.kt` (service-side, right before `startStream` /
  `startPractice`): `validateEndpoint` (blank/unsupported → Blocked; practice
  mode skips), `validateProfile` (≥320×240, 15–60 FPS, 250 kbps–12 Mbps,
  ≤8.29 M pixels), `validateEnvironment` (network required; storage ≥ estimate
  when recording; battery <10 % && not charging blocks;
  `PowerManager.THERMAL_STATUS_CRITICAL` blocks).
- `streaming/GoLiveReadinessPolicy.kt` (UI-side fast model): 5 checks
  (destination, network, microphone, capture, quality); `canStart` requires all
  blocking checks; 60 FPS / ≥1080p is caution-only.
  `PreflightOutcome` / `PreflightOutcomeEvaluator` is the tri-state
  (READY / ACTION_REQUIRED / CAUTION) report shown in Studio.

### 3.9 Status bus & telemetry

- `streaming/StreamingStatusBus.kt`: `object` singleton —
  `state: StateFlow<StreamSessionState>` (service writes),
  `healthHistory: StateFlow<List<StreamHealthSample>>` capped at 120 via
  `recordHealth()`. Process-local only.
- `streaming/StreamTelemetryPolicy.kt`: `shouldExposeFps` — FPS exposed only
  when LIVE and (encoder active || PRACTICE || recording), else forced to 0.
- A 1 s `elapsedTicker` publishes `elapsedSeconds`, records a
  `StreamHealthSample` (bitrate, fps, battery %, thermal status, network label
  Wi-Fi/Cellular/Ethernet), drives auto-stop (`AutoStopStore`), updates the
  notification. Up to 1200 session samples are persisted to
  `CreatorHistoryStore` / `LocalAnalyticsStore` on `completeStop`.
- `streaming/StreamingDiagnostics.kt`: bounded **200-event** redacted lifecycle
  log, surfaced in Library + support exports.
- `streaming/SupportabilityExport.kt` + `DeviceCompatibilityReport.kt`:
  redacted support bundle (device checks + selected profile + session status +
  bounded diagnostics; credentials redacted).

### 3.10 Foreground service & notifications

- Channel `unictoos-broadcasting` ("Broadcasting", `IMPORTANCE_LOW`),
  notification id 4101. Actions: **Mute/Unmute**, **Start/Stop recording**,
  **Stop** (all `PendingIntent.getService` back to the service); tap opens
  `MainActivity`.
- `ServiceCompat.startForeground` with types **MICROPHONE | MEDIA_PROJECTION |
  CAMERA** (camera type only for camera capture; pre-R uses 0 or
  MEDIA_PROJECTION only). Manifest declares
  `foregroundServiceType="mediaProjection|microphone|camera"`; permissions
  `FOREGROUND_SERVICE{,_MICROPHONE,_CAMERA,_MEDIA_PROJECTION}` are declared.
- `START_STICKY`, but **null-intent restarts return without resurrecting
  capture** (v0.5.0 hardening), and `onTaskRemoved` is **not** overridden.
  `onDestroy` cancels the scope, releases the pipeline via
  `streaming/PipelineReleasePolicy.kt`, stops preview/recording, releases the
  projection, unregisters receivers/network callback, releases the wake lock.
- Graphics-failure safety net: `UnictoosApplication` installs a global
  uncaught-exception handler; `streaming/EncoderCrashPolicy.isRecoverableGraphicsFailure`
  matches only the confirmed `GL error: 1285` / `GL_OUT_OF_MEMORY` signature in
  `com.pedro.encoder` frames → routes to `ACTION_ENCODER_GRAPHICS_FAILURE`
  (release EGL/preview, rebuild pipeline); everything else chains to the prior
  handler. Background: `GL_FAILURE_AUDIT.md` documents that RootEncoder ≤2.4.5
  tore down EGL asynchronously and the old `SystemClock.sleep(150)` settle
  delay was unreliable; upgrading to 2.8.0 (whose release notes include an EGL
  lifecycle fix) was the chosen remediation.

### 3.11 Adaptive bitrate & thermal protection

- `streaming/AdaptiveBitratePolicy.kt` (default **on**): step **down** after
  15 s below 80 % of target; step **up** after 60 s at/above 95 % of target.
  Changes apply to the live encoded stream (`setVideoBitrateOnFly`) — no
  capture/encoder restart. Settings toggle + quality-tier badge in UI.
- `streaming/ThermalProtectionPolicy.kt`: ticker calls `shouldThrottle`
  (moderate ≥ 2, 10 s debounce); on throttle,
  `setVideoBitrateOnFly(bitrate × 0.75)` with a 1 Mbps floor, once until
  thermal recovery resets.

---

## 4. Scene model & compositor

### 4.1 Scene JSON schema

Shared by `data/SceneStore.kt`, `data/ConfigExporter.kt`,
`data/ConfigImporter.kt`, and `streaming/ScenePayloadCodec.kt` (minor
variations):

```
scene: { id, name, aspectRatio, backgroundAudioMode,
         pipConfig { enabled, position, size, cornerRadiusDp, borderWidthDp, dropShadow },
         transitionMode, transitionDurationMs,
         sourceGroups [ { id, name, enabled, sourceIds[] } ],
         sources [ { id, name, type, enabled, zIndex, opacity,
                     textContent, textColor, textSizeSp,
                     x, y, width, height,        // normalized floats in [0,1]
                     fillColor, imageUri, groupId? } ] }
```

- `domain/SceneGeometryPolicy.clamp` coerces geometry (min size 0.05).
- Source types include camera/screen/text/image/color (`SourceType`); scene
  templates: `"portrait-camera"`, `"gameplay"`, `"talk-show"`
  (`StudioViewModel.addSceneTemplate`).
- `ScenePayloadCodec` stamps `schemaVersion: 2` but **`decode` ignores the
  version** (no branching) — tolerated today because decode only reads known
  fields; a future v3 is not actually handled.
- `SceneStore` has no schema version field at all (key `scenes_json_v1` *is*
  the version); it relies on coercion/defaults instead of migrations. Corrupt
  JSON is moved to `scenes_corrupt_backup_v1` (≤512 KB) and defaults restored.
- **"Safe scene-only configuration import"**
  (`data/ConfigImporter.importScenes`): gated on exact
  `"schema": "unictoos-config-v1"`, 512 KB cap, ≤64 scenes, scenes with >32
  sources skipped, numerics/geometry coerced, unknown enums fall back
  (`AspectRatio.PORTRAIT`, `SceneTransitionMode.CUT`, `SourceType.COLOR`).
  Destination credentials are **structurally excluded** — exporter writes
  `"streamKey": null` and only `serverUrl` + `isConfigured` metadata; importer
  parses only the `scenes` array ("Destination credentials are intentionally
  ignored.").

### 4.2 Composition plan

`streaming/SceneCompositionPlan.from(scene)` counts text overlays, image
layers, screen/camera presence, and *unsupported* layers (IMAGE + COLOR +
screen+camera-without-PiP). `applySceneOverlays()` renders **only TEXT
sources** through `overlay/OverlayRenderer` as GL filters; everything else is
reported as uncomposited. `stream/CompositorSurface.kt` describes the actual
compositor (below). The Scenes UI itself warns that crossfades are "disabled
until a compositor is available".

### 4.3 PiP (experimental)

`stream/CompositorSurface.kt`: screen is the primary `ScreenSource`; a second
RootEncoder `Camera2Source` is attached to a `SurfaceFilterRender` added to the
GL interface, scaled/positioned by
`domain/PipGeometryPolicy.rect(config, aspect)` from `domain/PipConfig`
(positions BOTTOM_RIGHT etc., sizes SMALL/MEDIUM/LARGE, corner radius, border,
drop shadow persisted per scene). Startup is best-effort: `start()` returns
false and the stream continues screen-only if the secondary camera can't init
(diagnostic `pip_fallback_screen_only`); `releaseSecondaryCamera()` tears down
on init failure. The service calls it **only after the encoder is streaming**
(`startExperimentalPipIfRequested`, `StreamingForegroundService.kt:673`).

History: `docs/PIP_COMPOSITOR_PLAN.md` records that RootEncoder 2.4.5 exposed
no concurrent screen+camera composition, and proposed a custom two-texture EGL
compositor (never built). v0.5.0 instead used the already-present
`SurfaceFilterRender` path. Per release notes: no dragging, no rounded-corner
guarantee, no custom EGL compositor — treat as experimental until proven on
the target device.

### 4.4 Overlays

- `overlay/StreamOverlay.kt`: sealed interface — `TextOverlay`, `ImageOverlay`,
  `TimerOverlay` (uptime countdown), `ChatOverlay` (last-5 chat lines);
  `safe()` sanitizes/clamps all fields.
- `overlay/OverlayRenderer.kt`: converts overlays into RootEncoder GL filters
  (`TextObjectFilterRender`), max 24 overlays, `glInterface.clearFilters()`
  then set/add. Timer formatted from uptime; chat joined as `author: text`.
- **Overlays are baked into the encoded stream**, not a Compose layer:
  `StreamingForegroundService` (~lines 975–1001) builds `TextOverlay`s from the
  scene's TEXT sources and calls
  `OverlayRenderer(glInterface = genericStream.getGlInterface(), …).render(overlays,
  uptimeSeconds)`. When a scene has no text overlays, the GL filter graph is
  left untouched (v0.5.1 crash fix — mutating it at startup caused the Go Live
  crash). `ImageOverlay` is currently **skipped** (caller-owned texture
  creation not implemented) — image/branding scene sources don't render.

---

## 5. Destination management & credential storage

### 5.1 Destinations

- Four platform slots: YouTube, Twitch, Kick, Custom
  (`domain/MultistreamModels.kt: DestinationId`). Each has its own credential
  slot in `data/CredentialStore.kt` — switching platforms loads that
  platform's saved values instead of overwriting.
- `domain/MultistreamModels.kt` also holds `DestinationState`,
  `AggregateStreamState`, `DestinationProfile` (per-platform protocol
  caps/bitrate ceilings — e.g. Kick 1080p60/8 Mbps), `DestinationSession`
  (explicitly "contains no endpoint or plaintext credential"; `credentialRef`
  is opaque). Labeled **"Stage A" contracts — not yet consumed by a session
  manager** ("contracts only until a future manager consumes them").
- `data/MultistreamSelectionStore.kt` persists only the *selection*:
  `Set<PlatformPreset>` as a string set (`selected_platforms_v1`, default
  `{YOUTUBE}`); malformed entries dropped. `StudioViewModel`
  enforces the ≤2 cap in `setMultistreamPlatformEnabled` (returns `false` when
  the cap is hit) and truncates on load (`.take(2)`, silently).
- `StreamEndpointPolicy.isSupported` + `DestinationConfig.isConfigured`
  (non-blank `serverUrl`, valid endpoint, `srt://` URL or non-blank key) gate
  Go Live.

### 5.2 CredentialStore (read carefully before touching)

`data/CredentialStore.kt` (194 lines). **Manual AES/GCM in a regular
SharedPreferences file** — not EncryptedSharedPreferences, not DataStore.

- File `unictoos_secure_credentials`; per-platform keys
  `<platform>_server_url` / `<platform>_stream_key` hold Base64 of
  `[4-byte IV length | IV | ciphertext]`.
- Key alias `unictoos_stream_credentials` in **AndroidKeyStore**,
  `AES/GCM/NoPadding`, 128-bit tag, randomized IV per encryption
  (`setRandomizedEncryptionRequired(true)`), no user authentication required.
  IV size validated 12–32 bytes on decrypt; decryption failure returns `null`
  → treated as empty (fail-open to blank).
- **Key invalidation is unhandled:** no handling of
  `KeyPermanentlyInvalidatedException` / `KeyStoreException` beyond
  `runCatching`-and-swallow. `ensureKey()` only creates the key if the alias is
  missing. If the keystore key is invalidated, `getKey()` throws; `encrypt`
  calls it *before* its try block. `init { ensureKey();
  migrateLegacyYoutubeCredentials() }` runs on construction — if the keystore
  is wedged, `StudioViewModel` falls back to `UnavailableCredentialRepository`
  (no-op) via `safeCredentialRepository`. Net effect: **credentials silently
  stop working rather than erroring loudly; key loss is unrecovered** (no
  re-key/backup strategy).
- Legacy migration: `migrateLegacyYoutubeCredentials()` handles pre-platform
  fields `encrypted_server_url` / `encrypted_stream_key` — decrypt-with-current
  first, then accepts plaintext only if unambiguous (`rtmp(s)://` prefix for
  server; 4–512 chars of `[A-Za-z0-9-_.]` for key). New-format values win
  per-field. Empty values are stored **unencrypted** as `""`.
- Backup posture: manifest sets `allowBackup=false` and
  `res/xml/backup_rules.xml` + `data_extraction_rules.xml` exclude
  sharedprefs/files/databases from cloud backup and device transfer
  (`disableIfNoEncryptionCapabilities=true`) — consistent with Keystore keys
  not surviving restore. A restored install starts clean.

---

## 6. State management & DI

### 6.1 DI: manual, no framework

No Hilt/Koin/Dagger/ServiceLocator. `StudioViewModelFactory` (26 lines) is a
plain `ViewModelProvider.Factory` that extracts the `Application` from
`CreationExtras` and calls `StudioViewModel(application)`. All repositories
are constructed via the VM's **default constructor parameters** against
repository *interfaces* (`CredentialRepository`, `SceneRepository`,
`StreamQualityRepository`, …) with `@JvmOverloads` so JVM tests can pass fakes
(`StudioViewModelBehaviorTest` does this). `safe*` helpers
(`safeCredentialRepository`, `safeMultistreamSelectionRepository`,
`safeAdaptiveBitrateRepository`) wrap construction in `runCatching` with
in-memory "Unavailable" fallbacks (e.g. `UnavailableCredentialRepository`
no-ops, `UnavailableMultistreamSelectionRepository` returns `{YOUTUBE}`).
`UnictoosApplication` owns **no singletons** — only the uncaught-exception
handler (§3.10). One shared VM instance is created in `ui/UnictoosApp.kt`
(`viewModel(factory = studioViewModelFactory)`) and passed through the whole
nav host.

### 6.2 StudioViewModel (576 lines)

The single screen-state holder — a **God ViewModel** covering scenes,
destinations/credentials, stream quality, thermal protection, adaptive
bitrate, audio settings, auto-stop, latency mode, multistream selection,
session mirroring, config export/import.

- **13 public StateFlows** (all `MutableStateFlow` + `asStateFlow()`):
  `scenes`, `destinations`, `session`, `streamQuality`,
  `thermalProtectionEnabled`, `adaptiveBitrateEnabled`, `audioSettings`,
  `autoStopDuration`, `destination: StateFlow<DestinationConfig>`,
  `activePlatform`, `multistreamPlatforms`, plus
  `healthHistory = StreamingStatusBus.healthHistory` (direct delegation).
- `session` is populated by collecting `StreamingStatusBus.state` in `init`
  — the bus is the single source of truth; the VM mirrors it.
- **No `Channel`/`SharedFlow` for one-shot events** in the VM (no
  snackbar/navigation event channel). The only `SharedFlow` in the app is
  `Media3RecordingEditor._states` (`extraBufferCapacity = 8`) emitting
  `LocalRecordingEditState.{Started,Completed,Failed}`, collected by
  `LibraryScreen`.
- Write paths update the flow *and* persist immediately
  (`_scenes.update { … .also(sceneStore::save) }`). High-frequency writes
  (`updateTextSource`, `setSourceGeometry`, `setPipConfig`, `setSourceOpacity`)
  use a debounced `scenePersistenceJob` (cancel previous, `delay(350L)` on
  `Dispatchers.IO`, then save).
- **Dual session truth (tech debt, verified at lines 558–575):**
  `startPreparing()`, `enterLive()`, `stopStream()` write directly to `_session`
  — `enterLive()` hardcodes `bitrateKbps = 4500, fps = 30` — while `init`
  *also* collects `StreamingStatusBus.state` into `_session`. Last-writer-wins
  race with the real pipeline; these look like stub/test scaffolding left in
  production. They bypass `StreamingForegroundService` entirely.
- `broadcastEndpoints()` caps multistream at 2 endpoints.

### 6.3 Domain models

`domain/` is pure Kotlin data + policy helpers: `Scene`/`SceneSource`
(`StudioModels.kt`), `ScenePresentation`, `SceneGeometryPolicy`,
`PipConfig`/`PipGeometryPolicy`, `StreamQuality` (+ presets),
`StreamUsageEstimate` (bitrate→data estimate for preflight storage check),
`LatencyMode`, `AudioSettings`, `AutoStopDuration`, `RecordingState`,
`CreatorHistoryModels`, `MultistreamModels`, `PlatformCapabilities`
(static per-platform capability catalog: `STREAM_KEY_READY` for
YouTube/Twitch/Kick, `MANUAL_CONFIGURATION` for Custom; `REQUIRES_BACKEND`
for oauth/chat/moderation/events everywhere — no runtime device probing here).

---

## 7. Persistence layer

| Store | File | Keys | Notes |
|---|---|---|---|
| `SceneStore` | `unictoos_scenes` | `scenes_json_v1` (one JSONArray) | corrupt → `scenes_corrupt_backup_v1` (≤512 KB), defaults restored |
| `CredentialStore` | `unictoos_secure_credentials` | `<platform>_server_url`, `<platform>_stream_key` | AES/GCM + Keystore (see §5.2) |
| `StreamQualityStore` | `unictoos_stream_quality` | `preset_v1, width_v1, height_v1, fps_v1, bitrate_v1, keyframe_interval_v1` | `.validated()` on load/save; default BALANCED |
| `AudioSettingsStore` | `unictoos_audio_settings` | `quality_v1, sample_rate_v1, echo_canceler_v1, noise_suppressor_v1` | default STANDARD/44.1 kHz/EC+NS on |
| `AutoStopStore` | `unictoos_auto_stop` | `duration_v1` | default OFF |
| `LatencyModeStore` | `unictoos_latency` | `mode_v1` | default STABLE |
| `ThermalProtectionStore` | `unictoos_thermal` | `automatic_protection_enabled_v1` | default true |
| `AdaptiveBitrateStore` | `unictoos_adaptive_bitrate` | `enabled_v1` | default true |
| `MultistreamSelectionStore` | `unictoos_multistream` | `selected_platforms_v1` (StringSet) | default {YOUTUBE} |
| `CreatorHistoryStore` | `unictoos_creator_history` | `sessions_v1, markers_v1, health_samples_v1` (JSON) | caps 120 / 1,200 |
| `LocalAnalyticsStore` | `unictoos_analytics.db` (SQLite, v1) | table `streaming_sessions` (13 cols) | manual `SQLiteOpenHelper`; bounded to newest 120; no URLs/credentials |

Patterns: every store = dedicated prefs file, `*_v1` key constants, enums as
`.name`, defensive `runCatching { valueOf() }.getOrDefault(...)`, `.apply()`
async writes. **No migration framework** — versioning is by key renaming, and
scene JSON is coerced on read. Defensive size caps on read/write (scenes ≤64,
groups ≤16, sources ≤32/scene, names ≤128 chars, IDs ≤96, text ≤2000, URIs
≤2000). **Analytics has no opt-out** — both stores are written unconditionally
(local-only, no network upload; deleted with app data).

`data/ConfigExporter.kt` builds `"unictoos-config-v1"` JSON by hand via
`StringBuilder` + manual `quote()` escaper (no kotlinx.serialization);
`MainActivity.shareConfig` writes it to
`File(cacheDir, "unictoos-diagnostics-<ts>.json")` and shares via FileProvider.
`data/Media3RecordingEditor.kt`: trim/export via Media3 Transformer
(`ClippingConfiguration` → `<name>-trimmed-<ts>.mp4`); export = trim with
`endMs = Long.MAX_VALUE/4`; `addChapter` returns `Unsupported` (chapter-like
data lives in Unictoos's own `StreamMarker`s instead); guarded by
`streaming/RecordingEditPolicy.kt` + `RecordingValidator.kt`.

---

## 8. Navigation & UI structure

**No Navigation Compose** (dependency present, unused). `ui/UnictoosApp.kt`
hand-rolls navigation: `internal enum class AppTab { HOME, SCENES, STUDIO
("Go Live"), LIBRARY, SETTINGS }`, selected tab in `rememberSaveable`,
screen switching via `AnimatedContent(fadeIn/fadeOut 120 ms)`. Bottom bar shows
only Home / Go Live / Library (Material3 `NavigationBar` in a "GlassyBottomBar");
Scenes/Settings are reached via the top `GlassyTopBar` overflow menu. Thin
route wrappers (`HomeRoute`, `StudioRoute`, `SettingsRoute`) collect VM flows;
screens are presentational. **No back stack, no deep links** — back exits the
app; manifest has no VIEW/BROWSABLE filters. Onboarding
(`ui/OnboardingScreen.kt`, 4 pages) gates on
`unictoos_onboarding/complete` and lands on STUDIO.

Screen responsibilities:
- **Home** (`HomeScreen.kt`): `ExecutiveHero` readiness banner with pulsing
  CTA, `PreflightCard`, `ReadinessGrid` (Scenes/Network/Destination/Microphone),
  quick actions, first-2 scene cards, encrypted-key trust row. Note: permission
  and network checks run **during composition** (re-evaluated on every
  recomposition).
- **Scenes** (`ScenesScreen.kt`): the scene editor — templates, scene list,
  per-source `SourceToggleRow` (enable, z-order, opacity, geometry sliders,
  text content/size), `PipControls` (enabled only with one Screen + one Camera
  source), `PresentationControls` (transition mode chips, source groups). All
  mutations go to the VM; nothing starts capture.
- **Studio** (`StudioScreen.kt`, 656 lines): the broadcast surface. Single
  `LazyColumn`: `BrandHeader` + `StatusPill`, `PreviewCard` (`AndroidView`
  hosting `PreviewSurfaceView`; "Preview is waiting" until
  `session.previewReady`), `LiveTelemetryCard` (bitrate/FPS/transport/quality
  tier/dropped frames), `GoLiveReadinessCard`, `DestinationHealthCard`
  (per-slot state + retry counts), `SessionErrorCard`, adaptive-bitrate message
  card, `BroadcastActionCard` (Go Live / Stop / "Practice locally";
  `canStart` only from IDLE/STOPPED/ERROR → `MainActivity.requestStreamStart`
  with endpoints joined by `\u001F`, capture mode, `ScenePayloadCodec`
  scene JSON), `QuickControls` (mute, flip camera, record, mark moment),
  `DestinationReadiness`, `SessionHealthCard` (thermal), `SessionPreferences`
  (aspect ratio, auto-stop).
- **Library** (`LibraryScreen.kt`): largely VM-independent — scans
  `<filesDir>/recordings/*.mp4` (play via FileProvider `ACTION_VIEW`, share,
  rename via dialog — `renameTo()` result ignored, delete, trim via
  `Media3RecordingEditor`), plus creator analytics (`CreatorHistoryStore`,
  `LocalAnalyticsStore`, `StreamingDiagnostics.snapshot()` timeline). It
  constructs stores directly with `context` rather than via the VM.
- **Settings** (`SettingsScreen.kt`): destination platform chips + server
  URL/stream-key fields + clear + dashboard deep-links (external browser),
  direct multistream (≤2, toast on cap), quality presets + custom slider, audio,
  latency mode, device controls (**microphone/keep-awake are local
  `rememberSaveable` only — not persisted**), thermal, adaptive bitrate,
  config export/import (SAF `OpenDocument` picker → `vm.importConfigJson`),
  redacted diagnostics export. Edits locked while a session is active
  (`settingsLocked`).

`ui/PreviewSurfaceView.kt`: plain `SurfaceView` (not TextureView) in
`AndroidView`; stateless, reports via `Listener`
(`onSurfaceAvailable`/`onSurfaceDestroyed`); `setPreviewBufferLimit` caps the
preview buffer at the encoder profile size via
`CaptureCompatibilityPolicy.previewBufferSize`. `MainActivity.attachPreviewSurface`
passes the `Surface` to the service via `ACTION_ATTACH_PREVIEW` +
`EXTRA_PREVIEW_SURFACE` (detach uses a token guard).

`ui/MainActivity.kt`: `RequestMultiplePermissions` + `StartActivityForResult`
(MediaProjection consent) launchers. `requestStreamStart(...)` validates mode,
requests RECORD_AUDIO + POST_NOTIFICATIONS (API 33+) + CAMERA (camera mode) in
one batch, then `startCameraCapture()` (`ACTION_PREPARE_CAMERA`) or
`launchProjection()` (`MediaProjectionManager.createScreenCaptureIntent()`).
Projection result → `ACTION_PREPARE_PROJECTION` + resultCode/data + extras.
Pending request survives config change via `onSaveInstanceState` (activity is
portrait-locked; no `android:configChanges`). All other controls are thin
`startService()` intent dispatchers (stop, releaseCapture, toggleMute,
switchCamera, toggleRecording, createMarker, dismissStatusMessage,
attach/detach preview).

Theme (`ui/theme/Theme.kt`): `V02Palette` (Neutral950→100, `AccentBlue`
`#7D6BFF`, `PhotonCyan` `#4DE8FF`, `Danger` `#FF5D73`, `Caution` `#FFA24F`,
`EventHorizon` `#FF8B45`), `Spacing`/`MotionTokens` token objects, legacy
`UnictoosPalette` aliases, full Material3 typography. **Diverges from
`V02_A1_DESIGN_TOKEN_PROPOSAL.md`**: the proposal wanted one blue accent
(`#5B8DEF`), no general-purpose cyan/violet, restrained live indicators; the
code keeps the "Black Hole" identity (broad `PhotonCyan` use, violet accent,
always-on orbiting-particle `BlackHoleBackdrop` Canvas). Spacing/motion tokens
match the proposal. Takeover decision needed: v0.2 restrained tokens vs.
current aesthetic — the migration was never completed.

`integrations/ExternalFeatureContracts.kt`: pure unimplemented boundary
interfaces — `CloudBackupProvider`, `RemoteControlTransport`,
`BondingRelayProvider` (SRTLA/RIST), `ExternalVideoInputProvider` (UVC/USB),
`PictureInPictureCompositor` ("future shared-surface compositor"),
`RecordingEditor`, `AudioProcessor`. (No ads contract exists despite root
`ADS.md`.)

Manifest (`app/src/main/AndroidManifest.xml`, verified): permissions CAMERA,
RECORD_AUDIO, ACCESS_NETWORK_STATE, INTERNET, WAKE_LOCK, POST_NOTIFICATIONS,
FOREGROUND_SERVICE (+ _MICROPHONE/_CAMERA/_MEDIA_PROJECTION variants);
`android.hardware.camera` `required="false"`; `allowBackup="false"`; single
exported `MainActivity` (portrait); non-exported `StreamingForegroundService`
with `foregroundServiceType="mediaProjection|microphone|camera"`;
non-exported FileProvider (`${applicationId}.fileprovider`,
`grantUriPermissions=true`) exposing `files-path/recordings/`. No receivers,
no deep links. FileProvider gap to note: `shareDiagnostics()` targets
`cacheDir`, which is not an exposed root — see debt item 15.

---

## 9. Key design decisions & constraints noticed in the code

1. **Policy-object architecture.** Every decision that can be pure is a
   stateless object (`StreamStateMachine`, `StreamFailurePolicy`,
   `AdaptiveBitratePolicy`, `CaptureModePolicy`, `GoLiveReadinessPolicy`,
   `ThermalProtectionPolicy`, `StreamEndpointPolicy`, `RecordingEditPolicy`,
   …) with a matching `*Test.kt` (34 JVM tests, mostly policies). This is the
   codebase's best trait: the untestable Android/GL/RootEncoder parts are
   isolated in the service, and the decisions are unit-tested.
2. **Generation-stamped callbacks.** The `sessionGeneration` AtomicLong fence
   is the central defense against stale RootEncoder/projection callbacks after
   pipeline release — a direct response to the async-EGL-teardown crashes
   documented in `GL_FAILURE_AUDIT.md`.
3. **Credential hygiene by construction.** Keys never in logs/diagnostics/
   exports/source (`tools/security_source_audit.py` gates this); AES/GCM +
   Keystore; `serverUrl` is exported but `streamKey` is structurally nulled.
   (Weaknesses: §5.2 key-invalidation handling, fail-open decrypt.)
4. **Defensive bounds everywhere.** Size caps on scene JSON, sources, names,
   URIs; coercion instead of migration; corrupt-scene backup; 512 KB import
   cap; 200-event diagnostics ring; 120-item analytics caps.
5. **Deliberate non-goals.** No backend, no OAuth, no chat, no cloud relay —
   enforced by `ExternalFeatureContracts` being interfaces-only and by release
   notes' "Scope deliberately deferred" sections. Multistream is device
   fan-out (one encoder, packet duplication), not cloud relay.
6. **Device-specific workarounds are explicit.** `CaptureCompatibilityPolicy.shouldIsolateLivePreview`
   disables local preview on Infinix X6853 (firmware exhausts GL resources
   with preview attached); `MultistreamDefaults.THREE_DESTINATION_DEVICE_GATE`
   gates 3-destination behind that device passing validation.
7. **Sandbox-built, never device-tested.** Every release note since v0.5.0
   carries an "evidence boundary": JVM tests + lint + APK assembly pass, but
   **no physical device, emulator, ADB, logcat, ingest account, or endurance
   run was ever available**. The v0.5.1 Go Live crash was fixed blind. Treat
   all runtime behavior (PiP, reconnect, background audio, 60-min stability,
   Android 15 OEM limits) as unverified.
8. **Zero TODO/FIXME/HACK/XXX markers** in `app/src/main/java` (verified by
   grep; the only matches were `toDouble()` false positives). Debt is tracked
   in root audit docs (`GL_FAILURE_AUDIT.md`, `STABILITY_AUDIT.md`,
   `PRODUCTION_READINESS_ALPHA18.md`, `docs/…`), not in code.

---

## 10. Gaps, tech debt & risks (takeover priorities)

1. **God ViewModel** (`StudioViewModel`, 576 lines, 13 StateFlows) — scenes,
   credentials, 7 settings stores, multistream, export/import, session mirror.
   Mitigated by repository interfaces; still the first thing to split.
2. **Dual session truth**: `startPreparing()`/`enterLive()`/`stopStream()`
   (lines 558–575) mutate `_session` directly with hardcoded telemetry
   (`bitrateKbps = 4500, fps = 30`) while `init` collects
   `StreamingStatusBus.state` into the same flow. Dead scaffolding racing the
   real pipeline — remove or wire through the service.
3. **Credential fragility** (§5.2): no `KeyPermanentlyInvalidatedException`
   recovery; decrypt fails open to blank; empty values stored unencrypted;
   silent fallback to no-op repository. The riskiest code has **no tests**
   (no store tests at all: no `CredentialStore` round-trip, no `SceneStore`
   corruption-backup, no legacy-migration tests).
4. **Manual DI doesn't scale** — every new store touches the VM constructor;
   no scopes/qualifiers.
5. **`ScenePayloadCodec.schemaVersion = 2` is write-only** — decode never
   branches on it.
6. **Analytics without opt-out** (SQLite + prefs history, unconditional).
7. **`multistreamPlatforms` silently truncates** stored selections to 2 on
   load (`.take(2)`).
8. **`LibraryScreen` bypasses the VM**, constructing three stores/editors
   directly with `context`.
9. **Per-destination control is observation-only**: reconnect is coordinated
   across slots; independent slot stop/retry doesn't exist
   (`MultistreamStateReducer` aggregates; `DestinationStateMachine` exists but
   no session manager consumes it — "Stage A contracts").
10. **PiP is experimental** (`startExperimentalPipIfRequested`): no dragging,
    no guaranteed rounded corners/border/shadow, no custom EGL compositor;
    screen-only fallback. `ImageOverlay` skipped; COLOR sources uncomposited —
    the scene model promises more than the compositor delivers.
11. **Compose perf concerns** (`StudioScreen` ~20 params; permission/network
    checks and `GoLiveReadinessPolicy.evaluate(...)` inside composition;
    `DeviceCompatibilityReportFactory.current(...)` recomputed per
    recomposition in Settings; always-on `rememberInfiniteTransition`s —
    `BlackHoleBackdrop` redraws a full-screen Canvas every frame; duplicated
    `LivePulseDot`; `LibraryScreen.renameTo()` ignores its boolean).
12. **Reconnect policy is keyword-based** (`StreamFailurePolicy.classify`) —
    brittle against ingest servers with unusual error strings; max 3 attempts,
    2 s→30 s backoff.
13. **No back stack / deep links** — limits future OAuth redirect flows and
    share-target entry points.
14. **Release APK for v0.5.3 was never built** (R8 killed by sandbox memory);
    only the debug APK exists for 0.5.3. Release APKs are unsigned anyway.
15. **Broken diagnostics export.** `MainActivity.shareDiagnostics()` writes the
    support-bundle JSON to `cacheDir`, but `res/xml/file_paths.xml` exposes only
    `<files-path … path="recordings/"/>` — so `FileProvider.getUriForFile()`
    throws `IllegalArgumentException` ("Failed to find configured root").
    The Settings diagnostics-export button crashes instead of sharing. Fix:
    write the bundle under `filesDir/recordings/` or add a `<cache-path/>` root.
16. **Compose hot-path churn.** Permission/network checks run inside composition
    (`HomeScreen`, `StudioScreen`, `PreflightCard`); `GoLiveReadinessPolicy.evaluate(...)`
    recomputed on every Studio recomposition; `DeviceCompatibilityReportFactory.current(...)`
    recomputed per Settings recomposition. Always-on `rememberInfiniteTransition`s —
    notably `BlackHoleBackdrop`, which redraws a full-screen Canvas with per-frame
    trig on every tab even when idle. Cheap individually, but wasted GPU/CPU.

---

## 11. Current state per v0.5.x release notes (repo root)

- **v0.5.0** — feature release: experimental screen+camera PiP via
  `SurfaceFilterRender` (+ screen-only fallback), per-destination health
  observation over the shared MultiStream pipeline, default-on adaptive
  bitrate (80 %/15 s down, 95 %/60 s up, no restart), camera-only background
  audio with partial wake lock, text overlays via `TextObjectFilterRender`,
  lifecycle hardening (`START_NOT_STICKY` null-intent restarts). Explicitly
  "not represented as a device-certified production release."
- **v0.5.1** — hotfix for the **Go Live crash**: no GL filter-graph mutation at
  startup when a scene has no text overlays; PiP filter creation deferred until
  the encoder consumes frames; `RECEIVER_NOT_EXPORTED` screen-state receiver
  made optional; synchronous service init guarded
  (`service_initialization_failed`, wake lock released, safe stop).
- **v0.5.2** — visual only: "Black Hole" theme (event-horizon black, deep
  blue-black surfaces, blue-violet actions, photon-cyan live accents, warm
  orange cautions); palette names kept as a compatibility layer.
- **v0.5.3** (current) — UI redesign only: floating glass control deck, top
  workspace bar, compact bottom nav, animated Canvas black-hole backdrop,
  breathing Home hero, framed broadcast-monitor Studio preview. Streaming/
  capture paths untouched; RootEncoder stays 2.8.0. **Debug APK only**
  (87,255,394 bytes, SHA-256
  `3d03209aceb3cd6c4e0dc8102e814a8da36bb2b1547f7201bb26936bab22d819`).
- Standing "reliability boundary" (all four notes): no backend/cloud
  relay/Firebase/OAuth/WHIP/WebRTC/desktop-OBS-remote; chat integrations
  (YouTube API chat, Twitch IRC/WS, Kick chat) deferred; image overlay texture
  decoding deferred; direct multistream capped at 2 (device fan-out).
- Open GitHub issue **#1 "Release Unictoos v0.5.5"** (opened 2026-08-25) —
  the release-tracking issue; repo has since shipped 0.5.3.

---

## 12. Validation & test boundary

- Gates that run in-repo: JVM unit tests (34 files, policy-heavy),
  `tools/feature_smoke_test.py` (157 assertions in v0.5.3, incl. palette/
  motion checks), `tools/security_source_audit.py` (zero credential literals,
  zero direct logging calls), Android lint, debug/instrumentation APK assembly.
- `testOptions.unitTests.isReturnDefaultValues = true` — Android framework
  calls in JVM tests return defaults silently; keep in mind when trusting
  green tests near framework boundaries.
- 3 instrumented tests: `CredentialStoreTest`, `ScenePayloadCodecInstrumentedTest`,
  `PreviewSurfaceViewLifecycleTest` (need a device/emulator; not runnable in
  the build sandbox).
- **Never validated**: real ingest (YouTube/Twitch/Kick/custom RTMP/SRT),
  PiP on hardware, 60-minute endurance, thermal/battery under load,
  Android 15 OEM background execution, MediaProjection consent edge cases,
  `MainActivity` recreation mid-capture. `docs/` contains the physical-device
  test matrices and guides for when a device is available
  (`PHYSICAL_DEVICE_TEST_MATRIX_ALPHA11.md`,
  `physical-device-stream-test-guide.md`).

---

## 13. Quick-reference file map (where to look first)

| Concern | Start here |
|---|---|
| Going live, end to end | `streaming/StreamingForegroundService.kt` (actions in `onStartCommand`), `ui/MainActivity.kt` (`requestStreamStart`) |
| Why a stream failed | `streaming/StreamFailurePolicy.kt`, `streaming/StreamingStateContracts.kt` (`StreamingError`) |
| Stream state meanings | `streaming/StreamStateMachine.kt` |
| Preflight/readiness | `streaming/StreamPreflight.kt`, `streaming/GoLiveReadinessPolicy.kt` |
| Destinations & keys | `data/CredentialStore.kt`, `domain/MultistreamModels.kt`, `data/MultistreamSelectionStore.kt` |
| Scenes (model/persist/codec) | `domain/StudioModels.kt`, `data/SceneStore.kt`, `streaming/ScenePayloadCodec.kt` |
| PiP | `stream/CompositorSurface.kt`, `domain/PipConfig.kt` (+`PipGeometryPolicy`) |
| Text overlays | `overlay/OverlayRenderer.kt`, `overlay/StreamOverlay.kt` |
| UI state | `StudioViewModel.kt`, `streaming/StreamingStatusBus.kt` |
| Screens | `ui/screens/StudioScreen.kt` (broadcast), `ui/screens/ScenesScreen.kt` (editor), `ui/screens/SettingsScreen.kt` (destinations) |
| Notifications | `streaming/StreamingForegroundService.kt` (`createNotificationChannel`, id 4101) |
| Recording trim/export | `data/Media3RecordingEditor.kt`, `streaming/RecordingEditPolicy.kt` |
| Diagnostics/support bundle | `streaming/StreamingDiagnostics.kt`, `streaming/SupportabilityExport.kt`, `streaming/DeviceCompatibilityReport.kt` |
| Security rules | `SECURITY.md`, `tools/security_source_audit.py` |
| What "done" means for device testing | `PHYSICAL_DEVICE_TEST_MATRIX_ALPHA11.md`, `physical-device-stream-test-guide.md` |

---

*End of report. All statements verified against source in
`app/src/main/java/com/unictoai/unictoos`, `app/src/main/AndroidManifest.xml`,
`app/build.gradle.kts`, and `RELEASE_NOTES_v0.5.*.md`. Anything marked
"experimental" or "not device-validated" reflects the release notes' own
evidence boundaries, not an inference.*
