# Unictoos v0.3.4

## Live-start hotfix

Unictoos v0.3.4 fixes a live-stream startup path that could remain in a loading or `CONNECTING` state indefinitely when RootEncoder did not deliver either a connection-success or connection-failed callback.

The service now starts a generation-aware connection watchdog after every initial or reconnect attempt. If the same active generation remains connected to a configured endpoint without reaching `LIVE` for 45 seconds, the attempt is recorded as a timeout and enters the existing bounded reconnect policy. The watchdog is cancelled on connection success, failure, disconnect, authentication failure, and manual stop. A stale callback or a stopped generation cannot trigger a new retry.

The hotfix preserves the existing preview-isolation behavior for the Infinix X6853 and the explicit capture-pipeline release boundary. It does not invent a newer RootEncoder version: the official RootEncoder release history still identifies 2.8.0 as the latest published stable release, and Unictoos remains on `com.github.pedroSG94.RootEncoder:library:2.8.0`.

## UI and build status

The release retains the Compose performance improvements that use specialized primitive snapshot state in the Library screen, Settings screen, and shared overlay editor. The build stack remains AGP 9.3.1, Gradle 9.5.0, Kotlin/Compose compiler 2.4.10, Compose BOM 2026.08.00, compile SDK 37, and target SDK 36.

## Validation

The final hotfix validation completed 97/97 static smoke checks, unit tests, Android lint, debug APK assembly, Android-test APK compilation, and the R8 release assembly. The sandbox has no connected Android device or GPU-accelerated emulator, so long-duration ingest, device graphics stress, and real destination authentication still require physical testing.

Install the debug APK from the GitHub release for device testing. The unsigned R8 artifact is supplied for build verification and is not intended for direct installation.
