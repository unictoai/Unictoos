package com.unictoai.unictoos.streaming

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import com.unictoai.unictoos.domain.DestinationId
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingDiagnosticsTest {
    @Test
    fun redactsEndpointAndSecretLikeDetails() {
        StreamingDiagnostics.clear()
        StreamingDiagnostics.record("session-1", 4L, "failure", "rtmps://example.test/live secret=abc token=xyz")

        val detail = StreamingDiagnostics.snapshot().single().detail
        assertFalse(detail.contains("rtmps://example.test"))
        assertFalse(detail.contains("abc"))
        assertFalse(detail.contains("xyz"))
        assertTrue(detail.contains("REDACTED") || detail.contains("ENDPOINT_REDACTED"))
    }

    @Test
    fun redactsJsonCredentialsAndBearerAuthorization() {
        StreamingDiagnostics.clear()
        StreamingDiagnostics.record(
            "session-json",
            5L,
            "failure",
            "{\"streamKey\":\"secret-json\",\"token\":\"secret-token\"} Authorization: Bearer abc.def.ghi",
        )

        val detail = StreamingDiagnostics.snapshot().single().detail
        assertFalse(detail.contains("secret-json"))
        assertFalse(detail.contains("secret-token"))
        assertFalse(detail.contains("abc.def.ghi"))
        assertTrue(detail.contains("[REDACTED]"))
    }

    @Test
    fun storesOptionalDestinationAndNetworkMetadata() {
        StreamingDiagnostics.clear()
        StreamingDiagnostics.record(
            sessionId = "session-2",
            generation = 8L,
            event = "connection_failed",
            detail = "socket disconnected",
            destinationId = DestinationId.TWITCH,
            networkEpoch = 3L,
            elapsedRealtimeMs = 42L,
        )

        val diagnostic = StreamingDiagnostics.snapshot().single()
        assertEquals(DestinationId.TWITCH, diagnostic.destinationId)
        assertEquals(3L, diagnostic.networkEpoch)
        assertEquals(42L, diagnostic.elapsedRealtimeMs)
    }

    @Test
    fun keepsOnlyTheMostRecentBoundedEvents() {
        StreamingDiagnostics.clear()
        repeat(220) { index -> StreamingDiagnostics.record("s", index.toLong(), "event", index.toString()) }

        val snapshot = StreamingDiagnostics.snapshot()
        assertEquals(200, snapshot.size)
        assertEquals(20L, snapshot.first().generation)
        assertEquals(219L, snapshot.last().generation)
    }
}
