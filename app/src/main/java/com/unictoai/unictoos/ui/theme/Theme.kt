package com.unictoai.unictoos.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Approved v0.2 neutral palette. Functional color is intentionally rare. */
object V02Palette {
    val Neutral950 = Color(0xFF0B0D0F)
    val Neutral900 = Color(0xFF111418)
    val Neutral850 = Color(0xFF171B20)
    val Neutral800 = Color(0xFF1E242A)
    val Neutral700 = Color(0xFF2A323A)
    val Neutral500 = Color(0xFF6E7883)
    val Neutral300 = Color(0xFFAEB7C1)
    val Neutral100 = Color(0xFFF1F4F7)
    val AccentBlue = Color(0xFF5B8DEF)
    val AccentBluePressed = Color(0xFF4677D6)
    val Danger = Color(0xFFE05A64)
    val Caution = Color(0xFFC9953B)
    val OnAccent = Color.White
}

object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val section: Dp = 40.dp
}

object MotionTokens {
    const val quick = 120
    const val standard = 200
    const val emphasis = 300
    val standardEasing: Easing = FastOutSlowInEasing
    val gentleEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

/** Legacy names remain as a migration bridge; screen commits will remove their use. */
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
    val Danger = V02Palette.Danger
    val TextPrimary = Color(0xFFF4F6F7)
    val TextMuted = Color(0xFFA6ADB7)
}

private val UnictoosDarkColors = darkColorScheme(
    primary = V02Palette.AccentBlue,
    onPrimary = V02Palette.OnAccent,
    primaryContainer = V02Palette.Neutral800,
    onPrimaryContainer = V02Palette.Neutral100,
    secondary = V02Palette.Neutral300,
    onSecondary = V02Palette.Neutral950,
    secondaryContainer = V02Palette.Neutral800,
    onSecondaryContainer = V02Palette.Neutral100,
    tertiary = V02Palette.Neutral300,
    onTertiary = V02Palette.Neutral950,
    background = V02Palette.Neutral950,
    onBackground = V02Palette.Neutral100,
    surface = V02Palette.Neutral900,
    onSurface = V02Palette.Neutral100,
    surfaceVariant = V02Palette.Neutral850,
    onSurfaceVariant = V02Palette.Neutral300,
    outline = V02Palette.Neutral700,
    error = V02Palette.Danger,
)

private val UnictoosLightColors = lightColorScheme(
    primary = Color(0xFF3D6FC9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF102754),
    secondary = Color(0xFF55616C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1E6EB),
    onSecondaryContainer = Color(0xFF1A2026),
    tertiary = Color(0xFF55616C),
    onTertiary = Color.White,
    background = Color(0xFFF7F9FB),
    onBackground = Color(0xFF111418),
    surface = Color.White,
    onSurface = Color(0xFF111418),
    surfaceVariant = Color(0xFFEEF1F4),
    onSurfaceVariant = Color(0xFF4F5A65),
    outline = Color(0xFFCBD2D9),
    error = Color(0xFFB3263A),
)

private val UnictoosTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-1.0).sp),
        headlineLarge = headlineLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
        headlineMedium = headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp),
        titleLarge = titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
        bodyLarge = bodyLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = bodyMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = bodySmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge = labelLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.15.sp),
        labelMedium = labelMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.35.sp),
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
