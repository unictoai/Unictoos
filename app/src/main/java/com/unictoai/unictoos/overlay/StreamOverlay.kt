package com.unictoai.unictoos.overlay

sealed interface StreamOverlay {
    val id: String
    val x: Float
    val y: Float

    data class TextOverlay(
        override val id: String,
        val text: String,
        override val x: Float,
        override val y: Float,
        val textSizeSp: Float,
        val textColor: Long = 0xFFFFFFFF,
        val backgroundColor: Long? = 0xB0101216,
        val font: String = "sans-serif",
        val width: Float = 0.90f,
        val height: Float = 0.20f,
        val opacity: Float = 1f,
    ) : StreamOverlay

    data class ImageOverlay(
        override val id: String,
        val imagePath: String,
        override val x: Float,
        override val y: Float,
        val width: Float,
        val height: Float,
        val opacity: Float = 1f,
    ) : StreamOverlay

    data class TimerOverlay(
        override val id: String,
        override val x: Float,
        override val y: Float,
        val format: String = "UPTIME: %H:%M:%S",
        val textSizeSp: Float = 16f,
        val textColor: Long = 0xFFFFFFFF,
        val backgroundColor: Long? = 0xB0101216,
    ) : StreamOverlay

    data class ChatOverlay(
        override val id: String,
        override val x: Float,
        override val y: Float,
        val messages: List<ChatLine> = emptyList(),
        val textSizeSp: Float = 14f,
        val textColor: Long = 0xFFFFFFFF,
        val backgroundColor: Long? = 0xB0101216,
    ) : StreamOverlay
}

data class ChatLine(
    val author: String,
    val text: String,
    val timestamp: Long,
)

fun StreamOverlay.safe(): StreamOverlay = when (this) {
    is StreamOverlay.TextOverlay -> copy(
        text = text.take(500),
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f),
        textSizeSp = textSizeSp.coerceIn(10f, 72f),
        width = width.coerceIn(0.05f, 1f),
        height = height.coerceIn(0.05f, 1f),
        opacity = opacity.coerceIn(0f, 1f),
    )
    is StreamOverlay.ImageOverlay -> copy(
        imagePath = imagePath.take(2_000),
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f),
        width = width.coerceIn(0.05f, 1f),
        height = height.coerceIn(0.05f, 1f),
        opacity = opacity.coerceIn(0f, 1f),
    )
    is StreamOverlay.TimerOverlay -> copy(
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f),
        textSizeSp = textSizeSp.coerceIn(10f, 72f),
    )
    is StreamOverlay.ChatOverlay -> copy(
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f),
        textSizeSp = textSizeSp.coerceIn(10f, 48f),
        messages = messages.takeLast(5).map { it.copy(author = it.author.take(80), text = it.text.take(300)) },
    )
}
