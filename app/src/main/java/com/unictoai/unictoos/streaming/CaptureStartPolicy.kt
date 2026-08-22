package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.StreamStatus

/**
 * Pure rules for the handoff between a user start request and capture
 * preparation. The foreground service remains the owner of the actual
 * encoder; this policy only makes the handoff deterministic and testable.
 */
object CaptureStartPolicy {
    data class Request(
        val endpoints: List<String>,
        val sceneJson: String,
        val practice: Boolean,
    ) {
        fun hasRequiredDestination(): Boolean = practice || endpoints.isNotEmpty()
    }

    enum class Decision {
        NO_REQUEST,
        WAIT_FOR_CAPTURE,
        START_NOW,
        STALE_REQUEST,
    }

    data class State(
        val request: Request? = null,
        val prepared: Boolean = false,
        val captureReady: Boolean = false,
        val requestGeneration: Long = 0L,
    )

    fun canQueue(
        status: StreamStatus,
        hasPendingRequest: Boolean,
        replacePending: Boolean,
    ): Boolean = StreamStateMachine.acceptsQueuedStart(status) && (!hasPendingRequest || replacePending)

    fun decide(state: State, currentGeneration: Long): Decision {
        val request = state.request ?: return Decision.NO_REQUEST
        if (state.requestGeneration > 0L && state.requestGeneration != currentGeneration) return Decision.STALE_REQUEST
        return if (state.prepared && state.captureReady) Decision.START_NOW else Decision.WAIT_FOR_CAPTURE
    }

    fun consume(state: State): State = state.copy(request = null)

    fun clearAfterFailure(state: State): State = state.copy(request = null, prepared = false, captureReady = false)
}
