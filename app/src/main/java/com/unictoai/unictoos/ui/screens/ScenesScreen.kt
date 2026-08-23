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
import com.unictoai.unictoos.domain.PipConfig
import com.unictoai.unictoos.domain.PipPosition
import com.unictoai.unictoos.domain.PipSize
import com.unictoai.unictoos.domain.PlatformPreset
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.SourceType
import com.unictoai.unictoos.domain.SceneTransitionMode
import com.unictoai.unictoos.data.CreatorHistoryStore
import com.unictoai.unictoos.domain.StreamDestination
import com.unictoai.unictoos.domain.StreamHealthSample
import com.unictoai.unictoos.domain.StreamSessionState
import com.unictoai.unictoos.domain.StreamStatus
import com.unictoai.unictoos.ui.theme.Spacing
import com.unictoai.unictoos.ui.theme.V02Palette
import com.unictoai.unictoos.ui.theme.UnictoosTheme
import com.unictoai.unictoos.ui.components.AddSourceDialog
import com.unictoai.unictoos.ui.components.BrandHeader
import com.unictoai.unictoos.ui.components.SceneCard
import com.unictoai.unictoos.ui.components.SectionHeader
import com.unictoai.unictoos.ui.components.SourceToggleRow

@Composable
internal fun ScenesScreen(
    scenes: List<Scene>,
    selectedSceneId: String,
    selectedScene: Scene,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onAddTemplate: (String) -> Unit,
    onToggleSource: (String, String) -> Unit,
    onAddSource: (String, String, SourceType) -> Unit,
    onMoveSource: (String, String, Int) -> Unit,
    onSetSourceOpacity: (String, String, Float) -> Unit,
    onSetSourceGeometry: (String, String, Float, Float, Float, Float) -> Unit,
    onSetPipConfig: (String, PipConfig?) -> Unit,
    onUpdateTextSource: (String, String, String, Float) -> Unit,
    onSetTransition: (String, SceneTransitionMode, Long) -> Unit,
    onCreateSourceGroup: (String, String, List<String>) -> Unit,
    onToggleSourceGroup: (String, String, Boolean) -> Unit,
    onOpenStudio: () -> Unit,
) {
    var showAddSource by rememberSaveable { mutableStateOf(false) }
    var groupName by rememberSaveable(selectedScene.id) { mutableStateOf("") }
    var selectedGroupSourceIds by rememberSaveable(selectedScene.id) { mutableStateOf(emptySet<String>()) }
    Column(Modifier.fillMaxSize().padding(Spacing.xl)) {
        BrandHeader("Your layouts", "Scenes") {
            FilledTonalButton(onClick = onAdd, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, "Create scene", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("New scene")
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text("Build a repeatable look for every kind of broadcast.", color = V02Palette.Neutral500)
        Spacer(Modifier.height(Spacing.lg))
        SectionHeader("Quick templates", "Start with a proven layout, then customize it")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), contentPadding = PaddingValues(vertical = Spacing.sm)) {
            item {
                FilledTonalButton(onClick = { onAddTemplate("portrait-camera") }, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Portrait live")
                }
            }
            item {
                FilledTonalButton(onClick = { onAddTemplate("gameplay") }, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Gameplay")
                }
            }
            item {
                FilledTonalButton(onClick = { onAddTemplate("talk-show") }, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Talk show")
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            items(scenes, key = { it.id }) { scene ->
                SceneCard(scene, selected = scene.id == selectedSceneId, onClick = { onSelect(scene.id) }, onOpenStudio = onOpenStudio)
            }
            item {
                Spacer(Modifier.height(Spacing.sm))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader("Sources in ${selectedScene.name}", "Tap a source to include or hide it")
                    TextButton(onClick = { showAddSource = true }) { Text("Add source") }
                }
                Spacer(Modifier.height(Spacing.sm))
                Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral900)) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        selectedScene.sources.forEach { source ->
                            SourceToggleRow(
                                title = source.name,
                                type = "${source.type.label} • layer ${source.zIndex + 1}",
                                enabled = source.enabled,
                                onClick = { onToggleSource(selectedScene.id, source.id) },
                                onMoveUp = { onMoveSource(selectedScene.id, source.id, -1) },
                                onMoveDown = { onMoveSource(selectedScene.id, source.id, 1) },
                                opacity = source.opacity,
                                onOpacityChange = { onSetSourceOpacity(selectedScene.id, source.id, it) },
                                x = source.x,
                                y = source.y,
                                width = source.width,
                                height = source.height,
                                onGeometryChange = { x, y, width, height -> onSetSourceGeometry(selectedScene.id, source.id, x, y, width, height) },
                                textContent = source.textContent,
                                textSizeSp = source.textSizeSp,
                                onTextChange = { content, size -> onUpdateTextSource(selectedScene.id, source.id, content, size) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                PipControls(
                    scene = selectedScene,
                    onChange = { config -> onSetPipConfig(selectedScene.id, config) },
                )
                Spacer(Modifier.height(Spacing.md))
                PresentationControls(
                    scene = selectedScene,
                    groupName = groupName,
                    selectedGroupSourceIds = selectedGroupSourceIds,
                    onGroupNameChange = { groupName = it },
                    onGroupSelectionChange = { sourceId ->
                        selectedGroupSourceIds = if (sourceId in selectedGroupSourceIds) selectedGroupSourceIds - sourceId else selectedGroupSourceIds + sourceId
                    },
                    onSetTransition = { mode -> onSetTransition(selectedScene.id, mode, selectedScene.transition.safeDurationMs) },
                    onCreateGroup = {
                        onCreateSourceGroup(selectedScene.id, groupName, selectedGroupSourceIds.toList())
                        groupName = ""
                        selectedGroupSourceIds = emptySet()
                    },
                    onToggleGroup = { groupId, enabled -> onToggleSourceGroup(selectedScene.id, groupId, enabled) },
                )
            }
        }
    }
    if (showAddSource) {
        AddSourceDialog(
            onDismiss = { showAddSource = false },
            onCreate = { name, type ->
                onAddSource(selectedScene.id, name, type)
                showAddSource = false
            },
        )
    }
}

@Composable
private fun PipControls(
    scene: Scene,
    onChange: (PipConfig?) -> Unit,
) {
    val hasScreen = scene.sources.any { it.enabled && it.type == SourceType.SCREEN }
    val hasCamera = scene.sources.any { it.enabled && it.type == SourceType.CAMERA }
    val supported = hasScreen && hasCamera
    val config = scene.pipConfig ?: PipConfig()
    SectionHeader("Camera PiP", "Compose screen capture with a draggable-style camera corner")
    Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral900)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Screen + camera composition", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (supported) "The camera is rendered as a PiP layer over screen capture."
                        else "Enable one Screen and one Camera source to use PiP.",
                        color = V02Palette.Neutral500,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = config.enabled && supported,
                    enabled = supported,
                    onCheckedChange = { enabled -> onChange(if (enabled) config.copy(enabled = true) else config.copy(enabled = false)) },
                )
            }
            if (supported && config.enabled) {
                Text("Position", fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(PipPosition.values().toList()) { position ->
                        FilterChip(
                            selected = config.position == position,
                            onClick = { onChange(config.copy(position = position)) },
                            label = { Text(position.label()) },
                        )
                    }
                }
                Text("Size", fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(PipSize.values().toList()) { size ->
                        FilterChip(
                            selected = config.size == size,
                            onClick = { onChange(config.copy(size = size)) },
                            label = { Text(size.label()) },
                        )
                    }
                }
            }
        }
    }
}

private fun PipPosition.label(): String = when (this) {
    PipPosition.TOP_LEFT -> "Top left"
    PipPosition.TOP_RIGHT -> "Top right"
    PipPosition.BOTTOM_LEFT -> "Bottom left"
    PipPosition.BOTTOM_RIGHT -> "Bottom right"
}

private fun PipSize.label(): String = when (this) {
    PipSize.SMALL -> "Small"
    PipSize.MEDIUM -> "Medium"
    PipSize.LARGE -> "Large"
}

@Composable
private fun PresentationControls(
    scene: Scene,
    groupName: String,
    selectedGroupSourceIds: Set<String>,
    onGroupNameChange: (String) -> Unit,
    onGroupSelectionChange: (String) -> Unit,
    onSetTransition: (SceneTransitionMode) -> Unit,
    onCreateGroup: () -> Unit,
    onToggleGroup: (String, Boolean) -> Unit,
) {
    SectionHeader("Presentation", "Local transitions and reusable source groups")
    Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral900)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text("Scene transition", fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(SceneTransitionMode.values().toList()) { mode ->
                    FilterChip(
                        selected = scene.transition.mode == mode,
                        onClick = { onSetTransition(mode) },
                        label = { Text(mode.label) },
                    )
                }
            }
            Text("Transitions are stored with the scene. Live media crossfades remain disabled until a compositor is available.", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall)
            if (scene.sources.size >= 2) {
                HorizontalDivider(color = V02Palette.Neutral700)
                Text("Source group", fontWeight = FontWeight.SemiBold)
                Text("Select two or more sources to save a reusable group.", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(scene.sources, key = { it.id }) { source ->
                        FilterChip(
                            selected = source.id in selectedGroupSourceIds,
                            onClick = { onGroupSelectionChange(source.id) },
                            label = { Text(source.name) },
                        )
                    }
                }
                OutlinedTextField(
                    value = groupName,
                    onValueChange = onGroupNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Group name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                Button(
                    onClick = onCreateGroup,
                    enabled = groupName.trim().isNotBlank() && selectedGroupSourceIds.size >= 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Save source group")
                }
            }
            if (scene.sourceGroups.isNotEmpty()) {
                Text("Saved groups", fontWeight = FontWeight.SemiBold)
                scene.sourceGroups.forEach { group ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(group.name, fontWeight = FontWeight.SemiBold)
                            Text("${group.sourceIds.size} sources", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(checked = group.enabled, onCheckedChange = { onToggleGroup(group.id, it) })
                    }
                }
            }
        }
    }
}
