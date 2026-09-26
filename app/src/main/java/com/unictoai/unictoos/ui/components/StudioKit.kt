package com.unictoai.unictoos.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unictoai.unictoos.ui.theme.StudioColorsScheme
import com.unictoai.unictoos.ui.theme.StudioTypeScale

// ---------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------

enum class StudioButtonStyle { Primary, Danger, Ghost, Subtle }

@Composable
fun StudioButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: StudioButtonStyle = StudioButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    fullWidth: Boolean = true,
) {
    val c = StudioColorsScheme
    val (bg, fg, border) = when (style) {
        StudioButtonStyle.Primary -> Triple(c.surface3, c.textPrimary, c.hairline)
        StudioButtonStyle.Danger -> Triple(c.signalRed, c.onSignal, Color.Transparent)
        StudioButtonStyle.Ghost -> Triple(Color.Transparent, c.textPrimary, c.hairline)
        StudioButtonStyle.Subtle -> Triple(c.surface2, c.textSecondary, Color.Transparent)
    }
    val shape = RoundedCornerShape(14.dp)
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = 52.dp)
            .clip(shape)
            .then(if (border != Color.Transparent) Modifier.border(1.dp, border, shape) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                enabled = enabled && !loading,
                onClick = onClick,
            ),
        color = bg,
        contentColor = fg,
        shape = shape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val contentAlpha = if (enabled) 1f else 0.4f
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = fg,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(10.dp))
            } else if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp).alpha(contentAlpha))
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = StudioTypeScale.bodyStrong,
                color = fg.copy(alpha = contentAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun GoLiveButton(
    isLive: Boolean,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StudioColorsScheme
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    val bg = if (isLive) c.surface2 else c.signalRed
    val fg = if (isLive) c.textPrimary else c.onSignal
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(shape)
            .then(
                if (isLive) Modifier.border(1.dp, c.signalRed.copy(alpha = 0.6f), shape)
                else Modifier,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                enabled = enabled && !loading,
                onClick = onClick,
            ),
        color = bg,
        contentColor = fg,
        shape = shape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(20.dp), color = fg, strokeWidth = 2.5.dp)
                Spacer(Modifier.width(12.dp))
                Text("Working…", style = StudioTypeScale.bodyStrong, color = fg)
            } else {
                if (!isLive) {
                    LiveDot(pulsing = enabled)
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    text = when {
                        isLive -> "End Stream"
                        !enabled -> "Go Live"
                        else -> "Go Live"
                    },
                    style = StudioTypeScale.headline,
                    color = fg.copy(alpha = if (enabled) 1f else 0.5f),
                )
            }
        }
    }
}

@Composable
fun LiveDot(
    pulsing: Boolean = true,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
) {
    val c = StudioColorsScheme
    if (pulsing) {
        val transition = rememberInfiniteTransition(label = "live")
        val alpha by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "pulse",
        )
        Box(
            modifier = modifier
                .size(size)
                .alpha(alpha)
                .background(c.signalRed, CircleShape),
        )
    } else {
        Box(modifier = modifier.size(size).background(c.signalRed, CircleShape))
    }
}

// ---------------------------------------------------------------------------
// Cards & sections
// ---------------------------------------------------------------------------

@Composable
fun StudioCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = StudioColorsScheme
    val shape = RoundedCornerShape(18.dp)
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ) else Modifier,
            ),
        color = c.surface1,
        shape = shape,
        border = androidx.compose.foundation.BorderStroke(1.dp, c.hairline),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    subtitle: String? = null,
) {
    val c = StudioColorsScheme
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title.uppercase(), style = StudioTypeScale.eyebrow, color = c.textTertiary)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = StudioTypeScale.caption, color = c.textSecondary)
            }
        }
        action?.invoke()
    }
}

@Composable
fun StatusRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = StudioColorsScheme
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = StudioTypeScale.body, color = c.textSecondary, modifier = Modifier.weight(1f))
        if (trailing != null) trailing() else Text(
            value,
            style = StudioTypeScale.bodyStrong,
            color = valueColor ?: c.textPrimary,
            textAlign = TextAlign.End,
        )
    }
}

// ---------------------------------------------------------------------------
// Chips, badges, segmented control
// ---------------------------------------------------------------------------

@Composable
fun StudioChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val c = StudioColorsScheme
    val shape = CircleShape
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        color = if (selected) c.cyanDim else c.surface2,
        shape = shape,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) c.cyan.copy(alpha = 0.55f) else c.hairline,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (selected) c.cyan else c.textTertiary,
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text,
                style = StudioTypeScale.label,
                color = if (selected) c.cyan else c.textSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun StateBadge(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
) {
    val c = StudioColorsScheme
    val (bg, fg) = when (tone) {
        BadgeTone.Live -> c.signalRedDim to c.signalRed
        BadgeTone.Active -> c.cyanDim to c.cyan
        BadgeTone.Ok -> Color(0x2234D399) to c.success
        BadgeTone.Warn -> Color(0x22FBBF24) to c.warning
        BadgeTone.Muted -> c.surface3 to c.textSecondary
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (tone == BadgeTone.Live) {
                LiveDot(pulsing = pulsing, size = 7.dp)
                Spacer(Modifier.width(6.dp))
            }
            Text(text.uppercase(), style = StudioTypeScale.eyebrow.copy(fontSize = 10.sp), color = fg)
        }
    }
}

enum class BadgeTone { Live, Active, Ok, Warn, Muted }

@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StudioColorsScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = c.surface2,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                val interaction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) c.surface3 else Color.Transparent)
                        .clickable(interactionSource = interaction, indication = null, role = Role.Button) { onSelect(value) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = StudioTypeScale.label,
                        color = if (isSelected) c.textPrimary else c.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Inputs
// ---------------------------------------------------------------------------

@Composable
fun StudioTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
    isPassword: Boolean = false,
) {
    val c = StudioColorsScheme
    Column(modifier = modifier) {
        if (label != null) {
            Text(label.uppercase(), style = StudioTypeScale.eyebrow, color = c.textTertiary)
            Spacer(Modifier.height(6.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, style = StudioTypeScale.body, color = c.textTertiary) },
            textStyle = StudioTypeScale.body.copy(color = c.textPrimary),
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            trailingIcon = trailingIcon,
            visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.cyan.copy(alpha = 0.6f),
                unfocusedBorderColor = c.hairline,
                focusedContainerColor = c.surface2,
                unfocusedContainerColor = c.surface2,
                cursorColor = c.cyan,
            ),
        )
    }
}

@Composable
fun StudioSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    label: String? = null,
    valueText: String? = null,
) {
    val c = StudioColorsScheme
    Column(modifier = modifier) {
        if (label != null || valueText != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (label != null) Text(label.uppercase(), style = StudioTypeScale.eyebrow, color = c.textTertiary)
                if (valueText != null) Text(valueText, style = StudioTypeScale.label, color = c.cyan)
            }
            Spacer(Modifier.height(2.dp))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = c.cyan,
                activeTrackColor = c.cyan,
                inactiveTrackColor = c.surface3,
            ),
        )
    }
}

@Composable
fun StudioSwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = StudioColorsScheme
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = StudioTypeScale.bodyStrong, color = c.textPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = StudioTypeScale.caption, color = c.textSecondary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.baseDeep,
                checkedTrackColor = c.cyan,
                uncheckedThumbColor = c.textTertiary,
                uncheckedTrackColor = c.surface3,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

// ---------------------------------------------------------------------------
// Dialogs & empty states
// ---------------------------------------------------------------------------

@Composable
fun StudioDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissText: String = "Cancel",
    danger: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = StudioColorsScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = c.surface1,
        shape = RoundedCornerShape(20.dp),
        title = { Text(title, style = StudioTypeScale.title, color = c.textPrimary) },
        text = { Column(content = content) },
        confirmButton = {
            if (confirmText != null && onConfirm != null) {
                TextButton(
                    onClick = onConfirm,
                    colors = ButtonDefaults.textButtonColors(contentColor = if (danger) c.signalRed else c.cyan),
                ) { Text(confirmText, style = StudioTypeScale.bodyStrong) }
            }
        },
        dismissButton = {
            if (dismissText.isNotBlank()) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = c.textSecondary),
                ) { Text(dismissText, style = StudioTypeScale.bodyStrong) }
            }
        },
    )
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = StudioColorsScheme
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(64.dp).background(c.surface2, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
        Text(title, style = StudioTypeScale.headline, color = c.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = StudioTypeScale.body, color = c.textSecondary, textAlign = TextAlign.Center)
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            StudioButton(text = actionText, onClick = onAction, fullWidth = false, style = StudioButtonStyle.Ghost)
        }
    }
}

@Composable
fun DividerHairline(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = StudioColorsScheme.hairline)
}
