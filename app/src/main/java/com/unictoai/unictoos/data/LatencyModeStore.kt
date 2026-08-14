package com.unictoai.unictoos.data

import android.content.Context
import com.unictoai.unictoos.domain.LatencyMode

interface LatencyModeRepository {
    fun load(): LatencyMode
    fun save(mode: LatencyMode)
}

class LatencyModeStore(context: Context) : LatencyModeRepository {
    private val preferences = context.getSharedPreferences("unictoos_latency", Context.MODE_PRIVATE)

    override fun load(): LatencyMode = runCatching {
        LatencyMode.valueOf(preferences.getString(KEY_MODE, LatencyMode.STABLE.name).orEmpty())
    }.getOrDefault(LatencyMode.STABLE)

    override fun save(mode: LatencyMode) {
        preferences.edit().putString(KEY_MODE, mode.name).apply()
    }

    private companion object {
        const val KEY_MODE = "mode_v1"
    }
}
