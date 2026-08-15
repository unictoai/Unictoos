package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.DestinationId
import com.unictoai.unictoos.domain.MultistreamDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiStreamDestinationManagerTest {
    @Test
    fun selectedDestinationsReceiveStableRootEncoderIndexes() {
        val slots = MultiStreamDestinationSlots(
            listOf(DestinationId.YOUTUBE, DestinationId.TWITCH),
        )

        assertEquals(listOf(DestinationId.YOUTUBE, DestinationId.TWITCH), slots.destinations)
        assertEquals(0, slots.indexOf(DestinationId.YOUTUBE))
        assertEquals(1, slots.indexOf(DestinationId.TWITCH))
        assertTrue(slots.contains(DestinationId.YOUTUBE))
        assertFalse(slots.contains(DestinationId.KICK))
    }

    @Test
    fun duplicateDestinationsDoNotConsumeAdditionalSlots() {
        val slots = MultiStreamDestinationSlots(
            listOf(DestinationId.TWITCH, DestinationId.TWITCH),
        )

        assertEquals(listOf(DestinationId.TWITCH), slots.destinations)
        assertEquals(0, slots.indexOf(DestinationId.TWITCH))
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptySelectionIsRejected() {
        MultiStreamDestinationSlots(emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun defaultDeviceCapRejectsThreeDestinations() {
        MultiStreamDestinationSlots(
            listOf(DestinationId.YOUTUBE, DestinationId.TWITCH, DestinationId.KICK),
        )
    }

    @Test
    fun explicitHigherCapStillNeverExceedsRootEncoderCapacity() {
        val slots = MultiStreamDestinationSlots(
            listOf(DestinationId.YOUTUBE, DestinationId.TWITCH, DestinationId.KICK),
            maximumSlots = 3,
        )

        assertEquals(3, slots.destinations.size)
        assertTrue(MultiStreamDestinationSlots.ROOT_ENCODER_MAX_DESTINATION_SLOTS <= 4)
        assertEquals(2, MultistreamDefaults.DIRECT_DESTINATION_CAP)
    }
}
