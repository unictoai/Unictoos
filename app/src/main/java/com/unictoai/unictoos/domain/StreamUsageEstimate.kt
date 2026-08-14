package com.unictoai.unictoos.domain

import kotlin.math.roundToInt

fun estimatedDataMegabytesPerHour(videoBitrate: Int, audioBitrate: Int): Int {
    val totalBitsPerHour = (videoBitrate.coerceAtLeast(0).toLong() + audioBitrate.coerceAtLeast(0).toLong()) * 3_600L
    return (totalBitsPerHour / 8_000_000.0).roundToInt()
}

fun formatEstimatedDataPerHour(videoBitrate: Int, audioBitrate: Int): String =
    "~${estimatedDataMegabytesPerHour(videoBitrate, audioBitrate)} MB/hour"
