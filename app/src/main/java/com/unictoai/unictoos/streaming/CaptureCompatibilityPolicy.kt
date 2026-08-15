package com.unictoai.unictoos.streaming

import kotlin.math.floor
import kotlin.math.min

data class PreviewBufferSize(val width: Int, val height: Int)

/**
 * Keeps the Android preview buffer at or below the active encoder dimensions while
 * preserving the preview view's aspect ratio. The stream encoder remains controlled
 * by the selected quality profile; this only limits the local preview surface.
 */
object CaptureCompatibilityPolicy {
    /**
     * The affected Infinix firmware can exhaust graphics resources while a preview
     * surface remains attached to an active encoder. Streaming can continue without
     * the local preview because the encoder owns its own output surface.
     */
    fun shouldIsolateLivePreview(manufacturer: String, model: String): Boolean =
        manufacturer.equals("Infinix", ignoreCase = true) && model.equals("X6853", ignoreCase = true)

    fun previewBufferSize(
        viewWidth: Int,
        viewHeight: Int,
        encoderWidth: Int,
        encoderHeight: Int,
    ): PreviewBufferSize {
        if (viewWidth <= 0 || viewHeight <= 0 || encoderWidth <= 0 || encoderHeight <= 0) {
            return PreviewBufferSize(viewWidth, viewHeight)
        }
        val scale = min(
            1f,
            min(encoderWidth.toFloat() / viewWidth.toFloat(), encoderHeight.toFloat() / viewHeight.toFloat()),
        )
        val width = evenAtLeastTwo(floor(viewWidth * scale).toInt())
        val height = evenAtLeastTwo(floor(viewHeight * scale).toInt())
        return PreviewBufferSize(width, height)
    }

    private fun evenAtLeastTwo(value: Int): Int = value.coerceAtLeast(2).let { candidate ->
        if (candidate % 2 == 0) candidate else candidate - 1
    }
}
