package com.unictoai.unictoos

import com.unictoai.unictoos.domain.SceneGeometryPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneGeometryPolicyTest {
    @Test
    fun clampsPositionAndSizeToCanvasBounds() {
        val geometry = SceneGeometryPolicy.clamp(0.9f, 0.8f, 1f, 1f)

        assertEquals(0.9f, geometry.x, 0.0001f)
        assertEquals(0.8f, geometry.y, 0.0001f)
        assertEquals(0.1f, geometry.width, 0.0001f)
        assertEquals(0.2f, geometry.height, 0.0001f)
        assertTrue(geometry.x + geometry.width <= 1.0001f)
        assertTrue(geometry.y + geometry.height <= 1.0001f)
    }

    @Test
    fun clampsNegativeAndOversizedInputs() {
        val geometry = SceneGeometryPolicy.clamp(-5f, 4f, -1f, 8f)

        assertEquals(0f, geometry.x, 0.0001f)
        assertEquals(0.95f, geometry.y, 0.0001f)
        assertEquals(0.05f, geometry.width, 0.0001f)
        assertEquals(0.05f, geometry.height, 0.0001f)
    }
}
