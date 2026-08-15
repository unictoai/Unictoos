# Unictoos v0.2.2 — Creator onboarding and stream formats

## Overview

Unictoos v0.2.2 builds on the stable v0.2.1 graphics path with a more complete launch experience. New users now see a short visual introduction before entering the broadcast workspace, and creators can choose between portrait 9:16 and landscape 16:9 formats directly from Studio setup.

## First-launch onboarding

The first launch now opens a four-page onboarding sequence with branded vertical artwork and exact in-app copy. The pages introduce streaming anywhere, professional scenes, reliable capture, and secure creator control. The flow includes progress indicators, Back, Next, Skip, and Get started actions. Completion is persisted in ordinary app preferences, so the sequence is not shown again on later launches unless app data is cleared.

The four 9:16 illustrations are included as local drawable resources. They use Unictoos’s charcoal, silver, and restrained electric-blue visual language and are presented with a readable gradient and app-rendered text rather than relying on artwork text for critical copy.

## Stream format options

Studio setup now exposes two functional format choices: **9:16 Portrait** and **16:9 Landscape**. The selected format updates the active scene, is persisted through the existing SceneStore, and is included in the existing scene payload and configuration export. The selected format is available before capture starts and is locked while a session is active.

The existing default scene behavior remains portrait-oriented for mobile-first creation, while the built-in Main Camera scene remains landscape-oriented. Creators can change either scene before starting a broadcast.

## Stability and preserved functionality

The v0.2.1 graphics protections remain included. These include the bounded local preview buffer, source-driven rendering without periodic ForceRenderer, generation-bound callbacks, serialized lifecycle state changes, idempotent failed-pipeline teardown, synchronous GL stop on render failure, duplicate preview callback suppression, and the Infinix X6853 live-preview isolation path. On the affected device, the local preview may pause after the stream becomes live while the encoder continues broadcasting.

Secure credential storage, existing authentication and reconnect policy, YouTube/Twitch/Kick/custom RTMP destinations, scenes, screen capture, camera capture, microphone controls, local recording, markers, practice mode, session health, and configuration export remain included. Simultaneous camera-plus-screen composition remains disclosed as a future compositor feature.

## Automated validation

The v0.2.2 source passed `lintDebug`, `testDebugUnitTest`, `assembleDebug`, `git diff --check`, and the complete feature smoke suite with 47/47 checks passing. The release APK is versionCode 28 and versionName 0.2.2.

## Physical-device test

This is an engineering/device-test release. Use a disposable Twitch test destination on the Infinix X6853. Confirm that the onboarding completes, select both 9:16 and 16:9 in separate test scenes, and verify a short stream before attempting a longer run. Do not use an important broadcast or an irreplaceable stream key until the format and graphics behavior have been confirmed on the physical device.
