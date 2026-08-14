# Unictoos PiP Compositor Technical Plan

## Decision

RootEncoder 2.4.5 does **not** expose native concurrent composition of `ScreenSource` and `Camera2Source`. `GenericStream` accepts one active `VideoSource` and supports source replacement through `changeVideoSource(...)`; its public GL interface provides filters for the active render path, not a second independent source compositor.

The current release therefore does not implement screen-plus-camera PiP. This is intentional: adding a corner control without a reliable compositor would produce a UI feature that cannot generate the promised stream.

## Proposed future architecture

A future PiP implementation should introduce a dedicated compositor between capture sources and the RootEncoder output surface. Screen capture should provide the base external texture. Camera capture should provide a second external texture in a shared EGL context. A compositor render pass should draw the screen texture first, then draw the camera texture into a normalized corner viewport before handing the composed frame to the encoder.

The first supported layout should be deliberately constrained to four positions and three sizes:

| Setting | Initial values |
|---|---|
| Position | Top-left, top-right, bottom-left, bottom-right |
| Size | Small, medium, large |
| Shape | Rectangular camera window with optional corner radius later |
| Dragging | Not in the first version; use normalized offsets only after stability is proven |

The scene model should persist a `PictureInPictureLayout` on the scene or camera source, with a stable enum position, size enum, enabled flag, and normalized inset. A later version may add free dragging only after device testing covers portrait/landscape transforms and camera rotation.

## Risks and acceptance criteria

The principal engineering risks are EGL/SurfaceTexture synchronization, camera orientation and mirroring, crop behavior between portrait and landscape scenes, MediaProjection teardown, encoder-size changes, GPU load, and thermal interaction with A4. The implementation must also preserve the existing single-source camera and screen paths.

Before merge, a future PiP task should demonstrate: stable 30 FPS on representative API 29–35 devices, no black or stale texture frames during projection permission changes, correct camera orientation in both scene ratios, clean release of both sources, no regression in audio capture, and an explicit thermal/battery health signal during sustained use. It should include physical-device validation rather than relying only on unit tests.

## Dependency policy

No new dependency is approved by this plan. The first implementation should evaluate the Android/OpenGL and RootEncoder surfaces already present. If a graphics dependency becomes necessary, it must be justified and reviewed separately before addition.
