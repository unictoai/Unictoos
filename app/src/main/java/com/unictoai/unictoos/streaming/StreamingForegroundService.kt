package com.unictoai.unictoos.streaming

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
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
import android.net.Network
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.os.Looper
import android.os.SystemClock
import android.os.StatFs
import android.view.Surface
import com.pedro.encoder.input.gl.render.filters.`object`.TextObjectFilterRender
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.pedro.common.ConnectChecker
import com.pedro.library.base.recording.RecordController
import com.pedro.library.view.RenderErrorCallback
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.ScreenSource
import com.unictoai.unictoos.R
import com.unictoai.unictoos.ui.MainActivity
import com.unictoai.unictoos.data.CreatorHistoryStore
import com.unictoai.unictoos.data.LocalAnalyticsStore
import com.unictoai.unictoos.data.StreamQualityStore
import com.unictoai.unictoos.data.ThermalProtectionStore
import com.unictoai.unictoos.data.AudioSettingsStore
import com.unictoai.unictoos.data.AutoStopStore
import com.unictoai.unictoos.data.LatencyModeStore
import com.unictoai.unictoos.domain.LatencyMode
import com.unictoai.unictoos.domain.AudioSettings
import com.unictoai.unictoos.domain.RecordingReadinessPolicy
import com.unictoai.unictoos.domain.RecordingState
import com.unictoai.unictoos.domain.StreamQuality
import com.unictoai.unictoos.domain.SessionMode
import com.unictoai.unictoos.domain.StreamHealthSample
import com.unictoai.unictoos.domain.StreamMarker
import com.unictoai.unictoos.domain.StreamSessionState
import com.unictoai.unictoos.domain.SessionSummary
import com.unictoai.unictoos.domain.StreamStatus
import com.unictoai.unictoos.domain.SourceType
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class StreamingForegroundService : Service(), ConnectChecker {
    private lateinit var genericStream: SingleDestinationMultiStreamAdapter
    private var mediaProjection: MediaProjection? = null
    private var intentionallyReleasingProjection = false
    private var microphoneSource: MicrophoneSource? = null
    private var cameraSource: Camera2Source? = null
    private var prepared = false
    private var genericStreamReleased = false
    private var pipelineReleaseState = PipelineReleaseState.AVAILABLE
    private val graphicsFailureRequested = java.util.concurrent.atomic.AtomicBoolean(false)
    @Volatile private var serviceDestroyed = false
    private val sessionGeneration = AtomicLong(0L)
    private val isolateLivePreviewForDevice = CaptureCompatibilityPolicy.shouldIsolateLivePreview(Build.MANUFACTURER, Build.MODEL)
    private var captureReady = false
    private var previewSurface: Surface? = null
    private var previewSurfaceToken = 0L
    private var previewWidth = 0
    private var previewHeight = 0
    private var previewAttached = false
    private var pendingStart: PendingStart? = null
    private var reconnectScheduled = false
    private var reconnectRunnable: Runnable? = null
    private var networkCallbackRegistered = false
    private var manualStop = false
    private var currentEndpoint: String = ""
    private var currentEndpoints: List<String> = emptyList()
    private var reconnectAttempt = 0
    private var startedAtElapsed = 0L
    private var adaptiveTargetBitrate = 0
    private var degradedSinceElapsed = 0L
    private var recoveredSinceElapsed = 0L
    private var thermalCapApplied = false
    private var highThermalSinceElapsed = 0L
    private val bitrateHistory = java.util.ArrayDeque<Int>()
    private val reconnectJitter = kotlin.random.Random(System.currentTimeMillis())
    private lateinit var historyStore: CreatorHistoryStore
    private lateinit var analyticsStore: LocalAnalyticsStore
    private lateinit var streamQuality: StreamQuality
    private lateinit var audioSettings: AudioSettings
    private var autoStopSeconds = 0L
    private lateinit var latencyMode: LatencyMode
    private var currentSessionId = ""
    private var activeRecordingFile: File? = null
    private var recordingFinalizationJob: Job? = null
    private var pendingTerminalStopReason: String? = null
    private val sessionHealthSamples = mutableListOf<StreamHealthSample>()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val capturePreparationMutex = Mutex()

    private val handler = Handler(Looper.getMainLooper())

    /**
     * All state-changing callbacks enter through this main-thread queue. RootEncoder,
     * network, projection, and recording callbacks must never mutate session state directly.
     */
    private inline fun postSerialized(crossinline block: () -> Unit) {
        handler.post {
            if (!serviceDestroyed) block()
        }
    }

    private fun isCurrentGeneration(generation: Long): Boolean =
        !serviceDestroyed && generation == sessionGeneration.get()

    private val projectionManager: MediaProjectionManager by lazy {
        getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            if (manualStop || intentionallyReleasingProjection) return
            postSerialized {
                if (manualStop || intentionallyReleasingProjection) return@postSerialized
                val message = "Screen capture was stopped by Android. Approve capture again before restarting"
                captureReady = false
                previewAttached = false
                pendingStart = null
                if (StreamingStatusBus.state.value.status != StreamStatus.IDLE && StreamingStatusBus.state.value.status != StreamStatus.STOPPED) {
                    stopStreaming(message)
                    publish(StreamStatus.ERROR, message)
                } else {
                    publish(StreamStatus.ERROR, message)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            val generation = sessionGeneration.get()
            postSerialized {
                if (isCurrentGeneration(generation) && !manualStop && currentEndpoint.isNotBlank() && ::genericStream.isInitialized && genericStream.isStreaming) {
                    scheduleReconnect("Network connection lost")
                }
            }
        }

        override fun onAvailable(network: Network) {
            postSerialized {
                if (StreamingStatusBus.state.value.status == StreamStatus.RECONNECTING) {
                    updateNotification("Network available • reconnecting")
                }
            }
        }
    }
    private var connectionWatchdogGeneration = 0L
    private var connectionAttemptStartedElapsed = 0L
    private val connectionWatchdog = Runnable {
        val generation = connectionWatchdogGeneration
        val elapsed = if (connectionAttemptStartedElapsed > 0L) {
            SystemClock.elapsedRealtime() - connectionAttemptStartedElapsed
        } else {
            0L
        }
        val state = StreamingStatusBus.state.value
        if (StreamStartupPolicy.shouldTimeout(
                status = state.status,
                hasEndpoint = currentEndpoint.isNotBlank(),
                generationMatches = generation > 0L && generation == sessionGeneration.get(),
                elapsedMs = elapsed,
            )
        ) {
            StreamingDiagnostics.record(currentSessionId, generation, "connection_start_timeout")
            onConnectionFailedForGeneration("Connection timed out while waiting for the destination", generation)
        }
    }

    private fun scheduleConnectionWatchdog(generation: Long) {
        connectionWatchdogGeneration = generation
        connectionAttemptStartedElapsed = SystemClock.elapsedRealtime()
        handler.removeCallbacks(connectionWatchdog)
        handler.postDelayed(connectionWatchdog, StreamStartupPolicy.CONNECTION_TIMEOUT_MS)
    }

    private fun cancelConnectionWatchdog() {
        handler.removeCallbacks(connectionWatchdog)
        connectionWatchdogGeneration = 0L
        connectionAttemptStartedElapsed = 0L
    }

    private val captureTimeout = Runnable {
        val state = StreamingStatusBus.state.value
        if (state.status == StreamStatus.PREPARING && pendingStart != null && !captureReady) {
            pendingStart = null
            publish(StreamStatus.ERROR, "Capture did not become ready within 30 seconds. Check capture permission and try again")
        }
    }

    private var elapsedTickerGeneration = 0L
    private val elapsedTicker = object : Runnable {
        override fun run() {
            if (startedAtElapsed > 0L && elapsedTickerGeneration == sessionGeneration.get()) {
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

    private fun createGenericStream(): SingleDestinationMultiStreamAdapter {
        val generation = sessionGeneration.incrementAndGet()
        graphicsFailureRequested.set(false)
        StreamingDiagnostics.record(currentSessionId, generation, "pipeline_created")
        return SingleDestinationMultiStreamAdapter(this, GenerationConnectChecker(generation)).apply {
            // Camera2Source and ScreenSource deliver SurfaceTexture frames themselves. A periodic
            // ForceRenderer adds a second render producer and can queue redundant GL work on devices
            // with slower encoder/EGL drivers. prepareVideo already enables RootEncoder's FPS limiter.
            getGlInterface().setForceRender(false)
            getGlInterface().setRenderErrorCallback(object : RenderErrorCallback {
                override fun onRenderError(error: RuntimeException) {
                    runCatching { getGlInterface().stop() }
                    postSerialized {
                        if (!isCurrentGeneration(generation)) return@postSerialized
                        StreamingDiagnostics.record(currentSessionId, generation, "render_error", error.message.orEmpty())
                        handleEncoderGraphicsFailure(generation)
                    }
                }
            })
            setFpsListener { fps ->
                postSerialized { publishFpsSample(fps, generation) }
            }
            if (latencyMode == LatencyMode.LOW_LATENCY) {
                // RootEncoder exposes client-cache sizing, but no public keyframe-interval override in this version.
                getStreamClient().resizeCache(0)
            }
        }
    }

    private inner class GenerationConnectChecker(private val generation: Long) : ConnectChecker {
        override fun onConnectionStarted(url: String) = postSerialized {
            if (isCurrentGeneration(generation)) onConnectionStartedForGeneration(url, generation)
        }

        override fun onConnectionSuccess() = postSerialized {
            if (isCurrentGeneration(generation)) onConnectionSuccessForGeneration(generation)
        }

        override fun onNewBitrate(bitrate: Long) = postSerialized {
            if (isCurrentGeneration(generation)) onNewBitrateForGeneration(bitrate, generation)
        }

        override fun onConnectionFailed(reason: String) = postSerialized {
            if (isCurrentGeneration(generation)) onConnectionFailedForGeneration(reason, generation)
        }

        override fun onDisconnect() = postSerialized {
            if (isCurrentGeneration(generation)) onDisconnectForGeneration(generation)
        }

        override fun onAuthError() = postSerialized {
            if (isCurrentGeneration(generation)) onAuthErrorForGeneration(generation)
        }

        override fun onAuthSuccess() = postSerialized {
            if (isCurrentGeneration(generation)) onAuthSuccessForGeneration(generation)
        }
    }

    private fun prepareGenericStream(): Boolean = runCatching {
        genericStream.prepareVideo(streamQuality.width, streamQuality.height, streamQuality.bitrate, rotation = 0) &&
            genericStream.prepareAudio(
                audioSettings.sampleRate,
                false,
                audioSettings.bitrate,
                echoCanceler = audioSettings.echoCanceler,
                noiseSuppressor = audioSettings.noiseSuppressor,
            )
    }.getOrDefault(false)

    private fun publishFpsSample(fps: Int, generation: Long) {
        if (!isCurrentGeneration(generation)) return
        val previous = StreamingStatusBus.state.value
        val encoderActive = ::genericStream.isInitialized && (genericStream.isStreaming || genericStream.isRecording)
        if (!StreamTelemetryPolicy.shouldExposeFps(previous, encoderActive)) {
            if (previous.fps != 0) StreamingStatusBus.update(previous.copy(fps = 0))
            return
        }
        StreamingStatusBus.update(previous.copy(fps = fps.coerceAtLeast(0)))
    }

    private fun resetTelemetryForInactiveSession() {
        val previous = StreamingStatusBus.state.value
        StreamingStatusBus.update(
            previous.copy(
                elapsedSeconds = 0L,
                bitrateKbps = 0,
                fps = 0,
                droppedFrames = -1,
                audioLevel = -1,
                recording = false,
                recordingState = RecordingState.IDLE,
                encoderReady = false,
            ),
        )
    }

    private fun resetCaptureStateForPreparation() {
        captureReady = false
        previewAttached = false
        resetTelemetryForInactiveSession()
    }

    private suspend fun releaseCapturePipelineForReprepare(): Boolean {
        sessionGeneration.incrementAndGet()
        releaseProjection()
        val released = releaseGenericStream("reprepare")
        if (!released || !PipelineReleasePolicy.canCreateNewPipeline(pipelineReleaseState)) {
            prepared = false
            captureReady = false
            previewAttached = false
            publish(StreamStatus.ERROR, "Previous capture resources are not fully released. Tap Fix and retry")
            return false
        }
        microphoneSource = null
        cameraSource = null
        captureReady = false
        previewAttached = false
        prepared = false
        genericStream = createGenericStream()
        genericStreamReleased = false
        pipelineReleaseState = PipelineReleaseState.AVAILABLE
        prepared = prepareGenericStream()
        return prepared
    }

    private fun releaseGenericStream(reason: String): Boolean {
        if (!::genericStream.isInitialized) {
            genericStreamReleased = true
            pipelineReleaseState = PipelineReleaseState.TERMINAL
            return true
        }
        if (genericStreamReleased && pipelineReleaseState == PipelineReleaseState.TERMINAL) return true
        val generation = sessionGeneration.get()
        val attempt = PipelineReleasePolicy.begin(pipelineReleaseState, generation) ?: return false
        pipelineReleaseState = PipelineReleaseState.RELEASING
        StreamingDiagnostics.record(currentSessionId, generation, "pipeline_release_begin", reason)
        runCatching { if (genericStream.isRecording) genericStream.stopRecord() }
            .onFailure { StreamingDiagnostics.record(currentSessionId, generation, "pipeline_release_error", "stop_record: ${it.message.orEmpty()}") }
        runCatching { if (genericStream.isStreaming) genericStream.stopStream() }
            .onFailure { StreamingDiagnostics.record(currentSessionId, generation, "pipeline_release_error", "stop_stream: ${it.message.orEmpty()}") }
        runCatching { if (genericStream.isOnPreview) genericStream.stopPreview() }
            .onFailure { StreamingDiagnostics.record(currentSessionId, generation, "pipeline_release_error", "stop_preview: ${it.message.orEmpty()}") }
        runCatching { genericStream.getGlInterface().stop() }
            .onFailure { StreamingDiagnostics.record(currentSessionId, generation, "pipeline_release_error", "stop_gl: ${it.message.orEmpty()}") }
        val releaseSucceeded = runCatching {
            genericStream.release()
            true
        }.onFailure {
            StreamingDiagnostics.record(currentSessionId, generation, "pipeline_release_error", "release_stream: ${it.message.orEmpty()}")
        }.getOrDefault(false)
        pipelineReleaseState = PipelineReleasePolicy.complete(
            state = pipelineReleaseState,
            attempt = attempt,
            currentGeneration = sessionGeneration.get(),
            releaseSucceeded = releaseSucceeded,
        )
        genericStreamReleased = pipelineReleaseState == PipelineReleaseState.TERMINAL
        if (genericStreamReleased) {
            runCatching { microphoneSource?.release() }
                .onFailure { StreamingDiagnostics.record(currentSessionId, generation, "pipeline_release_error", "release_microphone: ${it.message.orEmpty()}") }
            runCatching { cameraSource?.release() }
                .onFailure { StreamingDiagnostics.record(currentSessionId, generation, "pipeline_release_error", "release_camera: ${it.message.orEmpty()}") }
            StreamingDiagnostics.record(currentSessionId, generation, "pipeline_release_complete", reason)
        } else {
            StreamingDiagnostics.record(currentSessionId, generation, "pipeline_release_incomplete", reason)
        }
        return genericStreamReleased
    }

    override fun onCreate() {
        super.onCreate()
        serviceDestroyed = false
        historyStore = CreatorHistoryStore(applicationContext)
        analyticsStore = LocalAnalyticsStore(applicationContext)
        streamQuality = StreamQualityStore(applicationContext).load()
        audioSettings = AudioSettingsStore(applicationContext).load()
        latencyMode = LatencyModeStore(applicationContext).load()
        createNotificationChannel()
        registerNetworkCallback()
        genericStream = createGenericStream()
        prepared = prepareGenericStream()
        if (!prepared) publish(StreamStatus.ERROR, "This device cannot prepare the requested capture profile")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PREPARE_PROJECTION -> serviceScope.launch {
                capturePreparationMutex.withLock {
                    prepareProjectionFromIntent(intent)
                    tryStartPending()
                }
            }
            ACTION_PREPARE_CAMERA -> serviceScope.launch {
                capturePreparationMutex.withLock {
                    prepareCamera()
                    tryStartPending()
                }
            }
            ACTION_ATTACH_PREVIEW -> attachPreview(
                readPreviewSurface(intent),
                intent.getIntExtra(EXTRA_PREVIEW_WIDTH, 0),
                intent.getIntExtra(EXTRA_PREVIEW_HEIGHT, 0),
                intent.getLongExtra(EXTRA_PREVIEW_TOKEN, 0L),
            )
            ACTION_DETACH_PREVIEW -> detachPreview(intent.getLongExtra(EXTRA_PREVIEW_TOKEN, 0L))
            ACTION_START -> queueStart(PendingStart(decodeEndpoints(intent.getStringExtra(EXTRA_ENDPOINT).orEmpty()), intent.getStringExtra(EXTRA_SCENE_JSON).orEmpty(), practice = false))
            ACTION_START_PRACTICE -> queueStart(PendingStart(endpoints = emptyList(), sceneJson = intent.getStringExtra(EXTRA_SCENE_JSON).orEmpty(), practice = true))
            ACTION_CREATE_MARKER -> createMarker(intent.getStringExtra(EXTRA_MARKER_LABEL).orEmpty())
            ACTION_STOP -> stopStreaming()
            ACTION_TOGGLE_MUTE -> toggleMute()
            ACTION_SWITCH_CAMERA -> switchCamera()
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording()
            ACTION_DISMISS_STATUS -> dismissStatusMessage()
            ACTION_RELEASE_CAPTURE -> releaseCaptureAfterFailure()
            ACTION_ENCODER_GRAPHICS_FAILURE -> handleEncoderGraphicsFailure(
                intent.getLongExtra(EXTRA_PIPELINE_GENERATION, 0L).takeIf { it > 0L },
            )
        }
        return START_NOT_STICKY
    }

    private suspend fun prepareProjectionFromIntent(intent: Intent) {
        if (!startForegroundSafely(includeProjection = true, includeCamera = false)) return
        if (!releaseCapturePipelineForReprepare()) return
        resetCaptureStateForPreparation()
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
        val projection = projectionManager.getMediaProjection(resultCode, data) ?: return false
        intentionallyReleasingProjection = false
        mediaProjection = projection
        projection.registerCallback(projectionCallback, handler)

        return runCatching {
            genericStream.changeVideoSource(ScreenSource(applicationContext, projection))
            cameraSource = null
            microphoneSource?.release()
            microphoneSource = MicrophoneSource().also { genericStream.changeAudioSource(it) }
            captureReady = true
            true
        }.getOrDefault(false).also {
            if (!it) {
                captureReady = false
                releaseProjection()
            }
        }
    }

    private suspend fun prepareCamera() {
        if (!startForegroundSafely(includeProjection = false, includeCamera = true)) return
        if (!releaseCapturePipelineForReprepare()) return
        resetCaptureStateForPreparation()
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
            cameraSource = Camera2Source(applicationContext).also { genericStream.changeVideoSource(it) }
            microphoneSource?.release()
            microphoneSource = MicrophoneSource().also { genericStream.changeAudioSource(it) }
            captureReady = true
            publish(StreamStatus.PREPARING, "Camera and microphone ready • waiting for Studio preview")
            attachPreviewIfPossible()
        }.onFailure {
            runCatching { cameraSource?.release() }
            cameraSource = null
            captureReady = false
            publish(StreamStatus.ERROR, "Camera could not start: ${it.message.orEmpty()}")
        }
    }

    private fun switchCamera() {
        postSerialized {
            val source = cameraSource
            if (source == null || !captureReady) {
                publish(StreamStatus.ERROR, "Camera switching is available while camera capture is prepared")
                return@postSerialized
            }
            runCatching {
                source.switchCamera()
            }.onSuccess {
                StreamingDiagnostics.record(currentSessionId, sessionGeneration.get(), "camera_switched")
                publish(StreamingStatusBus.state.value.status, "Camera switched")
            }.onFailure {
                StreamingDiagnostics.record(currentSessionId, sessionGeneration.get(), "camera_switch_failed", it.message.orEmpty())
                publish(StreamStatus.ERROR, "Camera could not switch: ${it.message.orEmpty()}")
            }
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

    private fun queueStart(request: PendingStart) {
        val current = StreamingStatusBus.state.value
        if (pendingStart != null || !StreamStateMachine.acceptsQueuedStart(current.status)) {
            return
        }
        pendingStart = request
        manualStop = false
        if (startForegroundSafely(includeProjection = mediaProjection != null, includeCamera = cameraSource != null)) tryStartPending()
    }

    private fun attachPreview(surface: Surface?, width: Int, height: Int, token: Long) {
        if (surface == null || !surface.isValid || width <= 0 || height <= 0) {
            publish(StreamStatus.ERROR, "Studio preview surface is unavailable")
            return
        }
        val sameSurface = previewSurface === surface
        if (previewAttached && PreviewSurfaceIdentityPolicy.shouldReuse(
                currentToken = previewSurfaceToken,
                incomingToken = token,
                sameSurfaceObject = sameSurface,
                currentWidth = previewWidth,
                currentHeight = previewHeight,
                incomingWidth = width,
                incomingHeight = height,
            )
        ) {
            return
        }
        if (previewAttached && !sameSurface) releasePreviewForCaptureChange()
        previewSurface = surface
        previewSurfaceToken = token
        previewWidth = width
        previewHeight = height
        attachPreviewIfPossible()
        tryStartPending()
    }

    private fun releasePreviewForCaptureChange() {
        if (!::genericStream.isInitialized) {
            previewAttached = false
            return
        }
        runCatching {
            if (genericStream.isOnPreview) genericStream.stopPreview()
            genericStream.getGlInterface().clearFilters()
        }
        previewAttached = false
        val previous = StreamingStatusBus.state.value
        if (previous.previewReady) {
            StreamingStatusBus.update(previous.copy(previewReady = false, message = previous.message))
        }
    }

    private fun attachPreviewIfPossible() {
        if (isolateLivePreviewForDevice) {
            previewAttached = false
            val previous = StreamingStatusBus.state.value
            StreamingStatusBus.update(
                previous.copy(
                    captureReady = captureReady,
                    encoderReady = prepared,
                    previewReady = false,
                    message = if (pendingStart != null) {
                        "Capture ready • starting without local preview for device stability"
                    } else {
                        "Capture ready • local preview paused for device stability"
                    },
                ),
            )
            if (pendingStart == null && StreamingStatusBus.state.value.status == StreamStatus.PREPARING) {
                publish(StreamStatus.IDLE, "Capture ready • local preview paused for device stability")
            }
            tryStartPending()
            return
        }
        val surface = previewSurface ?: return
        if (!prepared || !captureReady || !surface.isValid || previewWidth <= 0 || previewHeight <= 0) return
        runCatching {
            if (genericStream.isOnPreview) genericStream.stopPreview()
            genericStream.startPreview(surface, previewWidth, previewHeight)
            previewAttached = true
            val previous = StreamingStatusBus.state.value
            StreamingStatusBus.update(previous.copy(captureReady = captureReady, encoderReady = prepared, previewReady = true, message = if (pendingStart != null) "Preview is ready • starting capture" else previous.message))
        }.onFailure {
            previewAttached = false
            publish(StreamStatus.ERROR, "Preview could not start: ${it.message.orEmpty()}")
        }
    }

    private fun detachPreview(token: Long) {
        if (PreviewSurfaceIdentityPolicy.isStaleDetach(previewSurfaceToken, token)) return
        releasePreviewForCaptureChange()
        previewSurface = null
        previewSurfaceToken = 0L
        previewWidth = 0
        previewHeight = 0
        val previous = StreamingStatusBus.state.value
        if (previous.status == StreamStatus.PREPARING && pendingStart != null) {
            publish(StreamStatus.PREPARING, "Waiting for Studio preview surface")
        } else {
            StreamingStatusBus.update(previous.copy(previewReady = false, message = previous.message))
        }
    }

    private fun decodeEndpoints(encoded: String): List<String> = encoded
        .split(ENDPOINT_SEPARATOR)
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .take(MAX_DIRECT_DESTINATIONS)

    private fun tryStartPending() {
        val request = pendingStart ?: return
        val previewRequired = !isolateLivePreviewForDevice
        if (!prepared || !captureReady) {
            publish(
                StreamStatus.PREPARING,
                if (previewRequired && !previewAttached) "Capture ready • preview optional; preparing session" else "Preparing capture",
            )
            handler.removeCallbacks(captureTimeout)
            handler.postDelayed(captureTimeout, CAPTURE_TIMEOUT_MS)
            return
        }
        pendingStart = null
        handler.removeCallbacks(captureTimeout)
        if (request.practice) startPractice(request.sceneJson) else startStream(request.endpoints, request.sceneJson)
    }

    private fun readPreviewSurface(intent: Intent): Surface? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(EXTRA_PREVIEW_SURFACE, Surface::class.java)
    } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(EXTRA_PREVIEW_SURFACE)
    }

    private data class PendingStart(val endpoints: List<String>, val sceneJson: String, val practice: Boolean)
    private data class SceneRenderReport(val textOverlays: Int, val unsupportedLayers: Int)

    private fun applySceneOverlays(sceneJson: String): SceneRenderReport {
        val scene = ScenePayloadCodec.decode(sceneJson) ?: return SceneRenderReport(0, 0)
        val plan = SceneCompositionPlan.from(scene)
        val textSources = scene.sources.filter { it.enabled && it.type == SourceType.TEXT && it.textContent.isNotBlank() }.sortedBy { it.zIndex }
        val unsupported = plan.unsupportedLayerCount
        genericStream.getGlInterface().clearFilters()
        val scaledDensity = resources.displayMetrics.density * resources.configuration.fontScale
        textSources.forEachIndexed { index, source ->
            val filter = TextObjectFilterRender().apply {
                setText(source.textContent, source.textSizeSp * scaledDensity, source.textColor.toInt(), source.fillColor.toInt())
                setAlpha(source.opacity.coerceIn(0f, 1f))
                setScale((source.width * 100f).coerceIn(5f, 100f), (source.height * 100f).coerceIn(5f, 100f))
                setPosition((source.x * 100f).coerceIn(0f, 100f), (source.y * 100f).coerceIn(0f, 100f))
            }
            if (index == 0) genericStream.getGlInterface().setFilter(filter) else genericStream.getGlInterface().addFilter(filter)
        }
        return SceneRenderReport(plan.textOverlayCount, unsupported)
    }

    private fun startPractice(sceneJson: String) {
        if (!prepared || !captureReady || microphoneSource == null) {
            publish(StreamStatus.ERROR, "Practice capture is not ready. Approve capture and wait for the capture pipeline")
            return
        }
        if (!runEnvironmentPreflight(practice = true)) return
        manualStop = false
        currentEndpoint = ""
        currentEndpoints = emptyList()
        reconnectAttempt = 0
        autoStopSeconds = AutoStopStore(applicationContext).load().seconds
        currentSessionId = "session-${System.currentTimeMillis()}"
        sessionHealthSamples.clear()
        StreamingStatusBus.clearHealth()
        val previous = StreamingStatusBus.state.value
        val report = applySceneOverlays(sceneJson)
        val sceneMessage = if (report.unsupportedLayers > 0) "Practice mode active • ${report.textOverlays} text overlay(s) rendered; some scene layers are not yet composited" else "Practice mode active • ${report.textOverlays} text overlay(s) rendered"
        StreamingStatusBus.update(previous.copy(mode = SessionMode.PRACTICE, bitrateKbps = 0, fps = 0, droppedFrames = -1, audioLevel = -1, message = "Starting local practice recording"))
        startRecording(prefix = "unictoos-practice")
        publish(StreamStatus.LIVE, sceneMessage)
    }

    private fun startStream(endpoints: List<String>, sceneJson: String) {
        StreamingStatusBus.clearHealth()
        val previous = StreamingStatusBus.state.value
        StreamingStatusBus.update(previous.copy(mode = SessionMode.BROADCAST, bitrateKbps = 0, fps = 0, droppedFrames = -1, audioLevel = -1))
        if (!RecordingReadinessPolicy.isReady(captureReady, prepared) || microphoneSource == null) {
            publish(StreamStatus.ERROR, "Capture is not ready. Approve capture and wait for the capture pipeline")
            return
        }
        if (endpoints.isEmpty()) {
            publish(StreamStatus.ERROR, "Configure at least one streaming destination before going live")
            return
        }
        if (!endpoints.all(::runEndpointPreflight)) return
        if (!runEnvironmentPreflight(practice = false)) return
        if (genericStream.isStreaming) return
        manualStop = false
        autoStopSeconds = AutoStopStore(applicationContext).load().seconds
        currentSessionId = "session-${System.currentTimeMillis()}"
        sessionHealthSamples.clear()
        currentEndpoints = endpoints
        currentEndpoint = endpoints.firstOrNull().orEmpty()
        val report = applySceneOverlays(sceneJson)
        val sceneMessage = if (report.unsupportedLayers > 0) "Connecting to your destination • ${report.textOverlays} text overlay(s) rendered; some scene layers are not yet composited" else "Connecting to your destination • ${report.textOverlays} text overlay(s) rendered"
        publish(StreamStatus.CONNECTING, sceneMessage)
        runCatching {
            genericStream.startStream(endpoints)
            scheduleConnectionWatchdog(sessionGeneration.get())
        }.onFailure {
            cancelConnectionWatchdog()
            publish(StreamStatus.ERROR, "Unable to start the stream: ${it.message.orEmpty()}")
        }
    }

    private fun stopStreaming(reason: String = "Broadcast stopped") {
        manualStop = true
        cancelConnectionWatchdog()
        val current = StreamingStatusBus.state.value
        val hadPendingStart = pendingStart != null
        pendingStart = null
        reconnectScheduled = false
        reconnectRunnable?.let(handler::removeCallbacks)
        reconnectRunnable = null
        handler.removeCallbacksAndMessages(null)
        sessionGeneration.incrementAndGet()
        elapsedTickerGeneration = 0L
        if (!hadPendingStart && !StreamStateMachine.acceptsStop(current.status)) return
        if (current.status != StreamStatus.STOPPING && current.status != StreamStatus.STOPPED && current.status != StreamStatus.IDLE) {
            publish(StreamStatus.STOPPING, if (hadPendingStart) "Cancelling capture" else "Stopping session")
        }
        releasePreviewForCaptureChange()
        if (::genericStream.isInitialized && genericStream.isStreaming) genericStream.stopStream()
        if (StreamingStatusBus.state.value.recordingState in setOf(RecordingState.STARTING, RecordingState.RECORDING)) {
            pendingTerminalStopReason = reason
            stopRecording()
            return
        }
        completeStop(reason)
    }

    private fun completeStop(reason: String) {
        pendingTerminalStopReason = null
        val completed = StreamingStatusBus.state.value
        if (sessionHealthSamples.isNotEmpty()) historyStore.addHealthSamples(sessionHealthSamples.toList())
        if (completed.elapsedSeconds > 0L) {
            val summary = SessionSummary(
                id = currentSessionId.ifBlank { "session-${System.currentTimeMillis()}" },
                mode = completed.mode,
                elapsedSeconds = completed.elapsedSeconds,
                bitrateKbps = completed.bitrateKbps,
                fps = completed.fps,
                droppedFrames = completed.droppedFrames,
                finishedAtMillis = System.currentTimeMillis(),
            )
            historyStore.addSession(summary)
            runCatching {
                analyticsStore.recordSession(
                    summary = summary,
                    platform = if (completed.mode == SessionMode.PRACTICE) "practice" else "direct",
                    reconnectCount = reconnectAttempt,
                    healthSamples = sessionHealthSamples.toList(),
                )
            }.onFailure { error ->
                StreamingDiagnostics.record(currentSessionId, sessionGeneration.get(), "analytics_write_failed", error.message.orEmpty())
            }
        }
        startedAtElapsed = 0L
        adaptiveTargetBitrate = 0
        degradedSinceElapsed = 0L
        recoveredSinceElapsed = 0L
        thermalCapApplied = false
        highThermalSinceElapsed = 0L
        bitrateHistory.clear()
        reconnectAttempt = 0
        currentEndpoint = ""
        currentEndpoints = emptyList()
        autoStopSeconds = 0L
        currentSessionId = ""
        sessionHealthSamples.clear()
        resetTelemetryForInactiveSession()
        publish(StreamStatus.STOPPED, reason)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseCaptureAfterFailure() {
        graphicsFailureRequested.set(true)
        manualStop = true
        pendingStart = null
        reconnectScheduled = false
        reconnectRunnable?.let(handler::removeCallbacks)
        reconnectRunnable = null
        handler.removeCallbacksAndMessages(null)
        sessionGeneration.incrementAndGet()
        elapsedTickerGeneration = 0L
        val released = releaseFailedPipeline()
        resetTelemetryForInactiveSession()
        StreamingStatusBus.update(
            StreamingStatusBus.state.value.copy(
                status = if (released) StreamStatus.IDLE else StreamStatus.ERROR,
                captureReady = false,
                encoderReady = false,
                previewReady = false,
                recording = false,
                recordingState = RecordingState.IDLE,
                message = if (released) "Capture resources released. Start capture again" else "Capture release is incomplete. Retry Fix before starting again",
            ),
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseFailedPipeline(): Boolean {
        recordingFinalizationJob?.cancel()
        recordingFinalizationJob = null
        pendingTerminalStopReason = null
        releasePreviewForCaptureChange()
        releaseProjection()
        val released = releaseGenericStream("failed_pipeline")
        if (!released) {
            prepared = false
            captureReady = false
            previewAttached = false
            return false
        }
        microphoneSource = null
        cameraSource = null
        captureReady = false
        prepared = false
        previewAttached = false
        previewSurface = null
        previewSurfaceToken = 0L
        previewWidth = 0
        previewHeight = 0
        activeRecordingFile = null
        currentEndpoint = ""
        currentEndpoints = emptyList()
        startedAtElapsed = 0L
        highThermalSinceElapsed = 0L
        thermalCapApplied = false
        return true
    }

    private fun handleEncoderGraphicsFailure(expectedGeneration: Long? = null) {
        if (expectedGeneration != null && !isCurrentGeneration(expectedGeneration)) return
        if (!graphicsFailureRequested.compareAndSet(false, true)) return
        val failedGeneration = sessionGeneration.get()
        StreamingDiagnostics.record(currentSessionId, failedGeneration, "graphics_failure_handling")
        manualStop = true
        pendingStart = null
        reconnectScheduled = false
        reconnectRunnable?.let(handler::removeCallbacks)
        reconnectRunnable = null
        handler.removeCallbacksAndMessages(null)
        sessionGeneration.incrementAndGet()
        elapsedTickerGeneration = 0L
        val released = releaseFailedPipeline()
        resetTelemetryForInactiveSession()
        StreamingDiagnostics.record(
            currentSessionId,
            sessionGeneration.get(),
            if (released) "graphics_failure_released" else "graphics_failure_release_incomplete",
        )
        updateNotification(if (released) "Graphics resources released • tap Fix to retry" else "Graphics release incomplete • tap Fix to retry")
        StreamingStatusBus.update(
            StreamingStatusBus.state.value.copy(
                status = StreamStatus.ERROR,
                captureReady = false,
                encoderReady = false,
                previewReady = false,
                recording = false,
                recordingState = RecordingState.IDLE,
                message = if (released) EncoderCrashPolicy.GRAPHICS_RESOURCE_MESSAGE else "Graphics resources are still releasing. Tap Fix again before retrying",
            ),
        )
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
        val session = StreamingStatusBus.state.value
        if (!RecordingReadinessPolicy.isReady(captureReady, prepared) || session.recordingState != RecordingState.IDLE) {
            publish(StreamStatus.ERROR, "Recording requires a ready capture and encoder pipeline")
            return
        }
        if (!hasRecordingStorage()) {
            publish(StreamStatus.ERROR, "Not enough storage is available for a recording")
            return
        }
        val directory = File(filesDir, "recordings").apply { mkdirs() }
        val output = File(directory, "$prefix-${System.currentTimeMillis()}.mp4")
        activeRecordingFile = output
        val generation = sessionGeneration.get()
        StreamingStatusBus.update(
            StreamingStatusBus.state.value.copy(
                recording = false,
                recordingState = RecordingState.STARTING,
                message = "Starting recording",
            ),
        )
        runCatching {
            genericStream.startRecord(output.absolutePath, object : RecordController.Listener {
                override fun onStatusChange(status: RecordController.Status) {
                    postSerialized {
                        if (!isCurrentGeneration(generation)) return@postSerialized
                        val recording = status == RecordController.Status.STARTED || status == RecordController.Status.RECORDING || status == RecordController.Status.RESUMED
                        val previous = StreamingStatusBus.state.value
                        StreamingStatusBus.update(
                            previous.copy(
                                recording = recording,
                                recordingState = if (recording) RecordingState.RECORDING else previous.recordingState,
                                message = if (recording) "Recording ${output.name}" else previous.message,
                            ),
                        )
                    }
                }
            })
        }.onFailure {
            activeRecordingFile = null
            StreamingStatusBus.update(StreamingStatusBus.state.value.copy(recording = false, recordingState = RecordingState.FAILED))
            publish(StreamStatus.ERROR, "Recording could not start: ${it.message.orEmpty()}")
        }
    }

    private fun stopRecording() {
        val current = StreamingStatusBus.state.value
        if (!::genericStream.isInitialized || current.recordingState !in setOf(RecordingState.STARTING, RecordingState.RECORDING)) return
        val generation = sessionGeneration.get()
        val output = activeRecordingFile
        activeRecordingFile = null
        runCatching { genericStream.stopRecord() }
            .onFailure { publish(StreamStatus.ERROR, "Recording could not finalize: ${it.message.orEmpty()}") }
        StreamingStatusBus.update(StreamingStatusBus.state.value.copy(recording = false, recordingState = RecordingState.STOPPING, message = "Finalizing recording…"))
        recordingFinalizationJob?.cancel()
        recordingFinalizationJob = serviceScope.launch {
            val result = withContext(Dispatchers.IO) { output?.let(RecordingValidator::validateWhenStable) }
            if (!isCurrentGeneration(generation)) return@launch
            val message = when (result) {
                is RecordingValidation.Valid -> "Recording saved on this device • ${result.durationMs / 1_000}s verified"
                is RecordingValidation.Invalid -> "Recording may be incomplete: ${result.reason}"
                null -> "Recording stopped"
            }
            val finalState = if (result is RecordingValidation.Invalid) RecordingState.FAILED else RecordingState.IDLE
            StreamingStatusBus.update(StreamingStatusBus.state.value.copy(recording = false, recordingState = finalState, message = message))
            pendingTerminalStopReason?.let { completeStop(it) }
        }
    }

    private fun estimateRecordingBytes(): Long {
        val expectedSeconds = AutoStopStore(applicationContext).load().seconds.takeIf { it > 0L } ?: 600L
        val bitsPerSecond = (streamQuality.bitrate + audioSettings.bitrate).toLong()
        return (bitsPerSecond * expectedSeconds / 8L * 1.20f).toLong().coerceAtLeast(MIN_RECORDING_BYTES)
    }

    private fun hasRecordingStorage(): Boolean {
        val stats = StatFs(filesDir.absolutePath)
        val availableBytes = stats.availableBlocksLong * stats.blockSizeLong
        return availableBytes >= estimateRecordingBytes()
    }

    private fun runEndpointPreflight(endpoint: String): Boolean {
        return when (val result = StreamPreflight.validateEndpoint(endpoint, practice = false)) {
            PreflightResult.Ready -> true
            is PreflightResult.Blocked -> {
                publish(StreamStatus.ERROR, result.message)
                false
            }
        }
    }

    private fun runEnvironmentPreflight(practice: Boolean): Boolean {
        val batteryIntent = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else -1
        val chargingStatus = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = chargingStatus == BatteryManager.BATTERY_STATUS_CHARGING || chargingStatus == BatteryManager.BATTERY_STATUS_FULL
        val thermalStatus = getSystemService(PowerManager::class.java)?.currentThermalStatus
            ?: PowerManager.THERMAL_STATUS_NONE
        val connectivity = getSystemService(ConnectivityManager::class.java)
        val capabilities = connectivity?.getNetworkCapabilities(connectivity.activeNetwork)
        val networkAvailable = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val storage = StatFs(filesDir.absolutePath)
        val availableBytes = storage.availableBlocksLong * storage.blockSizeLong
        val profileResult = StreamPreflight.validateProfile(streamQuality)
        val environmentResult = StreamPreflight.validateEnvironment(
            networkAvailable = practice || networkAvailable,
            availableStorageBytes = availableBytes,
            batteryPercent = batteryPercent,
            isCharging = charging,
            thermalStatus = thermalStatus,
            minimumStorageBytes = MIN_RECORDING_BYTES,
            storageMode = if (practice) StorageSessionMode.PRACTICE_RECORDING else StorageSessionMode.STREAM_ONLY,
            estimatedRecordingBytes = if (practice) estimateRecordingBytes() else 0L,
        )
        val result = when (profileResult) {
            PreflightResult.Ready -> environmentResult
            is PreflightResult.Blocked -> profileResult
        }
        return when (result) {
            PreflightResult.Ready -> true
            is PreflightResult.Blocked -> {
                publish(StreamStatus.ERROR, result.message)
                false
            }
        }
    }

    private fun scheduleReconnect(reason: String) {
        if (manualStop || reconnectScheduled) return
        val decision = StreamFailurePolicy.classify(reason)
        if (!decision.retryable) {
            manualStop = true
            publish(StreamStatus.ERROR, decision.userMessage)
            return
        }
        if (currentEndpoint.isBlank() || reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) {
            publish(StreamStatus.ERROR, "${decision.userMessage}. Reconnect limit reached; check the destination and network")
            return
        }
        reconnectAttempt += 1
        reconnectScheduled = true
        val jitter = reconnectJitter.nextLong(0L, RECONNECT_JITTER_MAX_MS + 1L)
        val delay = StreamFailurePolicy.reconnectDelayMs(reconnectAttempt, jitter)
        val generation = sessionGeneration.get()
        publish(StreamStatus.RECONNECTING, "${decision.userMessage} • retry $reconnectAttempt/$MAX_RECONNECT_ATTEMPTS")
        reconnectRunnable = Runnable {
            reconnectRunnable = null
            reconnectScheduled = false
            if (!isCurrentGeneration(generation) || manualStop || currentEndpoint.isBlank()) return@Runnable
            publish(StreamStatus.CONNECTING, "Reconnecting securely")
            runCatching {
                genericStream.startStream(currentEndpoints.ifEmpty { listOf(currentEndpoint) })
                scheduleConnectionWatchdog(generation)
            }.onFailure {
                cancelConnectionWatchdog()
                scheduleReconnect(it.message ?: "Reconnect failed")
            }
        }.also { runnable -> handler.postDelayed(runnable, delay) }
    }

    private fun startForegroundSafely(includeProjection: Boolean, includeCamera: Boolean): Boolean {
        val serviceTypes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                (if (includeProjection) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0) or
                (if (includeCamera) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA else 0)
        } else if (includeProjection) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        } else {
            0
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
        if (!StreamStateMachine.canTransition(previous.status, status)) return
        StreamingDiagnostics.record(currentSessionId, sessionGeneration.get(), "state_${status.name.lowercase()}", message.orEmpty())
        if (status == StreamStatus.LIVE && startedAtElapsed == 0L) {
            startedAtElapsed = SystemClock.elapsedRealtime()
            elapsedTickerGeneration = sessionGeneration.get()
            adaptiveTargetBitrate = streamQuality.bitrate
            degradedSinceElapsed = 0L
            recoveredSinceElapsed = 0L
            handler.removeCallbacks(elapsedTicker)
            handler.post(elapsedTicker)
        }
        StreamingStatusBus.update(
            previous.copy(
                status = status,
                fps = previous.fps,
                reconnectAttempt = reconnectAttempt,
                captureReady = captureReady,
                encoderReady = prepared,
                previewReady = previewAttached,
                pipelineGeneration = sessionGeneration.get(),
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
        val thermalStatus = getSystemService(PowerManager::class.java)?.currentThermalStatus
            ?: PowerManager.THERMAL_STATUS_NONE
        val nowElapsed = SystemClock.elapsedRealtime()
        if (thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE) {
            if (highThermalSinceElapsed == 0L) highThermalSinceElapsed = nowElapsed
        } else if (ThermalProtectionPolicy.resetOnRecovery(thermalStatus, PowerManager.THERMAL_STATUS_MODERATE)) {
            highThermalSinceElapsed = 0L
            thermalCapApplied = false
        }
        if (ThermalProtectionPolicy.shouldThrottle(
                enabled = ThermalProtectionStore(applicationContext).isEnabled(),
                thermalStatus = thermalStatus,
                highThermalSinceElapsedMs = highThermalSinceElapsed,
                nowElapsedMs = nowElapsed,
                alreadyApplied = thermalCapApplied,
                moderateStatus = PowerManager.THERMAL_STATUS_MODERATE,
            ) && prepared
        ) {
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
                droppedFrames = -1,
                audioLevel = -1,
                batteryPercent = batteryPercent,
                thermalStatus = thermalStatus,
                networkLabel = networkLabel,
            )
        sessionHealthSamples += sample
        while (sessionHealthSamples.size > MAX_SESSION_HEALTH_SAMPLES) sessionHealthSamples.removeAt(0)
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

    private fun buildNotification(text: String = "Broadcast controls are active"): Notification {
        val state = StreamingStatusBus.state.value
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val openIntent = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            Intent(this, MainActivity::class.java),
            flags,
        )
        fun serviceAction(requestCode: Int, action: String): PendingIntent = PendingIntent.getService(
            this,
            requestCode,
            Intent(this, StreamingForegroundService::class.java).setAction(action),
            flags,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_unictoos)
            .setContentTitle("Unictoos Studio")
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_unictoos, if (state.microphoneMuted) "Unmute" else "Mute", serviceAction(REQUEST_MUTE, ACTION_TOGGLE_MUTE))
            .addAction(R.drawable.ic_unictoos, if (state.recording) "Stop record" else "Record", serviceAction(REQUEST_RECORD, if (state.recording) ACTION_STOP_RECORDING else ACTION_START_RECORDING))
            .addAction(R.drawable.ic_unictoos, "Stop", serviceAction(REQUEST_STOP, ACTION_STOP))
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Broadcasting", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Keeps Unictoos capture and broadcast controls available"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        serviceDestroyed = true
        manualStop = true
        sessionGeneration.incrementAndGet()
        reconnectRunnable?.let(handler::removeCallbacks)
        reconnectRunnable = null
        recordingFinalizationJob?.cancel()
        recordingFinalizationJob = null
        pendingTerminalStopReason = null
        serviceScope.cancel()
        handler.removeCallbacksAndMessages(null)
        if (::genericStream.isInitialized && !genericStreamReleased) {
            releasePreviewForCaptureChange()
            if (StreamingStatusBus.state.value.recordingState in setOf(RecordingState.STARTING, RecordingState.RECORDING, RecordingState.STOPPING)) {
                runCatching { genericStream.stopRecord() }
            }
            releaseGenericStream("service_destroy")
        }
        microphoneSource?.release()
        cameraSource?.release()
        releaseProjection()
        unregisterNetworkCallback()
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

    private fun registerNetworkCallback() {
        if (networkCallbackRegistered) return
        runCatching {
            getSystemService(ConnectivityManager::class.java)?.registerDefaultNetworkCallback(networkCallback)
            networkCallbackRegistered = true
        }
    }

    private fun unregisterNetworkCallback() {
        if (!networkCallbackRegistered) return
        runCatching { getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(networkCallback) }
        networkCallbackRegistered = false
    }

    private fun releaseProjection() {
        val projection = mediaProjection ?: return
        intentionallyReleasingProjection = true
        runCatching { projection.unregisterCallback(projectionCallback) }
        runCatching { projection.stop() }
        mediaProjection = null
    }

    private fun onConnectionStartedForGeneration(url: String, generation: Long) {
        if (!isCurrentGeneration(generation)) return
        publish(StreamStatus.CONNECTING, "Connecting securely")
    }

    private fun onConnectionSuccessForGeneration(generation: Long) {
        if (!isCurrentGeneration(generation)) return
        cancelConnectionWatchdog()
        reconnectAttempt = 0
        reconnectScheduled = false
        val previewWasIsolated = isolateLivePreviewForDevice && runCatching {
            if (::genericStream.isInitialized && genericStream.isOnPreview) {
                // The affected device firmware does not need the local preview surface
                // after the encoder is live. Detach only that surface; keep capture and
                // the streaming encoder running.
                genericStream.stopPreview()
                previewAttached = false
                StreamingStatusBus.update(StreamingStatusBus.state.value.copy(previewReady = false))
                true
            } else {
                false
            }
        }.onFailure {
            StreamingDiagnostics.record(currentSessionId, generation, "live_preview_isolation_failed", it.message.orEmpty())
        }.getOrDefault(false)
        publish(StreamStatus.LIVE, if (previewWasIsolated) "Broadcast is live • preview paused for device stability" else "Broadcast is live")
    }

    private fun onNewBitrateForGeneration(bitrate: Long, generation: Long) {
        if (!isCurrentGeneration(generation)) return
        val state = StreamingStatusBus.state.value
        if (!genericStream.isStreaming || state.status !in setOf(StreamStatus.CONNECTING, StreamStatus.LIVE, StreamStatus.RECONNECTING)) return
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

    private fun onConnectionFailedForGeneration(reason: String, generation: Long) {
        if (!isCurrentGeneration(generation)) return
        cancelConnectionWatchdog()
        StreamingDiagnostics.record(currentSessionId, generation, "connection_failed", reason)
        scheduleReconnect(reason.ifBlank { "Connection failed" })
    }

    private fun onDisconnectForGeneration(generation: Long) {
        if (!isCurrentGeneration(generation)) return
        cancelConnectionWatchdog()
        StreamingDiagnostics.record(currentSessionId, generation, "disconnected")
        scheduleReconnect("Connection lost")
    }

    private fun onAuthErrorForGeneration(generation: Long) {
        if (!isCurrentGeneration(generation)) return
        cancelConnectionWatchdog()
        StreamingDiagnostics.record(currentSessionId, generation, "auth_error")
        manualStop = true
        reconnectScheduled = false
        reconnectRunnable?.let(handler::removeCallbacks)
        reconnectRunnable = null
        currentEndpoint = ""
        currentEndpoints = emptyList()
        runCatching { if (::genericStream.isInitialized && genericStream.isStreaming) genericStream.stopStream() }
        releasePreviewForCaptureChange()
        resetTelemetryForInactiveSession()
        publish(StreamStatus.ERROR, "Destination rejected the stream key. Check the selected platform and rotate the key if needed")
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun onAuthSuccessForGeneration(generation: Long) {
        if (!isCurrentGeneration(generation)) return
        publish(StreamStatus.CONNECTING, "Destination authenticated")
    }

    override fun onConnectionStarted(url: String) = postSerialized {
        onConnectionStartedForGeneration(url, sessionGeneration.get())
    }

    override fun onConnectionSuccess() = postSerialized {
        onConnectionSuccessForGeneration(sessionGeneration.get())
    }

    override fun onNewBitrate(bitrate: Long) = postSerialized {
        onNewBitrateForGeneration(bitrate, sessionGeneration.get())
    }

    override fun onConnectionFailed(reason: String) = postSerialized {
        onConnectionFailedForGeneration(reason, sessionGeneration.get())
    }

    override fun onDisconnect() = postSerialized {
        onDisconnectForGeneration(sessionGeneration.get())
    }

    override fun onAuthError() = postSerialized {
        onAuthErrorForGeneration(sessionGeneration.get())
    }

    override fun onAuthSuccess() = postSerialized {
        onAuthSuccessForGeneration(sessionGeneration.get())
    }

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
        const val ACTION_SWITCH_CAMERA = "com.unictoai.unictoos.action.SWITCH_CAMERA"
        const val ACTION_START_RECORDING = "com.unictoai.unictoos.action.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.unictoai.unictoos.action.STOP_RECORDING"
        const val ACTION_DISMISS_STATUS = "com.unictoai.unictoos.action.DISMISS_STATUS"
        const val ACTION_RELEASE_CAPTURE = "com.unictoai.unictoos.action.RELEASE_CAPTURE"
        const val ACTION_ENCODER_GRAPHICS_FAILURE = "com.unictoai.unictoos.action.ENCODER_GRAPHICS_FAILURE"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_PROJECTION_DATA = "extra_projection_data"
        const val EXTRA_ENDPOINT = "extra_endpoint"
        const val EXTRA_SCENE_JSON = "extra_scene_json"
        const val EXTRA_PREVIEW_SURFACE = "extra_preview_surface"
        const val EXTRA_PREVIEW_WIDTH = "extra_preview_width"
        const val EXTRA_PREVIEW_HEIGHT = "preview_height"
        const val EXTRA_PREVIEW_TOKEN = "preview_token"
        const val EXTRA_PIPELINE_GENERATION = "pipeline_generation"
        const val EXTRA_MARKER_LABEL = "extra_marker_label"
        private const val ENDPOINT_SEPARATOR = "\u001F"
        private const val MAX_DIRECT_DESTINATIONS = 2
        private const val CHANNEL_ID = "unictoos-broadcasting"
        private const val NOTIFICATION_ID = 4101
        private const val REQUEST_OPEN = 4102
        private const val REQUEST_MUTE = 4103
        private const val REQUEST_RECORD = 4104
        private const val REQUEST_STOP = 4105
        private const val MAX_RECONNECT_ATTEMPTS = 3
        private const val CAPTURE_TIMEOUT_MS = 30_000L
        private const val RECONNECT_JITTER_MAX_MS = 500L
        private const val MIN_RECORDING_BYTES = 64L * 1024L * 1024L
        private const val MAX_SESSION_HEALTH_SAMPLES = 1_200

        private fun formatElapsed(seconds: Long): String = "%02d:%02d".format(seconds / 60, seconds % 60)
    }
}
