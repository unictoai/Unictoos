package com.unictoai.unictoos.integrations

import com.unictoai.unictoos.domain.PlatformPreset

class DefaultPlatformIntegrationRegistry : PlatformIntegrationRegistry {
    private val integrations = PlatformPreset.values().map { DisconnectedPlatformIntegration(it) }
    private val byPlatform = integrations.associateBy { it.platform }

    override fun integrationFor(platform: PlatformPreset): PlatformIntegration =
        byPlatform[platform] ?: DisconnectedPlatformIntegration(platform)

    override fun all(): List<PlatformIntegration> = integrations
}
