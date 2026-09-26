package com.unictoai.unictoos.data

import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.Source
import com.unictoai.unictoos.domain.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.json.JSONObject
import org.junit.Test

class ConfigImporterTest {
    @Test
    fun importsScenesAndIgnoresDestinationCredentials() {
        val raw = ConfigExporter.export(
            scenes = listOf(
                Scene(
                    id = "scene-1",
                    name = "Portrait",
                    aspectRatio = AspectRatio.PORTRAIT,
                    sources = listOf(Source("camera", "Camera", SourceType.CAMERA)),
                ),
            ),
            destinations = emptyList(),
        )

        assertEquals("unictoos-config-v1", JSONObject(raw).optString("schema"))
        val result = ConfigImporter.importScenes(raw)

        assertTrue(raw, result is ConfigImportResult.Success)
        val scene = (result as ConfigImportResult.Success).scenes.single()
        assertEquals("Portrait", scene.name)
        assertEquals(SourceType.CAMERA, scene.sources.single().type)
    }

    @Test
    fun rejectsUnsupportedSchema() {
        val result = ConfigImporter.importScenes("{\"schema\":\"future\",\"scenes\":[]}")
        assertTrue(result is ConfigImportResult.Rejected)
    }

    @Test
    fun rejectsMalformedJson() {
        val result = ConfigImporter.importScenes("not-json")
        assertTrue(result is ConfigImportResult.Rejected)
    }

    @Test
    fun rejectsOversizedInput() {
        val result = ConfigImporter.importScenes("x".repeat(512_001))
        assertTrue(result is ConfigImportResult.Rejected)
    }

    @Test
    fun restoresPresentationMetadataWithoutCredentials() {
        val raw = """
            {"schema":"unictoos-config-v1","scenes":[{"id":"s","name":"S","aspectRatio":"LANDSCAPE","transitionMode":"FADE","transitionDurationMs":900,"sourceGroups":[{"id":"g","name":"Host","enabled":true,"sourceIds":["camera","title"]}],"sources":[{"id":"camera","name":"Camera","type":"CAMERA","groupId":"g"},{"id":"title","name":"Title","type":"TEXT","groupId":"g"}]}],"destinations":[{"streamKey":"must-not-be-used"}]}
        """.trimIndent()
        val result = ConfigImporter.importScenes(raw)
        assertTrue(result.toString(), result is ConfigImportResult.Success)
        val scene = (result as ConfigImportResult.Success).scenes.single()
        assertEquals("FADE", scene.transition.mode.name)
        assertEquals(900L, scene.transition.safeDurationMs)
        assertEquals(listOf("camera", "title"), scene.sourceGroups.single().sourceIds)
        assertEquals("g", scene.sources.first().groupId)
    }

    @Test
    fun clampsImportedGeometryAndText() {
        val raw = """
            {"schema":"unictoos-config-v1","scenes":[{"id":"s","name":"S","aspectRatio":"LANDSCAPE","sources":[{"id":"t","name":"T","type":"TEXT","textContent":"${"x".repeat(2_200)}","opacity":4,"x":-2,"y":3,"width":0,"height":9}]}]}
        """.trimIndent()
        val result = ConfigImporter.importScenes(raw)
        assertTrue(result.toString(), result is ConfigImportResult.Success)
        val success = result as ConfigImportResult.Success
        val source = success.scenes.single().sources.single()
        assertEquals(2_000, source.textContent.length)
        assertEquals(1f, source.opacity)
        assertEquals(0f, source.x)
        assertEquals(1f, source.y)
        assertEquals(0.05f, source.width)
        assertEquals(1f, source.height)
    }

    @Test
    fun deduplicatesSceneIdsOnImport() {
        val raw = """
            {"schema":"unictoos-config-v1","scenes":[{"id":"dup","name":"First"},{"id":"dup","name":"Second"},{"id":"dup","name":"Third"}]}
        """.trimIndent()
        val result = ConfigImporter.importScenes(raw)
        assertTrue(result.toString(), result is ConfigImportResult.Success)
        val scenes = (result as ConfigImportResult.Success).scenes
        assertEquals(3, scenes.size)
        assertEquals(3, scenes.map { it.id }.distinct().size)
        assertEquals(listOf("First", "Second", "Third"), scenes.map { it.name })
    }

    @Test
    fun remapsDuplicateSourceIdsAndDropsUnknownGroupReferences() {
        val raw = """
            {"schema":"unictoos-config-v1","scenes":[{"id":"s","name":"S",
              "sources":[{"id":"a","name":"One","type":"TEXT","groupId":"g"},{"id":"a","name":"Two","type":"TEXT","groupId":"g"},{"id":"","name":"Three","type":"TEXT","groupId":"missing"}],
              "sourceGroups":[{"id":"g","name":"Group","sourceIds":["a","a","ghost"]}]}]}
        """.trimIndent()
        val result = ConfigImporter.importScenes(raw)
        assertTrue(result.toString(), result is ConfigImportResult.Success)
        val scene = (result as ConfigImportResult.Success).scenes.single()
        val sourceIds = scene.sources.map { it.id }
        // Every source ID is unique, even with duplicate/blank input IDs.
        assertEquals(sourceIds.size, sourceIds.distinct().size)
        val group = scene.sourceGroups.single()
        // Group references resolve to real (deduplicated) source IDs; unknown IDs are dropped.
        assertTrue(group.sourceIds.isNotEmpty())
        assertTrue(group.sourceIds.all { it in sourceIds })
        assertTrue("ghost" !in group.sourceIds)
        // Sources keep their group membership through the remap; unknown groups clear it.
        assertEquals(group.id, scene.sources[0].groupId)
        assertEquals(group.id, scene.sources[1].groupId)
        assertEquals(null, scene.sources[2].groupId)
    }

    @Test
    fun coercesTransitionDurationOnImport() {
        val raw = """
            {"schema":"unictoos-config-v1","scenes":[
              {"id":"neg","name":"N","transitionDurationMs":-5},
              {"id":"huge","name":"H","transitionDurationMs":9000000000},
              {"id":"ok","name":"O","transitionDurationMs":900}]}
        """.trimIndent()
        val result = ConfigImporter.importScenes(raw)
        assertTrue(result.toString(), result is ConfigImportResult.Success)
        val byId = (result as ConfigImportResult.Success).scenes.associateBy { it.id }
        assertEquals(0L, byId.getValue("neg").transition.durationMs)
        assertEquals(1_500L, byId.getValue("huge").transition.durationMs)
        assertEquals(900L, byId.getValue("ok").transition.durationMs)
    }
}
