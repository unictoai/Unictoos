package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.StreamStatus

/**
 * Background audio is intentionally narrow: it applies only to a live,
 * camera-only capture. A MediaProjection session must never be muted by this
 * policy because screen capture is not the requested background mode.
 */
object BackgroundAudioPolicy {
    fun canEnter(
        enabled: Boolean,
        screenCaptureActive: Boolean,
        captureReady: Boolean,
        status: StreamStatus,
    ): Boolean = enabled && !screenCaptureActive && captureReady && status == StreamStatus.LIVE
}
