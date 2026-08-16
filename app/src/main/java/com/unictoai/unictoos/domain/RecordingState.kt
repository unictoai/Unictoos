package com.unictoai.unictoos.domain

/** Recording lifecycle is independent from streaming and local preview attachment. */
enum class RecordingState {
    IDLE,
    STARTING,
    RECORDING,
    STOPPING,
    FAILED,
}

/**
 * Recording requires the capture and encoder pipeline, not a UI preview surface.
 * Preview may be intentionally disabled by device compatibility policy.
 */
object RecordingReadinessPolicy {
    fun isReady(captureReady: Boolean, encoderReady: Boolean): Boolean = captureReady && encoderReady
}
