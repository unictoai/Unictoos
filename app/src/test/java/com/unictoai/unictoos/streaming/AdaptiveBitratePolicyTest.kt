package com.unictoai.unictoos.streaming

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveBitratePolicyTest {
    @Test
    fun degradationWaitsFifteenSecondsBeforeSteppingDown() {
        val beforeThreshold = decideAdaptiveBitrate(
            currentTargetBitrate = 4_500_000,
            baselineTargetBitrate = 4_500_000,
            rollingAverageBitrate = 2_000_000,
            degradedSeconds = 14,
            recoveredSeconds = 0,
        )
        val atThreshold = decideAdaptiveBitrate(
            currentTargetBitrate = 4_500_000,
            baselineTargetBitrate = 4_500_000,
            rollingAverageBitrate = 2_000_000,
            degradedSeconds = 15,
            recoveredSeconds = 0,
        )

        assertEquals(AdaptiveBitrateAction.HOLD, beforeThreshold.action)
        assertEquals(AdaptiveBitrateAction.STEP_DOWN, atThreshold.action)
        assertEquals(3_825_000, atThreshold.bitrate)
    }

    @Test
    fun recoveryWaitsSixtySecondsAndStepsUpGradually() {
        val beforeThreshold = decideAdaptiveBitrate(
            currentTargetBitrate = 3_825_000,
            baselineTargetBitrate = 4_500_000,
            rollingAverageBitrate = 3_700_000,
            degradedSeconds = 0,
            recoveredSeconds = 59,
        )
        val atThreshold = decideAdaptiveBitrate(
            currentTargetBitrate = 3_825_000,
            baselineTargetBitrate = 4_500_000,
            rollingAverageBitrate = 3_700_000,
            degradedSeconds = 0,
            recoveredSeconds = 60,
        )

        assertEquals(AdaptiveBitrateAction.HOLD, beforeThreshold.action)
        assertEquals(AdaptiveBitrateAction.STEP_UP, atThreshold.action)
        assertEquals(4_207_500, atThreshold.bitrate)
    }

    @Test
    fun holdKeepsCurrentTargetAndBoundsAreRespected() {
        val minimum = decideAdaptiveBitrate(
            currentTargetBitrate = 1_000_000,
            baselineTargetBitrate = 4_500_000,
            rollingAverageBitrate = 100_000,
            degradedSeconds = 20,
            recoveredSeconds = 0,
        )
        val maximum = decideAdaptiveBitrate(
            currentTargetBitrate = 4_500_000,
            baselineTargetBitrate = 4_500_000,
            rollingAverageBitrate = 4_500_000,
            degradedSeconds = 0,
            recoveredSeconds = 20,
        )

        assertEquals(AdaptiveBitrateAction.HOLD, minimum.action)
        assertEquals(1_000_000, minimum.bitrate)
        assertEquals(AdaptiveBitrateAction.HOLD, maximum.action)
        assertEquals(4_500_000, maximum.bitrate)
    }
}
