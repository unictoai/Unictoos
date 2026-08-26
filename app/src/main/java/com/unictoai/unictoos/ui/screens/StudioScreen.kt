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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cameraswitch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.unictoai.unictoos.DestinationConfig
import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.AudioSettings
import com.unictoai.unictoos.domain.AutoStopDuration
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.SourceType
import com.unictoai.unictoos.domain.StreamHealthSample
import com.unictoai.unictoos.health.DestinationHealth
import com.unictoai.unictoos.health.HealthState
import com.unictoai.unictoos.domain.StreamQuality
import com.unictoai.unictoos.domain.StreamSessionState
import com.unictoai.unictoos.domain.StreamStatus
import com.unictoai.unictoos.ui.PreviewSurfaceView
import com.unictoai.unictoos.ui.components.BrandHeader
import com.unictoai.unictoos.ui.components.MetricCard
import com.unictoai.unictoos.ui.components.ReadinessRow
import com.unictoai.unictoos.ui.components.SessionErrorCard
import com.unictoai.unictoos.ui.components.StatusPill
import com.unictoai.unictoos.ui.theme.MotionTokens
import com.unictoai.unictoos.ui.theme.Spacing
import com.unictoai.unictoos.ui.theme.V02Palette
import com.unictoai.unictoos.streaming.GoLiveReadiness
import com.unictoai.unictoos.streaming.CaptureModePolicy
import com.unictoai.unictoos.streaming.GoLiveReadinessPolicy

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
    onAspectRatioChange: (AspectRatio) -> Unit,
    onStart: () -> Unit,
    onPractice: () -> Unit,
    onStop: () -> Unit,
    onToggleMute: () -> Unit,
    onSwitchCamera: () -> Unit,
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
    val captureMode = CaptureModePolicy.forScene(scene)
    val captureLabel = when (captureMode) {
        "screen" -> "Screen capture"
        "camera" -> "Camera capture"
        else -> "Capture source not selected"
    }
    val context = LocalContext.current
    val effectiveQuality = remember(streamQuality, scene.aspectRatio) { streamQuality.forAspectRatio(scene.aspectRatio) }
    val connectivity = context.getSystemService(android.net.ConnectivityManager::class.java)
    val networkCapabilities = connectivity?.getNetworkCapabilities(connectivity.activeNetwork)
    val readiness = GoLiveReadinessPolicy.evaluate(
        destinationReady = destination.isConfigured,
        captureMode = captureMode,
        microphonePermission = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        cameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        networkAvailable = networkCapabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
        quality = effectiveQuality,
    )

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
                    Text(effectiveQuality.displayName, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = V02Palette.Neutral300, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            PreviewCard(
                session = session,
                previewListener = previewListener,
                encoderWidth = effectiveQuality.width,
                encoderHeight = effectiveQuality.height,
            )
        }
        if (isActive || session.status == StreamStatus.LIVE) {
            item { LiveTelemetryCard(session = session, latestHealth = healthHistory.lastOrNull()) }
        }
        item { GoLiveReadinessCard(readiness) }
        if (session.destinationHealth.isNotEmpty()) {
            item { DestinationHealthCard(session.destinationHealth) }
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
                readiness = readiness,
            )
        }
        item {
            QuickControls(
                session = session,
                enabled = isActive || session.status == StreamStatus.LIVE,
                cameraAvailable = captureMode == "camera",
                onToggleMute = onToggleMute,
                onSwitchCamera = onSwitchCamera,
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
                aspectRatio = scene.aspectRatio,
                enabled = !isActive,
                onAutoStopDurationChange = onAutoStopDurationChange,
                onAspectRatioChange = onAspectRatioChange,
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
    val motion = rememberInfiniteTransition(label = "previewSignal")
    val signalAlpha by motion.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(tween(1_700, easing = LinearEasing), RepeatMode.Reverse),
        label = "previewSignalAlpha",
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral950),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, if (session.status == StreamStatus.LIVE) V02Palette.PhotonCyan.copy(alpha = signalAlpha) else V02Palette.Neutral700.copy(alpha = 0.65f)),
    ) {
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
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = V02Palette.PhotonCyan.copy(alpha = signalAlpha), modifier = Modifier.size(28.dp))
                        Text("Preview is waiting", color = V02Palette.Neutral100, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(session.message ?: "Approve capture to start your live preview", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                        if (session.status == StreamStatus.PREPARING || session.status == StreamStatus.CONNECTING || session.status == StreamStatus.RECONNECTING) {
                            LinearProgressIndicator(Modifier.fillMaxWidth(0.72f), color = V02Palette.PhotonCyan, trackColor = V02Palette.Neutral700)
                        }
                    }
                }
            }
            if (session.status == StreamStatus.LIVE && (session.bitrateKbps > 0 || session.fps > 0)) {
                Surface(
                    Modifier.align(Alignment.TopEnd).padding(14.dp),
                    color = V02Palette.Neutral950.copy(alpha = 0.82f),
                    contentColor = V02Palette.Neutral100,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, V02Palette.PhotonCyan.copy(alpha = 0.28f)),
                ) {
                    Text(
                        "${if (session.bitrateKbps > 0) "${session.bitrateKbps} kbps" else "—"} • ${if (session.fps > 0) "${session.fps} fps" else "—"}",
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Surface(
                Modifier.align(Alignment.TopStart).padding(14.dp),
                color = if (session.status == StreamStatus.LIVE) V02Palette.PhotonCyan.copy(alpha = 0.22f) else V02Palette.Neutral800.copy(alpha = 0.78f),
                contentColor = V02Palette.Neutral100,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, if (session.status == StreamStatus.LIVE) V02Palette.PhotonCyan.copy(alpha = 0.58f) else V02Palette.Neutral700.copy(alpha = 0.62f)),
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
private fun DestinationHealthCard(destinations: List<DestinationHealth>) {
    Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral900), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text("Destination health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Per-destination insight • reconnect is coordinated by the shared encoder", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall)
            destinations.forEach { destination ->
                val (label, color) = when (destination.state) {
                    HealthState.HEALTHY -> "Healthy" to V02Palette.AccentBlue
                    HealthState.DEGRADED -> "Degraded" to V02Palette.Caution
                    HealthState.RECONNECTING -> "Reconnecting" to V02Palette.Caution
                    HealthState.FAILED -> "Failed" to V02Palette.Danger
                    HealthState.IDLE -> "Idle" to V02Palette.Neutral500
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(color))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(destination.profileName, fontWeight = FontWeight.SemiBold)
                        Text(
                            destination.lastError?.takeIf { it.isNotBlank() } ?: "$label • ${destination.retryCount}/${destination.maxRetries} retries",
                            color = V02Palette.Neutral500,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        if (destination.currentBitrate > 0L) "${destination.currentBitrate / 1000} kbps" else "—",
                        color = V02Palette.Neutral300,
                        style = MaterialTheme.typography.labelSmall,
                    )
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
    readiness: GoLiveReadiness,
) {
    Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral900), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(if (session.status == StreamStatus.LIVE) "Broadcast is live" else "Ready when you are", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                when {
                    session.status == StreamStatus.LIVE -> "Your destination is receiving the stream. Keep this screen open for controls."
                    canStart && !readiness.canStart -> readiness.blockingDetail ?: "Complete the readiness checks before going live."
                    canStart -> "Check the preview, then start one destination from this workspace."
                    else -> session.message?.takeIf { it.isNotBlank() }
                        ?: "The capture pipeline is preparing. Keep the app in the foreground until preview is ready."
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
                Text(
                    if (session.status == StreamStatus.LIVE) "Stop broadcast" else "Go Live",
                    fontWeight = FontWeight.Bold,
                )
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
    cameraAvailable: Boolean,
    onToggleMute: () -> Unit,
    onSwitchCamera: () -> Unit,
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
        if (cameraAvailable) {
            CompactControl(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Cameraswitch,
                label = "Flip camera",
                enabled = enabled,
                onClick = onSwitchCamera,
            )
        }
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
private fun LiveTelemetryCard(session: StreamSessionState, latestHealth: StreamHealthSample?) {
    val transport = latestHealth?.networkLabel?.takeIf { it.isNotBlank() } ?: "Detecting"
    Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral850), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Live signal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Measured by the active encoder", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(session.qualityTier.label, color = V02Palette.AccentBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    if (session.audioOnlyActive) Text("AUDIO ONLY", color = V02Palette.Caution, style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                TelemetryMetric("Bitrate", if (session.bitrateKbps > 0) "${session.bitrateKbps} kbps" else "—")
                TelemetryMetric("FPS", if (session.fps > 0) session.fps.toString() else "—")
                TelemetryMetric("Transport", transport)
            }
            if (session.droppedFrames >= 0) {
                Text("Dropped frames: ${session.droppedFrames}", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun RowScope.TelemetryMetric(label: String, value: String) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
private fun GoLiveReadinessCard(readiness: GoLiveReadiness) {
    val readyCount = readiness.checks.count { it.ready }
    Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral850), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Go live check", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Confirm the essentials before you start", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "$readyCount/${readiness.checks.size} ready",
                    color = if (readiness.canStart) V02Palette.AccentBlue else V02Palette.Caution,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            readiness.checks.forEach { check ->
                ReadinessRow(label = check.label, value = check.value, ready = check.ready)
            }
            Text(
                readiness.blockingDetail ?: readiness.cautionDetail ?: "All essential checks are ready for this profile.",
                color = V02Palette.Neutral500,
                style = MaterialTheme.typography.bodySmall,
            )
        }
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
    aspectRatio: AspectRatio,
    enabled: Boolean,
    onAutoStopDurationChange: (AutoStopDuration) -> Unit,
    onAspectRatioChange: (AspectRatio) -> Unit,
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
                    Text("Stream format", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AspectRatio.values().forEach { format ->
                            FilterChip(selected = format == aspectRatio, onClick = { onAspectRatioChange(format) }, enabled = enabled, label = { Text("${format.ratio} ${format.label}") })
                        }
                    }
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
