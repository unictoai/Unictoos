# Unictoos alpha11 physical-device validation matrix

The sandbox validation is static and build-based. The following cases must be executed on Android 10+ hardware before relying on the app for a public broadcast.

| ID | Test | Expected result | Evidence to record |
|---|---|---|---|
| L-01 | Install alpha11 and cold-launch | App opens without crash; Home shows Executive Broadcast UI and logo | Device model, Android version, launch result |
| L-02 | Open all six tabs | Home, Scenes, Studio, Engage, Library, and Settings render without crash | Screen recording or screenshots |
| L-03 | Deny microphone permission | Go Live and Practice show a clear microphone requirement; no crash | Permission result and UI message |
| L-04 | Grant microphone permission | Preflight changes to ready and capture preparation proceeds | Permission result and service notification |
| L-05 | Screen capture consent | Consent flow appears; cancel returns safely; approve prepares screen capture | Consent result and logcat if failure |
| L-06 | Camera-only capture | Camera permission, preview preparation, Practice, and Go Live work independently of screen capture | Device camera model and result |
| L-07 | Start Practice with screen | No platform endpoint is contacted; local MP4 appears in Library | Recording filename and duration |
| L-08 | Start Practice with camera | Local-only session records and stops cleanly | Recording filename and duration |
| L-09 | Stop Practice | Session summary is stored; status returns to idle; foreground notification is removed | Library analytics row |
| L-10 | Play recording | Android media handler opens the MP4 through a content URI | Playback result |
| L-11 | Share recording | Chooser opens; receiving app can read the content URI; no raw path is exposed | Share result |
| L-12 | Rename and delete recording | Name changes safely; deletion removes the item from Library | Before/after Library state |
| L-13 | Persist scene changes | Toggle source, reorder layers, adjust opacity, restart app, and confirm values persist | Before/after screenshots |
| L-14 | Mark live moment | Button appears only when live; marker time matches elapsed session time; no crash | Marker count and timestamp |
| L-15 | Stop live session | Summary is stored with duration and bitrate; marker remains local | Library analytics state |
| L-16 | Mute/unmute | Audio source state and UI update promptly while stream continues | Audio observation |
| L-17 | Network interruption | Bounded reconnect state appears; retries stop after configured limit; error is actionable | Network action and elapsed retry times |
| L-18 | Long session | Run at least 30 minutes; monitor battery, thermal state, bitrate, FPS, dropped frames, and recording | Device temperature/battery and session summary |
| L-19 | Background/notification behavior | Leaving the app does not unexpectedly stop a permitted active session; notification remains clear | Background result |
| L-20 | YouTube/Twitch/Kick destination | Use test credentials only; confirm endpoint, auth errors, stop behavior, and platform dashboard guidance | Platform, ingest result, redacted logs |
| L-21 | Engage disconnected state | No fake chat/events appear; OAuth boundary messaging is clear | Engage screenshot |
| L-22 | FileProvider security | Playback/share works; direct file paths are not exposed to receiving apps | Intent/URI observation |
| L-23 | Thermal and battery warnings | Health center reports network, battery, and thermal labels without excessive UI polling | Health Center screenshot |
| L-24 | Rotation and process recreation | Portrait policy is respected; app recreation does not corrupt scenes or credentials | Device result |

## Safety rules

Use disposable or test platform keys, never publish credentials in bug reports, and redact stream keys, OAuth tokens, ingest URLs, private channel identifiers, and personal information from screenshots and logs. Do not use a public broadcast for first validation. Start with local Practice mode, then use an unlisted/private destination where the platform supports it.

## Failure report format

For any crash or failed session, record the alpha version, commit, APK SHA-256, device model, Android version, capture mode, platform, exact steps, timestamp, and a redacted `adb logcat` stack trace. The most useful artifact is the first exception and its `Caused by` chain, not a screenshot of the crash dialog.
