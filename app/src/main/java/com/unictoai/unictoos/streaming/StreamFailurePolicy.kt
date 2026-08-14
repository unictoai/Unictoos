package com.unictoai.unictoos.streaming

/** Categories used to decide whether a failed connection is safe to retry. */
enum class StreamFailureKind {
    NETWORK,
    TIMEOUT,
    AUTHENTICATION,
    SERVER_REJECTION,
    ENCODER,
    CONFIGURATION,
    UNKNOWN,
}

data class StreamFailureDecision(
    val kind: StreamFailureKind,
    val retryable: Boolean,
    val userMessage: String,
)

object StreamFailurePolicy {
    fun classify(reason: String): StreamFailureDecision {
        val value = reason.lowercase()
        return when {
            value.contains("auth") || value.contains("key") || value.contains("credential") || value.contains("unauthorized") ->
                StreamFailureDecision(StreamFailureKind.AUTHENTICATION, retryable = false, "The destination rejected the stream credentials. Check the server URL and stream key")
            value.contains("unsupported") || value.contains("invalid url") || value.contains("malformed") || value.contains("protocol") ->
                StreamFailureDecision(StreamFailureKind.CONFIGURATION, retryable = false, "The destination URL is invalid or uses an unsupported protocol")
            value.contains("server") || value.contains("rejected") || value.contains("publish") ->
                StreamFailureDecision(StreamFailureKind.SERVER_REJECTION, retryable = false, "The destination server rejected the broadcast")
            value.contains("timeout") || value.contains("timed out") ->
                StreamFailureDecision(StreamFailureKind.TIMEOUT, retryable = true, "The destination did not respond in time")
            value.contains("encoder") || value.contains("codec") || value.contains("media format") ->
                StreamFailureDecision(StreamFailureKind.ENCODER, retryable = false, "The device encoder could not produce the selected stream profile")
            value.contains("network") || value.contains("socket") || value.contains("connect") || value.contains("disconnect") || value.contains("broken pipe") ->
                StreamFailureDecision(StreamFailureKind.NETWORK, retryable = true, "The network connection was interrupted")
            else -> StreamFailureDecision(StreamFailureKind.UNKNOWN, retryable = true, "The destination connection failed")
        }
    }

    fun reconnectDelayMs(
        attempt: Int,
        jitterMillis: Long = 0L,
    ): Long {
        val safeAttempt = attempt.coerceAtLeast(1)
        val exponential = 2_000L * (1L shl (safeAttempt - 1).coerceAtMost(4))
        return (exponential.coerceAtMost(30_000L) + jitterMillis).coerceIn(1_000L, 35_000L)
    }
}
