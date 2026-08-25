# Unictoos v0.5.2

Unictoos v0.5.2 applies a cohesive **Black Hole** visual theme across the complete Android app while preserving the existing capture, encoding, destination, and reliability paths.

## Visual changes

The global Compose theme now uses an event-horizon black foundation with deep blue-black surfaces, blue-violet accretion-light actions, photon-cyan live signal accents, warm orange caution states, and high-contrast text. Existing shared palette names were retained as a compatibility layer, so Home, Scenes, Studio, Library, Settings, navigation, cards, buttons, readiness checks, telemetry, error states, and status indicators inherit the new visual language without separate screen-by-screen behavior changes.

The Material 3 dark color scheme now includes coordinated primary, secondary, tertiary, container, outline, error, and text colors. The light scheme also uses a restrained black-hole-inspired violet, cyan, and warm-orange accent system for consistency if light rendering is explicitly selected. Typography, spacing, motion, and streaming logic were not replaced or made heavier.

## Reliability boundary

This is a visual update. No backend, cloud relay, Firebase, Supabase, OAuth redirect server, paid API, desktop OBS server, WHIP, WebRTC, RootEncoder transport, capture source, or permission flow was changed for the theme. The app remains an alpha engineering build and still requires physical-device validation for Go Live, screen/camera capture, PiP, platform ingest, Android 15 OEM behavior, and long-duration stability.

## Validation

| Gate | Result |
|---|---|
| JVM unit tests | Passed after the theme update. |
| Android lint | Passed after the theme update. |
| Debug APK assembly | Passed after the theme update. |
| Static feature smoke gate | Passed with 156 assertions, including the black-hole palette check. |

The package remains `com.unictoai.unictoos`, with version name `0.5.2` and versionCode `62`. Final artifacts are listed below. The debug APK is 87,239,010 bytes, the unsigned release APK is 8,549,243 bytes, and the instrumentation APK is 2,434,801 bytes.

| Artifact | SHA-256 |
|---|---|
| `Unictoos-v0.5.2-debug.apk` | `5b65582f70d6cc72aa77308a8b187daadd846dd0a5c452d97aa355211504d3d2` |
| `Unictoos-v0.5.2-release-unsigned.apk` | `4c9b2a9258d72100737db86467c84db002f502913cfcbee38b6bc5276f200fe3` |
| `Unictoos-v0.5.2-androidTest.apk` | `030b2608bccda2a91058cd798bd7365faf1391840eed32f4082047aa7174b722` |
