#!/usr/bin/env python3
"""Static smoke checks for the Unictoos alpha when no Android device is attached."""
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
MAIN = (ROOT / "app/src/main/java/com/unictoai/unictoos/ui/MainActivity.kt").read_text()
UI = "\n".join(path.read_text() for path in (ROOT / "app/src/main/java/com/unictoai/unictoos/ui").rglob("*.kt"))
SERVICE = (ROOT / "app/src/main/java/com/unictoai/unictoos/streaming/StreamingForegroundService.kt").read_text()
MANIFEST = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
APK = ROOT / "app/build/outputs/apk/debug/app-debug.apk"

checks = []
def check(name: str, condition: bool):
    checks.append((name, condition))

check("debug APK exists", APK.exists())
check("screen file organization", (ROOT / "app/src/main/java/com/unictoai/unictoos/ui/MainActivity.kt").exists() and not (ROOT / "app/src/main/java/com/unictoai/unictoos/MainActivity.kt").exists())
check("contributor guidance", (ROOT / "CONTRIBUTING.md").exists() and "one-file-per-screen" in (ROOT / "CONTRIBUTING.md").read_text())
check("launch activity declared", ".MainActivity" in MANIFEST and "android.intent.action.MAIN" in MANIFEST)
for permission in (
    "android.permission.CAMERA",
    "android.permission.RECORD_AUDIO",
    "android.permission.ACCESS_NETWORK_STATE",
    "android.permission.FOREGROUND_SERVICE",
    "android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION",
    "android.permission.FOREGROUND_SERVICE_MICROPHONE",
):
    check(f"manifest permission: {permission}", permission in MANIFEST)
for action in (
    "ACTION_PREPARE_PROJECTION",
    "ACTION_PREPARE_CAMERA",
    "ACTION_START",
    "ACTION_START_PRACTICE",
    "ACTION_STOP",
    "ACTION_TOGGLE_MUTE",
    "ACTION_START_RECORDING",
    "ACTION_STOP_RECORDING",
):
    check(f"service action: {action}", action in SERVICE)
for tab in ("HOME", "SCENES", "STUDIO", "ENGAGEMENT", "LIBRARY", "SETTINGS"):
    check(f"UI tab: {tab}", f"{tab}(\"" in UI)
check("camera capture path", "Camera2Source" in SERVICE and "CAPTURE_CAMERA" in MAIN)
check("screen capture path", "ScreenSource" in SERVICE and "CAPTURE_SCREEN" in MAIN)
check("secure credential path", "CredentialStore" in (ROOT / "app/src/main/java/com/unictoai/unictoos/StudioViewModel.kt").read_text())
check("scene persistence path", "SceneStore" in (ROOT / "app/src/main/java/com/unictoai/unictoos/StudioViewModel.kt").read_text() and (ROOT / "app/src/main/java/com/unictoai/unictoos/data/SceneStore.kt").exists())
check("engagement model boundary", (ROOT / "app/src/main/java/com/unictoai/unictoos/domain/EngagementModels.kt").exists() and "OAuth" in UI)
check("integration adapter boundary", (ROOT / "app/src/main/java/com/unictoai/unictoos/integrations/PlatformIntegration.kt").exists() and "StreamMetadataRequest" in (ROOT / "app/src/main/java/com/unictoai/unictoos/integrations/PlatformIntegration.kt").read_text())
check("creator history store", (ROOT / "app/src/main/java/com/unictoai/unictoos/data/CreatorHistoryStore.kt").exists() and "SessionSummary" in (ROOT / "app/src/main/java/com/unictoai/unictoos/data/CreatorHistoryStore.kt").read_text())
check("stream marker path", "ACTION_CREATE_MARKER" in SERVICE and "Mark moment" in UI)
check("platform dashboard links", "dashboard.twitch.tv" in UI and "dashboard.kick.com" in UI)
check("recording library path", "filesDir, \"recordings\"" in UI)
check("recording playback and share path", "FileProvider.getUriForFile" in UI and "ACTION_SEND" in UI)
check("file provider declared", "androidx.core.content.FileProvider" in MANIFEST and (ROOT / "app/src/main/res/xml/file_paths.xml").exists())
check("practice mode path", "ACTION_START_PRACTICE" in MAIN and "SessionMode.PRACTICE" in SERVICE)
check("health history path", "healthHistory" in UI and "recordHealth" in SERVICE)
check("preflight path", "PreflightCard" in UI and "ACCESS_NETWORK_STATE" in MANIFEST)
check("stale enum state is guarded", "firstOrNull { it.name == selectedPlatformName }" in UI)
check("empty scene state is guarded", "scenes.firstOrNull() ?: Scene(" in UI)
check("AudioRecord security is guarded", "catch (_: SecurityException)" in SERVICE)
check("microphone probe is off main thread", "withContext(Dispatchers.IO)" in SERVICE and "serviceScope.cancel()" in SERVICE)
check("foreground start is guarded", "startForegroundSafely" in SERVICE and "catch (error: SecurityException)" in SERVICE)
check("backup rules exist", (ROOT / "app/src/main/res/xml/data_extraction_rules.xml").exists())

if APK.exists():
    aapt = Path("/home/ubuntu/android-sdk/build-tools/35.0.0/aapt")
    if aapt.exists():
        output = subprocess.check_output([str(aapt), "dump", "badging", str(APK)], text=True)
        check("APK package id", "com.unictoai.unictoos" in output)
        check("APK has launchable activity", "launchable-activity:" in output)

failed = [name for name, ok in checks if not ok]
for name, ok in checks:
    print(f"{'PASS' if ok else 'FAIL'}: {name}")
print(f"\n{len(checks) - len(failed)}/{len(checks)} checks passed")
if failed:
    print("Failures:")
    print("\n".join(f"- {name}" for name in failed))
    sys.exit(1)
