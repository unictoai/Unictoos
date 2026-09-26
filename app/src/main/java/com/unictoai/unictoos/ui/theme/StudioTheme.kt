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
 * cool cyan for active / informational states. Everything else stays neutral
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
    val cyan: Color,
    val cyanDim: Color,
    val success: Color,
    val warning: Color,
    val onSignal: Color,
) {
    /** Semantic aliases used by the console surfaces. */
    val background: Color get() = base
    val surface: Color get() = surface1
    val surfaceRaised: Color get() = surface2
}

private val DarkStudioColors = StudioColors(
    base = Color(0xFF0A0D14),
    baseDeep = Color(0xFF06080D),
    surface1 = Color(0xFF111724),
    surface2 = Color(0xFF18202F),
    surface3 = Color(0xFF202A3D),
    hairline = Color(0x14FFFFFF),
    textPrimary = Color(0xFFF2F5FA),
    textSecondary = Color(0xFF9AA6B8),
    textTertiary = Color(0xFF5F6B80),
    signalRed = Color(0xFFFF3D5E),
    signalRedDim = Color(0x33FF3D5E),
    cyan = Color(0xFF3FD8FF),
    cyanDim = Color(0x263FD8FF),
    success = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
    onSignal = Color(0xFFFFFFFF),
)

private val LightStudioColors = StudioColors(
    base = Color(0xFFF4F6FA),
    baseDeep = Color(0xFFE9EDF3),
    surface1 = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF0F3F8),
    surface3 = Color(0xFFE4E9F1),
    hairline = Color(0x1A0A0D14),
    textPrimary = Color(0xFF0D1320),
    textSecondary = Color(0xFF4A5568),
    textTertiary = Color(0xFF8A94A6),
    signalRed = Color(0xFFE11D48),
    signalRedDim = Color(0x22E11D48),
    cyan = Color(0xFF0284C7),
    cyanDim = Color(0x1A0284C7),
    success = Color(0xFF059669),
    warning = Color(0xFFD97706),
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
        primary = signalRed,
        onPrimary = onSignal,
        secondary = cyan,
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
        primary = signalRed,
        onPrimary = onSignal,
        secondary = cyan,
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
