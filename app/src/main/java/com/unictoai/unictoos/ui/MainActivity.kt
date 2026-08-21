package com.unictoai.unictoos.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.widget.Toast
import androidx.core.content.FileProvider
import com.unictoai.unictoos.BuildConfig
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.unictoai.unictoos.ui.theme.UnictoosTheme

class MainActivity : ComponentActivity() {
    private var pendingEndpoint: String = ""
    private var pendingSceneJson: String = ""
    private var pendingCaptureMode: String = CAPTURE_SCREEN
    private var pendingPractice: Boolean = false
    private var previewSurfaceToken: Long = 0L
    private var activePreviewSurface: Surface? = null

    private val projectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) {
            pendingPractice = false
            return@registerForActivityResult
        }
        if (pendingCaptureMode == CAPTURE_CAMERA) {
            startCameraCapture()
            return@registerForActivityResult
        }
        val prepareIntent = Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_PREPARE_PROJECTION
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_RESULT_CODE, result.resultCode)
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_PROJECTION_DATA, result.data)
        }
        androidx.core.content.ContextCompat.startForegroundService(this, prepareIntent)
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = if (pendingPractice) {
                com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_START_PRACTICE
            } else {
                com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_START
            }
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_ENDPOINT, pendingEndpoint)
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_SCENE_JSON, pendingSceneJson)
        })
        pendingPractice = false
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val audioGranted = androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val cameraGranted = pendingCaptureMode != CAPTURE_CAMERA || androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (audioGranted && cameraGranted) {
            if (pendingCaptureMode == CAPTURE_CAMERA) startCameraCapture() else launchProjection()
        } else {
            Toast.makeText(this, "Microphone access is required to go live with audio", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        pendingEndpoint = savedInstanceState?.getString(KEY_PENDING_ENDPOINT).orEmpty()
        pendingSceneJson = savedInstanceState?.getString(KEY_PENDING_SCENE_JSON).orEmpty()
        pendingCaptureMode = savedInstanceState?.getString(KEY_PENDING_CAPTURE_MODE) ?: CAPTURE_SCREEN
        pendingPractice = savedInstanceState?.getBoolean(KEY_PENDING_PRACTICE) ?: false
        setContent {
            UnictoosTheme {
                UnictoosApp(
                    onRequestStreamStart = ::requestStreamStart,
                    onRequestPracticeStart = ::requestPracticeStart,
                    onStopStream = ::stopStream,
                    onReleaseCapture = ::releaseCapture,
                    onToggleMute = ::toggleMute,
                    onSwitchCamera = ::switchCamera,
                    onToggleRecording = ::toggleRecording,
                    onCreateMarker = ::createMarker,
                    onDismissStatusMessage = ::dismissStatusMessage,
                    onShareConfig = ::shareConfig,
                    onShareDiagnostics = ::shareDiagnostics,
                    onPreviewSurfaceAvailable = ::attachPreviewSurface,
                    onPreviewSurfaceDestroyed = ::detachPreviewSurface,
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_PENDING_ENDPOINT, pendingEndpoint)
        outState.putString(KEY_PENDING_SCENE_JSON, pendingSceneJson)
        outState.putString(KEY_PENDING_CAPTURE_MODE, pendingCaptureMode)
        outState.putBoolean(KEY_PENDING_PRACTICE, pendingPractice)
        super.onSaveInstanceState(outState)
    }

    internal fun requestStreamStart(endpoint: String, captureMode: String, sceneJson: String) {
        if (captureMode != CAPTURE_SCREEN && captureMode != CAPTURE_CAMERA) {
            Toast.makeText(this, "Enable a camera or screen source in the selected scene", Toast.LENGTH_LONG).show()
            return
        }
        if (endpoint.isBlank()) {
            Toast.makeText(this, "Add a streaming destination before going live", Toast.LENGTH_LONG).show()
            return
        }
        pendingEndpoint = endpoint
        pendingSceneJson = sceneJson
        pendingCaptureMode = captureMode
        pendingPractice = false
        val needsAudio = androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        val needsCamera = captureMode == CAPTURE_CAMERA && androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        if (needsAudio || needsCamera) {
            val permissions = buildList {
                add(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
                if (needsCamera) add(Manifest.permission.CAMERA)
            }
            permissionLauncher.launch(permissions.toTypedArray())
        } else if (captureMode == CAPTURE_CAMERA) {
            startCameraCapture()
        } else {
            launchProjection()
        }
    }

    internal fun startCameraCapture() {
        androidx.core.content.ContextCompat.startForegroundService(this, Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_PREPARE_CAMERA
        })
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = if (pendingPractice) {
                com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_START_PRACTICE
            } else {
                com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_START
            }
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_ENDPOINT, pendingEndpoint)
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_SCENE_JSON, pendingSceneJson)
        })
        pendingPractice = false
    }

    internal fun launchProjection() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    internal fun requestPracticeStart(captureMode: String, sceneJson: String) {
        if (captureMode != CAPTURE_SCREEN && captureMode != CAPTURE_CAMERA) {
            Toast.makeText(this, "Enable a camera or screen source in the selected scene", Toast.LENGTH_LONG).show()
            return
        }
        pendingEndpoint = ""
        pendingSceneJson = sceneJson
        pendingCaptureMode = captureMode
        pendingPractice = true
        val needsAudio = androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        val needsCamera = captureMode == CAPTURE_CAMERA && androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        if (needsAudio || needsCamera) {
            val permissions = buildList {
                add(Manifest.permission.RECORD_AUDIO)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
                if (needsCamera) add(Manifest.permission.CAMERA)
            }
            permissionLauncher.launch(permissions.toTypedArray())
        } else if (captureMode == CAPTURE_CAMERA) {
            startCameraCapture()
        } else {
            launchProjection()
        }
    }

    internal fun stopStream() {
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_STOP
        })
    }

    internal fun releaseCapture() {
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_RELEASE_CAPTURE
        })
    }

    internal fun toggleMute() {
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_TOGGLE_MUTE
        })
    }

    internal fun switchCamera() {
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_SWITCH_CAMERA
        })
    }

    internal fun toggleRecording(recording: Boolean) {
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = if (recording) {
                com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_STOP_RECORDING
            } else {
                com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_START_RECORDING
            }
        })
    }

    internal fun createMarker() {
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_CREATE_MARKER
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_MARKER_LABEL, "Marked moment")
        })
    }

    internal fun dismissStatusMessage() {
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_DISMISS_STATUS
        })
    }

    private fun attachPreviewSurface(surface: Surface, width: Int, height: Int) {
        val token = if (activePreviewSurface === surface && previewSurfaceToken > 0L) {
            previewSurfaceToken
        } else {
            previewSurfaceToken += 1L
            activePreviewSurface = surface
            previewSurfaceToken
        }
        activePreviewSurface = surface
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_ATTACH_PREVIEW
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_PREVIEW_SURFACE, surface)
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_PREVIEW_WIDTH, width)
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_PREVIEW_HEIGHT, height)
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_PREVIEW_TOKEN, token)
        })
    }

    private fun detachPreviewSurface(surface: Surface) {
        if (activePreviewSurface !== surface) return
        val token = previewSurfaceToken
        activePreviewSurface = null
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_DETACH_PREVIEW
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_PREVIEW_TOKEN, token)
        })
    }

    internal fun shareConfig(json: String) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, json)
        }, "Export Unictoos configuration"))
    }

    internal fun shareDiagnostics(json: String) {
        val file = File(cacheDir, "unictoos-diagnostics-${System.currentTimeMillis()}.json").apply {
            writeText(json)
        }
        val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Export Unictoos diagnostics"))
    }

    companion object {
        private const val CAPTURE_SCREEN = "screen"
        private const val CAPTURE_CAMERA = "camera"
        private const val KEY_PENDING_ENDPOINT = "pending_endpoint"
        private const val KEY_PENDING_SCENE_JSON = "pending_scene_json"
        private const val KEY_PENDING_CAPTURE_MODE = "pending_capture_mode"
        private const val KEY_PENDING_PRACTICE = "pending_practice"
    }
}
