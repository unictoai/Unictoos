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
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.os.Looper
import android.os.SystemClock
import android.view.Surface
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
import com.unictoai.unictoos.data.CreatorHistoryStore
import com.unictoai.unictoos.data.StreamQualityStore
import com.unictoai.unictoos.data.ThermalProtectionStore
import com.unictoai.unictoos.data.AudioSettingsStore
import com.unictoai.unictoos.data.AutoStopStore
import com.unictoai.unictoos.data.LatencyModeStore
import com.unictoai.unictoos.domain.LatencyMode
import com.unictoai.unictoos.domain.AudioSettings
import com.unictoai.unictoos.domain.StreamQuality
import com.unictoai.unictoos.domain.SessionMode
import com.unictoai.unictoos.domain.StreamHealthSample
import com.unictoai.unictoos.domain.StreamMarker
import com.unictoai.unictoos.domain.StreamSessionState
import com.unictoai.unictoos.domain.SessionSummary
import com.unictoai.unictoos.domain.StreamStatus
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StreamingForegroundService : Service(), ConnectChecker {
    private lateinit var genericStream: GenericStream
    private var mediaProjection: MediaProjection? = null
    private var microphoneSource: MicrophoneSource? = null
    private var cameraSource: Camera2Source? = null
    private var prepared = false
    private var captureReady = false
    private var previewSurface: Surface? = null
    private var previewWidth = 0
    private var previewHeight = 0
    private var previewAttached = false
    private var pendingStart: PendingStart? = null
    private var manualStop = false
    private var currentEndpoint: String = ""
    private var reconnectAttempt = 0
    private var startedAtElapsed = 0L
    private var adaptiveTargetBitrate = 0
    private var degradedSinceElapsed = 0L
    private var recoveredSinceElapsed = 0L
    private var thermalCapApplied = false
    private val bitrateHistory = java.util.ArrayDeque<Int>()
    private lateinit var historyStore: CreatorHistoryStore
    private lateinit var streamQuality: StreamQuality
    private lateinit var audioSettings: AudioSettings
    private var autoStopSeconds = 0L
    private lateinit var latencyMode: LatencyMode
    private var currentSessionId = ""
    private val sessionHealthSamples = mutableListOf<StreamHealthSample>()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val handler = Handler(Looper.getMainLooper())
    private val projectionManager: MediaProjectionManager by lazy {
        getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    private val captureTimeout = Runnable {
        val state = StreamingStatusBus.state.value
        if (state.status == StreamStatus.PREPARING && pendingStart != null && (!captureReady || !previewAttached)) {
            pendingStart = null
            publish(StreamStatus.ERROR, "Preview did not start within 30 seconds. Check capture permission and try again")
        }
    }

    private val elapsedTicker = object : Runnable {
        override fun run() {
            if (startedAtElapsed > 0L) {
                val elapsed = (SystemClock.elapsedRealtime() - startedAtElapsed) / 1_000L
                if (autoStopSeconds > 0L && elapsed >= autoStopSeconds) {
                    stopStreaming("Auto-stop completed after ${formatElapsed(autoStopSeconds)}")
                    return
                }
                val previous = StreamingStatusBus.state.value
                StreamingStatusBus.update(previous.copy(elapsedSeconds = elapsed))
                recordHealthSample(elapsed, previous)
                updateNotification("Live for ${formatElapsed(elapsed)}")
                handler.postDelayed(this, 1_000L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        historyStore = CreatorHistoryStore(applicationContext)
        streamQuality = StreamQualityStore(applicationContext).load()
        audioSettings = AudioSettingsStore(applicationContext).load()
        latencyMode = LatencyModeStore(applicationContext).load()
        createNotificationChannel()
        genericStream = GenericStream(this, this, NoVideoSource(), NoAudioSource()).apply {
            getGlInterface().setForceRender(true, streamQuality.fps)
        }
        if (latencyMode == LatencyMode.LOW_LATENCY) {
            // RootEncoder exposes client-cache sizing, but no public keyframe-interval override in this version.
            genericStream.getStreamClient().resizeCache(0)
        }
        prepared = runCatching {
            genericStream.prepareVideo(streamQuality.width, streamQuality.height, streamQuality.bitrate, rotation = 0) &&
                genericStream.prepareAudio(audioSettings.sampleRate, false, audioSettings.bitrate, echoCanceler = audioSettings.echoCanceler, noiseSuppressor = audioSettings.noiseSuppressor)
        }.getOrDefault(false)
        if (!prepared) publish(StreamStatus.ERROR, "This device cannot prepare the requested capture profile")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PREPARE_PROJECTION -> serviceScope.launch {
                prepareProjectionFromIntent(intent)
                tryStartPending()
            }
            ACTION_PREPARE_CAMERA -> serviceScope.launch {
                prepareCamera()
                tryStartPending()
            }
            ACTION_ATTACH_PREVIEW -> attachPreview(readPreviewSurface(intent), intent.getIntExtra(EXTRA_PREVIEW_WIDTH, 0), intent.getIntExtra(EXTRA_PREVIEW_HEIGHT, 0))
            ACTION_DETACH_PREVIEW -> detachPreview()
            ACTION_START -> {
                pendingStart = PendingStart(intent.getStringExtra(EXTRA_ENDPOINT).orEmpty(), practice = false)
                if (startForegroundSafely(mediaProjection != null)) tryStartPending()
            }
            ACTION_START_PRACTICE -> {
                pendingStart = PendingStart(endpoint = "", practice = true)
                if (startForegroundSafely(mediaProjection != null)) tryStartPending()
            }
            ACTION_CREATE_MARKER -> createMarker(intent.getStringExtra(EXTRA_MARKER_LABEL).orEmpty())
            ACTION_STOP -> stopStreaming()
            ACTION_TOGGLE_MUTE -> toggleMute()
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording()
            ACTION_DISMISS_STATUS -> dismissStatusMessage()
        }
        return START_NOT_STICKY
    }

    private suspend fun prepareProjectionFromIntent(intent: Intent) {
        if (!startForegroundSafely(includeProjection = true)) return
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
            publish(StreamStatus.PREPARING, "Microphone ready • waiting for Studio preview")
            attachPreviewIfPossible()
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
        }.getOrDefault(false).also {
            if (!it) captureReady = false
        }
    }

    private suspend fun prepareCamera() {
        if (!startForegroundSafely(includeProjection = false)) return
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
            publish(StreamStatus.PREPARING, "Camera and microphone ready • waiting for Studio preview")
            attachPreviewIfPossible()
        }.onFailure {
            captureReady = false
            publish(StreamStatus.ERROR, "Camera could not start: ${it.message.orEmpty()}")
        }
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private suspend fun checkMicrophoneInput(): Boolean = withContext(Dispatchers.IO) {
        val bufferSize = AudioRecord.getMinBufferSize(audioSettings.sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (bufferSize <= 0) return@withContext false
        val recorder = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, audioSettings.sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2)
        } catch (_: SecurityException) {
            return@withContext false
        } catch (_: IllegalArgumentException) {
            return@withContext false
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return@withContext false
        }
        runCatching {
            recorder.startRecording()
            val samples = ShortArray((bufferSize / 2).coerceAtLeast(256))
            recorder.read(samples, 0, samples.size) > 0
        }.getOrDefault(false).also {
            runCatching { recorder.stop() }
            recorder.release()
        }
    }

    private fun attachPreview(surface: Surface?, width: Int, height: Int) {
        if (surface == null || !surface.isValid || width <= 0 || height <= 0) {
            publish(StreamStatus.ERROR, "Studio preview surface is unavailable")
            return
        }
        previewSurface = surface
        previewWidth = width
        previewHeight = height
        attachPreviewIfPossible()
        tryStartPending()
    }

    private fun attachPreviewIfPossible() {
        val surface = previewSurface ?: return
        if (!prepared || !captureReady || !surface.isValid || previewWidth <= 0 || previewHeight <= 0) return
        runCatching {
            if (genericStream.isOnPreview) genericStream.stopPreview()
            genericStream.startPreview(surface, previewWidth, previewHeight)
            previewAttached = true
            val previous = StreamingStatusBus.state.value
            StreamingStatusBus.update(previous.copy(captureReady = captureReady, previewReady = true, message = if (pendingStart != null) "Preview is ready • starting capture" else previous.message))
        }.onFailure {
            previewAttached = false
            publish(StreamStatus.ERROR, "Preview could not start: ${it.message.orEmpty()}")
        }
    }

    private fun detachPreview() {
        if (::genericStream.isInitialized && genericStream.isOnPreview) {
            runCatching { genericStream.stopPreview() }
        }
        previewAttached = false
        previewSurface = null
        previewWidth = 0
        previewHeight = 0
        val previous = StreamingStatusBus.state.value
        if (previous.status == StreamStatus.PREPARING && pendingStart != null) {
            publish(StreamStatus.PREPARING, "Waiting for Studio preview surface")
        } else {
            StreamingStatusBus.update(previous.copy(previewReady = false, message = previous.message))
        }
    }

    private fun tryStartPending() {
        val request = pendingStart ?: return
        if (!prepared || !captureReady || !previewAttached) {
            val state = StreamingStatusBus.state.value
            if (state.status != StreamStatus.ERROR) publish(StreamStatus.PREPARING, if (!previewAttached) "Waiting for Studio preview surface" else "Preparing capture")
            handler.removeCallbacks(captureTimeout)
            handler.postDelayed(captureTimeout, CAPTURE_TIMEOUT_MS)
            return
        }
        pendingStart = null
        handler.removeCallbacks(captureTimeout)
        if (request.practice) startPractice() else startStream(request.endpoint)
    }

    private fun readPreviewSurface(intent: Intent): Surface? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(EXTRA_PREVIEW_SURFACE, Surface::class.java)
    } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(EXTRA_PREVIEW_SURFACE)
    }

    private data class PendingStart(val endpoint: String, val practice: Boolean)

    private fun startPractice() {
        if (!prepared || !captureReady || !previewAttached || microphoneSource == null) {
            publish(StreamStatus.ERROR, "Practice capture is not ready. Approve capture and wait for the live preview first")
            return
        }
        manualStop = false
        currentEndpoint = ""
        reconnectAttempt = 0
        autoStopSeconds = AutoStopStore(applicationContext).load().seconds
        currentSessionId = "session-${System.currentTimeMillis()}"
        sessionHealthSamples.clear()
        StreamingStatusBus.clearHealth()
        val previous = StreamingStatusBus.state.value
        StreamingStatusBus.update(previous.copy(mode = SessionMode.PRACTICE, message = "Starting local practice recording"))
        startRecording(prefix = "unictoos-practice")
        publish(StreamStatus.LIVE, "Practice mode active • nothing is published")
    }

    private fun startStream(endpoint: String) {
        StreamingStatusBus.clearHealth()
        val previous = StreamingStatusBus.state.value
        StreamingStatusBus.update(previous.copy(mode = SessionMode.BROADCAST))
        if (!prepared || !captureReady || !previewAttached || microphoneSource == null) {
            publish(StreamStatus.ERROR, "Capture is not ready. Approve capture and wait for the live preview first")
            return
        }
        if (endpoint.isBlank()) {
            publish(StreamStatus.ERROR, "Configure a streaming destination first")
            return
        }
        if (genericStream.isStreaming) return
        manualStop = false
        autoStopSeconds = AutoStopStore(applicationContext).load().seconds
        currentSessionId = "session-${System.currentTimeMillis()}"
        sessionHealthSamples.clear()
        currentEndpoint = endpoint
        publish(StreamStatus.PREPARING, "Connecting to your destination")
        runCatching { genericStream.startStream(endpoint) }
            .onFailure { publish(StreamStatus.ERROR, "Unable to start the stream: ${it.message.orEmpty()}") }
    }

    private fun stopStreaming(reason: String = "Broadcast stopped") {
        manualStop = true
        pendingStart = null
        handler.removeCallbacksAndMessages(null)
        if (::genericStream.isInitialized && genericStream.isStreaming) genericStream.stopStream()
        if (StreamingStatusBus.state.value.recording) stopRecording()
        val completed = StreamingStatusBus.state.value
        if (sessionHealthSamples.isNotEmpty()) historyStore.addHealthSamples(sessionHealthSamples.toList())
        if (completed.elapsedSeconds > 0L) {
            historyStore.addSession(
                SessionSummary(
                    id = "session-${System.currentTimeMillis()}",
                    mode = completed.mode,
                    elapsedSeconds = completed.elapsedSeconds,
                    bitrateKbps = completed.bitrateKbps,
                    fps = completed.fps,
                    droppedFrames = completed.droppedFrames,
                    finishedAtMillis = System.currentTimeMillis(),
                ),
            )
        }
        startedAtElapsed = 0L
        adaptiveTargetBitrate = 0
        degradedSinceElapsed = 0L
        recoveredSinceElapsed = 0L
        thermalCapApplied = false
        bitrateHistory.clear()
        reconnectAttempt = 0
        currentEndpoint = ""
        autoStopSeconds = 0L
        currentSessionId = ""
        sessionHealthSamples.clear()
        publish(StreamStatus.IDLE, reason)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createMarker(label: String) {
        val state = StreamingStatusBus.state.value
        if (state.status != StreamStatus.LIVE) return
        val safeLabel = label.trim().ifBlank { "Moment" }
        historyStore.addMarker(StreamMarker("marker-${System.currentTimeMillis()}", safeLabel, state.elapsedSeconds, System.currentTimeMillis()))
        publish(state.status, "Marker saved at ${formatElapsed(state.elapsedSeconds)}")
    }

    private fun toggleMute() {
        val source = microphoneSource ?: return
        if (source.isMuted()) source.unMute() else source.mute()
        val previous = StreamingStatusBus.state.value
        StreamingStatusBus.update(previous.copy(microphoneMuted = source.isMuted(), message = if (source.isMuted()) "Microphone muted" else "Microphone live"))
    }

    private fun startRecording(prefix: String = "unictoos") {
        // RootEncoder 2.4.5 records the already encoded stream buffers; it does not expose a separate
        // recording bitrate/resolution encoder. Local MP4 quality therefore follows the active stream profile.
        if (!prepared || StreamingStatusBus.state.value.recording) return
        val directory = File(filesDir, "recordings").apply { mkdirs() }
        val output = File(directory, "$prefix-${System.currentTimeMillis()}.mp4")
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

    private fun startForegroundSafely(includeProjection: Boolean): Boolean {
        val serviceTypes = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && includeProjection -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && includeProjection -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            else -> 0
        }
        return try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), serviceTypes)
            true
        } catch (error: SecurityException) {
            publish(StreamStatus.ERROR, "Android rejected the capture service: ${error.message.orEmpty()}")
            stopSelf()
            false
        } catch (error: IllegalArgumentException) {
            publish(StreamStatus.ERROR, "Capture service configuration is invalid: ${error.message.orEmpty()}")
            stopSelf()
            false
        }
    }

    private fun publish(status: StreamStatus, message: String? = null) {
        val previous = StreamingStatusBus.state.value
        if (status == StreamStatus.LIVE && startedAtElapsed == 0L) {
            startedAtElapsed = SystemClock.elapsedRealtime()
            adaptiveTargetBitrate = streamQuality.bitrate
            degradedSinceElapsed = 0L
            recoveredSinceElapsed = 0L
            handler.removeCallbacks(elapsedTicker)
            handler.post(elapsedTicker)
        }
        StreamingStatusBus.update(
            previous.copy(
                status = status,
                fps = if (status == StreamStatus.LIVE) streamQuality.fps else previous.fps,
                reconnectAttempt = reconnectAttempt,
                captureReady = captureReady,
                previewReady = previewAttached,
                message = message,
            ),
        )
        updateNotification(message ?: status.name.lowercase().replace('_', ' '))
    }

    private fun recordHealthSample(elapsed: Long, state: StreamSessionState) {
        val batteryIntent = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else -1
        val thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(PowerManager::class.java)?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
        } else {
            PowerManager.THERMAL_STATUS_NONE
        }
        if (ThermalProtectionStore(applicationContext).isEnabled() && thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE && !thermalCapApplied && prepared) {
            applyThermalProtection(thermalStatus)
        }
        val connectivity = getSystemService(ConnectivityManager::class.java)
        val capabilities = connectivity?.getNetworkCapabilities(connectivity.activeNetwork)
        val networkLabel = when {
            capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
            capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
            capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> "Offline"
        }
        val sample = StreamHealthSample(
                elapsedSeconds = elapsed,
                sessionId = currentSessionId,
                bitrateKbps = state.bitrateKbps,
                fps = state.fps,
                droppedFrames = state.droppedFrames,
                audioLevel = state.audioLevel,
                batteryPercent = batteryPercent,
                thermalStatus = thermalStatus,
                networkLabel = networkLabel,
            )
        sessionHealthSamples += sample
        StreamingStatusBus.recordHealth(sample)
    }

    private fun applyThermalProtection(thermalStatus: Int) {
        val currentTarget = adaptiveTargetBitrate.takeIf { it > 0 } ?: streamQuality.bitrate
        val reducedTarget = (currentTarget * 0.75f).toInt().coerceAtLeast(1_000_000)
        if (reducedTarget >= currentTarget) {
            thermalCapApplied = true
            return
        }
        runCatching { genericStream.setVideoBitrateOnFly(reducedTarget) }
            .onSuccess {
                adaptiveTargetBitrate = reducedTarget
                thermalCapApplied = true
                val label = when (thermalStatus) {
                    PowerManager.THERMAL_STATUS_CRITICAL, PowerManager.THERMAL_STATUS_EMERGENCY, PowerManager.THERMAL_STATUS_SHUTDOWN -> "critical temperature"
                    PowerManager.THERMAL_STATUS_SEVERE -> "high temperature"
                    else -> "device temperature"
                }
                publish(StreamStatus.LIVE, "Reduced quality to protect the device from $label • ${reducedTarget / 1000} kbps")
            }
    }

    private fun dismissStatusMessage() {
        val previous = StreamingStatusBus.state.value
        if (previous.message?.contains("Reduced quality", ignoreCase = true) == true) {
            StreamingStatusBus.update(previous.copy(message = null))
        }
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
        serviceScope.cancel()
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
        previewAttached = false
        previewSurface = null
        pendingStart = null
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
        val reported = bitrate.coerceAtLeast(0L).toInt()
        bitrateHistory.addLast(reported)
        if (bitrateHistory.size > 5) bitrateHistory.removeFirst()
        val average = if (bitrateHistory.isEmpty()) 0 else bitrateHistory.sum() / bitrateHistory.size
        val target = adaptiveTargetBitrate.takeIf { it > 0 } ?: streamQuality.bitrate
        val degraded = average < (target * 0.60f).toInt()
        val recovered = average >= (target * 0.85f).toInt()
        if (degraded) {
            if (degradedSinceElapsed == 0L) degradedSinceElapsed = SystemClock.elapsedRealtime()
            recoveredSinceElapsed = 0L
        } else if (recovered) {
            if (recoveredSinceElapsed == 0L) recoveredSinceElapsed = SystemClock.elapsedRealtime()
            degradedSinceElapsed = 0L
        } else {
            degradedSinceElapsed = 0L
            recoveredSinceElapsed = 0L
        }
        val degradedSeconds = if (degradedSinceElapsed > 0L) ((SystemClock.elapsedRealtime() - degradedSinceElapsed) / 1_000L).toInt() else 0
        val recoveredSeconds = if (recoveredSinceElapsed > 0L) ((SystemClock.elapsedRealtime() - recoveredSinceElapsed) / 1_000L).toInt() else 0
        val decision = decideAdaptiveBitrate(
            currentTargetBitrate = target,
            baselineTargetBitrate = streamQuality.bitrate,
            rollingAverageBitrate = average,
            degradedSeconds = degradedSeconds,
            recoveredSeconds = recoveredSeconds,
        )
        if (decision.action != AdaptiveBitrateAction.HOLD && decision.bitrate != target) {
            runCatching { genericStream.setVideoBitrateOnFly(decision.bitrate) }
                .onSuccess {
                    adaptiveTargetBitrate = decision.bitrate
                    val message = if (decision.action == AdaptiveBitrateAction.STEP_DOWN) {
                        "Reduced quality to maintain connection • ${decision.bitrate / 1000} kbps"
                    } else {
                        "Network recovered • quality raised to ${decision.bitrate / 1000} kbps"
                    }
                    publish(StreamStatus.LIVE, message)
                }
        }
        StreamingStatusBus.update(StreamingStatusBus.state.value.copy(bitrateKbps = (bitrate / 1000L).toInt()))
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
        const val ACTION_ATTACH_PREVIEW = "com.unictoai.unictoos.action.ATTACH_PREVIEW"
        const val ACTION_DETACH_PREVIEW = "com.unictoai.unictoos.action.DETACH_PREVIEW"
        const val ACTION_START = "com.unictoai.unictoos.action.START_STREAMING"
        const val ACTION_START_PRACTICE = "com.unictoai.unictoos.action.START_PRACTICE"
        const val ACTION_CREATE_MARKER = "com.unictoai.unictoos.action.CREATE_MARKER"
        const val ACTION_STOP = "com.unictoai.unictoos.action.STOP_STREAMING"
        const val ACTION_TOGGLE_MUTE = "com.unictoai.unictoos.action.TOGGLE_MUTE"
        const val ACTION_START_RECORDING = "com.unictoai.unictoos.action.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.unictoai.unictoos.action.STOP_RECORDING"
        const val ACTION_DISMISS_STATUS = "com.unictoai.unictoos.action.DISMISS_STATUS"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_PROJECTION_DATA = "extra_projection_data"
        const val EXTRA_ENDPOINT = "extra_endpoint"
        const val EXTRA_PREVIEW_SURFACE = "extra_preview_surface"
        const val EXTRA_PREVIEW_WIDTH = "extra_preview_width"
        const val EXTRA_PREVIEW_HEIGHT = "extra_preview_height"
        const val EXTRA_MARKER_LABEL = "extra_marker_label"
        private const val CHANNEL_ID = "unictoos-broadcasting"
        private const val NOTIFICATION_ID = 4101
        private const val MAX_RECONNECT_ATTEMPTS = 3
        private const val CAPTURE_TIMEOUT_MS = 30_000L
        private val RECONNECT_DELAYS_MS = longArrayOf(2_000L, 5_000L, 10_000L)

        private fun formatElapsed(seconds: Long): String = "%02d:%02d".format(seconds / 60, seconds % 60)
    }
}
