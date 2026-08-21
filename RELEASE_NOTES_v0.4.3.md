# Unictoos v0.4.3

## Local creator platform enhancement

Unictoos v0.4.3 implements the locally verifiable portion of the attached enhancement brief on top of the v0.4.2 broadcast baseline. The release keeps the mobile-first workflow and the generation-safe RootEncoder lifecycle while adding durable local analytics, richer scene authoring, and honest capability communication.

## New local features

Completed sessions are now written to a bounded SQLite database outside the media hot path. Library reads recent analytics and shows a local performance comparison covering session count, average duration, average bitrate, average dropped frames, and sessions with reconnect attempts. No credentials, destination URLs, OAuth tokens, or cloud identifiers are stored in this table.

Scenes now persist source groups, per-source group membership, transition mode, and bounded transition duration. The Scenes workspace exposes compact Cut, Fade, and Slide controls, reusable source-group creation, and group enable/disable actions. Existing v0.4.2 scene JSON remains readable through defaulted fields, and the quick templates now include useful grouping and transition defaults.

The release retains local MP4 recording, playback, sharing, and chapter-style stream markers. A safe recording trim policy validates local edit plans without claiming that an MP4 remuxer or multi-track exporter is bundled. Advanced audio validation covers noise-gate thresholds, compressor ratios, limiter ceilings, and equalizer bands while the active capture path remains RootEncoder’s verified microphone pipeline with echo cancellation and noise suppression.

More tools now includes a capability matrix that identifies available local features, device-validation requirements, integration-ready contracts, and external-service requirements. This prevents PiP, DSP, multi-track editing, OAuth, cloud, relay, and USB capabilities from appearing as falsely complete.

## Explicit boundaries

True screen-plus-camera PiP still requires a shared-surface compositor. Multi-track recording and post-session export still require a verified media editing implementation. Advanced PCM audio effects still require ownership of the pre-encode audio buffer. OAuth chat/events/moderation/metadata, cloud backup, remote control, SRTLA/RIST bonding, and UVC/HDMI capture remain behind contracts and physical or service validation.

## Validation

The release gate includes repository diff checks, the static smoke suite, JVM unit tests, Android lint, debug APK assembly, instrumentation APK assembly, and minified release assembly. A physical Android device remains necessary to validate long-duration capture, PiP, camera switching, two-destination fan-out, SRT listener interoperability, notification actions, SQLite persistence after process death, and thermal/battery behavior.

Version: `0.4.3`
Android version code: `44`
Minimum SDK: `29`
Target SDK: `36`
Compile SDK: `37`
RootEncoder: `2.8.0`
