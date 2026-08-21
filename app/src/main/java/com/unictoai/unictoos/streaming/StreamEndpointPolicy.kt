package com.unictoai.unictoos.streaming

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

    fun isSupported(endpoint: String): Boolean = transport(endpoint) != null

    fun validationMessage(endpoint: String): String = if (endpoint.trim().startsWith("srt://", ignoreCase = true)) {
        "SRT endpoints are supported for compatible SRT listeners; platform dashboards normally provide RTMP or RTMPS ingest URLs"
    } else {
        "The destination must use an RTMP, RTMPS, or SRT server URL"
    }
}
