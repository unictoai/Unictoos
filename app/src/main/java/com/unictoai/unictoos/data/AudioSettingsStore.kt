package com.unictoai.unictoos.data

import android.content.Context
import com.unictoai.unictoos.domain.AudioQuality
import com.unictoai.unictoos.domain.AudioSettings

interface AudioSettingsRepository {
    fun load(): AudioSettings
    fun save(settings: AudioSettings)
}

class AudioSettingsStore(context: Context) : AudioSettingsRepository {
    private val preferences = context.getSharedPreferences("unictoos_audio_settings", Context.MODE_PRIVATE)

    override fun load(): AudioSettings {
        val quality = runCatching {
            AudioQuality.valueOf(preferences.getString(KEY_QUALITY, AudioQuality.STANDARD.name).orEmpty())
        }.getOrDefault(AudioQuality.STANDARD)
        return AudioSettings(
            quality = quality,
            sampleRate = preferences.getInt(KEY_SAMPLE_RATE, 44_100).coerceIn(16_000, 48_000),
            echoCanceler = preferences.getBoolean(KEY_ECHO, true),
            noiseSuppressor = preferences.getBoolean(KEY_NOISE, true),
        )
    }

    override fun save(settings: AudioSettings) {
        preferences.edit()
            .putString(KEY_QUALITY, settings.quality.name)
            .putInt(KEY_SAMPLE_RATE, settings.sampleRate.coerceIn(16_000, 48_000))
            .putBoolean(KEY_ECHO, settings.echoCanceler)
            .putBoolean(KEY_NOISE, settings.noiseSuppressor)
            .apply()
    }

    private companion object {
        const val KEY_QUALITY = "quality_v1"
        const val KEY_SAMPLE_RATE = "sample_rate_v1"
        const val KEY_ECHO = "echo_canceler_v1"
        const val KEY_NOISE = "noise_suppressor_v1"
    }
}
