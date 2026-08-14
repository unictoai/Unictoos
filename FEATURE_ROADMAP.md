# Unictoos feature roadmap

Unictoos is moving toward a **mobile creator operating system**, not a collection of disconnected streaming buttons. The benchmark is documented in `mobile-streaming-benchmark.md`; this roadmap turns it into staged engineering work.

## Shipped in the current Creator Core Plus slice

The app now has a true camera-only capture path in addition to screen capture, camera permission handling, scene source creation, source visibility controls, a preflight card for network and permission readiness, a dedicated Engage workspace, platform integration boundaries, local recording, and resilient stream lifecycle handling.

## Priority 1: Creator Core Plus

| Feature | Purpose | Architecture |
|---|---|---|
| Scene compositor layers | Position and order screen, camera, image, text, and color sources | Extend the scene model with bounds, z-order, opacity, and persisted source payloads; render through the GL layer |
| Camera picture-in-picture | Show face camera over screen gameplay | Add a camera texture layer and draggable safe-area editor; do not silently claim support until the output is tested on hardware |
| Stream health center | Diagnose bitrate, FPS, dropped frames, audio, network, battery, and heat | Keep metrics in the foreground service and expose a bounded history to Compose |
| Practice mode | Rehearse privately before publishing | Prepare capture and local recording without a network destination; label the session clearly as local-only |
| Recording library | Play, share, rename, and delete MP4 recordings | Add MediaStore/FileProvider sharing and safe cleanup controls |

## Priority 2: Engagement Core

| Feature | Purpose | Boundary |
|---|---|---|
| Unified chat inbox | Read messages from connected platforms in one surface | Requires OAuth and a secure integration layer; stream keys are insufficient |
| Alerts and events | Show follows, subscriptions, cheers, raids, and donations | Use platform event APIs or a backend relay; keep provider payloads out of the encoder |
| Moderation tools | Handle blocked terms, AutoMod, bans, slow mode, shield mode, and message review | Require explicit OAuth scopes and audit-friendly user actions |
| Chat overlay themes | Include creator-selected chat in a scene | Make it an explicit scene source; never inject app ads or hidden content into the overlay |

## Priority 3: Growth and collaboration

Scheduling, stream title/category editing, thumbnails, clips, stream markers, post-stream recap, guest invitations, multi-guest sessions, and platform-specific archive links should be added through provider adapters. YouTube’s practice mode, scheduling, recap, and archive concepts are especially useful for the onboarding and post-stream workflow.

## Priority 4: Pro transport and operations

Adaptive bitrate ladders, network handoff recovery, Wi-Fi/cellular bonding, SRT/RIST/WebRTC/NDI adapters, external USB cameras, remote control, and multi-destination output should be treated as separate media-engine milestones. They require protocol-specific testing, backend or relay capacity where applicable, and a physical-device matrix.

## Revenue and open-source boundary

The free base app should include the capture engine, scenes, recording, diagnostics, and core platform destinations. Optional app-only sponsor space, templates, tips, and sponsorship integrations should remain outside the outgoing frame pipeline. Creator-controlled sponsor graphics are a scene feature and must be visually explicit. No provider should receive stream keys, ingest URLs, OAuth refresh tokens, microphone samples, or raw screen frames unless a later feature explicitly requires that data and the user has consented.
