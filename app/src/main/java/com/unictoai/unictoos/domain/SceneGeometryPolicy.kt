package com.unictoai.unictoos.domain

/** Keeps a source rectangle inside the normalized [0, 1] composition canvas. */
data class NormalizedGeometry(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

object SceneGeometryPolicy {
    private const val MIN_SIZE = 0.05f
    private const val MAX_POSITION = 1f - MIN_SIZE

    fun clamp(x: Float, y: Float, width: Float, height: Float): NormalizedGeometry {
        val safeX = x.coerceIn(0f, MAX_POSITION)
        val safeY = y.coerceIn(0f, MAX_POSITION)
        val safeWidth = width.coerceIn(MIN_SIZE, 1f - safeX)
        val safeHeight = height.coerceIn(MIN_SIZE, 1f - safeY)
        return NormalizedGeometry(safeX, safeY, safeWidth, safeHeight)
    }
}
