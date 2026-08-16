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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Single-destination compatibility surface for the first runtime migration.
 *
 * It preserves the service's existing lifecycle vocabulary while replacing the
 * GenericStream transport owner with RootEncoder MultiStream at slot zero. This
 * deliberately enables only one RTMP destination; independent destination
 * sessions are a later step after the Infinix X6853 pipeline is stable.
 */
class SingleDestinationMultiStreamAdapter(
    context: Context,
    connectChecker: ConnectChecker,
) {
    private val closed = AtomicBoolean(false)
    private val multiStream = MultiStream(
        context,
        arrayOf(connectChecker),
        emptyArray(),
        emptyArray(),
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

    fun startStream(endpoint: String) {
        checkOpen()
        require(endpoint.isNotBlank()) { "Endpoint cannot be blank" }
        multiStream.startStream(MultiType.RTMP, RTMP_SLOT, endpoint)
    }

    fun stopStream() {
        if (closed.get()) return
        multiStream.stopStream(MultiType.RTMP, RTMP_SLOT)
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
        var firstFailure: Throwable? = null
        fun attempt(block: () -> Unit) {
            runCatching(block).onFailure { if (firstFailure == null) firstFailure = it }
        }
        attempt { multiStream.stopStream(MultiType.RTMP, RTMP_SLOT) }
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

    private fun checkOpen() {
        check(!closed.get()) { "MultiStream adapter is closed" }
    }

    private companion object {
        const val RTMP_SLOT = 0
    }
}
