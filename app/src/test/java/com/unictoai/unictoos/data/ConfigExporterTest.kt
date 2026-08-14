package com.unictoai.unictoos.data

import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.PlatformPreset
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.Source
import com.unictoai.unictoos.domain.SourceType
import com.unictoai.unictoos.domain.StreamDestination
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigExporterTest {
    @Test
    fun exportContainsMetadataAndOmitsSecretValue() {
        val json = ConfigExporter.export(
            scenes = listOf(Scene("scene", "Show", listOf(Source("text", "Title", SourceType.TEXT, textContent = "Hello", x = 0.12f, y = 0.2f)), AspectRatio.PORTRAIT)),
            destinations = listOf(StreamDestination("youtube", "YouTube", PlatformPreset.YOUTUBE, "rtmps://example/live", "secret-key", true)),
        )
        assertTrue(json.contains("Hello"))
        assertTrue(json.contains("rtmps://example/live"))
        assertTrue(json.contains("\"x\":0.12"))
        assertTrue(json.contains("\"streamKey\":null"))
        assertFalse(json.contains("secret-key"))
    }
}
