# Unictoos v0.1.0-alpha17

## Real live preview and capture readiness

Alpha17 replaces the previous logo-only preview card with a real Android `SurfaceView` connected to RootEncoder. When screen capture or camera capture is approved, the foreground service attaches the valid preview surface and renders the actual capture stream inside Studio.

The capture flow is now serialized. A broadcast or practice request waits for capture preparation and preview attachment instead of racing the asynchronous permission/setup operation. This prevents the app from presenting a prepared-looking screen while the encoder has not received a usable video surface.

Studio now distinguishes between **Preview waiting**, **Preview ready**, and **Live • streaming**. The app will not start a practice session or broadcast until capture, microphone, and preview are ready. If a requested preview cannot be attached within 30 seconds, the session exits `PREPARING` with an actionable error instead of remaining indefinitely in that state.

Preview surface destruction is handled safely during navigation and rotation. If a live stream continues while Studio is temporarily not visible, the broadcast is not silently stopped; the UI reports that the preview is unavailable until the surface is recreated.

CredentialStore encryption, stream-key handling, destination authentication errors, reconnect behavior, and the RootEncoder dependency remain unchanged.

## Validation

Alpha17 passed `lintDebug`, `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, and the repository feature smoke suite with **47/47 checks passed**. Testing on a physical Android device is still required to verify camera/screen frames on the specific device and Android version.

Install alpha17, open **Studio**, start a **Practice** session, approve capture, and confirm that the actual camera or screen appears in the preview before testing an unlisted stream.
