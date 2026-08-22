package com.unictoai.unictoos.domain

import androidx.compose.runtime.Immutable

@Immutable
data class StreamQuality(
    val preset: StreamQualityPreset,
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrate: Int,
    val keyframeIntervalSeconds: Int = 2,
) {
    val bitrateMbps: Float get() = bitrate / 1_000_000f
    val displayName: String get() = preset.label
    val isCustom: Boolean get() = preset == StreamQualityPreset.CUSTOM

    fun validated(): StreamQuality = copy(
        width = width.coerceIn(320, 1_920),
        height = height.coerceIn(320, 1_920),
        fps = fps.coerceIn(24, 60).let { value -> if (value <= 24) 24 else if (value <= 30) 30 else 60 },
        bitrate = bitrate.coerceIn(MIN_BITRATE, MAX_BITRATE),
        keyframeIntervalSeconds = keyframeIntervalSeconds.coerceIn(1, 4),
    )

    fun forAspectRatio(aspectRatio: AspectRatio): StreamQuality = when {
        aspectRatio == AspectRatio.LANDSCAPE && width < height -> copy(width = height, height = width)
        aspectRatio == AspectRatio.PORTRAIT && width > height -> copy(width = height, height = width)
        else -> this
    }.validated()

    companion object {
        const val MIN_BITRATE = 1_000_000
        const val MAX_BITRATE = 8_000_000
        const val DEFAULT_BITRATE = 4_500_000
    }
}

enum class StreamQualityPreset(
    val label: String,
    val description: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrate: Int,
    val keyframeIntervalSeconds: Int = 2,
) {
    DATA_SAVER("480p • 30 FPS", "Lower data use", 480, 854, 30, 2_000_000),
    BALANCED("720p • 30 FPS", "Recommended default", 720, 1_280, 30, 4_500_000),
    HIGH_FPS_720("720p • 60 FPS", "Smoother motion", 720, 1_280, 60, 6_000_000),
    FULL_HD("1080p • 30 FPS", "Sharper picture", 1_080, 1_920, 30, 6_000_000),
    FULL_HD_HIGH_FPS("1080p • 60 FPS", "Requires strong upload", 1_080, 1_920, 60, 8_000_000),
    CUSTOM("Custom", "Tune bitrate and frame rate", 720, 1_280, 30, StreamQuality.DEFAULT_BITRATE),
    ;

    fun toQuality(): StreamQuality = StreamQuality(
        preset = this,
        width = width,
        height = height,
        fps = fps,
        bitrate = bitrate,
        keyframeIntervalSeconds = keyframeIntervalSeconds,
    )
}
