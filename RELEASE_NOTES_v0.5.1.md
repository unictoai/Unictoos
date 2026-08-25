# Unictoos v0.5.1

Unictoos v0.5.1 is a focused hotfix for the reported crash after tapping **Go Live** in v0.5.0. It preserves the v0.5.0 streaming feature set while reducing work performed during the most sensitive startup window and making optional service initialization failure-safe.

## Crash-focused changes

The common Go Live path no longer mutates RootEncoder's GL filter graph when a scene has no active text overlays. Text overlay installation is now guarded and degrades to a diagnostic rather than preventing stream startup. Experimental PiP filter creation is deferred until after the encoder has started consuming frames; projection preparation itself remains screen-source and microphone setup only. If the optional PiP layer cannot initialize, the service records `pip_fallback_screen_only` and continues with the primary capture path.

Dynamic screen-state receiver registration now uses the Android 13+ `RECEIVER_NOT_EXPORTED` form and is optional. Failure to register it does not abort camera or screen capture. Synchronous service initialization is guarded, records `service_initialization_failed`, releases any wake lock, and stops safely instead of allowing a service exception to terminate the process. A failed initialization cannot accept a later command, and null-intent service restarts remain non-resurrecting.

## Preserved v0.5 behavior

The hotfix retains bounded two-destination RootEncoder MultiStream transport, per-destination health observation over the shared encoder, default-on adaptive bitrate policy using the 80%/15-second degradation and 95%/60-second recovery thresholds, camera-only background audio behavior, text overlays, Keystore-backed credentials, scene persistence, and generation-safe capture teardown. No backend, cloud relay, Firebase, Supabase, OAuth redirect server, paid API, desktop OBS server, WHIP, or WebRTC service was added.

## Validation

| Gate | Result |
|---|---|
| JVM unit tests | Passed after the crash-focused service changes. |
| Android lint | Passed after the crash-focused service changes. |
| Debug APK assembly | Passed after the crash-focused service changes. |
| Static feature smoke gate | Passed with 155 assertions, including the new crash guards. |
| Source security audit | Passed with zero suspicious credential literals and zero direct logging calls. |

These are sandbox build and source checks. They do not prove successful platform ingest, PiP operation, Android 15 OEM background execution, graphics-resource stability, or a 60-minute stream. No physical Android device, emulator, ADB, or logcat was available for this hotfix, so the crash cannot be declared conclusively fixed until the new APK is tested on the target device.

## Installation

Use the debug APK for device testing. The unsigned release APK is included for users who have their own signing/distribution process. The package remains `com.unictoai.unictoos`, with version name `0.5.1` and versionCode `61`.

Final artifacts are listed below. The debug APK is 87,239,010 bytes, the unsigned release APK is 8,549,243 bytes, and the instrumentation APK is 2,434,801 bytes.

| Artifact | SHA-256 |
|---|---|
| `Unictoos-v0.5.1-debug.apk` | `abb8dd00204de0ee01d968b0679f7b52cd7a7a89efa47b088d939b0161389c47` |
| `Unictoos-v0.5.1-release-unsigned.apk` | `a5709ee0c2f87d6c03b113885b4df9044445a1a970f88ee613f2a4690f3de1e4` |
| `Unictoos-v0.5.1-androidTest.apk` | `030b2608bccda2a91058cd798bd7365faf1391840eed32f4082047aa7174b722` |
