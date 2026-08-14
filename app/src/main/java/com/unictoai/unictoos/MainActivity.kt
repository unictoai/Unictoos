package com.unictoai.unictoos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.SourceType
import com.unictoai.unictoos.domain.StreamStatus
import com.unictoai.unictoos.ui.theme.UnictoosTheme

private enum class AppTab(val label: String) {
    HOME("Home"),
    SCENES("Scenes"),
    STUDIO("Studio"),
    RECORDINGS("Recordings"),
    SETTINGS("Settings"),
}

class MainActivity : ComponentActivity() {
    private var pendingEndpoint: String = ""

    private val projectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult
        val prepareIntent = Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_PREPARE_PROJECTION
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_RESULT_CODE, result.resultCode)
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_PROJECTION_DATA, result.data)
        }
        ContextCompat.startForegroundService(this, prepareIntent)
        val startIntent = Intent(this, com.unictoai.unictoos.streaming.StreamingForegroundService::class.java).apply {
            action = com.unictoai.unictoos.streaming.StreamingForegroundService.ACTION_START
            putExtra(com.unictoai.unictoos.streaming.StreamingForegroundService.EXTRA_ENDPOINT, pendingEndpoint)
        }
        startService(startIntent)
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            launchProjection()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { UnictoosTheme { UnictoosApp(onRequestStreamStart = ::requestStreamStart, onStopStream = ::stopStream) } }
    }

    private fun requestStreamStart(endpoint: String) {
        pendingEndpoint = endpoint
        val needsAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        if (needsAudio) {
            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS))
        } else {
            launchProjection()
        }
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
}

@Composable
private fun UnictoosApp(
    onRequestStreamStart: (String) -> Unit,
    onStopStream: () -> Unit,
    vm: StudioViewModel = viewModel(),
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var selectedSceneId by rememberSaveable { mutableStateOf("starting-soon") }
    var showAddScene by rememberSaveable { mutableStateOf(false) }
    val scenes by vm.scenes.collectAsStateWithLifecycle()
    val destinations by vm.destinations.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val destination by vm.destination.collectAsStateWithLifecycle()
    val selectedScene = scenes.firstOrNull { it.id == selectedSceneId } ?: scenes.first()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                listOf(AppTab.HOME, AppTab.SCENES, AppTab.STUDIO, AppTab.RECORDINGS, AppTab.SETTINGS).forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon(), contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1) },
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
                )
                AppTab.SCENES -> ScenesScreen(
                    scenes = scenes,
                    selectedSceneId = selectedSceneId,
                    onSelect = { selectedSceneId = it },
                    onAdd = { showAddScene = true },
                )
                AppTab.STUDIO -> StudioScreen(
                    scene = selectedScene,
                    session = session,
                    destination = destination,
                    onStart = { onRequestStreamStart(destination.endpoint) },
                    onStop = onStopStream,
                    onEditScenes = { selectedTab = AppTab.SCENES },
                )
                AppTab.RECORDINGS -> RecordingsScreen()
                AppTab.SETTINGS -> SettingsScreen(destination = destination, onSaveDestination = vm::updateDestination)
            }
        }
    }

    if (showAddScene) {
        AddSceneDialog(
            onDismiss = { showAddScene = false },
            onCreate = { name -> vm.addScene(name); showAddScene = false },
        )
    }
}

private fun AppTab.icon() = when (this) {
    AppTab.HOME -> Icons.Default.Home
    AppTab.SCENES -> Icons.Default.Dashboard
    AppTab.STUDIO -> Icons.Default.LiveTv
    AppTab.RECORDINGS -> Icons.Default.Movie
    AppTab.SETTINGS -> Icons.Default.Settings
}

@Composable
private fun HomeScreen(
    scenes: List<Scene>,
    destinations: List<com.unictoai.unictoos.domain.StreamDestination>,
    session: com.unictoai.unictoos.domain.StreamSessionState,
    onGoStudio: () -> Unit,
    onOpenScenes: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Good to see you", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Unictoos", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                }
                StatusPill(session.status)
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LiveTv, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Creator studio", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("Your broadcast, built for the phone.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Compose a scene, check your signal, and go live without desktop-style clutter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onGoStudio, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (session.status == StreamStatus.LIVE) "Open live studio" else "Start a studio session")
                    }
                }
            }
        }
        item {
            SectionHeader("Broadcast readiness", "Before you go live")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ReadinessRow("Scenes ready", "${scenes.size} saved scenes", true)
                    ReadinessRow("Destinations", "${destinations.size} presets available", true)
                    ReadinessRow("Capture engine", "Native pipeline scaffold", false)
                    ReadinessRow("Stream health", if (session.status == StreamStatus.LIVE) "Monitoring live" else "Run a local test first", session.status == StreamStatus.LIVE)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("Your scenes", "")
                TextButton(onClick = onOpenScenes) { Text("View all") }
            }
        }
        items(scenes.take(2), key = { it.id }) { scene -> SceneCard(scene) }
    }
}

@Composable
private fun ScenesScreen(
    scenes: List<Scene>,
    selectedSceneId: String,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Scenes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Build layouts that feel like you.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Create scene") }
        }
        Spacer(Modifier.height(18.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(scenes, key = { it.id }) { scene ->
                SceneCard(scene, selected = scene.id == selectedSceneId, onClick = { onSelect(scene.id) })
            }
        }
    }
}

@Composable
private fun StudioScreen(
    scene: Scene,
    session: com.unictoai.unictoos.domain.StreamSessionState,
    destination: DestinationConfig,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onEditScenes: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Studio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(scene.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(session.status)
        }
        Box(
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)).background(Color(0xFF02070D)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.LiveTv, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Live preview", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text("${scene.aspectRatio.label}  •  ${scene.sources.count { it.enabled }} active sources", color = Color(0xFFA8BDD4))
                Text(if (destination.isConfigured) "${destination.platform.label} destination configured" else "Configure a destination in Settings before going live", color = Color(0xFFFFC857), style = MaterialTheme.typography.bodySmall)
                if (session.status == StreamStatus.PREPARING) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(0.55f))
                    Text(session.message.orEmpty(), color = Color(0xFFA8BDD4))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("Bitrate", if (session.bitrateKbps > 0) "${session.bitrateKbps} kbps" else "—", Modifier.weight(1f))
            MetricCard("FPS", if (session.fps > 0) session.fps.toString() else "—", Modifier.weight(1f))
            MetricCard("Audio", "Mic ready", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onEditScenes, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Dashboard, null)
                Spacer(Modifier.width(6.dp))
                Text("Edit scene")
            }
            Button(
                onClick = if (session.status == StreamStatus.LIVE) onStop else onStart,
                modifier = Modifier.weight(1f),
            ) {
                Icon(if (session.status == StreamStatus.LIVE) Icons.Default.Stop else Icons.Default.FiberManualRecord, null)
                Spacer(Modifier.width(6.dp))
                Text(if (session.status == StreamStatus.LIVE) "Stop" else "Go live")
            }
        }
    }
}

@Composable
private fun RecordingsScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Recordings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Movie, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Your local library is ready", fontWeight = FontWeight.SemiBold)
                    Text("Finished broadcasts will appear here without leaving your device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text("No recordings yet", style = MaterialTheme.typography.titleMedium)
        Text("Record a session from Studio to review, share, or delete it later.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsScreen(
    destination: DestinationConfig,
    onSaveDestination: (com.unictoai.unictoos.domain.PlatformPreset, String, String) -> Unit,
) {
    var microphoneEnabled by rememberSaveable { mutableStateOf(true) }
    var keepAwake by rememberSaveable { mutableStateOf(true) }
    var serverUrl by rememberSaveable(destination.serverUrl) { mutableStateOf(destination.serverUrl) }
    var streamKey by rememberSaveable(destination.streamKey) { mutableStateOf(destination.streamKey) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Keep your broadcast setup simple and local.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        SettingToggle("Microphone", "Allow Unictoos to use your microphone while live", microphoneEnabled) { microphoneEnabled = it }
        SettingToggle("Keep screen awake", "Prevent the display from sleeping in Studio", keepAwake) { keepAwake = it }
        HorizontalDivider()
        Text("Streaming destination", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Start with custom RTMP/RTMPS settings. This works with YouTube, Twitch, Kick, and other compatible destinations.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Server URL") },
            placeholder = { Text("rtmps://your-ingest-server/app") },
            singleLine = true,
        )
        OutlinedTextField(
            value = streamKey,
            onValueChange = { streamKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Stream key") },
            placeholder = { Text("Encrypted locally with Android Keystore") },
            singleLine = true,
        )
        Button(onClick = { onSaveDestination(destination.platform, serverUrl, streamKey) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (destination.isConfigured) "Update destination" else "Save destination")
        }
        SettingRow(Icons.Default.ArrowUpward, "Upload guidance", "Use a stable connection and test before going live")
        SettingRow(Icons.Default.Lock, "Credential protection", "Stream keys stay on this device")
        SettingRow(Icons.Default.Warning, "Alpha engine", "Test on a physical device before a public broadcast")
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SceneCard(scene: Scene, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    val colors = if (selected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)) else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    Card(onClick = onClick ?: {}, colors = colors) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF142B45)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.LiveTv, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(scene.name, fontWeight = FontWeight.SemiBold)
                Text("${scene.aspectRatio.ratio}  •  ${scene.sources.size} sources", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.MoreHoriz, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (subtitle.isNotBlank()) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReadinessRow(label: String, value: String, ready: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(if (ready) Color(0xFF45E09A) else Color(0xFFFFC857)))
            Spacer(Modifier.width(10.dp))
            Text(label)
        }
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatusPill(status: StreamStatus) {
    val (label, color) = when (status) {
        StreamStatus.LIVE -> "LIVE" to Color(0xFF45E09A)
        StreamStatus.PREPARING -> "PREPARING" to Color(0xFFFFC857)
        StreamStatus.RECONNECTING -> "RECONNECTING" to Color(0xFFFFC857)
        StreamStatus.ERROR -> "CHECK SETUP" to Color(0xFFFF6B6B)
        else -> "READY" to MaterialTheme.colorScheme.primary
    }
    AssistChip(onClick = {}, label = { Text(label) }, leadingIcon = { Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(color)) })
}

@Composable
private fun AddSceneDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create a scene") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Scene name") }, singleLine = true)
        },
        confirmButton = { TextButton(onClick = { onCreate(name) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
