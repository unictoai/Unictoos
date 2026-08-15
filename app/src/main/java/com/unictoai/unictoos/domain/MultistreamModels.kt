package com.unictoai.unictoos.domain

import androidx.compose.runtime.Immutable

/** Stable identifiers for destinations supported by the future multistream session layer. */
enum class DestinationId(val label: String) {
    YOUTUBE("YouTube"),
    TWITCH("Twitch"),
    KICK("Kick"),
    CUSTOM("Custom RTMP"),
}

enum class TransportProtocol {
    RTMP,
    RTMPS,
}

/** Per-destination state. It deliberately contains no endpoint or plaintext credential. */
enum class DestinationState {
    DISABLED,
    READY,
    CONNECTING,
    LIVE,
    RECONNECTING,
    DEGRADED,
    AUTH_ERROR,
    SERVER_ERROR,
    NETWORK_ERROR,
    STOPPING,
    STOPPED,
}

enum class AggregateStreamState {
    IDLE,
    PREPARING,
    CONNECTING,
    LIVE,
    PARTIAL_LIVE,
    RECONNECTING,
    PARTIAL_FAILURE,
    STOPPING,
    STOPPED,
    ERROR,
}

/**
 * Provider-neutral encoding and transport constraints.
 *
 * Nullable limits mean that the platform profile still needs to be resolved from
 * current provider guidance before preflight enforces a numeric ceiling.
 */
@Immutable
data class DestinationProfile(
    val id: DestinationId,
    val platform: PlatformPreset,
    val supportedProtocols: Set<TransportProtocol>,
    val maximumWidth: Int? = null,
    val maximumHeight: Int? = null,
    val maximumFps: Int? = null,
    val maximumVideoBitrateKbps: Int? = null,
    val recommendedVideoBitrateKbps: Int? = null,
    val requiresConstantBitrate: Boolean = true,
    val keyframeIntervalSeconds: Int? = 2,
)

/**
 * Destination connection state for a single multistream session.
 *
 * `credentialRef` is an opaque reference only. CredentialStore encryption and
 * plaintext credential handling remain outside this Stage A model.
 */
@Immutable
data class DestinationSession(
    val id: DestinationId,
    val profile: DestinationProfile,
    val state: DestinationState = DestinationState.DISABLED,
    val configured: Boolean = false,
    val credentialRef: String? = null,
    val connectionGeneration: Long = 0L,
    val reconnectAttempt: Int = 0,
    val bitrateKbps: Int = 0,
    val droppedVideoFrames: Long = -1L,
    val droppedAudioFrames: Long = -1L,
    val networkEpoch: Long = 0L,
    val lastError: String? = null,
)

/** Aggregate state contract for a future simultaneous multi-destination session. */
@Immutable
data class MultistreamSessionState(
    val mode: SessionMode = SessionMode.BROADCAST,
    val aggregateState: AggregateStreamState = AggregateStreamState.IDLE,
    val destinations: List<DestinationSession> = emptyList(),
    val networkEpoch: Long = 0L,
    val networkLabel: String = "Unknown",
    val elapsedSeconds: Long = 0L,
    val recording: Boolean = false,
    val message: String? = null,
)

/** Product safety defaults; these are contracts only until a future manager consumes them. */
object MultistreamDefaults {
    const val DIRECT_DESTINATION_CAP = 2
    const val THREE_DESTINATION_DEVICE_GATE = "Infinix X6853"
}

/** Initial provider profile metadata for Stage A. Runtime preflight is intentionally not wired yet. */
object DestinationProfiles {
    val youtube = DestinationProfile(
        id = DestinationId.YOUTUBE,
        platform = PlatformPreset.YOUTUBE,
        supportedProtocols = setOf(TransportProtocol.RTMPS),
    )

    val twitch = DestinationProfile(
        id = DestinationId.TWITCH,
        platform = PlatformPreset.TWITCH,
        supportedProtocols = setOf(TransportProtocol.RTMP, TransportProtocol.RTMPS),
    )

    val kick = DestinationProfile(
        id = DestinationId.KICK,
        platform = PlatformPreset.KICK,
        supportedProtocols = setOf(TransportProtocol.RTMP, TransportProtocol.RTMPS),
        maximumWidth = 1_920,
        maximumHeight = 1_080,
        maximumFps = 60,
        maximumVideoBitrateKbps = 8_000,
    )

    fun forId(id: DestinationId): DestinationProfile = when (id) {
        DestinationId.YOUTUBE -> youtube
        DestinationId.TWITCH -> twitch
        DestinationId.KICK -> kick
        DestinationId.CUSTOM -> DestinationProfile(
            id = DestinationId.CUSTOM,
            platform = PlatformPreset.CUSTOM,
            supportedProtocols = setOf(TransportProtocol.RTMP, TransportProtocol.RTMPS),
        )
    }
}
