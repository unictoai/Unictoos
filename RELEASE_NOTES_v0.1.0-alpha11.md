# Unictoos v0.1.0-alpha11

## Growth and creator workflow foundation

Alpha11 adds local creator history and stream-marker workflows. Completed live or practice sessions now produce a bounded local summary containing mode, duration, bitrate, FPS, dropped-frame state, and completion time. The Library surface summarizes completed sessions and marked moments without transmitting telemetry.

Creators can tap **Mark moment for clip** during an active live or practice session. The marker is stored locally with its label and elapsed time. This creates a reliable offline foundation for later platform clip APIs and local highlight extraction, while avoiding the false promise that a generic RTMP connection can create provider clips by itself.

The service owns marker timing and session-summary creation, so the UI does not poll the encoder or invent timing. The data remains outside the outgoing stream and is not sent to providers. Existing recording playback, secure sharing, rename/delete, health history, scene layer editing, Practice mode, dashboard links, and OAuth-ready integration boundaries remain included.

## Validation target

| Check | Result |
|---|---|
| Kotlin/Compose compilation | Passed during development |
| Static feature smoke suite | **44/44 passed** |
| Full lint, unit tests, debug/release builds | Passed |
| Physical-device validation | Required for long sessions, marker timing, recording lifecycle, thermals, and platform workflows |

## Next

The next growth slice can add local thumbnail generation, highlight export around markers, richer session charts, and provider clip/marker adapters after OAuth is connected. Pro transport features such as adaptive bitrate, SRT/RIST/WebRTC/NDI, multistream, guests, and USB/UVC remain separate protocol and device-validation programs.
