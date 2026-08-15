# Unictoos production feature roadmap

## Product direction

Unictoos is being positioned as a **local-first Android broadcast studio** for serious mobile creators. The product should compete on reliability, clarity, creator ownership, and an excellent mobile workflow rather than copying every desktop control. The roadmap is intentionally organized as atomic capabilities that can be implemented, tested, documented, and shipped independently.

Current competitor patterns show that creators value multistreaming, custom layouts, scenes, alerts, adjustable audio and video settings, destination metadata, recording, analytics, engagement, branding, and integrations. Streamlabs describes mobile multistreaming, scenes, custom layouts, alerts, audio controls, bitrate/FPS controls, and destination setup as a single mobile workflow [1]. StreamYard emphasizes cloud relay, guests, branding, multi-aspect output, multistream destination counts, and high-quality recordings [2]. Mevo highlights multicamera switching, local recording, picture-in-picture, auto-director, audio mixing, and graphics [3]. A 2026 market comparison also identifies user experience, customizability, engagement, integrations, and analytics as core selection criteria [4].

> **Roadmap principle:** reliability gates breadth. No feature is launch-ready until it has a clear owner, a failure state, a device-safe fallback, automated coverage where practical, and a physical-device validation path when it touches capture or streaming.

## Delivery tiers

| Tier | Meaning | Release rule |
|---|---|---|
| P0 | Reliability, privacy, installability, and stream correctness | Must not regress; blocks all other work when broken |
| P1 | Core creator workflow and mobile production quality | Ship in small validated batches |
| P2 | Audience engagement, branding, and creator efficiency | Ship after P0/P1 stability |
| P3 | Integrations, automation, monetization, and team workflows | Ship only with explicit privacy and support boundaries |
| R&D | Experimental or platform-dependent capability | Prototype behind a flag before public release |

## 1,000+ atomic capability inventory

The counts below represent **1,140 atomic roadmap slots**, not a promise that all slots will be implemented in one release. Each slot must become a small issue, a testable change, or an explicit decision to defer. The highest-value P0 and P1 slots are the next implementation targets.

| Pillar | Atomic slots | Immediate priorities |
|---|---:|---|
| Capture and device compatibility | 130 | Camera/screen lifecycle matrix, rotation recovery, thermal fallback, low-memory mode, device diagnostics |
| Encoder and transport | 120 | RTMP/RTMPS correctness, reconnect policy, bounded queues, network transitions, ingest preflight |
| Scenes and composition | 140 | Stable portrait/landscape layouts, source lifecycle, text/image/color layers, safe geometry, transition model |
| Audio and microphones | 95 | Permission recovery, source selection, mute correctness, level meter, headset/Bluetooth behavior, audio preflight |
| Recording and media library | 90 | Finalization safety, file validation, storage policy, thumbnails, share/export, retention controls |
| Destinations and platform metadata | 105 | YouTube/Twitch/Kick profiles, custom RTMP, titles, descriptions, privacy/category fields, destination health |
| Creator workflow | 100 | Onboarding, quick setup, preflight checklist, reusable presets, stream plans, markers, session notes |
| Engagement and moderation | 85 | Chat connectors, event timeline, alerts, polls, Q&A, moderation queues, block/mute actions |
| Branding and graphics | 90 | Logos, lower thirds, title cards, templates, color tokens, brand kits, safe areas, accessibility contrast |
| Analytics and observability | 75 | Session health, diagnostic export, structured logs, stream reports, failure correlation, privacy redaction |
| Accessibility and localization | 45 | TalkBack labels, scalable text, contrast, reduced motion, keyboard/switch support, translations |
| Privacy, security, and trust | 35 | Credential lifecycle, data deletion, export/import warnings, disclosure UI, secure logging, consent boundaries |
| Monetization and growth | 25 | Optional ad slot policy, sponsor-safe UI, creator tips integrations, referral surfaces, experiments |
| Team and automation | 10 | Shared presets, scheduled preparation, webhook boundaries, role permissions, background-task safety |

**Total planned atomic slots: 1,140.** The roadmap deliberately keeps multistreaming, cloud relay, guest workflows, platform APIs, and monetization behind architecture and policy gates. The current app remains local-first and free; no paid dependency is introduced by this roadmap.

## Next production batches

### Batch A — P0 reliability and supportability

The first engineering batch should add an in-app diagnostic export, a structured session event timeline, a device compatibility report, clearer preflight outcomes, and regression tests for app restart, rotation, permission denial, network loss, thermal pressure, storage exhaustion, and encoder failure. These changes improve the ability to solve physical-device issues without asking creators to repeat blind tests.

### Batch B — P1 creator setup

The next creator-facing batch should add destination metadata fields, reusable stream presets, a launch checklist, explicit stream-format preview, input selection, audio test mode, and a session summary. Every setting must be locked or clearly marked while live.

### Batch C — P1 scene production

Scene work should progress from stable single-source layouts to a tested compositor. The required gates are source ownership, bounded textures, safe geometry, deterministic transitions, and device-specific fallback behavior. Camera-plus-screen composition should not be enabled merely because both sources exist in a scene model.

### Batch D — P2 audience workflow

After capture is stable, add chat and event connectors, polls, Q&A, alerts, marker-to-clip workflows, moderation tools, and creator notes. Each integration must be optional, permissioned, and resilient when unavailable.

### Batch E — P2 analytics and brand system

Add reusable brand kits, lower-thirds, title cards, template import/export, stream reports, failure timelines, and performance comparisons. Analytics must remain redacted and local by default unless the user explicitly enables a remote integration.

## Acceptance gates

A release may be called production-ready only when the following are true: the APK installs and launches on supported Android versions; the full automated suite passes; the stream lifecycle is generation-safe; credentials remain protected; the active-device test covers at least a short sustained stream; failures provide a recovery path; settings do not mutate the live session unexpectedly; and every newly introduced feature has an explicit fallback or disabled state.

## References

[1]: https://streamlabs.com/content-hub/post/how-to-multistream-on-mobile "Streamlabs — How to Multistream on Mobile"

[2]: https://streamyard.com/blog/multistreaming-software-for-android "StreamYard — Multistreaming Software for Android"

[3]: https://mevo.com/pages/multi-camera-app "Mevo — Mevo Studio App"

[4]: https://www.switcherstudio.com/blog/best-live-streaming-apps "Switcher Studio — 20 Best Live Streaming Apps in 2026"
