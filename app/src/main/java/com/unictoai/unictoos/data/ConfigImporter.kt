package com.unictoai.unictoos.data

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

    /**
     * Returns a unique ID for imported content: keeps the author's ID when it is
     * non-blank and unused so far, otherwise derives a suffixed variant. Imported
     * files are untrusted, so verbatim IDs can collide (e.g. the same file imported
     * twice, or a crafted file) and corrupt scene/source selection downstream.
     */
    private fun uniqueImportId(rawId: String, fallback: String, used: MutableSet<String>): String {
        val candidate = rawId.trim().ifBlank { fallback }
        if (used.add(candidate)) return candidate
        var suffix = 2
        while (!used.add("$candidate-$suffix")) suffix++
        return "$candidate-$suffix"
    }

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
        val usedSceneIds = mutableSetOf<String>()
        for (index in 0 until length()) {
            val sceneJson = optJSONObject(index) ?: continue
            val sourcesJson = sceneJson.optJSONArray("sources")
            if (sourcesJson != null && sourcesJson.length() > MAX_SOURCES_PER_SCENE) continue
            val ratio = runCatching { AspectRatio.valueOf(sceneJson.optString("aspectRatio")) }
                .getOrDefault(AspectRatio.PORTRAIT)
            val transition = SceneTransition(
                mode = runCatching { SceneTransitionMode.valueOf(sceneJson.optString("transitionMode")) }.getOrDefault(SceneTransitionMode.CUT),
                durationMs = sceneJson.optLong("transitionDurationMs", SceneTransition.DEFAULT_DURATION_MS)
                    .coerceIn(SceneTransition.MIN_DURATION_MS, SceneTransition.MAX_DURATION_MS),
            )
            val sceneId = uniqueImportId(sceneJson.optString("id"), "imported-scene-$index", usedSceneIds)

            // Sources first: enforce per-scene ID uniqueness and remember the raw ->
            // final mapping so group references below can be rewritten to match.
            val usedSourceIds = mutableSetOf<String>()
            val sourceRemap = mutableMapOf<String, String>()
            val rawGroupIds = mutableListOf<String?>()
            val sources = sourcesJson?.let { json ->
                buildList {
                    for (sourceIndex in 0 until json.length()) {
                        val sourceJson = json.optJSONObject(sourceIndex) ?: continue
                        val type = runCatching { SourceType.valueOf(sourceJson.optString("type")) }
                            .getOrDefault(SourceType.COLOR)
                        val rawId = sourceJson.optString("id").trim()
                        val finalId = uniqueImportId(rawId, "imported-source-$index-$sourceIndex", usedSourceIds)
                        if (rawId.isNotBlank()) sourceRemap[rawId] = finalId
                        rawGroupIds.add(sourceJson.optString("groupId", "").trim().takeIf { it.isNotBlank() })
                        add(
                            Source(
                                id = finalId,
                                name = sourceJson.optString("name").trim().ifBlank { type.label },
                                type = type,
                                enabled = sourceJson.optBoolean("enabled", true),
                                zIndex = sourceJson.optInt("zIndex", sourceIndex).coerceIn(-1000, 1000),
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
                                groupId = null,
                            ),
                        )
                    }
                }
            }.orEmpty()

            // Groups: unique IDs per scene; rewrite source references through the
            // remap and drop references to sources that do not exist.
            val usedGroupIds = mutableSetOf<String>()
            val groupRemap = mutableMapOf<String, String>()
            val groups = sceneJson.optJSONArray("sourceGroups")?.let { groupsJson ->
                buildList {
                    for (groupIndex in 0 until groupsJson.length()) {
                        val groupJson = groupsJson.optJSONObject(groupIndex) ?: continue
                        val rawGroupId = groupJson.optString("id").trim()
                        val finalGroupId = uniqueImportId(rawGroupId, "imported-group-$index-$groupIndex", usedGroupIds)
                        if (rawGroupId.isNotBlank()) groupRemap[rawGroupId] = finalGroupId
                        val sourceIds = groupJson.optJSONArray("sourceIds")?.let { ids ->
                            buildList {
                                for (idIndex in 0 until ids.length()) {
                                    val rawSourceId = ids.optString(idIndex).trim()
                                    if (rawSourceId.isNotBlank()) sourceRemap[rawSourceId]?.let(::add)
                                }
                            }.distinct().take(32)
                        }.orEmpty()
                        add(
                            SourceGroup(
                                id = finalGroupId,
                                name = groupJson.optString("name").trim().ifBlank { "Imported group ${groupIndex + 1}" },
                                sourceIds = sourceIds,
                                enabled = groupJson.optBoolean("enabled", true),
                            ),
                        )
                    }
                }
            }.orEmpty()

            // Resolve each source's group through the remap; unknown groups become ungrouped.
            val resolvedSources = sources.mapIndexed { sourceIndex, source ->
                source.copy(groupId = rawGroupIds.getOrNull(sourceIndex)?.let { groupRemap[it] })
            }

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
            val name = sceneJson.optString("name").trim().ifBlank { "Imported scene" }
            add(
                Scene(
                    id = sceneId,
                    name = name,
                    aspectRatio = ratio,
                    sources = resolvedSources,
                    sourceGroups = groups,
                    transition = transition,
                    pipConfig = pipConfig,
                    backgroundAudioMode = sceneJson.optBoolean("backgroundAudioMode", false),
                ),
            )
        }
    }
}
