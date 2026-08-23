package com.unictoai.unictoos.data

import android.content.Context
import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.PipConfig
import com.unictoai.unictoos.domain.PipPosition
import com.unictoai.unictoos.domain.PipSize
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
        scenes.take(MAX_SCENES).forEach { scene ->
            json.put(JSONObject().apply {
                put("id", scene.id)
                put("name", scene.name)
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
                put("transitionMode", scene.transition.mode.name)
                put("transitionDurationMs", scene.transition.safeDurationMs)
                put("sourceGroups", JSONArray().apply {
                    scene.sourceGroups.take(MAX_GROUPS).forEach { group ->
                        put(JSONObject().apply {
                            put("id", group.id)
                            put("name", group.name)
                            put("enabled", group.enabled)
                            put("sourceIds", JSONArray(group.sourceIds))
                        })
                    }
                })
                put("sources", JSONArray().apply {
                    scene.sources.take(MAX_SOURCES_PER_SCENE).forEach { source ->
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
        for (index in 0 until minOf(length(), MAX_SCENES)) {
            val sceneJson = optJSONObject(index) ?: continue
            val sources = sceneJson.optJSONArray("sources")?.toSourceList(MAX_SOURCES_PER_SCENE).orEmpty()
            val ratio = runCatching { AspectRatio.valueOf(sceneJson.optString("aspectRatio")) }.getOrDefault(AspectRatio.PORTRAIT)
            val transition = SceneTransition(
                mode = runCatching { SceneTransitionMode.valueOf(sceneJson.optString("transitionMode")) }.getOrDefault(SceneTransitionMode.CUT),
                durationMs = sceneJson.optLong("transitionDurationMs", SceneTransition.DEFAULT_DURATION_MS),
            )
            val groups = sceneJson.optJSONArray("sourceGroups")?.toSourceGroupList().orEmpty()
            val pipJson = sceneJson.optJSONObject("pipConfig")
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
            val id = sceneJson.optString("id").take(MAX_ID_CHARS).ifBlank { "scene-$index" }
            val name = sceneJson.optString("name").take(MAX_NAME_CHARS).ifBlank { "Scene" }
            add(
                Scene(
                    id = id,
                    name = name,
                    aspectRatio = ratio,
                    sources = sources,
                    sourceGroups = groups,
                    transition = transition,
                    pipConfig = pipConfig,
                    backgroundAudioMode = sceneJson.optBoolean("backgroundAudioMode", false),
                ),
            )
        }
    }

    private fun JSONArray.toSourceGroupList(): List<SourceGroup> = buildList {
        for (index in 0 until minOf(length(), MAX_GROUPS)) {
            val groupJson = optJSONObject(index) ?: continue
            val sourceIds = groupJson.optJSONArray("sourceIds")?.let { ids ->
                buildList { for (idIndex in 0 until minOf(ids.length(), MAX_SOURCES_PER_SCENE)) ids.optString(idIndex).take(MAX_ID_CHARS).takeIf(String::isNotBlank)?.let(::add) }
            }.orEmpty()
            val id = groupJson.optString("id").take(MAX_ID_CHARS).ifBlank { "group-$index" }
            val name = groupJson.optString("name").take(MAX_NAME_CHARS).ifBlank { "Group ${index + 1}" }
            add(SourceGroup(id = id, name = name, sourceIds = sourceIds, enabled = groupJson.optBoolean("enabled", true)))
        }
    }

    private fun JSONArray.toSourceList(maxSources: Int = MAX_SOURCES_PER_SCENE): List<Source> = buildList {
        for (index in 0 until minOf(length(), maxSources)) {
            val sourceJson = optJSONObject(index) ?: continue
            val type = runCatching { SourceType.valueOf(sourceJson.optString("type")) }.getOrDefault(SourceType.COLOR)
            add(
                Source(
                    id = sourceJson.optString("id").take(MAX_ID_CHARS).ifBlank { "source-$index" },
                    name = sourceJson.optString("name").take(MAX_NAME_CHARS).ifBlank { type.label },
                    type = type,
                    enabled = sourceJson.optBoolean("enabled", true),
                    zIndex = sourceJson.optInt("zIndex", index),
                    opacity = sourceJson.optDouble("opacity", 1.0).toFloat().coerceIn(0f, 1f),
                    textContent = sourceJson.optString("textContent", "").take(MAX_TEXT_CHARS),
                    textColor = sourceJson.optLong("textColor", 0xFFFFFFFF),
                    textSizeSp = sourceJson.optDouble("textSizeSp", 22.0).toFloat().coerceIn(10f, 72f),
                    x = sourceJson.optDouble("x", 0.05).toFloat().coerceIn(0f, 1f),
                    y = sourceJson.optDouble("y", 0.08).toFloat().coerceIn(0f, 1f),
                    width = sourceJson.optDouble("width", 0.90).toFloat().coerceIn(0.05f, 1f),
                    height = sourceJson.optDouble("height", 0.24).toFloat().coerceIn(0.05f, 1f),
                    fillColor = sourceJson.optLong("fillColor", 0xFF101216),
                    imageUri = sourceJson.optString("imageUri", "").take(MAX_URI_CHARS),
                    groupId = sourceJson.optString("groupId", "").take(MAX_ID_CHARS).ifBlank { null },
                ),
            )
        }
    }

    private companion object {
        const val KEY_SCENES = "scenes_json_v1"
        const val KEY_CORRUPT_BACKUP = "scenes_corrupt_backup_v1"
        const val MAX_CORRUPT_BACKUP_CHARS = 512_000
        const val MAX_SCENES = 64
        const val MAX_GROUPS = 16
        const val MAX_SOURCES_PER_SCENE = 32
        const val MAX_ID_CHARS = 96
        const val MAX_NAME_CHARS = 128
        const val MAX_TEXT_CHARS = 2_000
        const val MAX_URI_CHARS = 2_000
    }
}
