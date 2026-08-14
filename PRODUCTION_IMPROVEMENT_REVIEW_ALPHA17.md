# Unictoos Production-Improvement Review

## Executive assessment

The attached prompt is a strong and appropriate production-hardening brief. It correctly insists that Unictoos must be evaluated as a real creator product rather than as a collection of attractive screens. The most important conclusion from comparing it with the alpha17 repository is that **Unictoos has a credible native Android streaming foundation, but it is not yet a production-complete scene-based streaming studio**.

Alpha17 now includes a real Android preview surface and a safer capture-start sequence. The repository also has a real foreground service, RootEncoder transport integration, encrypted local credential storage, MediaProjection capture, camera capture, microphone probing, local recording, bounded reconnect attempts, adaptive bitrate policy, persistence, and a meaningful static smoke suite. These are useful foundations, not merely a prototype facade. However, the outgoing encoded video still receives one active RootEncoder video source rather than the complete scene described by the scene editor. The repository itself documents that screen-plus-camera composition is planned rather than implemented.[1]

The central production risk is therefore **feature truthfulness**: the UI and data model expose scene concepts such as camera, screen, image, text, opacity, and z-order, but those scene layers are not yet rendered into the encoded stream. A second important risk is telemetry truthfulness: transport bitrate is reported from the engine, but configured FPS is copied into the live state rather than measured, dropped-frame telemetry is not demonstrably sourced from the encoder, and the audio meter is explicitly limited by RootEncoder’s public API.[2]

> **Recommendation:** Do not attempt to implement the entire prompt in one broad refactor. Preserve the existing architecture and execute a staged P0/P1/P2 program. First make the current single-source stream path measurably reliable and honest; then implement a real GPU compositor; then add provider APIs and larger creator features.

## Current alpha17 classification

The following classification is based on source code and repository documentation rather than README claims alone. “Fully working” means implemented in code and internally coherent; it does not mean verified across physical devices or real platform ingest unless explicitly stated.

| Subsystem | Classification | Evidence and production interpretation |
|---|---|---|
| Android project and build | **Fully working in the sandbox; device matrix missing** | Alpha17 passed lint, JVM tests, debug assembly, release assembly, and 47/47 static smoke checks. Physical-device behavior remains unverified in this review. |
| Compose UI and navigation | **Implemented** | The app has Home, Scenes, Studio, Engage, Library, More, and Settings flows with mobile-first controls. UI quality is no longer the primary production blocker. |
| Foreground streaming service | **Partially working / fragile under advanced lifecycle cases** | The service owns RootEncoder, capture, recording, notifications, reconnect scheduling, and cleanup. It still lacks a complete explicit state machine with separate CONNECTING, STOPPING, STOPPED, and FAILED states, and process-recreation behavior requires physical testing. |
| RTMP/RTMPS transport | **Partially working** | RootEncoder `GenericStream` supplies the real transport path and supports generic endpoints. Real YouTube, Twitch, and Kick ingest has not been verified in this environment. |
| Alpha17 preview | **Implemented, but readiness is surface-level** | Studio now attaches a real Android `Surface` to RootEncoder. `previewReady` currently means that the preview surface was attached successfully; it is not yet a measured first-frame or frame-rate confirmation. |
| Screen capture | **Partially working** | MediaProjection permission, foreground-service setup, source replacement, preview attachment, and cleanup are present. Projection-stop callbacks, process death, recreation, and repeated start/stop behavior still need device validation and stronger lifecycle handling. |
| Camera capture | **Partially working** | The Camera2 source path and permission checks exist. Camera switching, front-camera mirroring policy, rotation behavior, camera disconnect recovery, and a device matrix are not complete. |
| Microphone capture | **Partially working** | Microphone permission, an off-main-thread `AudioRecord` availability probe, RootEncoder microphone capture, mute, sample rate, bitrate, echo cancellation, and noise suppression settings exist. A real single-owner level meter, gain control, Bluetooth routing, and system-audio support are not complete. |
| Audio level telemetry | **Partially working and honestly limited** | The repository deliberately avoids opening a second `AudioRecord` and documents that RootEncoder 2.4.5 does not expose a safe public peak/RMS callback.[2] The next implementation should add a single-owner PCM tap or another supported measurement path rather than fabricate values. |
| Scene data model and persistence | **Partially working** | Scene/source metadata, text fields, opacity, z-order, aspect ratio, and defensive JSON loading are persisted. There is no schema migration system, duplicate-scene workflow, or explicit data-recovery/version strategy. |
| Scene compositor | **Planned but not implemented** | `GenericStream.changeVideoSource(...)` replaces one source; it does not combine screen, camera, image, text, and background layers. The current plan correctly identifies the need for a dedicated EGL/OpenGL compositor.[1] |
| Scene editor | **Partially working / UI-only for output-critical behavior** | Source toggles, text editing, opacity, and ordering are present. Touch drag, resize, image selection, background editing, and parity between editor preview and encoded output are missing. |
| Recording | **Partially working** | RootEncoder creates app-private MP4 recordings and Library indexes them. Storage preflight, insufficient-space handling, interrupted-session recovery, file validation, and broad playback/orientation testing are not complete. |
| Reconnect system | **Partially working** | There are bounded retries with 2-, 5-, and 10-second delays and authentication errors are not retried indefinitely. Exponential backoff with jitter, typed failure mapping, connection timeout handling, and permanent server-rejection classification are incomplete. |
| Adaptive bitrate | **Partially working** | A rolling bitrate window and sustained degradation/recovery thresholds are connected to `setVideoBitrateOnFly`, with pure policy tests. The policy does not yet incorporate measured dropped frames, encoder load, send failures, or a formal resolution/FPS safety envelope. |
| Stream health | **Partially working; some values need stronger provenance** | Bitrate callbacks, battery, thermal state, and network type are available. FPS is currently set from the configured profile when the service enters LIVE rather than proven to be measured, dropped frames are not visibly sourced from RootEncoder, and audio level remains limited. |
| Platform integrations | **UI/configuration boundary only** | YouTube, Twitch, Kick, and Custom RTMP destination slots are present, with encrypted server/key storage and platform hints. The shipped platform adapter is explicitly disconnected and returns backend/scope requirements instead of performing OAuth, metadata, chat, moderation, clips, or event APIs.[3] |
| True multistreaming | **Missing** | The service owns one `GenericStream` and one endpoint. The repository explicitly states that simultaneous delivery is not implemented.[4] |
| Credential security | **Mostly implemented; test coverage should expand** | Stream URLs and keys are stored using Android Keystore AES-GCM, keys are omitted from configuration exports, and credentials are kept out of the platform integration contract. Keystore behavior, corruption, backup behavior, diagnostic redaction, and release-build leakage should receive instrumentation and security tests. |
| Preflight | **Partially working** | Permission, microphone availability, destination presence, and capture readiness checks exist. Storage capacity, battery, thermal, encoder capability, URL/key validity, network quality, and resolution/FPS/bitrate compatibility are not all validated before start. |
| Network handling | **Partially working** | The service reports the current network type during health sampling and reacts to RootEncoder connection callbacks. It does not yet use a dedicated Android `NetworkCallback` to model Wi-Fi/cellular transitions before and during connection. |
| Performance and thermal behavior | **Partially working** | Thermal protection and bitrate reduction exist, and some work is moved off the UI thread. There is no repeatable CPU/GPU/RAM/frame-copy benchmark or Compose recomposition profile in the repository. |
| Tests and release process | **Partially working** | JVM tests cover selected policies, persistence/export behavior, usage estimates, and ViewModel behavior; one instrumentation test file exists. The test suite does not yet cover the full stream state machine, lifecycle cleanup, hardware capture, recording validity, or real platform ingest. |
| Documentation | **Good and unusually honest for an alpha** | README and limitation documents distinguish current generic RTMP capability from planned compositor, OAuth, chat, multistream, and physical-device work. The production prompt should be adopted as a release-gate checklist rather than used to overstate alpha readiness. |

## Highest-risk findings

### 1. Scene metadata is not scene output

This is the largest functional gap. The existing scene model can describe multiple source types, but the current RootEncoder path has one active video source. A scene containing SCREEN and CAMERA does not become a composed screen-plus-camera stream. Image, text, color, position, opacity, and z-order are therefore not yet guaranteed to affect outgoing video. The repository’s compositor plan explicitly states this boundary and proposes an EGL/OpenGL render pass as future work.[1]

This must not be “fixed” with a cosmetic preview overlay. The editor and the encoder must share the same composition graph or the product will show one result while broadcasting another. The correct implementation is a dedicated compositor that owns the scene render graph and presents its composed output to RootEncoder, with careful handling for camera and MediaProjection external textures, portrait/landscape transforms, teardown, and thermal load.

### 2. Alpha17 preview readiness does not yet prove first video frame

Alpha17 fixes the previous logo-only preview and attaches a real SurfaceView. That is a meaningful improvement, but `previewReady` is currently set after `genericStream.startPreview(...)`; it does not confirm that a frame was rendered, that the encoder consumed a frame, or that the frame rate is healthy. The next P0 improvement should expose a frame counter or a verified FPS callback from the active media path, then distinguish **surface attached**, **first frame received**, **encoder producing frames**, and **network connected**.

### 3. The state machine is too compressed for production diagnosis

The current enum has IDLE, PREPARING, LIVE, RECONNECTING, STOPPING, and ERROR. The service uses PREPARING for several different phases: encoder setup, preview waiting, destination connection, and authentication progress. The prompt’s proposed CONNECTING, STOPPED, and FAILED distinctions are justified. A typed state model should also carry the source of failure, retryability, session ID, and whether the service or UI initiated the transition.

A particular production requirement is a connection timeout. The current 30-second timeout covers pending capture/preview preparation, but the transport connection path does not yet have a clearly separate deadline for a server that never succeeds or fails.

### 4. Telemetry must not present configuration as measurement

The prompt is correct to prohibit fake health data. The service currently copies the configured FPS into the session when it publishes LIVE. That is a target, not an observed measurement. Similarly, `droppedFrames` needs a documented source before it is shown as a live diagnostic. Bitrate is the strongest current metric because it comes from RootEncoder’s callback. The next health pass should define provenance for every field and allow “unavailable” rather than substituting a configured value.

### 5. Platform setup is generic RTMP, not official platform integration

The core path can be useful for a creator who supplies a valid ingest URL and stream key. It should not be described as YouTube/Twitch/Kick API integration. The actual platform adapter is a safe disconnected boundary returning `RequiresBackend` or `RequiresScope`; OAuth, account identity, metadata, chat, moderation, clips, scheduling, and events are not shipped capabilities.[3]

The prompt’s requirement to use official APIs is correct. Implementing it will require isolated provider adapters, OAuth/PKCE flows, token storage and refresh, scope review, privacy disclosures, and potentially a backend for client-secret protection, webhooks, relay services, or long-lived subscriptions. None of this should be mixed into the RootEncoder transport layer.

### 6. True multistreaming is a separate architecture, not a UI toggle

The current service owns one endpoint. Sending simultaneously to three destinations would require either three independent output clients/encoders with multiplied resource use or a server-side relay. A relay is the more credible production architecture for larger-scale multistreaming, but it introduces backend cost, authentication, operational reliability, privacy, and legal/compliance considerations. The app must not label multiple configured destinations as simultaneous live delivery unless each destination has independently confirmed the stream.

## Recommended implementation order

| Priority | Workstream | Scope | Complexity | Exit criteria |
|---|---|---|---|---|
| P0-A | Authoritative stream state machine | Separate capture preparation, preview first-frame, connection, live, reconnecting, stopping, stopped, and failed states. Serialize commands and add connection deadlines. | Large | Repeated START → STOP → START works without duplicate encoders, stale LIVE state, or indefinite PREPARING. |
| P0-B | Capture lifecycle hardening | Register MediaProjection stop callbacks, handle surface destruction/recreation, camera disconnects, service recreation, process death, and complete RootEncoder/AudioRecord cleanup. | Large | Physical-device lifecycle matrix passes on representative Android 10–15 devices. |
| P0-C | Truthful diagnostics | Measure actual encoded FPS, frame production, dropped frames where supported, bitrate, reconnects, encoder errors, audio availability, battery, thermal, and network. Replace configured-value fallbacks with unavailable states. | Large | Every health field has a source, timestamp, and documented unavailable behavior. |
| P0-D | Recording reliability | Check storage before recording, handle write failures, verify MP4 structure/duration after stop where practical, recover from interrupted sessions, and test simultaneous stream/record. | Medium/Large | Generated recordings are playable and correctly indexed after normal and interrupted sessions. |
| P0-E | Security and data durability tests | Add Keystore instrumentation tests, corrupted ciphertext tests, export-redaction tests, backup-policy review, diagnostic redaction checks, and scene schema/version migration planning. | Medium | Secrets never appear in logs, exports, fixtures, or crash diagnostics; corrupted data fails safely without silent destructive reset. |
| P1-A | Real GPU compositor | Build a dedicated EGL/OpenGL scene graph for screen, camera, image, text, color/background, opacity, z-order, aspect ratio, and portrait/landscape output. | Very large | The encoded output demonstrably matches the Studio preview for screen-plus-camera, image, text, and background scenes at stable 30 FPS. |
| P1-B | Editor/output parity | Add touch drag, resize, image selection, background controls, layer ordering, and composition preview backed by the same scene model consumed by the compositor. | Large | Editing a source changes the actual outgoing composition, not only persisted metadata. |
| P1-C | Reconnect and network model | Add typed failure mapping, exponential backoff with jitter, Android network callbacks, retryability policy, and explicit permanent-auth/configuration failures. | Medium | Network loss/recovery and invalid credentials produce distinct, bounded, actionable states. |
| P1-D | Audio measurement path | Introduce a single-owner PCM pipeline or supported RootEncoder tap for peak/RMS measurement, with throttling and no second competing recorder. Add routing and channel behavior where Android permits. | Large | Audio level is measured from the same frames sent to the encoder and remains stable on physical devices. |
| P1-E | Encoder capability preflight | Detect supported resolution/FPS/codec combinations, enforce safe bitrate bounds, and expose keyframe behavior only where the engine supports it. | Medium | Unsupported profiles fail before a broadcast starts with a specific explanation. |
| P2-A | Official platform adapters | Implement isolated YouTube, Twitch, and Kick providers only after API/auth requirements are confirmed. Start with account identity and metadata, then chat/events/moderation by scoped milestones. | Large/External | Each capability has real OAuth/API evidence, scope handling, token lifecycle, privacy copy, and provider-specific tests. |
| P2-B | Backend-dependent services | Decide which OAuth, webhook, chat relay, analytics, or multistream functions require a backend. Define data retention, secrets, abuse controls, cost, and failure behavior before implementation. | Very large/External | Architecture and threat model approved; no fake local adapter responses remain. |
| P3 | Advanced creator features | Guests, advanced transitions, clips, analytics, scheduling, moderation automation, and other secondary features. | Large | Only begin after the single-destination core and compositor pass the physical reliability gate. |

## What should not be done

The project should not add a fake scene compositor by drawing a Compose overlay only in the app UI. It should not add a second `AudioRecord` merely to animate a meter while RootEncoder owns the microphone. It should not copy configured FPS into “measured FPS” labels, show zero dropped frames as if they were confirmed, or claim that three platform keys constitute multistreaming. It should not add platform-specific scraping or private endpoints. These shortcuts would make the product look more complete while reducing trustworthiness.

The existing RootEncoder limitation is architectural, not a missing button. RootEncoder’s documented StreamBase path supports attaching a preview and changing the active video source, but the current `GenericStream` abstraction does not itself provide the concurrent multi-layer compositor required by the prompt.[5]

## Required physical-device validation

The prompt is correct that build success is not production evidence. The following cannot be conclusively verified from this sandbox alone:

| Area | Required evidence |
|---|---|
| Capture | Physical Android 10–15 devices using camera and MediaProjection, including permission denial, rotation, backgrounding, and projection stop. |
| Ingest | Real disposable or rotatable YouTube, Twitch, Kick, and custom RTMP/RTMPS destinations, preferably with private/unlisted test broadcasts. |
| Network | Wi-Fi, cellular, Wi-Fi loss, cellular transition, airplane mode, weak network, recovery, and server rejection. |
| Performance | 720p30, 1080p30, and supported 1080p60 with temperature, battery, CPU, RAM, FPS, dropped frames, and reconnect observations. |
| Reliability | 15-, 30-, 60-, and 120-minute sessions, repeated START → STOP → START cycles, recording during streaming, and process/service interruption. |
| Composition | Screen-plus-camera, text, image, opacity, z-order, portrait, landscape, camera mirroring, and editor/output parity after the compositor is implemented. |

## Final verdict

The prompt should be accepted as the **production-hardening master brief**, with one clarification: it describes several distinct products and infrastructure projects, not one safe alpha-sized patch. The current repository is a reasonable foundation for P0 reliability work and a future compositor, but it should currently be labeled **alpha / generic RTMP streaming with single-source capture and planned scene composition**, not production-ready multi-platform studio software.

The best next engineering milestone is not another UI pass. It is a reliability release that makes frame production, stream state, failure causes, cleanup, recording validity, and physical-device behavior measurable and trustworthy. Once that passes, the GPU compositor becomes the highest-value product feature because it is the bridge between the existing scene editor and the actual creator promise.

## References

[1]: docs/PIP_COMPOSITOR_PLAN.md "Unictoos PiP compositor plan and RootEncoder limitation"
[2]: docs/TELEMETRY_LIMITATIONS.md "Unictoos telemetry limitations"
[3]: app/src/main/java/com/unictoai/unictoos/integrations/PlatformIntegration.kt "Unictoos platform integration boundary"
[4]: docs/SCENE_TRANSITIONS_AND_DESTINATIONS.md "Unictoos scene transition and destination boundaries"
[5]: https://github.com/pedroSG94/RootEncoder/wiki/StreamBase "RootEncoder StreamBase documentation"
