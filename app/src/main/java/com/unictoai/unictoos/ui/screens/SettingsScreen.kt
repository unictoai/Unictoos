package com.unictoai.unictoos.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unictoai.unictoos.DestinationConfig
import com.unictoai.unictoos.domain.AudioQuality
import com.unictoai.unictoos.domain.AudioSettings
import com.unictoai.unictoos.domain.LatencyMode
import com.unictoai.unictoos.domain.PlatformPreset
import com.unictoai.unictoos.domain.StreamQuality
import com.unictoai.unictoos.domain.StreamQualityPreset
import com.unictoai.unictoos.domain.StreamStatus
import com.unictoai.unictoos.ui.components.BadgeTone
import com.unictoai.unictoos.ui.components.DividerHairline
import com.unictoai.unictoos.ui.components.SectionHeader
import com.unictoai.unictoos.ui.components.SegmentedControl
import com.unictoai.unictoos.ui.components.StateBadge
import com.unictoai.unictoos.ui.components.StudioButton
import com.unictoai.unictoos.ui.components.StudioButtonStyle
import com.unictoai.unictoos.ui.components.StudioCard
import com.unictoai.unictoos.ui.components.StudioChip
import com.unictoai.unictoos.ui.components.StudioSlider
import com.unictoai.unictoos.ui.components.StudioSwitchRow
import com.unictoai.unictoos.ui.components.StudioTextField
import com.unictoai.unictoos.ui.theme.StudioColorsScheme
import com.unictoai.unictoos.ui.theme.StudioTypeScale

@Composable
internal fun SettingsScreen(
    destination: DestinationConfig,
    sessionStatus: StreamStatus,
    onSelectPlatform: (PlatformPreset) -> Unit,
    multistreamPlatforms: Set<PlatformPreset>,
    onMultistreamPlatformChange: (PlatformPreset, Boolean) -> Boolean,
    onSaveDestination: (PlatformPreset, String, String) -> Unit,
    onClearDestination: () -> Unit,
    streamQuality: StreamQuality,
    onStreamQualityPreset: (StreamQualityPreset) -> Unit,
    onCustomStreamQualityChange: (Int, Int) -> Unit,
    thermalProtectionEnabled: Boolean,
    onThermalProtectionChange: (Boolean) -> Unit,
    adaptiveBitrateEnabled: Boolean,
    onAdaptiveBitrateChange: (Boolean) -> Unit,
    audioSettings: AudioSettings,
    onAudioQualityChange: (AudioQuality) -> Unit,
    onEchoCancelerChange: (Boolean) -> Unit,
    onNoiseSuppressorChange: (Boolean) -> Unit,
    latencyMode: LatencyMode,
    onLatencyModeChange: (LatencyMode) -> Unit,
    onExportConfig: () -> Unit,
    onImportConfig: (String) -> Unit,
    onExportDiagnostics: () -> Unit,
    credentialsResetNotice: Boolean,
    onDismissCredentialsResetNotice: () -> Unit,
) {
    val context = LocalContext.current
    var serverUrl by rememberSaveable(destination.serverUrl) { mutableStateOf(destination.serverUrl) }
    var streamKey by rememberSaveable(destination.streamKey) { mutableStateOf(destination.streamKey) }
    var showStreamKey by rememberSaveable { mutableStateOf(false) }
    var customBitrateMbps by rememberSaveable(streamQuality.bitrate) { mutableStateOf(streamQuality.bitrate / 1_000_000f) }
    var customFps by rememberSaveable(streamQuality.fps) { mutableStateOf(streamQuality.fps) }
    val importConfigLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val imported = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (imported.isNullOrBlank()) {
            Toast.makeText(context, "The selected configuration could not be read", Toast.LENGTH_LONG).show()
        } else {
            onImportConfig(imported)
        }
    }
    val isLive = sessionStatus == StreamStatus.LIVE

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            SettingsHeader()
        }
        if (credentialsResetNotice) {
            item {
                StudioCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = StudioColorsScheme.warning, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Saved keys were cleared after a security update. Re-enter your stream key below.",
                            style = StudioTypeScale.body,
                            color = StudioColorsScheme.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    StudioButton("Got it", onClick = onDismissCredentialsResetNotice, style = StudioButtonStyle.Ghost)
                }
            }
        }
        item {
            SectionHeader("Destination", subtitle = "Where your stream goes")
            StudioCard {
                Text("PLATFORM", style = StudioTypeScale.eyebrow, color = StudioColorsScheme.textTertiary)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlatformPreset.entries.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { platform ->
                                StudioChip(
                                    text = platform.label,
                                    selected = destination.platform == platform,
                                    onClick = {
                                        onSelectPlatform(platform)
                                        serverUrl = ""
                                        streamKey = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                StudioTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = "Ingest URL",
                    placeholder = destination.platform.serverHint,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    destination.platform.helper,
                    style = StudioTypeScale.caption,
                    color = StudioColorsScheme.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                StudioTextField(
                    value = streamKey,
                    onValueChange = { streamKey = it },
                    label = "Stream key",
                    placeholder = "Paste your key",
                    isPassword = !showStreamKey,
                    trailingIcon = {
                        IconButton(onClick = { showStreamKey = !showStreamKey }) {
                            Icon(
                                if (showStreamKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showStreamKey) "Hide key" else "Show key",
                                tint = StudioColorsScheme.textTertiary,
                            )
                        }
                    },
                )
                Spacer(Modifier.height(14.dp))
                val draftValid = serverUrl.isNotBlank() && (serverUrl.trim().startsWith("srt://", ignoreCase = true) || streamKey.isNotBlank())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioButton(
                        text = "Save",
                        onClick = { onSaveDestination(destination.platform, serverUrl, streamKey) },
                        modifier = Modifier.weight(1f),
                        enabled = !isLive && draftValid,
                        icon = Icons.Default.CheckCircle,
                    )
                    StudioButton(
                        text = "Clear",
                        onClick = { onClearDestination(); serverUrl = ""; streamKey = "" },
                        style = StudioButtonStyle.Ghost,
                        modifier = Modifier.weight(1f),
                        enabled = !isLive && destination.isConfigured,
                        icon = Icons.Default.Delete,
                    )
                }
                if (isLive) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Destination editing is locked while live.",
                        style = StudioTypeScale.caption,
                        color = StudioColorsScheme.textTertiary,
                    )
                }
                val dashboardUrl = when (destination.platform) {
                    PlatformPreset.YOUTUBE -> "https://studio.youtube.com/channel/UC/livestreaming"
                    PlatformPreset.TWITCH -> "https://dashboard.twitch.tv/settings/stream"
                    PlatformPreset.KICK -> "https://dashboard.kick.com/channel/stream"
                    PlatformPreset.CUSTOM -> null
                }
                if (dashboardUrl != null) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    android.net.Uri.parse(dashboardUrl),
                                ),
                            )
                        }
                    }) {
                        Text(
                            "Open ${destination.platform.label} dashboard",
                            style = StudioTypeScale.label,
                            color = StudioColorsScheme.cyan,
                        )
                    }
                }
            }
        }
        item {
            SectionHeader("Multistream", subtitle = "Send to a second platform at once")
            StudioCard {
                PlatformPreset.entries.filter { it != PlatformPreset.CUSTOM }.forEach { platform ->
                    val enabled = platform in multistreamPlatforms
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(platform.label, style = StudioTypeScale.bodyStrong, color = StudioColorsScheme.textPrimary)
                            Text(
                                "Uses this platform's saved key",
                                style = StudioTypeScale.caption,
                                color = StudioColorsScheme.textSecondary,
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = enabled,
                            onCheckedChange = { onMultistreamPlatformChange(platform, it) },
                            enabled = !isLive,
                        )
                    }
                    DividerHairline()
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "The Studio destination plus one more platform — two outputs max.",
                    style = StudioTypeScale.caption,
                    color = StudioColorsScheme.textTertiary,
                )
            }
        }
        item {
            SectionHeader("Video quality", subtitle = "Encoder output")
            StudioCard {
                SegmentedControl(
                    options = StreamQualityPreset.entries.map { it to it.label },
                    selected = streamQuality.preset,
                    onSelect = {
                        customBitrateMbps = it.bitrate / 1_000_000f
                        customFps = it.fps
                        onStreamQualityPreset(it)
                    },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    streamQuality.preset.description,
                    style = StudioTypeScale.caption,
                    color = StudioColorsScheme.textSecondary,
                )
                if (streamQuality.preset == StreamQualityPreset.CUSTOM) {
                    Spacer(Modifier.height(12.dp))
                    StudioSlider(
                        value = customBitrateMbps,
                        onValueChange = {
                            customBitrateMbps = it
                            onCustomStreamQualityChange((it * 1_000_000).toInt(), customFps)
                        },
                        valueRange = 1f..8f,
                        steps = 6,
                        label = "Bitrate",
                        valueText = "%.1f Mbps".format(customBitrateMbps),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("FRAME RATE", style = StudioTypeScale.eyebrow, color = StudioColorsScheme.textTertiary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(24, 30, 60).forEach { fps ->
                            StudioChip(
                                text = "$fps FPS",
                                selected = customFps == fps,
                                onClick = {
                                    customFps = fps
                                    onCustomStreamQualityChange((customBitrateMbps * 1_000_000).toInt(), fps)
                                },
                            )
                        }
                    }
                }
            }
        }
        item {
            SectionHeader("Audio")
            StudioCard {
                SegmentedControl(
                    options = AudioQuality.entries.map { it to it.label },
                    selected = audioSettings.quality,
                    onSelect = onAudioQualityChange,
                )
                Spacer(Modifier.height(4.dp))
                StudioSwitchRow(
                    title = "Echo cancellation",
                    subtitle = "Removes speaker feedback on calls",
                    checked = audioSettings.echoCanceler,
                    onCheckedChange = onEchoCancelerChange,
                )
                DividerHairline()
                StudioSwitchRow(
                    title = "Noise suppression",
                    subtitle = "Cuts background hum and hiss",
                    checked = audioSettings.noiseSuppressor,
                    onCheckedChange = onNoiseSuppressorChange,
                )
            }
        }
        item {
            SectionHeader("Stream health")
            StudioCard {
                SegmentedControl(
                    options = LatencyMode.entries.map { it to it.label },
                    selected = latencyMode,
                    onSelect = onLatencyModeChange,
                )
                Spacer(Modifier.height(4.dp))
                StudioSwitchRow(
                    title = "Adaptive bitrate",
                    subtitle = "Lowers quality automatically on weak upload",
                    checked = adaptiveBitrateEnabled,
                    onCheckedChange = onAdaptiveBitrateChange,
                )
                DividerHairline()
                StudioSwitchRow(
                    title = "Thermal protection",
                    subtitle = "Eases load if the phone overheats",
                    checked = thermalProtectionEnabled,
                    onCheckedChange = onThermalProtectionChange,
                )
            }
        }
        item {
            SectionHeader("Backup & support")
            StudioCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioButton(
                        text = "Export",
                        onClick = onExportConfig,
                        style = StudioButtonStyle.Ghost,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ContentCopy,
                    )
                    StudioButton(
                        text = "Import",
                        onClick = { importConfigLauncher.launch(arrayOf("application/json")) },
                        style = StudioButtonStyle.Ghost,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                StudioButton(
                    text = "Export diagnostics",
                    onClick = onExportDiagnostics,
                    style = StudioButtonStyle.Subtle,
                    icon = Icons.Default.Info,
                )
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StateBadge(text = if (destination.isConfigured) "Destination ready" else "No destination", tone = if (destination.isConfigured) BadgeTone.Ok else BadgeTone.Warn)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Unictoos ${appVersionName()}",
                style = StudioTypeScale.caption,
                color = StudioColorsScheme.textTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SettingsHeader() {
    val c = StudioColorsScheme
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text("Settings", style = StudioTypeScale.display, color = c.textPrimary)
        Text("Destinations, quality and stream health", style = StudioTypeScale.body, color = c.textSecondary)
    }
}

@Composable
private fun appVersionName(): String {
    val context = LocalContext.current
    return runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        "v${info.versionName}"
    }.getOrDefault("v0.5.4")
}
