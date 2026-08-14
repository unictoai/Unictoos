package com.unictoai.unictoos.domain

import androidx.compose.runtime.Immutable

@Immutable
data class AudioSettings(
    val quality: AudioQuality = AudioQuality.STANDARD,
    val sampleRate: Int = 44_100,
    val echoCanceler: Boolean = true,
    val noiseSuppressor: Boolean = true,
) {
    val bitrate: Int get() = quality.bitrate
}

enum class AudioQuality(val label: String, val description: String, val bitrate: Int) {
    STANDARD("Standard • 128 kbps", "Reliable mobile default", 128_000),
    HIGH("High • 192 kbps", "More detail, more data", 192_000),
}
