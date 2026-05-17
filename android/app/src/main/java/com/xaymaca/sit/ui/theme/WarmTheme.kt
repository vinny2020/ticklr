package com.xaymaca.sit.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Warm-redesign palette ported from
// assets/design-system/project/warm-redesign/system.jsx (WARMTH constant).
// Spec defines light only; dark variants are derived. Mirrors the iOS
// WarmTheme.swift in feat/warm-redesign-ios.

enum class Warmth { Subtle, Mid, Strong }

data class WarmPalette(
    val paper: Color,
    val paperSurface: Color,
    val paperSurfaceAlt: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val cardShadow: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
)

object WarmTheme {

    @Composable
    @ReadOnlyComposable
    fun palette(warmth: Warmth = Warmth.Subtle): WarmPalette =
        if (LocalIsAppDark.current) darkPalette(warmth) else lightPalette(warmth)

    fun lightPalette(warmth: Warmth): WarmPalette = when (warmth) {
        Warmth.Subtle -> WarmPalette(
            paper           = Color(0xFFFFFFFF),
            paperSurface    = Color(0xFFF2EFE8),
            paperSurfaceAlt = Color(0xFFF8F4EB),
            cardBg          = Color(0xFFFBF7EE),
            cardBorder      = Color(0x0F3C2A14),    // 6% warm brown
            cardShadow      = Color(0x0A000000),
            ink             = Color(0xFF1A1F2A),
            ink2            = Color(0xFF5C6470),
            ink3            = Color(0xFF9099A4),
        )
        Warmth.Mid -> WarmPalette(
            paper           = Color(0xFFF4EFE3),
            paperSurface    = Color(0xFFEFE9DB),
            paperSurfaceAlt = Color(0xFFE8E1CF),
            cardBg          = Color(0xFFFAF5E8),
            cardBorder      = Color(0x143C2A14),    // 8%
            cardShadow      = Color(0x0D000000),
            ink             = Color(0xFF28241D),
            ink2            = Color(0xFF6B5F4F),
            ink3            = Color(0xFF9A8E7C),
        )
        Warmth.Strong -> WarmPalette(
            paper           = Color(0xFFEFE9DA),
            paperSurface    = Color(0xFFE7DFCC),
            paperSurfaceAlt = Color(0xFFDDD3BC),
            cardBg          = Color(0xFFFAF4E2),
            cardBorder      = Color(0x1A3C2A14),    // 10%
            cardShadow      = Color(0x0F000000),
            ink             = Color(0xFF211C13),
            ink2            = Color(0xFF6C5E48),
            ink3            = Color(0xFF998B72),
        )
    }

    fun darkPalette(warmth: Warmth): WarmPalette = when (warmth) {
        Warmth.Subtle -> WarmPalette(
            paper           = Color(0xFF161310),
            paperSurface    = Color(0xFF1F1B16),
            paperSurfaceAlt = Color(0xFF251F18),
            cardBg          = Color(0xFF241E16),
            cardBorder      = Color(0x14F5E6C8),    // 8% warm cream
            cardShadow      = Color(0x0A000000),
            ink             = Color(0xFFF4EFE3),
            ink2            = Color(0xFFB8A98E),
            ink3            = Color(0xFF847865),
        )
        Warmth.Mid -> WarmPalette(
            paper           = Color(0xFF13110D),
            paperSurface    = Color(0xFF1A1712),
            paperSurfaceAlt = Color(0xFF211C16),
            cardBg          = Color(0xFF26201A),
            cardBorder      = Color(0x1AF5E6C8),    // 10%
            cardShadow      = Color(0x0D000000),
            ink             = Color(0xFFF4EFE3),
            ink2            = Color(0xFFB5A78E),
            ink3            = Color(0xFF7E725F),
        )
        Warmth.Strong -> WarmPalette(
            paper           = Color(0xFF100E09),
            paperSurface    = Color(0xFF171410),
            paperSurfaceAlt = Color(0xFF1E1A14),
            cardBg          = Color(0xFF241E16),
            cardBorder      = Color(0x1FF5E6C8),    // 12%
            cardShadow      = Color(0x0F000000),
            ink             = Color(0xFFF4EFE3),
            ink2            = Color(0xFFB0A285),
            ink3            = Color(0xFF796C56),
        )
    }
}

val LocalWarmth = compositionLocalOf { Warmth.Subtle }
val LocalWarmPalette = compositionLocalOf { WarmTheme.lightPalette(Warmth.Subtle) }
/** SITTheme provides this so WarmTheme.palette() respects the user's
 *  in-app theme override, not just the system setting. */
val LocalIsAppDark = compositionLocalOf { false }

object WarmRadius {
    val CardHero: Dp = 22.dp
    val Card: Dp = 20.dp
    val CardCompact: Dp = 18.dp
    val Surface: Dp = 16.dp
    val Badge: Dp = 14.dp
    val Chip: Dp = 8.dp   // M3 filter chip per HANDOFF lines 60-62
    val Photo: Dp = 36.dp
}

object WarmSpacing {
    val Xs: Dp = 4.dp
    val Sm: Dp = 8.dp
    val Md: Dp = 12.dp
    val Lg: Dp = 16.dp
    val Xl: Dp = 22.dp
    val Xxl: Dp = 32.dp
}
