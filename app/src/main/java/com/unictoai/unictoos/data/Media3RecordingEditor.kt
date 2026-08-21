package com.unictoai.unictoos.data

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.unictoai.unictoos.integrations.RecordingEditor
import com.unictoai.unictoos.integrations.RecordingExportRequest
import com.unictoai.unictoos.integrations.RecordingChapter
import com.unictoai.unictoos.integrations.RecordingEditResult
import com.unictoai.unictoos.integrations.RecordingTrimRequest
import com.unictoai.unictoos.streaming.RecordingEditPolicy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

sealed interface LocalRecordingEditState {
    data class Started(val outputPath: String) : LocalRecordingEditState
    data class Completed(val outputPath: String) : LocalRecordingEditState
    data class Failed(val outputPath: String, val message: String) : LocalRecordingEditState
}

/** Media3-backed local editor. It never uploads media or touches stream credentials. */
class Media3RecordingEditor(context: Context) : RecordingEditor {
    private val appContext = context.applicationContext
    private val _states = MutableSharedFlow<LocalRecordingEditState>(extraBufferCapacity = 8)
    val states: SharedFlow<LocalRecordingEditState> = _states.asSharedFlow()

    override fun trim(request: RecordingTrimRequest): RecordingEditResult {
        val validation = RecordingEditPolicy.validateTrim(request.inputPath, request.startMs, request.endMs)
        if (validation !is com.unictoai.unictoos.streaming.RecordingEditValidation.Valid) {
            return RecordingEditResult.Failure((validation as com.unictoai.unictoos.streaming.RecordingEditValidation.Invalid).reason)
        }
        val input = File(request.inputPath)
        if (!input.isFile || !input.canRead()) return RecordingEditResult.Failure("Recording file cannot be read")
        val output = File(input.parentFile ?: appContext.filesDir, "${input.nameWithoutExtension}-trimmed-${System.currentTimeMillis()}.mp4")
        return startTrim(request, output)
    }

    override fun addChapter(marker: RecordingChapter): RecordingEditResult =
        if (marker.timeMs < 0L || marker.title.isBlank()) RecordingEditResult.Failure("Chapter time and title are required") else RecordingEditResult.Unsupported("Chapter metadata is retained by Unictoos markers; MP4 chapter muxing is not enabled yet")

    override fun export(request: RecordingExportRequest): RecordingEditResult {
        val input = File(request.inputPath)
        val output = File(request.outputPath)
        if (!input.isFile || !input.canRead()) return RecordingEditResult.Failure("Recording file cannot be read")
        if (output.absolutePath == input.absolutePath) return RecordingEditResult.Failure("Output must be different from input")
        return startTrim(RecordingTrimRequest(input.absolutePath, 0L, Long.MAX_VALUE / 4L), output)
    }

    private fun startTrim(request: RecordingTrimRequest, output: File): RecordingEditResult {
        output.parentFile?.mkdirs()
        val mediaItem = MediaItem.Builder()
            .setUri(request.inputPath)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(request.startMs)
                    .setEndPositionMs(request.endMs)
                    .build(),
            )
            .build()
        val edited = EditedMediaItem.Builder(mediaItem).build()
        val transformer = Transformer.Builder(appContext)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    _states.tryEmit(LocalRecordingEditState.Completed(output.absolutePath))
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exception: ExportException) {
                    _states.tryEmit(LocalRecordingEditState.Failed(output.absolutePath, exception.message ?: "Media export failed"))
                }
            })
            .build()
        return runCatching {
            transformer.start(edited, output.absolutePath)
            _states.tryEmit(LocalRecordingEditState.Started(output.absolutePath))
            RecordingEditResult.Planned
        }.getOrElse { error ->
            RecordingEditResult.Failure(error.message ?: "Unable to start media export")
        }
    }
}
