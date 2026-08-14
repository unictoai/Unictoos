# Unictoos complete feature matrix and release roadmap — 2026

## Product direction

Unictoos should become a **mobile creator operating system**: a dependable Android capture and streaming core surrounded by scene composition, device diagnostics, creator workflow tools, platform integrations, community controls, and optional professional transport adapters. The feature set below covers the capabilities found in the competitor audit while separating features that can be implemented locally from features that require OAuth, backend services, relay infrastructure, platform approval, or device-specific validation.

> “Add all features” means the complete capability roadmap is implemented through coherent milestones. It does not mean exposing unstable placeholder buttons or silently claiming support for capabilities that require a backend, platform scope, or physical-device testing.

## Status vocabulary

| Status | Meaning |
|---|---|
| **Shipped** | Present and validated in the current alpha07 codebase. |
| **Foundation** | Model or UI boundary exists, but the end-to-end capability is not connected. |
| **Next** | Highest-value implementation slice for the next release family. |
| **Planned** | Valuable, but dependent on earlier foundations or external services. |
| **Research** | Requires protocol, licensing, platform, or hardware investigation before implementation. |

## Complete capability matrix

| Domain | Capability | Current status | Priority | Target milestone | Primary dependency | Acceptance outcome |
|---|---|---:|---:|---|---|---|
| Capture | Screen capture | Shipped | P0 | Core | MediaProjection | User grants consent and receives a stable screen source. |
| Capture | Camera-only capture | Shipped | P0 | Core | Camera2Source, camera permission | Camera-only stream starts and stops without a screen permission. |
| Capture | Microphone capture and availability check | Shipped | P0 | Core | AudioRecord, microphone permission | Clear permission and unavailable-input errors; no crash. |
| Capture | Front/back camera switch | Planned | P1 | Core Plus | Camera2 capability model | Switch camera safely during preview or between sessions. |
| Capture | Camera zoom, focus, exposure, white balance, flash | Planned | P1 | Field Kit | Camera2 controls | Controls are capability-gated and tested on supported devices. |
| Capture | Audio-only mode | Planned | P1 | Field Kit | Service lifecycle, NoVideoSource | Audio-only background stream is clearly labeled and controllable. |
| Capture | Background streaming | Planned | P1 | Field Kit | Foreground service and notification UX | Stream continues when app loses focus under an explicit user setting. |
| Capture | External USB/UVC camera and audio | Research | P2 | Pro Input | UVC/USB host compatibility | Supported devices are detected and users see capability warnings. |
| Composition | Source visibility toggles | Shipped | P0 | Core | Scene model | Enabled sources are represented in the selected scene. |
| Composition | Text, image, logo, color sources | Foundation | P0 | Creator Studio | Scene payloads and renderer | Sources render in the outgoing composition with persisted content. |
| Composition | Layer bounds and drag positioning | Planned | P0 | Creator Studio | GL compositor | User drags layers inside portrait/landscape safe areas. |
| Composition | Layer z-order and opacity | Planned | P0 | Creator Studio | GL compositor | Layer ordering and opacity survive app restart. |
| Composition | Camera picture-in-picture | Planned | P0 | Creator Studio | Concurrent camera/screen compositor | Camera appears above screen content with a draggable frame. |
| Composition | Scene templates | Planned | P1 | Creator Studio | Template storage | Starting Soon, Camera, Gameplay, BRB, and Custom templates work offline. |
| Composition | Scene transitions | Planned | P1 | Creator Studio | Renderer transition layer | Crossfade, cut, and slide transitions work without blocking capture. |
| Composition | Safe-area and platform framing guides | Planned | P1 | Creator Studio | Layout editor | Portrait/landscape and platform-safe overlays are visible only in editor. |
| Composition | Browser/web/HTML widgets | Research | P1 | Creator Studio | Sandboxed WebView or remote renderer | Explicit, privacy-reviewed web source with no hidden provider data. |
| Composition | GPS, timestamp, and metadata overlays | Planned | P2 | Growth | Location permission, format templates | Creator can opt in and see exact data before stream. |
| Composition | Chat overlay themes | Planned | P1 | Engagement | OAuth/backend chat feed | Only selected chat appears; ads never enter implicitly. |
| Audio | Mute/unmute microphone | Shipped | P0 | Core | MicrophoneSource | Toggle updates stream state and UI immediately. |
| Audio | Input gain control | Planned | P1 | Audio Pro | Audio engine controls | Gain is adjustable with safe bounds and a reset action. |
| Audio | Audio mixer and source meters | Planned | P1 | Audio Pro | Audio routing and metrics | Creator sees microphone/source levels without excessive polling. |
| Audio | Audio ducking | Planned | P2 | Audio Pro | Mixer | Music lowers when speech is detected, with manual override. |
| Audio | Bluetooth audio and external microphone | Planned | P1 | Field Kit | Android audio device routing | Selected input is shown clearly and tested for route changes. |
| Recording | Local MP4 recording | Shipped | P0 | Core | RootEncoder record controller | Recording can run with streaming or as local-only practice. |
| Recording | Recording index and file metadata | Shipped | P0 | Core Plus | App-private storage | Recordings are listed with duration, size, and date. |
| Recording | Playback | Next | P0 | Core Plus | Android media playback | User can play a recording in-app. |
| Recording | Rename, delete, share/export | Next | P0 | Core Plus | MediaStore/FileProvider | File operations are explicit, safe, and recoverable. |
| Recording | Split files and screenshots | Planned | P1 | Growth | Record controller and MediaStore | Long sessions can split and save still images. |
| Recording | Post-stream recap | Planned | P1 | Growth | Session health and platform adapter | Summary shows duration, bitrate, drops, recording, and destination. |
| Reliability | Bounded reconnect | Shipped | P0 | Core | RootEncoder callbacks | Maximum attempts and backoff are visible and deterministic. |
| Reliability | Pause and standby | Planned | P0 | Core Plus | Encoder state and scene state | Pause keeps the destination session alive; standby starts paused. |
| Reliability | Stream health center | Next | P0 | Core Plus | Bounded metrics history | Bitrate, FPS, dropped frames, audio, network, battery, and heat are visible. |
| Reliability | Network quality and data budget | Next | P0 | Core Plus | ConnectivityManager, byte counters | User sees Wi-Fi/cellular state, estimated data use, and warnings. |
| Reliability | Adaptive bitrate | Planned | P0 | Transport Pro | Encoder reconfiguration | Bitrate responds within tested bounds to sustained network changes. |
| Reliability | Variable FPS and thermal response | Planned | P1 | Transport Pro | Encoder and thermal APIs | App recommends or applies safe degradation with user-visible reasons. |
| Reliability | Wi-Fi/cellular handoff strategy | Research | P1 | Transport Pro | Transport reconnect and device testing | Handoffs produce an explicit recovery state rather than silent failure. |
| Reliability | Battery, heat, and sunlight warnings | Next | P0 | Core Plus | BatteryManager, thermal APIs | Warnings are actionable and never block a creator without explanation. |
| Reliability | Practice mode | Next | P0 | Core Plus | Local capture/recording | Rehearsal cannot publish to a platform and is visibly marked local-only. |
| Platform | YouTube destination preset | Shipped | P0 | Core | RTMPS and secure key storage | Key can be stored and endpoint validated. |
| Platform | Twitch destination preset | Shipped | P0 | Core | RTMPS and secure key storage | Key can be stored and endpoint validated. |
| Platform | Kick destination preset | Shipped | P0 | Core | Kick RTMPS guidance | Correct server/key guidance and dashboard deep link. |
| Platform | Custom RTMP/RTMPS | Shipped | P0 | Core | Endpoint validation | User can configure an arbitrary secure endpoint. |
| Platform | Metadata: title/category/visibility/audience | Foundation | P1 | Platform Workflow | Provider OAuth/API or dashboard links | App either sets metadata via API or clearly routes to the platform dashboard. |
| Platform | Scheduling | Planned | P1 | Platform Workflow | Provider API | Scheduled broadcasts can be created, edited, and launched where supported. |
| Platform | YouTube practice/schedule/recap/archive | Planned | P1 | Platform Workflow | YouTube OAuth/API | Platform-specific workflow is shown only when eligible and authorized. |
| Platform | Twitch clips and stream markers | Planned | P1 | Growth | Twitch OAuth scopes | User can create marker/clip actions with confirmation. |
| Platform | Kick title/category workflow | Planned | P1 | Platform Workflow | Kick dashboard deep links | UI explains dashboard limitation instead of pretending API support. |
| Platform | Platform dashboard deep links | Next | P0 | Platform Workflow | Android intents | YouTube, Twitch, and Kick setup links open from context. |
| Engagement | OAuth account connections | Foundation | P1 | Engagement Core | PKCE/backend boundary | Tokens are scoped, encrypted, revocable, and never mixed with stream keys. |
| Engagement | Unified read-only chat inbox | Next | P1 | Engagement Core | OAuth/backend adapters | Messages from connected platforms are normalized and labeled by source. |
| Engagement | Quick replies and pinned chat | Planned | P1 | Engagement Core | Chat send scopes | Explicit confirmation and per-platform capability messaging. |
| Engagement | Follows/subs/cheers/raids/donations alerts | Planned | P1 | Engagement Core | EventSub/webhooks/provider events | Alerts are visible in Engage and optionally as explicit scene sources. |
| Engagement | Moderation: delete, timeout, ban, slow mode | Planned | P1 | Moderation | Explicit OAuth scopes | Every action shows platform, target, scope, and confirmation. |
| Engagement | AutoMod and blocked terms | Planned | P1 | Moderation | Twitch/YouTube APIs | Review queue and settings are separated from outgoing media. |
| Engagement | Polls, predictions, goals, hype train | Planned | P2 | Engagement Plus | Provider APIs | Platform capability differences are visible. |
| Engagement | Guest/collaboration sessions | Research | P2 | Collaboration | Provider APIs/backend/media composition | Guest permissions, invitations, and layout state are auditable. |
| Growth | Clips and highlight extraction | Planned | P1 | Growth | Local recording or provider clip APIs | Creator marks moments during stream and exports/share them. |
| Growth | Thumbnails and post-stream share | Planned | P1 | Growth | MediaStore and optional provider APIs | User can preview, save, and share without exposing keys. |
| Growth | Creator analytics and trends | Planned | P1 | Growth | Session history and provider APIs | Local metrics are available offline; provider metrics are opt-in. |
| Growth | Localization | Planned | P2 | Foundation | Android resources | Core workflows support translated strings without hard-coded UI text. |
| Pro transport | Multistream to multiple destinations | Research | P1 | Pro Transport | Gateway/relay or multiple encoders | No implementation until bandwidth, cost, and failure semantics are defined. |
| Pro transport | SRT/RIST/WebRTC/WHIP/RTSP | Research | P2 | Pro Transport | Native libraries and protocol test rigs | Protocol selection is capability-gated and independently testable. |
| Pro transport | NDI/remote control/REST | Research | P2 | Pro Operations | NDI SDK, control service | Remote control never exposes credentials or raw capture without consent. |
| Revenue | App-only sponsor space | Shipped | P1 | Core | Provider-neutral policy | Never appears in live frames or local recordings. |
| Revenue | Creator-controlled sponsor graphics | Planned | P1 | Growth | Explicit scene source | Creator adds/removes sponsor graphics; no hidden injection. |
| Revenue | Tips, sponsorship discovery, templates | Research | P2 | Revenue | Provider adapters and legal review | Optional integrations are transparent and modular. |

## Release sequence

| Release family | Primary outcome | Features that must ship together | Explicitly deferred |
|---|---|---|---|
| **alpha08 — Creator Core Plus** | Make Unictoos dependable before adding cosmetic breadth. | Recording playback/share/rename/delete, practice mode, pause/standby model, stream-health center, dropped-frame and data counters, battery/thermal warnings, platform dashboard links, persisted session history. | OAuth, multistream, new protocols, guest streaming. |
| **alpha09 — Creator Studio** | Turn scenes into a real mobile composition tool. | Persisted layer geometry, z-order, opacity, text/image/logo/color payloads, camera PiP, safe-area guides, templates, cut/crossfade transitions, capability-gated camera controls. | Web widgets, USB/UVC, remote control. |
| **alpha10 — Audio and Field Kit** | Improve mobile production quality and resilience. | Input gain, audio meters, Bluetooth routing, background/audio-only mode, camera switch, zoom/focus/flash, orientation handling, thermal degradation guidance, screenshot/split recording. | SRT/RIST/WebRTC/NDI. |
| **alpha11 — Engagement Core** | Give creators a safe community workspace. | OAuth/PKCE boundary, connected account manager, normalized read-only chat, event cards, alert preferences, explicit chat overlay source, token revocation. | Automated moderation and provider actions until scopes are audited. |
| **alpha12 — Moderation and Growth** | Make Engage actionable and make streams reusable. | Quick replies, pinned chat, moderation queue, blocked terms/AutoMod surfaces, clips, markers, thumbnails, post-stream recap, local analytics, share/export. | Guest streaming and multistream gateway. |
| **alpha13 — Platform Workflow** | Reduce platform switching. | Metadata adapters, scheduling, YouTube practice/schedule/recap links, Twitch schedule/clip/marker actions, Kick dashboard workflow, archives and deep links. | Provider features without stable APIs. |
| **alpha14+ — Pro Transport and Collaboration** | Extend Unictoos beyond standard RTMP. | Adaptive bitrate ladder, handoff strategy, SRT/RIST/WebRTC/WHIP/RTSP adapters, multistream gateway, guests, USB/UVC, remote control, NDI where licensing permits. | Anything without a testable device/protocol matrix. |
| **Revenue track** | Support sustainability without compromising trust. | App-only sponsor slots, creator-controlled graphics, optional tips/sponsorship adapters, premium templates while retaining the free open-source core. | Hidden ads, watermarks, stream-key monetization, or provider data leakage. |

## Definition of done for every milestone

Every feature must have a domain model, a user-visible state for unavailable/error/loading cases, a privacy boundary, a battery/performance budget, a static smoke check, unit tests for non-UI logic, and a physical-device test row when it touches capture, camera, audio, background execution, encoder behavior, or platform authentication. Features that require OAuth or backend services must remain clearly labeled as architecture-ready until the integration is actually connected.

## References

[1]: https://streamlabs.com/content-hub/tools/mobile-app "Streamlabs Mobile official product page"

[2]: https://guide.prismlive.com/ "PRISM Live Studio official documentation"

[3]: https://softvelum.com/larix/ "Larix Broadcaster official product page"

[4]: https://softvelum.com/larix/android/ "Larix Broadcaster for Android official documentation"

[5]: https://support.google.com/youtube/answer/9228390?hl=en&co=GENIE.Platform%3DiOS "YouTube Help: Create a live stream on mobile"

[6]: https://dev.twitch.tv/docs/api/reference "Twitch API Reference"

[7]: https://dev.twitch.tv/docs/chat/moderation/ "Twitch Chat Moderation API"

[8]: https://help.kick.com/en/articles/7135289-streaming-on-kick-from-your-mobile-phone "Kick Help Center: Streaming on KICK from your mobile phone"

[9]: https://streamelements.com/ "StreamElements official platform"

[10]: https://restream.io/chat "Restream Chat official page"
