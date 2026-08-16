package com.unictoai.unictoos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformCapabilitiesTest {
    @Test
    fun firstPartyPlatformsExposeStreamKeyStreamingOnly() {
        val capabilities = PlatformCapabilityCatalog.forPlatform(PlatformPreset.YOUTUBE)

        assertEquals(IntegrationReadiness.STREAM_KEY_READY, capabilities.streamingReadiness)
        assertEquals(IntegrationReadiness.REQUIRES_BACKEND, capabilities.oauthReadiness)
        assertEquals(IntegrationReadiness.REQUIRES_BACKEND, capabilities.chatReadiness)
        assertEquals(IntegrationReadiness.REQUIRES_BACKEND, capabilities.moderationReadiness)
        assertEquals(IntegrationReadiness.REQUIRES_BACKEND, capabilities.eventReadiness)
        assertFalse(capabilities.supportsStreamMarkers)
    }

    @Test
    fun customDestinationIsManualAndHasNoProviderCapabilities() {
        val capabilities = PlatformCapabilityCatalog.forPlatform(PlatformPreset.CUSTOM)

        assertEquals(IntegrationReadiness.MANUAL_CONFIGURATION, capabilities.streamingReadiness)
        assertEquals(IntegrationReadiness.REQUIRES_BACKEND, capabilities.oauthReadiness)
        assertFalse(capabilities.supportsStreamMarkers)
        assertTrue(capabilities.platform == PlatformPreset.CUSTOM)
    }
}
