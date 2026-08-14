## Unictoos v0.1.0-alpha04

This release completes the current remaining-phase engineering milestone for the Unictoos alpha.

### Streaming and reliability

The foreground media service now has explicit capture readiness guards, a microphone permission and `AudioRecord` availability check, a retained microphone source for mute/unmute control, bounded reconnect attempts, authentication-specific errors, elapsed live-time reporting, notification updates, and cleanup protection so a deliberate Stop action is not replaced by a late disconnect error.

### Destination management

YouTube, Twitch, Kick, and Custom RTMP now use separate encrypted credential slots. Switching platforms loads the correct saved server URL and stream key, and the old single-destination YouTube storage format is migrated when possible. The app provides safe server hints for YouTube and Twitch and asks creators to copy Kick’s current ingest URL from the Kick dashboard rather than relying on a stale endpoint.

### Creator features

Scenes now expose source visibility switches. Studio now includes microphone mute/unmute and local MP4 recording controls. Recordings are written to app-private storage and indexed by the Library screen. Stream state now includes reconnect attempt, recording, microphone mute, and elapsed-session information.

### Advertising policy

The release adds a provider-neutral `AdProvider` contract and a persisted opt-in policy for app-only sponsor space. No ad SDK is enabled in this alpha, no ad is inserted into outgoing stream frames or local recordings, and live broadcasts are never interrupted by the current sponsor slot.

### Build details

- Application ID: `com.unictoai.unictoos`
- Version name: `0.1.0-alpha04`
- Build type: debug alpha
- Minimum Android version: Android 10 / API 29
- Target SDK: 35

### Important testing note

This remains an alpha debug build. Install it on a physical Android device and verify microphone input, screen capture, mute, recording, reconnect behavior, per-platform credential switching, and YouTube/Twitch/Kick ingest with private or unlisted test streams before using it for an important broadcast.
