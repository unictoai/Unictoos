package com.unictoai.unictoos.data

import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.Source
import com.unictoai.unictoos.domain.SourceType
import org.json.JSONArray
import org.json.JSONObject

sealed interface ConfigImportResult {
    data class Success(val scenes: List<Scene>) : ConfigImportResult
    data class Rejected(val reason: String) : ConfigImportResult
}

/** Imports scene metadata only. Destination credentials are intentionally ignored. */
object ConfigImporter {
    private const val SCHEMA = "unictoos-config-v1"
    private const val MAX_JSON_CHARS = 512_000
    private const val MAX_SCENES = 64
    private const val MAX_SOURCES_PER_SCENE = 32

    fun importScenes(raw: String): ConfigImportResult {
        if (raw.length > MAX_JSON_CHARS) return ConfigImportResult.Rejected("Configuration file is too large")
        return runCatching {
            val root = JSONObject(raw)
            if (root.optString("schema") != SCHEMA) {
                return ConfigImportResult.Rejected("Unsupported Unictoos configuration version")
            }
            val sceneArray = root.optJSONArray("scenes")
                ?: return ConfigImportResult.Rejected("Configuration does not contain scenes")
            if (sceneArray.length() > MAX_SCENES) {
                return ConfigImportResult.Rejected("Configuration contains too many scenes")
            }
            val scenes = sceneArray.toSceneList()
            if (scenes.isEmpty()) ConfigImportResult.Rejected("Configuration contains no usable scenes")
            else ConfigImportResult.Success(scenes)
        }.getOrElse { ConfigImportResult.Rejected("Configuration file could not be read") }
    }

    private fun JSONArray.toSceneList(): List<Scene> = buildList {
        for (index in 0 until length()) {
            val sceneJson = optJSONObject(index) ?: continue
            val sourcesJson = sceneJson.optJSONArray("sources")
            if (sourcesJson != null && sourcesJson.length() > MAX_SOURCES_PER_SCENE) continue
            val sources = sourcesJson?.toSourceList().orEmpty()
            val ratio = runCatching { AspectRatio.valueOf(sceneJson.optString("aspectRatio")) }
                .getOrDefault(AspectRatio.PORTRAIT)
            val id = sceneJson.optString("id").trim().ifBlank { "imported-scene-$index" }
            val name = sceneJson.optString("name").trim().ifBlank { "Imported scene" }
            add(Scene(id = id, name = name, aspectRatio = ratio, sources = sources))
        }
    }

    private fun JSONArray.toSourceList(): List<Source> = buildList {
        for (index in 0 until length()) {
            val sourceJson = optJSONObject(index) ?: continue
            val type = runCatching { SourceType.valueOf(sourceJson.optString("type")) }
                .getOrDefault(SourceType.COLOR)
            add(
                Source(
                    id = sourceJson.optString("id").trim().ifBlank { "imported-source-$index" },
                    name = sourceJson.optString("name").trim().ifBlank { type.label },
                    type = type,
                    enabled = sourceJson.optBoolean("enabled", true),
                    zIndex = sourceJson.optInt("zIndex", index).coerceIn(-1000, 1000),
                    opacity = sourceJson.optDouble("opacity", 1.0).toFloat().coerceIn(0f, 1f),
                    textContent = sourceJson.optString("textContent", "").take(2_000),
                    textColor = sourceJson.optLong("textColor", 0xFFFFFFFF),
                    textSizeSp = sourceJson.optDouble("textSizeSp", 22.0).toFloat().coerceIn(10f, 72f),
                    x = sourceJson.optDouble("x", 0.05).toFloat().coerceIn(0f, 1f),
                    y = sourceJson.optDouble("y", 0.08).toFloat().coerceIn(0f, 1f),
                    width = sourceJson.optDouble("width", 0.90).toFloat().coerceIn(0.05f, 1f),
                    height = sourceJson.optDouble("height", 0.24).toFloat().coerceIn(0.05f, 1f),
                    fillColor = sourceJson.optLong("fillColor", 0xFF101216),
                    imageUri = sourceJson.optString("imageUri", "").take(2_000),
                ),
            )
        }
    }
}
