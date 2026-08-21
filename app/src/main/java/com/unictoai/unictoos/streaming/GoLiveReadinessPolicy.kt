package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.StreamQuality

/**
 * Fast UI-side readiness model for the Studio. The foreground service remains
 * the authoritative security and capture validator; this policy only explains
 * likely user actions before the request is sent.
 */
data class GoLiveReadinessCheck(
    val id: String,
    val label: String,
    val value: String,
    val ready: Boolean,
    val blocking: Boolean,
    val detail: String,
)

data class GoLiveReadiness(
    val checks: List<GoLiveReadinessCheck>,
) {
    val canStart: Boolean get() = checks.none { it.blocking && !it.ready }
    val blockingDetail: String? get() = checks.firstOrNull { it.blocking && !it.ready }?.detail
    val cautionDetail: String? get() = checks.firstOrNull { !it.blocking && !it.ready }?.detail
}

object GoLiveReadinessPolicy {
    fun evaluate(
        destinationReady: Boolean,
        captureMode: String,
        microphonePermission: Boolean,
        cameraPermission: Boolean,
        networkAvailable: Boolean,
        quality: StreamQuality,
    ): GoLiveReadiness {
        val captureReady = captureMode != "camera" || cameraPermission
        val captureLabel = if (captureMode == "camera") "Camera" else "Screen capture"
        val highLoad = quality.fps >= 60 || quality.width * quality.height >= 2_073_600
        return GoLiveReadiness(
            checks = listOf(
                GoLiveReadinessCheck(
                    id = "destination",
                    label = "Destination",
                    value = if (destinationReady) "Configured" else "Setup needed",
                    ready = destinationReady,
                    blocking = true,
                    detail = if (destinationReady) "A secure destination is ready" else "Add a destination in Settings before going live",
                ),
                GoLiveReadinessCheck(
                    id = "network",
                    label = "Network",
                    value = if (networkAvailable) "Available" else "Offline",
                    ready = networkAvailable,
                    blocking = true,
                    detail = if (networkAvailable) "A network is available for the connection" else "Connect to Wi-Fi or mobile data before going live",
                ),
                GoLiveReadinessCheck(
                    id = "microphone",
                    label = "Microphone",
                    value = if (microphonePermission) "Permission ready" else "Permission needed",
                    ready = microphonePermission,
                    blocking = true,
                    detail = if (microphonePermission) "Audio capture can be requested" else "Allow microphone access; Unictoos includes audio in every broadcast",
                ),
                GoLiveReadinessCheck(
                    id = "capture",
                    label = captureLabel,
                    value = if (captureReady) "Ready" else "Permission needed",
                    ready = captureReady,
                    blocking = true,
                    detail = if (captureReady) "$captureLabel is available" else "Allow camera access for the selected camera scene",
                ),
                GoLiveReadinessCheck(
                    id = "quality",
                    label = "Quality profile",
                    value = "${quality.width}×${quality.height} • ${quality.fps} FPS",
                    ready = !highLoad,
                    blocking = false,
                    detail = if (highLoad) "High-load profile; lower FPS or resolution if the device becomes hot or unstable" else "Conservative mobile profile selected",
                ),
            ),
        )
    }
}
