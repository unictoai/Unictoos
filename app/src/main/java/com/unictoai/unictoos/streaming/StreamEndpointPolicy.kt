package com.unictoai.unictoos.streaming

import java.net.URI

enum class StreamTransport {
    RTMP,
    SRT,
}

object StreamEndpointPolicy {
    fun transport(endpoint: String): StreamTransport? = when {
        endpoint.trim().startsWith("srt://", ignoreCase = true) -> StreamTransport.SRT
        endpoint.trim().startsWith("rtmp://", ignoreCase = true) || endpoint.trim().startsWith("rtmps://", ignoreCase = true) -> StreamTransport.RTMP
        else -> null
    }

    fun isSupported(endpoint: String): Boolean {
        val value = endpoint.trim()
        if (transport(value) == null || value.any(Char::isWhitespace)) return false
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        return !uri.host.isNullOrBlank() && uri.port >= -1
    }

    fun validationMessage(endpoint: String): String {
        val value = endpoint.trim()
        return when {
            value.isBlank() -> "Configure a streaming destination first"
            transport(value) == null -> "The destination must use an RTMP, RTMPS, or SRT server URL"
            !isSupported(value) -> "Enter a complete destination URL with a valid server host"
            value.startsWith("srt://", ignoreCase = true) -> "SRT endpoints are supported for compatible SRT listeners; platform dashboards normally provide RTMP or RTMPS ingest URLs"
            else -> "The destination URL is ready"
        }
    }
}
