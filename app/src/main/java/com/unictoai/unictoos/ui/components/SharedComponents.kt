package com.unictoai.unictoos.ui.components

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
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.mutableFloatStateOf
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
import com.unictoai.unictoos.streaming.PreflightOutcomeEvaluator
import com.unictoai.unictoos.streaming.PreflightOutcomeState
import com.unictoai.unictoos.ui.theme.MotionTokens
import com.unictoai.unictoos.ui.theme.Spacing
import com.unictoai.unictoos.ui.theme.V02Palette
import com.unictoai.unictoos.ui.theme.UnictoosTheme

@Composable
internal fun BrandHeader(
    eyebrow: String,
    title: String,
    action: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color.White, shape = RoundedCornerShape(15.dp), modifier = Modifier.size(48.dp)) {
                Image(
                    painter = painterResource(com.unictoai.unictoos.R.drawable.logo_unictoos),
                    contentDescription = "Unictoos logo",
                    modifier = Modifier.fillMaxSize().padding(5.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = V02Palette.Neutral300, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
        action?.invoke()
    }
}
@Composable
internal fun LivePulseDot() {
    val transition = rememberInfiniteTransition(label = "livePulse")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_400, easing = LinearEasing), RepeatMode.Reverse),
        label = "livePulseAlpha",
    )
    Box(Modifier.size(9.dp).clip(RoundedCornerShape(50)).background(V02Palette.AccentBlue.copy(alpha = alpha)))
}
@Composable
internal fun PreflightCard(
    destinationReady: Boolean,
    quality: StreamQuality,
) {
    val context = LocalContext.current
    val audioReady = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val cameraReady = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val connectivity = context.getSystemService(android.net.ConnectivityManager::class.java)
    val networkCapabilities = connectivity?.getNetworkCapabilities(connectivity.activeNetwork)
    val networkReady = networkCapabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    val outcomes = PreflightOutcomeEvaluator.evaluate(audioReady, cameraReady, networkReady, destinationReady, quality)
    val blocking = outcomes.firstOrNull { it.state == PreflightOutcomeState.ACTION_REQUIRED }
    val caution = outcomes.firstOrNull { it.state == PreflightOutcomeState.CAUTION }
    Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral850), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Preflight check", fontWeight = FontWeight.Bold)
                    Text("Know what is ready before you start", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.Wifi, null, tint = if (blocking == null) V02Palette.AccentBlue else V02Palette.Caution)
            }
            outcomes.forEach { outcome ->
                ReadinessRow(outcome.label, outcome.value, outcome.state == PreflightOutcomeState.READY)
            }
            Text(
                blocking?.detail ?: caution?.detail ?: "All essential checks are ready for the selected profile.",
                color = V02Palette.Neutral500,
                style = MaterialTheme.typography.bodySmall,
            )
            Text("Screen capture is requested by Android each time you begin a screen broadcast.", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable
internal fun SessionErrorCard(message: String, onAction: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = V02Palette.Danger.copy(alpha = 0.14f)), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = V02Palette.Danger)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Action needed", fontWeight = FontWeight.Bold)
                Text(message.ifBlank { "Unictoos could not prepare the broadcast" }, color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onAction) { Text("Fix") }
        }
    }
}

@Composable
internal fun SettingToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
internal fun SourceToggleRow(
    title: String,
    type: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    opacity: Float = 1f,
    onOpacityChange: (Float) -> Unit = {},
    x: Float = 0.05f,
    y: Float = 0.08f,
    width: Float = 0.90f,
    height: Float = 0.24f,
    onGeometryChange: (Float, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    textContent: String = "",
    textSizeSp: Float = 22f,
    onTextChange: (String, Float) -> Unit = { _, _ -> },
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = V02Palette.Neutral850, shape = RoundedCornerShape(10.dp)) {
            Icon(
                if (type.startsWith("Camera")) Icons.Default.Videocam else if (type.startsWith("Screen")) Icons.Default.LiveTv else Icons.Default.Dashboard,
                null,
                tint = V02Palette.Neutral300,
                modifier = Modifier.padding(8.dp).size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(type, color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onMoveUp, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("↑") }
        TextButton(onClick = onMoveDown, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("↓") }
            Switch(checked = enabled, onCheckedChange = { onClick() })
        }
        if (type.startsWith("Text")) {
            var draftText by rememberSaveable(title, textContent) { mutableStateOf(textContent) }
            var draftSize by rememberSaveable(title, textSizeSp) { mutableFloatStateOf(textSizeSp) }
            OutlinedTextField(
                value = draftText,
                onValueChange = {
                    draftText = it
                    onTextChange(it, draftSize)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                label = { Text("Overlay text") },
                placeholder = { Text("Add a title or callout") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Size", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(38.dp))
                Slider(
                    value = draftSize,
                    onValueChange = {
                        draftSize = it
                        onTextChange(draftText, it)
                    },
                    valueRange = 10f..72f,
                    modifier = Modifier.weight(1f),
                )
                Text("${draftSize.toInt()}sp", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(42.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Opacity", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(54.dp))
            androidx.compose.material3.Slider(value = opacity, onValueChange = onOpacityChange, enabled = enabled, modifier = Modifier.weight(1f))
            Text("${(opacity * 100).toInt()}%", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(42.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Layout", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall)
            GeometrySlider("X", x, 0f..0.95f, enabled) { onGeometryChange(it, y, width, height) }
            GeometrySlider("Y", y, 0f..0.95f, enabled) { onGeometryChange(x, it, width, height) }
            GeometrySlider("W", width, 0.05f..1f, enabled) { onGeometryChange(x, y, it, height) }
            GeometrySlider("H", height, 0.05f..1f, enabled) { onGeometryChange(x, y, width, it) }
        }
    }
}

@Composable
private fun GeometrySlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, enabled: Boolean, onValueChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(20.dp))
        Slider(value = value, onValueChange = onValueChange, valueRange = range, enabled = enabled, modifier = Modifier.weight(1f))
        Text("${(value * 100).toInt()}%", color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(42.dp))
    }
}

@Composable
internal fun TrustRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = V02Palette.Neutral850, shape = RoundedCornerShape(12.dp)) {
            Icon(icon, null, tint = V02Palette.Neutral300, modifier = Modifier.padding(9.dp).size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun SceneCard(
    scene: Scene,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onOpenStudio: (() -> Unit)? = null,
) {
    val colors = if (selected) {
        CardDefaults.cardColors(containerColor = V02Palette.AccentBlue.copy(alpha = 0.20f))
    } else {
        CardDefaults.cardColors(containerColor = V02Palette.Neutral900)
    }
    Card(
        onClick = onClick ?: {},
        colors = colors,
        shape = RoundedCornerShape(22.dp),
        border = if (onClick != null) BorderStroke(1.dp, if (selected) V02Palette.AccentBlue.copy(alpha = 0.72f) else V02Palette.Neutral700) else null,
        modifier = Modifier.animateContentSize(tween(MotionTokens.quick, easing = MotionTokens.gentleEasing)),
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(17.dp)).background(V02Palette.Neutral850),
                contentAlignment = Alignment.Center,
            ) {
                val activeSources = scene.sources.filter { it.enabled }.sortedBy { it.zIndex }.takeLast(4)
                if (activeSources.isEmpty()) {
                    Icon(sceneIcon(scene), contentDescription = "Empty ${scene.name} scene", tint = V02Palette.Neutral500, modifier = Modifier.size(25.dp))
                } else {
                    Column(Modifier.fillMaxSize().padding(7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        activeSources.forEach { source ->
                            Box(
                                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(5.dp)).background(sourcePreviewColor(source.type).copy(alpha = source.opacity.coerceIn(0.35f, 1f))),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(scene.name, fontWeight = FontWeight.SemiBold)
                Text("${scene.aspectRatio.ratio}  •  ${scene.sources.size} sources", color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(scene.sources.take(3), key = { it.id }) { source ->
                        Surface(color = Color.White.copy(alpha = 0.07f), shape = RoundedCornerShape(50)) {
                            Text(source.type.label, Modifier.padding(horizontal = 7.dp, vertical = 4.dp), color = V02Palette.Neutral500, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if (onOpenStudio != null) {
                IconButton(onClick = onOpenStudio) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Open studio", tint = V02Palette.Neutral300) }
            }
        }
    }
}

private fun sourcePreviewColor(type: SourceType): Color = when (type) {
    SourceType.SCREEN -> V02Palette.Neutral300
    SourceType.CAMERA -> V02Palette.Neutral500
    SourceType.IMAGE -> V02Palette.AccentBlue
    SourceType.TEXT -> V02Palette.AccentBlue
    SourceType.COLOR -> V02Palette.Caution.copy(alpha = 0.72f)
}

internal fun sceneIcon(scene: Scene) = when {
    scene.sources.any { it.type == SourceType.CAMERA } -> Icons.Default.Videocam
    scene.sources.any { it.type == SourceType.SCREEN } -> Icons.Default.LiveTv
    else -> Icons.Default.Dashboard
}

@Composable
internal fun AddSourceDialog(onDismiss: () -> Unit, onCreate: (String, SourceType) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var typeName by rememberSaveable { mutableStateOf(SourceType.TEXT.name) }
    val selectedType = SourceType.values().firstOrNull { it.name == typeName } ?: SourceType.TEXT
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a source") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text("Extend this scene with another layer.", color = V02Palette.Neutral500)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Source name") }, singleLine = true)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(SourceType.values().toList()) { option ->
                        FilterChip(selected = option == selectedType, onClick = { typeName = option.name }, label = { Text(option.label) })
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onCreate(name, selectedType) }) { Text("Add source") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun AddSceneDialog(onDismiss: () -> Unit, onCreate: (String, AspectRatio) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var ratioName by rememberSaveable { mutableStateOf(AspectRatio.PORTRAIT.name) }
    val ratio = AspectRatio.values().firstOrNull { it.name == ratioName } ?: AspectRatio.PORTRAIT
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create a scene") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text("Start with a layout designed for your phone.", color = V02Palette.Neutral500)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Scene name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    AspectRatio.values().forEach { option ->
                        FilterChip(selected = option == ratio, onClick = { ratioName = option.name }, label = { Text(option.label) })
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onCreate(name, ratio) }) { Text("Create scene") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun StatusPill(status: StreamStatus) {
    val (label, color) = when (status) {
        StreamStatus.LIVE -> "LIVE" to V02Palette.AccentBlue
        StreamStatus.PREPARING -> "PREPARING" to V02Palette.Caution
        StreamStatus.CONNECTING -> "CONNECTING" to V02Palette.Caution
        StreamStatus.RECONNECTING -> "RECONNECTING" to V02Palette.Caution
        StreamStatus.STOPPING -> "STOPPING" to V02Palette.Caution
        StreamStatus.STOPPED -> "STOPPED" to V02Palette.Neutral500
        StreamStatus.ERROR -> "CHECK SETUP" to V02Palette.Danger
        StreamStatus.IDLE -> "READY" to V02Palette.Neutral500
    }
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(50), modifier = Modifier.animateContentSize(tween(MotionTokens.standard, easing = MotionTokens.gentleEasing))) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (status == StreamStatus.LIVE) LivePulseDot() else Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(color))
            Spacer(Modifier.width(6.dp))
            AnimatedContent(targetState = label, label = "statusLabel") { animatedLabel ->
                Text(animatedLabel, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (subtitle.isNotBlank()) Text(subtitle, color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun ReadinessRow(label: String, value: String, ready: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(if (ready) V02Palette.AccentBlue else V02Palette.Caution))
            Spacer(Modifier.width(10.dp))
            Text(label)
        }
        Text(value, color = V02Palette.Neutral500, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun MetricCard(label: String, value: String, modifier: Modifier) {
    Card(modifier.animateContentSize(tween(MotionTokens.standard, easing = MotionTokens.gentleEasing)), colors = CardDefaults.cardColors(containerColor = V02Palette.Neutral850), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = V02Palette.Neutral500)
            Spacer(Modifier.height(3.dp))
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
