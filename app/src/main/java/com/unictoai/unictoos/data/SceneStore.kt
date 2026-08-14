package com.unictoai.unictoos.data

import android.content.Context
import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.Source
import com.unictoai.unictoos.domain.SourceType
import org.json.JSONArray
import org.json.JSONObject

interface SceneRepository {
    fun loadOrDefault(defaults: List<Scene>): List<Scene>
    fun save(scenes: List<Scene>)
}

class SceneStore(context: Context) : SceneRepository {
    private val preferences = context.getSharedPreferences("unictoos_scenes", Context.MODE_PRIVATE)

    override fun loadOrDefault(defaults: List<Scene>): List<Scene> {
        val raw = preferences.getString(KEY_SCENES, null) ?: return defaults
        return runCatching {
            val scenes = JSONArray(raw).toSceneList()
            if (scenes.isEmpty()) defaults else scenes
        }.getOrElse { defaults }
    }

    override fun save(scenes: List<Scene>) {
        val json = JSONArray()
        scenes.forEach { scene ->
            json.put(JSONObject().apply {
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
                        })
                    }
                })
            })
        }
        preferences.edit().putString(KEY_SCENES, json.toString()).apply()
    }

    private fun JSONArray.toSceneList(): List<Scene> = buildList {
        for (index in 0 until length()) {
            val sceneJson = optJSONObject(index) ?: continue
            val sources = sceneJson.optJSONArray("sources")?.toSourceList().orEmpty()
            val ratio = runCatching { AspectRatio.valueOf(sceneJson.optString("aspectRatio")) }.getOrDefault(AspectRatio.PORTRAIT)
            val id = sceneJson.optString("id").ifBlank { "scene-$index" }
            val name = sceneJson.optString("name").ifBlank { "Scene" }
            add(Scene(id = id, name = name, aspectRatio = ratio, sources = sources))
        }
    }

    private fun JSONArray.toSourceList(): List<Source> = buildList {
        for (index in 0 until length()) {
            val sourceJson = optJSONObject(index) ?: continue
            val type = runCatching { SourceType.valueOf(sourceJson.optString("type")) }.getOrDefault(SourceType.COLOR)
            add(
                Source(
                    id = sourceJson.optString("id").ifBlank { "source-$index" },
                    name = sourceJson.optString("name").ifBlank { type.label },
                    type = type,
                    enabled = sourceJson.optBoolean("enabled", true),
                    zIndex = sourceJson.optInt("zIndex", index),
                    opacity = sourceJson.optDouble("opacity", 1.0).toFloat().coerceIn(0f, 1f),
                    textContent = sourceJson.optString("textContent", ""),
                    textColor = sourceJson.optLong("textColor", 0xFFFFFFFF),
                    textSizeSp = sourceJson.optDouble("textSizeSp", 22.0).toFloat().coerceIn(10f, 72f),
                ),
            )
        }
    }

    private companion object {
        const val KEY_SCENES = "scenes_json_v1"
    }
}
