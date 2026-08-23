package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.PipConfig
import com.unictoai.unictoos.domain.PipGeometryPolicy
import com.unictoai.unictoos.domain.PipPosition
import com.unictoai.unictoos.domain.PipSize
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.Source
import com.unictoai.unictoos.domain.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PipConfigTest {
    @Test
    fun geometryKeepsPiPInsideFrameForEachCorner() {
        PipPosition.entries.forEach { position ->
            val rect = PipGeometryPolicy.rect(PipConfig(enabled = true, position = position, size = PipSize.MEDIUM), 16f / 9f)
            assertTrue(rect.left >= 0f)
            assertTrue(rect.top >= 0f)
            assertTrue(rect.right <= 1f)
            assertTrue(rect.bottom <= 1f)
            assertTrue(rect.right > rect.left)
            assertTrue(rect.bottom > rect.top)
        }
    }

    @Test
    fun scenePayloadRoundTripsPiPAndBackgroundAudio() {
        val scene = Scene(
            id = "gameplay",
            name = "Gameplay + Camera",
            aspectRatio = AspectRatio.LANDSCAPE,
            sources = listOf(
                Source("screen", "Screen", SourceType.SCREEN),
                Source("camera", "Camera", SourceType.CAMERA),
            ),
            pipConfig = PipConfig(enabled = true, position = PipPosition.TOP_LEFT, size = PipSize.LARGE, dropShadow = false),
            backgroundAudioMode = true,
        )

        val decoded = ScenePayloadCodec.decode(ScenePayloadCodec.encode(scene))

        assertEquals(scene.pipConfig, decoded?.pipConfig)
        assertTrue(decoded?.backgroundAudioMode == true)
    }

    @Test
    fun oldPayloadDefaultsToNoPiPAndNoBackgroundAudio() {
        val decoded = ScenePayloadCodec.decode("""{"id":"old","name":"Old","aspectRatio":"PORTRAIT","sources":[]}""")

        assertFalse(decoded?.pipConfig?.enabled == true)
        assertFalse(decoded?.backgroundAudioMode == true)
    }
}
