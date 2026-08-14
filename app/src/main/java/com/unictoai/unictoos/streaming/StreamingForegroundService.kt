package com.unictoai.unictoos.streaming

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.pedro.common.ConnectChecker
import com.pedro.library.base.recording.RecordController
import com.pedro.library.generic.GenericStream
import com.pedro.library.util.sources.audio.MicrophoneSource
import com.pedro.library.util.sources.audio.NoAudioSource
import com.pedro.library.util.sources.video.Camera2Source
import com.pedro.library.util.sources.video.NoVideoSource
import com.pedro.library.util.sources.video.ScreenSource
import com.unictoai.unictoos.R
import com.unictoai.unictoos.domain.StreamSessionState
import com.unictoai.unictoos.domain.StreamStatus
import java.io.File

class StreamingForegroundService : Service(), ConnectChecker {
    private lateinit var genericStream: GenericStream
    private var mediaProjection: MediaProjection? = null
    private var microphoneSource: MicrophoneSource? = null
    private var cameraSource: Camera2Source? = null
    private var prepared = false
    private var captureReady = false
    private var manualStop = false
    private var currentEndpoint: String = ""
    private var reconnectAttempt = 0
    private var startedAtElapsed = 0L

    private val handler = Handler(Looper.getMainLooper())
    private val projectionManager: MediaProjectionManager by lazy {
        getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    private val elapsedTicker = object : Runnable {
        override fun run() {
            if (startedAtElapsed > 0L) {
                val elapsed = (SystemClock.elapsedRealtime() - startedAtElapsed) / 1_000L
                val previous = StreamingStatusBus.state.value
                StreamingStatusBus.update(previous.copy(elapsedSeconds = elapsed))
                updateNotification("Live for ${formatElapsed(elapsed)}")
                handler.postDelayed(this, 1_000L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        genericStream = GenericStream(this, this, NoVideoSource(), NoAudioSource()).apply {
            getGlInterface().setForceRender(true, VIDEO_FPS)
        }
        prepared = runCatching {
            genericStream.prepareVideo(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_BITRATE, rotation = 0) &&
                genericStream.prepareAudio(AUDIO_SAMPLE_RATE, false, AUDIO_BITRATE, echoCanceler = true, noiseSuppressor = true)
        }.getOrDefault(false)
        if (!prepared) publish(StreamStatus.ERROR, "This device cannot prepare the requested capture profile")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PREPARE_PROJECTION -> prepareProjectionFromIntent(intent)
            ACTION_PREPARE_CAMERA -> prepareCamera()
            ACTION_START -> {
                startForegroundSafely()
                startStream(intent.getStringExtra(EXTRA_ENDPOINT).orEmpty())
            }
            ACTION_STOP -> stopStreaming()
            ACTION_TOGGLE_MUTE -> toggleMute()
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun prepareProjectionFromIntent(intent: Intent) {
        startForegroundSafely()
        if (!hasAudioPermission()) {
            publish(StreamStatus.ERROR, "Microphone permission is not granted")
            return
        }
        if (!checkMicrophoneInput()) {
            publish(StreamStatus.ERROR, "Microphone is unavailable. Check Android privacy controls and other apps using the mic")
            return
        }
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val projectionData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PROJECTION_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PROJECTION_DATA)
        }
        if (projectionData == null || !prepareProjection(resultCode, projectionData)) {
            publish(StreamStatus.ERROR, "Screen capture permission was not available")
        } else {
            publish(StreamStatus.PREPARING, "Microphone ready • capture is ready")
        }
    }

    private fun prepareProjection(resultCode: Int, data: Intent): Boolean {
        if (!prepared) return false
        mediaProjection?.stop()
        val projection = projectionManager.getMediaProjection(resultCode, data) ?: return false
        mediaProjection = projection
        return runCatching {
            genericStream.changeVideoSource(ScreenSource(applicationContext, projection))
            microphoneSource?.release()
            microphoneSource = MicrophoneSource().also { genericStream.changeAudioSource(it) }
            captureReady = true
            true
        }.getOrDefault(false)
    }

    private fun prepareCamera() {
        startForegroundSafely()
        if (!hasAudioPermission()) {
            publish(StreamStatus.ERROR, "Microphone permission is not granted")
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            publish(StreamStatus.ERROR, "Camera permission is not granted")
            return
        }
        if (!checkMicrophoneInput()) {
            publish(StreamStatus.ERROR, "Microphone is unavailable. Check Android privacy controls and other apps using the mic")
            return
        }
        if (!prepared) {
            publish(StreamStatus.ERROR, "This device cannot prepare the camera capture profile")
            return
        }
        runCatching {
            cameraSource?.release()
            cameraSource = Camera2Source(applicationContext).also { genericStream.changeVideoSource(it) }
            microphoneSource?.release()
            microphoneSource = MicrophoneSource().also { genericStream.changeAudioSource(it) }
            captureReady = true
            publish(StreamStatus.PREPARING, "Camera and microphone ready")
        }.onFailure {
            captureReady = false
            publish(StreamStatus.ERROR, "Camera could not start: ${it.message.orEmpty()}")
        }
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun checkMicrophoneInput(): Boolean {
        val bufferSize = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (bufferSize <= 0) return false
        val recorder = runCatching {
            AudioRecord(MediaRecorder.AudioSource.MIC, AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2)
        }.getOrNull() ?: return false
        return runCatching {
            recorder.startRecording()
            val samples = ShortArray((bufferSize / 2).coerceAtLeast(256))
            recorder.read(samples, 0, samples.size) > 0
        }.getOrDefault(false).also {
            runCatching { recorder.stop() }
            recorder.release()
        }
    }

    private fun startStream(endpoint: String) {
        if (!prepared || !captureReady || microphoneSource == null) {
            publish(StreamStatus.ERROR, "Capture is not ready. Approve screen capture and microphone access first")
            return
        }
        if (endpoint.isBlank()) {
            publish(StreamStatus.ERROR, "Configure a streaming destination first")
            return
        }
        if (genericStream.isStreaming) return
        manualStop = false
        currentEndpoint = endpoint
        publish(StreamStatus.PREPARING, "Connecting to your destination")
        runCatching { genericStream.startStream(endpoint) }
            .onFailure { publish(StreamStatus.ERROR, "Unable to start the stream: ${it.message.orEmpty()}") }
    }

    private fun stopStreaming() {
        manualStop = true
        handler.removeCallbacksAndMessages(null)
        if (::genericStream.isInitialized && genericStream.isStreaming) genericStream.stopStream()
        if (StreamingStatusBus.state.value.recording) stopRecording()
        startedAtElapsed = 0L
        reconnectAttempt = 0
        currentEndpoint = ""
        publish(StreamStatus.IDLE, "Broadcast stopped")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun toggleMute() {
        val source = microphoneSource ?: return
        if (source.isMuted()) source.unMute() else source.mute()
        val previous = StreamingStatusBus.state.value
        StreamingStatusBus.update(previous.copy(microphoneMuted = source.isMuted(), message = if (source.isMuted()) "Microphone muted" else "Microphone live"))
    }

    private fun startRecording() {
        if (!prepared || StreamingStatusBus.state.value.recording) return
        val directory = File(filesDir, "recordings").apply { mkdirs() }
        val output = File(directory, "unictoos-${System.currentTimeMillis()}.mp4")
        runCatching {
            genericStream.startRecord(output.absolutePath, object : RecordController.Listener {
                override fun onStatusChange(status: RecordController.Status) {
                    val recording = status == RecordController.Status.STARTED || status == RecordController.Status.RECORDING || status == RecordController.Status.RESUMED
                    val previous = StreamingStatusBus.state.value
                    StreamingStatusBus.update(previous.copy(recording = recording, message = if (recording) "Recording ${output.name}" else "Recording saved"))
                }
            })
        }.onFailure {
            publish(StreamStatus.ERROR, "Recording could not start: ${it.message.orEmpty()}")
        }
    }

    private fun stopRecording() {
        if (::genericStream.isInitialized && StreamingStatusBus.state.value.recording) {
            genericStream.stopRecord()
            val previous = StreamingStatusBus.state.value
            StreamingStatusBus.update(previous.copy(recording = false, message = "Recording saved on this device"))
        }
    }

    private fun scheduleReconnect(reason: String) {
        if (manualStop) return
        if (currentEndpoint.isBlank() || reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) {
            publish(StreamStatus.ERROR, if (reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) "Connection lost after $MAX_RECONNECT_ATTEMPTS reconnect attempts" else reason)
            return
        }
        reconnectAttempt += 1
        val delay = RECONNECT_DELAYS_MS[reconnectAttempt - 1]
        publish(StreamStatus.RECONNECTING, "$reason • retry $reconnectAttempt/$MAX_RECONNECT_ATTEMPTS")
        handler.postDelayed({
            if (!manualStop && currentEndpoint.isNotBlank()) {
                runCatching { genericStream.startStream(currentEndpoint) }
                    .onFailure { scheduleReconnect("Reconnect failed") }
            }
        }, delay)
    }

    private fun startForegroundSafely() {
        val serviceTypes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), serviceTypes)
    }

    private fun publish(status: StreamStatus, message: String? = null) {
        val previous = StreamingStatusBus.state.value
        if (status == StreamStatus.LIVE && startedAtElapsed == 0L) {
            startedAtElapsed = SystemClock.elapsedRealtime()
            handler.removeCallbacks(elapsedTicker)
            handler.post(elapsedTicker)
        }
        StreamingStatusBus.update(
            previous.copy(
                status = status,
                fps = if (status == StreamStatus.LIVE) VIDEO_FPS else previous.fps,
                reconnectAttempt = reconnectAttempt,
                message = message,
            ),
        )
        updateNotification(message ?: status.name.lowercase().replace('_', ' '))
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String = "Broadcast controls are active"): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_unictoos)
            .setContentTitle("Unictoos Studio")
            .setContentText(text)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Broadcasting", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps Unictoos capture and broadcast controls available"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        manualStop = true
        handler.removeCallbacksAndMessages(null)
        if (::genericStream.isInitialized) {
            if (StreamingStatusBus.state.value.recording) genericStream.stopRecord()
            if (genericStream.isStreaming) genericStream.stopStream()
            genericStream.release()
        }
        microphoneSource?.release()
        cameraSource?.release()
        mediaProjection?.stop()
        microphoneSource = null
        cameraSource = null
        captureReady = false
        mediaProjection = null
        startedAtElapsed = 0L
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConnectionStarted(url: String) = publish(StreamStatus.PREPARING, "Connecting securely")

    override fun onConnectionSuccess() {
        reconnectAttempt = 0
        publish(StreamStatus.LIVE, "Broadcast is live")
    }

    override fun onNewBitrate(bitrate: Long) {
        val previous = StreamingStatusBus.state.value
        StreamingStatusBus.update(previous.copy(bitrateKbps = (bitrate / 1000L).toInt()))
    }

    override fun onConnectionFailed(reason: String) = scheduleReconnect("Connection failed")

    override fun onDisconnect() = scheduleReconnect("Connection lost")

    override fun onAuthError() {
        manualStop = true
        publish(StreamStatus.ERROR, "Destination rejected the stream key. Check the selected platform and rotate the key if needed")
    }

    override fun onAuthSuccess() = publish(StreamStatus.PREPARING, "Destination authenticated")

    companion object {
        const val ACTION_PREPARE_PROJECTION = "com.unictoai.unictoos.action.PREPARE_PROJECTION"
        const val ACTION_PREPARE_CAMERA = "com.unictoai.unictoos.action.PREPARE_CAMERA"
        const val ACTION_START = "com.unictoai.unictoos.action.START_STREAMING"
        const val ACTION_STOP = "com.unictoai.unictoos.action.STOP_STREAMING"
        const val ACTION_TOGGLE_MUTE = "com.unictoai.unictoos.action.TOGGLE_MUTE"
        const val ACTION_START_RECORDING = "com.unictoai.unictoos.action.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.unictoai.unictoos.action.STOP_RECORDING"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_PROJECTION_DATA = "extra_projection_data"
        const val EXTRA_ENDPOINT = "extra_endpoint"
        private const val CHANNEL_ID = "unictoos-broadcasting"
        private const val NOTIFICATION_ID = 4101
        private const val VIDEO_WIDTH = 720
        private const val VIDEO_HEIGHT = 1280
        private const val VIDEO_FPS = 30
        private const val VIDEO_BITRATE = 4_500_000
        private const val AUDIO_SAMPLE_RATE = 44_100
        private const val AUDIO_BITRATE = 128_000
        private const val MAX_RECONNECT_ATTEMPTS = 3
        private val RECONNECT_DELAYS_MS = longArrayOf(2_000L, 5_000L, 10_000L)

        private fun formatElapsed(seconds: Long): String = "%02d:%02d".format(seconds / 60, seconds % 60)
    }
}
