package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.SessionMode
import com.unictoai.unictoos.domain.StreamSessionState
import com.unictoai.unictoos.domain.StreamStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamTelemetryPolicyTest {
    @Test
    fun exposesFpsForActiveBroadcastEncoder() {
        val state = StreamSessionState(status = StreamStatus.LIVE, mode = SessionMode.BROADCAST)
        assertTrue(StreamTelemetryPolicy.shouldExposeFps(state, encoderActive = true))
    }

    @Test
    fun exposesFpsForActivePracticeSession() {
        val state = StreamSessionState(status = StreamStatus.LIVE, mode = SessionMode.PRACTICE)
        assertTrue(StreamTelemetryPolicy.shouldExposeFps(state, encoderActive = false))
    }

    @Test
    fun suppressesFpsWhenSessionIsNotLive() {
        val state = StreamSessionState(status = StreamStatus.IDLE, mode = SessionMode.BROADCAST, fps = 66)
        assertFalse(StreamTelemetryPolicy.shouldExposeFps(state, encoderActive = true))
    }

    @Test
    fun suppressesFpsForNonLivePreparation() {
        val state = StreamSessionState(status = StreamStatus.PREPARING, mode = SessionMode.BROADCAST)
        assertFalse(StreamTelemetryPolicy.shouldExposeFps(state, encoderActive = true))
    }
}
