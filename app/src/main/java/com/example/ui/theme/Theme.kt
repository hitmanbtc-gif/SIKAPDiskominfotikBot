package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CivicIndigo800,
    onPrimary = Color.White,
    primaryContainer = CivicIndigo100,
    onPrimaryContainer = CivicIndigo900,
    secondary = GorontaloTeal600,
    onSecondary = Color.White,
    secondaryContainer = GorontaloTeal100,
    onSecondaryContainer = GorontaloTeal700,
    tertiary = AmberGold600,
    onTertiary = Color.White,
    tertiaryContainer = AmberGold100,
    onTertiaryContainer = AmberGold700,
    error = CoralRed700,
    onError = Color.White,
    errorContainer = CoralRed100,
    onErrorContainer = CoralRed700,
    background = SlateSurface,
    onBackground = SlateOnSurface,
    surface = Color.White,
    onSurface = SlateOnSurface,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateOnSurfaceMuted,
    outline = SlateOutline,
    outlineVariant = SlateSurfaceVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = CivicIndigo900,
    primaryContainer = CivicIndigo800,
    onPrimaryContainer = CivicIndigo100,
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = GorontaloTeal100,
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = AmberGold100,
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = CoralRed100,
    background = DarkSurface,
    onBackground = DarkOnSurface,
    surface = DarkSurfaceContainer,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceMuted,
    outline = DarkOutline,
    outlineVariant = DarkSurfaceVariant
)

@Composable
fun LapkinKominfoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
