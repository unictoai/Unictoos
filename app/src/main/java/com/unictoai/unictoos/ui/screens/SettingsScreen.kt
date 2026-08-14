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
import androidx.compose.material3.Slider
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
import com.unictoai.unictoos.domain.StreamQuality
import com.unictoai.unictoos.domain.StreamQualityPreset
import com.unictoai.unictoos.domain.AudioQuality
import com.unictoai.unictoos.domain.AudioSettings
import com.unictoai.unictoos.ui.theme.UnictoosPalette
import com.unictoai.unictoos.ui.theme.UnictoosTheme
import com.unictoai.unictoos.DestinationConfig
import com.unictoai.unictoos.ui.components.BrandHeader
import com.unictoai.unictoos.ui.components.SectionHeader
import com.unictoai.unictoos.ui.components.SettingToggle
import com.unictoai.unictoos.ui.components.TrustRow

@Composable
internal fun SettingsScreen(
    destination: DestinationConfig,
    onSelectPlatform: (PlatformPreset) -> Unit,
    onSaveDestination: (PlatformPreset, String, String) -> Unit,
    onClearDestination: () -> Unit,
    adsEnabled: Boolean,
    onAdsEnabledChange: (Boolean) -> Unit,
    streamQuality: StreamQuality,
    onStreamQualityPreset: (StreamQualityPreset) -> Unit,
    onCustomStreamQualityChange: (Int, Int) -> Unit,
    thermalProtectionEnabled: Boolean,
    onThermalProtectionChange: (Boolean) -> Unit,
    audioSettings: AudioSettings,
    onAudioQualityChange: (AudioQuality) -> Unit,
    onEchoCancelerChange: (Boolean) -> Unit,
    onNoiseSuppressorChange: (Boolean) -> Unit,
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
                    Button(onClick = { onSaveDestination(selectedPlatform, serverUrl, streamKey) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (destination.isConfigured) "Update secure destination" else "Save secure destination")
                    }
                    if (destination.isConfigured) {
                        TextButton(onClick = onClearDestination, modifier = Modifier.fillMaxWidth()) {
                            Text("Remove saved destination", color = UnictoosPalette.Danger)
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
            StreamQualitySettingsCard(
                quality = streamQuality,
                onPresetSelected = onStreamQualityPreset,
                onCustomChanged = onCustomStreamQualityChange,
            )
        }
        item {
            AudioSettingsCard(
                settings = audioSettings,
                onQualityChange = onAudioQualityChange,
                onEchoChange = onEchoCancelerChange,
                onNoiseChange = onNoiseSuppressorChange,
            )
        }
        item {
            SectionHeader("Device controls", "Permissions and broadcast behavior")
            Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.Surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingToggle("Microphone", "Check audio access before going live", microphoneEnabled) { microphoneEnabled = it }
                    HorizontalDivider(color = UnictoosPalette.Stroke)
                    SettingToggle("Keep screen awake", "Prevent the display from sleeping in Studio", keepAwake) { keepAwake = it }
                    HorizontalDivider(color = UnictoosPalette.Stroke)
                    SettingToggle("Automatic thermal protection", "Lower live bitrate when the device is running hot", thermalProtectionEnabled, onThermalProtectionChange)
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
private fun StreamQualitySettingsCard(
    quality: StreamQuality,
    onPresetSelected: (StreamQualityPreset) -> Unit,
    onCustomChanged: (Int, Int) -> Unit,
) {
    var customBitrate by rememberSaveable(quality.bitrate) { mutableStateOf(quality.bitrate / 1_000_000f) }
    var customFps by rememberSaveable(quality.fps) { mutableStateOf(quality.fps) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Stream quality", "Choose the picture profile used when the next session is prepared")
        Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.Surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(StreamQualityPreset.values().toList()) { preset ->
                        FilterChip(
                            selected = quality.preset == preset,
                            onClick = {
                                onPresetSelected(preset)
                                if (preset != StreamQualityPreset.CUSTOM) {
                                    customBitrate = preset.bitrate / 1_000_000f
                                    customFps = preset.fps
                                }
                            },
                            label = { Text(preset.label) },
                        )
                    }
                }
                Text(quality.preset.description, fontWeight = FontWeight.SemiBold)
                Text(
                    "${quality.width} × ${quality.height} • ${quality.fps} FPS • ${"%.1f".format(quality.bitrateMbps)} Mbps",
                    color = UnictoosPalette.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (quality.preset == StreamQualityPreset.FULL_HD_HIGH_FPS || quality.preset == StreamQualityPreset.FULL_HD) {
                    Text("1080p needs a strong, stable upload connection.", color = UnictoosPalette.Amber, style = MaterialTheme.typography.bodySmall)
                }
                if (quality.preset == StreamQualityPreset.CUSTOM) {
                    Text("Custom bitrate: ${"%.1f".format(customBitrate)} Mbps", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = customBitrate,
                        onValueChange = { customBitrate = it },
                        onValueChangeFinished = { onCustomChanged((customBitrate * 1_000_000).toInt(), customFps) },
                        valueRange = 1f..8f,
                        steps = 6,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(24, 30, 60).forEach { fps ->
                            FilterChip(
                                selected = customFps == fps,
                                onClick = {
                                    customFps = fps
                                    onCustomChanged((customBitrate * 1_000_000).toInt(), fps)
                                },
                                label = { Text("${fps} FPS") },
                            )
                        }
                    }
                }
                Text("Changes apply when the next capture session is prepared.", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}


@Composable
private fun AudioSettingsCard(
    settings: AudioSettings,
    onQualityChange: (AudioQuality) -> Unit,
    onEchoChange: (Boolean) -> Unit,
    onNoiseChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Audio quality", "Tune voice detail and microphone cleanup for the next session")
        Card(colors = CardDefaults.cardColors(containerColor = UnictoosPalette.Surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AudioQuality.values().forEach { option ->
                        FilterChip(
                            selected = settings.quality == option,
                            onClick = { onQualityChange(option) },
                            label = { Text(option.label) },
                        )
                    }
                }
                Text(settings.quality.description, color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(color = UnictoosPalette.Stroke)
                SettingToggle("Echo cancellation", "Reduce acoustic feedback when monitoring nearby", settings.echoCanceler, onEchoChange)
                HorizontalDivider(color = UnictoosPalette.Stroke)
                SettingToggle("Noise suppression", "Reduce steady background noise from the microphone", settings.noiseSuppressor, onNoiseChange)
                Text("Some Android devices may not support every audio effect identically.", color = UnictoosPalette.TextMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
