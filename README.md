# Unictoos

[![Android build](https://github.com/unictoai/Unictoos/actions/workflows/android.yml/badge.svg)](https://github.com/unictoai/Unictoos/actions/workflows/android.yml)
[![Release](https://img.shields.io/github/v/release/unictoai/Unictoos)](https://github.com/unictoai/Unictoos/releases)
[![License](https://img.shields.io/github/license/unictoai/Unictoos)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%2010%2B-3DDC84)](https://github.com/unictoai/Unictoos)
[![Kotlin](https://img.shields.io/badge/kotlin-2.x-7F52FF)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)](https://developer.android.com/jetpack/compose)

**Unictoos** is a free, open-source Android live-streaming studio for creators. Broadcast from your phone to **YouTube, Twitch, Kick**, or any custom **RTMP / RTMPS / SRT** destination — with scenes, local MP4 recording, and a focused touch-first Studio screen.

> ⚠️ **Alpha software.** Unictoos v0.5.3 compiles and packages cleanly and all automated checks pass, but real-device ingest validation (YouTube / Twitch / Kick) and endurance testing are still in progress. Do not trust an important broadcast or an irreplaceable stream key to a development build yet.

## ✨ Features

| Area | What you get |
|---|---|
| 📡 Streaming | RTMP / RTMPS / SRT via RootEncoder, bounded two-destination multistream fan-out, reconnects with backoff |
| 🎬 Studio | Live preview, bitrate/FPS telemetry, mute, local recording, camera switching, actionable notifications |
| 🎭 Scenes | Portrait/landscape scenes, source toggles, source groups, templates, safe config import |
| 🔐 Destinations | Per-platform credential slots (YouTube, Twitch, Kick, Custom) encrypted with Android Keystore |
| 🎞️ Library | Local MP4 recordings, session recaps, markers, trim & export |
| 🛡️ Reliability | Preflight checks, permission guards, capture readiness, redacted support diagnostics |
| 🧪 Experimental | Screen + camera picture-in-picture (bounded filter path, screen-only fallback) |

## 🚀 Quick start

1. Open **Settings** → pick **YouTube, Twitch, Kick, or Custom** → enter the server URL and stream key from that platform's dashboard. (For SRT use Custom with a full `srt://` URL and leave the key blank.)
2. Open **Go Live** → approve screen-capture / mic / camera permissions when Android asks.
3. Press **Go Live**. Mute and record from the Studio surface. If the connection drops, the app retries with backoff and tells you what failed.

> 🔑 Stream keys are encrypted on-device with the Android Keystore and are never logged, exported, or committed.

## 🛠️ Build locally

Requires **JDK 21**, **Android SDK Platform 35+**, and a physical device (Android 10+) for capture testing.

```bash
./gradlew test assembleDebug        # unit tests + debug APK
./gradlew lintDebug                  # Android lint
python3 tools/feature_smoke_test.py  # 155 static feature checks
python3 tools/security_source_audit.py
```

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 🧱 Architecture

```
Compose UI (Home · Studio · Scenes · Library · Settings)
        │  observes
StreamSessionState  ←──  StreamingStatusBus (single source of truth)
        │
StreamingForegroundService ── owns ──► MediaProjection / Camera / Mic
                                     ► RootEncoder (RTMP/RTMPS/SRT)
                                     ► MP4 recording, reconnects, notifications
CredentialStore (AES/GCM + Android Keystore)
```

One shared encoder fans out to at most two destinations. The UI never owns stream state — it mirrors the service's status bus.

## 🗺️ Roadmap

- [ ] Real-device ingest validation: YouTube, Twitch, Kick
- [ ] 60-minute endurance + thermal/battery sessions
- [ ] Independent per-destination health & retry policy
- [ ] Chat/alerts integrations, scheduling, thumbnails
- [ ] Stable 1.0 release

See [`FEATURE_ROADMAP.md`](FEATURE_ROADMAP.md) and [`RELEASE_NOTES_v0.5.3.md`](RELEASE_NOTES_v0.5.3.md) for detail.

## 🤝 Contributing

Contributions are welcome! Please read [`CONTRIBUTING.md`](CONTRIBUTING.md) and [`SECURITY.md`](SECURITY.md) first. Bug reports and feature ideas go in [Issues](https://github.com/unictoai/Unictoos/issues).

## 📄 License

Apache-2.0 — see [LICENSE](LICENSE). Third-party components (including [RootEncoder](https://github.com/pedroSG94/RootEncoder)) keep their own licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
