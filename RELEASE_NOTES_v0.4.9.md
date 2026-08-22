# Unictoos v0.4.9

## Pre-connection startup correction

This release replaces the Activity-to-service two-intent Go Live handoff with one atomic foreground-service request. Camera and Android screen-capture flows now carry the endpoint, scene, practice flag, and start-after-prepare request together. The foreground service queues the start request before capture preparation and starts the encoder only after the selected capture source is ready. This removes the race where a separate `START` command could arrive while capture preparation was still launching.

Capture-preparation coroutine failures are now converted into a visible terminal error instead of leaving the session indefinitely in `Preparing capture`. The service records redacted diagnostic events for camera and projection preparation exceptions and asks the user to retry with the relevant permission.

## Validation

- Static feature smoke checks: 141/141 passed.
- JVM unit tests: passed.
- Debug lint and release/build gates are required before publication.
- No physical Android device, ADB, logcat, YouTube, Twitch, or Kick ingest validation is available in this environment. The APK must therefore be tested on the target Infinix X6853 after installation.

## Install and test

1. Uninstall the previous Unictoos build or clear its app data.
2. Install the v0.4.9 debug APK from the GitHub release.
3. Open **Settings**, add and save a valid RTMP/RTMPS destination, then return to **Go Live**.
4. Choose the camera scene and tap **Go Live**.
5. Expected behavior is immediate staged progress, camera/microphone prompts when required, and a clear error if capture preparation fails. It should not remain indefinitely on `Preparing capture`.
6. If using screen capture, approve the Android capture prompt and retry only if Android denies the request.

A successful APK build does not prove platform ingest. Please report the exact visible state/message and whether the app reached `LIVE` when testing on the phone.

## Scope boundary

This release is narrowly focused on the pre-connection startup path. It does not claim to solve device-specific long-duration graphics-resource exhaustion or prove multi-destination ingest without physical-device evidence.

## Checksums

The final published checksums are generated from the exact APK files after the complete build gate.

## License

Unictoos remains open source under the repository's existing license.

## Version

- Version name: `0.4.9`
- Version code: `50`
- Application ID: `com.unictoai.unictoos`

## Support

For a device report, include Android version, capture mode, exact destination type, visible status message, and a screenshot if possible. Do not share stream keys or other credentials.

## Sources

- [Unictoos repository](https://github.com/unictoai/Unictoos)
- [Unictoos releases](https://github.com/unictoai/Unictoos/releases)

## Hashes

SHA-256 values for the exact published files:

| Artifact | SHA-256 |
|---|---|
| `Unictoos-v0.4.9-debug.apk` | `13580311b6fc8655ad34396210c475871c94eb71ae43c34d4aef92f03f8f7743` |
| `Unictoos-v0.4.9-release-unsigned.apk` | `594c4c3cf800a39aeb0b8bf4f297281c149cd9bb71fdc89ca793ad0b32c3b07b` |
| `Unictoos-v0.4.9-androidTest.apk` | `445bb19a13f5326042da0a334b14e4af22a74155feb4b0e0a972b3b8e6691df8` |

## End

The release is intentionally conservative: it changes the startup synchronization boundary and error visibility without adding unrelated features.

## Maintainer note

Before starting a stream, verify that a destination has been saved. Practice mode does not require an endpoint; normal streaming does.

## Reproducibility

Build with the repository's documented Gradle commands and JDK/Android SDK versions. Do not modify the APK after generating the checksum.

## Artifact policy

The GitHub release should include debug, release, and instrumentation APKs only after all gates pass.

## No fabricated device claims

A local build/test pass is not evidence that a physical phone opened its camera, acquired its microphone, or connected to a platform. Those outcomes must be reported separately from automated validation.

## Final scope summary

Atomic request handoff, bounded capture-preparation error handling, and release metadata are the contents of v0.4.9.

## End of release notes

