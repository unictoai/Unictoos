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
    val Ink = Color(0xFF101214)
    val InkSoft = Color(0xFF171A1D)
    val Surface = Color(0xFF1C2023)
    val SurfaceRaised = Color(0xFF24292D)
    val Stroke = Color(0xFF30363B)
    val Violet = Color(0xFF516F86)
    val VioletBright = Color(0xFF86A9C4)
    val Magenta = Color(0xFFE8617E)
    val Cyan = Color(0xFFA9D0DB)
    val Mint = Color(0xFF68D6A5)
    val Amber = Color(0xFFF2C56B)
    val Danger = Color(0xFFF06A76)
    val TextPrimary = Color(0xFFF4F6F7)
    val TextMuted = Color(0xFFA6ADB7)
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
        displaySmall = displaySmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-1.0).sp),
        headlineLarge = headlineLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp),
        headlineMedium = headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.35).sp),
        titleLarge = titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
        bodyLarge = bodyLarge.copy(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = bodyMedium.copy(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = bodySmall.copy(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge = labelLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.15.sp),
        labelMedium = labelMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.2.sp),
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
