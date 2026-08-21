package com.unictoai.unictoos.integrations

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/** Cloud storage boundary. Implementations must provide authentication, encryption, and conflict handling. */
interface CloudBackupProvider {
    val providerId: String
    suspend fun upload(document: CloudBackupDocument): CloudBackupResult
    suspend fun downloadLatest(): CloudBackupResult
}

data class CloudBackupDocument(
    val schema: String,
    val payload: String,
    val createdAtMillis: Long,
)

sealed interface CloudBackupResult {
    data class Success(val document: CloudBackupDocument? = null) : CloudBackupResult
    data class RequiresAuthentication(val provider: String) : CloudBackupResult
    data class Failure(val message: String) : CloudBackupResult
}

/** Authenticated remote-control boundary. Commands must be paired and scoped to one device session. */
interface RemoteControlTransport : AutoCloseable {
    val transportId: String
    fun observeCommands(): Flow<RemoteControlCommand>
    suspend fun connect(pairing: RemotePairing): RemoteControlResult
    suspend fun publish(state: RemoteControlState): RemoteControlResult
    override fun close()
}

data class RemotePairing(
    val deviceId: String,
    val pairingCode: String,
    val endpoint: Uri,
)

data class RemoteControlState(
    val status: String,
    val muted: Boolean,
    val recording: Boolean,
    val elapsedSeconds: Long,
)

data class RemoteControlCommand(val name: String, val requestId: String)

sealed interface RemoteControlResult {
    data object Connected : RemoteControlResult
    data object Disconnected : RemoteControlResult
    data class RequiresPairing(val explanation: String) : RemoteControlResult
    data class Failure(val message: String) : RemoteControlResult
}

/** Relay/bonding boundary for SRTLA or RIST implementations backed by a compatible server. */
interface BondingRelayProvider {
    val protocol: BondingProtocol
    suspend fun open(request: BondingRelayRequest): BondingRelayResult
    suspend fun close(sessionId: String): BondingRelayResult
}

enum class BondingProtocol { SRTLA, RIST }

data class BondingRelayRequest(
    val sessionId: String,
    val relayEndpoint: Uri,
    val accessTokenRef: String,
)

sealed interface BondingRelayResult {
    data class Opened(val sessionId: String, val publishEndpoint: Uri) : BondingRelayResult
    data class RequiresRelay(val explanation: String) : BondingRelayResult
    data class Failure(val message: String) : BondingRelayResult
}

/** Hardware capture boundary for UVC webcams and HDMI capture devices. */
interface ExternalVideoInputProvider {
    fun enumerate(): List<ExternalVideoInput>
    suspend fun requestPermission(deviceId: String): ExternalVideoInputResult
    suspend fun open(deviceId: String, format: ExternalVideoFormat): ExternalVideoInputResult
    suspend fun close(deviceId: String): ExternalVideoInputResult
}

data class ExternalVideoInput(val deviceId: String, val label: String, val supportsAudio: Boolean)
data class ExternalVideoFormat(val width: Int, val height: Int, val fps: Int)

sealed interface ExternalVideoInputResult {
    data object Ready : ExternalVideoInputResult
    data class RequiresUsbPermission(val deviceId: String) : ExternalVideoInputResult
    data class Unsupported(val explanation: String) : ExternalVideoInputResult
    data class Failure(val message: String) : ExternalVideoInputResult
}
