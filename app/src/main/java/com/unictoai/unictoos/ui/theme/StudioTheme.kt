package com.unictoai.unictoos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Unictoos "Studio" design system.
 *
 * A broadcast-console aesthetic: deep charcoal surfaces, hairline borders,
 * one hot signal-red reserved for LIVE / record / destructive actions and a
 * cool accent for active / informational states. Everything else stays neutral
 * so the red always means "you are on air".
 */
@Immutable
data class StudioColors(
    val base: Color,
    val baseDeep: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val hairline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val signalRed: Color,
    val signalRedDim: Color,
    val accent: Color,
    val accentDim: Color,
    val success: Color,
    val successDim: Color,
    val warning: Color,
    val warningDim: Color,
    val onSignal: Color,
) {
    /** Semantic aliases used by the console surfaces. */
    val background: Color get() = base
    val surface: Color get() = surface1
    val surfaceRaised: Color get() = surface2
}

private val DarkStudioColors = StudioColors(
    base = Color(0xFF0B0D10),
    baseDeep = Color(0xFF050607),
    surface1 = Color(0xFF14171B),
    surface2 = Color(0xFF1B1F24),
    surface3 = Color(0xFF242A31),
    hairline = Color(0x14FFFFFF),
    textPrimary = Color(0xFFF2F4F6),
    textSecondary = Color(0xFF9AA3AD),
    textTertiary = Color(0xFF626B76),
    signalRed = Color(0xFFFF3B30),
    signalRedDim = Color(0x33FF3B30),
    accent = Color(0xFF5B8DEF),
    accentDim = Color(0x265B8DEF),
    success = Color(0xFF34C77B),
    successDim = Color(0x2234C77B),
    warning = Color(0xFFFBBF24),
    warningDim = Color(0x22FBBF24),
    onSignal = Color(0xFFFFFFFF),
)

private val LightStudioColors = StudioColors(
    base = Color(0xFFF4F6F8),
    baseDeep = Color(0xFFE6EAEE),
    surface1 = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEDF0F4),
    surface3 = Color(0xFFDFE5EB),
    hairline = Color(0x140B0D10),
    textPrimary = Color(0xFF0F1418),
    textSecondary = Color(0xFF4E5862),
    textTertiary = Color(0xFF8B95A1),
    signalRed = Color(0xFFD92D20),
    signalRedDim = Color(0x22D92D20),
    accent = Color(0xFF2F6FED),
    accentDim = Color(0x1A2F6FED),
    success = Color(0xFF0E7A4F),
    successDim = Color(0x1A0E7A4F),
    warning = Color(0xFFB54708),
    warningDim = Color(0x1AB54708),
    onSignal = Color(0xFFFFFFFF),
)

@Immutable
data class StudioType(
    val display: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val eyebrow: TextStyle,
    val eyebrowLarge: TextStyle,
    val mono: TextStyle,
)

private val StudioTypography = StudioType(
    display = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    title = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    headline = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
    bodyStrong = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    label = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
    caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    eyebrow = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
    eyebrowLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp),
    mono = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
)

val LocalStudioColors = staticCompositionLocalOf { DarkStudioColors }
val LocalStudioType = staticCompositionLocalOf { StudioTypography }

private fun StudioColors.toMaterial(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = accent,
        onPrimary = onSignal,
        secondary = accent,
        tertiary = success,
        background = base,
        surface = surface1,
        surfaceVariant = surface2,
        onBackground = textPrimary,
        onSurface = textPrimary,
        onSurfaceVariant = textSecondary,
        outline = hairline,
        error = signalRed,
        onError = onSignal,
    )
} else {
    lightColorScheme(
        primary = accent,
        onPrimary = onSignal,
        secondary = accent,
        tertiary = success,
        background = base,
        surface = surface1,
        surfaceVariant = surface2,
        onBackground = textPrimary,
        onSurface = textPrimary,
        onSurfaceVariant = textSecondary,
        outline = hairline,
        error = signalRed,
        onError = onSignal,
    )
}

@Composable
fun StudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkStudioColors else LightStudioColors
    CompositionLocalProvider(
        LocalStudioColors provides colors,
        LocalStudioType provides StudioTypography,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial(darkTheme),
            content = content,
        )
    }
}

/** Convenient accessors inside a [StudioTheme]. */
val StudioColorsScheme: StudioColors
    @Composable
    get() = LocalStudioColors.current

val StudioTypeScale: StudioType
    @Composable
    get() = LocalStudioType.current
