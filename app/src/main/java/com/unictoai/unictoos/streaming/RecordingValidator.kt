package com.unictoai.unictoos.streaming

import android.media.MediaMetadataRetriever
import java.io.File

sealed interface RecordingValidation {
    data class Valid(val durationMs: Long) : RecordingValidation
    data class Invalid(val reason: String) : RecordingValidation
}

object RecordingValidator {
    fun validate(file: File): RecordingValidation {
        if (!file.exists()) return RecordingValidation.Invalid("Recording file was not created")
        if (file.length() <= 0L) return RecordingValidation.Invalid("Recording file is empty")
        val retriever = MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (duration <= 0L) RecordingValidation.Invalid("Recording has no readable duration") else RecordingValidation.Valid(duration)
        }.getOrElse { RecordingValidation.Invalid("Recording could not be opened") }
            .also { runCatching { retriever.release() } }
    }
}
