package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.StreamQualityPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamPreflightTest {
    @Test
    fun rejectsInvalidEndpointBeforeNetworkStart() {
        val result = StreamPreflight.validateEndpoint("https://example.com", practice = false)
        assertEquals(PreflightResult.Blocked("The destination must use an RTMP, RTMPS, or SRT server URL"), result)
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
    fun streamOnlyDoesNotRequireRecordingStorage() {
        val result = StreamPreflight.validateEnvironment(
            networkAvailable = true,
            availableStorageBytes = 1L,
            batteryPercent = 80,
            isCharging = false,
            thermalStatus = android.os.PowerManager.THERMAL_STATUS_NONE,
            minimumStorageBytes = 64L * 1024L * 1024L,
            storageMode = StorageSessionMode.STREAM_ONLY,
        )
        assertTrue(result is PreflightResult.Ready)
    }

    @Test
    fun practiceRecordingRequiresEstimatedStorage() {
        val result = StreamPreflight.validateEnvironment(
            networkAvailable = true,
            availableStorageBytes = 32L * 1024L * 1024L,
            batteryPercent = 80,
            isCharging = false,
            thermalStatus = android.os.PowerManager.THERMAL_STATUS_NONE,
            minimumStorageBytes = 64L * 1024L * 1024L,
            storageMode = StorageSessionMode.PRACTICE_RECORDING,
            estimatedRecordingBytes = 128L * 1024L * 1024L,
        )
        assertEquals(PreflightResult.Blocked("Not enough storage is available for a safe session"), result)
    }

    @Test
    fun acceptsTheDefaultProfile() {
        val result = StreamPreflight.validateProfile(StreamQualityPreset.BALANCED.toQuality())
        assertTrue(result is PreflightResult.Ready)
    }
}
