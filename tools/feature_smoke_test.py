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
RELEASE_BLOCK = SERVICE.split("private fun releaseGenericStream", 1)[1].split("override fun onCreate", 1)[0]
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
    "android.permission.FOREGROUND_SERVICE_CAMERA",
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
check("camera foreground service type is declared", 'android:foregroundServiceType="mediaProjection|microphone|camera"' in MANIFEST and "FOREGROUND_SERVICE_TYPE_CAMERA" in SERVICE)
check("capture preparation is serialized", "capturePreparationMutex.withLock" in SERVICE and "private val capturePreparationMutex = Mutex()" in SERVICE)
check("failed release remains retryable", "PipelineReleasePolicy.complete" in RELEASE_BLOCK and "pipelineReleaseState" in RELEASE_BLOCK and "genericStreamReleased = pipelineReleaseState == PipelineReleaseState.TERMINAL" in RELEASE_BLOCK)
ADAPTER = (ROOT / "app/src/main/java/com/unictoai/unictoos/streaming/SingleDestinationMultiStreamAdapter.kt").read_text()
check("runtime uses bounded MultiStream adapter", "SingleDestinationMultiStreamAdapter" in SERVICE and "MultiStream(" in ADAPTER and "MAX_DESTINATIONS = 2" in ADAPTER)
check("runtime adapter does not construct three encoders", "import com.pedro.library.generic.GenericStream" not in SERVICE and "= GenericStream(" not in SERVICE and "Array(MAX_DESTINATIONS)" in ADAPTER)
check("adapter release preserves retryability", "closed.set(false)" in ADAPTER and "firstFailure" in ADAPTER and "throw it" in ADAPTER)
check("backup rules exist", (ROOT / "app/src/main/res/xml/data_extraction_rules.xml").exists())
SUPPORT_EXPORT = ROOT / "app/src/main/java/com/unictoai/unictoos/streaming/SupportabilityExport.kt"
COMPATIBILITY = ROOT / "app/src/main/java/com/unictoai/unictoos/streaming/DeviceCompatibilityReport.kt"
PREFLIGHT = ROOT / "app/src/main/java/com/unictoai/unictoos/streaming/PreflightOutcome.kt"
SETTINGS = (ROOT / "app/src/main/java/com/unictoai/unictoos/ui/screens/SettingsScreen.kt").read_text()
LIBRARY = (ROOT / "app/src/main/java/com/unictoai/unictoos/ui/screens/LibraryScreen.kt").read_text()
check("supportability export contract", SUPPORT_EXPORT.exists() and "unictoos-support-bundle-v1" in SUPPORT_EXPORT.read_text() and "[REDACTED]" not in SUPPORT_EXPORT.read_text())
check("compatibility report contract", COMPATIBILITY.exists() and "DeviceCompatibilityReportFactory" in COMPATIBILITY.read_text() and "DIRECT_DESTINATION_CAP" in COMPATIBILITY.read_text())
check("preflight outcome contract", PREFLIGHT.exists() and "PreflightOutcomeEvaluator" in PREFLIGHT.read_text() and "ACTION_REQUIRED" in PREFLIGHT.read_text())
check("diagnostic export UI wiring", "onExportDiagnostics" in SETTINGS and "SupportabilityExport" in (ROOT / "app/src/main/java/com/unictoai/unictoos/ui/UnictoosApp.kt").read_text())
check("session timeline UI", "Session timeline" in LIBRARY and "StreamingDiagnostics.snapshot" in LIBRARY)
APP = (ROOT / "app/src/main/java/com/unictoai/unictoos/ui/UnictoosApp.kt").read_text()
check("glassy top navigation", "GlassyTopBar" in APP and "DropdownMenu" in APP and "AppTab.SETTINGS" in APP)
check("top bar uses Scaffold slot", "topBar = {" in APP and "padding(top = 72.dp)" not in APP)
check("top bar respects safe bounds", "statusBarsPadding()" in APP and "heightIn(min = 52.dp)" in APP and "TextOverflow.Ellipsis" in APP)
check("route-local state ownership", "private fun SettingsRoute" in APP and "val session by vm.session.collectAsStateWithLifecycle()" in APP)
check("reduced shell animation cost", "fadeIn(tween(120))" in APP and "animateContentSize" not in APP)
check("queued stream start is preserved", "acceptsQueuedStart" in SERVICE and "pendingStart = request" in SERVICE)
check("silent connection watchdog", "StreamStartupPolicy" in SERVICE and "scheduleConnectionWatchdog" in SERVICE and "CONNECTION_TIMEOUT_MS" in SERVICE)
check("connection watchdog is cancelled", "cancelConnectionWatchdog()" in SERVICE and "onConnectionSuccessForGeneration" in SERVICE and "onConnectionFailedForGeneration" in SERVICE)
check("silent start timeout regression", "silentConnectingStartTimesOutOnlyForCurrentGeneration" in (ROOT / "app/src/test/java/com/unictoai/unictoos/streaming/StreamFailurePolicyTest.kt").read_text())
check("operator go-live readiness policy", "object GoLiveReadinessPolicy" in (ROOT / "app/src/main/java/com/unictoai/unictoos/streaming/GoLiveReadinessPolicy.kt").read_text() and "GoLiveReadinessCard" in (ROOT / "app/src/main/java/com/unictoai/unictoos/ui/screens/StudioScreen.kt").read_text())
check("go-live readiness regression", "cameraModeRequiresCameraPermissionButScreenModeDoesNot" in (ROOT / "app/src/test/java/com/unictoai/unictoos/streaming/GoLiveReadinessPolicyTest.kt").read_text())
check("empty capture source is blocked", 'else -> "none"' in (ROOT / "app/src/main/java/com/unictoai/unictoos/ui/UnictoosApp.kt").read_text() and "Enable a camera or screen source" in (ROOT / "app/src/main/java/com/unictoai/unictoos/ui/MainActivity.kt").read_text() and "noCaptureSourceIsBlocking" in (ROOT / "app/src/test/java/com/unictoai/unictoos/streaming/GoLiveReadinessPolicyTest.kt").read_text())
check("stream key masked by default", "PasswordVisualTransformation" in (ROOT / "app/src/main/java/com/unictoai/unictoos/ui/screens/SettingsScreen.kt").read_text() and "Hide stream key" in (ROOT / "app/src/main/java/com/unictoai/unictoos/ui/screens/SettingsScreen.kt").read_text())
check("camera switch action", "ACTION_SWITCH_CAMERA" in SERVICE and "switchCamera()" in SERVICE and "onSwitchCamera" in UI)
check("actionable broadcast notification", "PendingIntent.getService" in SERVICE and '"Stop"' in SERVICE and '"Mute"' in SERVICE)
IMPORTER = (ROOT / "app/src/main/java/com/unictoai/unictoos/data/ConfigImporter.kt").read_text()
check("safe scene-only configuration import", "onImportConfig" in SETTINGS and "destinations were unchanged" in APP and "streamKey" not in IMPORTER)
check("scene template workflow", "addSceneTemplate" in (ROOT / "app/src/main/java/com/unictoai/unictoos/StudioViewModel.kt").read_text() and "Quick templates" in (ROOT / "app/src/main/java/com/unictoai/unictoos/ui/screens/ScenesScreen.kt").read_text())
check("live telemetry indicator", "LiveTelemetryCard" in (ROOT / "app/src/main/java/com/unictoai/unictoos/ui/screens/StudioScreen.kt").read_text() and "networkLabel" in (ROOT / "app/src/main/java/com/unictoai/unictoos/ui/screens/StudioScreen.kt").read_text())
check("post-session recap", "Last session recap" in LIBRARY and "SessionSummary" in LIBRARY)
ENDPOINT_POLICY = (ROOT / "app/src/main/java/com/unictoai/unictoos/streaming/StreamEndpointPolicy.kt").read_text()
EXTERNAL_CONTRACTS = (ROOT / "app/src/main/java/com/unictoai/unictoos/integrations/ExternalFeatureContracts.kt").read_text()
check("SRT transport contract", "MultiType.SRT" in ADAPTER and "srt://" in ENDPOINT_POLICY)
check("bounded direct multistream selection", "DIRECT_DESTINATION_CAP" in (ROOT / "app/src/main/java/com/unictoai/unictoos/domain/MultistreamModels.kt").read_text() and "take(2)" in (ROOT / "app/src/main/java/com/unictoai/unictoos/StudioViewModel.kt").read_text())
check("external integration contracts", "CloudBackupProvider" in EXTERNAL_CONTRACTS and "RemoteControlTransport" in EXTERNAL_CONTRACTS and "BondingRelayProvider" in EXTERNAL_CONTRACTS and "ExternalVideoInputProvider" in EXTERNAL_CONTRACTS)
ANALYTICS_STORE = (ROOT / "app/src/main/java/com/unictoai/unictoos/data/LocalAnalyticsStore.kt").read_text()
SCENE_PRESENTATION = (ROOT / "app/src/main/java/com/unictoai/unictoos/domain/ScenePresentation.kt").read_text()
AUDIO_POLICY = (ROOT / "app/src/main/java/com/unictoai/unictoos/streaming/AudioProcessingPolicy.kt").read_text()
RECORDING_EDIT_POLICY = (ROOT / "app/src/main/java/com/unictoai/unictoos/streaming/RecordingEditPolicy.kt").read_text()
MEDIA3_EDITOR = (ROOT / "app/src/main/java/com/unictoai/unictoos/data/Media3RecordingEditor.kt").read_text()
BUILD_GRADLE = (ROOT / "app/build.gradle.kts").read_text()
check("local SQLite analytics", "SQLiteOpenHelper" in ANALYTICS_STORE and "streaming_sessions" in ANALYTICS_STORE and "MAX_RECORDS = 120" in ANALYTICS_STORE)
check("scene presentation persistence", "sourceGroups" in SCENE_PRESENTATION and "SceneTransitionMode" in SCENE_PRESENTATION and "transitionDurationMs" in (ROOT / "app/src/main/java/com/unictoai/unictoos/data/SceneStore.kt").read_text())
check("recording edit safety policy", "validateTrim" in RECORDING_EDIT_POLICY and "Trim end must be after trim start" in RECORDING_EDIT_POLICY)
check("advanced audio validation contract", "AudioProcessingProfile" in AUDIO_POLICY and "MAX_EQ_BANDS" in AUDIO_POLICY)
check("real local Media3 editor", "androidx.media3:media3-transformer:1.11.0" in BUILD_GRADLE and "Transformer.Builder" in MEDIA3_EDITOR and 'Text("Trim")' in LIBRARY)
check("glassy three-item bottom navigation", "GlassyBottomBar" in APP and "listOf(AppTab.HOME, AppTab.STUDIO, AppTab.LIBRARY)" in APP)
check("secondary workspace menu", "AppTab.SCENES" in APP and "AppTab.ENGAGEMENT" in APP and "AppTab.MORE" in APP)
STREAMING_CONTRACTS = (ROOT / "app/src/main/java/com/unictoai/unictoos/streaming/StreamingStateContracts.kt").read_text()
DESTINATION_MANAGER = (ROOT / "app/src/main/java/com/unictoai/unictoos/streaming/DestinationSessionManager.kt").read_text()
SERVICE_SOURCE = SERVICE
SCENE_CODEC = (ROOT / "app/src/main/java/com/unictoai/unictoos/streaming/ScenePayloadCodec.kt").read_text()
PLATFORM_CAPABILITIES = (ROOT / "app/src/main/java/com/unictoai/unictoos/domain/PlatformCapabilities.kt").read_text()
DIAGNOSTICS = (ROOT / "app/src/main/java/com/unictoai/unictoos/streaming/StreamingDiagnostics.kt").read_text()
THERMAL = (ROOT / "app/src/main/java/com/unictoai/unictoos/streaming/ThermalProtectionPolicy.kt").read_text()
check("authoritative recording state", "recordingState" in (ROOT / "app/src/main/java/com/unictoai/unictoos/domain/StudioModels.kt").read_text() and "RecordingReadinessPolicy" in SERVICE_SOURCE)
check("preview-independent recording policy", "captureReady && encoderReady" in (ROOT / "app/src/main/java/com/unictoai/unictoos/domain/RecordingState.kt").read_text() and "!previewAttached" not in SERVICE_SOURCE.split("private fun startRecording", 1)[1].split("private fun stopRecording", 1)[0])
check("destination isolation foundation", "class DestinationSessionManager" in DESTINATION_MANAGER and "markFailure" in DESTINATION_MANAGER and "StreamingError" in DESTINATION_MANAGER)
check("bounded destination retry", "maximumReconnectAttempts" in DESTINATION_MANAGER and "DestinationState.RECONNECTING" in DESTINATION_MANAGER)
check("versioned scene persistence", 'put("schemaVersion"' in SCENE_CODEC and "MAX_SOURCES" in SCENE_CODEC)
check("truthful provider capabilities", "REQUIRES_BACKEND" in PLATFORM_CAPABILITIES and "STREAM_KEY_READY" in PLATFORM_CAPABILITIES)
check("thermal debounce policy", "DEFAULT_DEBOUNCE_SECONDS" in THERMAL and "highThermalSinceElapsed" in SERVICE_SOURCE)
check("structured error boundary", "sealed interface StreamingError" in STREAMING_CONTRACTS and "AuthenticationFailed" in STREAMING_CONTRACTS)
check("diagnostic secret redaction", "Bearer" in DIAGNOSTICS and "ENDPOINT_REDACTED" in DIAGNOSTICS)
PREVIEW_SURFACE = (ROOT / "app/src/main/java/com/unictoai/unictoos/ui/PreviewSurfaceView.kt").read_text()
RELEASE_POLICY = (ROOT / "app/src/main/java/com/unictoai/unictoos/streaming/PipelineReleasePolicy.kt").read_text()
BUILD_GRADLE = (ROOT / "app/build.gradle.kts").read_text()
ROOT_BUILD_GRADLE = (ROOT / "build.gradle.kts").read_text()
APP_BUILD_GRADLE = (ROOT / "app/src/main/java/com/unictoai/unictoos/UnictoosApplication.kt").read_text()
check("RootEncoder 2.8.0 dependency", "RootEncoder:library:2.8.0" in BUILD_GRADLE and "compileSdk = 37" in BUILD_GRADLE)
check("AGP 9.3.1 toolchain", 'com.android.application") version "9.3.1"' in ROOT_BUILD_GRADLE)
check("Kotlin 2.4.10 toolchain", 'org.jetbrains.kotlin.plugin.compose") version "2.4.10"' in ROOT_BUILD_GRADLE and 'id("org.jetbrains.kotlin.android")' not in ROOT_BUILD_GRADLE and 'id("org.jetbrains.kotlin.android")' not in BUILD_GRADLE)
check("Gradle 9.5 wrapper", "gradle-9.5.0-bin.zip" in (ROOT / "gradle/wrapper/gradle-wrapper.properties").read_text())
check("Compose 2026.08 BOM", "compose-bom:2026.08.00" in BUILD_GRADLE)
check("Android target SDK 36", "targetSdk = 36" in BUILD_GRADLE)
check("v0.4.4 release metadata", 'versionName = "0.4.4"' in BUILD_GRADLE and "versionCode = 45" in BUILD_GRADLE and (ROOT / "RELEASE_NOTES_v0.4.4.md").exists() and (ROOT / "docs/V0.4_RESEARCH_AND_PRODUCT_PLAN.md").exists())
check("release resource shrinking", "isShrinkResources = true" in BUILD_GRADLE)
check("explicit terminal release boundary", "PipelineReleaseState.TERMINAL" in RELEASE_POLICY and "canCreateNewPipeline" in SERVICE_SOURCE)
check("generation-safe graphics recovery", "EXTRA_PIPELINE_GENERATION" in SERVICE_SOURCE and "pipelineGeneration" in APP_BUILD_GRADLE and "isCurrentGeneration" in SERVICE_SOURCE)
check("no fixed graphics settle delay", "GL_PIPELINE_SHUTDOWN_SETTLE_MS" not in SERVICE_SOURCE)
check("single holder callback registration", PREVIEW_SURFACE.count("holder.addCallback(callback)") == 1 and "registerHolderCallback()" not in PREVIEW_SURFACE.split("init", 1)[1].split("}", 1)[0])
check("no synthetic surface destruction", "onSurfaceDestroyed(surface)" not in PREVIEW_SURFACE.split("fun releasePreviewListener", 1)[1].split("}", 1)[0])
PREVIEW_POLICY = (ROOT / "app/src/main/java/com/unictoai/unictoos/streaming/PreviewSurfaceIdentityPolicy.kt").read_text()
MAIN_ACTIVITY = (ROOT / "app/src/main/java/com/unictoai/unictoos/ui/MainActivity.kt").read_text()
check("preview surface identity replacement", "PreviewSurfaceIdentityPolicy.shouldReuse" in SERVICE_SOURCE and "sameSurfaceObject" in PREVIEW_POLICY and "EXTRA_PREVIEW_TOKEN" in MAIN_ACTIVITY)
check("stale preview detach protection", "isStaleDetach" in SERVICE and "activePreviewSurface !== surface" in MAIN_ACTIVITY)
check("tokenless detach cannot detach active preview", "activeToken > 0L && detachToken != activeToken" in PREVIEW_POLICY)
check("saved destinations hydrate on startup", "hydrateSavedDestinations" in (ROOT / "app/src/main/java/com/unictoai/unictoos/StudioViewModel.kt").read_text())
check("projection callback reset per generation", "intentionallyReleasingProjection = false" in SERVICE_SOURCE and "projection.registerCallback" in SERVICE_SOURCE)
check("repeated lifecycle test exists", "repeat(50)" in (ROOT / "app/src/test/java/com/unictoai/unictoos/streaming/PipelineReleasePolicyTest.kt").read_text() and (ROOT / "app/src/androidTest/java/com/unictoai/unictoos/ui/PreviewSurfaceViewLifecycleTest.kt").exists())

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
