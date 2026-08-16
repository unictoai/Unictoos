package com.unictoai.unictoos

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.unictoai.unictoos.domain.RecordingState
import com.unictoai.unictoos.domain.StreamStatus
import com.unictoai.unictoos.streaming.EncoderCrashPolicy
import com.unictoai.unictoos.streaming.StreamingForegroundService
import com.unictoai.unictoos.streaming.StreamingStatusBus

class UnictoosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        val applicationContext = this
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (EncoderCrashPolicy.isRecoverableGraphicsFailure(thread.name, throwable)) {
                val previous = StreamingStatusBus.state.value
                StreamingStatusBus.update(
                    previous.copy(
                        status = StreamStatus.ERROR,
                        captureReady = false,
                        encoderReady = false,
                        previewReady = false,
                        recording = false,
                        recordingState = RecordingState.IDLE,
                        message = EncoderCrashPolicy.GRAPHICS_RESOURCE_MESSAGE,
                    ),
                )
                Handler(Looper.getMainLooper()).post {
                    runCatching {
                        applicationContext.startService(
                            Intent(applicationContext, StreamingForegroundService::class.java).apply {
                                action = StreamingForegroundService.ACTION_ENCODER_GRAPHICS_FAILURE
                                putExtra(
                                    StreamingForegroundService.EXTRA_PIPELINE_GENERATION,
                                    previous.pipelineGeneration,
                                )
                            },
                        )
                    }
                }
                // Do not chain a confirmed RootEncoder GL exhaustion failure to
                // Android's default handler: that would terminate the process
                // before the service can release its EGL/preview resources.
            } else {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
