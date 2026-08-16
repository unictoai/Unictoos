package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.AggregateStreamState
import com.unictoai.unictoos.domain.DestinationId
import com.unictoai.unictoos.domain.DestinationSession
import com.unictoai.unictoos.domain.DestinationState
import com.unictoai.unictoos.domain.DestinationProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingStateContractsTest {
    @Test
    fun healthyDestinationsAggregateToLive() {
        val sessions = listOf(
            DestinationSession(DestinationId.YOUTUBE, DestinationProfiles.youtube, DestinationState.LIVE),
            DestinationSession(DestinationId.TWITCH, DestinationProfiles.twitch, DestinationState.LIVE),
        )

        assertEquals(AggregateStreamState.LIVE, MultistreamStateReducer.aggregate(sessions))
    }

    @Test
    fun oneFailedDestinationLeavesAggregateDegradedWhenAnotherIsLive() {
        val sessions = listOf(
            DestinationSession(DestinationId.YOUTUBE, DestinationProfiles.youtube, DestinationState.LIVE),
            DestinationSession(DestinationId.TWITCH, DestinationProfiles.twitch, DestinationState.FAILED),
        )

        assertEquals(AggregateStreamState.DEGRADED, MultistreamStateReducer.aggregate(sessions))
    }

    @Test
    fun allFailedDestinationsAggregateToFailed() {
        val sessions = listOf(
            DestinationSession(DestinationId.YOUTUBE, DestinationProfiles.youtube, DestinationState.FAILED),
            DestinationSession(DestinationId.TWITCH, DestinationProfiles.twitch, DestinationState.FAILED),
        )

        assertEquals(AggregateStreamState.FAILED, MultistreamStateReducer.aggregate(sessions))
    }

    @Test
    fun reconnectingDestinationDoesNotForceHealthyDestinationToStop() {
        assertTrue(DestinationStateMachine.canTransition(DestinationState.LIVE, DestinationState.RECONNECTING))
        assertTrue(DestinationStateMachine.canTransition(DestinationState.RECONNECTING, DestinationState.CONNECTING))
        assertTrue(DestinationStateMachine.acceptsRetry(DestinationState.FAILED))
        assertTrue(DestinationStateMachine.acceptsRetry(DestinationState.RECONNECTING))
        assertFalse(DestinationStateMachine.acceptsRetry(DestinationState.LIVE))
    }

    @Test
    fun recordingLifecycleRejectsStartTwiceAndAllowsIdempotentStopIntent() {
        assertTrue(RecordingStateMachine.canTransition(RecordingState.IDLE, RecordingState.STARTING))
        assertTrue(RecordingStateMachine.canTransition(RecordingState.STARTING, RecordingState.RECORDING))
        assertTrue(RecordingStateMachine.canTransition(RecordingState.RECORDING, RecordingState.STOPPING))
        assertTrue(RecordingStateMachine.canTransition(RecordingState.STOPPING, RecordingState.IDLE))
        assertFalse(RecordingStateMachine.canTransition(RecordingState.RECORDING, RecordingState.STARTING))
        assertFalse(RecordingStateMachine.canTransition(RecordingState.IDLE, RecordingState.STOPPING))
    }

    @Test
    fun structuredErrorsExposeSafeMessageWithoutCredentialData() {
        val error = StreamingError.AuthenticationFailed(
            developerMessage = "provider returned 401 for destination=${DestinationId.TWITCH}",
            destinationId = DestinationId.TWITCH,
        )

        assertFalse(error.retryable)
        assertTrue(error.userMessage.contains("credentials"))
        assertFalse(error.userMessage.contains("stream-key"))
        assertEquals(DestinationId.TWITCH, error.destinationId)
    }
}
