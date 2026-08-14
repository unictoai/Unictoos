#!/usr/bin/env python3
"""Static smoke checks for the Unictoos alpha when no Android device is attached."""
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
MAIN = (ROOT / "app/src/main/java/com/unictoai/unictoos/MainActivity.kt").read_text()
SERVICE = (ROOT / "app/src/main/java/com/unictoai/unictoos/streaming/StreamingForegroundService.kt").read_text()
MANIFEST = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
APK = ROOT / "app/build/outputs/apk/debug/app-debug.apk"

checks = []
def check(name: str, condition: bool):
    checks.append((name, condition))

check("debug APK exists", APK.exists())
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
    "ACTION_STOP",
    "ACTION_TOGGLE_MUTE",
    "ACTION_START_RECORDING",
    "ACTION_STOP_RECORDING",
):
    check(f"service action: {action}", action in SERVICE)
for tab in ("HOME", "SCENES", "STUDIO", "ENGAGEMENT", "LIBRARY", "SETTINGS"):
    check(f"UI tab: {tab}", f"{tab}(\"" in MAIN)
check("camera capture path", "Camera2Source" in SERVICE and "CAPTURE_CAMERA" in MAIN)
check("screen capture path", "ScreenSource" in SERVICE and "CAPTURE_SCREEN" in MAIN)
check("secure credential path", "CredentialStore" in (ROOT / "app/src/main/java/com/unictoai/unictoos/StudioViewModel.kt").read_text())
check("recording library path", "filesDir, \"recordings\"" in MAIN)
check("preflight path", "PreflightCard" in MAIN and "ACCESS_NETWORK_STATE" in MANIFEST)
check("stale enum state is guarded", "firstOrNull { it.name == selectedPlatformName }" in MAIN)
check("empty scene state is guarded", "scenes.firstOrNull() ?: Scene(" in MAIN)
check("AudioRecord security is guarded", "catch (_: SecurityException)" in SERVICE)
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
