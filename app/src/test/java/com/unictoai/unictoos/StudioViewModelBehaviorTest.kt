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
    private lateinit var viewModel: StudioViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        credentials = FakeCredentialRepository()
        scenes = FakeSceneRepository()
        ads = FakeAdsPreferences()
        viewModel = StudioViewModel(Application(), credentials, scenes, ads)
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

private class FakeAdsPreferences : AdsPreferencesRepository {
    private val state = kotlinx.coroutines.flow.MutableStateFlow(AdsPolicy())
    override val policy = state

    override fun setEnabled(enabled: Boolean) {
        state.value = state.value.copy(enabled = enabled, consentGranted = enabled)
    }
}
