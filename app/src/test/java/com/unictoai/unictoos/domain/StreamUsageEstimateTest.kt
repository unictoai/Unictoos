package com.unictoai.unictoos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamUsageEstimateTest {
    @Test
    fun estimatesStandard720pProfile() {
        assertEquals(2_083, estimatedDataMegabytesPerHour(4_500_000, 128_000))
        assertEquals("~2083 MB/hour", formatEstimatedDataPerHour(4_500_000, 128_000))
    }

    @Test
    fun clampsNegativeInputsToZero() {
        assertEquals(0, estimatedDataMegabytesPerHour(-1, -2))
    }
}
