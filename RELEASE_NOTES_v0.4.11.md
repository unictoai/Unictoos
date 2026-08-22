# Unictoos v0.4.11

## Smaller installable APK

Unictoos v0.4.11 reduces packaged image payload without changing the camera, microphone, MediaProjection, RootEncoder, RTMP/RTMPS/SRT, recording, or focused broadcast controls.

The four 1440×2560 onboarding images were converted from PNG to WebP while preserving their dimensions and resource names. Android continues to load them through the same drawable resource identifiers, so this is a packaging optimization rather than a streaming-path change.

| Artifact | v0.4.10 | v0.4.11 | Reduction |
|---|---:|---:|---:|
| Debug APK | 102,882,911 bytes | 87,107,879 bytes | 15,775,032 bytes / 15.3% |
| Unsigned release APK | 21,348,567 bytes | 8,516,479 bytes | 12,832,088 bytes / 60.1% |
| Instrumentation APK | 2,431,892 bytes | 2,431,040 bytes | 852 bytes / 0.04% |

The release APK reduction is primarily from replacing approximately 16 MiB of onboarding PNGs with approximately 0.59 MiB of WebP assets. Release resource shrinking remains enabled.

## Validation

The debug and instrumentation APKs were rebuilt successfully after the resource conversion. The minified release APK was also rebuilt successfully. JVM tests, debug lint, the static smoke gate, security-source audit, package metadata inspection, and whitespace validation passed. The smoke gate includes a regression check requiring all four onboarding assets to remain WebP.

Physical-device capture and YouTube/Twitch/Kick ingestion were not available in the build environment and are not claimed here.

## Version

- Application ID: `com.unictoai.unictoos`
- Version name: `0.4.11`
- Version code: `52`
- RootEncoder: `2.8.0`

Install the debug APK for phone testing. Before testing, uninstall the previous build or clear Unictoos app data, configure a destination in Settings, then open Go Live and approve the requested Android permissions.

## Artifact checksums

```text
733d9b994a6f2f1df042a915c8cd41099409979d1f7baa22c402e27aadb2740a  Unictoos-v0.4.11-androidTest.apk
07267f0ca88b2964b44e207df36be71efe5132c1d0e6259715a9e4d6d273b4bc  Unictoos-v0.4.11-debug.apk
83e561f5c8b7922443b87bef62fb3fff4c9ea0813093ef0a5ceb812c5fd53e1e  Unictoos-v0.4.11-release-unsigned.apk
```
