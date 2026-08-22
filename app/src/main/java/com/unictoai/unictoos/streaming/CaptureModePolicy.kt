package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.SourceType

/**
 * Selects the capture path that the current runtime can actually provide.
 *
 * Unictoos does not yet composite camera and screen sources into PiP. When an
 * older scene contains both enabled sources, prefer the camera path so a
 * first-run broadcast does not unexpectedly request screen capture instead of
 * using the visible camera scene. Screen-only scenes still use MediaProjection.
 */
object CaptureModePolicy {
    const val NONE = "none"
    const val SCREEN = "screen"
    const val CAMERA = "camera"

    fun forScene(scene: Scene): String = when {
        scene.sources.any { it.enabled && it.type == SourceType.CAMERA } -> CAMERA
        scene.sources.any { it.enabled && it.type == SourceType.SCREEN } -> SCREEN
        else -> NONE
    }
}
