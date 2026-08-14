package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.StreamQualityPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamPreflightTest {
    @Test
    fun rejectsInvalidEndpointBeforeNetworkStart() {
        val result = StreamPreflight.validateEndpoint("https://example.com", practice = false)
        assertEquals(PreflightResult.Blocked("The destination must use an RTMP or RTMPS server URL"), result)
    }

    @Test
    fun acceptsPracticeWithoutNetworkButStillChecksEnvironment() {
        val result = StreamPreflight.validateEnvironment(
            networkAvailable = true,
            availableStorageBytes = 128L * 1024L * 1024L,
            batteryPercent = 80,
            isCharging = false,
            thermalStatus = android.os.PowerManager.THERMAL_STATUS_NONE,
            minimumStorageBytes = 64L * 1024L * 1024L,
        )
        assertTrue(result is PreflightResult.Ready)
    }

    @Test
    fun blocksUnsafeEnvironment() {
        val result = StreamPreflight.validateEnvironment(
            networkAvailable = false,
            availableStorageBytes = 128L * 1024L * 1024L,
            batteryPercent = 80,
            isCharging = false,
            thermalStatus = android.os.PowerManager.THERMAL_STATUS_NONE,
            minimumStorageBytes = 64L * 1024L * 1024L,
        )
        assertEquals(PreflightResult.Blocked("No network connection is available"), result)
    }

    @Test
    fun acceptsTheDefaultProfile() {
        val result = StreamPreflight.validateProfile(StreamQualityPreset.BALANCED.toQuality())
        assertTrue(result is PreflightResult.Ready)
    }
}
