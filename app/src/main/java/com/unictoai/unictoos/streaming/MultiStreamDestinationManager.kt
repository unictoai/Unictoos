package com.unictoai.unictoos.streaming

import android.content.Context
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.sources.audio.AudioSource
import com.pedro.encoder.input.sources.audio.NoAudioSource
import com.pedro.encoder.input.sources.video.NoVideoSource
import com.pedro.encoder.input.sources.video.VideoSource
import com.pedro.library.multiple.MultiStream
import com.pedro.library.multiple.MultiType
import com.pedro.library.util.FpsListener
import com.pedro.library.util.streamclient.StreamBaseClient
import com.pedro.library.view.GlInterface
import com.pedro.library.view.RenderErrorCallback
import com.unictoai.unictoos.domain.DestinationId
import com.unictoai.unictoos.domain.MultistreamDefaults
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Bounded destination slots for the shared encoded-media fan-out path.
 *
 * The table deliberately maps each selected destination to a stable RootEncoder
 * MultiStream index. It never stores an endpoint, stream key, or credential.
 */
class MultiStreamDestinationSlots(
    destinations: List<DestinationId>,
    private val maximumSlots: Int = MultistreamDefaults.DIRECT_DESTINATION_CAP,
) {
    val destinations: List<DestinationId> = destinations.distinct().also { selected ->
        require(selected.isNotEmpty()) { "At least one destination is required" }
        require(selected.size <= maximumSlots) {
            "Selected destinations exceed the direct device cap of $maximumSlots"
        }
        require(selected.size <= ROOT_ENCODER_MAX_DESTINATION_SLOTS) {
            "Selected destinations exceed RootEncoder MultiStream capacity"
        }
    }

    private val indexes = this.destinations.withIndex().associate { (index, id) -> id to index }

    fun indexOf(destinationId: DestinationId): Int =
        indexes[destinationId] ?: error("Destination is not selected: $destinationId")

    fun contains(destinationId: DestinationId): Boolean = indexes.containsKey(destinationId)

    companion object {
        const val ROOT_ENCODER_MAX_DESTINATION_SLOTS = 4
    }
}

/**
 * Thin, UI-neutral owner for RootEncoder's shared-encoder MultiStream path.
 *
 * This class is intentionally not wired into StreamingForegroundService in this
 * increment. That keeps the proven single-destination path unchanged while the
 * adapter and its slot contracts receive deterministic tests. The next runtime
 * integration must add per-destination generations, retry state, and diagnostics.
 */
class MultiStreamDestinationManager(
    context: Context,
    selectedDestinations: List<DestinationId>,
    callbacks: Map<DestinationId, ConnectChecker>,
    maximumSlots: Int = MultistreamDefaults.DIRECT_DESTINATION_CAP,
) : AutoCloseable {
    private val slots = MultiStreamDestinationSlots(selectedDestinations, maximumSlots)
    private val closed = AtomicBoolean(false)
    private val activeDestinations = linkedSetOf<DestinationId>()
    private val multiStream: MultiStream

    init {
        val rtmpCallbacks = slots.destinations.map { destinationId ->
            callbacks[destinationId] ?: error("Missing ConnectChecker for $destinationId")
        }.toTypedArray()
        multiStream = MultiStream(
            context,
            rtmpCallbacks,
            emptyArray(),
            emptyArray(),
            emptyArray(),
            NoVideoSource(),
            NoAudioSource(),
        )
    }

    val destinations: List<DestinationId>
        get() = slots.destinations

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

    fun setFpsListener(callback: FpsListener.Callback) {
        checkOpen()
        multiStream.setFpsListener(callback)
    }

    fun setRenderErrorCallback(callback: RenderErrorCallback) {
        checkOpen()
        multiStream.getGlInterface().setRenderErrorCallback(callback)
    }

    fun glInterface(): GlInterface {
        checkOpen()
        return multiStream.getGlInterface()
    }

    fun streamClient(destinationId: DestinationId): StreamBaseClient {
        checkOpen()
        return multiStream.getStreamClient(MultiType.RTMP, slots.indexOf(destinationId))
    }

    fun start(destinationId: DestinationId, endpoint: String) {
        checkOpen()
        require(endpoint.isNotBlank()) { "Endpoint cannot be blank" }
        multiStream.startStream(MultiType.RTMP, slots.indexOf(destinationId), endpoint)
        activeDestinations += destinationId
    }

    fun stop(destinationId: DestinationId) {
        if (closed.get() || !slots.contains(destinationId)) return
        multiStream.stopStream(MultiType.RTMP, slots.indexOf(destinationId))
        activeDestinations -= destinationId
    }

    fun isActive(destinationId: DestinationId): Boolean =
        !closed.get() && activeDestinations.contains(destinationId)

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        activeDestinations.toList().forEach { destinationId ->
            runCatching { multiStream.stopStream(MultiType.RTMP, slots.indexOf(destinationId)) }
        }
        activeDestinations.clear()
        runCatching { multiStream.getGlInterface().stop() }
        runCatching { multiStream.release() }
    }

    private fun checkOpen() {
        check(!closed.get()) { "MultiStream destination manager is closed" }
    }
}
