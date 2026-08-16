package com.unictoai.unictoos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MultistreamModelsTest {
    @Test
    fun providerProfilesHaveStableDestinationIdentity() {
        assertEquals(DestinationId.YOUTUBE, DestinationProfiles.youtube.id)
        assertEquals(DestinationId.TWITCH, DestinationProfiles.twitch.id)
        assertEquals(DestinationId.KICK, DestinationProfiles.kick.id)
        assertEquals(TransportProtocol.RTMPS, DestinationProfiles.youtube.supportedProtocols.single())
    }

    @Test
    fun kickProfileCarriesKnownHardLimitsWithoutChangingRuntimeBehavior() {
        val profile = DestinationProfiles.kick

        assertEquals(1_920, profile.maximumWidth)
        assertEquals(1_080, profile.maximumHeight)
        assertEquals(60, profile.maximumFps)
        assertEquals(8_000, profile.maximumVideoBitrateKbps)
        assertTrue(profile.requiresConstantBitrate)
        assertEquals(2, profile.keyframeIntervalSeconds)
    }

    @Test
    fun destinationSessionStoresOnlyOpaqueCredentialReference() {
        val session = DestinationSession(
            id = DestinationId.TWITCH,
            profile = DestinationProfiles.twitch,
            configured = true,
            credentialRef = "credential-ref-twitch",
        )

        assertEquals("credential-ref-twitch", session.credentialRef)
        assertFalse(session.credentialRef!!.contains("stream-key"))
        assertNull(session.lastError)
        assertEquals(DestinationState.DISABLED, session.state)
    }

    @Test
    fun defaultsKeepTwoDestinationCapAndExplicitInfinixGate() {
        assertEquals(2, MultistreamDefaults.DIRECT_DESTINATION_CAP)
        assertEquals("Infinix X6853", MultistreamDefaults.THREE_DESTINATION_DEVICE_GATE)
    }

    @Test
    fun aggregateSessionCanRepresentPartialLiveWithoutStartingAnything() {
        val state = MultistreamSessionState(
            aggregateState = AggregateStreamState.DEGRADED,
            destinations = listOf(
                DestinationSession(DestinationId.YOUTUBE, DestinationProfiles.youtube, DestinationState.LIVE),
                DestinationSession(DestinationId.TWITCH, DestinationProfiles.twitch, DestinationState.FAILED),
            ),
        )

        assertEquals(AggregateStreamState.DEGRADED, state.aggregateState)
        assertEquals(2, state.destinations.size)
        assertEquals(DestinationState.LIVE, state.destinations.first().state)
        assertEquals(DestinationState.FAILED, state.destinations.last().state)
    }
}
