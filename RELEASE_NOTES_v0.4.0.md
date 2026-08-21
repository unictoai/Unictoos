# Unictoos v0.4.0

## Professional mobile broadcast upgrade

Unictoos v0.4.0 is the first product-focused upgrade after the graphics and lifecycle remediation work. The release keeps the mobile-first Unictoos interface while adding an operator-grade preflight workflow, stronger credential privacy, and a documented architecture informed by leading open-source broadcasting projects.

## What changed

### Go Live readiness dashboard

Studio now includes a compact **Go live check** panel covering destination configuration, network availability, microphone permission, selected capture source, and stream quality. Blocking checks explain the next user action. High-resolution or high-FPS profiles are marked as caution rather than silently rejected. The foreground service remains the authoritative validator after the UI request.

### Secure destination setup

Stream keys in Settings are masked by default and can be revealed only with an explicit visibility control. Existing encrypted credential storage, trimmed endpoint inputs, persisted destination hydration, and destination-specific settings remain in place.

### Stable streaming lifecycle

The release retains the generation-safe RootEncoder 2.8.0 adapter, terminal graphics release boundary, serialized capture preparation, one-shot MediaProjection lifecycle, stale preview-token protection, bounded reconnect policy, and 45-second silent connection watchdog. A silent connection attempt can no longer leave the operator on an indefinite loading state.

### UI performance and focus

The Compose shell continues to use stable route-local state and specialized primitive snapshot state holders. The Studio layout now presents the highest-value operational information before secondary controls, avoids desktop OBS density, and keeps the three-item mobile navigation model.

## Research-informed boundaries

The v0.4 architecture and roadmap were compared against RootEncoder, StreamPack, OBS Studio, StreamCaster Android, ScreenStreamerGo, LifeStreamer, Moblin, Owncast, and MediaMTX. The detailed comparison is in `docs/V0.4_RESEARCH_AND_PRODUCT_PLAN.md`.

True simultaneous YouTube/Twitch/Kick fan-out, SRTLA/RIST bonding, UVC camera support, OAuth chat/moderation, platform metadata APIs, cloud backup, and remote control are not claimed as complete in this release. These features require independently validated runtime paths, backend/provider permissions, relay infrastructure, or additional physical-device testing.

## Validation

The v0.4.0 source passed 100/100 static smoke checks, unit tests, Android lint, debug packaging, Android-test APK compilation, and R8 release packaging. The sandbox has no connected Android device or GPU-accelerated emulator, so long-duration streaming, destination-side ingest, Infinix graphics stress, camera/microphone switching, background streaming, and network handoff testing remain physical-device acceptance work.

## Build stack

The release uses AGP 9.3.1, Gradle 9.5.0, Kotlin/Compose compiler 2.4.10, Compose BOM 2026.08.00, compile SDK 37, target SDK 36, and RootEncoder 2.8.0.

## References

[1]: https://github.com/pedroSG94/RootEncoder "RootEncoder repository"

[2]: https://github.com/ThibaultBee/StreamPack "StreamPack repository"

[3]: https://github.com/obsproject/obs-studio "OBS Studio repository"

[4]: https://github.com/alxayo/StreamCaster-android "StreamCaster Android repository"

[5]: https://github.com/dimadesu/ScreenStreamerGo "ScreenStreamerGo repository"

[6]: https://github.com/dimadesu/LifeStreamer "LifeStreamer repository"

[7]: https://github.com/eerimoq/moblin "Moblin repository"

[8]: https://github.com/owncast/owncast "Owncast repository"

[9]: https://github.com/bluenviron/mediamtx "MediaMTX repository"

[10]: https://developer.android.com/media/grow/media-projection "Android MediaProjection guide"

[11]: https://developer.android.com/develop/background-work/services/fgs/service-types "Android foreground service type guidance"
