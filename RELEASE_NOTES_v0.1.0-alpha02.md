## Unictoos v0.1.0-alpha02

This update contains the first Phase 1 reliability and branding improvements for Unictoos.

### Changes since alpha01

- Integrated the supplied Unictoos logo into the Home UI, launcher identity, and Android 12+ splash screen.
- Improved microphone startup by checking `RECORD_AUDIO` permission before service preparation.
- Added an `AudioRecord` availability check before capture begins.
- Changed RootEncoder initialization to attach `MicrophoneSource` only after the permission and microphone checks pass.
- Added visible microphone states and explicit error messages when audio access is denied or unavailable.
- Added a guard against empty or invalid streaming destination endpoints.
- Kept the build compatible with Android 10 / API 29 and target SDK 35.

### APK details

- Application ID: `com.unictoai.unictoos`
- Version name: `0.1.0-alpha02`
- Build type: debug alpha
- Supported platform target: Android 10 and later

### Important testing note

This is still an alpha debug build. Install it on a physical Android device and verify microphone input, screen-capture consent, stream start/stop, network interruption behavior, and YouTube/Twitch/Kick ingest with a private or unlisted test stream before using it for an important broadcast.
