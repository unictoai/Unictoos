package com.unictoai.unictoos.streaming

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PipelineReleasePolicyTest {
    @Test
    fun successfulReleaseMarksPipelineReleased() {
        assertTrue(PipelineReleasePolicy.markReleased(previouslyReleased = false, releaseSucceeded = true))
    }

    @Test
    fun failedReleaseRemainsRetryable() {
        assertFalse(PipelineReleasePolicy.markReleased(previouslyReleased = false, releaseSucceeded = false))
    }

    @Test
    fun alreadyReleasedStateRemainsReleased() {
        assertTrue(PipelineReleasePolicy.markReleased(previouslyReleased = true, releaseSucceeded = false))
    }
}
