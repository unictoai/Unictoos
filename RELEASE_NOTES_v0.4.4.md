# Unictoos v0.4.4

## Open-source on-device expansion

Unictoos v0.4.4 adds a real local recording trim/export workflow backed by Jetpack Media3 Transformer 1.11.0. Creators can select an MP4 from Library, choose start and end seconds, and export a new trimmed MP4 into app-private storage without uploading media or changing the original file. The editor reports started, completed, and failed states to the mobile UI. The existing validation policy rejects unreadable files, invalid time ranges, and unsafe output requests before work starts.

The release also carries forward v0.4.3’s local SQLite session analytics, session comparison, persisted scene source groups, Cut/Fade/Slide transition metadata, reusable scene templates, safe scene-only import/export, live encoder telemetry, camera switching, actionable notification controls, bounded two-destination fan-out, and SRT endpoint routing.

## Open-source research decisions

The implementation was informed by StreamPack, RootEncoder, ScreenStreamerGo, Moblin, Android MediaProjection guidance, and AndroidX Media3 Transformer. Unictoos keeps the verified RootEncoder 2.8.0 pipeline in place rather than replacing the stable capture lifecycle with an untested engine migration. StreamPack and ScreenStreamerGo were used as architectural references; ScreenStreamerGo’s GPL-3.0 license means its code was not copied into the Apache-2.0 Unictoos application. Media3 is Apache-2.0 and is attributed in `THIRD_PARTY_NOTICES.md`.

## No backend or paid API required for local features

The new trim/export path, analytics, scene presentation controls, templates, local configuration backup, live telemetry, camera switching, notification actions, and existing recording workflows run on-device. They do not require an OAuth server, cloud account, paid API, relay service, or remote database.

## Explicit boundaries

True screen-plus-camera picture-in-picture still requires a shared-surface compositor that owns multiple producers and one encoder output. Multi-track recording, chapter-aware MP4 muxing, and advanced live PCM audio effects remain contracts until they can be implemented and tested against the actual capture path. Media3 audio processors are available for future local edit/export composition, but they are not silently inserted into live RootEncoder capture.

OAuth chat, events, moderation, metadata, scheduling, cloud backup, remote control, SRTLA/RIST bonding, WHIP signaling, and UVC/HDMI capture require provider credentials, relay/signaling infrastructure, pairing, or hardware. The app exposes capability status and integration contracts for these areas rather than fake success states.

## Validation

The source was checked with repository hygiene, the static feature smoke suite, JVM unit tests, Android lint, debug APK assembly, instrumentation APK assembly, and minified release assembly. Physical-device validation remains required for long-duration streaming, Media3 export against representative recordings, camera switching, two-destination fan-out, SRT interoperability, process-death persistence, thermal behavior, and provider-specific operation.
