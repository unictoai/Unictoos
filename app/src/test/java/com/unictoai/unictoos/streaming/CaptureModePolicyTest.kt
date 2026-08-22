package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.Source
import com.unictoai.unictoos.domain.SourceType
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureModePolicyTest {
    @Test
    fun mixedLegacyScenePrefersCameraBecausePiPIsNotImplemented() {
        val scene = Scene(
            id = "mixed",
            name = "Mixed",
            aspectRatio = AspectRatio.LANDSCAPE,
            sources = listOf(
                Source("screen", "Screen", SourceType.SCREEN),
                Source("camera", "Camera", SourceType.CAMERA),
            ),
        )

        assertEquals(CaptureModePolicy.CAMERA, CaptureModePolicy.forScene(scene))
    }

    @Test
    fun screenOnlySceneUsesScreenCapture() {
        val scene = Scene(
            id = "screen",
            name = "Screen",
            aspectRatio = AspectRatio.LANDSCAPE,
            sources = listOf(Source("screen", "Screen", SourceType.SCREEN)),
        )

        assertEquals(CaptureModePolicy.SCREEN, CaptureModePolicy.forScene(scene))
    }

    @Test
    fun emptySceneHasNoCaptureMode() {
        val scene = Scene(
            id = "empty",
            name = "Empty",
            aspectRatio = AspectRatio.PORTRAIT,
            sources = emptyList(),
        )

        assertEquals(CaptureModePolicy.NONE, CaptureModePolicy.forScene(scene))
    }
}
