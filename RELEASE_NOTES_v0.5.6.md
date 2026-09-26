# Unictoos v0.5.6

Unictoos v0.5.6 fixes the broken go-live permission flow and refines the studio color system. Pre-release for on-device testing.

## Bug fixes

- **Go-live permission deadlock fixed.** The readiness policy treated microphone/camera permissions as blocking checks, which disabled the Go Live button on fresh installs — and a disabled button can never trigger the permission request. Permissions are now caution checks: tapping Go Live requests microphone/camera/screen-capture permission through the normal system flow. Only a missing destination, missing network, or no capture source still blocks the button.
- **Go Live gating verified end to end**: Studio → `requestStreamStart` → permission dialog → MediaProjection consent → stream start.

## Color & look refinement

- **New accent system**: the neon cyan is gone, replaced with the `#5B8DEF` blue accent (from the approved v0.2 tokens) for interactive and active states — chips, sliders, switches, text-field focus, selected tabs, dialog actions.
- **Signal red reserved for live**: true broadcast red `#FF3B30`; the Go Live button is now blue when idle and turns solid red ("End Stream") only while live.
- **Primary buttons are blue**, not gray — clear call-to-action hierarchy across Settings, Library, and dialogs.
- **Calmer dark surfaces**: neutral broadcast-charcoal instead of navy-tinted, with matching light-theme values.
- **Preview bezel**: hairline console frame around the preview hero.
- Badge tones now use theme tokens instead of hardcoded colors.

## Evidence boundary

- Package `com.unictoai.unictoos`, versionName `0.5.6`, versionCode `66`.
- CI green: unit tests, lint, static feature smoke checks, debug APK build.
- **Not validated**: no physical-device, platform-ingest (YouTube/Twitch/Kick/SRT), endurance, or OEM background testing has been done. Alpha: do not trust an important broadcast to this build.
