package com.unictoai.unictoos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object UnictoosPalette {
    val Ink = Color(0xFF060711)
    val InkSoft = Color(0xFF0B0D19)
    val Surface = Color(0xFF111426)
    val SurfaceRaised = Color(0xFF181C34)
    val Stroke = Color(0xFF2A2E4A)
    val Violet = Color(0xFF7C4DFF)
    val VioletBright = Color(0xFF9B73FF)
    val Magenta = Color(0xFFE83E9E)
    val Cyan = Color(0xFF67E8F9)
    val Mint = Color(0xFF48E5A4)
    val Amber = Color(0xFFFFC857)
    val Danger = Color(0xFFFF6B81)
    val TextPrimary = Color(0xFFF5F3FF)
    val TextMuted = Color(0xFFA3A7C2)
}

private val UnictoosDarkColors = darkColorScheme(
    primary = UnictoosPalette.VioletBright,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF35216F),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = UnictoosPalette.Magenta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF5C1F48),
    onSecondaryContainer = Color(0xFFFFD9EF),
    tertiary = UnictoosPalette.Cyan,
    onTertiary = Color(0xFF002329),
    background = UnictoosPalette.Ink,
    onBackground = UnictoosPalette.TextPrimary,
    surface = UnictoosPalette.Surface,
    onSurface = UnictoosPalette.TextPrimary,
    surfaceVariant = UnictoosPalette.SurfaceRaised,
    onSurfaceVariant = UnictoosPalette.TextMuted,
    outline = UnictoosPalette.Stroke,
    error = UnictoosPalette.Danger,
)

private val UnictoosLightColors = lightColorScheme(
    primary = Color(0xFF5E35C9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DEFF),
    onPrimaryContainer = Color(0xFF1C075C),
    secondary = Color(0xFFB52F77),
    onSecondary = Color.White,
    tertiary = Color(0xFF006875),
    background = Color(0xFFF8F7FC),
    onBackground = Color(0xFF171522),
    surface = Color.White,
    onSurface = Color(0xFF171522),
    surfaceVariant = Color(0xFFF0EEF8),
    onSurfaceVariant = Color(0xFF68657A),
    outline = Color(0xFFD7D2E5),
    error = Color(0xFFB3261E),
)

private val UnictoosTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp),
        headlineLarge = headlineLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        headlineMedium = headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = FontFamily.SansSerif, lineHeight = 24.sp),
        bodyMedium = bodyMedium.copy(fontFamily = FontFamily.SansSerif, lineHeight = 21.sp),
        labelLarge = labelLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun UnictoosTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme || isSystemInDarkTheme()) UnictoosDarkColors else UnictoosLightColors,
        typography = UnictoosTypography,
        content = content,
    )
}
