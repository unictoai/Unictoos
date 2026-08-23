package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.SourceType

/**
 * Selects the capture path that the current runtime can actually provide.
 *
 * Mixed scenes remain camera-first unless PiP is explicitly enabled. This
 * protects older saved scenes from unexpectedly requesting screen capture while
 * allowing the v0.5 Gameplay + Camera template to opt into composition.
 */
object CaptureModePolicy {
    const val NONE = "none"
    const val SCREEN = "screen"
    const val CAMERA = "camera"

    fun forScene(scene: Scene): String = when {
        scene.pipConfig?.enabled == true &&
            scene.sources.any { it.enabled && it.type == SourceType.SCREEN } &&
            scene.sources.any { it.enabled && it.type == SourceType.CAMERA } -> SCREEN
        scene.sources.any { it.enabled && it.type == SourceType.CAMERA } -> CAMERA
        scene.sources.any { it.enabled && it.type == SourceType.SCREEN } -> SCREEN
        else -> NONE
    }
}
