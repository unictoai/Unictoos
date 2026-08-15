# Unictoos v0.2.0-alpha4 — Live Preview Stability Test

This build addresses a reproducible graphics-exhaustion failure that appeared about ten seconds after starting a live session. The failure was traced to repeated preview-surface availability callbacks during Compose recomposition: live telemetry updates caused the Android preview view to be updated, which could repeatedly post a surface-attach request while RootEncoder was already rendering. The service could then stop and restart the preview pipeline unnecessarily, accumulating asynchronous EGL work.

The preview listener is now stable across recompositions, `PreviewSurfaceView` only posts an availability callback when its listener actually changes, and `StreamingForegroundService` ignores duplicate attach requests for an already-valid preview with the same dimensions. The previous RootEncoder pipeline recreation, asynchronous EGL settle window, MediaProjection cleanup, and idle-FPS reset remain included.

The build passed `lintDebug`, `testDebugUnitTest`, `assembleDebug`, and all 47 feature smoke checks. The device-test gate remains essential because the original failure is graphics-driver and lifecycle dependent.

## Required retest on the Infinix X6853 / Android 15

Install alpha4, then start a camera or screen preview and begin a test broadcast. Leave it live beyond the previous ten-second failure point, preferably for at least 10 minutes. Repeat the start/stop preview cycle several times, background and foreground the app once, and verify that the preview remains active, the live status remains stable, and no graphics-exhaustion error appears. Also confirm that FPS remains a sensible active value while live and returns to `—` after stopping.

If the error recurs, capture a full logcat or Android bug report during the reproduction and note whether it occurs with camera or screen capture, whether the app was backgrounded, and whether local recording was enabled.
