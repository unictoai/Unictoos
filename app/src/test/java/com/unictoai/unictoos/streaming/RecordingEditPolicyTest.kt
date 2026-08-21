package com.unictoai.unictoos.streaming

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingEditPolicyTest {
    @Test
    fun acceptsSafeTrimRange() {
        val result = RecordingEditPolicy.validateTrim("recordings/live.mp4", 1_000L, 6_000L)
        assertTrue(result is RecordingEditValidation.Valid)
        assertEquals(5_000L, (result as RecordingEditValidation.Valid).plan.durationMs)
    }

    @Test
    fun rejectsInvalidTrimRanges() {
        assertTrue(RecordingEditPolicy.validateTrim("recordings/live.mp4", -1L, 6_000L) is RecordingEditValidation.Invalid)
        assertTrue(RecordingEditPolicy.validateTrim("recordings/live.mp4", 6_000L, 1_000L) is RecordingEditValidation.Invalid)
        assertTrue(RecordingEditPolicy.validateTrim("", 0L, 1_000L) is RecordingEditValidation.Invalid)
    }
}
