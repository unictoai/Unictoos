package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.PipConfig
import com.unictoai.unictoos.domain.PipPosition
import com.unictoai.unictoos.domain.PipSize
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.Source
import com.unictoai.unictoos.domain.SourceType
import org.json.JSONArray
import org.json.JSONObject

object ScenePayloadCodec {
    private const val CURRENT_SCHEMA_VERSION = 2
    private const val MAX_SOURCES = 64
    private const val MAX_ID_LENGTH = 120
    private const val MAX_NAME_LENGTH = 160

    fun encode(scene: Scene): String = JSONObject().apply {
        put("schemaVersion", CURRENT_SCHEMA_VERSION)
        put("id", scene.id.take(MAX_ID_LENGTH))
        put("name", scene.name.take(MAX_NAME_LENGTH))
        put("aspectRatio", scene.aspectRatio.name)
        put("backgroundAudioMode", scene.backgroundAudioMode)
        scene.pipConfig?.let { pip ->
            put("pipConfig", JSONObject().apply {
                put("enabled", pip.enabled)
                put("position", pip.position.name)
                put("size", pip.size.name)
                put("cornerRadiusDp", pip.safeCornerRadiusDp)
                put("borderWidthDp", pip.safeBorderWidthDp)
                put("dropShadow", pip.dropShadow)
            })
        }
        put("sources", JSONArray().apply {
            scene.sources.take(MAX_SOURCES).forEach { source ->
                put(JSONObject().apply {
                    put("id", source.id.take(MAX_ID_LENGTH))
                    put("name", source.name.take(MAX_NAME_LENGTH))
                    put("type", source.type.name)
                    put("enabled", source.enabled)
                    put("zIndex", source.zIndex)
                    put("opacity", source.opacity.toDouble())
                    put("textContent", source.textContent.take(2_000))
                    put("textColor", source.textColor)
                    put("textSizeSp", source.textSizeSp.toDouble())
                    put("x", source.x.toDouble())
                    put("y", source.y.toDouble())
                    put("width", source.width.toDouble())
                    put("height", source.height.toDouble())
                    put("fillColor", source.fillColor)
                    put("imageUri", source.imageUri.take(2_000))
                })
            }
        })
    }.toString()

    fun decode(raw: String?): Scene? = runCatching {
        if (raw.isNullOrBlank()) return null
        val json = JSONObject(raw)
        val sources = buildList {
            val array = json.optJSONArray("sources") ?: JSONArray()
            for (index in 0 until array.length().coerceAtMost(MAX_SOURCES)) {
                val item = array.optJSONObject(index) ?: continue
                val type = runCatching { SourceType.valueOf(item.optString("type")) }.getOrDefault(SourceType.COLOR)
                add(
                    Source(
                        id = item.optString("id").take(MAX_ID_LENGTH).ifBlank { "source-$index" },
                        name = item.optString("name").take(MAX_NAME_LENGTH).ifBlank { type.label },
                        type = type,
                        enabled = item.optBoolean("enabled", true),
                        zIndex = item.optInt("zIndex", index),
                        opacity = item.optDouble("opacity", 1.0).toFloat().coerceIn(0f, 1f),
                        textContent = item.optString("textContent", "").take(2_000),
                        textColor = item.optLong("textColor", 0xFFFFFFFF),
                        textSizeSp = item.optDouble("textSizeSp", 22.0).toFloat().coerceIn(10f, 72f),
                        x = item.optDouble("x", 0.05).toFloat().coerceIn(0f, 1f),
                        y = item.optDouble("y", 0.08).toFloat().coerceIn(0f, 1f),
                        width = item.optDouble("width", 0.90).toFloat().coerceIn(0.05f, 1f),
                        height = item.optDouble("height", 0.24).toFloat().coerceIn(0.05f, 1f),
                        fillColor = item.optLong("fillColor", 0xFF101216),
                        imageUri = item.optString("imageUri", "").take(2_000),
                    ),
                )
            }
        }
        val pipJson = json.optJSONObject("pipConfig")
        val pipConfig = pipJson?.let {
            PipConfig(
                enabled = it.optBoolean("enabled", false),
                position = runCatching { PipPosition.valueOf(it.optString("position")) }.getOrDefault(PipPosition.BOTTOM_RIGHT),
                size = runCatching { PipSize.valueOf(it.optString("size")) }.getOrDefault(PipSize.MEDIUM),
                cornerRadiusDp = it.optInt("cornerRadiusDp", 16).coerceIn(0, 64),
                borderWidthDp = it.optInt("borderWidthDp", 2).coerceIn(0, 8),
                dropShadow = it.optBoolean("dropShadow", true),
            )
        }
        Scene(
            id = json.optString("id", "scene").take(MAX_ID_LENGTH).ifBlank { "scene" },
            name = json.optString("name", "Scene").take(MAX_NAME_LENGTH).ifBlank { "Scene" },
            aspectRatio = runCatching { AspectRatio.valueOf(json.optString("aspectRatio")) }.getOrDefault(AspectRatio.PORTRAIT),
            sources = sources,
            pipConfig = pipConfig,
            backgroundAudioMode = json.optBoolean("backgroundAudioMode", false),
        )
    }.getOrNull()
}
