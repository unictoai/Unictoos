package com.unictoai.unictoos.integrations

import com.unictoai.unictoos.domain.ChatMessage
import com.unictoai.unictoos.domain.CreatorEvent
import com.unictoai.unictoos.domain.IntegrationAccount
import com.unictoai.unictoos.domain.PlatformPreset
import com.unictoai.unictoos.domain.StreamDestination
import kotlinx.coroutines.flow.Flow

/**
 * Capability and action boundary for provider integrations.
 * Implementations must own OAuth/PKCE or a documented backend relay.
 * Stream keys are deliberately not part of this contract.
 */
interface PlatformIntegration {
    val platform: PlatformPreset
    val capabilities: IntegrationCapabilities

    fun observeAccount(): Flow<IntegrationAccount>
    fun observeChat(): Flow<List<ChatMessage>>
    fun observeEvents(): Flow<List<CreatorEvent>>

    suspend fun connect(): IntegrationResult
    suspend fun disconnect(): IntegrationResult
    suspend fun updateMetadata(request: StreamMetadataRequest): IntegrationResult
    suspend fun sendChat(message: String): IntegrationResult
    suspend fun moderate(request: ModerationRequest): IntegrationResult
    suspend fun createClip(): IntegrationResult
    suspend fun createMarker(label: String): IntegrationResult
}

data class IntegrationCapabilities(
    val readChat: Boolean = false,
    val sendChat: Boolean = false,
    val readEvents: Boolean = false,
    val moderation: Boolean = false,
    val metadata: Boolean = false,
    val scheduling: Boolean = false,
    val clips: Boolean = false,
    val markers: Boolean = false,
)

data class StreamMetadataRequest(
    val title: String,
    val category: String = "",
    val visibility: String = "public",
    val audience: String = "general",
)

data class ModerationRequest(
    val action: String,
    val targetUserId: String,
    val messageId: String? = null,
    val reason: String? = null,
)

sealed interface IntegrationResult {
    data object Success : IntegrationResult
    data class RequiresScope(val scope: String) : IntegrationResult
    data class RequiresBackend(val explanation: String) : IntegrationResult
    data class Failure(val message: String) : IntegrationResult
}

interface PlatformIntegrationRegistry {
    fun integrationFor(platform: PlatformPreset): PlatformIntegration
    fun all(): List<PlatformIntegration>
}

/** Local-safe placeholder used until real provider adapters are configured. */
class DisconnectedPlatformIntegration(
    override val platform: PlatformPreset,
) : PlatformIntegration {
    override val capabilities = IntegrationCapabilities()
    override fun observeAccount(): Flow<IntegrationAccount> = kotlinx.coroutines.flow.flowOf(IntegrationAccount(platform = platform))
    override fun observeChat(): Flow<List<ChatMessage>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override fun observeEvents(): Flow<List<CreatorEvent>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override suspend fun connect(): IntegrationResult = IntegrationResult.RequiresBackend("OAuth/PKCE provider boundary is not configured")
    override suspend fun disconnect(): IntegrationResult = IntegrationResult.Success
    override suspend fun updateMetadata(request: StreamMetadataRequest): IntegrationResult = IntegrationResult.RequiresScope("channel:manage")
    override suspend fun sendChat(message: String): IntegrationResult = IntegrationResult.RequiresScope("chat:write")
    override suspend fun moderate(request: ModerationRequest): IntegrationResult = IntegrationResult.RequiresScope("moderation:manage")
    override suspend fun createClip(): IntegrationResult = IntegrationResult.RequiresScope("clips:edit")
    override suspend fun createMarker(label: String): IntegrationResult = IntegrationResult.RequiresScope("channel:manage:broadcast")
}
