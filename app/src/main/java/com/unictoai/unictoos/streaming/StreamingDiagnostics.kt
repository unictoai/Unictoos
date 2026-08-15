package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.DestinationId
import java.util.ArrayDeque

/** Process-local, bounded diagnostics for troubleshooting without storing stream credentials. */
data class StreamingDiagnostic(
    val elapsedRealtimeMs: Long,
    val sessionId: String,
    val generation: Long,
    val event: String,
    val detail: String,
    val destinationId: DestinationId? = null,
    val networkEpoch: Long = 0L,
)

object StreamingDiagnostics {
    private const val MAX_EVENTS = 200
    private const val MAX_DETAIL_CHARS = 240
    private val events = ArrayDeque<StreamingDiagnostic>(MAX_EVENTS)

    @Synchronized
    fun record(
        sessionId: String,
        generation: Long,
        event: String,
        detail: String = "",
        destinationId: DestinationId? = null,
        networkEpoch: Long = 0L,
        elapsedRealtimeMs: Long = android.os.SystemClock.elapsedRealtime(),
    ) {
        events.addLast(
            StreamingDiagnostic(
                elapsedRealtimeMs = elapsedRealtimeMs,
                sessionId = sessionId.take(64),
                generation = generation,
                event = event.take(64),
                detail = redact(detail).take(MAX_DETAIL_CHARS),
                destinationId = destinationId,
                networkEpoch = networkEpoch.coerceAtLeast(0L),
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
