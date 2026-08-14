package com.unictoai.unictoos.data

import android.content.Context

interface ThermalProtectionRepository {
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)
}

class ThermalProtectionStore(context: Context) : ThermalProtectionRepository {
    private val preferences = context.getSharedPreferences("unictoos_thermal", Context.MODE_PRIVATE)

    override fun isEnabled(): Boolean = preferences.getBoolean(KEY_ENABLED, true)

    override fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    private companion object {
        const val KEY_ENABLED = "automatic_protection_enabled_v1"
    }
}
