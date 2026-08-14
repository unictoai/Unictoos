# Unictoos Professional Product Plan

## Product goal

Transform the current Unictoos alpha into a serious, mobile-first Android creator studio for streaming to **YouTube, Twitch, Kick, and compatible RTMP/RTMPS destinations**. The product should feel intentional and professional on a phone, not like a compressed desktop broadcast tool. The uploaded Unictoos logo will become the primary brand asset across the launcher icon, splash screen, navigation identity, empty states, and release materials.

The current APK is a technical alpha, not a finished product. The next iteration must prioritize **reliability and clarity over feature count**. A creator must know what is configured, what permission is missing, whether the microphone is actually receiving audio, whether the destination accepted the stream, and how to recover when something fails.

## Baseline problems to address first

| Problem | Current risk | Planned response |
|---|---|---|
| UI feels unfinished | Users cannot understand the workflow quickly | Replace the scaffold with a coherent creator-studio information architecture and design system |
| Microphone permission appears to work but audio does not | A stream may be live with silent audio | Add explicit permission state, microphone self-test, input meters, audio-route diagnostics, and service-level error reporting |
| Platform keys are not obvious | Users do not know where to configure YouTube, Twitch, or Kick | Add a dedicated Destinations area with platform cards, setup instructions, secure fields, test connection, and key rotation/clear actions |
| Current service path is not fully validated | Screen capture, encoder, and transport can fail differently by device | Add capability detection, structured state transitions, physical-device tests, and clear recovery messages |
| Feature surface is narrow | The app does not yet feel like a serious creator product | Add recording controls, scene editing, overlays, health metrics, presets, and creator workflow features in milestones |
| Advertising is not designed | Ads could damage trust or appear in a broadcast | Add an optional in-app ad architecture that never inserts ads into the live output by default |
| Logo is not integrated | Brand identity is inconsistent | Generate Android-ready icon/splash variants from the uploaded logo while preserving its identity and colors |

## Phase 1 — Audit and reliability baseline

Before redesigning screens, inspect the current `unictoai/Unictoos` branch, the last released APK, and the current RootEncoder service flow. Reproduce the microphone failure on a physical Android device with a clean install. Capture permission results, `AudioRecord`/microphone availability, foreground-service state, audio-route selection, encoder preparation, and stream callbacks.

The microphone investigation will distinguish among permission denial, Android privacy toggle state, microphone contention by another app, audio-source initialization failure, muted or zero-level input, service lifecycle timing, and platform-side audio rejection. The UI will not report “microphone ready” merely because permission was granted; it will report a tested input state with an actual meter signal.

This phase will also establish a bug ledger, supported Android/device matrix, reproducible build command, release artifact mapping, and a definition of “stream ready.” The goal is to prevent visual work from hiding a broken broadcast path.

## Phase 2 — Logo and visual identity

Use the uploaded logo as the authoritative Unictoos mark. Preserve the purple-to-pink gradient, dark background, central broadcast symbol, and wordmark identity. Create application-ready variants through the appropriate image-generation/editing workflow rather than redrawing the brand manually.

| Asset | Use |
|---|---|
| Adaptive launcher icon | Foreground mark and safe-zone background for Android launchers |
| Monochrome icon | Android themed icon support |
| Splash mark | Centered logo with dark brand background and safe margins |
| Compact mark | Navigation, notifications, and small status surfaces |
| Wordmark lockup | Home header, about page, release documentation, and store material |
| Broadcast overlay mark | Optional creator-controlled scene source, disabled by default |

The visual system will use a dark navy-black base, electric violet and magenta accents, restrained cyan status highlights, white primary text, and muted blue-gray secondary text. Color semantics will be consistent: green for healthy/live, amber for attention/testing, red for failure, and violet/magenta for primary actions. The logo will not be stretched, clipped, or placed behind low-contrast UI.

## Phase 3 — Professional mobile UI redesign

Replace the current scaffold with a mobile creator workflow organized around five clear destinations:

1. **Home:** broadcast readiness, recent scenes, recent recordings, destination status, and one primary action.
2. **Studio:** large preview, selected scene, audio meters, live health, bitrate/FPS, mute, record, and stop controls.
3. **Scenes:** scene cards, source list, canvas editor, templates, portrait/landscape modes, and source visibility/lock controls.
4. **Destinations:** YouTube, Twitch, Kick, and Custom RTMP cards with setup status, secure key fields, test connection, and clear/rotate controls.
5. **Library/Settings:** recordings, storage, permissions, device capability, privacy, diagnostics, and optional ad preferences.

The new UI will avoid dense OBS-style panels. It will use large touch targets, bottom sheets, step-by-step setup, source chips, simple cards, a glanceable status header, and a compact Studio control rail. The first-run flow will be guided:

> **Create scene → choose destination → run microphone/capture checks → preview → Go Live**

Every important state will have a useful screen: no destination, missing permission, microphone silent, encoder unavailable, network unavailable, invalid stream key, reconnecting, live, stopped, and test successful. No button will lead to a dead end or silently fail.

## Phase 4 — Microphone and streaming repair

Refactor the media pipeline around explicit lifecycle states: `Idle`, `CheckingPermissions`, `PreparingAudio`, `PreparingCapture`, `Connecting`, `Live`, `Reconnecting`, `Stopping`, and `Error`. Each state will expose a user-readable message and a technical diagnostic code.

The microphone path will include permission state, Android privacy-switch state where available, selected audio source, sample rate, channel configuration, echo cancellation/noise suppression capability, current input level, clipping detection, and release/cleanup behavior. A pre-live microphone test will ask the user to speak and will show a meter response before streaming is allowed.

The stream service will add proper start ordering, foreground-service timing, MediaProjection consent handling, encoder capability detection, RTMP/RTMPS connection callbacks, bounded reconnect behavior, stream-key redaction, and guaranteed cleanup when the app or service stops. The service will not claim a live state before the transport success callback arrives.

## Phase 5 — Destination management for YouTube, Twitch, and Kick

Create a dedicated destination manager instead of a single generic text form. Each platform card will show whether it is configured, when it was last tested, which protocol is being used, and what the creator needs to update.

| Destination | Initial support | Later support |
|---|---|---|
| YouTube | Stream URL + stream key, private/unlisted testing, bitrate presets | OAuth, titles, scheduled streams, thumbnails, chat, metadata |
| Twitch | Ingest/server URL + stream key, test category/title guidance | OAuth, category/title management, chat, stream metadata |
| Kick | Current dashboard-provided ingest URL + stream key | Platform-specific metadata and account integration if officially supported |
| Custom RTMP/RTMPS | Manual URL/key, connection test, secure storage | Multiple destinations and advanced routing |

The first stable path will keep custom RTMP/RTMPS independent from OAuth so platform API changes do not prevent creators from going live. Stream keys will be stored using Keystore-backed encryption, excluded from scene exports and diagnostics, and cleared through a visible action.

## Phase 6 — Core creator features

After the broadcast path is dependable, add the features that make Unictoos useful beyond a basic screen streamer:

| Priority | Features |
|---|---|
| P0 | Camera source, screen source, microphone/system-audio options, local recording control, visible meters, destination presets, test mode |
| P1 | Scene templates, portrait/landscape presets, text/image/video/GIF sources, crop/resize/lock/hide/reorder, countdown and BRB scenes |
| P1 | Stream health dashboard, dropped-frame indicator, network quality, bitrate/FPS controls, thermal and battery warnings |
| P1 | Recording library with playback, share, rename, delete, storage checks, and recovery from interrupted recording |
| P2 | Multi-scene switching while live, multi-streaming, chat, platform OAuth, remote control, VTuber/Live2D |
| P2 | Cloud backup, collaboration, voice chat, creator asset packs, advanced transitions |

Each feature will be accepted only after it has a user-facing state model, a test case, a failure message, and documentation. Feature count will not be used as a substitute for reliability.

## Phase 7 — Optional advertising architecture

The advertising request will be implemented as **optional, in-app monetization**, not as an uncontrolled broadcast overlay. Ads should never appear inside the creator’s streamed or recorded output unless the creator explicitly adds a sponsor graphic as a scene source.

The architecture will use an `AdProvider` interface and a remote/configurable `AdsPolicy` so the app can support an ad network later without coupling the media engine to a provider SDK. Development builds will disable live ads. The production policy will be able to control whether ads are enabled, where they appear, frequency limits, and whether a no-ads or sponsorship build is available.

Initial placements should be limited to non-live surfaces such as Home, Library, or Settings. There will be no ad overlay over the Studio preview, no ad interruption during an active broadcast, and no ad insertion into the outgoing RTMP/RTMPS frames by default. Privacy consent, age/region requirements, network failure handling, and a user-visible ad preference will be documented before enabling a provider.

## Phase 8 — Quality, release, and professional readiness

The release process will include a clean checkout build, debug APK, release-signed artifact workflow, SHA-256 checksum, dependency/license report, changelog, known limitations, and a physical-device test report. The first public alpha will remain clearly labeled until the app passes sustained streaming tests.

Testing will cover Android 10 through the current supported Android version, low/mid/high-tier devices, Wi-Fi and mobile data, permission revocation, screen lock/unlock, rotation, app backgrounding, battery/thermal pressure, network interruption, invalid keys, reconnect behavior, microphone silence, recording storage exhaustion, and clean stream termination.

## Definition of success

Unictoos is ready for the next serious alpha when a creator can install the APK, understand the interface immediately, configure a YouTube/Twitch/Kick destination without guessing, verify microphone input before going live, create a scene, start a private test broadcast, observe a truthful live/connecting/error state, stop cleanly, and recover from common failures without losing credentials or being misled by the UI.

The current priority order is therefore:

> **Reliability first → professional mobile UI → destination clarity → creator features → optional ads → scale and integrations.**

## Assumptions

This plan assumes that the uploaded image is the final Unictoos logo direction, that ads are intended for the app interface rather than forced into the stream output, that the app remains Android-only for the next major milestone, and that core streaming remains free and open source. If ads are intended to be creator-controlled sponsor overlays inside the broadcast, that should be treated as a separate scene-source feature with explicit opt-in behavior.
