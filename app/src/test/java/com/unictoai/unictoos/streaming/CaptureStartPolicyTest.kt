package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.StreamStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureStartPolicyTest {
    private val camera = CaptureStartPolicy.Request(
        endpoints = listOf("rtmps://example.test/live"),
        sceneJson = "camera-scene",
        practice = false,
    )

    @Test
    fun queuedBeforePrepareWaitsThenStartsExactlyOnce() {
        val queued = CaptureStartPolicy.State(request = camera, requestGeneration = 7L)
        assertEquals(CaptureStartPolicy.Decision.WAIT_FOR_CAPTURE, CaptureStartPolicy.decide(queued, 7L))

        val ready = queued.copy(prepared = true, captureReady = true)
        assertEquals(CaptureStartPolicy.Decision.START_NOW, CaptureStartPolicy.decide(ready, 7L))
        assertEquals(CaptureStartPolicy.Decision.NO_REQUEST, CaptureStartPolicy.decide(CaptureStartPolicy.consume(ready), 7L))
    }

    @Test
    fun captureFailureClearsPendingRequestAndReadiness() {
        val failed = CaptureStartPolicy.clearAfterFailure(
            CaptureStartPolicy.State(request = camera, prepared = true, captureReady = true),
        )
        assertEquals(null, failed.request)
        assertFalse(failed.prepared)
        assertFalse(failed.captureReady)
    }

    @Test
    fun staleRequestCannotStartOnNewPipelineGeneration() {
        val stale = CaptureStartPolicy.State(
            request = camera,
            prepared = true,
            captureReady = true,
            requestGeneration = 4L,
        )
        assertEquals(CaptureStartPolicy.Decision.STALE_REQUEST, CaptureStartPolicy.decide(stale, 5L))
    }

    @Test
    fun practiceDoesNotRequireEndpointButBroadcastDoes() {
        assertFalse(CaptureStartPolicy.Request(emptyList(), "scene", false).hasRequiredDestination())
        assertTrue(CaptureStartPolicy.Request(emptyList(), "scene", true).hasRequiredDestination())
        assertTrue(camera.hasRequiredDestination())
    }

    @Test
    fun cameraAndProjectionUseSameQueueRule() {
        val projection = camera.copy(sceneJson = "projection-scene")
        assertTrue(CaptureStartPolicy.canQueue(StreamStatus.IDLE, false, false))
        assertTrue(CaptureStartPolicy.canQueue(StreamStatus.PREPARING, true, true))
        assertEquals(
            CaptureStartPolicy.decide(
                CaptureStartPolicy.State(request = camera, prepared = true, captureReady = true, requestGeneration = 1L),
                1L,
            ),
            CaptureStartPolicy.decide(
                CaptureStartPolicy.State(request = projection, prepared = true, captureReady = true, requestGeneration = 1L),
                1L,
            ),
        )
    }
}
