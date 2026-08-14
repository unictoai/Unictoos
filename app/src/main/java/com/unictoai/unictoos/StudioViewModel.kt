package com.unictoai.unictoos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unictoai.unictoos.data.CredentialStore
import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.PlatformPreset
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.Source
import com.unictoai.unictoos.domain.SourceType
import com.unictoai.unictoos.domain.StreamDestination
import com.unictoai.unictoos.domain.StreamSessionState
import com.unictoai.unictoos.domain.StreamStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.unictoai.unictoos.streaming.StreamingStatusBus

data class DestinationConfig(
    val platform: PlatformPreset = PlatformPreset.YOUTUBE,
    val serverUrl: String = "",
    val streamKey: String = "",
) {
    val isConfigured: Boolean get() = serverUrl.isNotBlank() && streamKey.isNotBlank()
    val endpoint: String get() = if (isConfigured) serverUrl.trimEnd('/') + "/" + streamKey else ""
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val credentialStore = CredentialStore(application.applicationContext)
    private val _scenes = MutableStateFlow(
        listOf(
            Scene(
                id = "starting-soon",
                name = "Starting Soon",
                aspectRatio = AspectRatio.PORTRAIT,
                sources = listOf(
                    Source("bg", "Background", SourceType.COLOR),
                    Source("title", "Welcome text", SourceType.TEXT),
                ),
            ),
            Scene(
                id = "main-camera",
                name = "Main Camera",
                aspectRatio = AspectRatio.LANDSCAPE,
                sources = listOf(
                    Source("screen", "Gameplay", SourceType.SCREEN),
                    Source("camera", "Face Cam", SourceType.CAMERA),
                ),
            ),
        ),
    )
    val scenes: StateFlow<List<Scene>> = _scenes.asStateFlow()

    private val _destinations = MutableStateFlow(
        listOf(
            StreamDestination("youtube", "YouTube", PlatformPreset.YOUTUBE),
            StreamDestination("twitch", "Twitch", PlatformPreset.TWITCH),
            StreamDestination("kick", "Kick", PlatformPreset.KICK),
        ),
    )
    val destinations: StateFlow<List<StreamDestination>> = _destinations.asStateFlow()

    private val _session = MutableStateFlow(StreamSessionState())
    val session: StateFlow<StreamSessionState> = _session.asStateFlow()

    private val _destination = MutableStateFlow(DestinationConfig())
    val destination: StateFlow<DestinationConfig> = _destination.asStateFlow()

    init {
        val saved = credentialStore.load()
        _destination.value = DestinationConfig(serverUrl = saved.first, streamKey = saved.second)
        viewModelScope.launch {
            StreamingStatusBus.state.collect { state -> _session.value = state }
        }
    }

    fun updateDestination(platform: PlatformPreset, serverUrl: String, streamKey: String) {
        credentialStore.save(serverUrl, streamKey)
        _destination.value = DestinationConfig(platform, serverUrl, streamKey)
    }

    fun clearDestination() {
        credentialStore.clear()
        _destination.value = DestinationConfig()
    }

    fun addScene(name: String) {
        val safeName = name.trim().ifBlank { "New Scene" }
        _scenes.update { current ->
            current + Scene(
                id = "scene-${current.size + 1}",
                name = safeName,
                aspectRatio = AspectRatio.PORTRAIT,
                sources = listOf(Source("color-${current.size + 1}", "Background", SourceType.COLOR)),
            )
        }
    }

    fun toggleSource(sceneId: String, sourceId: String) {
        _scenes.update { scenes ->
            scenes.map { scene ->
                if (scene.id != sceneId) scene else scene.copy(
                    sources = scene.sources.map { source ->
                        if (source.id == sourceId) source.copy(enabled = !source.enabled) else source
                    },
                )
            }
        }
    }

    fun startPreparing() {
        _session.update { it.copy(status = StreamStatus.PREPARING, message = "Preparing capture and encoder") }
    }

    fun enterLive() {
        _session.update {
            it.copy(
                status = StreamStatus.LIVE,
                message = "Broadcast is live",
                bitrateKbps = 4500,
                fps = 30,
            )
        }
    }

    fun stopStream() {
        _session.update { StreamSessionState(status = StreamStatus.IDLE, message = "Broadcast stopped") }
    }
}
