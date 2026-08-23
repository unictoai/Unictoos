package com.unictoai.unictoos.domain

import androidx.compose.runtime.Immutable
import kotlin.math.max
import kotlin.math.min

@Immutable
enum class PipPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
}

@Immutable
enum class PipSize(val widthFraction: Float) {
    SMALL(0.20f),
    MEDIUM(0.30f),
    LARGE(0.40f),
}

/**
 * Scene-level camera picture-in-picture configuration. The streaming service
 * treats this as an explicit request and can safely fall back to a single
 * source when the device cannot provide a second capture surface.
 */
@Immutable
data class PipConfig(
    val enabled: Boolean = false,
    val position: PipPosition = PipPosition.BOTTOM_RIGHT,
    val size: PipSize = PipSize.MEDIUM,
    val cornerRadiusDp: Int = 16,
    val borderWidthDp: Int = 2,
    val dropShadow: Boolean = true,
) {
    val safeCornerRadiusDp: Int
        get() = cornerRadiusDp.coerceIn(0, 64)

    val safeBorderWidthDp: Int
        get() = borderWidthDp.coerceIn(0, 8)
}

data class PipRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

object PipGeometryPolicy {
    fun rect(config: PipConfig, frameAspectRatio: Float): PipRect {
        val safeAspect = frameAspectRatio.coerceIn(0.25f, 4f)
        val width = config.size.widthFraction.coerceIn(0.20f, 0.40f)
        val height = min(width / (16f / 9f) / safeAspect, 0.40f)
        val margin = 0.04f
        val left = when (config.position) {
            PipPosition.TOP_LEFT, PipPosition.BOTTOM_LEFT -> margin
            PipPosition.TOP_RIGHT, PipPosition.BOTTOM_RIGHT -> 1f - margin - width
        }
        val top = when (config.position) {
            PipPosition.TOP_LEFT, PipPosition.TOP_RIGHT -> margin
            PipPosition.BOTTOM_LEFT, PipPosition.BOTTOM_RIGHT -> 1f - margin - height
        }
        return PipRect(
            left = left.coerceIn(0f, 1f - width),
            top = top.coerceIn(0f, 1f - height),
            right = max(left + width, 0f).coerceAtMost(1f),
            bottom = max(top + height, 0f).coerceAtMost(1f),
        )
    }
}
