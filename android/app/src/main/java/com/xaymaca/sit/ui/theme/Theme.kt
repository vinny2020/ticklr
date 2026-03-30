package com.xaymaca.sit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SITDarkColorScheme = darkColorScheme(
    primary = Cobalt,
    onPrimary = White,
    primaryContainer = CobaltDark,
    onPrimaryContainer = OnNavy,
    secondary = Amber,
    onSecondary = Navy,
    secondaryContainer = AmberDark,
    onSecondaryContainer = Navy,
    tertiary = Amber,
    onTertiary = Navy,
    tertiaryContainer = AmberDark,
    onTertiaryContainer = Navy,
    background = Navy,
    onBackground = OnNavy,
    surface = NavySurface,
    onSurface = OnNavy,
    surfaceVariant = NavyLight,
    onSurfaceVariant = OnNavySecondary,
    outline = OnNavyTertiary,
    outlineVariant = NavyLight,
    error = Color(0xFFCF6679),
    onError = White,
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    inverseSurface = OnNavy,
    inverseOnSurface = Navy,
    inversePrimary = CobaltDark,
    scrim = Color(0x99000000)
)

private val SITLightColorScheme = lightColorScheme(
    primary = Cobalt,
    onPrimary = White,
    primaryContainer = CobaltLight,
    onPrimaryContainer = Navy,
    secondary = AmberDark,
    onSecondary = White,
    secondaryContainer = AmberLight,
    onSecondaryContainer = Navy,
    tertiary = AmberDark,
    onTertiary = White,
    tertiaryContainer = AmberLight,
    onTertiaryContainer = Navy,
    background = LightSurface,
    onBackground = LightOnSurface,
    surface = White,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFE4E8F0),
    onSurfaceVariant = Color(0xFF4A5568),
    outline = Color(0xFF9AA5B4),
    outlineVariant = Color(0xFFCBD5E1)
)

@Composable
fun SITTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SITDarkColorScheme else SITLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SITTypography,
        content = content
    )
}
