package com.xaymaca.sit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Base MaterialTheme schemes re-derived from the warm palette (TIC-49).
// Neutrals are the WarmTheme Subtle palette (paper / cardBg / ink / ink2 / ink3);
// accents are the warm category palette. Overriding the full surfaceContainer
// family is deliberate — Material3's baseline values are purple-tinted grays and
// would otherwise leak through the nav bar, menus, sheets, and elevated cards.
private val warmLight = WarmTheme.lightPalette(Warmth.Subtle)
private val warmDark = WarmTheme.darkPalette(Warmth.Subtle)

private val SITLightColorScheme = lightColorScheme(
    primary = WarmPrimary,
    onPrimary = WarmOnAccent,
    primaryContainer = WarmPrimaryContainer,
    onPrimaryContainer = warmLight.ink,
    secondary = WarmSecondary,
    onSecondary = WarmOnAccent,
    secondaryContainer = WarmSecondaryContainer,
    onSecondaryContainer = warmLight.ink,
    tertiary = WarmTertiary,
    onTertiary = WarmOnAccent,
    tertiaryContainer = WarmTertiaryContainer,
    onTertiaryContainer = warmLight.ink,
    background = warmLight.paper,
    onBackground = warmLight.ink,
    surface = warmLight.paper,
    onSurface = warmLight.ink,
    surfaceVariant = warmLight.paperSurface,
    onSurfaceVariant = warmLight.ink2,
    surfaceTint = WarmPrimary,
    inverseSurface = warmLight.ink,
    inverseOnSurface = warmLight.paper,
    inversePrimary = WarmPrimaryContainer,
    surfaceBright = warmLight.paper,
    surfaceDim = Color(0xFFE8E1D3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = warmLight.cardBg,
    surfaceContainer = warmLight.paperSurface,
    surfaceContainerHigh = Color(0xFFEDE8DC),
    surfaceContainerHighest = Color(0xFFE7E1D4),
    outline = warmLight.ink3,
    outlineVariant = Color(0xFFE2DACA),
    error = WarmError,
    onError = WarmOnAccent,
    errorContainer = WarmErrorContainer,
    onErrorContainer = warmLight.ink,
    scrim = Color(0x99000000)
)

private val SITDarkColorScheme = darkColorScheme(
    primary = WarmPrimaryDark,
    onPrimary = Color(0xFF3A2317),
    primaryContainer = WarmPrimaryContainerDark,
    onPrimaryContainer = WarmPrimaryContainer,
    secondary = WarmSecondaryDark,
    onSecondary = Color(0xFF362808),
    secondaryContainer = WarmSecondaryContainerDark,
    onSecondaryContainer = WarmSecondaryContainer,
    tertiary = WarmTertiaryDark,
    onTertiary = Color(0xFF1E2D17),
    tertiaryContainer = WarmTertiaryContainerDark,
    onTertiaryContainer = WarmTertiaryContainer,
    background = warmDark.paper,
    onBackground = warmDark.ink,
    surface = warmDark.paper,
    onSurface = warmDark.ink,
    surfaceVariant = warmDark.paperSurface,
    onSurfaceVariant = warmDark.ink2,
    surfaceTint = WarmPrimaryDark,
    inverseSurface = warmDark.ink,
    inverseOnSurface = warmDark.paper,
    inversePrimary = WarmPrimary,
    surfaceBright = Color(0xFF322A20),
    surfaceDim = warmDark.paper,
    surfaceContainerLowest = Color(0xFF110E0B),
    surfaceContainerLow = warmDark.paperSurface,
    surfaceContainer = warmDark.cardBg,
    surfaceContainerHigh = Color(0xFF2B241B),
    surfaceContainerHighest = Color(0xFF322A20),
    outline = warmDark.ink3,
    outlineVariant = Color(0xFF3A332A),
    error = WarmErrorDark,
    onError = Color(0xFF2A0E08),
    errorContainer = WarmErrorContainerDark,
    onErrorContainer = WarmErrorContainer,
    scrim = Color(0x99000000)
)

@Composable
fun SITTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SITDarkColorScheme else SITLightColorScheme

    // Make the in-app dark/light decision available to WarmTheme.palette()
    // so the warm chrome respects the user's theme override (not just
    // the system setting).
    CompositionLocalProvider(LocalIsAppDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SITTypography,
            content = content,
        )
    }
}
