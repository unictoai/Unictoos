package com.unictoai.unictoos.domain

import androidx.compose.runtime.Immutable

@Immutable
data class FeatureCapability(
    val name: String,
    val status: CapabilityStatus,
    val detail: String,
)

enum class CapabilityStatus(val label: String) {
    AVAILABLE("Available locally"),
    DEVICE_VALIDATION("Needs device validation"),
    INTEGRATION_READY("Integration-ready"),
    SERVICE_REQUIRED("External service required"),
}

object FeatureCapabilityCatalog {
    fun all(): List<FeatureCapability> = listOf(
        FeatureCapability("Screen and camera capture", CapabilityStatus.AVAILABLE, "MediaProjection, Camera2, microphone, and local recording are wired in the foreground service."),
        FeatureCapability("Scenes, templates, groups, and transitions", CapabilityStatus.AVAILABLE, "Scene metadata is persisted locally; live compositing remains intentionally bounded."),
        FeatureCapability("Two-destination direct fan-out", CapabilityStatus.DEVICE_VALIDATION, "Uses one shared RootEncoder pipeline and needs real-device network validation."),
        FeatureCapability("SRT listener publishing", CapabilityStatus.DEVICE_VALIDATION, "RootEncoder routing is present; a compatible SRT listener is still required for validation."),
        FeatureCapability("PiP compositor", CapabilityStatus.INTEGRATION_READY, "Requires a shared-surface compositor before simultaneous screen and camera composition is enabled."),
        FeatureCapability("Recording trim and multi-track export", CapabilityStatus.INTEGRATION_READY, "Local edit and export contracts exist; no unverified media remuxer is bundled."),
        FeatureCapability("Advanced audio DSP", CapabilityStatus.INTEGRATION_READY, "Noise gate, compressor, limiter, and EQ validation contracts exist; RootEncoder remains the active capture path."),
        FeatureCapability("Chat, events, moderation, and metadata", CapabilityStatus.SERVICE_REQUIRED, "Platform OAuth scopes and provider adapters are required."),
        FeatureCapability("Cloud backup, remote control, and bonding", CapabilityStatus.SERVICE_REQUIRED, "A user-controlled backend, relay, or companion transport is required."),
        FeatureCapability("UVC and HDMI capture", CapabilityStatus.DEVICE_VALIDATION, "USB host permissions and physical capture hardware testing are required."),
    )
}
