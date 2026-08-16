package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.StreamQualityPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportabilityFeaturesTest {
    @Test
    fun compatibilityReportBlocksUnsupportedAndroidAndWarnsOnHighLoadProfiles() {
        val report = DeviceCompatibilityReportFactory.fromInputs(
            manufacturer = "Test",
            model = "Generic",
            sdkInt = 28,
            quality = StreamQualityPreset.FULL_HD_HIGH_FPS.toQuality(),
            cameraAvailable = false,
            isLargeHeap = false,
        )

        assertEquals(CompatibilityLevel.BLOCKED, report.overallLevel)
        assertTrue(report.checks.first { it.id == "android_sdk" }.detail.contains("Android 10"))
        assertEquals(CompatibilityLevel.CAUTION, report.checks.first { it.id == "stream_profile" }.level)
        assertEquals(CompatibilityLevel.CAUTION, report.checks.first { it.id == "camera" }.level)
    }

    @Test
    fun preflightExplainsMissingNetworkAndDestination() {
        val outcomes = PreflightOutcomeEvaluator.evaluate(
            audioReady = true,
            cameraReady = true,
            networkReady = false,
            destinationReady = false,
            quality = StreamQualityPreset.BALANCED.toQuality(),
        )

        assertEquals(PreflightOutcomeState.ACTION_REQUIRED, outcomes.first { it.id == "network" }.state)
        assertEquals(PreflightOutcomeState.ACTION_REQUIRED, outcomes.first { it.id == "destination" }.state)
        assertTrue(outcomes.first { it.id == "network" }.detail.contains("Connect"))
        assertTrue(outcomes.first { it.id == "destination" }.detail.contains("Settings"))
    }

    @Test
    fun preflightMarksHighLoadProfileAsCautionNotBlocked() {
        val outcomes = PreflightOutcomeEvaluator.evaluate(
            audioReady = true,
            cameraReady = true,
            networkReady = true,
            destinationReady = true,
            quality = StreamQualityPreset.FULL_HD_HIGH_FPS.toQuality(),
        )

        assertEquals(PreflightOutcomeState.CAUTION, outcomes.first { it.id == "quality" }.state)
        assertFalse(outcomes.first { it.id == "quality" }.detail.isBlank())
    }

    @Test
    fun supportExportContainsCompatibilityAndRedactsDiagnosticSecrets() {
        StreamingDiagnostics.clear()
        StreamingDiagnostics.record(
            sessionId = "session-test",
            generation = 3L,
            event = "connection_failed",
            detail = "streamKey=secret-value rtmps://example.invalid/live",
        )
        val report = DeviceCompatibilityReportFactory.fromInputs(
            manufacturer = "Test",
            model = "Generic",
            sdkInt = 35,
            quality = StreamQualityPreset.BALANCED.toQuality(),
            cameraAvailable = true,
            isLargeHeap = true,
        )

        val json = SupportabilityExport.json(
            report = report,
            quality = StreamQualityPreset.BALANCED.toQuality(),
            sessionStatus = "ERROR",
            configuredDestinationCount = 1,
            diagnostics = StreamingDiagnostics.snapshot(),
            generatedAtMillis = 123L,
        )

        assertTrue(json.contains("unictoos-support-bundle-v1"))
        assertTrue(json.contains("connection_failed"))
        assertTrue(json.contains("[REDACTED]"))
        assertTrue(json.contains("[ENDPOINT_REDACTED]"))
        assertFalse(json.contains("secret-value"))
        assertFalse(json.contains("example.invalid"))
    }
}
