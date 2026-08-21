package com.unictoai.unictoos.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalAnalyticsComparisonTest {
    @Test
    fun emptyHistoryProducesNeutralComparison() {
        assertEquals(LocalAnalyticsComparison(0, 0L, 0, 0, 0), LocalAnalyticsComparison.from(emptyList()))
    }

    @Test
    fun averagesRecentSessionMetrics() {
        val records = listOf(
            LocalAnalyticsSession("a", "BROADCAST", "direct", 0L, 10_000L, 10L, 4_000, 4_500, 30, 2, 0, null, null),
            LocalAnalyticsSession("b", "BROADCAST", "direct", 0L, 20_000L, 20L, 6_000, 6_500, 30, -1, 2, null, null),
        )
        assertEquals(LocalAnalyticsComparison(2, 15L, 5_000, 1, 1), LocalAnalyticsComparison.from(records))
    }
}
