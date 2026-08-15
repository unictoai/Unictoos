# Unictoos v0.2.5 — Single-slot MultiStream runtime migration

Unictoos v0.2.5 is the first runtime migration step inspired by the technology evidence found in the user-provided LiveLens 0.8.6 APK. The foreground streaming service now uses a compatibility wrapper backed by RootEncoder 2.5.9 `MultiStream` rather than `GenericStream`, while exposing the same service lifecycle to the existing mobile-first UI.

This is intentionally a **single-destination** build. The adapter uses RTMP slot zero, preserves the current endpoint and authentication flow, keeps the existing generation-bound callbacks, retains the Infinix X6853 preview-free fallback, and does not expose simultaneous YouTube/Twitch/Kick fan-out yet. The MultiStream destination manager and slot contracts from v0.2.4 remain available for the later per-destination session integration.

The migration uses RootEncoder’s shared encoder and per-output client model rather than creating multiple camera, GL, or MediaCodec pipelines. It also keeps the existing release ordering, RenderErrorCallback handling, bounded reconnect policy, recording path, and CredentialStore encryption unchanged. This build is intended to determine whether the current graphics failure is specific to the GenericStream owner or remains in the shared RootEncoder GL/render path.

The mobile UI is unchanged and remains intentionally touch-first rather than OBS-like. No LiveLens proprietary source or APK code was copied or decompiled into Unictoos.

## Validation

The required sandbox gate must pass `lintDebug`, `testDebugUnitTest`, `assembleDebug`, `git diff --check`, and the repository smoke suite. Physical validation is mandatory on the Infinix X6853. Stream to one destination at 720p/30 FPS for 15–20 minutes, deliberately test graphics exhaustion and the Fix/retry path if possible, and report whether the error recurs. Simultaneous multistreaming and the two-destination default remain blocked until this single pipeline is proven stable.
