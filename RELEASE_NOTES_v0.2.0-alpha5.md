# Unictoos v0.2.0-alpha5 — Explicit Retry Cleanup

This build addresses the confirmed retry-path defect behind the graphics-exhaustion reports. The Studio **Fix** action previously only navigated to Settings; it did not stop preview, release `GenericStream`, release the active MediaProjection/camera sources, or wait for RootEncoder’s asynchronous GL executor teardown.

Alpha5 adds a dedicated release-capture service action. Tapping Fix now runs explicit failure cleanup, releases preview and capture resources, releases the failed RootEncoder pipeline once, waits for the GL teardown window, resets capture state to idle, and stops the service cleanly before the user starts capture again. The uncaught RootEncoder GL safety-net path uses the same shared cleanup routine. Service destruction also avoids double-releasing an already released pipeline.

The previous alpha4 duplicate-preview-attach fix remains included. The build passed `lintDebug`, `testDebugUnitTest`, `assembleDebug`, and all 47 feature smoke checks.

## Required device test

Reproduce the graphics-exhaustion error once, then tap **Fix**. Wait for the message that capture resources were released. Start the same camera or screen capture again and confirm that preparation succeeds rather than immediately failing. Repeat the failure/retry sequence at least three times if possible, then leave a live test running for at least 10 minutes.

Priority 1 is not considered complete until this retry test succeeds on the Infinix X6853 running Android 15.
