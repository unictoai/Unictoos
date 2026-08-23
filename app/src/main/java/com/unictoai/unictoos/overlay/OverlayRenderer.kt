package com.unictoai.unictoos.overlay

import com.pedro.encoder.input.gl.render.filters.`object`.TextObjectFilterRender
import com.pedro.library.view.GlInterface

/**
 * Applies bounded text-like overlays to RootEncoder's GL output. Image assets
 * remain caller-owned because Android URI/file access must be validated before
 * a texture is created. All filters are installed before the encoder consumes
 * the next frame.
 */
class OverlayRenderer(
    private val glInterface: GlInterface,
    private val density: Float,
) {
    data class Result(
        val renderedCount: Int,
        val skippedCount: Int,
    )

    fun render(overlays: List<StreamOverlay>, uptimeSeconds: Long = 0L): Result {
        glInterface.clearFilters()
        var rendered = 0
        var skipped = 0
        overlays.take(MAX_OVERLAYS).forEach { raw ->
            when (val overlay = raw.safe()) {
                is StreamOverlay.TextOverlay -> {
                    addTextFilter(
                        text = overlay.text,
                        textSizeSp = overlay.textSizeSp,
                        textColor = overlay.textColor,
                        backgroundColor = overlay.backgroundColor,
                        x = overlay.x,
                        y = overlay.y,
                        width = overlay.width,
                        height = overlay.height,
                        opacity = overlay.opacity,
                    )
                    rendered += 1
                }
                is StreamOverlay.TimerOverlay -> {
                    addTextFilter(
                        text = formatTimer(overlay.format, uptimeSeconds),
                        textSizeSp = overlay.textSizeSp,
                        textColor = overlay.textColor,
                        backgroundColor = overlay.backgroundColor,
                        x = overlay.x,
                        y = overlay.y,
                        width = 0.52f,
                        height = 0.14f,
                        opacity = 1f,
                    )
                    rendered += 1
                }
                is StreamOverlay.ChatOverlay -> {
                    val text = overlay.messages.takeLast(5).joinToString("\n") { "${it.author.take(32)}: ${it.text.take(120)}" }
                    if (text.isBlank()) {
                        skipped += 1
                    } else {
                        addTextFilter(
                            text = text,
                            textSizeSp = overlay.textSizeSp,
                            textColor = overlay.textColor,
                            backgroundColor = overlay.backgroundColor,
                            x = overlay.x,
                            y = overlay.y,
                            width = 0.62f,
                            height = 0.34f,
                            opacity = 1f,
                        )
                        rendered += 1
                    }
                }
                is StreamOverlay.ImageOverlay -> skipped += 1
            }
        }
        return Result(renderedCount = rendered, skippedCount = skipped)
    }

    private fun addTextFilter(
        text: String,
        textSizeSp: Float,
        textColor: Long,
        backgroundColor: Long?,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        opacity: Float,
    ) {
        val filter = TextObjectFilterRender().apply {
            setText(text, textSizeSp * density, textColor.toInt(), (backgroundColor ?: 0L).toInt())
            setAlpha(opacity.coerceIn(0f, 1f))
            setScale((width * 100f).coerceIn(5f, 100f), (height * 100f).coerceIn(5f, 100f))
            setPosition((x * 100f).coerceIn(0f, 100f), (y * 100f).coerceIn(0f, 100f))
        }
        if (glInterface.filtersCount() == 0) glInterface.setFilter(filter) else glInterface.addFilter(filter)
    }

    private fun formatTimer(format: String, uptimeSeconds: Long): String {
        val seconds = uptimeSeconds.coerceAtLeast(0L)
        val h = seconds / 3_600L
        val m = (seconds % 3_600L) / 60L
        val s = seconds % 60L
        return if (format.contains("%H:%M:%S")) {
            format.replace("%H", "%02d".format(h)).replace("%M", "%02d".format(m)).replace("%S", "%02d".format(s))
        } else {
            "UPTIME: %02d:%02d:%02d".format(h, m, s)
        }
    }

    private companion object {
        const val MAX_OVERLAYS = 24
    }
}
