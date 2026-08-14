package com.unictoai.unictoos

import android.app.Application
import app.cash.turbine.test
import com.unictoai.unictoos.data.CredentialRepository
import com.unictoai.unictoos.data.SceneRepository
import com.unictoai.unictoos.monetization.AdsPolicy
import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.PlatformPreset
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.SourceType
import com.unictoai.unictoos.domain.StreamStatus
import com.unictoai.unictoos.domain.StreamQuality
import com.unictoai.unictoos.domain.StreamQualityPreset
import com.unictoai.unictoos.data.StreamQualityRepository
import com.unictoai.unictoos.data.ThermalProtectionRepository
import com.unictoai.unictoos.data.AudioSettingsRepository
import com.unictoai.unictoos.data.AutoStopRepository
import com.unictoai.unictoos.data.LatencyModeRepository
import com.unictoai.unictoos.domain.AutoStopDuration
import com.unictoai.unictoos.domain.AudioQuality
import com.unictoai.unictoos.domain.LatencyMode
import com.unictoai.unictoos.domain.AudioSettings
import com.unictoai.unictoos.monetization.AdsPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModelBehaviorTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var credentials: FakeCredentialRepository
    private lateinit var scenes: FakeSceneRepository
    private lateinit var ads: FakeAdsPreferences
    private lateinit var quality: FakeStreamQualityRepository
    private lateinit var thermal: FakeThermalProtectionRepository
    private lateinit var audio: FakeAudioSettingsRepository
    private lateinit var autoStop: FakeAutoStopRepository
    private lateinit var latency: FakeLatencyModeRepository
    private lateinit var viewModel: StudioViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        credentials = FakeCredentialRepository()
        scenes = FakeSceneRepository()
        ads = FakeAdsPreferences()
        quality = FakeStreamQualityRepository()
        thermal = FakeThermalProtectionRepository()
        audio = FakeAudioSettingsRepository()
        autoStop = FakeAutoStopRepository()
        latency = FakeLatencyModeRepository()
        viewModel = StudioViewModel(Application(), credentials, scenes, ads, quality, thermal, audio, autoStop, latency)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addSceneUsesRequestedRatioDefaultSourceAndUniqueId() {
        val before = viewModel.scenes.value

        viewModel.addScene("  Vertical show  ", AspectRatio.LANDSCAPE)

        val added = viewModel.scenes.value.last()
        assertEquals(before.size + 1, viewModel.scenes.value.size)
        assertEquals("Vertical show", added.name)
        assertEquals(AspectRatio.LANDSCAPE, added.aspectRatio)
        assertEquals("scene-${before.size + 1}", added.id)
        assertEquals(SourceType.COLOR, added.sources.single().type)
        assertNotEquals(before.first().id, added.id)
        assertEquals(viewModel.scenes.value, scenes.saved.last())
    }

    @Test
    fun toggleSourceOnlyChangesTargetedSource() {
        val original = viewModel.scenes.value.first { it.id == "main-camera" }
        val originalCamera = original.sources.first { it.id == "camera" }
        val originalScreen = original.sources.first { it.id == "screen" }

        viewModel.toggleSource("main-camera", "screen")

        val updated = viewModel.scenes.value.first { it.id == "main-camera" }
        assertEquals(!originalScreen.enabled, updated.sources.first { it.id == "screen" }.enabled)
        assertEquals(originalCamera, updated.sources.first { it.id == "camera" })
    }

    @Test
    fun addSourceAppendsWithGeneratedIdAndNextZIndex() {
        viewModel.addSource("main-camera", "Overlay", SourceType.TEXT)

        val source = viewModel.scenes.value.first { it.id == "main-camera" }.sources.last()
        assertEquals("text-3", source.id)
        assertEquals("Overlay", source.name)
        assertEquals(SourceType.TEXT, source.type)
        assertEquals(2, source.zIndex)
        assertTrue(source.enabled)
    }

    @Test
    fun moveSourceReordersBothDirectionsClampsAndIgnoresInvalidIds() {
        fun ids() = viewModel.scenes.value.first { it.id == "main-camera" }.sources.map { it.id }

        viewModel.moveSource("main-camera", "camera", -1)
        assertEquals(listOf("camera", "screen"), ids())
        viewModel.moveSource("main-camera", "camera", -1)
        assertEquals(listOf("camera", "screen"), ids())
        viewModel.moveSource("main-camera", "camera", 1)
        assertEquals(listOf("screen", "camera"), ids())
        viewModel.moveSource("main-camera", "missing", -1)
        assertEquals(listOf("screen", "camera"), ids())
        assertEquals(listOf(0, 1), viewModel.scenes.value.first { it.id == "main-camera" }.sources.map { it.zIndex })
    }

    @Test
    fun setSourceOpacityClampsOnlyTargetedSource() {
        viewModel.setSourceOpacity("main-camera", "screen", 2f)
        var updated = viewModel.scenes.value.first { it.id == "main-camera" }
        assertEquals(1f, updated.sources.first { it.id == "screen" }.opacity)
        assertEquals(1f, updated.sources.first { it.id == "camera" }.opacity)

        viewModel.setSourceOpacity("main-camera", "camera", -1f)
        updated = viewModel.scenes.value.first { it.id == "main-camera" }
        assertEquals(0f, updated.sources.first { it.id == "camera" }.opacity)
        assertEquals(1f, updated.sources.first { it.id == "screen" }.opacity)
    }

    @Test
    fun destinationOperationsUseRepositoryAndUpdateState() {
        credentials.values[PlatformPreset.TWITCH] = "saved-url" to "saved-key"

        viewModel.selectDestination(PlatformPreset.TWITCH)
        assertEquals(PlatformPreset.TWITCH, viewModel.destination.value.platform)
        assertEquals("saved-url", viewModel.destination.value.serverUrl)
        assertEquals("saved-key", viewModel.destination.value.streamKey)
        assertEquals(PlatformPreset.TWITCH, credentials.loadedPlatforms.last())

        viewModel.updateDestination(PlatformPreset.TWITCH, "rtmps://twitch.example/app", "new-key")
        assertEquals("rtmps://twitch.example/app", viewModel.destination.value.serverUrl)
        assertTrue(viewModel.destinations.value.first { it.platform == PlatformPreset.TWITCH }.isConfigured)
        assertEquals(Triple(PlatformPreset.TWITCH, "rtmps://twitch.example/app", "new-key"), credentials.saved.last())

        viewModel.clearDestination()
        assertEquals(PlatformPreset.TWITCH, credentials.cleared.last())
        assertFalse(viewModel.destination.value.isConfigured)
    }

    @Test
    fun sessionStateEmitsMeaningfulTransitions() = runTest {
        viewModel.session.test {
            assertEquals(StreamStatus.IDLE, awaitItem().status)
            viewModel.startPreparing()
            assertEquals(StreamStatus.PREPARING, awaitItem().status)
            viewModel.enterLive()
            assertEquals(StreamStatus.LIVE, awaitItem().status)
            viewModel.stopStream()
            assertEquals(StreamStatus.IDLE, awaitItem().status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun exposesApplicationConstructorForAndroidViewModelFactory() {
        StudioViewModel::class.java.getConstructor(Application::class.java)
    }

    @Test
    fun startupFallsBackWhenPersistenceRepositoriesFail() {
        val fallback = StudioViewModel(
            Application(),
            ThrowingCredentialRepository(),
            ThrowingSceneRepository(),
            FakeAdsPreferences(),
            ThrowingStreamQualityRepository(),
            ThrowingThermalProtectionRepository(),
            ThrowingAudioSettingsRepository(),
            ThrowingAutoStopRepository(),
            ThrowingLatencyModeRepository(),
        )

        assertEquals(StreamQualityPreset.BALANCED, fallback.streamQuality.value.preset)
        assertEquals(AudioQuality.STANDARD, fallback.audioSettings.value.quality)
        assertEquals(AutoStopDuration.OFF, fallback.autoStopDuration.value)
        assertEquals(LatencyMode.STABLE, fallback.latencyMode.value)
    }
}

private class FakeCredentialRepository : CredentialRepository {
    val values = mutableMapOf<PlatformPreset, Pair<String, String>>()
    val loadedPlatforms = mutableListOf<PlatformPreset>()
    val saved = mutableListOf<Triple<PlatformPreset, String, String>>()
    val cleared = mutableListOf<PlatformPreset>()

    override fun save(platform: PlatformPreset, serverUrl: String, streamKey: String) {
        saved += Triple(platform, serverUrl, streamKey)
        values[platform] = serverUrl to streamKey
    }

    override fun load(platform: PlatformPreset): Pair<String, String> {
        loadedPlatforms += platform
        return values[platform] ?: ("" to "")
    }

    override fun clear(platform: PlatformPreset) {
        cleared += platform
        values.remove(platform)
    }
}

private class FakeSceneRepository : SceneRepository {
    val saved = mutableListOf<List<Scene>>()

    override fun loadOrDefault(defaults: List<Scene>): List<Scene> = defaults

    override fun save(scenes: List<Scene>) {
        saved += scenes
    }
}

private class FakeStreamQualityRepository : StreamQualityRepository {
    var value = StreamQualityPreset.BALANCED.toQuality()

    override fun load(): StreamQuality = value

    override fun save(quality: StreamQuality) {
        value = quality
    }
}

private class FakeThermalProtectionRepository : ThermalProtectionRepository {
    private var enabledState = true

    override fun isEnabled(): Boolean = enabledState

    override fun setEnabled(enabled: Boolean) {
        enabledState = enabled
    }
}

private class FakeAudioSettingsRepository : AudioSettingsRepository {
    private var value = AudioSettings()

    override fun load(): AudioSettings = value

    override fun save(settings: AudioSettings) {
        value = settings
    }
}

private class FakeAutoStopRepository : AutoStopRepository {
    private var value = AutoStopDuration.OFF

    override fun load(): AutoStopDuration = value

    override fun save(duration: AutoStopDuration) {
        value = duration
    }
}

private class FakeLatencyModeRepository : LatencyModeRepository {
    private var value = LatencyMode.STABLE

    override fun load(): LatencyMode = value

    override fun save(mode: LatencyMode) {
        value = mode
    }
}

private class ThrowingCredentialRepository : CredentialRepository {
    override fun save(platform: PlatformPreset, serverUrl: String, streamKey: String) = Unit
    override fun load(platform: PlatformPreset): Pair<String, String> = error("simulated credential storage failure")
    override fun clear(platform: PlatformPreset) = Unit
}

private class ThrowingSceneRepository : SceneRepository {
    override fun loadOrDefault(defaults: List<Scene>): List<Scene> = error("simulated scene storage failure")
    override fun save(scenes: List<Scene>) = Unit
}

private class ThrowingStreamQualityRepository : StreamQualityRepository {
    override fun load(): StreamQuality = error("simulated quality storage failure")
    override fun save(quality: StreamQuality) = Unit
}

private class ThrowingThermalProtectionRepository : ThermalProtectionRepository {
    override fun isEnabled(): Boolean = error("simulated thermal storage failure")
    override fun setEnabled(enabled: Boolean) = Unit
}

private class ThrowingAudioSettingsRepository : AudioSettingsRepository {
    override fun load(): AudioSettings = error("simulated audio storage failure")
    override fun save(settings: AudioSettings) = Unit
}

private class ThrowingAutoStopRepository : AutoStopRepository {
    override fun load(): AutoStopDuration = error("simulated auto-stop storage failure")
    override fun save(duration: AutoStopDuration) = Unit
}

private class ThrowingLatencyModeRepository : LatencyModeRepository {
    override fun load(): LatencyMode = error("simulated latency storage failure")
    override fun save(mode: LatencyMode) = Unit
}

private class FakeAdsPreferences : AdsPreferencesRepository {
    private val state = kotlinx.coroutines.flow.MutableStateFlow(AdsPolicy())
    override val policy = state

    override fun setEnabled(enabled: Boolean) {
        state.value = state.value.copy(enabled = enabled, consentGranted = enabled)
    }
}
