# Unictoos Debug APK: Physical Device and Stream Ingestion Test Guide

This guide tests the current Unictoos alpha APK on a real Android device and verifies the screen-capture plus microphone stream path against YouTube, Twitch, or Kick.

> **Important alpha limitation:** The current build includes the native MediaProjection/RootEncoder streaming service and can test screen capture plus microphone ingestion. The Compose scene editor is currently the UI/domain foundation; camera compositing, full scene rendering, the recordings library UI, and platform OAuth/metadata integrations still require additional implementation and device validation. Treat the first test as an ingestion and service-lifecycle test, not as final proof that every scene feature is production-ready.

## 1. Prepare a safe test environment

Use a physical Android device running Android 10 or later. For the first test, use a device with at least 6 GB RAM, a stable Wi-Fi connection, the battery above 50%, and the device connected to power if possible. Close battery-saving modes and do not test during an important broadcast.

Use a **private, unlisted, or test stream** and a disposable or rotatable stream key. Never paste a real stream key into a public issue, screenshot, video, shell history, or chat message. After testing, rotate or revoke the key if there is any doubt that it was exposed.

The APK metadata for the current debug build is:

| Property | Value |
|---|---|
| Application ID | `com.unictoai.unictoos` |
| App label | Unictoos |
| Version | `0.1.0-alpha01` |
| Minimum Android version | Android 10 / API 29 |
| Target SDK | 35 |
| APK SHA-256 | `c2ed6498590b300552316eef54fefddf0d2082f14bdc676a72919ee421ab6c1f` |

## 2. Install the APK

### Option A: Install by tapping the APK

Download `app-debug.apk` to the Android device. Open it from Files or Downloads and allow the file manager or browser to install unknown apps if Android asks. Install or update Unictoos.

If an older development build is installed and the installation reports a signature or package conflict, uninstall the old build first. Uninstalling clears the app’s locally stored encrypted destination credentials.

### Option B: Install with ADB

On the computer, enable **Developer options** and **USB debugging** on the Android device:

1. Open Settings → About phone.
2. Tap Build number seven times until Developer options are enabled.
3. Open Developer options and enable USB debugging.
4. Connect the device with a data-capable USB cable.
5. Accept the computer-authorisation prompt on the device.

Verify the connection:

```bash
adb devices
```

The device should appear with status `device`, not `unauthorized`. Install the APK:

```bash
adb install -r app-debug.apk
```

If you need a clean reset:

```bash
adb uninstall com.unictoai.unictoos
adb install app-debug.apk
```

## 3. Configure the streaming destination

Open Unictoos → **Settings**. Enter the platform’s current ingest/server URL in **Server URL** and the current stream key in **Stream key**. The current build combines them as:

```text
serverUrl + "/" + streamKey
```

Therefore enter the server base URL without the stream key already appended. Use the RTMPS URL shown by the platform whenever it provides one. Do not guess a platform endpoint if the dashboard supplies a different regional or account-specific URL.

The current Settings screen stores the server URL and stream key using an Android Keystore-backed encryption key. The debug build is still alpha, so verify this behavior through code review and do not assume it replaces platform-side key rotation.

### YouTube Live

Create a test stream in YouTube Studio → Go live. Choose private or unlisted visibility. Open the stream settings and copy the platform-provided **Stream URL** and **Stream key**. Paste the Stream URL into Server URL and the Stream key into Stream key. Save the destination.

Keep YouTube Studio open in a browser so its preview, connection state, health panel, and stream diagnostics can be observed while Unictoos is broadcasting.

### Twitch

Open Creator Dashboard → Settings → Stream. Copy the current Twitch ingest/server URL and the Primary Stream Key. If Twitch provides more than one server option, use the recommended or geographically closest ingest server for the test. Paste the server URL and key into Unictoos and save.

Use a test category and title in Twitch. Confirm that the account’s stream key has not been reset since it was copied.

### Kick

Use Kick’s creator or streaming dashboard to obtain the current ingest/server URL and stream key. Kick’s ingest details can vary by account or dashboard state, so use the values currently shown to you instead of copying a stale endpoint from an old tutorial. Paste the server URL and key into Unictoos and save.

Use a test title/category and keep the stream private or otherwise limited if the platform permits it.

## 4. Run the first local device test

Before testing a live platform, verify that the app can start its Android capture path:

1. Open Unictoos → Studio.
2. Select the default scene.
3. Press **Go live**.
4. Approve the Android screen-capture consent dialog.
5. Approve microphone and notification permissions if requested.
6. Look for the Unictoos foreground notification indicating that broadcast controls are active.
7. Confirm that the Studio status changes from preparing/connecting to live or reports a clear error.
8. On the device, change to another app or open a simple static screen. This creates visible screen content for the test.

The current native service uses a 720×1280, 30 FPS, approximately 4.5 Mbps video profile and a 44.1 kHz microphone path. Device capabilities and platform acceptance may still vary. The current stream path is intended to test screen capture and microphone ingestion; it does not yet prove that the Compose scene editor’s visual layout is being rendered into the outgoing stream.

## 5. Verify platform ingestion

Use the platform dashboard and the Unictoos device at the same time. A successful test should satisfy all of the following:

| Check | Expected result |
|---|---|
| Permission flow | Android shows an explicit screen-capture consent dialog; no silent capture occurs |
| Foreground execution | A visible Unictoos notification remains while capture/streaming is active |
| Connection | The platform dashboard reports an incoming connection or healthy preview |
| Video | The dashboard preview shows the device screen changing when the device content changes |
| Audio | The microphone meter or preview audio responds when you speak, subject to platform preview delay |
| Status | Unictoos reports connecting/live and does not immediately fall back to an error state |
| Stability | The stream remains connected for at least 5–10 minutes without repeated reconnects |
| Stop | Pressing Stop ends the service and the platform eventually reports the stream ended |

For the first test, use the following sequence:

1. Start the test stream with the platform dashboard open.
2. Wait up to one minute for the platform preview to become available.
3. Speak a short test phrase and show a moving screen or timer.
4. Observe the platform health/preview page for at least five minutes.
5. Temporarily disable Wi-Fi or enable airplane mode for approximately 10 seconds, then restore the connection. Record whether the app reports an error, reconnects, or requires a manual restart.
6. Stop the stream from Unictoos.
7. Verify that the platform ends the session and, if the platform supports it, that the private/unlisted VOD or stream summary is present.

Do not publish the test stream until video, audio, stop behavior, and key handling have been verified.

## 6. Collect diagnostics if the test fails

On the computer, capture the Android log while reproducing the issue:

```bash
adb logcat -c
adb logcat -v time > unictoos-log.txt
```

Reproduce the failure, press Ctrl+C, and inspect the log locally. Search for relevant signals:

```bash
grep -Ei "Unictoos|MediaProjection|ForegroundService|SecurityException|RootEncoder|GenericStream|RTMP|RTMPS|auth|failed|disconnect" unictoos-log.txt
```

Before sharing any log, remove stream URLs, stream keys, account identifiers, email addresses, access tokens, and private IP addresses. The current build should not log stream keys; if one appears, stop testing, rotate the key, and report the exact redacted context privately.

Useful additional checks are:

```bash
adb shell dumpsys activity services com.unictoai.unictoos
adb shell dumpsys media_projection
adb shell dumpsys battery
adb shell dumpsys meminfo com.unictoai.unictoos
```

Common failure meanings:

| Symptom | Likely area to inspect |
|---|---|
| Projection dialog is cancelled or never appears | Android permission flow or activity lifecycle |
| Foreground-service `SecurityException` | Android service type, permission, or target-SDK requirement |
| “Destination rejected the stream key” | Wrong key, expired key, wrong server URL, or account-side restriction |
| Platform sees connection but no video | Encoder capability, projection source, orientation, or ingest compatibility |
| Video but no audio | Microphone permission, audio route, device privacy switch, or platform monitoring delay |
| Immediate disconnect | URL/key formatting, network/TLS, unsupported codec/profile, or server rejection |
| Device becomes hot or drops frames | Bitrate/resolution/FPS too high for the device; capture and encoder profiling needed |
| App status stays preparing | Callback propagation or service/engine lifecycle issue |

## 7. Test completion checklist

The first physical-device test is complete when you have recorded the following for each platform:

- Device model, Android version, RAM tier, connection type, and battery state.
- Platform name and whether the stream was private/unlisted/test-only.
- Server URL class used, without recording the secret key.
- Resolution, FPS, bitrate, and test duration.
- Whether video appeared in the platform preview.
- Whether microphone audio appeared.
- Whether the stream survived a short network interruption.
- Whether Stop ended the session cleanly.
- Redacted log excerpts for any failures.

Do not call the app production-ready until the YouTube, Twitch, and Kick tests pass on at least one representative physical device each, followed by longer-duration tests on more than one device tier.
