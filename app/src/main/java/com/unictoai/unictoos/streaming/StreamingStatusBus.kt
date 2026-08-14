package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.StreamHealthSample
import com.unictoai.unictoos.domain.StreamSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object StreamingStatusBus {
    private const val MAX_HEALTH_SAMPLES = 120
    private val _state = MutableStateFlow(StreamSessionState())
    val state: StateFlow<StreamSessionState> = _state.asStateFlow()
    private val _healthHistory = MutableStateFlow<List<StreamHealthSample>>(emptyList())
    val healthHistory: StateFlow<List<StreamHealthSample>> = _healthHistory.asStateFlow()

    fun update(state: StreamSessionState) {
        _state.value = state
    }

    fun recordHealth(sample: StreamHealthSample) {
        _healthHistory.value = (_healthHistory.value + sample).takeLast(MAX_HEALTH_SAMPLES)
    }

    fun clearHealth() {
        _healthHistory.value = emptyList()
    }
}
