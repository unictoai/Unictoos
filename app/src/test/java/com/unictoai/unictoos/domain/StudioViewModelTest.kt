package com.unictoai.unictoos.domain

import com.unictoai.unictoos.DestinationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioDomainTest {
    @Test
    fun destinationRequiresKeyForRtmpButNotForCompleteSrtListener() {
        assertFalse(DestinationConfig().isConfigured)
        assertFalse(DestinationConfig(serverUrl = "rtmps://example.com/app").isConfigured)
        assertTrue(
            DestinationConfig(
                serverUrl = "rtmps://example.com/app",
                streamKey = "secret",
            ).isConfigured,
        )
        val srt = DestinationConfig(serverUrl = "srt://listener.example:9000?streamid=unictoos-live")
        assertTrue(srt.isConfigured)
        assertEquals("srt://listener.example:9000?streamid=unictoos-live", srt.endpoint)
    }

    @Test
    fun malformedSrtDestinationIsNotConfigured() {
        assertFalse(DestinationConfig(serverUrl = "srt://").isConfigured)
        assertFalse(DestinationConfig(serverUrl = "srt://?streamid=missing-host").isConfigured)
    }

    @Test
    fun destinationBuildsEndpointWithoutDuplicateSlash() {
        val config = DestinationConfig(
            serverUrl = "rtmps://example.com/app/",
            streamKey = "stream-key",
        )
        assertEquals("rtmps://example.com/app/stream-key", config.endpoint)
    }

    @Test
    fun sceneDefaultsToEmptySourceList() {
        val scene = Scene(id = "test", name = "Test")
        assertEquals(0, scene.sources.size)
        assertEquals(AspectRatio.PORTRAIT, scene.aspectRatio)
    }

    @Test
    fun platformPresetsExposeSafeServerHints() {
        assertTrue(PlatformPreset.YOUTUBE.serverHint.startsWith("rtmps://"))
        assertTrue(PlatformPreset.TWITCH.serverHint.startsWith("rtmps://"))
        assertTrue(PlatformPreset.KICK.serverHint.contains("dashboard"))
    }

    @Test
    fun streamSessionDefaultsToSafeIdleState() {
        val session = StreamSessionState()
        assertEquals(StreamStatus.IDLE, session.status)
        assertFalse(session.microphoneMuted)
        assertFalse(session.recording)
        assertEquals(0, session.reconnectAttempt)
    }
}
