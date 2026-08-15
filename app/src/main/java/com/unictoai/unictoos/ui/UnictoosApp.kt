package com.unictoai.unictoos.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.Surface
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unictoai.unictoos.StudioViewModel
import com.unictoai.unictoos.StudioViewModelFactory
import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.SourceType
import com.unictoai.unictoos.domain.StreamStatus
import com.unictoai.unictoos.DestinationConfig
import com.unictoai.unictoos.domain.AudioSettings
import com.unictoai.unictoos.domain.AutoStopDuration
import com.unictoai.unictoos.domain.StreamDestination
import com.unictoai.unictoos.domain.StreamQuality
import com.unictoai.unictoos.monetization.AdsPolicy
import com.unictoai.unictoos.ui.components.AddSceneDialog
import com.unictoai.unictoos.ui.screens.EngagementScreen
import com.unictoai.unictoos.ui.screens.HomeScreen
import com.unictoai.unictoos.ui.screens.LibraryScreen
import com.unictoai.unictoos.ui.screens.MoreScreen
import com.unictoai.unictoos.ui.screens.ScenesScreen
import com.unictoai.unictoos.ui.screens.SettingsScreen
import com.unictoai.unictoos.ui.screens.StudioScreen
import com.unictoai.unictoos.ui.theme.UnictoosPalette

internal enum class AppTab(val label: String) {
    HOME("Home"),
    SCENES("Scenes"),
    STUDIO("Studio"),
    ENGAGEMENT("Engage"),
    LIBRARY("Library"),
    MORE("More"),
    SETTINGS("Settings"),
}

internal fun AppTab.icon() = when (this) {
    AppTab.HOME -> Icons.Default.Home
    AppTab.SCENES -> Icons.Default.Dashboard
    AppTab.STUDIO -> Icons.Default.LiveTv
    AppTab.ENGAGEMENT -> Icons.AutoMirrored.Filled.Chat
    AppTab.LIBRARY -> Icons.Default.Movie
    AppTab.MORE -> Icons.Default.Tune
    AppTab.SETTINGS -> Icons.Default.Settings
}

private val studioViewModelFactory = StudioViewModelFactory()

@Composable
internal fun UnictoosApp(
    onRequestStreamStart: (String, String, String) -> Unit,
    onRequestPracticeStart: (String, String) -> Unit,
    onStopStream: () -> Unit,
    onReleaseCapture: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleRecording: (Boolean) -> Unit,
    onCreateMarker: () -> Unit,
    onDismissStatusMessage: () -> Unit,
    onShareConfig: (String) -> Unit,
    onPreviewSurfaceAvailable: (Surface, Int, Int) -> Unit,
    onPreviewSurfaceDestroyed: (Surface) -> Unit,
    vm: StudioViewModel = viewModel(factory = studioViewModelFactory),
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var selectedSceneId by rememberSaveable { mutableStateOf("starting-soon") }
    var showAddScene by rememberSaveable { mutableStateOf(false) }
    val scenes by vm.scenes.collectAsStateWithLifecycle()
    val destinations by vm.destinations.collectAsStateWithLifecycle()
    val destination by vm.destination.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val adsPolicy by vm.adsPolicy.collectAsStateWithLifecycle()
    val streamQuality by vm.streamQuality.collectAsStateWithLifecycle()
    val thermalProtectionEnabled by vm.thermalProtectionEnabled.collectAsStateWithLifecycle()
    val audioSettings by vm.audioSettings.collectAsStateWithLifecycle()
    val autoStopDuration by vm.autoStopDuration.collectAsStateWithLifecycle()
    val latencyMode by vm.latencyMode.collectAsStateWithLifecycle()
    val selectedScene = remember(scenes, selectedSceneId) {
        scenes.firstOrNull { it.id == selectedSceneId } ?: scenes.firstOrNull() ?: Scene(
        id = "fallback",
        name = "Quick Start",
        aspectRatio = AspectRatio.PORTRAIT,
            sources = emptyList(),
        )
    }
    val context = LocalContext.current
    var showOnboarding by remember {
        mutableStateOf(!context.getSharedPreferences(ONBOARDING_PREFERENCES, android.content.Context.MODE_PRIVATE).getBoolean(ONBOARDING_COMPLETE, false))
    }
    if (showOnboarding) {
        OnboardingScreen(
            onFinished = {
                context.getSharedPreferences(ONBOARDING_PREFERENCES, android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(ONBOARDING_COMPLETE, true)
                    .apply()
                showOnboarding = false
            },
        )
        return
    }

    Scaffold(
        containerColor = UnictoosPalette.Ink,
        bottomBar = {
            UnictoosBottomBar(selectedTab = selectedTab, onSelect = { selectedTab = it })
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                label = "tabTransition",
            ) { tab ->
            when (tab) {
                AppTab.HOME -> HomeRoute(
                    vm = vm,
                    scenes = scenes,
                    destinations = destinations,
                    adsPolicy = adsPolicy,
                    onGoStudio = { selectedTab = AppTab.STUDIO },
                    onOpenScenes = { selectedTab = AppTab.SCENES },
                    onOpenLibrary = { selectedTab = AppTab.LIBRARY },
                    onOpenSettings = { selectedTab = AppTab.SETTINGS },
                )
                AppTab.SCENES -> ScenesScreen(
                    scenes = scenes,
                    selectedSceneId = selectedSceneId,
                    selectedScene = selectedScene,
                    onSelect = { selectedSceneId = it },
                    onAdd = { showAddScene = true },
                    onToggleSource = vm::toggleSource,
                    onAddSource = vm::addSource,
                    onMoveSource = vm::moveSource,
                    onSetSourceOpacity = vm::setSourceOpacity,
                    onSetSourceGeometry = vm::setSourceGeometry,
                    onUpdateTextSource = vm::updateTextSource,
                    onOpenStudio = { selectedTab = AppTab.STUDIO },
                )
                AppTab.STUDIO -> StudioRoute(
                    vm = vm,
                    scene = selectedScene,
                    destination = destination,
                    streamQuality = streamQuality,
                    audioSettings = audioSettings,
                    autoStopDuration = autoStopDuration,
                    onAspectRatioChange = { ratio -> vm.setSceneAspectRatio(selectedScene.id, ratio) },
                    onRequestStreamStart = onRequestStreamStart,
                    onRequestPracticeStart = onRequestPracticeStart,
                    onStopStream = onStopStream,
                    onReleaseCapture = onReleaseCapture,
                    onToggleMute = onToggleMute,
                    onToggleRecording = onToggleRecording,
                    onCreateMarker = onCreateMarker,
                    onDismissStatusMessage = onDismissStatusMessage,
                    onEditScenes = { selectedTab = AppTab.SCENES },
                    onOpenSettings = { selectedTab = AppTab.SETTINGS },
                    onPreviewSurfaceAvailable = onPreviewSurfaceAvailable,
                    onPreviewSurfaceDestroyed = onPreviewSurfaceDestroyed,
                )
                AppTab.ENGAGEMENT -> EngagementScreen(onOpenSettings = { selectedTab = AppTab.SETTINGS })
                AppTab.LIBRARY -> LibraryScreen(onOpenStudio = { selectedTab = AppTab.STUDIO })
                AppTab.MORE -> MoreScreen(
                    onOpenEngage = { selectedTab = AppTab.ENGAGEMENT },
                    onOpenSettings = { selectedTab = AppTab.SETTINGS },
                    onReplayOnboarding = { showOnboarding = true },
                )
                AppTab.SETTINGS -> SettingsScreen(
                    destination = destination,
                    sessionStatus = session.status,
                    onSelectPlatform = vm::selectDestination,
                    onSaveDestination = vm::updateDestination,
                    onClearDestination = vm::clearDestination,
                    adsEnabled = adsPolicy.enabled,
                    onAdsEnabledChange = vm::setAdsEnabled,
                    streamQuality = streamQuality,
                    onStreamQualityPreset = vm::setStreamQualityPreset,
                    onCustomStreamQualityChange = vm::updateCustomStreamQuality,
                    thermalProtectionEnabled = thermalProtectionEnabled,
                    onThermalProtectionChange = vm::setThermalProtectionEnabled,
                    audioSettings = audioSettings,
                    onAudioQualityChange = vm::setAudioQuality,
                    onEchoCancelerChange = vm::setEchoCanceler,
                    onNoiseSuppressorChange = vm::setNoiseSuppressor,
                    latencyMode = latencyMode,
                    onLatencyModeChange = vm::setLatencyMode,
                    onExportConfig = { onShareConfig(vm.exportConfigJson()) },
                )
            }
            }
        }
    }

    if (showAddScene) {
        AddSceneDialog(
            onDismiss = { showAddScene = false },
            onCreate = { name, ratio ->
                vm.addScene(name, ratio)
                showAddScene = false
            },
        )
    }
}

@Composable
private fun HomeRoute(
    vm: StudioViewModel,
    scenes: List<Scene>,
    destinations: List<StreamDestination>,
    adsPolicy: AdsPolicy,
    onGoStudio: () -> Unit,
    onOpenScenes: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val session by vm.session.collectAsStateWithLifecycle()
    HomeScreen(
        scenes = scenes,
        destinations = destinations,
        session = session,
        onGoStudio = onGoStudio,
        onOpenScenes = onOpenScenes,
        onOpenLibrary = onOpenLibrary,
        onOpenSettings = onOpenSettings,
        showAdSlot = adsPolicy.enabled && adsPolicy.consentGranted && session.status != StreamStatus.LIVE,
    )
}

@Composable
private fun StudioRoute(
    vm: StudioViewModel,
    scene: Scene,
    destination: DestinationConfig,
    streamQuality: StreamQuality,
    audioSettings: AudioSettings,
    autoStopDuration: AutoStopDuration,
    onAspectRatioChange: (AspectRatio) -> Unit,
    onRequestStreamStart: (String, String, String) -> Unit,
    onRequestPracticeStart: (String, String) -> Unit,
    onStopStream: () -> Unit,
    onReleaseCapture: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleRecording: (Boolean) -> Unit,
    onCreateMarker: () -> Unit,
    onDismissStatusMessage: () -> Unit,
    onEditScenes: () -> Unit,
    onOpenSettings: () -> Unit,
    onPreviewSurfaceAvailable: (Surface, Int, Int) -> Unit,
    onPreviewSurfaceDestroyed: (Surface) -> Unit,
) {
    val session by vm.session.collectAsStateWithLifecycle()
    val healthHistory by vm.healthHistory.collectAsStateWithLifecycle()
    val captureMode = remember(scene) {
        when {
            scene.sources.any { it.type == SourceType.SCREEN && it.enabled } -> "screen"
            scene.sources.any { it.type == SourceType.CAMERA && it.enabled } -> "camera"
            else -> "screen"
        }
    }
    StudioScreen(
        scene = scene,
        session = session,
        healthHistory = healthHistory,
        destination = destination,
        streamQuality = streamQuality,
        audioSettings = audioSettings,
        autoStopDuration = autoStopDuration,
        onAutoStopDurationChange = vm::setAutoStopDuration,
        onAspectRatioChange = onAspectRatioChange,
        onStart = { onRequestStreamStart(destination.endpoint, captureMode, com.unictoai.unictoos.streaming.ScenePayloadCodec.encode(scene)) },
        onPractice = { onRequestPracticeStart(captureMode, com.unictoai.unictoos.streaming.ScenePayloadCodec.encode(scene)) },
        onStop = onStopStream,
        onToggleMute = onToggleMute,
        onToggleRecording = { onToggleRecording(session.recording) },
        onCreateMarker = onCreateMarker,
        onDismissStatusMessage = onDismissStatusMessage,
        onEditScenes = onEditScenes,
        onOpenSettings = onOpenSettings,
        onReleaseCapture = onReleaseCapture,
        onPreviewSurfaceAvailable = onPreviewSurfaceAvailable,
        onPreviewSurfaceDestroyed = onPreviewSurfaceDestroyed,
    )
}

private const val ONBOARDING_PREFERENCES = "unictoos_onboarding"
private const val ONBOARDING_COMPLETE = "complete"

@Composable
internal fun UnictoosBottomBar(selectedTab: AppTab, onSelect: (AppTab) -> Unit) {
    NavigationBar(
        modifier = Modifier.animateContentSize(tween(220)),
        containerColor = UnictoosPalette.InkSoft,
        tonalElevation = 8.dp,
    ) {
        listOf(AppTab.HOME, AppTab.STUDIO, AppTab.SCENES, AppTab.LIBRARY, AppTab.MORE).forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon(), contentDescription = tab.label) },
                label = { Text(tab.label, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = UnictoosPalette.TextPrimary,
                    indicatorColor = UnictoosPalette.Violet.copy(alpha = 0.24f),
                    unselectedIconColor = UnictoosPalette.TextMuted,
                    unselectedTextColor = UnictoosPalette.TextMuted,
                ),
            )
        }
    }
}

