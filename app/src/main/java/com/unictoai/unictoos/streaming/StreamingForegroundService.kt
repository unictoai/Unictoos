package com.unictoai.unictoos.streaming

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.pedro.common.ConnectChecker
import com.pedro.library.util.sources.audio.MicrophoneSource
import com.pedro.library.util.sources.audio.NoAudioSource
import com.pedro.library.util.sources.video.NoVideoSource
import com.pedro.library.util.sources.video.ScreenSource
import com.pedro.library.generic.GenericStream
import com.unictoai.unictoos.R
import com.unictoai.unictoos.domain.StreamSessionState
import com.unictoai.unictoos.domain.StreamStatus

class StreamingForegroundService : Service(), ConnectChecker {
    private lateinit var genericStream: GenericStream
    private var mediaProjection: MediaProjection? = null
    private var prepared = false

    private val projectionManager: MediaProjectionManager by lazy {
        getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        genericStream = GenericStream(this, this, NoVideoSource(), NoAudioSource()).apply {
            getGlInterface().setForceRender(true, 30)
        }
        prepared = try {
            genericStream.prepareVideo(
                VIDEO_WIDTH,
                VIDEO_HEIGHT,
                VIDEO_BITRATE,
                rotation = 0,
            ) && genericStream.prepareAudio(
                AUDIO_SAMPLE_RATE,
                false,
                AUDIO_BITRATE,
                echoCanceler = true,
                noiseSuppressor = true,
            )
        } catch (_: IllegalArgumentException) {
            false
        }
        if (!prepared) {
            publish(StreamStatus.ERROR, "This device cannot prepare the requested capture profile")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PREPARE_PROJECTION -> {
                startForegroundSafely()
                if (!hasAudioPermission()) {
                    publish(StreamStatus.ERROR, "Microphone permission is not granted")
                    return START_NOT_STICKY
                }
                if (!checkMicrophoneInput()) {
                    publish(StreamStatus.ERROR, "Microphone is unavailable. Check Android privacy controls and other apps using the mic")
                    return START_NOT_STICKY
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
            ACTION_START -> {
                startForegroundSafely()
                startStream(intent.getStringExtra(EXTRA_ENDPOINT).orEmpty())
            }
            ACTION_STOP -> stopStreaming()
        }
        return START_NOT_STICKY
    }

    private fun prepareProjection(resultCode: Int, data: Intent): Boolean {
        if (!prepared) return false
        mediaProjection?.stop()
        val projection = projectionManager.getMediaProjection(resultCode, data) ?: return false
        mediaProjection = projection
        return try {
                                genericStream.changeVideoSource(ScreenSource(applicationContext, projection))
                    genericStream.changeAudioSource(MicrophoneSource())
                    true

        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun checkMicrophoneInput(): Boolean {
        val bufferSize = AudioRecord.getMinBufferSize(
            AUDIO_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (bufferSize <= 0) return false
        val recorder = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                AUDIO_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2,
            )
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
        if (!prepared || endpoint.isBlank()) {
            publish(StreamStatus.ERROR, "Configure a streaming destination first")
            return
        }
        if (!genericStream.isStreaming) {
            publish(StreamStatus.PREPARING, "Connecting to your destination")
            genericStream.startStream(endpoint)
        }
    }

    private fun stopStreaming() {
        if (::genericStream.isInitialized && genericStream.isStreaming) {
            genericStream.stopStream()
        }
        publish(StreamStatus.IDLE, "Broadcast stopped")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundSafely() {
        val serviceTypes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), serviceTypes)
    }

    private fun publish(status: StreamStatus, message: String? = null) {
        val previous = StreamingStatusBus.state.value
        StreamingStatusBus.update(
            StreamSessionState(
                status = status,
                elapsedSeconds = previous.elapsedSeconds,
                bitrateKbps = previous.bitrateKbps,
                fps = if (status == StreamStatus.LIVE) 30 else previous.fps,
                droppedFrames = previous.droppedFrames,
                message = message,
            ),
        )
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_unictoos)
            .setContentTitle("Unictoos Studio")
            .setContentText("Broadcast controls are active")
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
        if (::genericStream.isInitialized) {
            if (genericStream.isStreaming) genericStream.stopStream()
            genericStream.release()
        }
        mediaProjection?.stop()
        mediaProjection = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConnectionStarted(url: String) = publish(StreamStatus.PREPARING, "Connecting")

    override fun onConnectionSuccess() = publish(StreamStatus.LIVE, "Broadcast is live")

    override fun onNewBitrate(bitrate: Long) {
        val previous = StreamingStatusBus.state.value
        StreamingStatusBus.update(previous.copy(bitrateKbps = (bitrate / 1000L).toInt()))
    }

    override fun onConnectionFailed(reason: String) = publish(StreamStatus.ERROR, reason)

    override fun onDisconnect() = publish(StreamStatus.IDLE, "Disconnected")

    override fun onAuthError() = publish(StreamStatus.ERROR, "Destination rejected the stream key")

    override fun onAuthSuccess() = publish(StreamStatus.PREPARING, "Destination authenticated")

    companion object {
        const val ACTION_PREPARE_PROJECTION = "com.unictoai.unictoos.action.PREPARE_PROJECTION"
        const val ACTION_START = "com.unictoai.unictoos.action.START_STREAMING"
        const val ACTION_STOP = "com.unictoai.unictoos.action.STOP_STREAMING"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_PROJECTION_DATA = "extra_projection_data"
        const val EXTRA_ENDPOINT = "extra_endpoint"
        private const val CHANNEL_ID = "unictoos-broadcasting"
        private const val NOTIFICATION_ID = 4101
        private const val VIDEO_WIDTH = 720
        private const val VIDEO_HEIGHT = 1280
        private const val VIDEO_BITRATE = 4_500_000
        private const val AUDIO_SAMPLE_RATE = 44_100
        private const val AUDIO_BITRATE = 128_000
    }
}
