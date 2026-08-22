package com.unictoai.unictoos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unictoai.unictoos.data.ConfigExporter
import com.unictoai.unictoos.data.ConfigImportResult
import com.unictoai.unictoos.data.ConfigImporter
import com.unictoai.unictoos.data.CredentialRepository
import com.unictoai.unictoos.data.CredentialStore
import com.unictoai.unictoos.data.SceneRepository
import com.unictoai.unictoos.data.SceneStore
import com.unictoai.unictoos.data.StreamQualityRepository
import com.unictoai.unictoos.data.StreamQualityStore
import com.unictoai.unictoos.data.ThermalProtectionRepository
import com.unictoai.unictoos.data.ThermalProtectionStore
import com.unictoai.unictoos.data.MultistreamSelectionRepository
import com.unictoai.unictoos.data.MultistreamSelectionStore
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
import com.unictoai.unictoos.domain.SceneTransition
import com.unictoai.unictoos.domain.SceneTransitionMode
import com.unictoai.unictoos.domain.Source
import com.unictoai.unictoos.domain.SourceGroup
import com.unictoai.unictoos.domain.SceneGeometryPolicy
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.unictoai.unictoos.streaming.PreflightResult
import com.unictoai.unictoos.streaming.StreamPreflight
import com.unictoai.unictoos.streaming.StreamingStatusBus
import com.unictoai.unictoos.streaming.StreamEndpointPolicy

private object UnavailableCredentialRepository : CredentialRepository {
    override fun save(platform: PlatformPreset, serverUrl: String, streamKey: String) = Unit
    override fun load(platform: PlatformPreset): Pair<String, String> = "" to ""
    override fun clear(platform: PlatformPreset) = Unit
}

private fun safeCredentialRepository(application: Application): CredentialRepository =
    runCatching { CredentialStore(application.applicationContext) }.getOrElse { UnavailableCredentialRepository }

private fun safeStreamQuality(repository: StreamQualityRepository): StreamQuality =
    runCatching { repository.load() }.getOrElse { StreamQualityPreset.BALANCED.toQuality() }

private fun safeAudioSettings(repository: AudioSettingsRepository): AudioSettings =
    runCatching { repository.load() }.getOrDefault(AudioSettings())

private fun safeAutoStop(repository: AutoStopRepository): AutoStopDuration =
    runCatching { repository.load() }.getOrDefault(AutoStopDuration.OFF)

private fun safeLatencyMode(repository: LatencyModeRepository): LatencyMode =
    runCatching { repository.load() }.getOrDefault(LatencyMode.STABLE)

private object UnavailableMultistreamSelectionRepository : MultistreamSelectionRepository {
    override fun load(): Set<PlatformPreset> = setOf(PlatformPreset.YOUTUBE)
    override fun save(platforms: Set<PlatformPreset>) = Unit
}

private fun safeMultistreamSelectionRepository(application: Application): MultistreamSelectionRepository =
    runCatching { MultistreamSelectionStore(application.applicationContext) }.getOrElse { UnavailableMultistreamSelectionRepository }

private fun safeMultistreamPlatforms(repository: MultistreamSelectionRepository): Set<PlatformPreset> =
    runCatching { repository.load() }.getOrDefault(setOf(PlatformPreset.YOUTUBE))

data class DestinationConfig(
    val platform: PlatformPreset = PlatformPreset.YOUTUBE,
    val serverUrl: String = "",
    val streamKey: String = "",
) {
    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && StreamEndpointPolicy.isSupported(serverUrl) &&
            (serverUrl.trim().startsWith("srt://", ignoreCase = true) || streamKey.isNotBlank())
    val endpoint: String get() = if (isConfigured) {
        if (serverUrl.trim().startsWith("srt://", ignoreCase = true)) serverUrl.trim() else serverUrl.trimEnd('/') + "/" + streamKey.trim()
    } else ""
}

class StudioViewModel @JvmOverloads constructor(
    application: Application,
    private val credentialStore: CredentialRepository = safeCredentialRepository(application),
    private val sceneStore: SceneRepository = SceneStore(application.applicationContext),
    private val adsPreferences: AdsPreferencesRepository = AdsPreferences(application.applicationContext),
    private val streamQualityStore: StreamQualityRepository = StreamQualityStore(application.applicationContext),
    private val thermalProtectionStore: ThermalProtectionRepository = ThermalProtectionStore(application.applicationContext),
    private val audioSettingsStore: AudioSettingsRepository = AudioSettingsStore(application.applicationContext),
    private val autoStopStore: AutoStopRepository = AutoStopStore(application.applicationContext),
    private val latencyModeStore: LatencyModeRepository = LatencyModeStore(application.applicationContext),
    private val multistreamSelectionStore: MultistreamSelectionRepository = safeMultistreamSelectionRepository(application),
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
    private var scenePersistenceJob: Job? = null

    private fun scheduleScenePersistence() {
        scenePersistenceJob?.cancel()
        scenePersistenceJob = viewModelScope.launch(Dispatchers.IO) {
            delay(350L)
            sceneStore.save(_scenes.value)
        }
    }
    val session: StateFlow<StreamSessionState> = _session.asStateFlow()
    val healthHistory: StateFlow<List<StreamHealthSample>> = StreamingStatusBus.healthHistory
    val adsPolicy: StateFlow<AdsPolicy> = adsPreferences.policy

    private val _streamQuality = MutableStateFlow(safeStreamQuality(streamQualityStore))
    val streamQuality: StateFlow<StreamQuality> = _streamQuality.asStateFlow()

    private val _thermalProtectionEnabled = MutableStateFlow(runCatching { thermalProtectionStore.isEnabled() }.getOrDefault(true))
    val thermalProtectionEnabled: StateFlow<Boolean> = _thermalProtectionEnabled.asStateFlow()

    private val _audioSettings = MutableStateFlow(safeAudioSettings(audioSettingsStore))
    val audioSettings: StateFlow<AudioSettings> = _audioSettings.asStateFlow()

    private val _autoStopDuration = MutableStateFlow(safeAutoStop(autoStopStore))
    val autoStopDuration: StateFlow<AutoStopDuration> = _autoStopDuration.asStateFlow()

    private val _latencyMode = MutableStateFlow(safeLatencyMode(latencyModeStore))
    val latencyMode: StateFlow<LatencyMode> = _latencyMode.asStateFlow()

    private val _destination = MutableStateFlow(DestinationConfig())
    val destination: StateFlow<DestinationConfig> = _destination.asStateFlow()

    private val _activePlatform = MutableStateFlow(PlatformPreset.YOUTUBE)
    val activePlatform: StateFlow<PlatformPreset> = _activePlatform.asStateFlow()

    private val _multistreamPlatforms = MutableStateFlow(safeMultistreamPlatforms(multistreamSelectionStore).take(2).toSet())
    val multistreamPlatforms: StateFlow<Set<PlatformPreset>> = _multistreamPlatforms.asStateFlow()

    init {
        _scenes.value = runCatching { sceneStore.loadOrDefault(_scenes.value) }.getOrDefault(_scenes.value)
        hydrateSavedDestinations()
        val saved = loadCredentials(PlatformPreset.YOUTUBE)
        _destination.value = DestinationConfig(platform = PlatformPreset.YOUTUBE, serverUrl = saved.first, streamKey = saved.second)
        viewModelScope.launch {
            StreamingStatusBus.state.collect { state -> _session.value = state }
        }
    }

    fun exportConfigJson(): String = ConfigExporter.export(_scenes.value, _destinations.value)

    fun importConfigJson(raw: String): ConfigImportResult {
        return when (val result = ConfigImporter.importScenes(raw)) {
            is ConfigImportResult.Success -> {
                _scenes.value = result.scenes
                sceneStore.save(result.scenes)
                result
            }
            is ConfigImportResult.Rejected -> result
        }
    }

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

    fun setMultistreamPlatformEnabled(platform: PlatformPreset, enabled: Boolean): Boolean {
        val current = _multistreamPlatforms.value
        val updated = if (enabled) {
            if (platform in current) current else if (current.size >= 2) return false else current + platform
        } else {
            current - platform
        }
        val safe = updated.ifEmpty { setOf(platform) }
        _multistreamPlatforms.value = safe
        multistreamSelectionStore.save(safe)
        return true
    }

    fun broadcastEndpoints(primary: DestinationConfig): List<String> {
        val selected = _destinations.value
            .filter { it.platform in _multistreamPlatforms.value && it.isConfigured }
            .map { if (it.serverUrl.trim().startsWith("srt://", ignoreCase = true)) it.serverUrl.trim() else it.serverUrl.trimEnd('/') + "/" + it.streamKey }
        return (listOf(primary.endpoint) + selected).filter { it.isNotBlank() }.distinct().take(2)
    }

    fun selectDestination(platform: PlatformPreset) {
        _activePlatform.value = platform
        val saved = loadCredentials(platform)
        _destination.value = DestinationConfig(platform = platform, serverUrl = saved.first, streamKey = saved.second)
        _destinations.update { destinations ->
            destinations.map { destination ->
                if (destination.platform == platform) {
                    destination.copy(
                        serverUrl = saved.first,
                        streamKey = saved.second,
                        isConfigured = isUsableDestination(saved.first, saved.second),
                    )
                } else {
                    destination
                }
            }
        }
    }

    fun updateDestination(platform: PlatformPreset, serverUrl: String, streamKey: String) {
        val normalizedServerUrl = serverUrl.trim()
        val normalizedStreamKey = streamKey.trim()
        credentialStore.save(platform, normalizedServerUrl, normalizedStreamKey)
        _activePlatform.value = platform
        _destination.value = DestinationConfig(
            platform = platform,
            serverUrl = normalizedServerUrl,
            streamKey = normalizedStreamKey,
        )
        _destinations.update { destinations ->
            destinations.map { destination ->
                if (destination.platform == platform) {
                    destination.copy(
                        serverUrl = normalizedServerUrl,
                        streamKey = normalizedStreamKey,
                        isConfigured = isUsableDestination(normalizedServerUrl, normalizedStreamKey),
                    )
                } else {
                    destination
                }
            }
        }
    }

    private fun loadCredentials(platform: PlatformPreset): Pair<String, String> =
        runCatching { credentialStore.load(platform) }.getOrDefault("" to "")

    private fun isUsableDestination(serverUrl: String, streamKey: String): Boolean =
        DestinationConfig(platform = PlatformPreset.CUSTOM, serverUrl = serverUrl, streamKey = streamKey).isConfigured &&
            StreamPreflight.validateEndpoint(DestinationConfig(platform = PlatformPreset.CUSTOM, serverUrl = serverUrl, streamKey = streamKey).endpoint, practice = false) is PreflightResult.Ready

    private fun hydrateSavedDestinations() {
        _destinations.value = _destinations.value.map { destination ->
            val saved = loadCredentials(destination.platform)
            destination.copy(
                serverUrl = saved.first,
                streamKey = saved.second,
                isConfigured = isUsableDestination(saved.first, saved.second),
            )
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

    fun setSceneAspectRatio(sceneId: String, aspectRatio: AspectRatio) {
        _scenes.update { scenes ->
            scenes.map { scene ->
                if (scene.id == sceneId) scene.copy(aspectRatio = aspectRatio) else scene
            }.also(sceneStore::save)
        }
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

    fun addSceneTemplate(templateId: String) {
        val suffix = System.currentTimeMillis().toString()
        val template = when (templateId) {
            "portrait-camera" -> Scene(
                id = "template-portrait-$suffix",
                name = "Portrait Live",
                aspectRatio = AspectRatio.PORTRAIT,
                sources = listOf(
                    Source("camera-$suffix", "Camera", SourceType.CAMERA, groupId = "group-$suffix"),
                    Source("text-$suffix", "Live title", SourceType.TEXT, textContent = "Live now", groupId = "group-$suffix"),
                ),
                sourceGroups = listOf(SourceGroup("group-$suffix", "On camera", listOf("camera-$suffix", "text-$suffix"))),
                transition = SceneTransition(SceneTransitionMode.FADE),
            )
            "gameplay" -> Scene(
                id = "template-gameplay-$suffix",
                name = "Gameplay + Camera",
                aspectRatio = AspectRatio.LANDSCAPE,
                sources = listOf(
                    Source("screen-$suffix", "Gameplay", SourceType.SCREEN, groupId = "group-$suffix"),
                    Source("camera-$suffix", "Face cam", SourceType.CAMERA, x = 0.70f, y = 0.06f, width = 0.25f, height = 0.25f, groupId = "group-$suffix"),
                    Source("text-$suffix", "Stream title", SourceType.TEXT, textContent = "Gameplay", groupId = "group-$suffix"),
                ),
                sourceGroups = listOf(SourceGroup("group-$suffix", "Live composition", listOf("screen-$suffix", "camera-$suffix", "text-$suffix"))),
                transition = SceneTransition(SceneTransitionMode.CUT),
            )
            else -> Scene(
                id = "template-talk-$suffix",
                name = "Talk Show",
                aspectRatio = AspectRatio.LANDSCAPE,
                sources = listOf(
                    Source("camera-$suffix", "Camera", SourceType.CAMERA, groupId = "group-$suffix"),
                    Source("text-$suffix", "Lower third", SourceType.TEXT, textContent = "Unictoos live", y = 0.78f, height = 0.14f, groupId = "group-$suffix"),
                ),
                sourceGroups = listOf(SourceGroup("group-$suffix", "Host", listOf("camera-$suffix", "text-$suffix"))),
                transition = SceneTransition(SceneTransitionMode.FADE),
            )
        }
        _scenes.update { (it + template).also(sceneStore::save) }
    }

    fun setSceneTransition(sceneId: String, mode: SceneTransitionMode, durationMs: Long = SceneTransition.DEFAULT_DURATION_MS) {
        _scenes.update { scenes ->
            scenes.map { scene ->
                if (scene.id == sceneId) scene.copy(transition = SceneTransition(mode, durationMs)) else scene
            }.also(sceneStore::save)
        }
    }

    fun setSourceGroup(sceneId: String, groupId: String, enabled: Boolean) {
        _scenes.update { scenes ->
            scenes.map { scene ->
                if (scene.id != sceneId) scene else scene.copy(
                    sourceGroups = scene.sourceGroups.map { group -> if (group.id == groupId) group.copy(enabled = enabled) else group },
                )
            }.also(sceneStore::save)
        }
    }

    fun createSourceGroup(sceneId: String, name: String, sourceIds: List<String>) {
        val safeName = name.trim().ifBlank { "Source group" }.take(64)
        _scenes.update { scenes ->
            scenes.map { scene ->
                if (scene.id != sceneId) scene else {
                    val validIds = sourceIds.filter { id -> scene.sources.any { it.id == id } }.distinct()
                    if (validIds.isEmpty()) scene else {
                        val groupId = "group-${System.currentTimeMillis()}"
                        scene.copy(
                            sourceGroups = (scene.sourceGroups + SourceGroup(groupId, safeName, validIds)).take(16),
                            sources = scene.sources.map { source -> if (source.id in validIds) source.copy(groupId = groupId) else source },
                        )
                    }
                }
            }.also(sceneStore::save)
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
                if (scene.sources.isEmpty()) return@map scene
                val currentIndex = scene.sources.indexOfFirst { it.id == sourceId }
                if (currentIndex < 0) return@map scene
                val targetIndex = (currentIndex + direction).coerceIn(0, scene.sources.lastIndex)
                if (currentIndex == targetIndex) return@map scene
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
            }
        }
        scheduleScenePersistence()
    }

    fun setSourceGeometry(sceneId: String, sourceId: String, x: Float, y: Float, width: Float, height: Float) {
        val geometry = SceneGeometryPolicy.clamp(x, y, width, height)
        _scenes.update { scenes ->
            scenes.map { scene ->
                if (scene.id != sceneId) scene else scene.copy(
                    sources = scene.sources.map { source ->
                        if (source.id != sourceId) source else source.copy(
                            x = geometry.x,
                            y = geometry.y,
                            width = geometry.width,
                            height = geometry.height,
                        )
                    },
                )
            }
        }
        scheduleScenePersistence()
    }

    fun setSourceOpacity(sceneId: String, sourceId: String, opacity: Float) {
        _scenes.update { scenes ->
            scenes.map { scene ->
                if (scene.id != sceneId) scene else scene.copy(sources = scene.sources.map { source -> if (source.id == sourceId) source.copy(opacity = opacity.coerceIn(0f, 1f)) else source })
            }
        }
        scheduleScenePersistence()
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
