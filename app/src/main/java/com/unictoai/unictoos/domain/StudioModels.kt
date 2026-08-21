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
    val x: Float = 0.05f,
    val y: Float = 0.08f,
    val width: Float = 0.90f,
    val height: Float = 0.24f,
    val fillColor: Long = 0xFF101216,
    val imageUri: String = "",
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
    CUSTOM("Custom transport", "Use RTMP, RTMPS, or a full SRT listener URL", "rtmp(s)://your-ingest-server/app or srt://host:port?streamid=..."),
}

enum class StreamStatus {
    IDLE,
    PREPARING,
    CONNECTING,
    LIVE,
    RECONNECTING,
    STOPPING,
    STOPPED,
    ERROR,
}

enum class SessionMode {
    BROADCAST,
    PRACTICE,
}

@Immutable
data class StreamHealthSample(
    val elapsedSeconds: Long,
    val sessionId: String = "",
    val bitrateKbps: Int,
    val fps: Int,
    val droppedFrames: Int = -1,
    val audioLevel: Int = -1,
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
    val droppedFrames: Int = -1,
    val audioLevel: Int = -1,
    val microphoneMuted: Boolean = false,
    val recording: Boolean = false,
    val recordingState: RecordingState = RecordingState.IDLE,
    val reconnectAttempt: Int = 0,
    val captureReady: Boolean = false,
    val encoderReady: Boolean = false,
    val previewReady: Boolean = false,
    val pipelineGeneration: Long = 0L,
    val message: String? = null,
)
