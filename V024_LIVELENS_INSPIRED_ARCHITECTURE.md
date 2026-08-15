# Unictoos v0.2.4 LiveLens-inspired architecture

## Decision summary

The uploaded LiveLens 0.8.6 APK provides strong evidence of a hybrid Android streaming design: app-owned FBO scene classes, a foreground stream service, persistent stream configuration/status, a custom native Vulkan library, and a bundled RootEncoder family that includes `MultiStream`, `MultiPreviewConfig`, `GlStreamInterface`, and `RenderErrorCallback`. Unictoos will independently reproduce the stable technology pattern without copying LiveLens proprietary implementation code, branding, UI, or decompiled logic.

The first v0.2.4 increment adds a bounded, UI-neutral `MultiStreamDestinationManager` around the open RootEncoder 2.5.9 `MultiStream` API. The manager maps selected destinations to stable RTMP indexes, uses one shared encoded-media pipeline, duplicates packet buffers inside RootEncoder’s fan-out path, and keeps per-destination callback slots ready for later session integration. It is intentionally not wired into the active `StreamingForegroundService` in this increment because the Infinix X6853 graphics pipeline has not yet passed the required physical stability gate.

## Adopted principles

| Evidence or behavior | Independent Unictoos decision |
|---|---|
| LiveLens exposes app-owned FBO scene classes | Keep scene composition as an app-owned boundary and avoid placing platform/session logic in Compose UI |
| LiveLens bundles `com.pedro.library.multiple.MultiStream` | Use the open RootEncoder MultiStream API for future shared-encoder fan-out rather than creating three encoders |
| RootEncoder MultiStream exposes per-index RTMP clients | Assign a stable index to each selected destination and attach an independent ConnectChecker |
| RootEncoder duplicates encoded buffers per client | Preserve one capture/encoder pipeline and fan out encoded media after encoding |
| LiveLens exposes `StreamService`, `StreamConfig`, and `StreamStatusStore` | Keep foreground service ownership separate from UI state and destination configuration |
| LiveLens claims safe/device-based limits | Preserve the two-destination direct-device cap, add capability preflight before runtime fan-out, and gate three destinations on Infinix X6853 |
| LiveLens supports optional recording and overlays | Keep recording and scene overlays behind the shared media boundary, but do not add them to this risky runtime increment |

## RootEncoder 2.5.9 integration contract

`MultiStream` accepts arrays of `ConnectChecker` callbacks, supports `MultiType.RTMP`, and exposes `startStream(MultiType.RTMP, index, endpoint)`, `stopStream(MultiType.RTMP, index)`, and `getStreamClient(MultiType.RTMP, index)`. The current manager constructs it with `NoVideoSource` and `NoAudioSource` so that camera and microphone resources are not opened during manager construction. Source replacement remains explicit through `changeVideoSource` and `changeAudioSource`.

The manager is bounded to the existing `MultistreamDefaults.DIRECT_DESTINATION_CAP` of two. An explicit higher cap is allowed for deterministic tests and future device-gated work, but RootEncoder’s maximum supported slot count is four and the product policy still keeps three-destination rollout gated to approved hardware.

## Runtime integration still required

The next integration task must port the current one-endpoint lifecycle through the manager while retaining one-destination behavior. It must add per-destination connection generations, retry budgets, network epochs, independent stop/reconnect/auth states, aggregate state reduction, and diagnostics metadata. It must not modify CredentialStore encryption or route a failed destination into global GL teardown.

Only after the single-destination path passes a 15–20 minute Infinix X6853 run, including deliberate graphics-exhaustion and Fix/retry validation, may the manager be wired into capture runtime. Two-destination testing comes next. Three destinations remain disabled by default and require physical validation on the Infinix X6853.

## UI contract

The existing Unictoos mobile-first UI remains unchanged. It is not being converted into an OBS-style desktop control surface. Future destination cards may be added to the existing Studio workspace, but the service and data contracts must be implemented first so the UI does not advertise unsupported simultaneous streaming.

## Evidence limits

The LiveLens APK was statically inspected only. Class names and library presence are evidence of packaged capabilities, not proof that every bundled API is active at runtime. LiveLens’s successful real-device test is valuable behavioral evidence, but its proprietary renderer and service implementation are not copied or reverse-engineered into Unictoos.
