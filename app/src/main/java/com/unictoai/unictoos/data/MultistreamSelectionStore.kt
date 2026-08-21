package com.unictoai.unictoos.data

import android.content.Context
import com.unictoai.unictoos.domain.PlatformPreset

interface MultistreamSelectionRepository {
    fun load(): Set<PlatformPreset>
    fun save(platforms: Set<PlatformPreset>)
}

class MultistreamSelectionStore(context: Context) : MultistreamSelectionRepository {
    private val preferences = context.getSharedPreferences("unictoos_multistream", Context.MODE_PRIVATE)

    override fun load(): Set<PlatformPreset> = preferences.getStringSet(KEY_PLATFORMS, null)
        .orEmpty()
        .mapNotNull { value -> runCatching { PlatformPreset.valueOf(value) }.getOrNull() }
        .toSet()
        .ifEmpty { setOf(PlatformPreset.YOUTUBE) }

    override fun save(platforms: Set<PlatformPreset>) {
        preferences.edit()
            .putStringSet(KEY_PLATFORMS, platforms.map { it.name }.toSet())
            .apply()
    }

    private companion object {
        const val KEY_PLATFORMS = "selected_platforms_v1"
    }
}
