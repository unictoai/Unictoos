# Unictoos v0.1.0-alpha09

## Scene Layer Foundation

Alpha09 begins the Creator Studio composition milestone. Scene sources now carry persisted layer order and opacity metadata, and the Scenes editor exposes compact up/down controls plus an opacity slider for each source. Source mutations are saved locally through the version-tolerant SceneStore, so a creator’s layout intent survives app restarts.

The editor now makes the composition model explicit: sources have a stable identity, type, visibility, z-index, and opacity. This prepares the app for the tested GL compositor that will render screen, camera, image, text, color, and future web/widget sources together.

## Important capability boundary

The current RootEncoder capture path still selects a compatible capture source for streaming. Alpha09 does not silently claim that camera-plus-screen picture-in-picture is already rendered in the outgoing stream. The next compositor slice must be validated on physical devices before the UI advertises the capability as production-ready.

Alpha08 Creator Core Plus remains included: local Practice mode, health telemetry, recording playback/share/rename/delete, secure FileProvider sharing, scene persistence, platform dashboard links, the Engage workspace, secure credentials, bounded reconnect, and crash-hardening safeguards.

## Validation target

| Check | Result |
|---|---|
| Kotlin/Compose compilation | Passed during development after layer-editor changes |
| Android lint | Passed |
| JVM unit tests | Passed |
| Debug and release APK builds | Passed |
| Static smoke suite | **41/41 passed** |
| Physical-device validation | Required for opacity/order persistence and future compositor output |

## Next

The next Creator Studio slice will add source payload editing, safe-area guides, template duplication, scene transitions, and a hardware-tested camera picture-in-picture compositor. Engagement Core will then move from boundary UI to real OAuth/PKCE adapters, normalized read-only chat, and provider event subscriptions.
