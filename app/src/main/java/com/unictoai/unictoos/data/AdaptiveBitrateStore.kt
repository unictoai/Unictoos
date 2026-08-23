package com.unictoai.unictoos.data

import android.content.Context

interface AdaptiveBitrateRepository {
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)
}

class AdaptiveBitrateStore(context: Context) : AdaptiveBitrateRepository {
    private val preferences = context.getSharedPreferences("unictoos_adaptive_bitrate", Context.MODE_PRIVATE)

    override fun isEnabled(): Boolean = preferences.getBoolean(KEY_ENABLED, true)

    override fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    private companion object {
        const val KEY_ENABLED = "enabled_v1"
    }
}
