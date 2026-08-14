package com.unictoai.unictoos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UnictoosDarkColors = darkColorScheme(
    primary = Color(0xFF2DE2E6),
    onPrimary = Color(0xFF001F21),
    secondary = Color(0xFFFF4D8D),
    onSecondary = Color(0xFF2B0013),
    background = Color(0xFF07111F),
    onBackground = Color(0xFFE7F3FF),
    surface = Color(0xFF0E1C2E),
    onSurface = Color(0xFFE7F3FF),
    surfaceVariant = Color(0xFF172840),
    onSurfaceVariant = Color(0xFFA8BDD4),
    outline = Color(0xFF2B4563),
    error = Color(0xFFFF6B6B),
)

private val UnictoosLightColors = lightColorScheme(
    primary = Color(0xFF006B70),
    secondary = Color(0xFFB40054),
    background = Color(0xFFF6FAFF),
    surface = Color.White,
    surfaceVariant = Color(0xFFE6F1F8),
)

@Composable
fun UnictoosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) UnictoosDarkColors else UnictoosLightColors,
        content = content,
    )
}
