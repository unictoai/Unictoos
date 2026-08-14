package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.Source
import com.unictoai.unictoos.domain.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneCompositionPlanTest {
    @Test
    fun planCountsRealTextAndUnsupportedConcurrentLayers() {
        val scene = Scene(
            id = "s",
            name = "Show",
            aspectRatio = AspectRatio.LANDSCAPE,
            sources = listOf(
                Source("screen", "Screen", SourceType.SCREEN),
                Source("camera", "Camera", SourceType.CAMERA),
                Source("title", "Title", SourceType.TEXT, textContent = "Live now"),
                Source("image", "Image", SourceType.IMAGE),
            ),
        )
        val plan = SceneCompositionPlan.from(scene)
        assertEquals(1, plan.textOverlayCount)
        assertEquals(1, plan.imageLayerCount)
        assertTrue(plan.hasConcurrentVideoSources)
        assertEquals(2, plan.unsupportedLayerCount)
    }

    @Test
    fun disabledOrEmptyTextSourcesDoNotCountAsRenderedOverlays() {
        val scene = Scene(
            id = "s",
            name = "Show",
            sources = listOf(
                Source("disabled", "Disabled", SourceType.TEXT, enabled = false, textContent = "Hidden"),
                Source("empty", "Empty", SourceType.TEXT, textContent = ""),
                Source("color", "Color", SourceType.COLOR),
            ),
        )
        val plan = SceneCompositionPlan.from(scene)
        assertEquals(0, plan.textOverlayCount)
        assertFalse(plan.hasConcurrentVideoSources)
        assertEquals(1, plan.unsupportedLayerCount)
    }
}
