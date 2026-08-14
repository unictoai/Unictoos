# Mobile livestreaming benchmark notes

## Streamlabs Mobile

Source: [Streamlabs Mobile official product page](https://streamlabs.com/mobile-app)

The page documents mobile screen plus camera streaming, IRL streaming, support for Twitch, Kick, YouTube, TikTok, and Facebook, scenes, alerts, chat, event lists, overlays, widgets, text/images/music/video/web sources, VTuber modes, themes, brand colors, fonts, banners, tickers, and recording. It also highlights multistreaming, network improvement through Network Boost, device handoff through Stream Shift, and Disconnect Protection. The product separates free/basic capabilities from paid Ultra features, with advanced multistreaming, premium overlays, stronger reliability tools, and priority support in paid tiers.

Product implications for Unictoos: the minimum competitive feature set is not only “start RTMP.” Creators expect screen/camera composition, scenes, overlays, chat/event awareness, recording, stream health, recovery, and a clear separation between free core broadcasting and optional premium/monetization layers.

## PRISM Live Studio

Source: [PRISM Live Studio official product page](https://prismlive.com/en_us/)

The page positions PRISM as an all-in-one live streaming and recording tool. It highlights simultaneous streaming to live platforms, real-time screen sharing, avatar/AvatarLive modes, high-definition live streaming, richer sources, and colorful effects including masks, background filters, touch/reaction filters, and emotional filters. It also promotes a mobile app alongside desktop tools and an optional PRISM Plus tier.

Product implications for Unictoos: mobile creators value camera effects, avatars, reactions, source richness, HD presets, and a product identity that feels creative rather than purely technical. These should be staged after the reliable capture and destination path.

## Larix Broadcaster

Source: [Larix Broadcaster official page](https://softvelum.com/larix/)

Larix documents a professional transport-first feature set: RTMP, SRT, NDI, WebRTC, RTSP, Zixi, and RIST; portrait/landscape orientation; front/back camera switching; recording and screenshots; stream pause with pre-stream standby; audio-only capture; multiple simultaneous connections; adaptive bitrate behavior; variable FPS; overlays; GPS and timestamp data; remote management through Larix Tuner; and external camera/USB workflows. The page also distinguishes premium protocol and professional features.

Product implications for Unictoos: a powerful roadmap needs a transport abstraction rather than hard-coding only RTMP, an explicit standby/pause state, adaptive bitrate, camera switching, screenshots, optional audio-only mode, multiple destinations, and a diagnostics surface. SRT/RIST/WebRTC/NDI should be later engine adapters, not mixed into the first release path.

## YouTube mobile live

Source: [YouTube Help: Create a live stream on mobile](https://support.google.com/youtube/answer/9228390)

YouTube documents mobile live requirements such as channel eligibility, verification, enabling live streaming, and a waiting period for the first stream. Its mobile workflow includes creating or scheduling a stream, visibility and audience settings, screen casting, live-chat options, practice mode, a recap with stream statistics, an archive, and—in supported cases—a highlight/Short generated from the live stream. YouTube also documents vertical mobile live playables and chat moderation.

Product implications for Unictoos: platform setup should explain eligibility without pretending the app can bypass platform rules; the roadmap should include private practice mode, stream metadata presets, schedule/visibility fields where API access is available, a post-stream recap, archive links, chat moderation, and a vertical-first workflow.

## Twitch creator controls

Sources: [Twitch API reference](https://dev.twitch.tv/docs/api/reference/), [Twitch chat and moderation documentation](https://dev.twitch.tv/docs/chat/send-receive-messages/), [Twitch moderation documentation](https://dev.twitch.tv/docs/chat/moderation/)

Twitch’s official developer surface covers channel information, stream metadata, scheduling, clips and stream markers, chat messages, announcements, emotes and badges, chat settings, polls, predictions, raids, goals, subscriptions, guest/guest-star sessions, ads, and extensive moderation including AutoMod, blocked terms, timeouts, bans, shield mode, and held-message review. Chat events are delivered through EventSub or related transports and require explicit OAuth scopes and backend/event handling.

Product implications for Unictoos: a serious creator app needs a read-only chat/engagement layer first, then platform-specific OAuth integrations for posting, moderation, clips, markers, polls, goals, and guest workflows. Stream keys alone are not enough for these features. Unictoos should not put platform OAuth secrets in the Android client without a secure backend or a carefully limited PKCE flow.

## Kick mobile setup

Source: [Kick Help Center: Streaming on KICK from your mobile phone](https://help.kick.com/en/articles/7135289-streaming-on-kick-from-your-mobile-phone)

Kick documents mobile camera, screen sharing, and IRL streaming. Its Streamlabs mobile instructions require the creator to retrieve the stream key from the Kick dashboard, use the specific RTMPS URL `rtmps://fa723fc1b171.global-contribute.live-video.net:443/app`, and set stream title and category on the Kick website because those fields are not available inside the mobile custom-RTMP flow. The help page also emphasizes stabilization, audio, lighting, power, heat, network handoffs, data usage, privacy, and personal safety.

Product implications for Unictoos: Kick should have a first-party preset with the current documented ingest URL, a direct “Open Kick dashboard” action, title/category setup guidance, and mobile safety/thermal/network checklists. The preset must be maintained when Kick changes its endpoint.

## StreamElements and Restream patterns

Sources: [StreamElements official platform](https://streamelements.com/), [Restream Chat official page](https://restream.io/chat)

StreamElements emphasizes free alerts and overlays, a chatbot, tipping, leaderboard/community recognition, sponsorships, and a companion management product. Its current public positioning is broader creator infrastructure than a native capture engine, and its discontinued StreamElements Live mobile product is a warning not to assume a mobile companion app will remain supported without clear ownership.

Restream’s official Chat product consolidates messages from multiple streaming platforms, supports replying from one screen, cross-platform chat relay, chat overlays, themes, filtering, rude-chatter hiding, and notifications/text-to-speech in its desktop app.

Product implications for Unictoos: a unified chat inbox, moderation/filtering, chat overlay themes, alerts, tipping/sponsorship surfaces, and community recognition are valuable differentiators. They require platform APIs/OAuth or a backend relay and should be built as a separate integration layer rather than coupled to RTMP credentials or the encoder.
