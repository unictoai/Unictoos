# Graphics-resource exhaustion audit

Date: 2026-08-15

## Confirmed findings

The application’s alpha6 cleanup path does call `genericStream.release()`, but RootEncoder 2.4.5 does not make that cleanup synchronous. In the cached RootEncoder 2.4.5 bytecode, `GlStreamInterface.stop()` sets its `running` flag to false and submits the actual EGL teardown (`SurfaceManager.release()`, `MainRender.release()`, and executor shutdown) asynchronously to the single-thread GL executor. `deAttachPreview()` is also queued asynchronously. `GenericStream.release()` delegates to `stopPreview()` and `stopSources()`, but those operations still depend on the GL executor for the actual GL/EGL release.

When the GL_OUT_OF_MEMORY exception is thrown on the RootEncoder render executor, that executor is the thread that has just failed. The application-level uncaught-exception handler then sends a service action, and the service calls `genericStream.release()` from the main thread. Because the old executor may already be dead, the queued stop runnable can be rejected or never run, leaving EGL/GL resources allocated even though the app’s Kotlin state is reset. This explains why the Fix button can display the expected recovery message while the next capture immediately fails again.

The service also uses `SystemClock.sleep(150)` as a fixed settle delay after release, but it does not verify that RootEncoder’s GL interface is no longer running or that the executor has terminated. A fixed delay is not a reliable completion signal for an asynchronous or already-crashed render thread.

`PreviewSurfaceView` registers the same `SurfaceHolder.Callback` in both `init` and `onAttachedToWindow`. It also emits a synthetic `onSurfaceDestroyed()` from `releasePreviewListener()`. Those behaviors can create duplicate surface lifecycle events and stale attach/detach traffic during Compose disposal or retry, increasing the chance of repeated preview operations around a failed pipeline.

`MainActivity` intentionally dispatches prepare and start as separate service intents. The service queues the start until capture and preview are ready, which is valid, but it means a retry can overlap with surface callbacks unless the failed pipeline has a deterministic completion boundary.

## Upstream signal

RootEncoder 2.8.0 release notes explicitly include “Fix EGL lifecycle on stop (OpenGlView / GlStreamInterface)” and related GL resource fixes. The application currently depends on RootEncoder 2.4.5. The next implementation should evaluate upgrading to the upstream fix first, then add application-side idempotent recovery and preview callback de-duplication.

## Remediation direction

1. Upgrade RootEncoder to the upstream version containing the EGL lifecycle fix, subject to API/build compatibility.
2. Make preview-surface callback registration exactly-once and remove synthetic destruction notifications that duplicate Android surface lifecycle.
3. Add an idempotent recovery boundary that prevents a new capture prepare from starting until the failed service/pipeline has been fully released.
4. Add diagnostics for recovery start, release completion, and prepare generation so device testing can prove whether the old pipeline is gone.
5. Re-run lint, unit tests, assembleDebug, and the 47-check smoke suite after each commit; then publish a new device-test APK.
