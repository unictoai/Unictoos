package com.unictoai.unictoos.ui.screens

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
import com.unictoai.unictoos.ui.theme.MotionTokens
import com.unictoai.unictoos.ui.theme.Spacing
import com.unictoai.unictoos.ui.theme.V02Palette
import com.unictoai.unictoos.ui.theme.UnictoosTheme
import com.unictoai.unictoos.ui.components.BrandHeader
import com.unictoai.unictoos.ui.components.PreflightCard
import com.unictoai.unictoos.ui.components.SceneCard
import com.unictoai.unictoos.ui.components.SectionHeader
import com.unictoai.unictoos.ui.components.SessionErrorCard
import com.unictoai.unictoos.ui.components.SponsorBanner
import com.unictoai.unictoos.ui.components.StatusPill
import com.unictoai.unictoos.ui.components.LivePulseDot

@Composable
internal fun HomeScreen(
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
        contentPadding = PaddingValues(start = Spacing.xl, top = Spacing.lg, end = Spacing.xl, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        item { BrandHeader("Creator workspace", "Your broadcast desk") { StatusPill(session.status) } }
        if (session.status == StreamStatus.ERROR) item { SessionErrorCard(session.message.orEmpty(), onGoStudio) }
        if (showAdSlot) item { SponsorBanner() }
        item { ExecutiveHero(session = session, onOpenStudio = onGoStudio) }
        item { PreflightCard() }
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(MotionTokens.standard, easing = MotionTokens.gentleEasing)) + slideInVertically(tween(MotionTokens.standard, easing = MotionTokens.gentleEasing)) { it / 5 },
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
                enter = fadeIn(tween(MotionTokens.standard, delayMillis = 40, easing = MotionTokens.gentleEasing)) + slideInVertically(tween(MotionTokens.standard, delayMillis = 40, easing = MotionTokens.gentleEasing)) { it / 6 },
            ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader("Quick actions", "Keep your setup within one tap")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QuickAction(Icons.Default.Dashboard, "Scenes", "Build a layout", onOpenScenes, Modifier.weight(1f))
                            QuickAction(Icons.Default.Tune, "Destinations", "Manage keys", onOpenSettings, Modifier.weight(1f))
                        }
                        QuickAction(Icons.Default.Movie, "Library", "View recordings and session history", onOpenLibrary, Modifier.fillMaxWidth())
                    }
            }
        }
        item {
            AnimatedVisibility(
                visible = scenes.isNotEmpty(),
                enter = fadeIn(tween(MotionTokens.standard, delayMillis = 80, easing = MotionTokens.gentleEasing)) + slideInVertically(tween(MotionTokens.standard, delayMillis = 80, easing = MotionTokens.gentleEasing)) { it / 7 },
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
                color = V02Palette.Neutral900.copy(alpha = 0.68f),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = V02Palette.AccentBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Your stream keys stay encrypted on this device.", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

internal fun configuredDestinationLabel(destinations: List<StreamDestination>): String =
    destinations.firstOrNull { it.isConfigured }?.name ?: "Add a destination"
@Composable
internal fun ExecutiveHero(session: StreamSessionState, onOpenStudio: () -> Unit) {
    val isLive = session.status == StreamStatus.LIVE
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(tween(MotionTokens.emphasis, easing = MotionTokens.gentleEasing)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral900),
        border = BorderStroke(1.dp, V02Palette.Neutral700.copy(alpha = 0.65f)),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(if (isLive) "BROADCAST IN PROGRESS" else "BROADCAST READINESS", style = MaterialTheme.typography.labelMedium, color = if (isLive) V02Palette.AccentBlue else V02Palette.Neutral300, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    AnimatedContent(targetState = isLive, label = "heroTitle") { live ->
                        Text(if (live) "You are live." else "Ready when you are.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(color = (if (isLive) V02Palette.AccentBlue else V02Palette.Neutral300).copy(alpha = 0.12f), shape = RoundedCornerShape(16.dp)) {
                    Icon(if (isLive) Icons.Default.FiberManualRecord else Icons.Default.Bolt, contentDescription = null, tint = if (isLive) V02Palette.AccentBlue else V02Palette.Neutral300, modifier = Modifier.padding(12.dp).size(22.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLive) LivePulseDot() else Box(Modifier.size(9.dp).clip(RoundedCornerShape(50)).background(V02Palette.AccentBlue))
                Spacer(Modifier.width(9.dp))
                Text(if (isLive) "Session is active" else "All essential checks are ready", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onOpenStudio,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isLive) V02Palette.Danger else Color.White, contentColor = if (isLive) Color.White else V02Palette.Neutral950),
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
internal fun ReadinessGrid(
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
internal fun ReadinessCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, ready: Boolean, modifier: Modifier) {
    Card(
        modifier = modifier.animateContentSize(tween(MotionTokens.standard, easing = MotionTokens.gentleEasing)),
        colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral850),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = if (ready) V02Palette.AccentBlue else V02Palette.Caution, modifier = Modifier.size(18.dp))
                Icon(if (ready) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = if (ready) V02Palette.AccentBlue else V02Palette.Caution, modifier = Modifier.size(17.dp))
            }
            Text(label, color = V02Palette.Neutral500, style = MaterialTheme.typography.labelMedium)
            Text(value, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
@Composable
internal fun QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.animateContentSize(tween(MotionTokens.standard, easing = MotionTokens.gentleEasing)),
        colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral850),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Surface(color = V02Palette.AccentBlue.copy(alpha = 0.16f), shape = RoundedCornerShape(12.dp)) {
                Icon(icon, null, tint = V02Palette.AccentBlue, modifier = Modifier.padding(8.dp).size(20.dp))
            }
            Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
