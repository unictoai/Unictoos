package com.unictoai.unictoos.streaming

/**
 * Keeps a failed resource-release attempt retryable.
 *
 * A pipeline is considered released only after the underlying release call
 * reports success. This policy is pure so the failure/retry contract can be
 * tested without starting Android capture or RootEncoder.
 */
object PipelineReleasePolicy {
    fun markReleased(previouslyReleased: Boolean, releaseSucceeded: Boolean): Boolean =
        previouslyReleased || releaseSucceeded
}
