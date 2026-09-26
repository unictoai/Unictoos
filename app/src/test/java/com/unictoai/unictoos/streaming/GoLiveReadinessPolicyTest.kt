package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.StreamQualityPreset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoLiveReadinessPolicyTest {
    private val quality = StreamQualityPreset.BALANCED.toQuality()

    @Test
    fun configuredHealthyScreenSetupIsReady() {
        val result = GoLiveReadinessPolicy.evaluate(
            destinationReady = true,
            captureMode = "screen",
            microphonePermission = true,
            cameraPermission = false,
            networkAvailable = true,
            quality = quality,
        )

        assertTrue(result.canStart)
        assertTrue(result.checks.first { it.id == "capture" }.ready)
    }

    @Test
    fun missingDestinationOrNetworkIsBlockingButMissingPermissionsAreNot() {
        val result = GoLiveReadinessPolicy.evaluate(
            destinationReady = false,
            captureMode = "screen",
            microphonePermission = false,
            cameraPermission = false,
            networkAvailable = false,
            quality = quality,
        )

        assertFalse(result.canStart)
        assertTrue(result.checks.filter { it.id in setOf("destination", "network") }.all { it.blocking && !it.ready })
        assertFalse(result.checks.first { it.id == "microphone" }.blocking)
    }

    @Test
    fun missingPermissionsAreCautionNotBlocking() {
        val camera = GoLiveReadinessPolicy.evaluate(true, "camera", true, false, true, quality)
        val screen = GoLiveReadinessPolicy.evaluate(true, "screen", false, false, true, quality)

        assertTrue(camera.canStart)
        assertTrue(screen.canStart)
        assertFalse(camera.checks.first { it.id == "capture" }.ready)
        assertFalse(camera.checks.first { it.id == "capture" }.blocking)
        assertFalse(screen.checks.first { it.id == "microphone" }.ready)
        assertFalse(screen.checks.first { it.id == "microphone" }.blocking)
    }

    @Test
    fun noCaptureSourceIsBlocking() {
        val result = GoLiveReadinessPolicy.evaluate(true, "none", true, true, true, quality)

        assertFalse(result.canStart)
        assertFalse(result.checks.first { it.id == "capture" }.ready)
        assertTrue(result.checks.first { it.id == "capture" }.blocking)
    }

    @Test
    fun highLoadQualityIsCautionOnly() {
        val result = GoLiveReadinessPolicy.evaluate(
            destinationReady = true,
            captureMode = "screen",
            microphonePermission = true,
            cameraPermission = false,
            networkAvailable = true,
            quality = StreamQualityPreset.FULL_HD_HIGH_FPS.toQuality(),
        )

        assertTrue(result.canStart)
        assertFalse(result.checks.first { it.id == "quality" }.blocking)
        assertFalse(result.checks.first { it.id == "quality" }.ready)
    }
}
