package com.unictoai.unictoos.domain

enum class LatencyMode(val label: String, val description: String) {
    STABLE("Stable", "Prioritizes resilience and buffer headroom"),
    LOW_LATENCY("Low latency", "Reduces client cache for faster interaction; less tolerant of weak upload"),
}
