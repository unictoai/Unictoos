package com.unictoai.unictoos

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
import com.unictoai.unictoos.domain.StreamDestination
import com.unictoai.unictoos.domain.StreamHealthSample
import com.unictoai.unictoos.domain.StreamSessionState
import com.unictoai.unictoos.domain.StreamStatus
import com.unictoai.unictoos.ui.theme.UnictoosPalette
import com.unictoai.unictoos.ui.theme.UnictoosTheme

private enum class AppTab(val label: String) {
    HOME("Home"),
    SCENES("Scenes"),
    STUDIO("Studio"),
    ENGAGEMENT("Engage"),
    LIBRARY("Library"),
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
                )
            }
        }
    }

    private fun requestStreamStart(endpoint: String, captureMode: String) {
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

    private fun startCameraCapture() {
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

    private fun launchProjection() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun requestPracticeStart(captureMode: String) {
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

    private fun stopStream() {
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_STOP
        })
    }

    private fun toggleMute() {
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_TOGGLE_MUTE
        })
    }

    private fun toggleRecording(recording: Boolean) {
        startService(Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = if (recording) {
                com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_STOP_RECORDING
            } else {
                com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_START_RECORDING
            }
        })
    }

    companion object {
        private const val CAPTURE_SCREEN = "screen"
        private const val CAPTURE_CAMERA = "camera"
    }
}

@Composable
private fun UnictoosApp(
    onRequestStreamStart: (String, String) -> Unit,
    onRequestPracticeStart: (String) -> Unit,
    onStopStream: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleRecording: (Boolean) -> Unit,
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
                    onEditScenes = { selectedTab = AppTab.SCENES },
                    onOpenSettings = { selectedTab = AppTab.SETTINGS },
                )
                AppTab.ENGAGEMENT -> EngagementScreen()
                AppTab.LIBRARY -> LibraryScreen()
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
private fun UnictoosBottomBar(selectedTab: AppTab, onSelect: (AppTab) -> Unit) {
    NavigationBar(
        modifier = Modifier.animateContentSize(tween(220)),
        containerColor = UnictoosPalette.InkSoft,
        tonalElevation = 8.dp,
    ) {
        listOf(AppTab.HOME, AppTab.SCENES, AppTab.STUDIO, AppTab.ENGAGEMENT, AppTab.LIBRARY, AppTab.SETTINGS).forEach { tab ->
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

private fun AppTab.icon() = when (this) {
    AppTab.HOME -> Icons.Default.Home
    AppTab.SCENES -> Icons.Default.Dashboard
    AppTab.STUDIO -> Icons.Default.LiveTv
    AppTab.ENGAGEMENT -> Icons.AutoMirrored.Filled.Chat
    AppTab.LIBRARY -> Icons.Default.Movie
    AppTab.SETTINGS -> Icons.Default.Settings
}

@Composable
private fun BrandHeader(
    eyebrow: String,
    title: String,
    action: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(com.unictoai.unictoos.R.drawable.logo_unictoos),
                contentDescription = "Unictoos logo",
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = UnictoosPalette.Cyan, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
        action?.invoke()
    }
}

@Composable
private fun HomeScreen(
    scenes: List<Scene>,
    destinations: List<StreamDestination>,
    session: StreamSessionState,
    onGoStudio: () -> Unit,
    onOpenScenes: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    showAdSlot: Boolean = false,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { BrandHeader("Creator workspace", "Your broadcast desk") { StatusPill(session.status) } }
        if (session.status == StreamStatus.ERROR) item { SessionErrorCard(session.message.orEmpty(), onGoStudio) }
        if (showAdSlot) item { SponsorBanner() }
        item { ExecutiveHero(session = session, onOpenStudio = onGoStudio) }
        item { PreflightCard() }
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(380)) + slideInVertically(tween(380)) { it / 5 },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader("Broadcast readiness", "A quick check before you go live")
                    ReadinessGrid(
                        scenesReady = scenes.isNotEmpty(),
                        sceneValue = "${scenes.size} ready",
                        destinationReady = destinations.any { it.isConfigured },
                        destinationValue = configuredDestinationLabel(destinations),
                        microphoneReady = session.status != StreamStatus.ERROR,
                        microphoneValue = if (session.message?.contains("Microphone", true) == true) "Ready to test" else "Checked before live",
                    )
                }
            }
        }
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(440, delayMillis = 90)) + slideInVertically(tween(440, delayMillis = 90)) { it / 6 },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader("Quick actions", "Keep your setup within one tap")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAction(Icons.Default.Dashboard, "Scenes", "Build a layout", onOpenScenes, Modifier.weight(1f))
                        QuickAction(Icons.Default.Tune, "Destinations", "Manage keys", onOpenSettings, Modifier.weight(1f))
                        QuickAction(Icons.Default.Movie, "Library", "View recordings", onOpenLibrary, Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            AnimatedVisibility(
                visible = scenes.isNotEmpty(),
                enter = fadeIn(tween(500, delayMillis = 150)) + slideInVertically(tween(500, delayMillis = 150)) { it / 7 },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SectionHeader("Your scenes", "Layouts ready to launch")
                        TextButton(onClick = onOpenScenes) { Text("View all") }
                    }
                    scenes.take(2).forEach { scene -> SceneCard(scene) }
                }
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = UnictoosPalette.Surface.copy(alpha = 0.68f),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = UnictoosPalette.Mint, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Your stream keys stay encrypted on this device.", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun configuredDestinationLabel(destinations: List<StreamDestination>): String =
    destinations.firstOrNull { it.isConfigured }?.name ?: "Add a destination"

@Composable
private fun ExecutiveHero(session: StreamSessionState, onOpenStudio: () -> Unit) {
    val isLive = session.status == StreamStatus.LIVE
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(tween(320, easing = FastOutSlowInEasing)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = UnictoosPalette.Surface),
        border = BorderStroke(1.dp, UnictoosPalette.Stroke.copy(alpha = 0.65f)),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(if (isLive) "BROADCAST IN PROGRESS" else "BROADCAST READINESS", style = MaterialTheme.typography.labelMedium, color = if (isLive) UnictoosPalette.Mint else UnictoosPalette.Cyan, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    AnimatedContent(targetState = isLive, label = "heroTitle") { live ->
                        Text(if (live) "You are live." else "Ready when you are.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(color = (if (isLive) UnictoosPalette.Mint else UnictoosPalette.Cyan).copy(alpha = 0.12f), shape = RoundedCornerShape(16.dp)) {
                    Icon(if (isLive) Icons.Default.FiberManualRecord else Icons.Default.Bolt, contentDescription = null, tint = if (isLive) UnictoosPalette.Mint else UnictoosPalette.Cyan, modifier = Modifier.padding(12.dp).size(22.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLive) LivePulseDot() else Box(Modifier.size(9.dp).clip(RoundedCornerShape(50)).background(UnictoosPalette.Mint))
                Spacer(Modifier.width(9.dp))
                Text(if (isLive) "Session is active" else "All essential checks are ready", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onOpenStudio,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isLive) UnictoosPalette.Danger else Color.White, contentColor = if (isLive) Color.White else UnictoosPalette.Ink),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(if (isLive) Icons.Default.LiveTv else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(9.dp))
                Text(if (isLive) "Open live studio" else "Open broadcast studio", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun LivePulseDot() {
    val transition = rememberInfiniteTransition(label = "livePulse")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "livePulseAlpha",
    )
    Box(Modifier.size(9.dp).clip(RoundedCornerShape(50)).background(UnictoosPalette.Mint.copy(alpha = alpha)))
}

@Composable
private fun ReadinessGrid(
    scenesReady: Boolean,
    sceneValue: String,
    destinationReady: Boolean,
    destinationValue: String,
    microphoneReady: Boolean,
    microphoneValue: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReadinessCard(Icons.Default.Dashboard, "Scenes", sceneValue, scenesReady, Modifier.weight(1f))
            ReadinessCard(Icons.Default.Wifi, "Network", "Checked before live", true, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReadinessCard(Icons.Default.Tune, "Destination", destinationValue, destinationReady, Modifier.weight(1f))
            ReadinessCard(Icons.Default.Mic, "Microphone", microphoneValue, microphoneReady, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReadinessCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, ready: Boolean, modifier: Modifier) {
    Card(
        modifier = modifier.animateContentSize(tween(240)),
        colors = CardDefaults.cardColors(containerColor = UnictoosPalette.SurfaceRaised),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = if (ready) UnictoosPalette.Mint else UnictoosPalette.Amber, modifier = Modifier.size(18.dp))
                Icon(if (ready) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = if (ready) UnictoosPalette.Mint else UnictoosPalette.Amber, modifier = Modifier.size(17.dp))
            }
            Text(label, color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.labelMedium)
            Text(value, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.animateContentSize(tween(220)),
        colors = CardDefaults.cardColors(containerColor = UnictoosPalette.SurfaceRaised),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Surface(color = UnictoosPalette.Violet.copy(alpha = 0.16f), shape = RoundedCornerShape(12.dp)) {
                Icon(icon, null, tint = UnictoosPalette.VioletBright, modifier = Modifier.padding(8.dp).size(20.dp))
            }
            Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ScenesScreen(
    scenes: List<Scene>,
    selectedSceneId: String,
    selectedScene: Scene,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onToggleSource: (String, String) -> Unit,
    onAddSource: (String, String, SourceType) -> Unit,
    onMoveSource: (String, String, Int) -> Unit,
    onSetSourceOpacity: (String, String, Float) -> Unit,
    onOpenStudio: () -> Unit,
) {
    var showAddSource by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        BrandHeader("Your layouts", "Scenes") {
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, "Create scene", tint = UnictoosPalette.Cyan)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Build a repeatable look for every kind of broadcast.", color = UnictoosPalette.TextMuted)
        Spacer(Modifier.height(18.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(scenes, key = { it.id }) { scene ->
                SceneCard(scene, selected = scene.id == selectedSceneId, onClick = { onSelect(scene.id) }, onOpenStudio = onOpenStudio)
            }
            item {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader("Sources in ${selectedScene.name}", "Tap a source to include or hide it")
                    TextButton(onClick = { showAddSource = true }) { Text("Add source") }
                }
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.Surface)) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        selectedScene.sources.forEach { source ->
                            SourceToggleRow(
                                title = source.name,
                                type = "${source.type.label} • layer ${source.zIndex + 1}",
                                enabled = source.enabled,
                                onClick = { onToggleSource(selectedScene.id, source.id) },
                                onMoveUp = { onMoveSource(selectedScene.id, source.id, -1) },
                                onMoveDown = { onMoveSource(selectedScene.id, source.id, 1) },
                                opacity = source.opacity,
                                onOpacityChange = { onSetSourceOpacity(selectedScene.id, source.id, it) },
                            )
                        }
                    }
                }
            }
        }
    }
    if (showAddSource) {
        AddSourceDialog(
            onDismiss = { showAddSource = false },
            onCreate = { name, type ->
                onAddSource(selectedScene.id, name, type)
                showAddSource = false
            },
        )
    }
}

@Composable
private fun StudioScreen(
    scene: Scene,
    session: StreamSessionState,
    healthHistory: List<StreamHealthSample>,
    destination: DestinationConfig,
    onStart: () -> Unit,
    onPractice: () -> Unit,
    onStop: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleRecording: () -> Unit,
    onEditScenes: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            BrandHeader("Broadcast workspace", "Studio") { StatusPill(session.status) }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(scene.name, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text("•", color = UnictoosPalette.TextMuted)
                Spacer(Modifier.width(8.dp))
                Text(scene.aspectRatio.label, color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Box(
                Modifier.fillMaxWidth().height(330.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xFF111417)).animateContentSize(tween(280)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(
                        painter = painterResource(com.unictoai.unictoos.R.drawable.logo_unictoos),
                        contentDescription = "Unictoos preview mark",
                        modifier = Modifier.size(88.dp).clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Text("LIVE PREVIEW", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Text("${scene.sources.count { it.enabled }} active sources  •  ${scene.aspectRatio.ratio}", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
                    AnimatedVisibility(
                        visible = session.status == StreamStatus.PREPARING || session.status == StreamStatus.RECONNECTING,
                        enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 4 },
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(Modifier.fillMaxWidth(0.6f), color = UnictoosPalette.Cyan, trackColor = Color.White.copy(alpha = 0.14f))
                            Text(session.message.orEmpty(), color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Surface(
                    Modifier.align(Alignment.TopStart).padding(14.dp),
                    color = if (session.status == StreamStatus.LIVE) UnictoosPalette.Magenta else Color.White.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(if (session.status == StreamStatus.LIVE) "LIVE" else "PREVIEW", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (session.status == StreamStatus.ERROR) item { SessionErrorCard(session.message.orEmpty(), onOpenSettings) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Bitrate", if (session.bitrateKbps > 0) "${session.bitrateKbps} kbps" else "—", Modifier.weight(1f))
                MetricCard("FPS", if (session.fps > 0) session.fps.toString() else "—", Modifier.weight(1f))
                val audioLabel = when {
                    session.status == StreamStatus.LIVE -> "Mic live"
                    session.message?.contains("Microphone", true) == true -> "Mic ready"
                    session.status == StreamStatus.ERROR -> "Check mic"
                    else -> "Not checked"
                }
                MetricCard("Audio", audioLabel, Modifier.weight(1f))
            }
        }
        item {
            HealthCenterCard(history = healthHistory, session = session)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.Surface)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (destination.isConfigured) Icons.Default.Wifi else Icons.Default.Warning, null, tint = if (destination.isConfigured) UnictoosPalette.Mint else UnictoosPalette.Amber)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (destination.isConfigured) "${destination.platform.label} is ready" else "No destination connected", fontWeight = FontWeight.SemiBold)
                        Text(if (destination.isConfigured) "Your key is stored securely on this device" else "Add a YouTube, Twitch, Kick, or custom RTMP destination", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!destination.isConfigured) TextButton(onClick = onOpenSettings) { Text("Set up") }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onToggleMute, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Icon(if (session.microphoneMuted) Icons.Default.MicOff else Icons.Default.Mic, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (session.microphoneMuted) "Unmute" else "Mute")
                }
                OutlinedButton(onClick = onToggleRecording, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Movie, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (session.recording) "Stop record" else "Record")
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onEditScenes, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Dashboard, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Edit scene")
                }
                OutlinedButton(onClick = onPractice, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), enabled = session.status == StreamStatus.IDLE || session.status == StreamStatus.ERROR) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Practice")
                }
                Button(
                    onClick = if (session.status == StreamStatus.LIVE) onStop else onStart,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (session.status == StreamStatus.LIVE) UnictoosPalette.Danger else UnictoosPalette.Magenta),
                ) {
                    Icon(if (session.status == StreamStatus.LIVE) Icons.Default.Stop else Icons.Default.FiberManualRecord, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (session.status == StreamStatus.LIVE) "Stop" else "Go live")
                }
            }
        }
    }
}

@Composable
private fun HealthCenterCard(history: List<StreamHealthSample>, session: StreamSessionState) {
    val latest = history.lastOrNull()
    val thermalLabel = when (latest?.thermalStatus) {
        android.os.PowerManager.THERMAL_STATUS_LIGHT -> "Warm"
        android.os.PowerManager.THERMAL_STATUS_MODERATE -> "Hot"
        android.os.PowerManager.THERMAL_STATUS_SEVERE, android.os.PowerManager.THERMAL_STATUS_CRITICAL, android.os.PowerManager.THERMAL_STATUS_EMERGENCY, android.os.PowerManager.THERMAL_STATUS_SHUTDOWN -> "Reduce quality"
        else -> "Normal"
    }
    Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.SurfaceRaised), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Health center", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(if (session.mode.name == "PRACTICE") "Local rehearsal diagnostics" else "Live session diagnostics", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = UnictoosPalette.Cyan)
            }
            if (latest == null) {
                Text("Health telemetry appears here once a session is active.", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Bitrate", "${latest.bitrateKbps} kbps", Modifier.weight(1f))
                    MetricCard("FPS", latest.fps.toString(), Modifier.weight(1f))
                    MetricCard("Drops", latest.droppedFrames.toString(), Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Network  ${latest.networkLabel}", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
                    Text("Battery  ${if (latest.batteryPercent >= 0) "${latest.batteryPercent}%" else "—"}", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
                    Text("Thermal  $thermalLabel", color = if (thermalLabel == "Normal") UnictoosPalette.Mint else UnictoosPalette.Amber, style = MaterialTheme.typography.bodySmall)
                }
                Text("${history.size} samples retained locally for this session", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PreflightCard() {
    val context = LocalContext.current
    val audioReady = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val cameraReady = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val networkReady = context.getSystemService(android.net.ConnectivityManager::class.java)?.activeNetwork != null
    Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.SurfaceRaised), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Preflight check", fontWeight = FontWeight.Bold); Text("Know what is ready before you start", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall) }
                Icon(Icons.Default.Wifi, null, tint = if (networkReady) UnictoosPalette.Mint else UnictoosPalette.Amber)
            }
            ReadinessRow("Network", if (networkReady) "Connected" else "Check connection", networkReady)
            ReadinessRow("Microphone", if (audioReady) "Permission granted" else "Permission needed", audioReady)
            ReadinessRow("Camera", if (cameraReady) "Permission granted" else "Requested for camera scenes", cameraReady)
            Text("Screen capture is requested by Android each time you begin a screen broadcast.", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EngagementScreen() {
    var selectedChannel by rememberSaveable { mutableStateOf("All") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            BrandHeader("Community control", "Engage") { StatusPill(StreamStatus.IDLE) }
            Spacer(Modifier.height(6.dp))
            Text("Bring chat, alerts, and creator actions into one calm mobile workspace.", color = UnictoosPalette.TextMuted)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.SurfaceRaised), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Unified inbox", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Chat, events, and alerts in one view", color = UnictoosPalette.TextMuted)
                        }
                        Icon(Icons.AutoMirrored.Filled.Chat, null, tint = UnictoosPalette.Cyan, modifier = Modifier.size(30.dp))
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("All", "YouTube", "Twitch", "Kick")) { channel ->
                            FilterChip(selected = selectedChannel == channel, onClick = { selectedChannel = channel }, label = { Text(channel) })
                        }
                    }
                    Text("No accounts connected", color = UnictoosPalette.Amber, fontWeight = FontWeight.SemiBold)
                    Text("Connect OAuth accounts to read chat and events. Stream keys remain separate and are never used as chat credentials.", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { SectionHeader("Platform integrations", "OAuth is kept separate from stream keys") }
        items(PlatformPreset.values().toList()) { platform -> IntegrationCard(platform) }
        item {
            SectionHeader("Events and alerts", "Ready for provider adapters")
            Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.Surface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReadinessRow("Follows, subscriptions, cheers, raids", "Event connection", false)
                    ReadinessRow("Pinned chat and quick replies", "Explicit send scope", false)
                    ReadinessRow("Clips and stream markers", "Platform API", false)
                }
            }
        }
        item {
            SectionHeader("Moderation desk", "Every action requires an explicit provider permission")
            Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.Surface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Moderation stays out of the media path", fontWeight = FontWeight.SemiBold)
                    Text("Blocked terms, AutoMod review, timeouts, bans, slow mode, shield mode, and message deletion will be connected only after OAuth scopes and audit logging are in place.", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
                    Text("No provider actions are available while disconnected.", color = UnictoosPalette.Amber, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun IntegrationCard(platform: PlatformPreset) {
    Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.Surface), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = UnictoosPalette.Violet.copy(alpha = 0.20f), shape = RoundedCornerShape(12.dp)) { Text(platform.label.take(1), Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = UnictoosPalette.VioletBright, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(platform.label, fontWeight = FontWeight.SemiBold)
                Text(if (platform == PlatformPreset.CUSTOM) "Custom RTMP only" else "Stream key works now • OAuth tools are separate", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text("READY".takeIf { platform != PlatformPreset.CUSTOM } ?: "MANUAL", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LibraryScreen() {
    val context = LocalContext.current
    val recordingsDirectory = java.io.File(context.filesDir, "recordings")
    var recordings by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var renameTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var renameValue by rememberSaveable { mutableStateOf("") }

    fun refresh() {
        recordings = recordingsDirectory.listFiles()
            ?.filter { it.extension.equals("mp4", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.name }
            .orEmpty()
    }

    fun contentUri(name: String): android.net.Uri? {
        val file = java.io.File(recordingsDirectory, name)
        return if (file.exists()) runCatching {
            androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull() else null
    }

    fun play(name: String) {
        contentUri(name)?.let { uri ->
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/mp4")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            }
        }
    }

    fun share(name: String) {
        contentUri(name)?.let { uri ->
            runCatching {
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Share recording"))
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            BrandHeader("Your content", "Library")
            Spacer(Modifier.height(6.dp))
            Text("Recordings stay on this device until you choose to share them.", color = UnictoosPalette.TextMuted)
        }
        if (recordings.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.SurfaceRaised), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Movie, null, tint = UnictoosPalette.VioletBright, modifier = Modifier.size(32.dp))
                        Text("No recordings yet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text("Press Record in Studio or start Practice mode. Saved MP4 sessions will appear here.", color = UnictoosPalette.TextMuted)
                    }
                }
            }
        } else {
            item { SectionHeader("Saved recordings", "Stored locally on this device") }
            items(recordings, key = { it }) { name ->
                Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.Surface), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = UnictoosPalette.Violet.copy(alpha = 0.16f), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Movie, null, tint = UnictoosPalette.Cyan, modifier = Modifier.padding(9.dp).size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name.removeSuffix(".mp4"), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("MP4 • app-private storage", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { play(name) }) { Icon(Icons.Default.PlayArrow, "Play recording", tint = UnictoosPalette.Cyan) }
                        IconButton(onClick = { share(name) }) { Icon(Icons.Default.Share, "Share recording", tint = UnictoosPalette.Cyan) }
                        IconButton(onClick = { renameTarget = name; renameValue = name.removeSuffix(".mp4") }) { Icon(Icons.Default.Edit, "Rename recording", tint = UnictoosPalette.TextMuted) }
                        IconButton(onClick = { java.io.File(recordingsDirectory, name).delete(); refresh() }) { Icon(Icons.Default.Delete, "Delete recording", tint = UnictoosPalette.Danger) }
                    }
                }
            }
        }
        item {
            SectionHeader("Creator assets", "Overlays, intros, and saved scene media")
            ReadinessRow("Scene templates", "Available", true)
            ReadinessRow("Overlay library", "Next milestone", false)
            ReadinessRow("Cloud backup", "Not connected", false)
        }
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename recording") },
            text = { OutlinedTextField(value = renameValue, onValueChange = { renameValue = it }, label = { Text("Recording name") }, singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    val old = renameTarget
                    val safe = renameValue.trim().ifBlank { old?.removeSuffix(".mp4").orEmpty() }.replace(Regex("[^A-Za-z0-9 _-]"), "_")
                    if (old != null && safe.isNotBlank()) java.io.File(recordingsDirectory, old).renameTo(java.io.File(recordingsDirectory, "$safe.mp4"))
                    renameTarget = null
                    refresh()
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingsScreen(
    destination: DestinationConfig,
    onSelectPlatform: (PlatformPreset) -> Unit,
    onSaveDestination: (PlatformPreset, String, String) -> Unit,
    onClearDestination: () -> Unit,
    adsEnabled: Boolean,
    onAdsEnabledChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var microphoneEnabled by rememberSaveable { mutableStateOf(true) }
    var keepAwake by rememberSaveable { mutableStateOf(true) }
    var selectedPlatformName by rememberSaveable(destination.platform.name) { mutableStateOf(destination.platform.name) }
    var serverUrl by rememberSaveable(destination.serverUrl) { mutableStateOf(destination.serverUrl) }
    var streamKey by rememberSaveable(destination.streamKey) { mutableStateOf(destination.streamKey) }
    val selectedPlatform = PlatformPreset.values().firstOrNull { it.name == selectedPlatformName } ?: PlatformPreset.YOUTUBE

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            BrandHeader("Control center", "Settings")
            Spacer(Modifier.height(6.dp))
            Text("Keep your broadcast setup simple, secure, and ready to repeat.", color = UnictoosPalette.TextMuted)
        }
        item {
            SectionHeader("Destination", "Choose where Unictoos should send your broadcast")
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PlatformPreset.values().toList()) { platform ->
                    FilterChip(
                        selected = selectedPlatform == platform,
                        onClick = { selectedPlatformName = platform.name; onSelectPlatform(platform) },
                        label = { Text(platform.label) },
                    )
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.Surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(selectedPlatform.helper, fontWeight = FontWeight.SemiBold)
                    Text("Use the current ingest URL shown in your platform dashboard. Never share your stream key in screenshots or logs.", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Server URL") },
                        placeholder = { Text(selectedPlatform.serverHint) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    OutlinedTextField(
                        value = streamKey,
                        onValueChange = { streamKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Stream key") },
                        placeholder = { Text("Stored with Android Keystore") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { onSaveDestination(selectedPlatform, serverUrl, streamKey) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                            Text(if (destination.isConfigured) "Update destination" else "Save destination")
                        }
                        if (destination.isConfigured) {
                            OutlinedButton(onClick = onClearDestination, modifier = Modifier.weight(0.65f), shape = RoundedCornerShape(14.dp)) {
                                Text("Clear")
                            }
                        }
                    }
                    val dashboardUrl = when (selectedPlatform) {
                        PlatformPreset.YOUTUBE -> "https://studio.youtube.com/channel/UC/livestreaming"
                        PlatformPreset.TWITCH -> "https://dashboard.twitch.tv/settings/stream"
                        PlatformPreset.KICK -> "https://dashboard.kick.com/channel/stream"
                        PlatformPreset.CUSTOM -> null
                    }
                    if (dashboardUrl != null) {
                        TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(dashboardUrl))) } }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Open ${selectedPlatform.label} dashboard")
                        }
                    }
                }
            }
        }
        item {
            SectionHeader("Device controls", "Permissions and broadcast behavior")
            Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.Surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingToggle("Microphone", "Check audio access before going live", microphoneEnabled) { microphoneEnabled = it }
                    HorizontalDivider(color = UnictoosPalette.Stroke)
                    SettingToggle("Keep screen awake", "Prevent the display from sleeping in Studio", keepAwake) { keepAwake = it }
                }
            }
        }
        item {
            SectionHeader("Support Unictoos", "Optional app-only sponsor space")
            Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.SurfaceRaised), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingToggle("Show sponsor banners", "Ads may appear in Home or Library only; never inside a live broadcast", adsEnabled, onAdsEnabledChange)
                    Text("No advertising provider is enabled in this alpha build. Your choice only controls the future app-only slot.", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            SectionHeader("Privacy and trust", "What Unictoos promises")
            TrustRow(Icons.Default.Lock, "Credential protection", "Stream keys stay encrypted on this device")
            TrustRow(Icons.Default.Visibility, "Transparent capture", "Android asks for screen capture permission every time")
            TrustRow(Icons.Default.Warning, "Alpha engine", "Test on a physical device before a public broadcast")
        }
    }
}

@Composable
private fun SessionErrorCard(message: String, onAction: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.Danger.copy(alpha = 0.14f)), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = UnictoosPalette.Danger)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Action needed", fontWeight = FontWeight.Bold)
                Text(message.ifBlank { "Unictoos could not prepare the broadcast" }, color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onAction) { Text("Fix") }
        }
    }
}

@Composable
private fun SponsorBanner() {
    Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.SurfaceRaised), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = UnictoosPalette.Violet.copy(alpha = 0.22f), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.GraphicEq, null, tint = UnictoosPalette.VioletBright, modifier = Modifier.padding(9.dp).size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Optional sponsor space", fontWeight = FontWeight.SemiBold)
                Text("This banner is app-only and never sent to your stream.", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text("OFFLINE", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SourceToggleRow(
    title: String,
    type: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    opacity: Float = 1f,
    onOpacityChange: (Float) -> Unit = {},
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = UnictoosPalette.SurfaceRaised, shape = RoundedCornerShape(10.dp)) {
            Icon(
                if (type.startsWith("Camera")) Icons.Default.Videocam else if (type.startsWith("Screen")) Icons.Default.LiveTv else Icons.Default.Dashboard,
                null,
                tint = UnictoosPalette.Cyan,
                modifier = Modifier.padding(8.dp).size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(type, color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onMoveUp, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("↑") }
        TextButton(onClick = onMoveDown, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("↓") }
            Switch(checked = enabled, onCheckedChange = { onClick() })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Opacity", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(54.dp))
            androidx.compose.material3.Slider(value = opacity, onValueChange = onOpacityChange, enabled = enabled, modifier = Modifier.weight(1f))
            Text("${(opacity * 100).toInt()}%", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(42.dp))
        }
    }
}

@Composable
private fun TrustRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = UnictoosPalette.SurfaceRaised, shape = RoundedCornerShape(12.dp)) {
            Icon(icon, null, tint = UnictoosPalette.Cyan, modifier = Modifier.padding(9.dp).size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SceneCard(
    scene: Scene,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onOpenStudio: (() -> Unit)? = null,
) {
    val colors = if (selected) {
        CardDefaults.cardColors(containerColor = UnictoosPalette.Violet.copy(alpha = 0.20f))
    } else {
        CardDefaults.cardColors(containerColor = UnictoosPalette.Surface)
    }
    Card(onClick = onClick ?: {}, colors = colors, shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(62.dp).clip(RoundedCornerShape(17.dp)).background(
                    Brush.linearGradient(listOf(UnictoosPalette.Violet.copy(alpha = 0.8f), UnictoosPalette.Magenta.copy(alpha = 0.8f))),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(sceneIcon(scene), null, tint = Color.White, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(scene.name, fontWeight = FontWeight.SemiBold)
                Text("${scene.aspectRatio.ratio}  •  ${scene.sources.size} sources", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(scene.sources.take(3), key = { it.id }) { source ->
                        Surface(color = Color.White.copy(alpha = 0.07f), shape = RoundedCornerShape(50)) {
                            Text(source.type.label, Modifier.padding(horizontal = 7.dp, vertical = 4.dp), color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if (onOpenStudio != null) {
                IconButton(onClick = onOpenStudio) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Open studio", tint = UnictoosPalette.Cyan) }
            }
        }
    }
}

private fun sceneIcon(scene: Scene) = when {
    scene.sources.any { it.type == SourceType.CAMERA } -> Icons.Default.Videocam
    scene.sources.any { it.type == SourceType.SCREEN } -> Icons.Default.LiveTv
    else -> Icons.Default.Dashboard
}

@Composable
private fun AddSourceDialog(onDismiss: () -> Unit, onCreate: (String, SourceType) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var typeName by rememberSaveable { mutableStateOf(SourceType.TEXT.name) }
    val selectedType = SourceType.values().firstOrNull { it.name == typeName } ?: SourceType.TEXT
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a source") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Extend this scene with another layer.", color = UnictoosPalette.TextMuted)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Source name") }, singleLine = true)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SourceType.values().toList()) { option ->
                        FilterChip(selected = option == selectedType, onClick = { typeName = option.name }, label = { Text(option.label) })
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onCreate(name, selectedType) }) { Text("Add source") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddSceneDialog(onDismiss: () -> Unit, onCreate: (String, AspectRatio) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var ratioName by rememberSaveable { mutableStateOf(AspectRatio.PORTRAIT.name) }
    val ratio = AspectRatio.values().firstOrNull { it.name == ratioName } ?: AspectRatio.PORTRAIT
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create a scene") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Start with a layout designed for your phone.", color = UnictoosPalette.TextMuted)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Scene name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AspectRatio.values().forEach { option ->
                        FilterChip(selected = option == ratio, onClick = { ratioName = option.name }, label = { Text(option.label) })
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onCreate(name, ratio) }) { Text("Create scene") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun StatusPill(status: StreamStatus) {
    val (label, color) = when (status) {
        StreamStatus.LIVE -> "LIVE" to UnictoosPalette.Mint
        StreamStatus.PREPARING -> "PREPARING" to UnictoosPalette.Amber
        StreamStatus.RECONNECTING -> "RECONNECTING" to UnictoosPalette.Amber
        StreamStatus.STOPPING -> "STOPPING" to UnictoosPalette.Amber
        StreamStatus.ERROR -> "CHECK SETUP" to UnictoosPalette.Danger
        StreamStatus.IDLE -> "READY" to UnictoosPalette.TextMuted
    }
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(50), modifier = Modifier.animateContentSize(tween(220))) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (status == StreamStatus.LIVE) LivePulseDot() else Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(color))
            Spacer(Modifier.width(6.dp))
            AnimatedContent(targetState = label, label = "statusLabel") { animatedLabel ->
                Text(animatedLabel, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (subtitle.isNotBlank()) Text(subtitle, color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReadinessRow(label: String, value: String, ready: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(if (ready) UnictoosPalette.Mint else UnictoosPalette.Amber))
            Spacer(Modifier.width(10.dp))
            Text(label)
        }
        Text(value, color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier) {
    Card(modifier.animateContentSize(tween(220)), colors = CardDefaults.cardColors(containerColor = UnictoosPalette.SurfaceRaised), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = UnictoosPalette.TextMuted)
            Spacer(Modifier.height(3.dp))
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
