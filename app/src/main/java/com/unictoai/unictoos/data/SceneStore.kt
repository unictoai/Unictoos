package com.unictoai.unictoos.data

import android.content.Context
import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.Source
import com.unictoai.unictoos.domain.SourceType
import com.unictoai.unictoos.domain.SceneTransition
import com.unictoai.unictoos.domain.SceneTransitionMode
import com.unictoai.unictoos.domain.SourceGroup
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
        }.getOrElse {
            preferences.edit()
                .putString(KEY_CORRUPT_BACKUP, raw.take(MAX_CORRUPT_BACKUP_CHARS))
                .remove(KEY_SCENES)
                .apply()
            defaults
        }
    }

    override fun save(scenes: List<Scene>) {
        val json = JSONArray()
        scenes.forEach { scene ->
            json.put(JSONObject().apply {
                put("id", scene.id)
                put("name", scene.name)
                put("aspectRatio", scene.aspectRatio.name)
                put("transitionMode", scene.transition.mode.name)
                put("transitionDurationMs", scene.transition.safeDurationMs)
                put("sourceGroups", JSONArray().apply {
                    scene.sourceGroups.forEach { group ->
                        put(JSONObject().apply {
                            put("id", group.id)
                            put("name", group.name)
                            put("enabled", group.enabled)
                            put("sourceIds", JSONArray(group.sourceIds))
                        })
                    }
                })
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
                            source.groupId?.let { groupId -> put("groupId", groupId) }
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
            val transition = SceneTransition(
                mode = runCatching { SceneTransitionMode.valueOf(sceneJson.optString("transitionMode")) }.getOrDefault(SceneTransitionMode.CUT),
                durationMs = sceneJson.optLong("transitionDurationMs", SceneTransition.DEFAULT_DURATION_MS),
            )
            val groups = sceneJson.optJSONArray("sourceGroups")?.toSourceGroupList().orEmpty()
            val id = sceneJson.optString("id").ifBlank { "scene-$index" }
            val name = sceneJson.optString("name").ifBlank { "Scene" }
            add(Scene(id = id, name = name, aspectRatio = ratio, sources = sources, sourceGroups = groups, transition = transition))
        }
    }

    private fun JSONArray.toSourceGroupList(): List<SourceGroup> = buildList {
        for (index in 0 until length()) {
            val groupJson = optJSONObject(index) ?: continue
            val sourceIds = groupJson.optJSONArray("sourceIds")?.let { ids ->
                buildList { for (idIndex in 0 until ids.length()) ids.optString(idIndex).takeIf(String::isNotBlank)?.let(::add) }
            }.orEmpty()
            val id = groupJson.optString("id").ifBlank { "group-$index" }
            val name = groupJson.optString("name").ifBlank { "Group ${index + 1}" }
            add(SourceGroup(id = id, name = name, sourceIds = sourceIds, enabled = groupJson.optBoolean("enabled", true)))
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
                    x = sourceJson.optDouble("x", 0.05).toFloat().coerceIn(0f, 1f),
                    y = sourceJson.optDouble("y", 0.08).toFloat().coerceIn(0f, 1f),
                    width = sourceJson.optDouble("width", 0.90).toFloat().coerceIn(0.05f, 1f),
                    height = sourceJson.optDouble("height", 0.24).toFloat().coerceIn(0.05f, 1f),
                    fillColor = sourceJson.optLong("fillColor", 0xFF101216),
                    imageUri = sourceJson.optString("imageUri", ""),
                    groupId = sourceJson.optString("groupId", "").ifBlank { null },
                ),
            )
        }
    }

    private companion object {
        const val KEY_SCENES = "scenes_json_v1"
        const val KEY_CORRUPT_BACKUP = "scenes_corrupt_backup_v1"
        const val MAX_CORRUPT_BACKUP_CHARS = 512_000
    }
}
