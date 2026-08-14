package com.unictoai.unictoos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unictoai.unictoos.data.ConfigExporter
import com.unictoai.unictoos.data.CredentialRepository
import com.unictoai.unictoos.data.CredentialStore
import com.unictoai.unictoos.data.SceneRepository
import com.unictoai.unictoos.data.SceneStore
import com.unictoai.unictoos.data.StreamQualityRepository
import com.unictoai.unictoos.data.StreamQualityStore
import com.unictoai.unictoos.data.ThermalProtectionRepository
import com.unictoai.unictoos.data.ThermalProtectionStore
import com.unictoai.unictoos.data.AudioSettingsRepository
import com.unictoai.unictoos.data.AudioSettingsStore
import com.unictoai.unictoos.data.AutoStopRepository
import com.unictoai.unictoos.data.AutoStopStore
import com.unictoai.unictoos.data.LatencyModeRepository
import com.unictoai.unictoos.data.LatencyModeStore
import com.unictoai.unictoos.monetization.AdsPolicy
import com.unictoai.unictoos.monetization.AdsPreferences
import com.unictoai.unictoos.monetization.AdsPreferencesRepository
import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.AutoStopDuration
import com.unictoai.unictoos.domain.LatencyMode
import com.unictoai.unictoos.domain.AudioQuality
import com.unictoai.unictoos.domain.AudioSettings
import com.unictoai.unictoos.domain.PlatformPreset
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.Source
import com.unictoai.unictoos.domain.SourceType
import com.unictoai.unictoos.domain.StreamDestination
import com.unictoai.unictoos.domain.StreamHealthSample
import com.unictoai.unictoos.domain.StreamSessionState
import com.unictoai.unictoos.domain.StreamStatus
import com.unictoai.unictoos.domain.StreamQuality
import com.unictoai.unictoos.domain.StreamQualityPreset
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

class StudioViewModel(
    application: Application,
    private val credentialStore: CredentialRepository = CredentialStore(application.applicationContext),
    private val sceneStore: SceneRepository = SceneStore(application.applicationContext),
    private val adsPreferences: AdsPreferencesRepository = AdsPreferences(application.applicationContext),
    private val streamQualityStore: StreamQualityRepository = StreamQualityStore(application.applicationContext),
    private val thermalProtectionStore: ThermalProtectionRepository = ThermalProtectionStore(application.applicationContext),
    private val audioSettingsStore: AudioSettingsRepository = AudioSettingsStore(application.applicationContext),
    private val autoStopStore: AutoStopRepository = AutoStopStore(application.applicationContext),
    private val latencyModeStore: LatencyModeRepository = LatencyModeStore(application.applicationContext),
) : AndroidViewModel(application) {
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
    val healthHistory: StateFlow<List<StreamHealthSample>> = StreamingStatusBus.healthHistory
    val adsPolicy: StateFlow<AdsPolicy> = adsPreferences.policy

    private val _streamQuality = MutableStateFlow(streamQualityStore.load())
    val streamQuality: StateFlow<StreamQuality> = _streamQuality.asStateFlow()

    private val _thermalProtectionEnabled = MutableStateFlow(thermalProtectionStore.isEnabled())
    val thermalProtectionEnabled: StateFlow<Boolean> = _thermalProtectionEnabled.asStateFlow()

    private val _audioSettings = MutableStateFlow(audioSettingsStore.load())
    val audioSettings: StateFlow<AudioSettings> = _audioSettings.asStateFlow()

    private val _autoStopDuration = MutableStateFlow(autoStopStore.load())
    val autoStopDuration: StateFlow<AutoStopDuration> = _autoStopDuration.asStateFlow()

    private val _latencyMode = MutableStateFlow(latencyModeStore.load())
    val latencyMode: StateFlow<LatencyMode> = _latencyMode.asStateFlow()

    private val _destination = MutableStateFlow(DestinationConfig())
    val destination: StateFlow<DestinationConfig> = _destination.asStateFlow()

    private val _activePlatform = MutableStateFlow(PlatformPreset.YOUTUBE)
    val activePlatform: StateFlow<PlatformPreset> = _activePlatform.asStateFlow()

    init {
        _scenes.value = sceneStore.loadOrDefault(_scenes.value)
        val saved = credentialStore.load(PlatformPreset.YOUTUBE)
        _destination.value = DestinationConfig(platform = PlatformPreset.YOUTUBE, serverUrl = saved.first, streamKey = saved.second)
        viewModelScope.launch {
            StreamingStatusBus.state.collect { state -> _session.value = state }
        }
    }

    fun exportConfigJson(): String = ConfigExporter.export(_scenes.value, _destinations.value)

    fun setAdsEnabled(enabled: Boolean) {
        adsPreferences.setEnabled(enabled)
    }

    fun setThermalProtectionEnabled(enabled: Boolean) {
        _thermalProtectionEnabled.value = enabled
        thermalProtectionStore.setEnabled(enabled)
    }

    fun setAutoStopDuration(duration: AutoStopDuration) {
        _autoStopDuration.value = duration
        autoStopStore.save(duration)
    }

    fun setLatencyMode(mode: LatencyMode) {
        _latencyMode.value = mode
        latencyModeStore.save(mode)
    }

    fun setAudioQuality(quality: AudioQuality) {
        val updated = _audioSettings.value.copy(quality = quality)
        _audioSettings.value = updated
        audioSettingsStore.save(updated)
    }

    fun setEchoCanceler(enabled: Boolean) {
        val updated = _audioSettings.value.copy(echoCanceler = enabled)
        _audioSettings.value = updated
        audioSettingsStore.save(updated)
    }

    fun setNoiseSuppressor(enabled: Boolean) {
        val updated = _audioSettings.value.copy(noiseSuppressor = enabled)
        _audioSettings.value = updated
        audioSettingsStore.save(updated)
    }

    fun setStreamQualityPreset(preset: StreamQualityPreset) {
        val quality = if (preset == StreamQualityPreset.CUSTOM) {
            _streamQuality.value.copy(preset = preset).validated()
        } else {
            preset.toQuality()
        }
        _streamQuality.value = quality
        streamQualityStore.save(quality)
    }

    fun updateCustomStreamQuality(bitrate: Int, fps: Int) {
        val quality = _streamQuality.value.copy(
            preset = StreamQualityPreset.CUSTOM,
            bitrate = bitrate,
            fps = fps,
        ).validated()
        _streamQuality.value = quality
        streamQualityStore.save(quality)
    }

    fun selectDestination(platform: PlatformPreset) {
        _activePlatform.value = platform
        val saved = credentialStore.load(platform)
        _destination.value = DestinationConfig(platform = platform, serverUrl = saved.first, streamKey = saved.second)
    }

    fun updateDestination(platform: PlatformPreset, serverUrl: String, streamKey: String) {
        credentialStore.save(platform, serverUrl, streamKey)
        _activePlatform.value = platform
        _destination.value = DestinationConfig(platform, serverUrl, streamKey)
        _destinations.update { destinations ->
            destinations.map { destination ->
                if (destination.platform == platform) {
                    destination.copy(serverUrl = serverUrl, streamKey = streamKey, isConfigured = serverUrl.isNotBlank() && streamKey.isNotBlank())
                } else {
                    destination
                }
            }
        }
    }

    fun clearDestination() {
        val platform = _activePlatform.value
        credentialStore.clear(platform)
        _destination.value = DestinationConfig(platform = platform)
        _destinations.update { destinations -> destinations.map { destination ->
            if (destination.platform == platform) destination.copy(serverUrl = "", streamKey = "", isConfigured = false) else destination
        } }
    }

    fun addScene(name: String, aspectRatio: AspectRatio = AspectRatio.PORTRAIT) {
        val safeName = name.trim().ifBlank { "New Scene" }
        _scenes.update { current ->
            (current + Scene(
                id = "scene-${current.size + 1}",
                name = safeName,
                aspectRatio = aspectRatio,
                sources = listOf(Source("color-${current.size + 1}", "Background", SourceType.COLOR)),
            )).also(sceneStore::save)
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
            }.also(sceneStore::save)
        }
    }

    fun addSource(sceneId: String, name: String, type: SourceType) {
        val safeName = name.trim().ifBlank { type.label }
        _scenes.update { scenes ->
            scenes.map { scene ->
                if (scene.id != sceneId) scene else {
                    val sourceId = "${type.name.lowercase()}-${scene.sources.size + 1}"
                    scene.copy(sources = scene.sources + Source(sourceId, safeName, type, enabled = true, zIndex = scene.sources.size))
                }
            }.also(sceneStore::save)
        }
    }

    fun moveSource(sceneId: String, sourceId: String, direction: Int) {
        _scenes.update { scenes ->
            scenes.map { scene ->
                if (scene.id != sceneId) return@map scene
                val currentIndex = scene.sources.indexOfFirst { it.id == sourceId }
                val targetIndex = (currentIndex + direction).coerceIn(0, scene.sources.lastIndex)
                if (currentIndex < 0 || currentIndex == targetIndex) return@map scene
                val reordered = scene.sources.toMutableList().apply { add(targetIndex, removeAt(currentIndex)) }
                    .mapIndexed { index, source -> source.copy(zIndex = index) }
                scene.copy(sources = reordered)
            }.also(sceneStore::save)
        }
    }

    fun updateTextSource(sceneId: String, sourceId: String, content: String, sizeSp: Float) {
        _scenes.update { scenes ->
            scenes.map { scene ->
                if (scene.id != sceneId) scene else scene.copy(
                    sources = scene.sources.map { source ->
                        if (source.id == sourceId && source.type == SourceType.TEXT) {
                            source.copy(textContent = content.take(240), textSizeSp = sizeSp.coerceIn(10f, 72f))
                        } else source
                    },
                )
            }.also(sceneStore::save)
        }
    }

    fun setSourceOpacity(sceneId: String, sourceId: String, opacity: Float) {
        _scenes.update { scenes ->
            scenes.map { scene ->
                if (scene.id != sceneId) scene else scene.copy(sources = scene.sources.map { source -> if (source.id == sourceId) source.copy(opacity = opacity.coerceIn(0f, 1f)) else source })
            }.also(sceneStore::save)
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
