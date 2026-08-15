# Unictoos v0.2.4 — LiveLens-inspired streaming foundation

Unictoos v0.2.4 begins an independent architecture migration informed by static analysis of the user-provided LiveLens 0.8.6 APK and by LiveLens’s successful real-device streaming behavior. The goal is to adopt the same class of technology choices—bounded scene composition, a foreground streaming owner, capability-aware profiles, and one shared encoded-media path with destination fan-out—without copying proprietary LiveLens implementation code or changing Unictoos’s mobile-first interaction model.

This build adds a UI-neutral `MultiStreamDestinationManager` backed by the open RootEncoder 2.5.9 `MultiStream` API. It provides stable destination-slot mapping, per-destination `ConnectChecker` slots, explicit RTMP start/stop methods, shared encoder preparation, and bounded destination selection. The existing two-destination direct-device cap remains active, and three-destination support remains gated to the Infinix X6853.

The manager is intentionally not wired into the live foreground service in this release. The current single-destination capture/encoder path remains unchanged while the recurring Infinix X6853 graphics-resource exhaustion is physically retested. This avoids using an unverified pipeline as the foundation for simultaneous streaming and keeps diagnosis separable.

The existing mobile-first Unictoos UI, CredentialStore encryption, platform authentication behavior, Infinix preview-free fallback, RootEncoder render-error handling, release bookkeeping, recording flow, and 16:9/9:16 scene options are retained. The v0.2.4 architecture document explains the evidence and the staged runtime integration plan.

## Validation

The sandbox validation passed `lintDebug`, `testDebugUnitTest`, `assembleDebug`, `git diff --check`, and 48/48 static smoke checks. Physical validation remains required on the Infinix X6853: run a single-platform 720p/30 FPS stream for 15–20 minutes, reproduce graphics exhaustion deliberately if possible, tap Fix, and verify that retry succeeds. This build is not evidence that the graphics leak is fixed and does not yet enable simultaneous multistreaming.
