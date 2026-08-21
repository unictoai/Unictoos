# Open-source component research

## Evidence captured on 2026-08-21

### StreamPack
Source: https://github.com/ThibaultBee/StreamPack

The repository describes StreamPack as an Apache-2.0 Android library for low-latency live broadcasting. Its README lists RTMP/RTMPS and SRT streaming, multiple file outputs including MP4/WebM/fragmented MP4, simultaneous record-and-stream, device capability querying, custom audio/video sources, Camera2, MediaCodec, coroutine-based pipelines, and `SingleStreamer`/`DualStreamer`/`StreamerPipeline` components. The repository page shows release 3.2.0 and ongoing commits. This is relevant as an architectural reference and possible future dependency, but replacing a stable RootEncoder pipeline solely from README claims would require API migration and physical-device regression testing.

### RootEncoder
Source: https://github.com/pedroSG94/RootEncoder

The repository describes RootEncoder as a Java/Kotlin Android encoder supporting RTMP, RTSP, SRT, UDP, and beta WHIP. Its listed capabilities include Camera1/Camera2, surface and buffer encoders, echo cancellation, noise suppression, camera switching, bitrate changes, multiple hardware/software codecs, MP4 recording while streaming, image/GIF/text insertion, OpenGL filters, file sources, and device-screen capture. The SRT list includes packet resend and AES encryption but marks SRT authentication as unsupported. The current Unictoos baseline already uses RootEncoder 2.8.0, so the safest backend-free expansion is additive: use existing verified RootEncoder paths and add locally owned policy/UI/persistence rather than swap engines during a feature build.

## Initial selection rule

Use permissive-license components only after checking the exact license and transitive native dependencies. Prefer Android platform APIs and already bundled RootEncoder capabilities when they cover the need. Treat server-dependent protocols, OAuth, cloud synchronization, signaling, relay bonding, and provider metadata as contracts rather than local features.

### ScreenStreamerGo
Source: https://github.com/dimadesu/ScreenStreamerGo

This Kotlin Android app is a small practical broadcaster built around StreamPack. Its README lists SRT/RTMP screen sharing, H.264/H.265 video, AAC/Opus audio, light/dark UI, and optional SRTLA bonding through the separate Bond Bunny app. The repository is GPL-3.0, has a small commit history, and explicitly calls out unresolved audio playback behavior and roadmap items such as aggressive reconnect and adaptive bitrate. It is useful as a feature reference, but its GPL-3.0 license and StreamPack submodule mean direct code reuse in an Apache-2.0 Unictoos app would require careful legal review; the safer choice is independent implementation against permissively licensed dependencies.

### Android MediaProjection guidance
Source: https://developer.android.com/media/grow/media-projection

Android’s current documentation states that user consent is required before each media-projection session and that a projection token is single-use for `createVirtualDisplay()` on Android 14 and higher. This validates Unictoos’s existing generation-safe foreground-service design and means a local “all features” pass must not reuse a projection token across retries or pretend that a screen/camera compositor can be added without its own surface lifecycle. Android platform capture APIs are preferable to pulling in another capture framework when the existing service already owns the correct lifecycle.

### Moblin
Source: https://github.com/eerimoq/moblin

Moblin is an actively developed open-source iOS IRL streaming app. Its repository contains a large test and application codebase, remote-control frontend, screen-recording components, and settings URLs for quick buttons and stream configuration. It is a useful reference for creator workflow ideas such as quick controls and remote operation, but it is primarily Swift/iOS and its repository scope is not a drop-in Android dependency. Protocols and relay features such as SRTLA/RIST still depend on compatible network infrastructure, so Unictoos should not present them as backend-free.

### Jetpack Media3 Transformer
Source: https://developer.android.com/media/implement/editing-app

Android’s official Media3 Transformer guide documents local trimming, scaling, rotation, overlays, effects, export, and raw-PCM `AudioProcessor` chains. It also describes transmuxing when input codecs already match the requested output and provides Android API/device requirements for some HDR operations. This makes Media3 Transformer a credible permissive/open-source candidate for a future local Library editor, but adding it immediately would increase the dependency surface and needs an API-version decision. A safe first pass is to add a Media3-backed editor only if the project’s existing dependency graph and APK size budget permit it; otherwise retain the tested trim policy and explicit editor contract.

### AndroidX Media3 repository and license
Source: https://github.com/androidx/media
License: https://github.com/androidx/media/blob/release/LICENSE

The AndroidX Media3 repository is actively maintained on its release branch and documents a 1.11.0 release line. Its license page identifies Apache License 2.0 with preservation of copyright and license notices and state-of-changes requirements. This is compatible with Unictoos’s Apache-2.0 distribution model. Media3 Transformer is therefore a credible future local editor dependency, but the current release keeps the dependency surface unchanged after the initial enhancement build; its integration should be isolated and benchmarked because it adds transcoding code and APK size.
