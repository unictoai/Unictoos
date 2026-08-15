package com.unictoai.unictoos.streaming

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun keepsOnlyTheMostRecentBoundedEvents() {
        StreamingDiagnostics.clear()
        repeat(220) { index -> StreamingDiagnostics.record("s", index.toLong(), "event", index.toString()) }

        val snapshot = StreamingDiagnostics.snapshot()
        assertEquals(200, snapshot.size)
        assertEquals(20L, snapshot.first().generation)
        assertEquals(219L, snapshot.last().generation)
    }
}
