package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.Source
import com.unictoai.unictoos.domain.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenePayloadCodecInstrumentedTest {
    @Test
    fun encodedSceneCarriesSchemaVersionAndDecodes() {
        val scene = Scene(
            id = "scene-1",
            name = "Launch",
            sources = listOf(Source("title", "Title", SourceType.TEXT, textContent = "Hello")),
        )

        val encoded = ScenePayloadCodec.encode(scene)
        val decoded = ScenePayloadCodec.decode(encoded)

        assertTrue(encoded.contains("schemaVersion"))
        assertEquals(scene, decoded)
    }

    @Test
    fun legacySceneWithoutSchemaVersionStillDecodesSafely() {
        val legacy = """
            {"id":"legacy","name":"Legacy","aspectRatio":"LANDSCAPE","sources":[{"type":"TEXT","textContent":"Old"},{"type":"UNKNOWN"},null]}
        """.trimIndent()

        val decoded = ScenePayloadCodec.decode(legacy)

        assertEquals("legacy", decoded?.id)
        assertEquals(AspectRatio.LANDSCAPE, decoded?.aspectRatio)
        assertEquals(2, decoded?.sources?.size)
        assertEquals(SourceType.COLOR, decoded?.sources?.last()?.type)
    }

    @Test
    fun malformedSceneDoesNotThrowAndUsesNullResult() {
        assertEquals(null, ScenePayloadCodec.decode("not-json"))
        assertEquals(null, ScenePayloadCodec.decode(null))
    }
}
