package com.unictoai.unictoos.domain

enum class AutoStopDuration(val label: String, val seconds: Long) {
    OFF("No limit", 0L),
    FIFTEEN_MINUTES("15 min", 15L * 60L),
    THIRTY_MINUTES("30 min", 30L * 60L),
    ONE_HOUR("60 min", 60L * 60L),
    TWO_HOURS("120 min", 120L * 60L),
}
