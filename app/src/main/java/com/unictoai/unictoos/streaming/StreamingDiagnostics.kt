package com.unictoai.unictoos.streaming

import java.util.ArrayDeque

/** Process-local, bounded diagnostics for troubleshooting without storing stream credentials. */
data class StreamingDiagnostic(
    val elapsedRealtimeMs: Long,
    val sessionId: String,
    val generation: Long,
    val event: String,
    val detail: String,
)

object StreamingDiagnostics {
    private const val MAX_EVENTS = 200
    private const val MAX_DETAIL_CHARS = 240
    private val events = ArrayDeque<StreamingDiagnostic>(MAX_EVENTS)

    @Synchronized
    fun record(sessionId: String, generation: Long, event: String, detail: String = "", elapsedRealtimeMs: Long = android.os.SystemClock.elapsedRealtime()) {
        events.addLast(
            StreamingDiagnostic(
                elapsedRealtimeMs = elapsedRealtimeMs,
                sessionId = sessionId.take(64),
                generation = generation,
                event = event.take(64),
                detail = redact(detail).take(MAX_DETAIL_CHARS),
            ),
        )
        while (events.size > MAX_EVENTS) events.removeFirst()
    }

    @Synchronized
    fun snapshot(): List<StreamingDiagnostic> = events.toList()

    @Synchronized
    fun clear() = events.clear()

    private fun redact(value: String): String = value
        .replace(Regex("(?i)(stream[_-]?key|token|password|authorization|secret)\\s*[=:]\\s*\\S+"), "$1=[REDACTED]")
        .replace(Regex("(?i)rtmps?://\\S+"), "[ENDPOINT_REDACTED]")
}
