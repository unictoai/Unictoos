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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.unictoai.unictoos.streaming.StreamingDiagnostic
import com.unictoai.unictoos.streaming.StreamingDiagnostics
import com.unictoai.unictoos.ui.theme.Spacing
import com.unictoai.unictoos.ui.theme.V02Palette
import com.unictoai.unictoos.ui.theme.UnictoosTheme
import com.unictoai.unictoos.ui.components.BrandHeader
import com.unictoai.unictoos.ui.components.ReadinessRow
import com.unictoai.unictoos.ui.components.SectionHeader

@Composable
internal fun LibraryScreen(onOpenStudio: () -> Unit = {}) {
    val context = LocalContext.current
    val recordingsDirectory = java.io.File(context.filesDir, "recordings")
    var recordings by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var sessionSummaries by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var markerCount by rememberSaveable { mutableIntStateOf(0) }
    var healthSamples by remember { mutableStateOf(emptyList<StreamHealthSample>()) }
    var timeline by remember { mutableStateOf(emptyList<StreamingDiagnostic>()) }
    var renameTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var renameValue by rememberSaveable { mutableStateOf("") }

    fun refresh() {
        recordings = recordingsDirectory.listFiles()
            ?.filter { it.extension.equals("mp4", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.name }
            .orEmpty()
    }

    fun contentUri(name: String): android.net.Uri? {
        val file = java.io.File(recordingsDirectory, name)
        return if (file.exists()) runCatching {
            androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull() else null
    }

    fun play(name: String) {
        contentUri(name)?.let { uri ->
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/mp4")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            }
        }
    }

    fun share(name: String) {
        contentUri(name)?.let { uri ->
            runCatching {
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Share recording"))
            }
        }
    }

    LaunchedEffect(Unit) {
        refresh()
        val history = CreatorHistoryStore(context)
        val sessions = history.loadSessions()
        sessionSummaries = sessions.map { summary -> "${summary.mode.name.lowercase().replaceFirstChar { it.uppercase() }} • ${summary.elapsedSeconds / 60} min • ${summary.bitrateKbps} kbps" }
        markerCount = history.loadMarkers().size
        healthSamples = history.loadHealthSamples()
        timeline = StreamingDiagnostics.snapshot().takeLast(24)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        item {
            BrandHeader("Your content", "Library")
            Spacer(Modifier.height(6.dp))
            Text("Recordings stay on this device until you choose to share them.", color = V02Palette.Neutral500)
        }
        item {
            SectionHeader("Creator analytics", "Local history only")
            Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral850), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    ReadinessRow("Completed sessions", sessionSummaries.size.toString(), sessionSummaries.isNotEmpty())
                    ReadinessRow("Marked moments", markerCount.toString(), markerCount > 0)
                    ReadinessRow("Latest session", sessionSummaries.lastOrNull() ?: "No completed sessions", sessionSummaries.isNotEmpty())
                }
            }
        }
        item {
            SectionHeader("Stream health history", "Persisted locally from completed sessions")
            Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral850), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    if (healthSamples.isEmpty()) {
                        Text("Health samples will appear after your first completed session.", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                    } else {
                        val recent: List<StreamHealthSample> = healthSamples.takeLast(36)
                        val maxBitrate: Int = recent.maxOf { sample -> sample.bitrateKbps }.coerceAtLeast(1)
                        Row(Modifier.fillMaxWidth().height(96.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            for (sample in recent) {
                                Box(
                                    Modifier.weight(1f).fillMaxHeight((sample.bitrateKbps.toFloat() / maxBitrate).coerceIn(0.08f, 1f)).clip(RoundedCornerShape(3.dp)).background(if (sample.networkLabel == "Offline") V02Palette.Caution else V02Palette.Neutral300),
                                )
                            }
                        }
                        Text("${healthSamples.size} samples retained • latest ${recent.lastOrNull()?.bitrateKbps ?: 0} kbps", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item {
            SectionHeader("Session timeline", "Recent lifecycle events retained for support")
            Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral850), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    if (timeline.isEmpty()) {
                        Text("Events will appear after Unictoos prepares or starts a session.", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                    } else {
                        timeline.asReversed().take(12).forEach { event ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(event.event.replace('_', ' '), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                val detail = event.detail.ifBlank { "generation ${event.generation}" }
                                Text(detail, color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        if (recordings.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral850), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        Icon(Icons.Default.Movie, null, tint = V02Palette.AccentBlue, modifier = Modifier.size(32.dp))
                        Text("No recordings yet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text("Press Record in Studio or start Practice mode. Saved MP4 sessions will appear here.", color = V02Palette.Neutral500)
                        FilledTonalButton(onClick = onOpenStudio, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Open Studio")
                        }
                    }
                }
            }
        } else {
            item { SectionHeader("Saved recordings", "Stored locally on this device") }
            items(recordings, key = { it }) { name ->
                Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral900), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = V02Palette.AccentBlue.copy(alpha = 0.16f), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.Movie, null, tint = V02Palette.Neutral300, modifier = Modifier.padding(Spacing.md).size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(name.removeSuffix(".mp4"), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("MP4 • app-private storage", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            OutlinedButton(onClick = { play(name) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Play")
                            }
                            OutlinedButton(onClick = { share(name) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share")
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { renameTarget = name; renameValue = name.removeSuffix(".mp4") }) { Text("Rename") }
                            TextButton(onClick = { java.io.File(recordingsDirectory, name).delete(); refresh() }) { Text("Delete", color = V02Palette.Danger) }
                        }
                    }
                }
            }
        }
        item {
            SectionHeader("Creator assets", "Overlays, intros, and saved scene media")
            ReadinessRow("Scene templates", "Available", true)
            ReadinessRow("Overlay library", "Next milestone", false)
            ReadinessRow("Cloud backup", "Not connected", false)
        }
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename recording") },
            text = { OutlinedTextField(value = renameValue, onValueChange = { renameValue = it }, label = { Text("Recording name") }, singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    val old = renameTarget
                    val safe = renameValue.trim().ifBlank { old?.removeSuffix(".mp4").orEmpty() }.replace(Regex("[^A-Za-z0-9 _-]"), "_")
                    if (old != null && safe.isNotBlank()) java.io.File(recordingsDirectory, old).renameTo(java.io.File(recordingsDirectory, "$safe.mp4"))
                    renameTarget = null
                    refresh()
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } },
        )
    }
}
