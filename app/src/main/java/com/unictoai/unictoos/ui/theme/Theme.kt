package com.unictoai.unictoos.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Unictoos professional palette.
 *
 * Restrained creator-tool language per the v0.2 design-token proposal: cool
 * neutral surfaces, one confident blue accent reserved for primary actions,
 * selection, and the live indicator, red for errors only, and amber for
 * genuinely cautionary states. No competing decorative accents.
 */
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

/** Compatibility names used by the app shell and existing screens. */
object UnictoosPalette {
    val Ink = Color(0xFF04050B)
    val InkSoft = Color(0xFF080B14)
    val Surface = Color(0xFF0E1220)
    val SurfaceRaised = Color(0xFF151B2C)
    val Stroke = Color(0xFF27324A)
    val Violet = Color(0xFF6F5AE8)
    val VioletBright = Color(0xFF9B86FF)
    val Magenta = Color(0xFFFF5D92)
    val Cyan = Color(0xFF4DE8FF)
    val Mint = Color(0xFF54D7B1)
    val Amber = Color(0xFFFFB45C)
    val Danger = V02Palette.Danger
    val TextPrimary = Color(0xFFF4F7FF)
    val TextMuted = Color(0xFF9AA8C2)
}

private val UnictoosDarkColors = darkColorScheme(
    primary = V02Palette.AccentBlue,
    onPrimary = V02Palette.OnAccent,
    primaryContainer = V02Palette.Neutral800,
    onPrimaryContainer = V02Palette.Neutral100,
    secondary = V02Palette.AccentBlue,
    onSecondary = V02Palette.OnAccent,
    secondaryContainer = Color(0xFF1B2A4A),
    onSecondaryContainer = Color(0xFFD3E2FF),
    tertiary = V02Palette.Caution,
    onTertiary = V02Palette.Neutral950,
    tertiaryContainer = Color(0xFF2E2417),
    onTertiaryContainer = Color(0xFFF3DFAE),
    background = V02Palette.Neutral950,
    onBackground = V02Palette.Neutral100,
    surface = V02Palette.Neutral900,
    onSurface = V02Palette.Neutral100,
    surfaceVariant = V02Palette.Neutral850,
    onSurfaceVariant = V02Palette.Neutral300,
    outline = V02Palette.Neutral700,
    outlineVariant = Color(0xFF1C263A),
    error = V02Palette.Danger,
    onError = Color.White,
)

private val UnictoosLightColors = lightColorScheme(
    primary = Color(0xFF5B46D5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E1FF),
    onPrimaryContainer = Color(0xFF1D0F5B),
    secondary = Color(0xFF006978),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB7F2FC),
    onSecondaryContainer = Color(0xFF001F25),
    tertiary = Color(0xFF9A4300),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBCA),
    onTertiaryContainer = Color(0xFF351000),
    background = Color(0xFFF8F7FC),
    onBackground = Color(0xFF11131C),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF11131C),
    surfaceVariant = Color(0xFFEDECF5),
    onSurfaceVariant = Color(0xFF4C4C5A),
    outline = Color(0xFF7D7C8A),
    outlineVariant = Color(0xFFD0CFDA),
    error = Color(0xFFBA1A35),
    onError = Color.White,
)

private val UnictoosShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
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
        shapes = UnictoosShapes,
        content = content,
    )
}
