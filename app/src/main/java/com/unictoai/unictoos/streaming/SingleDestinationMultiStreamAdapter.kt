package com.unictoai.unictoos.streaming

import android.content.Context
import android.view.Surface
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.sources.audio.AudioSource
import com.pedro.encoder.input.sources.video.VideoSource
import com.pedro.library.base.recording.RecordController
import com.pedro.library.multiple.MultiStream
import com.pedro.library.multiple.MultiType
import com.pedro.library.util.FpsListener
import com.pedro.library.util.streamclient.StreamBaseClient
import com.pedro.library.view.GlInterface
import com.pedro.library.view.RenderErrorCallback
import com.pedro.encoder.input.sources.audio.NoAudioSource
import com.pedro.encoder.input.sources.video.NoVideoSource
import com.unictoai.unictoos.health.DestinationSlotEvent
import com.unictoai.unictoos.health.HealthState
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Single-destination compatibility surface for the first runtime migration.
 *
 * It preserves the service's existing lifecycle vocabulary while replacing the
 * GenericStream transport owner with RootEncoder MultiStream at slot zero. This
 * keeps the same capture and encoded-media pipeline while allowing a bounded
 * two-endpoint RTMP/RTMPS/SRT fan-out. The service remains responsible for
 * endpoint validation and aggregate lifecycle policy.
 */
class SingleDestinationMultiStreamAdapter(
    context: Context,
    connectChecker: ConnectChecker,
    private val onSlotEvent: (DestinationSlotEvent) -> Unit = {},
) {
    private val closed = AtomicBoolean(false)
    private val trackerLock = Any()
    private val activeSlots = linkedSetOf<Int>()
    private val successfulSlots = mutableSetOf<Int>()
    private val authenticatedSlots = mutableSetOf<Int>()
    private val failureReported = AtomicBoolean(false)
    private val disconnectReported = AtomicBoolean(false)
    private val authErrorReported = AtomicBoolean(false)
    private val rtmpCheckers: Array<ConnectChecker> = Array(MAX_DESTINATIONS) { index -> SlotConnectChecker(index, connectChecker) }
    private val srtCheckers: Array<ConnectChecker> = Array(MAX_DESTINATIONS) { index -> SlotConnectChecker(index, connectChecker) }
    private val multiStream = MultiStream(
        context,
        rtmpCheckers,
        emptyArray(),
        srtCheckers,
        emptyArray(),
        NoVideoSource(),
        NoAudioSource(),
    )

    val isStreaming: Boolean
        get() = !closed.get() && multiStream.isStreaming

    val isOnPreview: Boolean
        get() = !closed.get() && multiStream.isOnPreview

    val isRecording: Boolean
        get() = !closed.get() && multiStream.isRecording

    fun prepareVideo(width: Int, height: Int, bitrate: Int, rotation: Int = 0): Boolean {
        checkOpen()
        return multiStream.prepareVideo(width, height, bitrate, rotation)
    }

    fun prepareAudio(
        sampleRate: Int,
        stereo: Boolean,
        bitrate: Int,
        echoCanceler: Boolean,
        noiseSuppressor: Boolean,
    ): Boolean {
        checkOpen()
        return multiStream.prepareAudio(sampleRate, stereo, bitrate, echoCanceler, noiseSuppressor)
    }

    fun changeVideoSource(source: VideoSource) {
        checkOpen()
        multiStream.changeVideoSource(source)
    }

    fun changeAudioSource(source: AudioSource) {
        checkOpen()
        multiStream.changeAudioSource(source)
    }

    fun getGlInterface(): GlInterface {
        checkOpen()
        return multiStream.getGlInterface()
    }

    fun getStreamClient(): StreamBaseClient {
        checkOpen()
        return multiStream.getStreamClient(MultiType.RTMP, RTMP_SLOT)
    }

    fun setFpsListener(callback: FpsListener.Callback) {
        checkOpen()
        multiStream.setFpsListener(callback)
    }

    fun startPreview(surface: Surface, width: Int, height: Int) {
        checkOpen()
        multiStream.startPreview(surface, width, height)
    }

    fun stopPreview() {
        if (closed.get()) return
        multiStream.stopPreview()
    }

    fun startStream(endpoint: String) = startStream(listOf(endpoint))

    fun startStream(endpoints: List<String>) {
        checkOpen()
        require(endpoints.isNotEmpty()) { "At least one endpoint is required" }
        require(endpoints.size <= MAX_DESTINATIONS) { "At most $MAX_DESTINATIONS endpoints are supported" }
        val normalizedEndpoints = endpoints.map(String::trim)
        require(normalizedEndpoints.all(StreamEndpointPolicy::isSupported)) { "Every endpoint must be a complete RTMP, RTMPS, or SRT URL" }
        if (isStreaming || synchronized(trackerLock) { activeSlots.isNotEmpty() }) stopStream()
        synchronized(trackerLock) {
            activeSlots.clear()
            successfulSlots.clear()
            authenticatedSlots.clear()
            normalizedEndpoints.indices.forEach(activeSlots::add)
            normalizedEndpoints.indices.forEach { index -> emitSlot(DestinationSlotEvent(index, HealthState.RECONNECTING)) }
            failureReported.set(false)
            disconnectReported.set(false)
            authErrorReported.set(false)
        }
        normalizedEndpoints.forEachIndexed { index, endpoint ->
            multiStream.startStream(transportFor(endpoint), index, endpoint)
        }
    }

    fun stopStream() {
        if (closed.get()) return
        synchronized(trackerLock) {
            activeSlots.clear()
            successfulSlots.clear()
            authenticatedSlots.clear()
        }
        repeat(MAX_DESTINATIONS) { index ->
            runCatching { multiStream.stopStream(MultiType.RTMP, index) }
            runCatching { multiStream.stopStream(MultiType.SRT, index) }
        }
    }

    fun startRecord(path: String, listener: RecordController.Listener) {
        checkOpen()
        multiStream.startRecord(path, RecordController.RecordTracks.ALL, listener)
    }

    fun stopRecord(): Boolean {
        if (closed.get()) return false
        return multiStream.stopRecord()
    }

    fun setVideoBitrateOnFly(bitrate: Int) {
        checkOpen()
        multiStream.setVideoBitrateOnFly(bitrate)
    }

    fun release() {
        if (!closed.compareAndSet(false, true)) return
        synchronized(trackerLock) {
            activeSlots.clear()
            successfulSlots.clear()
            authenticatedSlots.clear()
        }
        var firstFailure: Throwable? = null
        fun attempt(block: () -> Unit) {
            runCatching(block).onFailure { if (firstFailure == null) firstFailure = it }
        }
        repeat(MAX_DESTINATIONS) { index ->
            attempt { multiStream.stopStream(MultiType.RTMP, index) }
            attempt { multiStream.stopStream(MultiType.SRT, index) }
        }
        attempt { multiStream.stopPreview() }
        attempt { multiStream.getGlInterface().stop() }
        attempt { multiStream.release() }
        firstFailure?.let {
            // Keep the adapter retryable when any underlying RootEncoder teardown
            // operation failed. The service's PipelineReleasePolicy must observe
            // the failure instead of incorrectly marking the pipeline released.
            closed.set(false)
            throw it
        }
    }

    private fun emitSlot(event: DestinationSlotEvent) {
        runCatching { onSlotEvent(event) }
    }

    private fun checkOpen() {
        check(!closed.get()) { "MultiStream adapter is closed" }
    }

    private inner class SlotConnectChecker(
        private val slotIndex: Int,
        private val delegate: ConnectChecker,
    ) : ConnectChecker {
        override fun onConnectionStarted(url: String) {
            emitSlot(DestinationSlotEvent(slotIndex, HealthState.RECONNECTING))
            if (isPrimarySlot()) delegate.onConnectionStarted(url)
        }

        override fun onConnectionSuccess() {
            emitSlot(DestinationSlotEvent(slotIndex, HealthState.HEALTHY))
            val shouldPublish = synchronized(trackerLock) {
                successfulSlots += slotIndex
                successfulSlots.containsAll(activeSlots) && activeSlots.isNotEmpty()
            }
            if (shouldPublish) delegate.onConnectionSuccess()
        }

        override fun onNewBitrate(bitrate: Long) {
            emitSlot(DestinationSlotEvent(slotIndex, HealthState.HEALTHY, bitrate = bitrate))
            // The service’s adaptive target is per encoded output. Use slot zero as the
            // authoritative signal so a second destination cannot double the bitrate.
            if (isPrimarySlot()) delegate.onNewBitrate(bitrate)
        }

        override fun onConnectionFailed(reason: String) {
            if (!isActiveSlot()) return
            emitSlot(DestinationSlotEvent(slotIndex, HealthState.FAILED, error = reason.take(240)))
            if (failureReported.compareAndSet(false, true)) {
                delegate.onConnectionFailed("Destination ${slotIndex + 1}: ${reason.ifBlank { "connection failed" }}")
            }
        }

        override fun onDisconnect() {
            if (!isActiveSlot()) return
            emitSlot(DestinationSlotEvent(slotIndex, HealthState.RECONNECTING))
            if (disconnectReported.compareAndSet(false, true)) delegate.onDisconnect()
        }

        override fun onAuthError() {
            if (isActiveSlot()) emitSlot(DestinationSlotEvent(slotIndex, HealthState.FAILED, error = "Authentication failed"))
            if (isActiveSlot() && authErrorReported.compareAndSet(false, true)) delegate.onAuthError()
        }

        override fun onAuthSuccess() {
            val shouldPublish = synchronized(trackerLock) {
                authenticatedSlots += slotIndex
                authenticatedSlots.containsAll(activeSlots) && activeSlots.isNotEmpty()
            }
            if (shouldPublish) delegate.onAuthSuccess()
        }

        private fun isActiveSlot(): Boolean = synchronized(trackerLock) { slotIndex in activeSlots }

        private fun emitSlot(event: DestinationSlotEvent) {
            runCatching { onSlotEvent(event) }
        }

        private fun isPrimarySlot(): Boolean = synchronized(trackerLock) {
            activeSlots.firstOrNull() == slotIndex
        }
    }

    private fun transportFor(endpoint: String): MultiType = when {
        endpoint.startsWith("srt://", ignoreCase = true) -> MultiType.SRT
        endpoint.startsWith("rtmp://", ignoreCase = true) || endpoint.startsWith("rtmps://", ignoreCase = true) -> MultiType.RTMP
        else -> error("Unsupported endpoint scheme")
    }

    private companion object {
        const val RTMP_SLOT = 0
        const val MAX_DESTINATIONS = 2
    }
}
