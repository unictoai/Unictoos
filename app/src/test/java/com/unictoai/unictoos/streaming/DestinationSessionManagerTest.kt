package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.AggregateStreamState
import com.unictoai.unictoos.domain.DestinationId
import com.unictoai.unictoos.domain.DestinationSession
import com.unictoai.unictoos.domain.DestinationState
import com.unictoai.unictoos.domain.DestinationProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationSessionManagerTest {
    private fun manager(): DestinationSessionManager = DestinationSessionManager(
        listOf(
            DestinationSession(DestinationId.YOUTUBE, DestinationProfiles.youtube, configured = true),
            DestinationSession(DestinationId.TWITCH, DestinationProfiles.twitch, configured = true),
        ),
    )

    @Test
    fun startAllMovesEachConfiguredDestinationToConnecting() {
        val destinationManager = manager()

        assertEquals(listOf(DestinationId.YOUTUBE, DestinationId.TWITCH), destinationManager.startAll())
        assertTrue(destinationManager.snapshot().all { it.state == DestinationState.CONNECTING })
    }

    @Test
    fun twitchFailureDoesNotTerminateYoutube() {
        val destinationManager = manager()
        destinationManager.startAll()
        destinationManager.markLive(DestinationId.YOUTUBE)

        val failed = destinationManager.markFailure(
            DestinationId.TWITCH,
            StreamingError.AuthenticationFailed("401", DestinationId.TWITCH),
        )

        assertEquals(DestinationState.FAILED, failed.state)
        assertEquals(DestinationState.LIVE, destinationManager.snapshot().first { it.id == DestinationId.YOUTUBE }.state)
        assertEquals(AggregateStreamState.DEGRADED, destinationManager.aggregateState())
    }

    @Test
    fun transientFailureReconnectsThenBecomesFailedAtBound() {
        val destinationManager = manager()
        destinationManager.startAll()

        repeat(DestinationSessionManager.DEFAULT_MAX_RECONNECT_ATTEMPTS) {
            val result = destinationManager.markFailure(
                DestinationId.YOUTUBE,
                StreamingError.NetworkUnavailable("socket closed", DestinationId.YOUTUBE),
            )
            if (it < DestinationSessionManager.DEFAULT_MAX_RECONNECT_ATTEMPTS - 1) {
                assertEquals(DestinationState.RECONNECTING, result.state)
                destinationManager.retry(DestinationId.YOUTUBE)
            } else {
                assertEquals(DestinationState.FAILED, result.state)
            }
        }
    }

    @Test
    fun allDestinationsFailedAggregateToFailed() {
        val destinationManager = manager()
        destinationManager.startAll()
        destinationManager.markFailure(DestinationId.YOUTUBE, StreamingError.AuthenticationFailed("401", DestinationId.YOUTUBE))
        destinationManager.markFailure(DestinationId.TWITCH, StreamingError.AuthenticationFailed("401", DestinationId.TWITCH))

        assertEquals(AggregateStreamState.FAILED, destinationManager.aggregateState())
    }
}
