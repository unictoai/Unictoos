package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.Source
import com.unictoai.unictoos.domain.SourceType
import org.json.JSONArray
import org.json.JSONObject

object ScenePayloadCodec {
    fun encode(scene: Scene): String = JSONObject().apply {
        put("id", scene.id)
        put("name", scene.name)
        put("aspectRatio", scene.aspectRatio.name)
        put("sources", JSONArray().apply {
            scene.sources.forEach { source ->
                put(JSONObject().apply {
                    put("id", source.id)
                    put("name", source.name)
                    put("type", source.type.name)
                    put("enabled", source.enabled)
                    put("zIndex", source.zIndex)
                    put("opacity", source.opacity.toDouble())
                    put("textContent", source.textContent)
                    put("textColor", source.textColor)
                    put("textSizeSp", source.textSizeSp.toDouble())
                    put("x", source.x.toDouble())
                    put("y", source.y.toDouble())
                    put("width", source.width.toDouble())
                    put("height", source.height.toDouble())
                    put("fillColor", source.fillColor)
                    put("imageUri", source.imageUri)
                })
            }
        })
    }.toString()

    fun decode(raw: String?): Scene? = runCatching {
        if (raw.isNullOrBlank()) return null
        val json = JSONObject(raw)
        val sources = buildList {
            val array = json.optJSONArray("sources") ?: JSONArray()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val type = runCatching { SourceType.valueOf(item.optString("type")) }.getOrDefault(SourceType.COLOR)
                add(
                    Source(
                        id = item.optString("id").ifBlank { "source-$index" },
                        name = item.optString("name").ifBlank { type.label },
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
        Scene(
            id = json.optString("id", "scene"),
            name = json.optString("name", "Scene"),
            aspectRatio = runCatching { AspectRatio.valueOf(json.optString("aspectRatio")) }.getOrDefault(AspectRatio.PORTRAIT),
            sources = sources,
        )
    }.getOrNull()
}
