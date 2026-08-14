package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.domain.StreamSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object StreamingStatusBus {
    private val _state = MutableStateFlow(StreamSessionState())
    val state: StateFlow<StreamSessionState> = _state.asStateFlow()

    fun update(state: StreamSessionState) {
        _state.value = state
    }
}
