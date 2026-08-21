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
}
