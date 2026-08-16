package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.StreamQuality

enum class PreflightOutcomeState {
    READY,
    ACTION_REQUIRED,
    CAUTION,
}

data class PreflightOutcome(
    val id: String,
    val label: String,
    val value: String,
    val state: PreflightOutcomeState,
    val detail: String,
)

object PreflightOutcomeEvaluator {
    fun evaluate(
        audioReady: Boolean,
        cameraReady: Boolean,
        networkReady: Boolean,
        destinationReady: Boolean,
        quality: StreamQuality,
    ): List<PreflightOutcome> = listOf(
        PreflightOutcome(
            id = "network",
            label = "Network",
            value = if (networkReady) "Connected" else "Action needed",
            state = if (networkReady) PreflightOutcomeState.READY else PreflightOutcomeState.ACTION_REQUIRED,
            detail = if (networkReady) "A network is available for the next connection attempt" else "Connect to Wi-Fi or mobile data before going live",
        ),
        PreflightOutcome(
            id = "microphone",
            label = "Microphone",
            value = if (audioReady) "Permission granted" else "Permission needed",
            state = if (audioReady) PreflightOutcomeState.READY else PreflightOutcomeState.ACTION_REQUIRED,
            detail = if (audioReady) "Audio permission is available" else "Allow microphone access for a broadcast with audio",
        ),
        PreflightOutcome(
            id = "camera",
            label = "Camera",
            value = if (cameraReady) "Available" else "Screen mode ready",
            state = if (cameraReady) PreflightOutcomeState.READY else PreflightOutcomeState.CAUTION,
            detail = if (cameraReady) "Camera scenes can request the camera when needed" else "Camera permission is not available; screen capture remains supported",
        ),
        PreflightOutcome(
            id = "destination",
            label = "Destination",
            value = if (destinationReady) "Configured" else "Not configured",
            state = if (destinationReady) PreflightOutcomeState.READY else PreflightOutcomeState.ACTION_REQUIRED,
            detail = if (destinationReady) "A secure destination is ready for the next session" else "Add a YouTube, Twitch, Kick, or custom RTMP destination in Settings",
        ),
        PreflightOutcome(
            id = "quality",
            label = "Stream profile",
            value = "${quality.width}×${quality.height} • ${quality.fps} FPS",
            state = if (quality.fps >= 60 || quality.width * quality.height >= 2_073_600) PreflightOutcomeState.CAUTION else PreflightOutcomeState.READY,
            detail = if (quality.fps >= 60 || quality.width * quality.height >= 2_073_600) "High-load profile; lower FPS or resolution if the device becomes hot or unstable" else "Conservative mobile profile",
        ),
    )
}
