package com.unictoai.unictoos.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unictoai.unictoos.domain.PipConfig
import com.unictoai.unictoos.domain.Scene
import com.unictoai.unictoos.domain.SceneTransitionMode
import com.unictoai.unictoos.domain.Source
import com.unictoai.unictoos.domain.SourceType
import com.unictoai.unictoos.ui.components.BadgeTone
import com.unictoai.unictoos.ui.components.DividerHairline
import com.unictoai.unictoos.ui.components.EmptyState
import com.unictoai.unictoos.ui.components.SectionHeader
import com.unictoai.unictoos.ui.components.StateBadge
import com.unictoai.unictoos.ui.components.StudioButton
import com.unictoai.unictoos.ui.components.StudioButtonStyle
import com.unictoai.unictoos.ui.components.StudioCard
import com.unictoai.unictoos.ui.components.StudioChip
import com.unictoai.unictoos.ui.components.StudioDialog
import com.unictoai.unictoos.ui.components.StudioSlider
import com.unictoai.unictoos.ui.components.StudioTextField
import com.unictoai.unictoos.ui.theme.StudioColorsScheme
import com.unictoai.unictoos.ui.theme.StudioTypeScale

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
    var showAddScene by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            ScenesHeader(onAdd = { showAddScene = true })
        }
        item {
            SectionHeader("Templates", subtitle = "Start from a proven layout")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item { TemplateCard("Portrait live", "Camera + title", Icons.Default.Videocam) { onAddTemplate("portrait-camera") } }
                item { TemplateCard("Gameplay", "Screen + face cam", Icons.Default.Dashboard) { onAddTemplate("gameplay") } }
                item { TemplateCard("Talk show", "Camera + lower third", Icons.Default.Videocam) { onAddTemplate("talk") } }
            }
        }
        item {
            SectionHeader("Your scenes", subtitle = "${scenes.size} saved")
        }
        if (scenes.isEmpty()) {
            item {
                EmptyState(
                    title = "No scenes yet",
                    subtitle = "Create your first scene or start from a template above.",
                    icon = Icons.Default.Dashboard,
                    actionText = "New scene",
                    onAction = { showAddScene = true },
                )
            }
        } else {
            items(scenes, key = { it.id }) { scene ->
                SceneListRow(
                    scene = scene,
                    selected = scene.id == selectedSceneId,
                    onSelect = { onSelect(scene.id) },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        item {
            SectionHeader(
                "Sources",
                subtitle = "Layers in “${selectedScene.name}”",
                action = {
                    androidx.compose.material3.TextButton(onClick = { showAddSource = true }) {
                        Text("+ Add", style = StudioTypeScale.label, color = StudioColorsScheme.cyan)
                    }
                },
            )
        }
        if (selectedScene.sources.isEmpty()) {
            item {
                EmptyState(
                    title = "No sources",
                    subtitle = "Add a camera, screen, text or image layer to this scene.",
                    icon = Icons.Default.Add,
                    actionText = "Add source",
                    onAction = { showAddSource = true },
                )
            }
        } else {
            items(selectedScene.sources.sortedBy { it.zIndex }, key = { it.id }) { source ->
                SourceRow(
                    source = source,
                    onToggle = { onToggleSource(selectedScene.id, source.id) },
                    onMoveUp = { onMoveSource(selectedScene.id, source.id, -1) },
                    onMoveDown = { onMoveSource(selectedScene.id, source.id, 1) },
                    onOpacityChange = { onSetSourceOpacity(selectedScene.id, source.id, it) },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            StudioButton("Open in Studio", onClick = onOpenStudio, style = StudioButtonStyle.Primary)
        }
    }

    if (showAddScene) {
        StudioDialog(
            title = "New scene",
            onDismiss = { showAddScene = false },
            confirmText = "Create",
            onConfirm = {
                onAdd()
                showAddScene = false
            },
        ) {
            Text(
                "A blank scene will be created — add sources and apply a template right after.",
                style = StudioTypeScale.caption,
                color = StudioColorsScheme.textSecondary,
            )
        }
    }

    if (showAddSource) {
        AddSourceDialog(
            onDismiss = { showAddSource = false },
            onAdd = { name, type ->
                onAddSource(selectedScene.id, name, type)
                showAddSource = false
            },
        )
    }
}

@Composable
private fun ScenesHeader(onAdd: () -> Unit) {
    val c = StudioColorsScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Scenes", style = StudioTypeScale.display, color = c.textPrimary)
            Text("Your broadcast layouts", style = StudioTypeScale.body, color = c.textSecondary)
        }
        StudioButton(
            text = "New",
            onClick = onAdd,
            icon = Icons.Default.Add,
            fullWidth = false,
            style = StudioButtonStyle.Ghost,
        )
    }
}

@Composable
private fun TemplateCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    val c = StudioColorsScheme
    StudioCard(
        onClick = onClick,
        modifier = Modifier.width(168.dp),
        contentPadding = PaddingValues(14.dp),
    ) {
        Icon(icon, null, tint = c.cyan, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, style = StudioTypeScale.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = StudioTypeScale.caption, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SceneListRow(scene: Scene, selected: Boolean, onSelect: () -> Unit) {
    val c = StudioColorsScheme
    StudioCard(onClick = onSelect, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(scene.name, style = StudioTypeScale.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${scene.sources.count { it.enabled }} of ${scene.sources.size} sources • ${scene.aspectRatio.label}",
                    style = StudioTypeScale.caption,
                    color = c.textSecondary,
                )
            }
            if (selected) {
                StateBadge(text = "Active", tone = BadgeTone.Active)
            } else {
                Icon(
                    if (scene.sources.any { it.enabled }) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    null,
                    tint = c.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SourceRow(
    source: Source,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onOpacityChange: (Float) -> Unit,
) {
    val c = StudioColorsScheme
    StudioCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (source.enabled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                null,
                tint = if (source.enabled) c.cyan else c.textTertiary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(source.name, style = StudioTypeScale.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(source.type.label, style = StudioTypeScale.caption, color = c.textSecondary)
            }
            IconButton(onClick = onMoveUp, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ArrowUpward, "Move up", tint = c.textTertiary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onMoveDown, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ArrowDownward, "Move down", tint = c.textTertiary, modifier = Modifier.size(18.dp))
            }
            StudioChip(
                text = if (source.enabled) "On" else "Off",
                selected = source.enabled,
                onClick = onToggle,
            )
        }
        if (source.enabled) {
            Spacer(Modifier.height(4.dp))
            StudioSlider(
                value = source.opacity,
                onValueChange = onOpacityChange,
                valueRange = 0f..1f,
                label = "Opacity",
                valueText = "${(source.opacity * 100).toInt()}%",
            )
        }
    }
}

@Composable
private fun AddSourceDialog(onDismiss: () -> Unit, onAdd: (String, SourceType) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(SourceType.CAMERA) }
    StudioDialog(
        title = "Add source",
        onDismiss = onDismiss,
        confirmText = "Add",
        onConfirm = { onAdd(name.ifBlank { type.label }, type) },
    ) {
        StudioTextField(value = name, onValueChange = { name = it }, placeholder = "Source name")
        Spacer(Modifier.height(12.dp))
        Text("TYPE", style = StudioTypeScale.eyebrow, color = StudioColorsScheme.textTertiary)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SourceType.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { option ->
                        StudioChip(
                            text = option.label,
                            selected = type == option,
                            onClick = { type = option },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        DividerHairline()
    }
}
