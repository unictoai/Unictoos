# Unictoos Engineering Brief — Phase 1 Feasibility and Scope

**Repository:** `unictoai/Unictoos`  
**Baseline:** `main` at `5f41f63` (`v0.1.0-alpha12` post-refactor)  
**Purpose:** Record the implementation boundary and follow-up scope for Parts A–C before changing production code.

## Executive conclusion

The requested work can be divided into three groups. **A1, A2, and A5 are directly implementable on the current architecture.** RootEncoder 2.4.5 exposes both `setVideoBitrateOnFly(int)` and the existing `prepareVideo(...)` / `prepareAudio(...)` configuration surface, so adaptive bitrate can be applied during a live session while resolution, frame rate, and audio configuration remain session-start settings.

**A3 is not a native RootEncoder feature and should stop at a technical plan in this pass.** `GenericStream` is constructed with one `VideoSource`; its supported source operation is replacement through `changeVideoSource(...)`, not concurrent composition. A proper screen-plus-camera PiP implementation therefore needs a dedicated GL compositor and its own validation cycle. **A6 is not supported by the exposed recording API**: RootEncoder's recording controller receives the already encoded video/audio buffers and formats from the live pipeline, without an independent recording encoder configuration.

The current UI already has a reusable animated live dot and a dark-first visual system. C1 is therefore a focused hierarchy improvement rather than a redesign. Scenes currently have a gradient icon and source-type chips, but no visual layer thumbnail; C3 can add a lightweight abstract preview without pretending to render the real camera or screen feed.

> **Approval gate:** This document is a scope and feasibility review. No implementation has been started for the new brief. C1 and C3 include proposed before/after directions for approval before code changes.

## Phase 1 audit findings

### Current streaming configuration

`StreamingForegroundService` currently prepares one fixed video pipeline at **720 × 1280, 30 FPS, 4.5 Mbps**, with audio at **44.1 kHz and 128 kbps**. The service reports RootEncoder's `onNewBitrate` callback into `StreamingStatusBus`, but does not yet maintain a rolling quality decision or change the encoder bitrate in response to the callback. Recording is started through `genericStream.startRecord(...)` on the same configured stream instance.

The existing service also samples battery percentage, thermal status, network transport, bitrate, FPS, dropped frames, and audio level once per second. `StreamingStatusBus` retains at most 120 health samples in process memory. `CreatorHistoryStore` persists session summaries and markers, but not individual `StreamHealthSample` records.

### RootEncoder 2.4.5 capability boundary

The cached API jar for RootEncoder 2.4.5 was inspected rather than relying on an unverified online example. `GenericStream` inherits the following relevant methods from `StreamBase`:

| Capability | RootEncoder 2.4.5 result | Consequence for Unictoos |
|---|---|---|
| Prepare video dimensions/FPS/bitrate | Available through `prepareVideo(...)` | A1 can replace fixed constants at session preparation time. |
| Change video bitrate while streaming | Available as `setVideoBitrateOnFly(int)` | A2 can apply live bitrate steps without faking an API. |
| Change resolution/FPS while streaming | No exposed equivalent found | Preset changes affecting dimensions/FPS must apply on the next preparation, or require a controlled restart. |
| Prepare audio sample rate/processing flags | Available through `prepareAudio(...)` | A5 can expose sample rate/bitrate and echo/noise flags through persisted settings. |
| Multiple simultaneous video sources | Not exposed by `GenericStream` | A3 requires a custom compositor; do not implement a half-working version here. |
| Switch one active video source | Available through `changeVideoSource(...)` | This confirms replacement, not screen-plus-camera composition. |
| Separate recording encoder settings | Not exposed | A6 should be documented and skipped for the current pipeline. |
| Recording controller input | Receives encoded buffers and media formats | A custom recorder cannot recover independent resolution/bitrate after live encoding. |

No new dependency is required for the directly implementable items. The existing Kotlin coroutines, Compose, SharedPreferences pattern, RootEncoder, JUnit, and Turbine dependencies are sufficient. A3 may require either a future custom GL implementation using the Android/RootEncoder surfaces already present or a carefully justified graphics dependency; that dependency decision belongs to the separate PiP design task.

## Part A — Streaming power and efficiency

### A1 — Adjustable resolution, bitrate, and FPS presets

**Feasibility: implementable now. Complexity: M.** Add a domain-level `StreamQuality` model containing a stable preset identifier, width, height, FPS, bitrate, and keyframe interval. Define the requested presets: 480p@30 data saver, 720p@30 balanced/default, 720p@60, 1080p@30, 1080p@60 strong-upload warning, and Custom with bitrate constrained to 1–8 Mbps and FPS constrained to 24/30/60. The model should keep portrait and landscape dimensions explicit rather than inferring them from the preset label, because the current scenes include both aspect ratios.

Add a `StreamQualityStore` backed by `SharedPreferences`, following `SceneStore`'s repository pattern. Extend `StudioViewModel` with an injectable quality repository and state/update methods, while leaving `CredentialStore` untouched. `SettingsScreen` should provide the picker and custom controls under a new Stream Quality section, including a clear indication that 1080p presets require a strong upload. `StudioScreen` should show the active selection as a compact chip near the scene/session header.

At session preparation, the service should load the persisted quality and pass its values to `prepareVideo(...)`. The service must retain a safe fallback to 720p@30 if preferences are absent or malformed. The implementation should not silently apply a changed resolution or FPS to an already running encoder; the selected values should be captured at preparation time and displayed as the active session configuration.

**Recommended commit boundary:** quality model/store and ViewModel/UI wiring, followed by service preparation wiring only if the implementation review prefers two commits. The brief's one-task-per-commit rule should be respected either way, with a full lint, unit-test, and debug APK build after each commit.

### A2 — Adaptive bitrate on network degradation

**Feasibility: implementable now. Complexity: M.** RootEncoder 2.4.5 exposes `setVideoBitrateOnFly(int)`, so the service can apply live bitrate changes without modifying the library. The decision logic should be isolated from Android and RootEncoder in a pure Kotlin class or function. It should consume a target bitrate, a bounded history of reported bitrate samples, elapsed time in the degraded/recovered state, and a minimum/maximum bitrate range, then return a decision such as hold, step down, or step up.

The proposed policy is to use a rolling average and enter degradation only after the reported average remains below approximately 60% of target for five consecutive seconds. Step down by a conservative increment, for example 10–15% of the current target with a lower bound determined by the selected quality. Recovery should require approximately 15 seconds above the recovery threshold and should step up gradually, never immediately returning to the original target. Exact increments should be covered by unit tests rather than encoded in UI code.

The service should call `setVideoBitrateOnFly(...)` only when the target changes and publish a visible message such as **“Reduced quality to maintain connection”**. The current Studio screen does not keep live messages prominent after the preparation state, so the implementation must add a persistent in-session quality-change notice or event row rather than relying only on a transient notification. The active target bitrate should also be visible in the health center. Recovery should likewise be surfaced so the user knows that the temporary adaptation has ended.

**Important limitation:** the callback is the only current throughput signal in the service. It should be treated as a reported encoder/network health signal, not as a claim of exact user bandwidth. A later task may add a more direct sent/dropped-frame measurement if needed.

### A3 — Picture-in-picture screen-plus-camera compositor

**Feasibility: not natively supported; stop after technical scoping. Complexity: L.** RootEncoder's `GenericStream` constructor accepts one `VideoSource`, and the available source API supports replacing that source. The exposed GL interface provides filters over the active rendered input, but there is no second-source compositor API that combines `ScreenSource` and `Camera2Source` into one encoded frame.

A future PiP task should introduce a dedicated compositor layer. The likely architecture is a custom video source or GL rendering stage with a shared EGL context: the screen source remains the base texture, the camera source is rendered into a second external texture, and a compositor draws the camera texture into a rounded or rectangular corner viewport before the final frame is delivered to RootEncoder's encoder surface. The compositor must own texture synchronization, source lifecycle, orientation and crop handling, portrait/landscape transforms, camera mirroring, encoder-size changes, and clean teardown when projection permission ends.

The future data model would add a persisted per-scene layout object containing position (`TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`), size (`SMALL`, `MEDIUM`, `LARGE`), and optionally a normalized offset for later drag support. The first version should restrict controls to the four corners and three sizes as requested; arbitrary drag-and-resize can follow only after the basic compositor is stable.

The principal risks are EGL/SurfaceTexture synchronization across two producers, device-specific camera orientation behavior, additional GPU and thermal load, screen-projection lifecycle errors, and regression risk in the existing single-source paths. **No PiP code should be added in the current implementation pass.**

### A4 — Battery- and thermal-aware quality throttling

**Feasibility: partially implementable now, with an encoder boundary. Complexity: M.** The service already samples battery and `PowerManager.currentThermalStatus`. A policy can detect `THERMAL_STATUS_MODERATE` as the first actionable Android thermal level and treat severe/critical/emergency/shutdown as increasingly urgent. The service can use the A2 live bitrate mechanism immediately, publish a dismissible explanation, and record that a thermal cap is active.

A full preset downgrade that changes resolution or FPS cannot be applied to a running RootEncoder session through the exposed API. Therefore the safe design is two-tiered: apply an immediate bitrate reduction during the current session, and mark a lower resolution/FPS preset as the next-session configuration unless a future task implements a controlled stop/reprepare/reconnect flow. This avoids claiming that the stream is 720p when it has actually been reconfigured silently and avoids destabilizing a live broadcast.

Add a persisted Settings toggle, enabled by default, for **Automatic thermal quality protection**. The active thermal cap and its reason must be visible in the Studio health center and in the status message, with a dismiss action that hides the explanation without disabling the policy. User dismissal must not undo the protective bitrate change. The implementation must not alter reconnect or authentication error handling.

### A5 — Audio quality and processing options

**Feasibility: implementable now. Complexity: S–M.** Add a small persisted audio settings model with Standard at 128 kbps and High at 192 kbps, plus booleans for echo cancellation and noise suppression. Keep the current 44.1 kHz sample rate unless a separate requirement changes it; the user request is specifically about bitrate and processing toggles.

The existing `prepareAudio(...)` API already accepts the audio configuration and processing flags. The service should load the settings when preparing the session and pass the selected values without changing microphone permission behavior. `SettingsScreen` should place these controls beside Stream Quality, and the Studio health center should continue to report the live audio state. Device support for echo/noise processing should be handled defensively because Android audio effects can be unavailable or inconsistent on some hardware; the UI should report requested configuration, not promise identical DSP behavior on every device.

### A6 — Independent recording quality

**Feasibility: not supported by the current RootEncoder pipeline; skip implementation. Complexity: L if pursued later.** `startRecord(...)` attaches a recording controller to the stream. The recording controller receives the encoded buffers and media formats produced by the live encoders; its public API does not expose a separate video encoder bitrate, resolution, or FPS configuration. Consequently, local MP4 recording currently inherits the live encoded stream's video/audio characteristics.

A future independent-recording task would require a second encoding pipeline or a custom dual-output architecture, with significant CPU/GPU, battery, synchronization, storage, and thermal consequences. It should not be simulated by adding a second selector that has no effect. Add a code comment near the recording entry point documenting this RootEncoder limitation when the surrounding Part A changes are implemented.

## Part B — Follow-up feature scoping

The table below describes the smallest credible implementation boundary, likely files, and rough complexity. These are **not implementation approvals**.

| # | Feature | Technical scope and requirements | Likely file impact | Complexity |
|---:|---|---|---|:---:|
| 1 | Stream duration/data estimator | Calculate estimated MB/hour from the selected video bitrate plus audio bitrate, show current-session and projected usage, and label it as an estimate because protocol overhead and network retransmission vary. | `StudioScreen.kt`, `StudioViewModel.kt`, `StudioModels.kt` or a pure formatter/test file, quality/audio settings models | S |
| 2 | Scheduled/timed auto-stop | Add a duration selector, persist only a user preference if useful, start a monotonic countdown in the foreground service, surface remaining time, and stop safely at zero. Must define behavior during reconnect and app/process recreation. | `StudioScreen.kt`, `StudioViewModel.kt`, `StreamingForegroundService.kt`, `StreamingStatusBus.kt`, service tests | M |
| 3 | Text overlay editor | `SourceType.TEXT` exists, but `Source` currently has no text payload, font, color, alignment, or position fields, and the UI only adds a named source. Add an editor, persisted text style/layout data, preview rendering, and a streaming compositor path; the data/UI portion is not sufficient without rendering support. | `StudioModels.kt`, `SceneStore.kt`, `ScenesScreen.kt`, `SharedComponents.kt`, `StudioViewModel.kt`, future rendering layer/service | L |
| 4 | Webcam/USB capture card | Detect supported USB video devices, request and manage USB permission, translate camera frames into a RootEncoder-compatible video source, expose source selection, and handle attach/detach. RootEncoder's built-in camera sources do not automatically provide generic USB capture support. | Manifest, new USB/device source package, `StudioModels.kt`, `ScenesScreen.kt`, service, device tests | L |
| 5 | Multi-destination streaming | One captured/composited signal must feed multiple concurrent RTMP/RTMPS clients, with independent connection/error/reconnect state and aggregate bandwidth accounting. A single `GenericStream` start call is insufficient; likely requires multiple encoders or a fan-out architecture. | Streaming package, `StreamingStatusBus.kt`, destination models/UI, service, extensive integration tests | L / High |
| 6 | Session-over-session health graph | Current health samples exist only in the bounded in-memory `StreamingStatusBus.healthHistory`; `CreatorHistoryStore` persists session summaries and markers, not samples. Add a versioned, bounded persistence format and aggregation/downsampling, then render a graph with empty/loading states. | `CreatorHistoryStore.kt` or new `HealthHistoryStore.kt`, `StudioModels.kt`, `LibraryScreen.kt` or a new analytics screen, ViewModel, chart rendering | M |
| 7 | Quick-swap scene transitions | Define active scene state in the service, implement cut immediately and fade through a compositor/rendering layer, and ensure source lifecycle changes do not interrupt the stream. Fade cannot be delivered by the current one-source RootEncoder path without rendering support. | `ScenesScreen.kt`, `StudioViewModel.kt`, `StreamingForegroundService.kt`, future compositor, scene models | L |
| 8 | Local scene/destination config export | Serialize scenes and non-secret destination metadata to shareable JSON. Explicitly omit stream keys, redact any endpoint fields if they may contain secrets, validate imports, and use Android Sharesheet/FileProvider. | `SceneStore.kt`, a new config codec/export repository, `StudioViewModel.kt`, `SettingsScreen.kt`, FileProvider/share code, tests | M |
| 9 | In-app audio level meter | Expose a safe peak/RMS signal from the microphone capture path, sample it at a UI-friendly rate, publish it through the status bus, and render a throttled meter. The current `audioLevel` field is present but not populated with a user-facing live meter contract. | `StreamingForegroundService.kt`, `StreamingStatusBus.kt`, `StudioModels.kt`, `StudioScreen.kt`, audio tests | M |
| 10 | Low-latency mode | Add a persisted mode that changes keyframe interval and any supported encoder/client buffering parameters, with platform-specific warnings and fallback behavior. Verify the RootEncoder/ingest implications experimentally; lower latency trades away resilience. | quality/stream settings models and store, `SettingsScreen.kt`, service preparation, Studio status UI, tests | M |

The highest-value next candidates after A1/A2/A5 are **B1, B6, and B9** because they build on existing telemetry and require less media-pipeline risk. **B3, B5, B7, and B10** should be scheduled only with explicit media-architecture acceptance criteria.

## Part C — UI and visual polish

### C1 — Studio live-status hierarchy: proposed before/after

**Before:** the live state is represented by a small magenta status surface at the top-left of the preview, a compact `StatusPill` in the header, and three metric cards below the preview. The colors are on-brand, but the signal competes with the preview and the metric cards do not form a single glanceable health cluster. The shared `LivePulseDot` already animates alpha between 0.55 and 1.0, but the preview badge itself does not use a stronger animated treatment.

**After proposal:** make the live state a deliberately dominant but restrained broadcast signal. Increase the LIVE badge's horizontal and vertical padding, use a higher-contrast mint/magenta treatment with a thin accent stroke, place the animated dot before the word LIVE, and add a soft pulsing accent ring or halo that stays within the graphite visual language. Group the bitrate, FPS, audio, battery, and thermal readouts under a clearly labeled **Live health** band immediately below the preview, with stronger values and quieter labels. Keep the animation at approximately 900–1,100 ms and use `animateFloat` with `infiniteRepeatable`; do not animate the whole card or create a distracting flashing effect. When not live, return to the current quieter PREVIEW/READY treatment.

**Approval question:** approve this as a contrast-and-grouping improvement using the existing Mint/Magenta accents, with no new palette and no replacement of the preview card.

### C2 — Surface and elevation language

The audit found that actionable and informational cards use similar Material3 card treatment in shared components. The intended direction is to reserve a subtle `Stroke` border and pressed-state scale/alpha feedback for tappable scene/action cards, while keeping metrics, health telemetry, and trust rows informational and visually quieter. This should be implemented centrally in `SharedComponents.kt` where possible, not as unrelated per-screen overrides. The exact pressed-state animation should remain short and tactile, approximately 100–150 ms, without affecting layout stability.

### C3 — Scene thumbnails: proposed before/after

**Before:** `SceneCard` shows a 62 dp gradient square containing a single scene icon, followed by scene name, ratio, source count, and up to three source-type chips. This is readable, but scenes with different source combinations can look nearly identical when scanning the list.

**After proposal:** replace or augment the single icon tile with a small abstract aspect-ratio preview made of colored blocks representing enabled source layers. Use deterministic source-type colors from the existing palette: screen as cyan, camera as magenta, image as violet, text as mint, and color/background as a muted graphite/amber block. Arrange blocks in z-order with a simple base/background rectangle and one or two offset layers; show opacity through alpha. Keep the thumbnail purely symbolic—no fake camera image, no screen capture, and no heavy Canvas renderer. Preserve the scene name and source chips as the accessible textual explanation, and provide a content description such as “Main Camera scene preview: screen and camera layers.”

**Approval question:** approve a lightweight colored-block layer preview in the existing 62–72 dp scene tile, preserving the graphite palette and avoiding real media rendering.

### C4 — Onboarding and empty states

Library and Engage should be checked for first-run states. The target is a designed state with an accent icon, a concise explanation, and one useful call to action: Library should guide the user to record a practice or live session; Engage should explain that platform authorization is not connected and point to the integration boundary/settings. Empty states should not imply that a failed network request is equivalent to a first-run state.

### C5 — Motion and tactile feedback

The current bottom navigation is hosted in `UnictoosApp.kt`; the scope is a 150–200 ms crossfade or small horizontal slide when the selected tab changes, with no change to navigation semantics. Go Live/Stop should provide immediate press feedback and an icon/label state transition while the service enters PREPARING. The visual feedback must not claim LIVE until `onConnectionSuccess()` publishes LIVE.

### C6 — Accessibility

`TextMuted` is `0xFFA6ADB7` and should be checked against both `Surface` and `SurfaceRaised` using actual rendered background values before changing the token. If any body-sized combination fails WCAG AA, introduce a more readable muted token for body copy while retaining the current lower-contrast token only for decorative metadata. Icon-only buttons and controls must receive meaningful content descriptions; the audit found multiple `Icon(..., null)` usages in shared components, including warning, microphone/source, trust, and scene icons. Text buttons with visible labels can retain null icon descriptions to avoid duplicate announcements, but mute/record and other icon-only actions require explicit labels.

## Recommended implementation order after approval

A safe sequence is **A1 quality persistence and preparation**, **A2 adaptive bitrate with pure decision tests**, **A5 audio settings**, and then **A4 thermal policy** once the quality/bitrate state model is stable. C1, C2, C3, C4, C5, and C6 can be scheduled as separate UI-only commits, with C1 and C3 requiring approval of the directions above first. A3 should become its own design/investigation task, and A6 should remain documented rather than exposing a selector that the engine cannot honor.

Every approved implementation task should remain isolated in its own commit, avoid CredentialStore and reconnect/auth-error changes, and run the repository's required validation after the change:

```text
./gradlew lintDebug test assembleDebug
python3 tools/feature_smoke_test.py
```

A task is not complete until the build and relevant tests pass and the commit remains focused on the approved item.
