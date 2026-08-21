package com.unictoai.unictoos.streaming

data class RecordingTrimPlan(
    val inputPath: String,
    val startMs: Long,
    val endMs: Long,
) {
    val durationMs: Long get() = endMs - startMs
}

sealed interface RecordingEditValidation {
    data class Valid(val plan: RecordingTrimPlan) : RecordingEditValidation
    data class Invalid(val reason: String) : RecordingEditValidation
}

object RecordingEditPolicy {
    fun validateTrim(inputPath: String, startMs: Long, endMs: Long): RecordingEditValidation {
        if (inputPath.isBlank()) return RecordingEditValidation.Invalid("Choose a recording first")
        if (inputPath.contains('\u0000')) return RecordingEditValidation.Invalid("Recording path is invalid")
        if (startMs < 0L) return RecordingEditValidation.Invalid("Trim start cannot be negative")
        if (endMs <= startMs) return RecordingEditValidation.Invalid("Trim end must be after trim start")
        if (endMs - startMs > MAX_EDIT_DURATION_MS) return RecordingEditValidation.Invalid("Trim range is too large for a safe local edit")
        return RecordingEditValidation.Valid(RecordingTrimPlan(inputPath, startMs, endMs))
    }

    private const val MAX_EDIT_DURATION_MS = 24L * 60L * 60L * 1_000L
}
