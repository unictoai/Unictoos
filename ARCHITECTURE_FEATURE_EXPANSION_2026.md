# Unictoos feature expansion architecture — 2026

## Architectural rule

Unictoos must keep the **media path**, **creator data**, and **external integrations** separate. The media path owns capture, composition, encoding, recording, reconnect, and foreground-service lifetime. Creator data owns scenes, templates, settings, session history, recordings, and local diagnostics. External integrations own OAuth, platform metadata, chat, events, moderation, scheduling, clips, and provider-specific capabilities.

> Stream keys are not OAuth credentials. OAuth refresh tokens are not scene data. Raw capture frames and microphone samples must not leave the device unless a future feature explicitly requires it and the creator consents.

## Current application boundary

The existing Compose UI calls `StudioViewModel`. The ViewModel owns scene and destination state and subscribes to `StreamingStatusBus`. `StreamingForegroundService` owns MediaProjection, Camera2Source, MicrophoneSource, RootEncoder, reconnect, mute, recording, and notifications. `CredentialStore` uses Android Keystore for per-platform stream keys and endpoints. This boundary should be extended rather than replaced.

## Planned modules inside the native app

| Module | Responsibility | May access | Must not access |
|---|---|---|---|
| `domain` | Immutable scenes, layers, session health, recordings, integrations, platform capabilities | Plain domain types | Android services, network clients, raw credentials |
| `data/local` | DataStore/JSON or Room-backed persistence for scenes, preferences, session history, recording metadata | Encrypted local storage and app-private files | Provider APIs or raw media frames |
| `data/credentials` | Keystore-backed stream keys and OAuth token envelopes | Android Keystore | UI rendering or media buffers |
| `streaming` | Capture, compositor, encoder, recording, reconnect, health sampling, notifications | MediaProjection, Camera2, AudioRecord, RootEncoder | OAuth tokens, chat text, ad providers |
| `integrations` | Platform adapters for YouTube, Twitch, Kick, custom RTMP metadata, chat, events, moderation | User-approved OAuth tokens and provider APIs | Stream keys unless a platform explicitly requires a local RTMP preset |
| `ui` | Compose screens, state models, loading/error/permission surfaces, accessibility | ViewModel state and user actions | Direct network calls, direct Keystore access, media engine internals |
| `monetization` | Provider-neutral app-only sponsor policy and creator-controlled graphics policy | Preferences and explicit scene state | Stream frames, recordings, credentials, raw capture |

## Persistence strategy

The first expansion slice can use a versioned JSON/DataStore repository for scenes, templates, preferences, and session history because the current app has a small data volume and already depends on DataStore. Recording files remain in app-private storage with a metadata index. A migration version must be stored with every serialized aggregate. If chat history, analytics, or large event data grows beyond bounded local history, move those collections to Room in a later milestone.

Scene persistence must include source payload, enabled state, bounds, z-order, opacity, and an optional editor-only flag. A persisted scene should be renderable without network access. Templates are immutable seed definitions copied into user-owned scenes so future template updates cannot unexpectedly alter a creator’s broadcast.

## Media-path design

The foreground service remains the single owner of capture and encoder objects. The compositor is introduced behind a `SceneRenderer` interface with a capability-safe fallback to the current single-source engine. The service publishes bounded health samples at a low frequency, such as one sample per second for UI and a shorter rolling window for diagnostics. The UI must never poll RootEncoder directly.

Practice mode uses the same capture preparation path but never calls `startStream`. It may write a local MP4 and publish `PRACTICE` as a separate session mode. Pause and standby must be explicit service actions with a clear notification and a safe fallback when the encoder cannot pause without disconnecting.

## External integration design

OAuth is introduced through a provider-neutral `IntegrationAccount` model and a PKCE-capable boundary. The Android client may hold short-lived access state, but refresh tokens should be protected and preferably exchanged through a backend when provider policy requires a confidential client. Each action declares required scopes and shows a confirmation surface for moderation, chat sends, clips, and metadata changes.

The first engagement release should normalize read-only chat and events into local domain models. Provider adapters must expose capability flags so Kick’s dashboard-managed title/category workflow, Twitch’s moderation scopes, and YouTube’s eligibility/scheduling rules are represented honestly.

## Battery and thermal budgets

The app should not run continuous animations, high-frequency UI polling, or background network work when idle. Health sampling is bounded and pauses when no session exists. Preview rendering uses the selected scene FPS and resolution instead of a permanently elevated rate. Thermal and battery warnings should recommend reducing resolution, FPS, or bitrate before attempting automatic degradation. Background streaming must be opt-in and accompanied by a foreground notification.

## Release gates

A feature that touches capture, audio, camera, foreground services, background execution, encoding, OAuth, or external protocol support cannot be considered complete from a sandbox build alone. It requires unit coverage for state transitions, static smoke checks, build validation, an explicit physical-device test row, and a documented fallback path. The app must remain launchable and usable with all integrations disconnected.
