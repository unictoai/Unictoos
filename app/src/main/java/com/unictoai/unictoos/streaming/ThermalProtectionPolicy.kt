package com.unictoai.unictoos.streaming

/** Pure thermal policy used by the service ticker; it never performs encoder work itself. */
object ThermalProtectionPolicy {
    const val MODERATE_STATUS = 2
    const val DEFAULT_DEBOUNCE_SECONDS = 10L
    const val THROTTLE_FACTOR = 0.75f

    fun shouldThrottle(
        enabled: Boolean,
        thermalStatus: Int,
        highThermalSinceElapsedMs: Long,
        nowElapsedMs: Long,
        alreadyApplied: Boolean,
        moderateStatus: Int = MODERATE_STATUS,
        debounceSeconds: Long = DEFAULT_DEBOUNCE_SECONDS,
    ): Boolean {
        if (!enabled || alreadyApplied || thermalStatus < moderateStatus || highThermalSinceElapsedMs <= 0L) return false
        return nowElapsedMs - highThermalSinceElapsedMs >= debounceSeconds * 1_000L
    }

    fun resetOnRecovery(thermalStatus: Int, moderateStatus: Int = MODERATE_STATUS): Boolean =
        thermalStatus < moderateStatus
}
