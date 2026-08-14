# External Input and Multi-Destination Audit

## Current RootEncoder surface

The Unictoos service currently uses `Camera2Source`, `ScreenSource`, `MicrophoneSource`, `NoVideoSource`, and `NoAudioSource` from RootEncoder 2.4.5. The public library surface does not include a UVC/USB capture-card source, a provider fan-out manager, or a multi-endpoint `GenericStream` session abstraction.

| Requested capability | Current state | Safe follow-up |
|---|---|---|
| Android camera | Supported through `Camera2Source` | Keep current permission and lifecycle path |
| USB webcam/capture card | Not exposed by current source layer | Add a dedicated UVC source after device and format testing |
| Multiple destinations | One endpoint per service instance | Design per-destination sessions and aggregate health first |
| Destination-specific reconnect | Existing single-endpoint behavior | Preserve existing auth/error semantics; do not fork them as a side effect |
| Shared composed output | Not available from current source path | Requires the future GL compositor described in `PIP_COMPOSITOR_PLAN.md` |

## USB/UVC follow-up

A production USB-input implementation must cover USB host permission, attach/detach broadcasts, UVC format negotiation, frame timestamps, color conversion, rotation, aspect-ratio policy, and release behavior when a capture card disappears. It should be validated across several Android 10–15 devices because USB camera support is device- and vendor-dependent.

## Multi-destination follow-up

A production multi-destination implementation should model each destination independently, including endpoint, platform, connection status, bitrate allocation, reconnect attempts, authentication errors, and user-visible failure actions. The preflight must estimate aggregate upload demand and battery impact before starting. The system must define whether adaptive bitrate changes all destinations together or applies per destination, and whether recording represents the primary stream or a local composed output.

No new dependency is added by this audit, and no runtime behavior is changed by the document.
