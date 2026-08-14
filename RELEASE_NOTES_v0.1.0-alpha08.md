# Unictoos v0.1.0-alpha08

## Creator Core Plus

Alpha08 expands the alpha07 Executive Broadcast core into a more useful mobile creator workflow. Scenes now persist locally across app restarts, including scene names, portrait/landscape choice, source types, source names, and visibility. The persistence format is versioned and guarded with safe enum fallbacks.

The Studio workspace now includes a local-only **Practice** path. Practice mode uses the same permission and capture preparation flow as a broadcast, records an MP4 on the device, and never sends a destination endpoint. The session is labeled as a rehearsal so creators can test capture, microphone, composition, and recording without risking an accidental public broadcast.

The recording Library now supports playback through Android’s media handlers, secure sharing through FileProvider content URIs, safe rename, and delete. Recordings remain in app-private storage until the creator explicitly shares one. A new FileProvider boundary prevents filesystem paths from being exposed to other apps.

Studio now includes a bounded Health Center. During active live or practice sessions, the foreground service records one low-frequency sample per second into a maximum 120-sample process-local history. The UI summarizes bitrate, FPS, dropped-frame state, network transport label, battery percentage, thermal state, and retained sample count. Health sampling is inactive while idle to avoid unnecessary battery work.

Engage has been reorganized into a mobile-first creator workspace with unified-inbox framing, platform filters, event and alert categories, a moderation desk, and explicit OAuth boundaries. This release does not pretend that stream keys are OAuth credentials: provider chat, alerts, clips, metadata, and moderation remain disconnected until the required provider applications, scopes, token protection, and backend/webhook boundaries are connected. Settings now provides direct dashboard links for platform workflows that are not safely available through generic RTMP fields, including Kick’s title/category limitation.

## Preserved reliability and trust boundaries

The alpha06 crash-hardening baseline and alpha07 visual redesign remain intact. The media service continues to own capture and encoding. Stream keys remain protected by Android Keystore. App-only sponsor space remains outside stream frames and recordings. No engagement or monetization surface receives raw capture, microphone samples, or credentials unless a later feature explicitly requires it and the creator consents.

## Validation

| Check | Result |
|---|---|
| Kotlin/Compose compilation | Passed |
| Android lint | Passed |
| JVM unit tests | Passed |
| Debug APK build | Passed |
| Release APK build | Passed |
| Static feature smoke suite | **41/41 passed** |
| Physical-device validation | Required for capture, practice, recording, FileProvider handlers, thermals, and long-session behavior |

## Next milestone families

The next implementation family is the Creator Studio compositor: persisted layer geometry, z-order, opacity, text/image/logo payloads, camera picture-in-picture, safe-area guides, templates, and tested transitions. Engagement Core will then connect OAuth/PKCE adapters, normalized read-only chat, events, and alert preferences. Pro transport work such as adaptive bitrate, SRT/RIST/WebRTC/NDI, multistream, guests, and USB/UVC remains a separate device and protocol validation program.
