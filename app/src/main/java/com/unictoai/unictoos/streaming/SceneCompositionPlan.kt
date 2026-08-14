package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.SourceType

data class SceneCompositionPlan(
    val textOverlayCount: Int,
    val imageLayerCount: Int,
    val hasScreenLayer: Boolean,
    val hasCameraLayer: Boolean,
    val unsupportedLayerCount: Int,
) {
    val hasConcurrentVideoSources: Boolean
        get() = hasScreenLayer && hasCameraLayer

    companion object {
        fun from(scene: Scene): SceneCompositionPlan {
            val enabled = scene.sources.filter { it.enabled }
            val textCount = enabled.count { it.type == SourceType.TEXT && it.textContent.isNotBlank() }
            val imageCount = enabled.count { it.type == SourceType.IMAGE }
            val hasScreen = enabled.any { it.type == SourceType.SCREEN }
            val hasCamera = enabled.any { it.type == SourceType.CAMERA }
            val unsupported = imageCount + enabled.count { it.type == SourceType.COLOR } + if (hasScreen && hasCamera) 1 else 0
            return SceneCompositionPlan(textCount, imageCount, hasScreen, hasCamera, unsupported)
        }
    }
}
