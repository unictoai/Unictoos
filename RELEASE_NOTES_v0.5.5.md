# Unictoos v0.5.5

Unictoos v0.5.5 is a full UI redesign plus a streaming-engine repair release. It is published as a pre-release for on-device testing — install it, go live, and report what breaks.

## UI redesign

- **Console-style studio UI** across Home, Studio, Scenes, Library, Settings, and Onboarding: deep charcoal surfaces, hairline borders, signal red reserved for LIVE/record/destructive actions, cyan for active/informational state.
- **New design system**: `StudioTheme` + `StudioKit` (buttons, cards, text fields, chips, section headers, dividers) used consistently on every screen.
- **Workflow-first Studio screen**: preview → readiness → Go Live → visible status/errors → stop/reconnect, with a live dock (mute, record, marker) and a real telemetry strip (bitrate, FPS, quality tier, dropped frames, elapsed).
- **Restored**: platform dashboard links (YouTube/Twitch/Kick) in Settings, quality-tier telemetry in the live strip.

## Streaming repairs

- **Multistream adapter reworked** (`SingleDestinationMultiStreamAdapter` + pure `SlotAggregatePolicy`):
  - First successfully connected destination wins aggregate LIVE (a slow/bad secondary no longer blocks a healthy one).
  - Failed secondary retries at slot level while a peer stays healthy; a secondary auth failure no longer kills the healthy peer.
  - Deterministic per-slot disconnect + shared encoder teardown; release failures are now observable instead of silent.
- **SRT fixed**: stream key is now sent as a URL-encoded `streamid` query parameter (was silently dropped, so keyed SRT destinations could never connect).
- **FPS forwarding**: the configured quality preset's FPS now reaches the encoder (was always 30).
- **Low-latency fix**: `resizeCache(0)` now applies after each endpoint's transport is known (was always falling back to RTMP slot 0, no-op for SRT-only sessions).
- **Go Live gating**: the button now respects the full readiness policy, not just stream status.

## Evidence boundary

- Package `com.unictoai.unictoos`, versionName `0.5.5`, versionCode `65`.
- CI green: unit tests, lint, 155/155 static feature smoke checks, debug APK build.
- **Not validated**: no physical-device, platform-ingest (YouTube/Twitch/Kick/SRT), endurance, or OEM background testing has been done. Alpha: do not trust an important broadcast to this build.
