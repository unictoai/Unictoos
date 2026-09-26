package com.unictoai.unictoos.ui.screens

import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.unictoai.unictoos.DestinationConfig
import com.unictoai.unictoos.domain.AspectRatio
import com.unictoai.unictoos.domain.AudioSettings
import com.unictoai.unictoos.domain.AutoStopDuration
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.StreamHealthSample
import com.unictoai.unictoos.domain.StreamQuality
import com.unictoai.unictoos.domain.StreamSessionState
import com.unictoai.unictoos.domain.StreamStatus
import com.unictoai.unictoos.health.DestinationHealth
import com.unictoai.unictoos.health.HealthState
import com.unictoai.unictoos.streaming.CaptureModePolicy
import com.unictoai.unictoos.streaming.GoLiveReadinessPolicy
import com.unictoai.unictoos.ui.PreviewSurfaceView
import com.unictoai.unictoos.ui.components.BadgeTone
import com.unictoai.unictoos.ui.components.DividerHairline
import com.unictoai.unictoos.ui.components.GoLiveButton
import com.unictoai.unictoos.ui.components.LiveDot
import com.unictoai.unictoos.ui.components.SectionHeader
import com.unictoai.unictoos.ui.components.StateBadge
import com.unictoai.unictoos.ui.components.StatusRow
import com.unictoai.unictoos.ui.components.StudioButton
import com.unictoai.unictoos.ui.components.StudioButtonStyle
import com.unictoai.unictoos.ui.components.StudioCard
import com.unictoai.unictoos.ui.theme.StudioColorsScheme
import com.unictoai.unictoos.ui.theme.StudioTypeScale

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
    val isLive = session.status == StreamStatus.LIVE
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
        "screen" -> "Screen"
        "camera" -> "Camera"
        else -> "No source"
    }
    val context = LocalContext.current
    val effectiveQuality = remember(streamQuality, scene.aspectRatio) {
        streamQuality.forAspectRatio(scene.aspectRatio)
    }
    val connectivity = context.getSystemService(ConnectivityManager::class.java)
    val networkCapabilities = connectivity?.getNetworkCapabilities(connectivity.activeNetwork)
    val readiness = GoLiveReadinessPolicy.evaluate(
        destinationReady = destination.isConfigured,
        captureMode = captureMode,
        microphonePermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED,
        cameraPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED,
        networkAvailable = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
        quality = effectiveQuality,
    )
    var showDetails by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            StudioTopBar(
                sceneName = scene.name,
                captureLabel = captureLabel,
                aspectLabel = scene.aspectRatio.label,
                qualityLabel = effectiveQuality.displayName,
                status = session.status,
            )
        }
        item {
            PreviewHero(
                session = session,
                previewListener = previewListener,
                encoderWidth = effectiveQuality.width,
                encoderHeight = effectiveQuality.height,
                aspectRatio = if (scene.aspectRatio == AspectRatio.PORTRAIT) 9f / 16f else 16f / 9f,
            )
        }
        if (session.status == StreamStatus.ERROR) {
            item {
                ErrorBanner(
                    message = session.message.orEmpty().ifBlank { "Something went wrong. Check your setup and try again." },
                    onRetry = onReleaseCapture,
                )
            }
        }
        if (session.message?.contains("Reduced quality", ignoreCase = true) == true ||
            session.message?.contains("quality raised", ignoreCase = true) == true
        ) {
            item {
                NoticeBanner(message = session.message.orEmpty(), onDismiss = onDismissStatusMessage)
            }
        }
        item {
            GoLiveButton(
                isLive = isLive,
                enabled = (canStart && readiness.canStart) || isLive,
                loading = session.status == StreamStatus.CONNECTING || session.status == StreamStatus.PREPARING,
                onClick = { if (isLive) onStop() else onStart() },
            )
            if (!isLive && !readiness.canStart) {
                Spacer(Modifier.height(8.dp))
                Text(
                    (readiness.blockingDetail ?: readiness.cautionDetail ?: "Finish setup to go live"),
                    style = StudioTypeScale.caption,
                    color = StudioColorsScheme.warning,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (!isLive) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioButton(
                        text = "Practice",
                        onClick = onPractice,
                        style = StudioButtonStyle.Ghost,
                        modifier = Modifier.weight(1f),
                        enabled = canStart,
                    )
                    StudioButton(
                        text = "Scenes",
                        onClick = onEditScenes,
                        style = StudioButtonStyle.Ghost,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            ControlDock(
                session = session,
                enabled = isActive,
                cameraAvailable = captureMode == "camera",
                onToggleMute = onToggleMute,
                onSwitchCamera = onSwitchCamera,
                onToggleRecording = onToggleRecording,
                onCreateMarker = onCreateMarker,
            )
        }
        item {
            DestinationRow(
                destination = destination,
                health = session.destinationHealth,
                onOpenSettings = onOpenSettings,
            )
        }
        if (isLive || session.destinationHealth.isNotEmpty()) {
            item {
                TelemetryStrip(session = session, health = healthHistory.lastOrNull())
            }
        }
        item {
            TextButton(onClick = { showDetails = !showDetails }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (showDetails) "Hide session details" else "Session details",
                    style = StudioTypeScale.label,
                    color = StudioColorsScheme.accent,
                )
            }
            if (showDetails) {
                SessionDetails(
                    session = session,
                    autoStopDuration = autoStopDuration,
                    aspectRatio = scene.aspectRatio,
                    enabled = !isActive,
                    onAutoStopDurationChange = onAutoStopDurationChange,
                    onAspectRatioChange = onAspectRatioChange,
                    onOpenSettings = onOpenSettings,
                )
            }
        }
    }
}

@Composable
private fun StudioTopBar(
    sceneName: String,
    captureLabel: String,
    aspectLabel: String,
    qualityLabel: String,
    status: StreamStatus,
) {
    val c = StudioColorsScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                sceneName,
                style = StudioTypeScale.title,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "$captureLabel • $aspectLabel • $qualityLabel",
                style = StudioTypeScale.caption,
                color = c.textSecondary,
            )
        }
        StateBadge(
            text = when (status) {
                StreamStatus.LIVE -> "Live"
                StreamStatus.CONNECTING -> "Connecting"
                StreamStatus.RECONNECTING -> "Reconnecting"
                StreamStatus.PREPARING -> "Preparing"
                StreamStatus.ERROR -> "Error"
                StreamStatus.STOPPING -> "Stopping"
                else -> "Idle"
            },
            tone = when (status) {
                StreamStatus.LIVE -> BadgeTone.Live
                StreamStatus.CONNECTING, StreamStatus.RECONNECTING, StreamStatus.PREPARING -> BadgeTone.Active
                StreamStatus.ERROR -> BadgeTone.Warn
                else -> BadgeTone.Muted
            },
            pulsing = status == StreamStatus.LIVE,
        )
    }
}

@Composable
private fun PreviewHero(
    session: StreamSessionState,
    previewListener: PreviewSurfaceView.Listener,
    encoderWidth: Int,
    encoderHeight: Int,
    aspectRatio: Float,
) {
    val c = StudioColorsScheme
    val isLive = session.status == StreamStatus.LIVE
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(22.dp))
            .background(c.baseDeep)
            .border(1.dp, c.hairline, RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { context ->
                PreviewSurfaceView(context).apply {
                    setPreviewBufferLimit(encoderWidth, encoderHeight)
                    setPreviewListener(previewListener)
                }
            },
            update = { view ->
                view.setPreviewBufferLimit(encoderWidth, encoderHeight)
                view.setPreviewListener(previewListener)
            },
            onRelease = { it.releasePreviewListener() },
            modifier = Modifier.fillMaxSize(),
        )
        if (!session.previewReady) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp),
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = null,
                    tint = c.textTertiary,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text("Preview waiting", style = StudioTypeScale.headline, color = c.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    session.message ?: "Approve capture to start the live preview",
                    style = StudioTypeScale.caption,
                    color = c.textSecondary,
                )
                if (session.status == StreamStatus.PREPARING ||
                    session.status == StreamStatus.CONNECTING ||
                    session.status == StreamStatus.RECONNECTING
                ) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(0.6f),
                        color = c.accent,
                        trackColor = c.surface3,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLive) {
                StateBadge(text = "Live", tone = BadgeTone.Live, pulsing = true)
            } else {
                StateBadge(text = "Preview", tone = BadgeTone.Muted)
            }
            if (isLive && (session.bitrateKbps > 0 || session.fps > 0)) {
                Surface(color = c.baseDeep.copy(alpha = 0.72f), shape = RoundedCornerShape(50)) {
                    Text(
                        "${if (session.bitrateKbps > 0) "${session.bitrateKbps}k" else "—"} • ${if (session.fps > 0) "${session.fps}fps" else "—"}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = StudioTypeScale.caption,
                        color = c.textPrimary,
                    )
                }
            }
        }
        if (session.recording) {
            Row(
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveDot(pulsing = true, size = 8.dp)
                Spacer(Modifier.width(6.dp))
                Text("REC", style = StudioTypeScale.eyebrow.copy(fontSize = 10.sp), color = c.signalRed)
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    StudioCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = StudioColorsScheme.warning, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                message,
                style = StudioTypeScale.body,
                color = StudioColorsScheme.textPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(12.dp))
        StudioButton("Reset capture", onClick = onRetry, style = StudioButtonStyle.Ghost)
    }
}

@Composable
private fun NoticeBanner(message: String, onDismiss: () -> Unit) {
    StudioCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                message,
                style = StudioTypeScale.caption,
                color = StudioColorsScheme.textSecondary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss", style = StudioTypeScale.label, color = StudioColorsScheme.accent)
            }
        }
    }
}

@Composable
private fun ControlDock(
    session: StreamSessionState,
    enabled: Boolean,
    cameraAvailable: Boolean,
    onToggleMute: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleRecording: () -> Unit,
    onCreateMarker: () -> Unit,
) {
    val c = StudioColorsScheme
    StudioCard(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockButton(
                icon = if (session.microphoneMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (session.microphoneMuted) "Unmute" else "Mute",
                enabled = enabled,
                active = session.microphoneMuted,
                onClick = onToggleMute,
            )
            DockButton(
                icon = Icons.Default.Cameraswitch,
                label = "Flip",
                enabled = enabled && cameraAvailable,
                onClick = onSwitchCamera,
            )
            DockButton(
                icon = if (session.recording) Icons.Default.FiberManualRecord else Icons.Default.RadioButtonChecked,
                label = if (session.recording) "Stop Rec" else "Record",
                enabled = enabled,
                active = session.recording,
                activeTint = c.signalRed,
                onClick = onToggleRecording,
            )
            DockButton(
                icon = Icons.Default.BookmarkAdd,
                label = "Marker",
                enabled = enabled && session.status == StreamStatus.LIVE,
                onClick = onCreateMarker,
            )
        }
    }
}

@Composable
private fun DockButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
    activeTint: Color = StudioColorsScheme.accent,
) {
    val c = StudioColorsScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .padding(6.dp),
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (active) activeTint.copy(alpha = 0.16f) else c.surface2,
                    CircleShape,
                ),
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = when {
                    !enabled -> c.textTertiary.copy(alpha = 0.5f)
                    active -> activeTint
                    else -> c.textPrimary
                },
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = StudioTypeScale.caption,
            color = if (enabled) c.textSecondary else c.textTertiary.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun DestinationRow(
    destination: DestinationConfig,
    health: List<DestinationHealth>,
    onOpenSettings: () -> Unit,
) {
    val c = StudioColorsScheme
    StudioCard(onClick = onOpenSettings, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("DESTINATION", style = StudioTypeScale.eyebrow.copy(fontSize = 10.sp), color = c.textTertiary)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (destination.isConfigured) {
                        destination.platform.name.lowercase().replaceFirstChar { it.uppercase() }
                    } else {
                        "Not configured"
                    },
                    style = StudioTypeScale.bodyStrong,
                    color = c.textPrimary,
                )
                if (destination.isConfigured) {
                    Text(
                        destination.serverUrl,
                        style = StudioTypeScale.caption,
                        color = c.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val worst = health
                .firstOrNull { it.state == HealthState.FAILED }
                ?: health.firstOrNull { it.state == HealthState.RECONNECTING }
                ?: health.firstOrNull { it.state == HealthState.DEGRADED }
                ?: health.firstOrNull { it.state == HealthState.HEALTHY }
            if (worst != null && destination.isConfigured) {
                StateBadge(
                    text = when (worst.state) {
                        HealthState.HEALTHY -> "Healthy"
                        HealthState.RECONNECTING -> "Retrying"
                        HealthState.FAILED -> "Failed"
                        else -> worst.state.name.lowercase()
                    },
                    tone = when (worst.state) {
                        HealthState.HEALTHY -> BadgeTone.Ok
                        HealthState.RECONNECTING -> BadgeTone.Active
                        HealthState.FAILED -> BadgeTone.Warn
                        else -> BadgeTone.Muted
                    },
                )
            } else {
                StateBadge(
                    text = if (destination.isConfigured) "Ready" else "Setup",
                    tone = if (destination.isConfigured) BadgeTone.Ok else BadgeTone.Warn,
                )
            }
        }
    }
}

@Composable
private fun TelemetryStrip(session: StreamSessionState, health: StreamHealthSample?) {
    StudioCard(contentPadding = PaddingValues(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TelemetryStat("Bitrate", if (session.bitrateKbps > 0) "${session.bitrateKbps}k" else "—")
            TelemetryStat("FPS", if (session.fps > 0) "${session.fps}" else "—")
            TelemetryStat("Tier", session.qualityTier.label)
            TelemetryStat("Dropped", if (session.droppedFrames >= 0) "${session.droppedFrames}" else "—")
            TelemetryStat("Elapsed", formatStudioElapsed(session.elapsedSeconds))
        }
        if (health != null && health.bitrateKbps > 0) {
            Spacer(Modifier.height(8.dp))
            DividerHairline()
            Spacer(Modifier.height(8.dp))
            StatusRow("Signal", "${health.bitrateKbps} kbps")
        }
    }
}

@Composable
private fun TelemetryStat(label: String, value: String) {
    val c = StudioColorsScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = StudioTypeScale.headline, color = c.textPrimary)
        Spacer(Modifier.height(2.dp))
        Text(label.uppercase(), style = StudioTypeScale.eyebrow.copy(fontSize = 10.sp), color = c.textTertiary)
    }
}

private fun formatStudioElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
private fun SessionDetails(
    session: StreamSessionState,
    autoStopDuration: AutoStopDuration,
    aspectRatio: AspectRatio,
    enabled: Boolean,
    onAutoStopDurationChange: (AutoStopDuration) -> Unit,
    onAspectRatioChange: (AspectRatio) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = StudioColorsScheme
    StudioCard {
        Text("SESSION", style = StudioTypeScale.eyebrow.copy(fontSize = 10.sp), color = c.textTertiary)
        Spacer(Modifier.height(8.dp))
        StatusRow("Status", session.status.name.lowercase())
        StatusRow("Preview", if (session.previewReady) "Ready" else "Waiting")
        StatusRow("Capture", if (session.captureReady) "Ready" else "Waiting")
        StatusRow("Encoder", if (session.encoderReady) "Ready" else "Waiting")
        if (session.message != null) {
            Spacer(Modifier.height(4.dp))
            Text(session.message, style = StudioTypeScale.caption, color = c.textSecondary)
        }
        Spacer(Modifier.height(8.dp))
        DividerHairline()
        Spacer(Modifier.height(8.dp))
        Text("PREFERENCES", style = StudioTypeScale.eyebrow.copy(fontSize = 10.sp), color = c.textTertiary)
        Spacer(Modifier.height(8.dp))
        Text("Auto-stop", style = StudioTypeScale.label, color = c.textSecondary)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AutoStopDuration.entries.forEach { option ->
                FilterChip(
                    selected = autoStopDuration == option,
                    onClick = { if (enabled) onAutoStopDurationChange(option) },
                    label = { Text(option.label) },
                    enabled = enabled,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Orientation", style = StudioTypeScale.label, color = c.textSecondary)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AspectRatio.entries.forEach { option ->
                FilterChip(
                    selected = aspectRatio == option,
                    onClick = { if (enabled) onAspectRatioChange(option) },
                    label = { Text(option.label) },
                    enabled = enabled,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        StudioButton("Stream settings", onClick = onOpenSettings, style = StudioButtonStyle.Ghost)
    }
}
