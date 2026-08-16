package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.AggregateStreamState
import com.unictoai.unictoos.domain.DestinationId
import com.unictoai.unictoos.domain.DestinationSession
import com.unictoai.unictoos.domain.DestinationState

/**
 * UI-neutral destination coordinator contract.
 *
 * This class owns destination state and retry decisions only. It deliberately does not own
 * RootEncoder, MediaProjection, endpoints, or plaintext credentials. Runtime fan-out remains
 * a separate gated integration task until the single capture/encoder pipeline is physically
 * soak-tested.
 */
class DestinationSessionManager(
    initialSessions: List<DestinationSession>,
    private val maximumReconnectAttempts: Int = DEFAULT_MAX_RECONNECT_ATTEMPTS,
) {
    private val sessions = initialSessions.associateBy { it.id }.toMutableMap()

    init {
        require(maximumReconnectAttempts > 0) { "maximumReconnectAttempts must be positive" }
        require(initialSessions.map { it.id }.distinct().size == initialSessions.size) {
            "Destination IDs must be unique"
        }
    }

    fun snapshot(): List<DestinationSession> = sessions.values.toList()

    fun aggregateState(): AggregateStreamState = MultistreamStateReducer.aggregate(snapshot())

    fun startAll(): List<DestinationId> = sessions.values
        .filter { it.configured && it.state in setOf(DestinationState.DISABLED, DestinationState.STOPPED, DestinationState.FAILED) }
        .map { session ->
            update(session.copy(state = DestinationState.CONFIGURING, lastError = null))
            update(session.copy(state = DestinationState.CONNECTING, lastError = null))
            session.id
        }

    fun markLive(destinationId: DestinationId): DestinationSession = transition(destinationId) { session ->
        require(DestinationStateMachine.canTransition(session.state, DestinationState.LIVE)) {
            "Cannot mark ${session.id} live from ${session.state}"
        }
        session.copy(state = DestinationState.LIVE, lastError = null)
    }

    fun markFailure(destinationId: DestinationId, error: StreamingError): DestinationSession = transition(destinationId) { session ->
        val nextAttempt = session.reconnectAttempt + 1
        val retryable = error.retryable && nextAttempt < maximumReconnectAttempts
        session.copy(
            state = if (retryable) DestinationState.RECONNECTING else DestinationState.FAILED,
            reconnectAttempt = nextAttempt,
            lastError = error.userMessage,
        )
    }

    fun retry(destinationId: DestinationId): DestinationSession = transition(destinationId) { session ->
        require(DestinationStateMachine.acceptsRetry(session.state)) {
            "Destination ${session.id} is not retryable from ${session.state}"
        }
        session.copy(state = DestinationState.CONNECTING, lastError = null)
    }

    fun stop(destinationId: DestinationId): DestinationSession = transition(destinationId) { session ->
        if (session.state == DestinationState.DISABLED || session.state == DestinationState.STOPPED) {
            session
        } else {
            session.copy(state = DestinationState.STOPPED)
        }
    }

    fun stopAll(): List<DestinationSession> = sessions.keys.map(::stop)

    private fun transition(
        destinationId: DestinationId,
        block: (DestinationSession) -> DestinationSession,
    ): DestinationSession {
        val current = sessions[destinationId] ?: error("Unknown destination: $destinationId")
        val updated = block(current)
        update(updated)
        return updated
    }

    private fun update(session: DestinationSession) {
        sessions[session.id] = session
    }

    companion object {
        const val DEFAULT_MAX_RECONNECT_ATTEMPTS = 3
    }
}
