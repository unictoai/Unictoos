package com.unictoai.unictoos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamQualityTest {
    @Test
    fun landscapeSceneSwapsPortraitPresetDimensions() {
        val quality = StreamQualityPreset.FULL_HD.toQuality().forAspectRatio(AspectRatio.LANDSCAPE)

        assertEquals(1_920, quality.width)
        assertEquals(1_080, quality.height)
    }

    @Test
    fun portraitSceneKeepsPortraitPresetDimensions() {
        val quality = StreamQualityPreset.FULL_HD.toQuality().forAspectRatio(AspectRatio.PORTRAIT)

        assertEquals(1_080, quality.width)
        assertEquals(1_920, quality.height)
    }
}
