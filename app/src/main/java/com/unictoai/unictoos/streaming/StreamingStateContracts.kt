package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.AggregateStreamState
import com.unictoai.unictoos.domain.DestinationId
import com.unictoai.unictoos.domain.DestinationSession
import com.unictoai.unictoos.domain.DestinationState
import com.unictoai.unictoos.domain.RecordingState

/** Safe, structured failures exposed to UI and diagnostics instead of raw exceptions. */
sealed interface StreamingError {
    val userMessage: String
    val developerMessage: String
    val retryable: Boolean
    val destinationId: DestinationId?

    data class PermissionDenied(
        override val developerMessage: String,
        override val destinationId: DestinationId? = null,
    ) : StreamingError {
        override val userMessage: String = "Allow the required Android permission before starting capture"
        override val retryable: Boolean = true
    }

    data class ProjectionUnavailable(
        override val developerMessage: String,
    ) : StreamingError {
        override val userMessage: String = "Screen capture permission is no longer available"
        override val retryable: Boolean = true
        override val destinationId: DestinationId? = null
    }

    data class EncoderUnavailable(
        override val developerMessage: String,
    ) : StreamingError {
        override val userMessage: String = "This device cannot start the selected capture profile"
        override val retryable: Boolean = true
        override val destinationId: DestinationId? = null
    }

    data class UnsupportedConfiguration(
        override val developerMessage: String,
        override val destinationId: DestinationId? = null,
    ) : StreamingError {
        override val userMessage: String = "The selected stream profile is not supported by this device or destination"
        override val retryable: Boolean = false
    }

    data class NetworkUnavailable(
        override val developerMessage: String,
        override val destinationId: DestinationId? = null,
    ) : StreamingError {
        override val userMessage: String = "The network connection was lost"
        override val retryable: Boolean = true
    }

    data class AuthenticationFailed(
        override val developerMessage: String,
        override val destinationId: DestinationId,
    ) : StreamingError {
        override val userMessage: String = "The destination rejected the saved credentials"
        override val retryable: Boolean = false
    }

    data class DestinationRejected(
        override val developerMessage: String,
        override val destinationId: DestinationId,
    ) : StreamingError {
        override val userMessage: String = "The destination rejected this stream"
        override val retryable: Boolean = false
    }

    data class ThermalLimit(
        override val developerMessage: String,
    ) : StreamingError {
        override val userMessage: String = "Device temperature is too high to continue at the current quality"
        override val retryable: Boolean = true
        override val destinationId: DestinationId? = null
    }

    data class StorageUnavailable(
        override val developerMessage: String,
    ) : StreamingError {
        override val userMessage: String = "There is not enough device storage for this recording"
        override val retryable: Boolean = false
        override val destinationId: DestinationId? = null
    }

    data class Unknown(
        override val developerMessage: String,
        override val destinationId: DestinationId? = null,
    ) : StreamingError {
        override val userMessage: String = "The streaming session could not continue"
        override val retryable: Boolean = false
    }
}

object DestinationStateMachine {
    fun canTransition(from: DestinationState, to: DestinationState): Boolean = when (from) {
        DestinationState.DISABLED -> to == DestinationState.CONFIGURING || to == DestinationState.STOPPED
        DestinationState.CONFIGURING -> to in setOf(DestinationState.CONNECTING, DestinationState.DISABLED, DestinationState.STOPPING, DestinationState.FAILED)
        DestinationState.CONNECTING -> to in setOf(DestinationState.LIVE, DestinationState.RECONNECTING, DestinationState.FAILED, DestinationState.STOPPING)
        DestinationState.LIVE -> to in setOf(DestinationState.RECONNECTING, DestinationState.FAILED, DestinationState.STOPPING)
        DestinationState.RECONNECTING -> to in setOf(DestinationState.CONNECTING, DestinationState.LIVE, DestinationState.FAILED, DestinationState.STOPPING)
        DestinationState.FAILED -> to in setOf(DestinationState.CONFIGURING, DestinationState.RECONNECTING, DestinationState.STOPPING, DestinationState.STOPPED)
        DestinationState.STOPPING -> to == DestinationState.STOPPED
        DestinationState.STOPPED -> to in setOf(DestinationState.CONFIGURING, DestinationState.DISABLED)
    }

    fun acceptsRetry(state: DestinationState): Boolean =
        state == DestinationState.FAILED || state == DestinationState.RECONNECTING
}

object RecordingStateMachine {
    fun canTransition(from: RecordingState, to: RecordingState): Boolean = when (from) {
        RecordingState.IDLE -> to == RecordingState.STARTING
        RecordingState.STARTING -> to in setOf(RecordingState.RECORDING, RecordingState.FAILED, RecordingState.STOPPING)
        RecordingState.RECORDING -> to in setOf(RecordingState.STOPPING, RecordingState.FAILED)
        RecordingState.STOPPING -> to in setOf(RecordingState.IDLE, RecordingState.FAILED)
        RecordingState.FAILED -> to in setOf(RecordingState.IDLE, RecordingState.STARTING)
    }
}

object MultistreamStateReducer {
    fun aggregate(sessions: List<DestinationSession>): AggregateStreamState {
        val enabled = sessions.filter { it.state != DestinationState.DISABLED }
        if (enabled.isEmpty()) return AggregateStreamState.IDLE
        if (enabled.any { it.state == DestinationState.STOPPING }) return AggregateStreamState.STOPPING
        if (enabled.all { it.state == DestinationState.STOPPED }) return AggregateStreamState.STOPPED

        val liveCount = enabled.count { it.state == DestinationState.LIVE }
        val transitional = enabled.any {
            it.state == DestinationState.CONFIGURING ||
                it.state == DestinationState.CONNECTING ||
                it.state == DestinationState.RECONNECTING
        }
        val failed = enabled.any { it.state == DestinationState.FAILED }

        return when {
            liveCount == enabled.size -> AggregateStreamState.LIVE
            liveCount > 0 -> AggregateStreamState.DEGRADED
            transitional -> AggregateStreamState.STARTING
            failed -> AggregateStreamState.FAILED
            else -> AggregateStreamState.PREPARING
        }
    }
}
