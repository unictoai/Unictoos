package com.unictoai.unictoos.health

data class DestinationSlotEvent(
    val slotIndex: Int,
    val state: HealthState,
    val bitrate: Long? = null,
    val error: String? = null,
)
