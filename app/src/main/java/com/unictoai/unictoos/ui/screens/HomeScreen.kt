package com.unictoai.unictoos.ui.screens

import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.SourceType
import com.unictoai.unictoos.domain.StreamDestination
import com.unictoai.unictoos.domain.StreamQuality
import com.unictoai.unictoos.domain.StreamSessionState
import com.unictoai.unictoos.domain.StreamStatus
import com.unictoai.unictoos.ui.components.BadgeTone
import com.unictoai.unictoos.ui.components.EmptyState
import com.unictoai.unictoos.ui.components.LiveDot
import com.unictoai.unictoos.ui.components.SectionHeader
import com.unictoai.unictoos.ui.components.StateBadge
import com.unictoai.unictoos.ui.components.StudioButton
import com.unictoai.unictoos.ui.components.StudioButtonStyle
import com.unictoai.unictoos.ui.components.StudioCard
import com.unictoai.unictoos.ui.theme.StudioColorsScheme
import com.unictoai.unictoos.ui.theme.StudioTypeScale
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import java.util.Locale

@Composable
internal fun HomeScreen(
    scenes: List<Scene>,
    destinations: List<StreamDestination>,
    session: StreamSessionState,
    onGoStudio: () -> Unit,
    onOpenScenes: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    streamQuality: StreamQuality,
) {
    val context = LocalContext.current
    val microphoneReady = ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
    val connectivity = context.getSystemService(ConnectivityManager::class.java)
    val networkReady = connectivity
        ?.getNetworkCapabilities(connectivity.activeNetwork)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    val captureReady = scenes.any { scene ->
        scene.sources.any { it.enabled && (it.type == SourceType.SCREEN || it.type == SourceType.CAMERA) }
    }
    val destinationReady = destinations.any { it.isConfigured }
    val isLive = session.status == StreamStatus.LIVE
    val isError = session.status == StreamStatus.ERROR

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            HomeHeader(isLive = isLive)
        }
        if (isError) {
            item {
                StudioCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = StudioColorsScheme.warning, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Stream ran into a problem", style = StudioTypeScale.bodyStrong, color = StudioColorsScheme.textPrimary)
                            Text(
                                session.message.orEmpty().ifBlank { "Check your destination and connection, then try again." },
                                style = StudioTypeScale.caption,
                                color = StudioColorsScheme.textSecondary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    StudioButton("Open Studio", onClick = onGoStudio, style = StudioButtonStyle.Ghost)
                }
            }
        }
        item {
            HeroCard(
                isLive = isLive,
                session = session,
                onGoStudio = onGoStudio,
            )
        }
        item {
            SectionHeader("Readiness", subtitle = "Everything checked before you go live")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReadinessTile(
                        icon = Icons.Default.Dashboard,
                        label = "Scenes",
                        value = if (scenes.isEmpty()) "None yet" else "${scenes.size} ready",
                        ready = scenes.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    )
                    ReadinessTile(
                        icon = Icons.Default.Wifi,
                        label = "Network",
                        value = if (networkReady) "Connected" else "Offline",
                        ready = networkReady,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReadinessTile(
                        icon = Icons.Default.LiveTv,
                        label = "Destination",
                        value = destinations.firstOrNull { it.isConfigured }?.name ?: "Not set",
                        ready = destinationReady,
                        modifier = Modifier.weight(1f),
                    )
                    ReadinessTile(
                        icon = Icons.Default.Mic,
                        label = "Microphone",
                        value = if (microphoneReady) "Ready" else "Needs access",
                        ready = microphoneReady,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            SectionHeader("Quick actions")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile(
                    icon = Icons.Default.Dashboard,
                    title = "Scenes",
                    subtitle = "Layouts",
                    onClick = onOpenScenes,
                    modifier = Modifier.weight(1f),
                )
                QuickTile(
                    icon = Icons.Default.Tune,
                    title = "Destinations",
                    subtitle = "Keys & URLs",
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f),
                )
                QuickTile(
                    icon = Icons.Default.Movie,
                    title = "Library",
                    subtitle = "Recordings",
                    onClick = onOpenLibrary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (scenes.isNotEmpty()) {
            item {
                SectionHeader(
                    "Scenes",
                    subtitle = "${scenes.size} saved",
                    action = {
                        androidx.compose.material3.TextButton(onClick = onOpenScenes) {
                            Text("View all", style = StudioTypeScale.label, color = StudioColorsScheme.accent)
                        }
                    },
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    scenes.take(2).forEach { scene ->
                        SceneRow(
                            name = scene.name,
                            detail = "${scene.sources.count { it.enabled }} sources • ${scene.aspectRatio.name.lowercase(Locale.US)}",
                            onClick = onOpenScenes,
                        )
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.Lock, null, tint = StudioColorsScheme.textTertiary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Stream keys stay encrypted on this device",
                    style = StudioTypeScale.caption,
                    color = StudioColorsScheme.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(isLive: Boolean) {
    val c = StudioColorsScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Unictoos", style = StudioTypeScale.display, color = c.textPrimary)
            Text(
                if (isLive) "You're on air" else "Your broadcast desk",
                style = StudioTypeScale.body,
                color = c.textSecondary,
            )
        }
        StateBadge(
            text = if (isLive) "Live" else "Idle",
            tone = if (isLive) BadgeTone.Live else BadgeTone.Muted,
            pulsing = isLive,
        )
    }
}

@Composable
private fun HeroCard(
    isLive: Boolean,
    session: StreamSessionState,
    onGoStudio: () -> Unit,
) {
    val c = StudioColorsScheme
    StudioCard(contentPadding = PaddingValues(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (isLive) "BROADCASTING" else "STUDIO",
                    style = StudioTypeScale.eyebrow,
                    color = if (isLive) c.signalRed else c.textTertiary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isLive) "You are live" else "Ready when you are",
                    style = StudioTypeScale.title,
                    color = c.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        isLive && session.elapsedSeconds > 0 -> formatElapsed(session.elapsedSeconds)
                        isLive -> "Connecting audience…"
                        else -> "Preview, then go live in one tap"
                    },
                    style = StudioTypeScale.body,
                    color = c.textSecondary,
                )
            }
            if (isLive) LiveDot(pulsing = true, size = 14.dp)
        }
        Spacer(Modifier.height(16.dp))
        if (isLive) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HeroStat("Bitrate", if (session.bitrateKbps > 0) "${session.bitrateKbps}k" else "—")
                HeroStat("FPS", if (session.fps > 0) "${session.fps}" else "—")
                HeroStat("Dropped", if (session.droppedFrames >= 0) "${session.droppedFrames}" else "—")
            }
            Spacer(Modifier.height(16.dp))
        }
        StudioButton(
            text = if (isLive) "Open Live Studio" else "Go Live",
            onClick = onGoStudio,
            icon = if (isLive) Icons.Default.LiveTv else Icons.Default.PlayArrow,
            style = if (isLive) StudioButtonStyle.Danger else StudioButtonStyle.Primary,
        )
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    val c = StudioColorsScheme
    Column {
        Text(value, style = StudioTypeScale.headline, color = c.textPrimary)
        Text(label.uppercase(), style = StudioTypeScale.eyebrow.copy(fontSize = 10.sp), color = c.textTertiary)
    }
}

private fun formatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
private fun ReadinessTile(
    icon: ImageVector,
    label: String,
    value: String,
    ready: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = StudioColorsScheme
    StudioCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = if (ready) c.accent else c.textTertiary, modifier = Modifier.size(18.dp))
            Icon(
                if (ready) Icons.Default.CheckCircle else Icons.Default.Warning,
                null,
                tint = if (ready) c.success else c.warning,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(label.uppercase(), style = StudioTypeScale.eyebrow.copy(fontSize = 10.sp), color = c.textTertiary)
        Spacer(Modifier.height(2.dp))
        Text(value, style = StudioTypeScale.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StudioColorsScheme
    StudioCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Icon(icon, null, tint = c.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, style = StudioTypeScale.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = StudioTypeScale.caption, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SceneRow(name: String, detail: String, onClick: () -> Unit) {
    val c = StudioColorsScheme
    StudioCard(onClick = onClick, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = StudioTypeScale.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(detail, style = StudioTypeScale.caption, color = c.textSecondary)
            }
            Icon(Icons.Default.PlayArrow, null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
        }
    }
}
