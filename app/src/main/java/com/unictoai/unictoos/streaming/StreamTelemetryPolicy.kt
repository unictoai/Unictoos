package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.SessionMode
import com.unictoai.unictoos.domain.StreamSessionState
import com.unictoai.unictoos.domain.StreamStatus

object StreamTelemetryPolicy {
    fun shouldExposeFps(state: StreamSessionState, encoderActive: Boolean): Boolean =
        state.status == StreamStatus.LIVE && (encoderActive || state.mode == SessionMode.PRACTICE || state.recording)
}
