# Telemetry Limitations

## Audio level

The Studio health center now renders an audio-level meter from `StreamHealthSample.audioLevel`. RootEncoder 2.4.5's public `MicrophoneSource` API exposes mute/unmute and capture lifecycle methods, but it does not expose a public peak/RMS callback or a safe read-only level stream from the active encoder input.

Unictoos therefore does not open a second `AudioRecord` while RootEncoder owns the microphone. A second recorder could conflict with the active capture session and would produce misleading or device-dependent results. Until a supported microphone tap is introduced, the meter honestly shows **“Waiting for microphone level telemetry”** when the engine reports zero and displays reported values when available.

A future audio-meter task should introduce a single-owner capture path that calculates peak/RMS before the encoder consumes the same PCM frames, then publishes a throttled 0–100 level through `StreamingStatusBus`. It must preserve microphone permission behavior, echo-cancellation/noise-suppression settings, and physical-device stability.
