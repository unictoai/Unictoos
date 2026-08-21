package com.unictoai.unictoos.streaming

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import com.unictoai.unictoos.domain.StreamStatus
import org.junit.Test

class StreamFailurePolicyTest {
    @Test
    fun authenticationAndConfigurationFailuresArePermanent() {
        assertFalse(StreamFailurePolicy.classify("authentication failed for stream key").retryable)
        assertEquals(StreamFailureKind.AUTHENTICATION, StreamFailurePolicy.classify("authentication failed for stream key").kind)
        assertFalse(StreamFailurePolicy.classify("invalid URL protocol").retryable)
        assertEquals(StreamFailureKind.CONFIGURATION, StreamFailurePolicy.classify("invalid URL protocol").kind)
    }

    @Test
    fun networkAndTimeoutFailuresAreRetryable() {
        assertTrue(StreamFailurePolicy.classify("socket disconnected").retryable)
        assertEquals(StreamFailureKind.NETWORK, StreamFailurePolicy.classify("socket disconnected").kind)
        assertTrue(StreamFailurePolicy.classify("connection timeout").retryable)
        assertEquals(StreamFailureKind.TIMEOUT, StreamFailurePolicy.classify("connection timeout").kind)
    }

    @Test
    fun silentConnectingStartTimesOutOnlyForCurrentGeneration() {
        assertTrue(
            StreamStartupPolicy.shouldTimeout(
                status = StreamStatus.CONNECTING,
                hasEndpoint = true,
                generationMatches = true,
                elapsedMs = StreamStartupPolicy.CONNECTION_TIMEOUT_MS,
            ),
        )
        assertFalse(
            StreamStartupPolicy.shouldTimeout(
                status = StreamStatus.LIVE,
                hasEndpoint = true,
                generationMatches = true,
                elapsedMs = StreamStartupPolicy.CONNECTION_TIMEOUT_MS + 1L,
            ),
        )
        assertFalse(
            StreamStartupPolicy.shouldTimeout(
                status = StreamStatus.CONNECTING,
                hasEndpoint = true,
                generationMatches = false,
                elapsedMs = StreamStartupPolicy.CONNECTION_TIMEOUT_MS + 1L,
            ),
        )
        assertFalse(
            StreamStartupPolicy.shouldTimeout(
                status = StreamStatus.CONNECTING,
                hasEndpoint = false,
                generationMatches = true,
                elapsedMs = StreamStartupPolicy.CONNECTION_TIMEOUT_MS + 1L,
            ),
        )
    }

    @Test
    fun reconnectDelayIsCappedAndIncludesBoundedJitter() {
        assertEquals(2_000L, StreamFailurePolicy.reconnectDelayMs(1))
        assertEquals(4_000L, StreamFailurePolicy.reconnectDelayMs(2))
        assertEquals(30_000L, StreamFailurePolicy.reconnectDelayMs(8))
        assertEquals(35_000L, StreamFailurePolicy.reconnectDelayMs(8, jitterMillis = 10_000L))
    }
}
