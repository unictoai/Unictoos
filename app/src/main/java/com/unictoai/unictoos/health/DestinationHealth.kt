package com.unictoai.unictoos.health

import androidx.compose.runtime.Immutable

@Immutable
enum class HealthState {
    HEALTHY,
    DEGRADED,
    RECONNECTING,
    FAILED,
    IDLE,
}

@Immutable
data class DestinationHealth(
    val profileId: String,
    val profileName: String,
    val state: HealthState = HealthState.IDLE,
    val currentBitrate: Long = 0L,
    val droppedFrames: Int = -1,
    val pingMs: Long? = null,
    val lastError: String? = null,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
)
