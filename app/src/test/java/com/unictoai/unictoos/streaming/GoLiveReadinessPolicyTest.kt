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
    fun missingDestinationNetworkOrMicrophoneIsBlocking() {
        val result = GoLiveReadinessPolicy.evaluate(
            destinationReady = false,
            captureMode = "screen",
            microphonePermission = false,
            cameraPermission = false,
            networkAvailable = false,
            quality = quality,
        )

        assertFalse(result.canStart)
        assertTrue(result.checks.filter { it.id in setOf("destination", "network", "microphone") }.all { it.blocking && !it.ready })
    }

    @Test
    fun cameraModeRequiresCameraPermissionButScreenModeDoesNot() {
        val camera = GoLiveReadinessPolicy.evaluate(true, "camera", true, false, true, quality)
        val screen = GoLiveReadinessPolicy.evaluate(true, "screen", true, false, true, quality)

        assertFalse(camera.canStart)
        assertTrue(screen.canStart)
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
