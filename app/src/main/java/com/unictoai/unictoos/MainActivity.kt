package com.unictoai.unictoos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.PlatformPreset
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.SourceType
import com.unictoai.unictoos.domain.StreamDestination
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

    private val projectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult
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
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_START
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_ENDPOINT, pendingEndpoint)
        })
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
                add(Manifest.permission.POST_NOTIFICATIONS)
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
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_START
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_ENDPOINT, pendingEndpoint)
        })
    }

    private fun launchProjection() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        projectionLauncher.launch(manager.createScreenCaptureIntent())
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
    val destination by vm.destination.collectAsStateWithLifecycle()
    val adsPolicy by vm.adsPolicy.collectAsStateWithLifecycle()
    val selectedScene = scenes.firstOrNull { it.id == selectedSceneId } ?: scenes.first()

    Scaffold(
        containerColor = UnictoosPalette.Ink,
        bottomBar = {
            NavigationBar(containerColor = UnictoosPalette.InkSoft, tonalElevation = 0.dp) {
                listOf(AppTab.HOME, AppTab.SCENES, AppTab.STUDIO, AppTab.ENGAGEMENT, AppTab.LIBRARY, AppTab.SETTINGS).forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon(), contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = UnictoosPalette.VioletBright,
                            indicatorColor = UnictoosPalette.Violet.copy(alpha = 0.24f),
                            unselectedIconColor = UnictoosPalette.TextMuted,
                            unselectedTextColor = UnictoosPalette.TextMuted,
                        ),
                    )
                }
            }
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
                    onOpenStudio = { selectedTab = AppTab.STUDIO },
                )
                AppTab.STUDIO -> StudioScreen(
                    scene = selectedScene,
                    session = session,
                    destination = destination,
                    onStart = {
                        val captureMode = if (selectedScene.sources.any { it.type == SourceType.SCREEN && it.enabled }) "screen" else "camera"
                        onRequestStreamStart(destination.endpoint, captureMode)
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
    onOpenSettings: () -> Unit,
    showAdSlot: Boolean = false,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
                item {
            BrandHeader("Creator workspace", "Make your moment live") { StatusPill(session.status) }
        }
        if (showAdSlot) item { SponsorBanner() }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Box(
                    Modifier.background(
                        Brush.linearGradient(listOf(Color(0xFF43209A), Color(0xFFAC2F83), Color(0xFF161630))),
                    ).padding(22.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
                                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FiberManualRecord, null, tint = Color(0xFFFF719B), modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("MOBILE LIVE STUDIO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                        Text("Your broadcast,\nbuilt for the phone.", style = MaterialTheme.typography.displaySmall, color = Color.White)
                        Text("Set the scene, check your signal, and go live without desktop-style clutter.", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = onGoStudio,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF2A125D)),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (session.status == StreamStatus.LIVE) "Open live studio" else "Open studio")
                        }
                    }
                }
            }
        }
        item {
            PreflightCard()
        }
        item {
            SectionHeader("Broadcast readiness", "Everything you need before the red button")
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.Surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ReadinessRow("Scenes", "${scenes.size} ready", scenes.isNotEmpty())
                    ReadinessRow("Destination", configuredDestinationLabel(destinations), destinations.any { it.isConfigured })
                    ReadinessRow("Capture", "Screen permission on demand", true)
                    ReadinessRow("Microphone", if (session.message?.contains("Microphone", true) == true) "Ready to test" else "Checked before live", session.status != StreamStatus.ERROR)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("Quick actions", "Jump back into your workflow")
                TextButton(onClick = onOpenSettings) { Text("Destinations") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Icons.Default.Dashboard, "Scenes", "Build a layout", onOpenScenes, Modifier.weight(1f))
                QuickAction(Icons.Default.Settings, "Setup", "Check keys", onOpenSettings, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("Your scenes", "Layouts ready to launch")
                TextButton(onClick = onOpenScenes) { Text("View all") }
            }
        }
        items(scenes.take(2), key = { it.id }) { scene -> SceneCard(scene) }
    }
}

private fun configuredDestinationLabel(destinations: List<StreamDestination>): String =
    destinations.firstOrNull { it.isConfigured }?.name ?: "Add a destination"

@Composable
private fun QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = UnictoosPalette.SurfaceRaised), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, tint = UnictoosPalette.VioletBright, modifier = Modifier.size(22.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
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
                            SourceToggleRow(source.name, source.type.label, source.enabled) { onToggleSource(selectedScene.id, source.id) }
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
    destination: DestinationConfig,
    onStart: () -> Unit,
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
                Modifier.fillMaxWidth().height(330.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xFF02030A)),
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
                    if (session.status == StreamStatus.PREPARING || session.status == StreamStatus.RECONNECTING) {
                        LinearProgressIndicator(Modifier.fillMaxWidth(0.6f), color = UnictoosPalette.VioletBright, trackColor = Color.White.copy(alpha = 0.14f))
                        Text(session.message.orEmpty(), color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { BrandHeader("Community control", "Engage") { StatusPill(StreamStatus.IDLE) }; Spacer(Modifier.height(6.dp)); Text("Bring chat, alerts, and creator actions into one calm mobile workspace.", color = UnictoosPalette.TextMuted) }
        item { Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.SurfaceRaised), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.AutoMirrored.Filled.Chat, null, tint = UnictoosPalette.Cyan, modifier = Modifier.size(30.dp)); Text("Unified chat inbox", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Connect platform accounts to read chat, emotes, alerts, and moderation events without leaving Studio.", color = UnictoosPalette.TextMuted); Text("No accounts connected", color = UnictoosPalette.Amber, fontWeight = FontWeight.SemiBold) } } }
        item { SectionHeader("Platform integrations", "OAuth is kept separate from stream keys") }
        items(PlatformPreset.values().toList()) { platform -> IntegrationCard(platform) }
        item { SectionHeader("Planned creator controls", "Built around explicit permissions") }
        item { ReadinessRow("Chat and emotes", "OAuth integration", false); ReadinessRow("Alerts and follows", "Event connection", false); ReadinessRow("Moderation", "Permissioned tools", false); ReadinessRow("Clips and markers", "Platform API", false) }
    }
}

@Composable
private fun IntegrationCard(platform: PlatformPreset) {
    Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.Surface), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = UnictoosPalette.Violet.copy(alpha = 0.20f), shape = RoundedCornerShape(12.dp)) { Text(platform.label.take(1), Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = UnictoosPalette.VioletBright, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(platform.label, fontWeight = FontWeight.SemiBold); Text("Stream key works now • OAuth tools are separate", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall) }
            Text("SOON", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LibraryScreen() {
    val context = LocalContext.current
    var recordings by rememberSaveable { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(Unit) {
        recordings = java.io.File(context.filesDir, "recordings")
            .listFiles()
            ?.filter { it.extension.equals("mp4", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.name }
            .orEmpty()
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BrandHeader("Your content", "Library")
        Text("Recordings and reusable broadcast assets stay close to your workflow.", color = UnictoosPalette.TextMuted)
        if (recordings.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.SurfaceRaised), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Movie, null, tint = UnictoosPalette.VioletBright, modifier = Modifier.size(32.dp))
                    Text("No recordings yet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text("Press Record in Studio. Saved MP4 sessions will appear here in app storage.", color = UnictoosPalette.TextMuted)
                }
            }
        } else {
            SectionHeader("Saved recordings", "Stored locally on this device")
            recordings.forEach { name ->
                Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.Surface)) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Movie, null, tint = UnictoosPalette.Cyan)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("MP4 • app-private storage", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        SectionHeader("Creator assets", "Overlays, intros, and saved scene media")
        ReadinessRow("Scene templates", "Available", true)
        ReadinessRow("Overlay library", "Next milestone", false)
        ReadinessRow("Cloud backup", "Not connected", false)
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
    var microphoneEnabled by rememberSaveable { mutableStateOf(true) }
    var keepAwake by rememberSaveable { mutableStateOf(true) }
    var selectedPlatformName by rememberSaveable(destination.platform.name) { mutableStateOf(destination.platform.name) }
    var serverUrl by rememberSaveable(destination.serverUrl) { mutableStateOf(destination.serverUrl) }
    var streamKey by rememberSaveable(destination.streamKey) { mutableStateOf(destination.streamKey) }
    val selectedPlatform = PlatformPreset.valueOf(selectedPlatformName)

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
private fun SourceToggleRow(title: String, type: String, enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = UnictoosPalette.SurfaceRaised, shape = RoundedCornerShape(10.dp)) {
            Icon(
                if (type == "Camera") Icons.Default.Videocam else if (type == "Screen") Icons.Default.LiveTv else Icons.Default.Dashboard,
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
        Switch(checked = enabled, onCheckedChange = { onClick() })
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
    val selectedType = SourceType.valueOf(typeName)
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
    val ratio = AspectRatio.valueOf(ratioName)
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
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(color))
            Spacer(Modifier.width(6.dp))
            Text(label, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
    Card(modifier, colors = CardDefaults.cardColors(containerColor = UnictoosPalette.SurfaceRaised), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = UnictoosPalette.TextMuted)
            Spacer(Modifier.height(3.dp))
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
