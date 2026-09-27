# Unictoos v0.5.7

Unictoos v0.5.7 delivers the guided go-live flow, full scene/source management, and creator-quality polish. Pre-release for on-device testing.

## Go-live flow (fixed dead end)

- **Guided setup instead of dead button.** Tapping Go Live with a missing destination or no capture source now opens a "Finish setup to go live" dialog naming each blocker, with action buttons (Add destination → Settings, Open scenes → Scenes). The button never sits dead.
- **Default scene has a capture source.** Fresh installs get a "Starting Soon" scene with an enabled screen-capture base layer, so there's always a valid capture mode.
- **Smoother motion.** Go Live button has springy press-scale feedback; readiness warnings fade/expand; setup dialog scales in; backdrop has a slow-drifting accent aura.

## Scene & source management

- **Scenes:** rename, duplicate (with source/group ID remapping), delete (refuses to delete the last scene, auto-selects another).
- **Sources:** rename, delete (cleans up group references, reindexes z-order).
- **Idle scene switcher.** Scene chips above the Studio preview while the stream is inactive — compose before going live. (Live scene switching is not claimed; it needs a service-side hot-update path.)
- **Text overlay templates.** One-tap Title, Lower Third, and Social Handle overlays with sensible positioning.

## Creator experience

- **Post-stream recap.** YouTube-style summary dialog after a broadcast ends (duration, bitrate, FPS, dropped frames).
- **Home quick-start.** "Add your first destination" card appears when no destination is configured.

## Evidence boundary

- CI green: lint, unit tests, smoke suite (178/179; only the expected no-local-APK check fails locally), debug APK assembles.
- **Not validated:** real ingest (YouTube/Twitch/Kick/SRT), PiP on hardware, 60-min endurance, thermal/battery, Android 15 OEM background limits, MediaProjection edge cases. Do not claim streaming is fully working until a physical-device test completes.
