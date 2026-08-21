package com.unictoai.unictoos.domain

import androidx.compose.runtime.Immutable

@Immutable
data class SourceGroup(
    val id: String,
    val name: String,
    val sourceIds: List<String> = emptyList(),
    val enabled: Boolean = true,
)

enum class SceneTransitionMode(val label: String) {
    CUT("Cut"),
    FADE("Fade"),
    SLIDE("Slide"),
}

@Immutable
data class SceneTransition(
    val mode: SceneTransitionMode = SceneTransitionMode.CUT,
    val durationMs: Long = DEFAULT_DURATION_MS,
) {
    val safeDurationMs: Long
        get() = durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)

    companion object {
        const val MIN_DURATION_MS = 0L
        const val MAX_DURATION_MS = 1_500L
        const val DEFAULT_DURATION_MS = 250L
    }
}

object ScenePresentationPolicy {
    fun normalizeGroups(scene: Scene): List<SourceGroup> {
        val validIds = scene.sources.map { it.id }.toSet()
        return scene.sourceGroups.mapNotNull { group ->
            val ids = group.sourceIds.filter { it in validIds }.distinct()
            if (ids.isEmpty()) null else group.copy(sourceIds = ids)
        }
    }

    fun canApplyLiveTransition(status: StreamStatus): Boolean = status == StreamStatus.IDLE || status == StreamStatus.STOPPED
}
