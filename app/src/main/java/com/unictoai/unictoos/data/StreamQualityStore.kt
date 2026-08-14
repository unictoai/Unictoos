package com.unictoai.unictoos.data

import android.content.Context
import com.unictoai.unictoos.domain.StreamQuality
import com.unictoai.unictoos.domain.StreamQualityPreset

interface StreamQualityRepository {
    fun load(): StreamQuality
    fun save(quality: StreamQuality)
}

class StreamQualityStore(context: Context) : StreamQualityRepository {
    private val preferences = context.getSharedPreferences("unictoos_stream_quality", Context.MODE_PRIVATE)

    override fun load(): StreamQuality {
        val preset = runCatching {
            StreamQualityPreset.valueOf(preferences.getString(KEY_PRESET, StreamQualityPreset.BALANCED.name).orEmpty())
        }.getOrDefault(StreamQualityPreset.BALANCED)
        val defaults = preset.toQuality()
        return defaults.copy(
            width = preferences.getInt(KEY_WIDTH, defaults.width),
            height = preferences.getInt(KEY_HEIGHT, defaults.height),
            fps = preferences.getInt(KEY_FPS, defaults.fps),
            bitrate = preferences.getInt(KEY_BITRATE, defaults.bitrate),
            keyframeIntervalSeconds = preferences.getInt(KEY_KEYFRAME_INTERVAL, defaults.keyframeIntervalSeconds),
        ).validated()
    }

    override fun save(quality: StreamQuality) {
        val safe = quality.validated()
        preferences.edit()
            .putString(KEY_PRESET, safe.preset.name)
            .putInt(KEY_WIDTH, safe.width)
            .putInt(KEY_HEIGHT, safe.height)
            .putInt(KEY_FPS, safe.fps)
            .putInt(KEY_BITRATE, safe.bitrate)
            .putInt(KEY_KEYFRAME_INTERVAL, safe.keyframeIntervalSeconds)
            .apply()
    }

    private companion object {
        const val KEY_PRESET = "preset_v1"
        const val KEY_WIDTH = "width_v1"
        const val KEY_HEIGHT = "height_v1"
        const val KEY_FPS = "fps_v1"
        const val KEY_BITRATE = "bitrate_v1"
        const val KEY_KEYFRAME_INTERVAL = "keyframe_interval_v1"
    }
}
