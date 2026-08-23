package com.unictoai.unictoos.streaming

import kotlin.math.roundToInt

enum class AdaptiveBitrateAction {
    HOLD,
    STEP_DOWN,
    STEP_UP,
}

data class AdaptiveBitrateDecision(
    val action: AdaptiveBitrateAction,
    val bitrate: Int,
)

/**
 * Decides whether a live video target should move based on the rolling reported bitrate.
 * Time counters are supplied by the caller so this policy is deterministic and unit-testable.
 */
fun decideAdaptiveBitrate(
    currentTargetBitrate: Int,
    baselineTargetBitrate: Int,
    rollingAverageBitrate: Int,
    degradedSeconds: Int,
    recoveredSeconds: Int,
    minimumBitrate: Int = 1_000_000,
    maximumBitrate: Int = baselineTargetBitrate,
): AdaptiveBitrateDecision {
    val safeCurrent = currentTargetBitrate.coerceIn(minimumBitrate, maximumBitrate)
    val belowDegradationThreshold = rollingAverageBitrate < (safeCurrent * DEGRADATION_RATIO).roundToInt()
    val aboveRecoveryThreshold = rollingAverageBitrate >= (safeCurrent * RECOVERY_RATIO).roundToInt()

    if (belowDegradationThreshold && degradedSeconds >= DEGRADATION_WAIT_SECONDS && safeCurrent > minimumBitrate) {
        val next = (safeCurrent * 0.85f).roundToInt().coerceAtLeast(minimumBitrate)
        return AdaptiveBitrateDecision(AdaptiveBitrateAction.STEP_DOWN, next)
    }

    if (aboveRecoveryThreshold && recoveredSeconds >= RECOVERY_WAIT_SECONDS && safeCurrent < maximumBitrate) {
        val next = (safeCurrent * 1.10f).roundToInt().coerceAtMost(maximumBitrate)
        return AdaptiveBitrateDecision(AdaptiveBitrateAction.STEP_UP, next)
    }

    return AdaptiveBitrateDecision(AdaptiveBitrateAction.HOLD, safeCurrent)
}

private const val DEGRADATION_RATIO = 0.80f
private const val RECOVERY_RATIO = 0.95f
private const val DEGRADATION_WAIT_SECONDS = 15
private const val RECOVERY_WAIT_SECONDS = 60
