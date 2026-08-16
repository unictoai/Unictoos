package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.BuildConfig
import com.unictoai.unictoos.domain.StreamQuality

object SupportabilityExport {
    fun json(
        report: DeviceCompatibilityReport,
        quality: StreamQuality,
        sessionStatus: String,
        configuredDestinationCount: Int,
        diagnostics: List<StreamingDiagnostic>,
        generatedAtMillis: Long,
    ): String = buildString {
        append('{')
        field("schema", "unictoos-support-bundle-v1")
        append(',')
        field("appVersion", BuildConfig.VERSION_NAME)
        append(',')
        field("generatedAtMillis", generatedAtMillis)
        append(',')
        field("sessionStatus", sessionStatus)
        append(',')
        field("configuredDestinationCount", configuredDestinationCount)
        append(",\"streamProfile\":{")
        field("width", quality.width)
        append(',')
        field("height", quality.height)
        append(',')
        field("fps", quality.fps)
        append(',')
        field("bitrateKbps", quality.bitrate / 1_000)
        append(',')
        field("keyframeIntervalSeconds", quality.keyframeIntervalSeconds)
        append('}')
        append(",\"device\":{")
        field("manufacturer", report.manufacturer)
        append(',')
        field("model", report.model)
        append(',')
        field("sdkInt", report.sdkInt)
        append(",\"checks\":[")
        report.checks.forEachIndexed { index, check ->
            if (index > 0) append(',')
            append('{')
            field("id", check.id)
            append(',')
            field("label", check.label)
            append(',')
            field("value", check.value)
            append(',')
            field("level", check.level.name)
            append(',')
            field("detail", check.detail)
            append('}')
        }
        append("]}")
        append(",\"diagnostics\":[")
        diagnostics.forEachIndexed { index, diagnostic ->
            if (index > 0) append(',')
            append('{')
            field("elapsedRealtimeMs", diagnostic.elapsedRealtimeMs)
            append(',')
            field("sessionId", diagnostic.sessionId)
            append(',')
            field("generation", diagnostic.generation)
            append(',')
            field("event", diagnostic.event)
            append(',')
            field("detail", diagnostic.detail)
            append(',')
            field("destinationId", diagnostic.destinationId?.name)
            append(',')
            field("networkEpoch", diagnostic.networkEpoch)
            append('}')
        }
        append("]}")
    }

    private fun StringBuilder.field(name: String, value: Any?) {
        append(quote(name)).append(':')
        when (value) {
            null -> append("null")
            is Number, is Boolean -> append(value)
            else -> append(quote(value.toString()))
        }
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
