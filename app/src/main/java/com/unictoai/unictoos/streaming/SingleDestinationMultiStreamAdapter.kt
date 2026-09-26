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
import com.pedro.encoder.input.sources.audio.NoAudioSource
import com.pedro.encoder.input.sources.video.NoVideoSource
import com.unictoai.unictoos.health.DestinationSlotEvent
import com.unictoai.unictoos.health.HealthState
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Multi-destination transport owner backed by RootEncoder MultiStream.
 *
 * Lifecycle policy:
 * - Stopping is deterministic. RootEncoder's per-slot stop is a heuristic that
 *   races the slot clients' asynchronous disconnect, so after disconnecting the
 *   slots the adapter forces the unconditional shared-encoder teardown. Without
 *   this, a failed first attempt leaves a stale streaming flag and every later
 *   start silently no-ops or throws "Stream already started".
 * - The session is LIVE when the first destination connects (see
 *   [SlotAggregatePolicy]). A slow or broken secondary destination never
 *   blocks the primary, and its failures are retried at slot level instead of
 *   tearing down the healthy output.
 */
class SingleDestinationMultiStreamAdapter(
    context: Context,
    connectChecker: ConnectChecker,
    private val onSlotEvent: (DestinationSlotEvent) -> Unit = {},
) {
    private val closed = AtomicBoolean(false)
    private val trackerLock = Any()
    private val slotEndpoints = mutableMapOf<Int, String>()
    private val slotPolicy = SlotAggregatePolicy()
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

    fun prepareVideo(width: Int, height: Int, bitrate: Int, fps: Int = 30, rotation: Int = 0): Boolean {
        checkOpen()
        return multiStream.prepareVideo(width, height, bitrate, fps, rotation = rotation)
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

    /**
     * Returns the client for the first active endpoint's transport, so cache
     * tuning (for example low-latency sizing) targets the session that is
     * actually running instead of always hitting RTMP slot zero.
     */
    fun getStreamClient(): StreamBaseClient {
        checkOpen()
        val (type, index) = synchronized(trackerLock) {
            val entry = slotEndpoints.entries.firstOrNull()
            if (entry != null) transportFor(entry.value) to entry.key else MultiType.RTMP to RTMP_SLOT
        }
        return multiStream.getStreamClient(type, index)
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

    fun startStream(endpoint: String, lowLatencyCache: Boolean = false) =
        startStream(listOf(endpoint), lowLatencyCache)

    /**
     * @param lowLatencyCache when true, disables the client send cache on every
     * active slot's client (RootEncoder exposes client-cache sizing, but no
     * public keyframe-interval override in this version). Applied here instead
     * of at pipeline creation because only now are the endpoint transports known.
     */
    fun startStream(endpoints: List<String>, lowLatencyCache: Boolean = false) {
        checkOpen()
        require(endpoints.isNotEmpty()) { "At least one endpoint is required" }
        require(endpoints.size <= MAX_DESTINATIONS) { "At most $MAX_DESTINATIONS endpoints are supported" }
        val normalizedEndpoints = endpoints.map(String::trim)
        require(normalizedEndpoints.all(StreamEndpointPolicy::isSupported)) { "Every endpoint must be a complete RTMP, RTMPS, or SRT URL" }
        if (isStreaming || synchronized(trackerLock) { slotEndpoints.isNotEmpty() }) stopStream()
        synchronized(trackerLock) {
            slotEndpoints.clear()
            normalizedEndpoints.forEachIndexed { index, endpoint -> slotEndpoints[index] = endpoint }
        }
        slotPolicy.reset(normalizedEndpoints.indices.toList())
        normalizedEndpoints.forEachIndexed { index, _ ->
            emitSlot(DestinationSlotEvent(index, HealthState.RECONNECTING))
        }
        normalizedEndpoints.forEachIndexed { index, endpoint ->
            val type = transportFor(endpoint)
            // Bounded per-slot retries so a single bad destination can recover
            // without a full session restart.
            runCatching { multiStream.getStreamClient(type, index).setReTries(SLOT_RETRIES) }
            if (lowLatencyCache) runCatching { multiStream.getStreamClient(type, index).resizeCache(0) }
            multiStream.startStream(type, index, endpoint)
        }
    }

    fun stopStream() {
        if (closed.get()) return
        synchronized(trackerLock) { slotEndpoints.clear() }
        slotPolicy.clear()
        repeat(MAX_DESTINATIONS) { index ->
            runCatching { multiStream.stopStream(MultiType.RTMP, index) }
            runCatching { multiStream.stopStream(MultiType.SRT, index) }
        }
        // Deterministic shared-encoder teardown. The per-slot stop above is a
        // heuristic that races the slot clients' asynchronous disconnect: when
        // every slot client still reports streaming, the shared encoder is left
        // running and its isStreaming flag stays set, which poisons the next
        // startStream (silent no-op or "Stream already started"). The no-arg
        // path unconditionally resets the flag, stops the encoders and
        // re-prepares them, so the next attempt always starts clean. It is
        // skipped while recording so an active recording keeps its encoders.
        if (multiStream.isStreaming && !multiStream.isRecording) runCatching { multiStream.stopStream() }
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
        synchronized(trackerLock) { slotEndpoints.clear() }
        slotPolicy.clear()
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

    /**
     * Retries one failed slot through RootEncoder's built-in slot retry, which
     * disconnects and reconnects that client with the retry flag set. The
     * shared encoder keeps running because the healthy peer still holds it, so
     * the live output is never interrupted.
     */
    private fun retryFailedSlot(slotIndex: Int, reason: String) {
        val type = synchronized(trackerLock) {
            val endpoint = slotEndpoints[slotIndex] ?: return
            transportFor(endpoint)
        }
        emitSlot(DestinationSlotEvent(slotIndex, HealthState.RECONNECTING))
        val retried = runCatching {
            multiStream.getStreamClient(type, slotIndex).reTry(SLOT_RETRY_DELAY_MS, reason)
        }.getOrDefault(false)
        if (!retried) {
            emitSlot(DestinationSlotEvent(slotIndex, HealthState.FAILED, error = reason.take(240)))
        }
    }

    private inner class SlotConnectChecker(
        private val slotIndex: Int,
        private val delegate: ConnectChecker,
    ) : ConnectChecker {
        override fun onConnectionStarted(url: String) {
            if (!slotPolicy.isActive(slotIndex)) return
            slotPolicy.onStarted(slotIndex)
            emitSlot(DestinationSlotEvent(slotIndex, HealthState.RECONNECTING))
            if (slotPolicy.telemetrySlot() == slotIndex) delegate.onConnectionStarted(url)
        }

        override fun onConnectionSuccess() {
            if (!slotPolicy.isActive(slotIndex)) return
            emitSlot(DestinationSlotEvent(slotIndex, HealthState.HEALTHY))
            val result = slotPolicy.onSuccess(slotIndex)
            if (result.publishAggregate) delegate.onConnectionSuccess()
            // A peer that failed while this slot was still attempting gets its
            // slot-level retry now that the session is live.
            result.retrySlots.forEach { retryFailedSlot(it, "Retrying after peer connected") }
        }

        override fun onNewBitrate(bitrate: Long) {
            if (!slotPolicy.isActive(slotIndex)) return
            emitSlot(DestinationSlotEvent(slotIndex, HealthState.HEALTHY, bitrate = bitrate))
            // The service's adaptive target is per encoded output. Use the first
            // connected slot as the authoritative signal so a second destination
            // cannot double the bitrate.
            if (slotPolicy.telemetrySlot() == slotIndex) delegate.onNewBitrate(bitrate)
        }

        override fun onConnectionFailed(reason: String) {
            if (!slotPolicy.isActive(slotIndex)) return
            emitSlot(DestinationSlotEvent(slotIndex, HealthState.FAILED, error = reason.take(240)))
            val result = slotPolicy.onFailed(slotIndex)
            if (result.retrySelf) {
                // Another destination is live: retry only this slot instead of
                // tearing down the healthy output for a failure it didn't cause.
                retryFailedSlot(slotIndex, reason.ifBlank { "connection failed" })
            } else if (result.publishAggregate) {
                delegate.onConnectionFailed("Destination ${slotIndex + 1}: ${reason.ifBlank { "connection failed" }}")
            }
            // Otherwise a peer is still attempting; its outcome decides.
        }

        override fun onDisconnect() {
            if (!slotPolicy.isActive(slotIndex)) return
            emitSlot(DestinationSlotEvent(slotIndex, HealthState.RECONNECTING))
            val result = slotPolicy.onDisconnected(slotIndex)
            if (result.retrySelf) {
                retryFailedSlot(slotIndex, "Connection lost")
            } else if (result.publishAggregate) {
                // No live output remains; the service owns the full session
                // retry policy (backoff, attempt budget, watchdog).
                delegate.onDisconnect()
            }
        }

        override fun onAuthError() {
            if (!slotPolicy.isActive(slotIndex)) return
            emitSlot(DestinationSlotEvent(slotIndex, HealthState.FAILED, error = "Authentication failed"))
            // A bad key on one destination must not kill a healthy peer, and a
            // retry cannot fix authentication, so the slot simply stays failed.
            // The aggregate is fatal only when every destination rejected its
            // key; a mixed auth/network outcome stays retryable.
            when (slotPolicy.onAuthError(slotIndex)) {
                SlotAggregatePolicy.AuthOutcome.PUBLISH_AUTH_ERROR -> delegate.onAuthError()
                SlotAggregatePolicy.AuthOutcome.PUBLISH_FAILURE ->
                    delegate.onConnectionFailed("Destination ${slotIndex + 1}: authentication failed")
                SlotAggregatePolicy.AuthOutcome.NOTHING -> Unit
            }
        }

        override fun onAuthSuccess() {
            if (!slotPolicy.isActive(slotIndex)) return
            delegate.onAuthSuccess()
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
        const val SLOT_RETRIES = 3
        const val SLOT_RETRY_DELAY_MS = 3_000L
    }
}
