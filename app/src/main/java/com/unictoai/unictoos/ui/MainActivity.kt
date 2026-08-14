package com.unictoai.unictoos.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.unictoai.unictoos.ui.theme.UnictoosTheme

class MainActivity : ComponentActivity() {
    private var pendingEndpoint: String = ""
    private var pendingCaptureMode: String = CAPTURE_SCREEN
    private var pendingPractice: Boolean = false

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
        super.onCreate(savedInstanceState)
        setContent {
            UnictoosTheme {
                UnictoosApp(
                    onRequestStreamStart = ::requestStreamStart,
                    onRequestPracticeStart = ::requestPracticeStart,
                    onStopStream = ::stopStream,
                    onToggleMute = ::toggleMute,
                    onToggleRecording = ::toggleRecording,
                    onCreateMarker = ::createMarker,
                    onDismissStatusMessage = ::dismissStatusMessage,
                    onShareConfig = ::shareConfig,
                    onPreviewSurfaceAvailable = ::attachPreviewSurface,
                    onPreviewSurfaceDestroyed = ::detachPreviewSurface,
                )
            }
        }
    }

    internal fun requestStreamStart(endpoint: String, captureMode: String) {
        if (endpoint.isBlank()) {
            Toast.makeText(this, "Add a streaming destination before going live", Toast.LENGTH_LONG).show()
            return
        }
        pendingEndpoint = endpoint
        pendingCaptureMode = captureMode
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
        })
        pendingPractice = false
    }

    internal fun launchProjection() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    internal fun requestPracticeStart(captureMode: String) {
        pendingEndpoint = ""
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

    internal fun toggleMute() {
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_TOGGLE_MUTE
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
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_ATTACH_PREVIEW
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_PREVIEW_SURFACE, surface)
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_PREVIEW_WIDTH, width)
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_PREVIEW_HEIGHT, height)
        })
    }

    private fun detachPreviewSurface(surface: Surface) {
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_DETACH_PREVIEW
        })
    }

    internal fun shareConfig(json: String) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, json)
        }, "Export Unictoos configuration"))
    }

    companion object {
        private const val CAPTURE_SCREEN = "screen"
        private const val CAPTURE_CAMERA = "camera"
    }
}
