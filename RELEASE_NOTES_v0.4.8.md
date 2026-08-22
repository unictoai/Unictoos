# Unictoos v0.4.8

Unictoos v0.4.8 is a focused live-start correction release for users who could not reach a working stream after pressing Go Live.

## Startup correction

The built-in **Main Camera** scene now keeps its optional screen source disabled by default. This prevents a mixed legacy scene from unexpectedly routing a camera broadcast into Android screen capture. A shared `CaptureModePolicy` is now used by both the Go Live request and Studio readiness UI: camera is selected when an enabled camera source exists, screen is selected only for a screen-only scene, and empty scenes are rejected.

Existing mixed scenes are handled by the same camera-first policy, so upgraded installations no longer unexpectedly ask for screen capture when the selected scene includes an active camera. Users who want screen capture can disable the camera source and enable the screen source in Scenes.

## Permission flow

The visible **Go Live** button remains available while setup is incomplete. Pressing it requests microphone permission, requests camera permission for camera capture, and then opens Android’s separate screen-capture consent dialog for screen-only broadcasts. If a destination is missing, the app explains that setup step instead of silently attempting a connection with an empty endpoint.

## Validation

The capture-mode policy has dedicated JVM regression tests. The release also passes the expanded static smoke suite, JVM unit tests, debug lint, debug APK assembly, instrumentation APK assembly, minified release APK assembly, package/version inspection, and checksum verification. Real Android permission dialogs, camera hardware, and platform ingest still require physical-device validation.
