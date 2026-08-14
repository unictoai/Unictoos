package com.unictoai.unictoos.domain

import androidx.compose.runtime.Immutable

@Immutable
data class Scene(
    val id: String,
    val name: String,
    val sources: List<Source> = emptyList(),
    val aspectRatio: AspectRatio = AspectRatio.PORTRAIT,
)

@Immutable
data class Source(
    val id: String,
    val name: String,
    val type: SourceType,
    val enabled: Boolean = true,
)

enum class SourceType(val label: String) {
    SCREEN("Screen"),
    CAMERA("Camera"),
    IMAGE("Image"),
    TEXT("Text"),
    COLOR("Color"),
}

enum class AspectRatio(val label: String, val ratio: String) {
    PORTRAIT("Portrait", "9:16"),
    LANDSCAPE("Landscape", "16:9"),
}

@Immutable
data class StreamDestination(
    val id: String,
    val name: String,
    val platform: PlatformPreset,
    val serverUrl: String = "",
    val streamKey: String = "",
    val isConfigured: Boolean = false,
)

enum class PlatformPreset(val label: String, val helper: String) {
    YOUTUBE("YouTube", "Paste your YouTube stream key"),
    TWITCH("Twitch", "Paste your Twitch stream key"),
    KICK("Kick", "Paste your Kick stream key"),
    CUSTOM("Custom RTMP", "Use any RTMP or RTMPS destination"),
}

enum class StreamStatus {
    IDLE,
    PREPARING,
    LIVE,
    RECONNECTING,
    STOPPING,
    ERROR,
}

@Immutable
data class StreamSessionState(
    val status: StreamStatus = StreamStatus.IDLE,
    val elapsedSeconds: Long = 0,
    val bitrateKbps: Int = 0,
    val fps: Int = 0,
    val droppedFrames: Int = 0,
    val message: String? = null,
)
