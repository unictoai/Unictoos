package com.unictoai.unictoos.data

import android.content.Context
import com.unictoai.unictoos.domain.AutoStopDuration

interface AutoStopRepository {
    fun load(): AutoStopDuration
    fun save(duration: AutoStopDuration)
}

class AutoStopStore(context: Context) : AutoStopRepository {
    private val preferences = context.getSharedPreferences("unictoos_auto_stop", Context.MODE_PRIVATE)

    override fun load(): AutoStopDuration = runCatching {
        AutoStopDuration.valueOf(preferences.getString(KEY_DURATION, AutoStopDuration.OFF.name).orEmpty())
    }.getOrDefault(AutoStopDuration.OFF)

    override fun save(duration: AutoStopDuration) {
        preferences.edit().putString(KEY_DURATION, duration.name).apply()
    }

    private companion object {
        const val KEY_DURATION = "duration_v1"
    }
}
