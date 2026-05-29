package com.xaymaca.sit.ui.theme

import androidx.compose.ui.graphics.Color

// SIT brand colors
val Navy = Color(0xFF0A1628)
val NavyLight = Color(0xFF1A2E4A)
val NavySurface = Color(0xFF162035)
val Cobalt = Color(0xFF2563EB)
val CobaltDark = Color(0xFF1D4ED8)
val CobaltLight = Color(0xFF3B82F6)
val Amber = Color(0xFFF5C842)
val AmberDark = Color(0xFFD4A017)
val AmberLight = Color(0xFFFDD74F)

// Text on dark background
val OnNavy = Color(0xFFE8EDF5)
val OnNavySecondary = Color(0xFF8FA3BF)
val OnNavyTertiary = Color(0xFF5C738A)

// Light theme colors
val White = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF5F7FA)
val LightOnSurface = Color(0xFF0A1628)
val LightOnSurfaceSecondary = Color(0xFF2D3748) // Darker gray for readability
val LightOnSurfaceTertiary = Color(0xFF4A5568)

// ── Warm-redesign Material accents (TIC-49) ───────────────────────────────
// The base MaterialTheme is re-derived from the warm palette so every Material
// widget (buttons, switches, app bars, nav bar, sheets, text fields, etc.)
// inherits warm colors instead of the old Navy/Cobalt/Amber scheme. Neutrals
// come from WarmTheme.lightPalette/darkPalette(Subtle); accents are the warm
// category palette (WarmCategory.kt), mirroring the iOS warm redesign.
//
// On-accent foreground — warm near-white, matching WarmFilterChip / TickleActionSheet.
val WarmOnAccent = Color(0xFFFAF4E2)

// primary = Community terracotta — the brand-neutral "house" accent for actions.
val WarmPrimary = Color(0xFFB26342)
val WarmPrimaryContainer = Color(0xFFF0D4C2)
val WarmPrimaryDark = Color(0xFFD98E64)          // lightened for contrast on dark paper
val WarmPrimaryContainerDark = Color(0xFF5A3422)

// secondary = Milestones mustard — the warm "tickle-due" amber, kept as an accent only.
val WarmSecondary = Color(0xFFA7791C)
val WarmSecondaryContainer = Color(0xFFEDDEB6)
val WarmSecondaryDark = Color(0xFFE6D1A0)
val WarmSecondaryContainerDark = Color(0xFF4A3A1A)

// tertiary = Work forest.
val WarmTertiary = Color(0xFF4F6B47)
val WarmTertiaryContainer = Color(0xFFDCE4D2)
val WarmTertiaryDark = Color(0xFFC7D3BD)
val WarmTertiaryContainerDark = Color(0xFF2E3B28)

// error = the warm red-orange already used for the Tickle delete swipe.
val WarmError = Color(0xFFB2422C)
val WarmErrorContainer = Color(0xFFF4D9CF)
val WarmErrorDark = Color(0xFFE0795C)
val WarmErrorContainerDark = Color(0xFF5A271A)
