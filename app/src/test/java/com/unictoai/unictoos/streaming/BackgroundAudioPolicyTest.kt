package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.StreamStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundAudioPolicyTest {
    @Test
    fun allowsLiveCameraOnlySession() {
        assertTrue(
            BackgroundAudioPolicy.canEnter(
                enabled = true,
                screenCaptureActive = false,
                captureReady = true,
                status = StreamStatus.LIVE,
            ),
        )
    }

    @Test
    fun rejectsScreenCaptureAndNonLiveSessions() {
        assertFalse(BackgroundAudioPolicy.canEnter(true, true, true, StreamStatus.LIVE))
        assertFalse(BackgroundAudioPolicy.canEnter(true, false, true, StreamStatus.CONNECTING))
        assertFalse(BackgroundAudioPolicy.canEnter(false, false, true, StreamStatus.LIVE))
        assertFalse(BackgroundAudioPolicy.canEnter(true, false, false, StreamStatus.LIVE))
    }
}
