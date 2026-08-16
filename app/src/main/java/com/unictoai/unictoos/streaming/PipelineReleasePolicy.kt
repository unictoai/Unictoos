package com.unictoai.unictoos.streaming

/** Terminal release boundary for one RootEncoder pipeline generation. */
enum class PipelineReleaseState {
    AVAILABLE,
    RELEASING,
    TERMINAL,
    FAILED,
}

data class PipelineReleaseAttempt(val generation: Long)

/**
 * Pure lifecycle policy. A new pipeline may be created only after the previous
 * generation reaches TERMINAL; a failed attempt remains retryable and never
 * masquerades as a completed release.
 */
object PipelineReleasePolicy {
    fun begin(state: PipelineReleaseState, generation: Long): PipelineReleaseAttempt? =
        if (state == PipelineReleaseState.RELEASING) null else PipelineReleaseAttempt(generation)

    fun complete(
        state: PipelineReleaseState,
        attempt: PipelineReleaseAttempt,
        currentGeneration: Long,
        releaseSucceeded: Boolean,
    ): PipelineReleaseState {
        if (attempt.generation != currentGeneration) return state
        return if (releaseSucceeded) PipelineReleaseState.TERMINAL else PipelineReleaseState.FAILED
    }

    fun canCreateNewPipeline(state: PipelineReleaseState): Boolean = state == PipelineReleaseState.TERMINAL

    /** Compatibility helper retained for callers/tests that only need the old boolean contract. */
    fun markReleased(previouslyReleased: Boolean, releaseSucceeded: Boolean): Boolean =
        previouslyReleased || releaseSucceeded
}
