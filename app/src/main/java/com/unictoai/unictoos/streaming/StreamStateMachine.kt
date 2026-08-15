package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.StreamStatus

/**
 * Pure lifecycle rules for the foreground streaming service.
 *
 * The service remains the owner of the real RootEncoder session; this reducer
 * only decides whether a requested state transition is valid. Keeping the
 * rules pure makes duplicate start/stop and stale callback behavior testable
 * without requiring an Android device.
 */
object StreamStateMachine {
    fun canTransition(from: StreamStatus, to: StreamStatus): Boolean = when (from) {
        StreamStatus.IDLE -> to in setOf(StreamStatus.PREPARING, StreamStatus.STOPPED, StreamStatus.ERROR)
        StreamStatus.PREPARING -> to in setOf(StreamStatus.PREPARING, StreamStatus.IDLE, StreamStatus.CONNECTING, StreamStatus.LIVE, StreamStatus.RECONNECTING, StreamStatus.STOPPING, StreamStatus.STOPPED, StreamStatus.ERROR)
        StreamStatus.CONNECTING -> to in setOf(StreamStatus.CONNECTING, StreamStatus.LIVE, StreamStatus.RECONNECTING, StreamStatus.STOPPING, StreamStatus.STOPPED, StreamStatus.ERROR)
        StreamStatus.LIVE -> to in setOf(StreamStatus.LIVE, StreamStatus.RECONNECTING, StreamStatus.STOPPING, StreamStatus.STOPPED, StreamStatus.ERROR)
        StreamStatus.RECONNECTING -> to in setOf(StreamStatus.RECONNECTING, StreamStatus.CONNECTING, StreamStatus.LIVE, StreamStatus.STOPPING, StreamStatus.STOPPED, StreamStatus.ERROR)
        StreamStatus.STOPPING -> to in setOf(StreamStatus.STOPPING, StreamStatus.STOPPED, StreamStatus.IDLE, StreamStatus.ERROR)
        StreamStatus.STOPPED -> to in setOf(StreamStatus.STOPPED, StreamStatus.IDLE, StreamStatus.PREPARING, StreamStatus.ERROR)
        StreamStatus.ERROR -> to in setOf(StreamStatus.ERROR, StreamStatus.IDLE, StreamStatus.PREPARING, StreamStatus.STOPPED)
    }

    fun acceptsStart(status: StreamStatus): Boolean = status == StreamStatus.IDLE || status == StreamStatus.STOPPED || status == StreamStatus.ERROR

    fun acceptsStop(status: StreamStatus): Boolean = status != StreamStatus.IDLE && status != StreamStatus.STOPPED

    fun normalizeRequested(from: StreamStatus, requested: StreamStatus): StreamStatus? =
        requested.takeIf { canTransition(from, it) }
}
