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
    val zIndex: Int = 0,
    val opacity: Float = 1f,
    val textContent: String = "",
    val textColor: Long = 0xFFFFFFFF,
    val textSizeSp: Float = 22f,
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

enum class PlatformPreset(val label: String, val helper: String, val serverHint: String) {
    YOUTUBE("YouTube", "Paste your YouTube stream key", "rtmps://a.rtmp.youtube.com/live2"),
    TWITCH("Twitch", "Paste your Twitch stream key", "rtmps://live.twitch.tv/app"),
    KICK("Kick", "Paste your Kick stream key", "Copy the current ingest URL from Kick dashboard"),
    CUSTOM("Custom RTMP", "Use any RTMP or RTMPS destination", "rtmp(s)://your-ingest-server/app"),
}

enum class StreamStatus {
    IDLE,
    PREPARING,
    LIVE,
    RECONNECTING,
    STOPPING,
    ERROR,
}

enum class SessionMode {
    BROADCAST,
    PRACTICE,
}

@Immutable
data class StreamHealthSample(
    val elapsedSeconds: Long,
    val bitrateKbps: Int,
    val fps: Int,
    val droppedFrames: Int,
    val audioLevel: Int,
    val batteryPercent: Int,
    val thermalStatus: Int,
    val networkLabel: String,
)

@Immutable
data class StreamSessionState(
    val status: StreamStatus = StreamStatus.IDLE,
    val mode: SessionMode = SessionMode.BROADCAST,
    val elapsedSeconds: Long = 0,
    val bitrateKbps: Int = 0,
    val fps: Int = 0,
    val droppedFrames: Int = 0,
    val audioLevel: Int = 0,
    val microphoneMuted: Boolean = false,
    val recording: Boolean = false,
    val reconnectAttempt: Int = 0,
    val message: String? = null,
)
