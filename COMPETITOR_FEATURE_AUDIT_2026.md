# Unictoos competitor feature audit — 2026 refresh

## Executive finding

The competitor set does not represent one product category. Streamlabs and PRISM emphasize creator composition, effects, scenes, alerts, and community surfaces. Larix emphasizes transport breadth, adaptive behavior, pause/standby, background modes, overlays, external devices, and remote operations. YouTube, Twitch, and Kick expose platform-specific workflows such as scheduling, practice, metadata, moderation, clips, events, and channel controls. Unictoos should therefore expand as a layered mobile creator operating system rather than a single large settings page.

## Current alpha07 coverage

Unictoos already provides native Android capture, screen and camera paths, microphone checks, RTMP/RTMPS transport through RootEncoder, secure per-platform stream-key storage, bounded reconnect, local MP4 recording, scene/source toggles, preflight checks, a six-tab mobile UI, Engage architecture, app-only sponsor policy, and stability guards. The scene model currently supports source type and visibility but not persisted layer geometry, z-order, opacity, or compositor output. The streaming service exposes elapsed time, bitrate, FPS, reconnect state, microphone mute, and recording state, but it does not yet provide a bounded health-history model, dropped-frame sampling, thermal warnings, adaptive bitrate, or network handoff strategy.

## Confirmed competitor capabilities and implications

| Capability area | Confirmed capability | Unictoos implication | Risk / boundary |
|---|---|---|---|
| Creator composition | Streamlabs documents mobile scenes, widgets, overlay themes, custom images, and disconnect protection. PRISM documents streaming, recording, customization, and mobile/desktop workflows. [1] [2] | Build a real scene compositor with templates, text, image/logo, color, web/widget boundaries, safe areas, and camera PiP. | Must be tested on hardware; do not claim compositor support before output validation. |
| Transport and recovery | Larix documents RTMP/RTMPS, SRT, NDI, WebRTC/WHIP, RIST, RTSP, multiple outputs, pause/standby, adaptive bitrate, variable FPS, audio-only mode, and background streaming. [3] [4] | Treat RTMP/RTMPS reliability as the free core; add health center, pause/standby, background/audio-only modes, and protocol adapters in separate milestones. | New protocols and bonding require native libraries, licensing review, server testing, and a device matrix. |
| Orientation and camera | Larix documents portrait/landscape modes, live rotation, front/back switching, multi-camera/PiP on supported hardware, camera controls, zoom, flash, USB/UVC, and Bluetooth audio. [4] | Add device-aware camera controls, orientation policy, camera switching, pinch-to-zoom, flash, external audio/camera discovery, and capability warnings. | Android hardware support varies; feature availability must be capability-gated. |
| YouTube workflow | YouTube mobile supports eligibility checks, scheduling, visibility, audience, screencast, live-chat options, recap/archive, highlights, and a private Live Practice Mode. [5] | Add a YouTube setup checklist, metadata/scheduling adapter, practice mode, post-stream recap, archive/deep links, and highlight workflow where APIs permit. | Eligibility and API access are platform-controlled; provide honest dashboard links when direct control is unavailable. |
| Twitch platform APIs | Twitch exposes chat, announcements, emotes, badges, chat settings, pinned messages, clips, markers, scheduling, goals, EventSub, Guest Star, AutoMod, blocked terms, bans, shield mode, and moderation actions. [6] [7] | Build OAuth-scoped adapters, read-only unified inbox first, then quick replies, alerts, moderation, markers, clips, goals, and guest workflows. | Requires user OAuth, explicit scopes, refresh-token handling, backend/webhook strategy, and audit logs. |
| Kick workflow | Kick’s current mobile guide documents front/back camera, screen sharing, IRL streaming, custom RTMP setup, dashboard stream-key access, dashboard-managed title/category, and mobile battery/heat/network concerns. [8] | Add Kick-specific setup guidance, dashboard deep links, title/category limitation messaging, heat/battery/network warnings, and a simple RTMPS preset. | Do not pretend generic RTMP fields provide Kick metadata control. |
| Community and revenue | Streamlabs and StreamElements expose alerts, tips, sponsorship, and community features; Restream emphasizes cross-platform chat. [1] [9] [10] | Keep provider integrations modular; add app-only sponsor slots, explicit creator overlays, tips/alerts adapters, and unified chat. | Providers must never receive stream keys, capture frames, microphone samples, or OAuth refresh tokens beyond the required integration boundary. |

## Gaps to close

The highest-value gaps are a real camera-plus-screen compositor, persisted scene layer editing, recording library operations, stream health and thermal diagnostics, practice mode, pause/standby, device capability checks, metadata/scheduling adapters, OAuth-backed chat, alert/event cards, moderation boundaries, clips/markers, post-stream recap, and carefully isolated pro transport features.

The phrase “add all features” should be interpreted as implementing the complete roadmap through explicit release milestones. Multistream gateway, network bonding, SRT/RIST/WebRTC/NDI, guests, USB camera support, provider moderation, and monetization require separate validation and integration work; exposing placeholder buttons without working boundaries would reduce trust.

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
