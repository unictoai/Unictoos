package com.unictoai.unictoos.domain

import androidx.compose.runtime.Immutable

/** Describes what the current Android client actually supports for a provider. */
enum class IntegrationReadiness {
    STREAM_KEY_READY,
    MANUAL_CONFIGURATION,
    REQUIRES_BACKEND,
}

@Immutable
data class PlatformCapabilities(
    val platform: PlatformPreset,
    val streamingReadiness: IntegrationReadiness,
    val oauthReadiness: IntegrationReadiness,
    val chatReadiness: IntegrationReadiness,
    val moderationReadiness: IntegrationReadiness,
    val eventReadiness: IntegrationReadiness,
    val supportsStreamMarkers: Boolean,
)

object PlatformCapabilityCatalog {
    fun forPlatform(platform: PlatformPreset): PlatformCapabilities = when (platform) {
        PlatformPreset.CUSTOM -> PlatformCapabilities(
            platform = platform,
            streamingReadiness = IntegrationReadiness.MANUAL_CONFIGURATION,
            oauthReadiness = IntegrationReadiness.REQUIRES_BACKEND,
            chatReadiness = IntegrationReadiness.REQUIRES_BACKEND,
            moderationReadiness = IntegrationReadiness.REQUIRES_BACKEND,
            eventReadiness = IntegrationReadiness.REQUIRES_BACKEND,
            supportsStreamMarkers = false,
        )
        else -> PlatformCapabilities(
            platform = platform,
            streamingReadiness = IntegrationReadiness.STREAM_KEY_READY,
            oauthReadiness = IntegrationReadiness.REQUIRES_BACKEND,
            chatReadiness = IntegrationReadiness.REQUIRES_BACKEND,
            moderationReadiness = IntegrationReadiness.REQUIRES_BACKEND,
            eventReadiness = IntegrationReadiness.REQUIRES_BACKEND,
            supportsStreamMarkers = false,
        )
    }
}
