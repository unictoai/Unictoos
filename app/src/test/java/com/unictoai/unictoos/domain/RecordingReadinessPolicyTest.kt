package com.unictoai.unictoos.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingReadinessPolicyTest {
    @Test
    fun readyCaptureAndEncoderDoNotRequirePreview() {
        assertTrue(RecordingReadinessPolicy.isReady(captureReady = true, encoderReady = true))
    }

    @Test
    fun missingCaptureOrEncoderBlocksRecording() {
        assertFalse(RecordingReadinessPolicy.isReady(captureReady = false, encoderReady = true))
        assertFalse(RecordingReadinessPolicy.isReady(captureReady = true, encoderReady = false))
    }
}
