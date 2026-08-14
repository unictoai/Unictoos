package com.unictoai.unictoos.domain

import com.unictoai.unictoos.DestinationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioDomainTest {
    @Test
    fun destinationRequiresBothServerAndKey() {
        assertFalse(DestinationConfig().isConfigured)
        assertFalse(DestinationConfig(serverUrl = "rtmps://example.com/app").isConfigured)
        assertTrue(
            DestinationConfig(
                serverUrl = "rtmps://example.com/app",
                streamKey = "secret",
            ).isConfigured,
        )
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
}
