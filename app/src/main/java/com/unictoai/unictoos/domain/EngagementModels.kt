package com.unictoai.unictoos.domain

import androidx.compose.runtime.Immutable

@Immutable
data class IntegrationAccount(
    val platform: PlatformPreset,
    val displayName: String = "",
    val connected: Boolean = false,
    val scopes: Set<String> = emptySet(),
)

@Immutable
data class ChatMessage(
    val id: String,
    val platform: PlatformPreset,
    val author: String,
    val text: String,
    val timestampMillis: Long,
    val isPinned: Boolean = false,
)

@Immutable
data class CreatorEvent(
    val id: String,
    val platform: PlatformPreset,
    val kind: String,
    val summary: String,
    val timestampMillis: Long,
)

enum class ModerationAction {
    DELETE_MESSAGE,
    TIMEOUT_USER,
    BAN_USER,
    APPROVE_AUTOMOD,
    DENY_AUTOMOD,
}
