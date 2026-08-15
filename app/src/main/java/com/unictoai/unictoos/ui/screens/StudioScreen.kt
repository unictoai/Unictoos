package com.unictoai.unictoos.ui.screens

import android.view.Surface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.unictoai.unictoos.DestinationConfig
import com.unictoai.unictoos.domain.AudioSettings
import com.unictoai.unictoos.domain.AutoStopDuration
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.SourceType
import com.unictoai.unictoos.domain.StreamHealthSample
import com.unictoai.unictoos.domain.StreamQuality
import com.unictoai.unictoos.domain.StreamSessionState
import com.unictoai.unictoos.domain.StreamStatus
import com.unictoai.unictoos.ui.PreviewSurfaceView
import com.unictoai.unictoos.ui.components.BrandHeader
import com.unictoai.unictoos.ui.components.MetricCard
import com.unictoai.unictoos.ui.components.SessionErrorCard
import com.unictoai.unictoos.ui.components.StatusPill
import com.unictoai.unictoos.ui.theme.MotionTokens
import com.unictoai.unictoos.ui.theme.Spacing
import com.unictoai.unictoos.ui.theme.V02Palette

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
    val actionSource = remember { MutableInteractionSource() }
    val actionPressed by actionSource.collectIsPressedAsState()
    val isActive = session.status in setOf(
        StreamStatus.PREPARING,
        StreamStatus.CONNECTING,
        StreamStatus.LIVE,
        StreamStatus.RECONNECTING,
        StreamStatus.STOPPING,
    )
    val canStart = session.status in setOf(StreamStatus.IDLE, StreamStatus.STOPPED, StreamStatus.ERROR)
    val captureLabel = when {
        scene.sources.any { it.enabled && it.type == SourceType.SCREEN } -> "Screen capture"
        scene.sources.any { it.enabled && it.type == SourceType.CAMERA } -> "Camera capture"
        else -> "Capture source not selected"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            BrandHeader(
                eyebrow = "BROADCAST WORKSPACE",
                title = "Studio",
                action = { StatusPill(session.status) },
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(scene.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$captureLabel  •  ${scene.aspectRatio.label}", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                }
                Surface(color = V02Palette.Neutral800, shape = RoundedCornerShape(50)) {
                    Text(streamQuality.displayName, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = V02Palette.Neutral300, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            PreviewCard(
                session = session,
                previewListener = previewListener,
                encoderWidth = streamQuality.width,
                encoderHeight = streamQuality.height,
            )
        }
        if (session.status == StreamStatus.ERROR) {
            item { SessionErrorCard(session.message.orEmpty(), onReleaseCapture) }
        }
        if (session.message?.contains("Reduced quality", ignoreCase = true) == true || session.message?.contains("quality raised", ignoreCase = true) == true) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Caution.copy(alpha = 0.14f)), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = V02Palette.Caution)
                        Spacer(Modifier.width(10.dp))
                        Text(session.message.orEmpty(), modifier = Modifier.weight(1f), color = V02Palette.Neutral300, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onDismissStatusMessage) { Text("Dismiss") }
                    }
                }
            }
        }
        item {
            BroadcastActionCard(
                session = session,
                canStart = canStart,
                isActive = isActive,
                actionSource = actionSource,
                actionPressed = actionPressed,
                onStart = onStart,
                onStop = onStop,
                onPractice = onPractice,
            )
        }
        item {
            QuickControls(
                session = session,
                enabled = isActive || session.status == StreamStatus.LIVE,
                onToggleMute = onToggleMute,
                onToggleRecording = onToggleRecording,
                onCreateMarker = onCreateMarker,
            )
        }
        item {
            DestinationReadiness(destination = destination, onOpenSettings = onOpenSettings)
        }
        item {
            SessionHealthCard(session = session, history = healthHistory)
        }
        item {
            SessionPreferences(
                autoStopDuration = autoStopDuration,
                enabled = !isActive,
                onAutoStopDurationChange = onAutoStopDurationChange,
                onEditScenes = onEditScenes,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

@Composable
private fun PreviewCard(
    session: StreamSessionState,
    previewListener: PreviewSurfaceView.Listener,
    encoderWidth: Int,
    encoderHeight: Int,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.Black), shape = RoundedCornerShape(24.dp)) {
        Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { context -> PreviewSurfaceView(context).apply {
                    setPreviewBufferLimit(encoderWidth, encoderHeight)
                    setPreviewListener(previewListener)
                } },
                update = { view ->
                    view.setPreviewBufferLimit(encoderWidth, encoderHeight)
                    view.setPreviewListener(previewListener)
                },
                onRelease = { it.releasePreviewListener() },
                modifier = Modifier.fillMaxSize(),
            )
            if (!session.previewReady) {
                Surface(color = V02Palette.Neutral900.copy(alpha = 0.96f), shape = RoundedCornerShape(18.dp), modifier = Modifier.padding(Spacing.xl)) {
                    Column(Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.lg), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = V02Palette.Neutral300, modifier = Modifier.size(28.dp))
                        Text("Preview is waiting", color = V02Palette.Neutral100, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(session.message ?: "Approve capture to start your live preview", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                        if (session.status == StreamStatus.PREPARING || session.status == StreamStatus.CONNECTING || session.status == StreamStatus.RECONNECTING) {
                            LinearProgressIndicator(Modifier.fillMaxWidth(0.72f), color = V02Palette.AccentBlue, trackColor = V02Palette.Neutral700)
                        }
                    }
                }
            }
            Surface(
                Modifier.align(Alignment.TopStart).padding(14.dp),
                color = if (session.status == StreamStatus.LIVE) V02Palette.AccentBlue else Color.White.copy(alpha = 0.12f),
                contentColor = Color.White,
                shape = RoundedCornerShape(50),
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (session.status == StreamStatus.LIVE) LivePulseDot()
                    Spacer(Modifier.width(if (session.status == StreamStatus.LIVE) 7.dp else 0.dp))
                    Text(if (session.status == StreamStatus.LIVE) "LIVE" else "PREVIEW", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BroadcastActionCard(
    session: StreamSessionState,
    canStart: Boolean,
    isActive: Boolean,
    actionSource: MutableInteractionSource,
    actionPressed: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPractice: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral900), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(if (session.status == StreamStatus.LIVE) "Broadcast is live" else "Ready when you are", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                when {
                    session.status == StreamStatus.LIVE -> "Your destination is receiving the stream. Keep this screen open for controls."
                    canStart -> "Check the preview, then start one destination from this workspace."
                    else -> "The capture pipeline is preparing. Keep the app in the foreground until preview is ready."
                },
                color = V02Palette.Neutral500,
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = if (session.status == StreamStatus.LIVE) onStop else onStart,
                enabled = canStart || session.status == StreamStatus.LIVE,
                interactionSource = actionSource,
                modifier = Modifier.fillMaxWidth().height(56.dp).graphicsLayer {
                    val scale = if (actionPressed) 0.97f else 1f
                    scaleX = scale
                    scaleY = scale
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (session.status == StreamStatus.LIVE) V02Palette.Danger else V02Palette.AccentBlue,
                    contentColor = Color.White,
                    disabledContainerColor = V02Palette.Neutral800,
                    disabledContentColor = V02Palette.Neutral500,
                ),
            ) {
                Icon(if (session.status == StreamStatus.LIVE) Icons.Default.Stop else Icons.Default.FiberManualRecord, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (session.status == StreamStatus.LIVE) "Stop broadcast" else "Go live", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onPractice, enabled = !isActive, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Practice locally")
            }
        }
    }
}

@Composable
private fun QuickControls(
    session: StreamSessionState,
    enabled: Boolean,
    onToggleMute: () -> Unit,
    onToggleRecording: () -> Unit,
    onCreateMarker: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        CompactControl(
            modifier = Modifier.weight(1f),
            icon = if (session.microphoneMuted) Icons.Default.MicOff else Icons.Default.Mic,
            label = if (session.microphoneMuted) "Unmute" else "Mute",
            enabled = enabled,
            onClick = onToggleMute,
        )
        CompactControl(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Movie,
            label = if (session.recording) "Stop record" else "Record",
            enabled = enabled,
            onClick = onToggleRecording,
        )
        CompactControl(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Bolt,
            label = "Mark moment",
            enabled = session.status == StreamStatus.LIVE,
            onClick = onCreateMarker,
        )
    }
}

@Composable
private fun CompactControl(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.height(48.dp), contentPadding = PaddingValues(horizontal = 6.dp), shape = RoundedCornerShape(14.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DestinationReadiness(destination: DestinationConfig, onOpenSettings: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral850), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = if (destination.isConfigured) V02Palette.AccentBlue.copy(alpha = 0.16f) else V02Palette.Caution.copy(alpha = 0.16f), shape = RoundedCornerShape(12.dp)) {
                Icon(if (destination.isConfigured) Icons.Default.CheckCircle else Icons.Default.Wifi, contentDescription = null, tint = if (destination.isConfigured) V02Palette.AccentBlue else V02Palette.Caution, modifier = Modifier.padding(10.dp).size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(if (destination.isConfigured) "${destination.platform.label} destination" else "No destination connected", fontWeight = FontWeight.SemiBold)
                Text(if (destination.isConfigured) "Credentials are stored securely on this device" else "Connect YouTube, Twitch, Kick, or a custom RTMP endpoint in Settings", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
            }
            if (!destination.isConfigured) TextButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Set up") }
        }
    }
}

@Composable
private fun SessionHealthCard(session: StreamSessionState, history: List<StreamHealthSample>) {
    val latest = history.lastOrNull()
    val thermalLabel = when (latest?.thermalStatus) {
        android.os.PowerManager.THERMAL_STATUS_LIGHT -> "Warm"
        android.os.PowerManager.THERMAL_STATUS_MODERATE -> "Hot"
        android.os.PowerManager.THERMAL_STATUS_SEVERE, android.os.PowerManager.THERMAL_STATUS_CRITICAL, android.os.PowerManager.THERMAL_STATUS_EMERGENCY, android.os.PowerManager.THERMAL_STATUS_SHUTDOWN -> "Reduce quality"
        else -> "Normal"
    }
    Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral850), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Session health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (session.status == StreamStatus.LIVE) "Live telemetry" else "Ready for a clean start", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = V02Palette.Neutral300)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Bitrate", if (session.bitrateKbps > 0) "${session.bitrateKbps} kbps" else "—", Modifier.weight(1f))
                MetricCard("FPS", if (session.status == StreamStatus.LIVE && session.fps > 0) session.fps.toString() else "—", Modifier.weight(1f))
                MetricCard("Thermal", thermalLabel, Modifier.weight(1f))
            }
            if (latest != null) {
                Text("${history.size} health samples retained locally", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall)
            } else {
                Text("Health details appear here after capture starts.", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SessionPreferences(
    autoStopDuration: AutoStopDuration,
    enabled: Boolean,
    onAutoStopDurationChange: (AutoStopDuration) -> Unit,
    onEditScenes: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral900), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Session setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Scene, quality, and destination controls stay organized here.", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide" else "Open") }
            }
            AnimatedVisibility(visible = expanded, enter = fadeIn(tween(MotionTokens.standard)) + slideInVertically(tween(MotionTokens.standard)) { it / 4 }) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text("Auto-stop", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AutoStopDuration.values().forEach { duration ->
                            FilterChip(selected = duration == autoStopDuration, onClick = { onAutoStopDurationChange(duration) }, enabled = enabled, label = { Text(duration.label) })
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedButton(onClick = onEditScenes, enabled = enabled, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.LiveTv, contentDescription = null); Spacer(Modifier.width(5.dp)); Text("Edit scene") }
                        OutlinedButton(onClick = onOpenSettings, enabled = enabled, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Settings, contentDescription = null); Spacer(Modifier.width(5.dp)); Text("Settings") }
                    }
                }
            }
        }
    }
}

@Composable
private fun LivePulseDot() {
    val transition = rememberInfiniteTransition(label = "v021LivePulse")
    val alpha by transition.animateFloat(initialValue = 0.55f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1_400, easing = LinearEasing), RepeatMode.Reverse), label = "v021LivePulseAlpha")
    Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = alpha)))
}
