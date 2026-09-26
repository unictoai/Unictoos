package com.unictoai.unictoos.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unictoai.unictoos.data.CreatorHistoryStore
import com.unictoai.unictoos.data.LocalAnalyticsComparison
import com.unictoai.unictoos.data.LocalAnalyticsSession
import com.unictoai.unictoos.data.LocalAnalyticsStore
import com.unictoai.unictoos.data.LocalRecordingEditState
import com.unictoai.unictoos.data.Media3RecordingEditor
import com.unictoai.unictoos.domain.SessionSummary
import com.unictoai.unictoos.domain.StreamHealthSample
import com.unictoai.unictoos.integrations.RecordingEditResult
import com.unictoai.unictoos.integrations.RecordingTrimRequest
import com.unictoai.unictoos.streaming.StreamingDiagnostic
import com.unictoai.unictoos.streaming.StreamingDiagnostics
import com.unictoai.unictoos.ui.components.DividerHairline
import com.unictoai.unictoos.ui.components.EmptyState
import com.unictoai.unictoos.ui.components.SectionHeader
import com.unictoai.unictoos.ui.components.StatusRow
import com.unictoai.unictoos.ui.components.StudioButton
import com.unictoai.unictoos.ui.components.StudioButtonStyle
import com.unictoai.unictoos.ui.components.StudioCard
import com.unictoai.unictoos.ui.components.StudioDialog
import com.unictoai.unictoos.ui.components.StudioTextField
import com.unictoai.unictoos.ui.theme.StudioColorsScheme
import com.unictoai.unictoos.ui.theme.StudioTypeScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun LibraryScreen(onOpenStudio: () -> Unit = {}) {
    val context = LocalContext.current
    val recordingsDirectory = File(context.filesDir, "recordings")
    var recordings by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var sessionSummaries by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var latestSession by remember { mutableStateOf<SessionSummary?>(null) }
    var analyticsSessions by remember { mutableStateOf(emptyList<LocalAnalyticsSession>()) }
    var markerCount by rememberSaveable { mutableIntStateOf(0) }
    var healthSamples by remember { mutableStateOf(emptyList<StreamHealthSample>()) }
    var timeline by remember { mutableStateOf(emptyList<StreamingDiagnostic>()) }
    var renameTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var renameValue by rememberSaveable { mutableStateOf("") }
    var trimTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var trimStartSeconds by rememberSaveable { mutableStateOf("0") }
    var trimEndSeconds by rememberSaveable { mutableStateOf("60") }
    var editMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val recordingEditor = remember(context) { Media3RecordingEditor(context) }

    fun refresh() {
        recordings = recordingsDirectory.listFiles()
            ?.filter { it.extension.equals("mp4", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.name }
            .orEmpty()
    }

    fun contentUri(name: String): android.net.Uri? {
        val file = File(recordingsDirectory, name)
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
        val analytics = LocalAnalyticsStore(context)
        val sessions = history.loadSessions()
        latestSession = sessions.maxByOrNull { it.finishedAtMillis }
        sessionSummaries = sessions.map { summary ->
            "${summary.mode.name.lowercase().replaceFirstChar { it.uppercase() }} • ${summary.elapsedSeconds / 60} min • ${summary.bitrateKbps} kbps"
        }
        markerCount = history.loadMarkers().size
        healthSamples = history.loadHealthSamples()
        timeline = StreamingDiagnostics.snapshot().takeLast(24)
        analyticsSessions = withContext(Dispatchers.IO) { analytics.loadRecent() }
    }
    LaunchedEffect(recordingEditor) {
        recordingEditor.states.collect { state ->
            editMessage = when (state) {
                is LocalRecordingEditState.Completed -> "Trim export completed and was saved locally."
                is LocalRecordingEditState.Failed -> "Trim export failed: ${state.message}"
                is LocalRecordingEditState.Started -> "Trim export started. The new MP4 will be saved locally when complete."
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            LibraryHeader()
        }
        item {
            SectionHeader("Overview", subtitle = "Kept on this device only")
            StudioCard {
                StatusRow("Completed sessions", sessionSummaries.size.toString())
                StatusRow("Marked moments", markerCount.toString())
                StatusRow(
                    "Latest session",
                    sessionSummaries.lastOrNull() ?: "No completed sessions",
                )
            }
        }
        if (analyticsSessions.isNotEmpty()) {
            item {
                val comparison = LocalAnalyticsComparison.from(analyticsSessions)
                SectionHeader("Performance", subtitle = "Recent-session baseline")
                StudioCard {
                    StatusRow("Sessions compared", comparison.sessionCount.toString())
                    StatusRow("Avg duration", formatLibraryDuration(comparison.averageDurationSeconds))
                    StatusRow("Avg bitrate", "${comparison.averageBitrateKbps} kbps")
                    StatusRow("Avg dropped frames", comparison.averageDroppedFrames.toString())
                    StatusRow("Sessions with reconnects", comparison.reconnectingSessions.toString())
                }
            }
        }
        latestSession?.let { summary ->
            item {
                SectionHeader("Last session")
                StudioCard {
                    Text(
                        if (summary.mode.name == "PRACTICE") "Practice session" else "Broadcast session",
                        style = StudioTypeScale.headline,
                        color = StudioColorsScheme.textPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Finished ${
                            java.text.DateFormat.getDateTimeInstance(
                                java.text.DateFormat.SHORT,
                                java.text.DateFormat.SHORT,
                            ).format(java.util.Date(summary.finishedAtMillis))
                        }",
                        style = StudioTypeScale.caption,
                        color = StudioColorsScheme.textSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecapMetric("Duration", formatLibraryDuration(summary.elapsedSeconds), Modifier.weight(1f))
                        RecapMetric("Bitrate", "${summary.bitrateKbps} kbps", Modifier.weight(1f))
                        RecapMetric("FPS", summary.fps.toString(), Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            SectionHeader("Signal history", subtitle = "${healthSamples.size} samples retained")
            StudioCard {
                if (healthSamples.isEmpty()) {
                    Text(
                        "Health samples will appear after your first completed session.",
                        style = StudioTypeScale.body,
                        color = StudioColorsScheme.textSecondary,
                    )
                } else {
                    val recent: List<StreamHealthSample> = healthSamples.takeLast(36)
                    val maxBitrate: Int = recent.maxOf { it.bitrateKbps }.coerceAtLeast(1)
                    Row(
                        Modifier.fillMaxWidth().height(88.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        for (sample in recent) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight(
                                        (sample.bitrateKbps.toFloat() / maxBitrate).coerceIn(0.08f, 1f),
                                    )
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (sample.networkLabel == "Offline") StudioColorsScheme.warning
                                        else StudioColorsScheme.accent.copy(alpha = 0.75f),
                                    ),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Latest ${recent.lastOrNull()?.bitrateKbps ?: 0} kbps",
                        style = StudioTypeScale.caption,
                        color = StudioColorsScheme.textSecondary,
                    )
                }
            }
        }
        if (recordings.isEmpty()) {
            item {
                EmptyState(
                    title = "No recordings yet",
                    subtitle = "Press Record in Studio or start Practice mode. Saved MP4 sessions will appear here.",
                    icon = Icons.Default.Movie,
                    actionText = "Open Studio",
                    onAction = onOpenStudio,
                )
            }
        } else {
            item {
                SectionHeader("Recordings", subtitle = "${recordings.size} stored locally")
            }
            items(recordings, key = { it }) { name ->
                RecordingRow(
                    name = name,
                    onPlay = { play(name) },
                    onShare = { share(name) },
                    onTrim = { trimTarget = name },
                    onRename = { renameTarget = name; renameValue = name.removeSuffix(".mp4") },
                    onDelete = { File(recordingsDirectory, name).delete(); refresh() },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        if (timeline.isNotEmpty()) {
            item {
                SectionHeader("Session timeline", subtitle = "Recent events, kept for support")
                StudioCard {
                    timeline.asReversed().take(10).forEach { event ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                event.event.replace('_', ' '),
                                style = StudioTypeScale.bodyStrong,
                                color = StudioColorsScheme.textPrimary,
                            )
                            val detail = event.detail.ifBlank { "generation ${event.generation}" }
                            Text(
                                detail,
                                style = StudioTypeScale.caption,
                                color = StudioColorsScheme.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        DividerHairline()
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }

    if (renameTarget != null) {
        StudioDialog(
            title = "Rename recording",
            onDismiss = { renameTarget = null },
            confirmText = "Save",
            onConfirm = {
                val old = renameTarget
                val safe = renameValue.trim()
                    .ifBlank { old?.removeSuffix(".mp4").orEmpty() }
                    .replace(Regex("[^A-Za-z0-9 _-]"), "_")
                if (old != null && safe.isNotBlank()) {
                    File(recordingsDirectory, old).renameTo(File(recordingsDirectory, "$safe.mp4"))
                }
                renameTarget = null
                refresh()
            },
        ) {
            StudioTextField(value = renameValue, onValueChange = { renameValue = it }, placeholder = "Recording name")
        }
    }
    if (trimTarget != null) {
        StudioDialog(
            title = "Trim recording",
            onDismiss = { trimTarget = null },
            confirmText = "Export trim",
            onConfirm = {
                val target = trimTarget
                val start = trimStartSeconds.toLongOrNull()
                val end = trimEndSeconds.toLongOrNull()
                if (target != null && start != null && end != null) {
                    val input = File(recordingsDirectory, target)
                    when (val result = recordingEditor.trim(RecordingTrimRequest(input.absolutePath, start * 1_000L, end * 1_000L))) {
                        RecordingEditResult.Planned -> editMessage = "Trim export started. The new MP4 will be saved locally when complete."
                        is RecordingEditResult.Failure -> editMessage = result.message
                        is RecordingEditResult.Unsupported -> editMessage = result.explanation
                    }
                    trimTarget = null
                }
            },
        ) {
            Text(
                "Creates a new local MP4 without changing the original.",
                style = StudioTypeScale.caption,
                color = StudioColorsScheme.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            StudioTextField(
                value = trimStartSeconds,
                onValueChange = { trimStartSeconds = it.filter(Char::isDigit).take(8) },
                label = "Start (seconds)",
            )
            Spacer(Modifier.height(10.dp))
            StudioTextField(
                value = trimEndSeconds,
                onValueChange = { trimEndSeconds = it.filter(Char::isDigit).take(8) },
                label = "End (seconds)",
            )
        }
    }
    if (editMessage != null) {
        StudioDialog(
            title = "Local editor",
            onDismiss = { editMessage = null },
            confirmText = "Done",
            onConfirm = { editMessage = null; refresh() },
            dismissText = "",
        ) {
            Text(editMessage.orEmpty(), style = StudioTypeScale.body, color = StudioColorsScheme.textPrimary)
        }
    }
}

@Composable
private fun LibraryHeader() {
    val c = StudioColorsScheme
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text("Library", style = StudioTypeScale.display, color = c.textPrimary)
        Text("Recordings and session history", style = StudioTypeScale.body, color = c.textSecondary)
    }
}

@Composable
private fun RecapMetric(label: String, value: String, modifier: Modifier = Modifier) {
    val c = StudioColorsScheme
    StudioCard(modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Text(label.uppercase(), style = StudioTypeScale.eyebrow.copy(fontSize = 10.sp), color = c.textTertiary)
        Spacer(Modifier.height(2.dp))
        Text(value, style = StudioTypeScale.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RecordingRow(
    name: String,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onTrim: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = StudioColorsScheme
    StudioCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Movie, null, tint = c.accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name.removeSuffix(".mp4"),
                    style = StudioTypeScale.bodyStrong,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("MP4 • on this device", style = StudioTypeScale.caption, color = c.textSecondary)
            }
            IconButton(onClick = onPlay) {
                Icon(Icons.Default.PlayArrow, "Play", tint = c.textPrimary)
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, "Share", tint = c.textSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        DividerHairline()
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            androidx.compose.material3.TextButton(onClick = onTrim) {
                Icon(Icons.Default.ContentCut, null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Trim", style = StudioTypeScale.label, color = c.textSecondary)
            }
            androidx.compose.material3.TextButton(onClick = onRename) {
                Icon(Icons.Default.DriveFileRenameOutline, null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Rename", style = StudioTypeScale.label, color = c.textSecondary)
            }
            androidx.compose.material3.TextButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = c.signalRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Delete", style = StudioTypeScale.label, color = c.signalRed)
            }
        }
    }
}

private fun formatLibraryDuration(seconds: Long): String =
    "%02d:%02d".format(seconds / 60, seconds % 60)
