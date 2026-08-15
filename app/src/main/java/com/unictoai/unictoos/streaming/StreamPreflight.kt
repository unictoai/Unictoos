package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.StreamQuality

sealed interface PreflightResult {
    data object Ready : PreflightResult
    data class Blocked(val message: String) : PreflightResult
}

enum class StorageSessionMode {
    STREAM_ONLY,
    STREAM_PLUS_RECORDING,
    PRACTICE_RECORDING,
}

object StreamPreflight {
    fun validateEndpoint(endpoint: String, practice: Boolean): PreflightResult {
        if (practice) return PreflightResult.Ready
        val value = endpoint.trim()
        if (value.isBlank()) return PreflightResult.Blocked("Configure a streaming destination first")
        if (!value.startsWith("rtmp://", ignoreCase = true) && !value.startsWith("rtmps://", ignoreCase = true)) {
            return PreflightResult.Blocked("The destination must use an RTMP or RTMPS server URL")
        }
        return PreflightResult.Ready
    }

    fun validateProfile(profile: StreamQuality): PreflightResult {
        if (profile.width < 320 || profile.height < 240) return PreflightResult.Blocked("The selected resolution is too small for streaming")
        if (profile.fps !in 15..60) return PreflightResult.Blocked("FPS must be between 15 and 60")
        if (profile.bitrate !in 250_000..12_000_000) return PreflightResult.Blocked("Video bitrate is outside the safe device range")
        if (profile.width.toLong() * profile.height.toLong() > 8_294_400L) return PreflightResult.Blocked("The selected resolution is too demanding for this build")
        return PreflightResult.Ready
    }

    fun validateEnvironment(
        networkAvailable: Boolean,
        availableStorageBytes: Long,
        batteryPercent: Int,
        isCharging: Boolean,
        thermalStatus: Int,
        minimumStorageBytes: Long,
        storageMode: StorageSessionMode = StorageSessionMode.STREAM_ONLY,
        estimatedRecordingBytes: Long = 0L,
    ): PreflightResult {
        if (!networkAvailable) return PreflightResult.Blocked("No network connection is available")
        val requiredStorageBytes = when (storageMode) {
            StorageSessionMode.STREAM_ONLY -> 0L
            StorageSessionMode.STREAM_PLUS_RECORDING, StorageSessionMode.PRACTICE_RECORDING ->
                maxOf(minimumStorageBytes, estimatedRecordingBytes)
        }
        if (availableStorageBytes < requiredStorageBytes) return PreflightResult.Blocked("Not enough storage is available for a safe session")
        if (batteryPercent in 0..9 && !isCharging) return PreflightResult.Blocked("Battery is below 10%. Connect a charger before streaming")
        if (thermalStatus >= android.os.PowerManager.THERMAL_STATUS_CRITICAL) return PreflightResult.Blocked("Device temperature is too high to start a stable session")
        return PreflightResult.Ready
    }
}
