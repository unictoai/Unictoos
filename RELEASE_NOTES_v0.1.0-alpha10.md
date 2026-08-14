# Unictoos v0.1.0-alpha10

## Engagement Core boundary

Alpha10 adds the provider-neutral integration boundary for future YouTube, Twitch, Kick, and custom platform adapters. The new contract covers connected-account state, normalized chat messages, creator events, metadata updates, chat sends, moderation requests, clips, and stream markers. Each integration advertises capabilities and returns explicit results for missing scopes, missing backend configuration, or provider failures.

The boundary intentionally excludes stream keys. OAuth/PKCE or a documented backend relay is required for chat, events, moderation, metadata, scheduling, clips, and markers. A disconnected registry is provided so the app remains launchable and honest while real provider applications, callback URLs, token protection, scopes, and webhook infrastructure are configured.

The Engage screen now presents a unified-inbox workflow, platform filters, event and alert categories, and a moderation desk. It does not display fake messages or pretend that a generic RTMP key grants provider APIs. Settings continues to provide platform dashboard links for workflows that cannot be safely controlled through generic RTMP.

Alpha09 scene-layer foundation and all previous Creator Core Plus features remain included: persisted source z-order and opacity, local Practice mode, health telemetry, recording playback/share/rename/delete, FileProvider boundaries, secure credentials, crash hardening, bounded reconnect, and the Executive Broadcast mobile UI.

## Validation

| Check | Result |
|---|---|
| Kotlin/Compose compilation | Passed |
| Static feature smoke suite | **42/42 passed** |
| Physical-device validation | Still required for capture, provider login/callbacks, long sessions, thermals, and production encoder behavior |

## Next

The next engagement milestone requires real provider adapters. It should start with OAuth/PKCE account connection and read-only normalized chat, then add event subscriptions, alert preferences, quick replies, and moderation actions only after each provider scope is reviewed. The media path must remain independent from all integration payloads.
