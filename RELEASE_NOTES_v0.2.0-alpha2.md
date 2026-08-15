# Unictoos v0.2.0-alpha2

Unictoos v0.2.0-alpha2 is a stability release focused on a confirmed Android 15 crash affecting RootEncoder’s OpenGL rendering thread. It keeps the visual overhaul and explicit ViewModel construction from alpha1, while adding a narrowly scoped graphics-failure safety net and stronger preview resource lifecycle management.

## Crash 2: RootEncoder GL exhaustion

The confirmed `GL_OUT_OF_MEMORY` failure was thrown from RootEncoder’s background rendering thread and could terminate the process before the foreground service had an opportunity to report a useful state. The application now installs a process-level uncaught-exception handler that recognizes the confirmed RootEncoder GL signature, publishes `StreamStatus.ERROR` through the existing `StreamingStatusBus`, stops the active capture service, and reports that graphics resources were exhausted. Unrelated exceptions continue to the Android default handler rather than being silently swallowed.

The classifier is deliberately conservative. It requires a RootEncoder stack frame, an encoder or pool-style render thread, and the confirmed `GL error: 1285` or `GL_OUT_OF_MEMORY` signature. Unit tests cover the positive signature, nested causes, unrelated encoder failures, and GL-shaped failures on unrelated threads.

## Preview and capture resource lifecycle

Repeated camera and screen-capture preparation now stops the active RootEncoder preview and clears GL filters before replacing the video source. Normal stop, detach, encoder-failure, and service-destruction paths use the same guarded preview teardown, followed by source release and projection cleanup. The Compose-hosted `PreviewSurfaceView` also reports its final surface destruction when it leaves composition, preventing stale service references from retaining a preview across repeated navigation or configuration changes.

These changes are intended to reduce EGL texture and surface accumulation during repeated start/stop and capture-source transitions. They do not change credential encryption, stream reconnect policy, or authentication-error handling.

## Validation

The following checks passed after the final Crash 2 changes:

```text
./gradlew lintDebug testDebugUnitTest assembleDebug --no-daemon --max-workers=1 -Dorg.gradle.jvmargs='-Xmx1024m -Dfile.encoding=UTF-8'
python3 tools/feature_smoke_test.py
```

The result was **47/47 smoke checks passed**, and the new encoder crash policy tests passed with the complete unit-test suite. The alpha2 release APK is built separately with the one-gigabyte Gradle heap required by the sandbox’s R8 release build.

## Physical-device verification plan

On the Infinix X6853 running Android 15, install the alpha2 APK after removing any older debug build if Android reports a signature or downgrade conflict. Grant microphone, camera, and screen-capture permissions, open Studio, and verify that the preview appears before starting a practice recording or broadcast.

For the primary regression test, prepare the camera preview, stop it, and repeat the start/stop cycle at least twenty times. Repeat the same exercise with screen capture, then alternate camera and screen capture without force-stopping the app. During each cycle, confirm that the preview returns, the UI remains responsive, and the status does not remain stuck in `PREPARING` or `CONNECTING`.

For endurance testing, leave the preview active for at least fifteen minutes, then background and foreground the app several times while the preview is running. Finally, perform one short practice recording and one short real broadcast with a disposable or test destination. Verify that stopping the session returns the app to a usable state and that no Android crash dialog appears. If the crash recurs, capture the Android bug report immediately and record whether it occurred during camera preparation, screen preparation, preview attachment, source switching, or an active stream.

## Known scope limits

This alpha2 release mitigates the confirmed RootEncoder GL failure and closes the identified preview lifecycle gaps, but physical-device validation remains necessary for long-session GPU memory behavior because Android OEM graphics drivers vary. Official platform OAuth, chat, moderation, simultaneous multi-destination output, and full multi-source compositing remain outside this release’s scope.
