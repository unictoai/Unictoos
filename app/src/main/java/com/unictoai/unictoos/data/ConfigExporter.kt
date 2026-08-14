package com.unictoai.unictoos.data

import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.StreamDestination

object ConfigExporter {
    fun export(scenes: List<Scene>, destinations: List<StreamDestination>): String = buildString {
        append("{\"schema\":\"unictoos-config-v1\",\"scenes\":[")
        scenes.forEachIndexed { index, scene ->
            if (index > 0) append(',')
            append("{\"id\":").append(quote(scene.id))
                .append(",\"name\":").append(quote(scene.name))
                .append(",\"aspectRatio\":").append(quote(scene.aspectRatio.name))
                .append(",\"sources\":[")
            scene.sources.forEachIndexed { sourceIndex, source ->
                if (sourceIndex > 0) append(',')
                append("{\"id\":").append(quote(source.id))
                    .append(",\"name\":").append(quote(source.name))
                    .append(",\"type\":").append(quote(source.type.name))
                    .append(",\"enabled\":").append(source.enabled)
                    .append(",\"zIndex\":").append(source.zIndex)
                    .append(",\"opacity\":").append(source.opacity)
                    .append(",\"textContent\":").append(quote(source.textContent))
                    .append(",\"textColor\":").append(source.textColor)
                    .append(",\"textSizeSp\":").append(source.textSizeSp)
                    .append('}')
            }
            append("]}")
        }
        append("],\"destinations\":[")
        destinations.forEachIndexed { index, destination ->
            if (index > 0) append(',')
            append("{\"id\":").append(quote(destination.id))
                .append(",\"name\":").append(quote(destination.name))
                .append(",\"platform\":").append(quote(destination.platform.name))
                .append(",\"serverUrl\":").append(quote(destination.serverUrl))
                .append(",\"isConfigured\":").append(destination.isConfigured)
                .append(",\"streamKey\":null}")
        }
        append("]}")
    }

    private fun quote(value: String): String = buildString {
        append('"')
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
        append('"')
    }
}
