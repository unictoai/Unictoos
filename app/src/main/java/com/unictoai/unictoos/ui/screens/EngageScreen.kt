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
import com.unictoai.unictoos.ui.theme.Spacing
import com.unictoai.unictoos.ui.theme.V02Palette
import com.unictoai.unictoos.ui.theme.UnictoosTheme
import com.unictoai.unictoos.ui.components.BrandHeader
import com.unictoai.unictoos.ui.components.SectionHeader
import com.unictoai.unictoos.ui.components.StatusPill
import com.unictoai.unictoos.ui.components.ReadinessRow

@Composable
internal fun EngagementScreen(onOpenSettings: () -> Unit = {}) {
    var selectedChannel by rememberSaveable { mutableStateOf("All") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        item {
            BrandHeader("Community control", "Engage") { StatusPill(StreamStatus.IDLE) }
            Spacer(Modifier.height(6.dp))
            Text("Bring chat, alerts, and creator actions into one calm mobile workspace.", color = V02Palette.Neutral500)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral850), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Unified inbox", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Chat, events, and alerts in one view", color = V02Palette.Neutral500)
                        }
                        Icon(Icons.AutoMirrored.Filled.Chat, null, tint = V02Palette.Neutral300, modifier = Modifier.size(30.dp))
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        items(listOf("All", "YouTube", "Twitch", "Kick")) { channel ->
                            FilterChip(selected = selectedChannel == channel, onClick = { selectedChannel = channel }, label = { Text(channel) })
                        }
                    }
                    Text("No accounts connected", color = V02Palette.Caution, fontWeight = FontWeight.SemiBold)
                    Text("Connect OAuth accounts to read chat and events. Stream keys remain separate and are never used as chat credentials.", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Review integrations")
                    }
                }
            }
        }
        item { SectionHeader("Platform integrations", "OAuth is kept separate from stream keys") }
        items(PlatformPreset.values().toList()) { platform -> IntegrationCard(platform) }
        item {
            SectionHeader("Events and alerts", "Ready for provider adapters")
            Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral900), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    ReadinessRow("Follows, subscriptions, cheers, raids", "Event connection", false)
                    ReadinessRow("Pinned chat and quick replies", "Explicit send scope", false)
                    ReadinessRow("Clips and stream markers", "Platform API", false)
                }
            }
        }
        item {
            SectionHeader("Moderation desk", "Every action requires an explicit provider permission")
            Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral900), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text("Moderation stays out of the media path", fontWeight = FontWeight.SemiBold)
                    Text("Blocked terms, AutoMod review, timeouts, bans, slow mode, shield mode, and message deletion will be connected only after OAuth scopes and audit logging are in place.", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                    Text("No provider actions are available while disconnected.", color = V02Palette.Caution, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun IntegrationCard(platform: PlatformPreset) {
    Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral900), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = V02Palette.AccentBlue.copy(alpha = 0.20f), shape = RoundedCornerShape(12.dp)) { Text(platform.label.take(1), Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = V02Palette.AccentBlue, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(platform.label, fontWeight = FontWeight.SemiBold)
                Text(if (platform == PlatformPreset.CUSTOM) "Custom RTMP only" else "Stream key works now • OAuth tools are separate", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
            }
            Text("READY".takeIf { platform != PlatformPreset.CUSTOM } ?: "MANUAL", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}
