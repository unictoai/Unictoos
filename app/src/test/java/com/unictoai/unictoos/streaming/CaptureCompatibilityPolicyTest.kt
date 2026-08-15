package com.unictoai.unictoos.streaming

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureCompatibilityPolicyTest {
    @Test
    fun clampsLargePreviewToEncoderBoundsWithoutUpscaling() {
        val result = CaptureCompatibilityPolicy.previewBufferSize(
            viewWidth = 1080,
            viewHeight = 1440,
            encoderWidth = 720,
            encoderHeight = 1280,
        )

        assertEquals(720, result.width)
        assertEquals(960, result.height)
    }

    @Test
    fun preservesSmallPreviewSizeAndMakesDimensionsEven() {
        val result = CaptureCompatibilityPolicy.previewBufferSize(
            viewWidth = 601,
            viewHeight = 901,
            encoderWidth = 1280,
            encoderHeight = 720,
        )

        assertEquals(480, result.width)
        assertEquals(720, result.height)
    }

    @Test
    fun invalidDimensionsAreReturnedForCallerValidation() {
        val result = CaptureCompatibilityPolicy.previewBufferSize(
            viewWidth = 0,
            viewHeight = 0,
            encoderWidth = 720,
            encoderHeight = 1280,
        )

        assertEquals(0, result.width)
        assertEquals(0, result.height)
    }
}
