package com.unictoai.unictoos.domain

import androidx.compose.runtime.Immutable

@Immutable
data class SessionSummary(
    val id: String,
    val mode: SessionMode,
    val elapsedSeconds: Long,
    val bitrateKbps: Int,
    val fps: Int,
    val droppedFrames: Int,
    val finishedAtMillis: Long,
)

@Immutable
data class StreamMarker(
    val id: String,
    val label: String,
    val elapsedSeconds: Long,
    val createdAtMillis: Long,
)
