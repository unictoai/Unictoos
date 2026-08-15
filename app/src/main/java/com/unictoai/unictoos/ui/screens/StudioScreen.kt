package com.unictoai.unictoos.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.AutoStopDuration
import com.unictoai.unictoos.domain.AudioSettings
import com.unictoai.unictoos.domain.formatEstimatedDataPerHour
import com.unictoai.unictoos.domain.PlatformPreset
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.SourceType
import com.unictoai.unictoos.data.CreatorHistoryStore
import com.unictoai.unictoos.domain.StreamDestination
import com.unictoai.unictoos.domain.StreamHealthSample
import com.unictoai.unictoos.domain.StreamSessionState
import com.unictoai.unictoos.domain.StreamQuality
import com.unictoai.unictoos.domain.StreamStatus
import com.unictoai.unictoos.ui.theme.MotionTokens
import com.unictoai.unictoos.ui.theme.Spacing
import com.unictoai.unictoos.ui.theme.V02Palette
import com.unictoai.unictoos.ui.theme.UnictoosTheme
import com.unictoai.unictoos.DestinationConfig
import com.unictoai.unictoos.ui.components.MetricCard
import com.unictoai.unictoos.ui.components.SessionErrorCard
import com.unictoai.unictoos.ui.components.StatusPill
import com.unictoai.unictoos.ui.PreviewSurfaceView

@Composable
internal fun StudioScreen(
    scene: Scene,
    session: StreamSessionState,
    healthHistory: List<StreamHealthSample>,
    destination: DestinationConfig,
    streamQuality: StreamQuality,
    audioSettings: AudioSettings,
    autoStopDuration: AutoStopDuration,
    onAutoStopDurationChange: (AutoStopDuration) -> Unit,
    onStart: () -> Unit,
    onPractice: () -> Unit,
    onStop: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleRecording: () -> Unit,
    onCreateMarker: () -> Unit,
    onDismissStatusMessage: () -> Unit,
    onEditScenes: () -> Unit,
    onOpenSettings: () -> Unit,
    onReleaseCapture: () -> Unit,
    onPreviewSurfaceAvailable: (Surface, Int, Int) -> Unit,
    onPreviewSurfaceDestroyed: (Surface) -> Unit,
) {
    val primaryActionSource = remember { MutableInteractionSource() }
    val primaryActionPressed by primaryActionSource.collectIsPressedAsState()
    val previewAvailableState = rememberUpdatedState(onPreviewSurfaceAvailable)
    val previewDestroyedState = rememberUpdatedState(onPreviewSurfaceDestroyed)
    val previewListener = remember {
        object : PreviewSurfaceView.Listener {
            override fun onSurfaceAvailable(surface: Surface, width: Int, height: Int) {
                previewAvailableState.value(surface, width, height)
            }

            override fun onSurfaceDestroyed(surface: Surface) {
                previewDestroyedState.value(surface)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text("Studio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Broadcast workspace", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                }
                StatusPill(session.status)
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(scene.name, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(Spacing.sm))
                Text("•", color = V02Palette.Neutral500)
                Spacer(Modifier.width(Spacing.sm))
                Text(scene.aspectRatio.label, color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(Spacing.sm))
                Surface(color = V02Palette.Neutral300.copy(alpha = 0.13f), shape = RoundedCornerShape(50)) {
                    Text(streamQuality.displayName, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = V02Palette.Neutral300, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item {
            Text(
                "${formatEstimatedDataPerHour(streamQuality.bitrate, audioSettings.bitrate)} estimated at the active profile • protocol overhead varies",
                color = V02Palette.Neutral500,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (scene.sources.any { it.enabled && it.type == SourceType.SCREEN } && scene.sources.any { it.enabled && it.type == SourceType.CAMERA }) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Caution.copy(alpha = 0.14f)), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Single-source capture", fontWeight = FontWeight.Bold, color = V02Palette.Caution)
                        Text("This scene contains screen and camera layers, but simultaneous composition is not available yet. The current broadcast uses screen capture; camera compositing will be added only with a real GPU compositor.", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Box(
                Modifier.fillMaxWidth().height(330.dp).clip(RoundedCornerShape(20.dp)).background(V02Palette.Neutral950).animateContentSize(tween(MotionTokens.emphasis, easing = MotionTokens.gentleEasing)),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { context ->
                        PreviewSurfaceView(context).apply {
                            setPreviewListener(previewListener)
                        }
                    },
                    update = { view ->
                        view.setPreviewListener(previewListener)
                    },
                    onRelease = { view ->
                        view.releasePreviewListener()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                if (!session.previewReady) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(color = V02Palette.Neutral950.copy(alpha = 0.92f), shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(Spacing.xl)) {
                            Column(Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.lg), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = V02Palette.Neutral300, modifier = Modifier.size(30.dp))
                                Text("LIVE PREVIEW", color = V02Palette.Neutral100, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    session.message ?: "Approve capture to start the real preview",
                                    color = V02Palette.Neutral500,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (session.status == StreamStatus.PREPARING || session.status == StreamStatus.CONNECTING || session.status == StreamStatus.RECONNECTING) {
                                    LinearProgressIndicator(Modifier.fillMaxWidth(0.72f), color = V02Palette.AccentBlue, trackColor = V02Palette.Neutral700)
                                }
                            }
                        }
                    }
                }
                Surface(
                    Modifier.align(Alignment.TopStart).padding(14.dp),
                    color = when {
                        session.status == StreamStatus.LIVE -> V02Palette.AccentBlue.copy(alpha = 0.96f)
                        session.previewReady -> V02Palette.AccentBlue.copy(alpha = 0.86f)
                        else -> Color.White.copy(alpha = 0.10f)
                    },
                    contentColor = V02Palette.OnAccent,
                    shape = RoundedCornerShape(50),
                    border = if (session.status == StreamStatus.LIVE) BorderStroke(1.dp, V02Palette.AccentBlue.copy(alpha = 0.72f)) else null,
                ) {
                    Row(Modifier.padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (session.status == StreamStatus.LIVE) V02LiveDot()
                        Spacer(Modifier.width(if (session.status == StreamStatus.LIVE) 7.dp else 0.dp))
                        Text(
                            when {
                                session.status == StreamStatus.LIVE -> "LIVE • STREAMING"
                                session.previewReady -> "PREVIEW READY"
                                else -> "PREVIEW WAITING"
                            },
                            color = V02Palette.OnAccent,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        if (session.status == StreamStatus.ERROR) item { SessionErrorCard(session.message.orEmpty(), onReleaseCapture) }
        if (session.message?.contains("Reduced quality", ignoreCase = true) == true || session.message?.contains("quality raised", ignoreCase = true) == true) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Caution.copy(alpha = 0.14f)), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Quality notice", tint = V02Palette.Caution)
                        Spacer(Modifier.width(10.dp))
                        Text(session.message.orEmpty(), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onDismissStatusMessage) { Text("Dismiss") }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (session.status == StreamStatus.LIVE) "Live health" else "Session health", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (session.status == StreamStatus.LIVE) V02Palette.AccentBlue else V02Palette.Neutral500)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Bitrate", if (session.bitrateKbps > 0) "${session.bitrateKbps} kbps" else "—", Modifier.weight(1f))
                    MetricCard("FPS", if (session.status == StreamStatus.LIVE && session.fps > 0) session.fps.toString() else "—", Modifier.weight(1f))
                    val audioLabel = when {
                        session.status == StreamStatus.LIVE -> "Mic live"
                        session.message?.contains("Microphone", true) == true -> "Mic ready"
                        session.status == StreamStatus.ERROR -> "Check mic"
                        else -> "Not checked"
                    }
                    MetricCard("Audio", audioLabel, Modifier.weight(1f))
                }
            }
        }
        item {
            HealthCenterCard(history = healthHistory, session = session)
        }
        item {
            AnimatedVisibility(visible = session.status == StreamStatus.LIVE, enter = fadeIn(tween(MotionTokens.standard, easing = MotionTokens.gentleEasing)) + slideInVertically(tween(MotionTokens.standard, easing = MotionTokens.gentleEasing)) { it / 3 }) {
                OutlinedButton(onClick = onCreateMarker, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Bolt, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Mark moment for clip")
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral900)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (destination.isConfigured) Icons.Default.Wifi else Icons.Default.Warning, null, tint = if (destination.isConfigured) V02Palette.AccentBlue else V02Palette.Caution)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (destination.isConfigured) "${destination.platform.label} is ready" else "No destination connected", fontWeight = FontWeight.SemiBold)
                        Text(if (destination.isConfigured) "Your key is stored securely on this device" else "Add a YouTube, Twitch, Kick, or custom RTMP destination", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!destination.isConfigured) TextButton(onClick = onOpenSettings) { Text("Set up") }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral850), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text("Session controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Auto-stop after", style = MaterialTheme.typography.labelLarge, color = V02Palette.Neutral500)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        items(AutoStopDuration.values().toList()) { duration ->
                            FilterChip(
                                selected = autoStopDuration == duration,
                                onClick = { onAutoStopDurationChange(duration) },
                                label = { Text(duration.label) },
                            )
                        }
                    }
                    Button(
                        onClick = if (session.status == StreamStatus.LIVE) onStop else onStart,
                        modifier = Modifier.fillMaxWidth().height(56.dp).graphicsLayer {
                            val scale = if (primaryActionPressed) 0.97f else 1f
                            scaleX = scale
                            scaleY = scale
                        },
                        enabled = session.status == StreamStatus.IDLE || session.status == StreamStatus.STOPPED || session.status == StreamStatus.ERROR || session.status == StreamStatus.LIVE,
                        interactionSource = primaryActionSource,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (session.status == StreamStatus.LIVE) V02Palette.Danger else V02Palette.AccentBlue,
                            contentColor = V02Palette.OnAccent,
                            disabledContainerColor = V02Palette.Neutral800,
                            disabledContentColor = V02Palette.Neutral500,
                        ),
                    ) {
                        Icon(if (session.status == StreamStatus.LIVE) Icons.Default.Stop else Icons.Default.FiberManualRecord, null)
                        Spacer(Modifier.width(Spacing.sm))
                        Text(if (session.status == StreamStatus.LIVE) "Stop broadcast" else "Go live", style = MaterialTheme.typography.labelLarge)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedButton(onClick = onToggleMute, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                            Icon(if (session.microphoneMuted) Icons.Default.MicOff else Icons.Default.Mic, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (session.microphoneMuted) "Unmute" else "Mute")
                        }
                        OutlinedButton(onClick = onToggleRecording, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.Movie, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (session.recording) "Stop record" else "Record")
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        TextButton(onClick = onEditScenes, modifier = Modifier.weight(1f), enabled = session.status != StreamStatus.LIVE) {
                            Icon(Icons.Default.Dashboard, null)
                            Spacer(Modifier.width(5.dp))
                            Text("Edit scene")
                        }
                        TextButton(onClick = onPractice, modifier = Modifier.weight(1f), enabled = session.status == StreamStatus.IDLE || session.status == StreamStatus.STOPPED || session.status == StreamStatus.ERROR) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(5.dp))
                            Text("Practice")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V02LiveDot() {
    val transition = rememberInfiniteTransition(label = "liveIndicator")
    val alpha by transition.animateFloat(
        initialValue = 0.62f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "liveIndicatorOpacity",
    )
    Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(V02Palette.OnAccent.copy(alpha = alpha)))
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
    Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral850), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Health center", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(if (session.mode.name == "PRACTICE") "Local rehearsal diagnostics" else "Live session diagnostics", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = V02Palette.Neutral300)
            }
            if (latest == null) {
                Text("Health telemetry appears here once a session is active.", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Bitrate", if (latest.bitrateKbps > 0) "${latest.bitrateKbps} kbps" else "—", Modifier.weight(1f))
                    MetricCard("FPS", if (session.status == StreamStatus.LIVE && latest.fps > 0) latest.fps.toString() else "—", Modifier.weight(1f))
                    MetricCard("Drops", if (latest.droppedFrames >= 0) latest.droppedFrames.toString() else "—", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Network  ${latest.networkLabel}", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                    Text("Battery  ${if (latest.batteryPercent >= 0) "${latest.batteryPercent}%" else "—"}", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                    Text("Thermal  $thermalLabel", color = if (thermalLabel == "Normal") V02Palette.AccentBlue else V02Palette.Caution, style = MaterialTheme.typography.bodySmall)
                }
                Text("Audio level", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall)
                LinearProgressIndicator(
                    progress = { if (latest.audioLevel >= 0) latest.audioLevel.coerceIn(0, 100) / 100f else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = V02Palette.AccentBlue,
                    trackColor = Color.White.copy(alpha = 0.10f),
                )
                Text(if (latest.audioLevel >= 0) "${latest.audioLevel}% peak" else "Microphone level telemetry unavailable from the active encoder", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall)
                Text("${history.size} samples retained locally for this session", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
