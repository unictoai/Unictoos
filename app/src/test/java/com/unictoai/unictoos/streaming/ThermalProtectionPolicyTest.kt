package com.unictoai.unictoos.streaming

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalProtectionPolicyTest {
    @Test
    fun sustainedModerateThermalPressureTriggersAfterDebounce() {
        assertFalse(
            ThermalProtectionPolicy.shouldThrottle(
                enabled = true,
                thermalStatus = ThermalProtectionPolicy.MODERATE_STATUS,
                highThermalSinceElapsedMs = 1_000L,
                nowElapsedMs = 10_999L,
                alreadyApplied = false,
            ),
        )
        assertTrue(
            ThermalProtectionPolicy.shouldThrottle(
                enabled = true,
                thermalStatus = ThermalProtectionPolicy.MODERATE_STATUS,
                highThermalSinceElapsedMs = 1_000L,
                nowElapsedMs = 11_000L,
                alreadyApplied = false,
            ),
        )
    }

    @Test
    fun disabledOrAlreadyAppliedThermalProtectionDoesNotRepeat() {
        assertFalse(
            ThermalProtectionPolicy.shouldThrottle(
                enabled = false,
                thermalStatus = ThermalProtectionPolicy.MODERATE_STATUS,
                highThermalSinceElapsedMs = 0L,
                nowElapsedMs = 20_000L,
                alreadyApplied = false,
            ),
        )
        assertFalse(
            ThermalProtectionPolicy.shouldThrottle(
                enabled = true,
                thermalStatus = ThermalProtectionPolicy.MODERATE_STATUS,
                highThermalSinceElapsedMs = 0L,
                nowElapsedMs = 20_000L,
                alreadyApplied = true,
            ),
        )
    }

    @Test
    fun lowerThermalStatusResetsProtectionWindow() {
        assertTrue(ThermalProtectionPolicy.resetOnRecovery(thermalStatus = 1))
        assertFalse(ThermalProtectionPolicy.resetOnRecovery(thermalStatus = ThermalProtectionPolicy.MODERATE_STATUS))
    }
}
