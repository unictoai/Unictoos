package com.unictoai.unictoos.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.PlatformPreset
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.SourceType
import com.unictoai.unictoos.data.CreatorHistoryStore
import com.unictoai.unictoos.domain.StreamDestination
import com.unictoai.unictoos.domain.StreamHealthSample
import com.unictoai.unictoos.domain.StreamSessionState
import com.unictoai.unictoos.domain.StreamStatus
import com.unictoai.unictoos.ui.theme.UnictoosPalette
import com.unictoai.unictoos.ui.theme.UnictoosTheme
import com.unictoai.unictoos.DestinationConfig
import com.unictoai.unictoos.StudioViewModel
import com.unictoai.unictoos.ui.components.BrandHeader
import com.unictoai.unictoos.ui.components.AddSceneDialog
import com.unictoai.unictoos.ui.screens.EngagementScreen
import com.unictoai.unictoos.ui.screens.HomeScreen
import com.unictoai.unictoos.ui.screens.LibraryScreen
import com.unictoai.unictoos.ui.screens.MoreScreen
import com.unictoai.unictoos.ui.screens.ScenesScreen
import com.unictoai.unictoos.ui.screens.SettingsScreen
import com.unictoai.unictoos.ui.screens.StudioScreen

internal enum class AppTab(val label: String) {
    HOME("Home"),
    SCENES("Scenes"),
    STUDIO("Studio"),
    ENGAGEMENT("Engage"),
    LIBRARY("Library"),
    MORE("More"),
    SETTINGS("Settings"),
}

class MainActivity : ComponentActivity() {
    private var pendingEndpoint: String = ""
    private var pendingCaptureMode: String = CAPTURE_SCREEN
    private var pendingPractice: Boolean = false

    private val projectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) {
            pendingPractice = false
            return@registerForActivityResult
        }
        if (pendingCaptureMode == CAPTURE_CAMERA) {
            startCameraCapture()
            return@registerForActivityResult
        }
        val prepareIntent = Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_PREPARE_PROJECTION
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_RESULT_CODE, result.resultCode)
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_PROJECTION_DATA, result.data)
        }
        androidx.core.content.ContextCompat.startForegroundService(this, prepareIntent)
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = if (pendingPractice) {
                com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_START_PRACTICE
            } else {
                com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_START
            }
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_ENDPOINT, pendingEndpoint)
        })
        pendingPractice = false
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val audioGranted = androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val cameraGranted = pendingCaptureMode != CAPTURE_CAMERA || androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (audioGranted && cameraGranted) {
            if (pendingCaptureMode == CAPTURE_CAMERA) startCameraCapture() else launchProjection()
        } else {
            Toast.makeText(this, "Microphone access is required to go live with audio", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UnictoosTheme {
                UnictoosApp(
                    onRequestStreamStart = ::requestStreamStart,
                    onRequestPracticeStart = ::requestPracticeStart,
                    onStopStream = ::stopStream,
                    onToggleMute = ::toggleMute,
                    onToggleRecording = ::toggleRecording,
                    onCreateMarker = ::createMarker,
                )
            }
        }
    }

    internal fun requestStreamStart(endpoint: String, captureMode: String) {
        if (endpoint.isBlank()) {
            Toast.makeText(this, "Add a streaming destination before going live", Toast.LENGTH_LONG).show()
            return
        }
        pendingEndpoint = endpoint
        pendingCaptureMode = captureMode
        val needsAudio = androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        val needsCamera = captureMode == CAPTURE_CAMERA && androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        if (needsAudio || needsCamera) {
            val permissions = buildList {
                add(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
                if (needsCamera) add(Manifest.permission.CAMERA)
            }
            permissionLauncher.launch(permissions.toTypedArray())
        } else if (captureMode == CAPTURE_CAMERA) {
            startCameraCapture()
        } else {
            launchProjection()
        }
    }

    internal fun startCameraCapture() {
        androidx.core.content.ContextCompat.startForegroundService(this, Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_PREPARE_CAMERA
        })
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = if (pendingPractice) {
                com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_START_PRACTICE
            } else {
                com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_START
            }
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_ENDPOINT, pendingEndpoint)
        })
        pendingPractice = false
    }

    internal fun launchProjection() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    internal fun requestPracticeStart(captureMode: String) {
        pendingEndpoint = ""
        pendingCaptureMode = captureMode
        pendingPractice = true
        val needsAudio = androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        val needsCamera = captureMode == CAPTURE_CAMERA && androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        if (needsAudio || needsCamera) {
            val permissions = buildList {
                add(Manifest.permission.RECORD_AUDIO)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
                if (needsCamera) add(Manifest.permission.CAMERA)
            }
            permissionLauncher.launch(permissions.toTypedArray())
        } else if (captureMode == CAPTURE_CAMERA) {
            startCameraCapture()
        } else {
            launchProjection()
        }
    }

    internal fun stopStream() {
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_STOP
        })
    }

    internal fun toggleMute() {
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_TOGGLE_MUTE
        })
    }

    internal fun toggleRecording(recording: Boolean) {
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = if (recording) {
                com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_STOP_RECORDING
            } else {
                com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_START_RECORDING
            }
        })
    }

    internal fun createMarker() {
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_CREATE_MARKER
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_MARKER_LABEL, "Marked moment")
        })
    }

    companion object {
        private const val CAPTURE_SCREEN = "screen"
        private const val CAPTURE_CAMERA = "camera"
    }
}

@Composable
internal fun UnictoosApp(
    onRequestStreamStart: (String, String) -> Unit,
    onRequestPracticeStart: (String) -> Unit,
    onStopStream: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleRecording: (Boolean) -> Unit,
    onCreateMarker: () -> Unit,
    vm: StudioViewModel = viewModel(),
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var selectedSceneId by rememberSaveable { mutableStateOf("starting-soon") }
    var showAddScene by rememberSaveable { mutableStateOf(false) }
    val scenes by vm.scenes.collectAsStateWithLifecycle()
    val destinations by vm.destinations.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val healthHistory by vm.healthHistory.collectAsStateWithLifecycle()
    val destination by vm.destination.collectAsStateWithLifecycle()
    val adsPolicy by vm.adsPolicy.collectAsStateWithLifecycle()
    val selectedScene = scenes.firstOrNull { it.id == selectedSceneId } ?: scenes.firstOrNull() ?: Scene(
        id = "fallback",
        name = "Quick Start",
        aspectRatio = AspectRatio.PORTRAIT,
        sources = emptyList(),
    )

    Scaffold(
        containerColor = UnictoosPalette.Ink,
        bottomBar = {
            UnictoosBottomBar(selectedTab = selectedTab, onSelect = { selectedTab = it })
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                AppTab.HOME -> HomeScreen(
                    scenes = scenes,
                    destinations = destinations,
                    session = session,
                    onGoStudio = { selectedTab = AppTab.STUDIO },
                    onOpenScenes = { selectedTab = AppTab.SCENES },
                    onOpenLibrary = { selectedTab = AppTab.LIBRARY },
                    onOpenSettings = { selectedTab = AppTab.SETTINGS },
                    showAdSlot = adsPolicy.enabled && adsPolicy.consentGranted && session.status != StreamStatus.LIVE,
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
                    onOpenStudio = { selectedTab = AppTab.STUDIO },
                )
                AppTab.STUDIO -> StudioScreen(
                    scene = selectedScene,
                    session = session,
                    healthHistory = healthHistory,
                    destination = destination,
                        onStart = {
                            val captureMode = when {
                                selectedScene.sources.any { it.type == SourceType.SCREEN && it.enabled } -> "screen"
                                selectedScene.sources.any { it.type == SourceType.CAMERA && it.enabled } -> "camera"
                                else -> "screen"
                            }
                            onRequestStreamStart(destination.endpoint, captureMode)
                        },
                        onPractice = {
                            val captureMode = when {
                                selectedScene.sources.any { it.type == SourceType.SCREEN && it.enabled } -> "screen"
                                selectedScene.sources.any { it.type == SourceType.CAMERA && it.enabled } -> "camera"
                                else -> "screen"
                            }
                            onRequestPracticeStart(captureMode)
                        },
                        onStop = onStopStream,
                    onToggleMute = onToggleMute,
                    onToggleRecording = { onToggleRecording(session.recording) },
                    onCreateMarker = onCreateMarker,
                    onEditScenes = { selectedTab = AppTab.SCENES },
                    onOpenSettings = { selectedTab = AppTab.SETTINGS },
                )
                AppTab.ENGAGEMENT -> EngagementScreen()
                AppTab.LIBRARY -> LibraryScreen()
                AppTab.MORE -> MoreScreen(
                    onOpenEngage = { selectedTab = AppTab.ENGAGEMENT },
                    onOpenSettings = { selectedTab = AppTab.SETTINGS },
                )
                AppTab.SETTINGS -> SettingsScreen(
                    destination = destination,
                    onSelectPlatform = vm::selectDestination,
                    onSaveDestination = vm::updateDestination,
                    onClearDestination = vm::clearDestination,
                    adsEnabled = adsPolicy.enabled,
                    onAdsEnabledChange = vm::setAdsEnabled,
                )
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

internal fun AppTab.icon() = when (this) {
    AppTab.HOME -> Icons.Default.Home
    AppTab.SCENES -> Icons.Default.Dashboard
    AppTab.STUDIO -> Icons.Default.LiveTv
    AppTab.ENGAGEMENT -> Icons.AutoMirrored.Filled.Chat
    AppTab.LIBRARY -> Icons.Default.Movie
    AppTab.MORE -> Icons.Default.Tune
    AppTab.SETTINGS -> Icons.Default.Settings
}
