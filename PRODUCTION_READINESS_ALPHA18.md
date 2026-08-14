# Unictoos alpha18 production-readiness report

Alpha18 is a reliability and first-compositor milestone, not a claim of feature parity with desktop broadcasting software. The implementation keeps RootEncoder 2.4.5 and the existing Android architecture unchanged while moving session ownership and diagnostics toward production behavior.

| Area | Alpha18 status | Evidence or boundary |
|---|---|---|
| Stream lifecycle | Implemented | Pure transition rules, duplicate-start gate, explicit CONNECTING and STOPPED states, stale-callback rejection, and regression tests. |
| Capture lifecycle | Implemented | Preview gating, 30-second capture timeout, MediaProjection stop callback, and cleanup on service destruction. |
| Reconnect | Implemented with limits | Failure classification, three-attempt cap, exponential backoff, bounded jitter, and Android network callbacks. |
| Preflight | Implemented | RTMP/RTMPS endpoint checks, profile bounds, storage, network, battery, and critical thermal checks. |
| Health telemetry | Partially measured | FPS comes from RootEncoder’s FPS callback; bitrate comes from encoder callback. RootEncoder still does not expose reliable PCM level or dropped-frame counters in this integration, so the UI shows `—` rather than fabricated values. |
| Local recording | Hardened but constrained | Storage preflight and post-stop MP4 validation are present. Recording follows the active RootEncoder stream profile; independent recording quality is not available. |
| Scene editor | Improved | Source opacity, z-order, text, and normalized X/Y/width/height are persisted and editable with touch sliders. |
| GPU scene output | First milestone implemented | Enabled TEXT sources are applied as RootEncoder GL object filters to preview, stream, and recording output. |
| Multi-source compositor | Not complete | RootEncoder still has one active camera or screen input. Simultaneous screen-plus-camera PiP, image layers, color backgrounds behind video, and transitions remain follow-up work. |
| Platform APIs | Boundary only | YouTube, Twitch, and Kick use generic RTMP/RTMPS destination configuration; official API, chat, moderation, and multi-destination fan-out are not claimed. |
| Credential security | Preserved | Existing Android Keystore AES/GCM credential storage is unchanged. Configuration export always emits `streamKey: null`. |
| Persistence recovery | Improved | Corrupt scene JSON is backed up locally before defaults are restored. |

## Required physical-device acceptance test

Install `app-debug.apk` on the Infinix X6853 running Android 15. Open Studio, select a scene with Screen or Camera and a TEXT source, approve microphone and capture permissions, and wait for a visible preview. Start Practice, confirm that the text overlay appears in the preview and local recording, stop, and verify the library entry. Repeat with a private or unlisted platform stream, then test Android capture revocation, network loss and recovery, manual stop during reconnect, and a second start after STOPPED.

The release is not ready to be called production-stable until these checks pass on physical devices with the exact target capture modes. The sandbox build proves compilation and automated regressions; it cannot prove camera hardware, encoder output, microphone routing, or platform ingest behavior.
