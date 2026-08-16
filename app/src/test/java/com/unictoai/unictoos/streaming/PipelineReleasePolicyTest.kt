package com.unictoai.unictoos.streaming

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PipelineReleasePolicyTest {
    @Test
    fun successfulReleaseReachesTerminalAndAllowsNewPipeline() {
        val attempt = PipelineReleasePolicy.begin(PipelineReleaseState.AVAILABLE, generation = 1L)
        assertNotNull(attempt)

        val state = PipelineReleasePolicy.complete(
            state = PipelineReleaseState.RELEASING,
            attempt = attempt!!,
            currentGeneration = 1L,
            releaseSucceeded = true,
        )

        assertEquals(PipelineReleaseState.TERMINAL, state)
        assertTrue(PipelineReleasePolicy.canCreateNewPipeline(state))
    }

    @Test
    fun failedReleaseRemainsRetryableAndBlocksNewPipeline() {
        val attempt = PipelineReleasePolicy.begin(PipelineReleaseState.AVAILABLE, generation = 2L)!!
        val state = PipelineReleasePolicy.complete(
            state = PipelineReleaseState.RELEASING,
            attempt = attempt,
            currentGeneration = 2L,
            releaseSucceeded = false,
        )

        assertEquals(PipelineReleaseState.FAILED, state)
        assertFalse(PipelineReleasePolicy.canCreateNewPipeline(state))
        assertNotNull(PipelineReleasePolicy.begin(state, generation = 2L))
    }

    @Test
    fun releaseCannotOverlapAndStaleGenerationCannotCompleteIt() {
        val attempt = PipelineReleasePolicy.begin(PipelineReleaseState.RELEASING, generation = 3L)
        assertNull(attempt)

        val activeAttempt = PipelineReleasePolicy.begin(PipelineReleaseState.AVAILABLE, generation = 4L)!!
        val state = PipelineReleasePolicy.complete(
            state = PipelineReleaseState.RELEASING,
            attempt = activeAttempt,
            currentGeneration = 5L,
            releaseSucceeded = true,
        )

        assertEquals(PipelineReleaseState.RELEASING, state)
        assertFalse(PipelineReleasePolicy.canCreateNewPipeline(state))
    }

    @Test
    fun fiftySuccessfulRecreateCyclesAlwaysCrossTerminalBoundary() {
        var state = PipelineReleaseState.AVAILABLE
        repeat(50) { cycle ->
            val attempt = PipelineReleasePolicy.begin(state, generation = cycle.toLong() + 1L)!!
            state = PipelineReleasePolicy.complete(
                state = PipelineReleaseState.RELEASING,
                attempt = attempt,
                currentGeneration = cycle.toLong() + 1L,
                releaseSucceeded = true,
            )
            assertTrue("cycle $cycle did not reach terminal", PipelineReleasePolicy.canCreateNewPipeline(state))
            state = PipelineReleaseState.AVAILABLE
        }
        assertEquals(PipelineReleaseState.AVAILABLE, state)
    }

    @Test
    fun compatibilityBooleanContractRemainsStable() {
        assertTrue(PipelineReleasePolicy.markReleased(previouslyReleased = false, releaseSucceeded = true))
        assertFalse(PipelineReleasePolicy.markReleased(previouslyReleased = false, releaseSucceeded = false))
        assertTrue(PipelineReleasePolicy.markReleased(previouslyReleased = true, releaseSucceeded = false))
    }
}
