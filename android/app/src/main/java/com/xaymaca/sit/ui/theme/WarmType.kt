package com.xaymaca.sit.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.xaymaca.sit.R

// Heading-font selection for warm-redesign cards & screens.
// Mirrors `headingFontFor(locale, warmth)` in
// assets/design-system/project/warm-redesign/system.jsx (line 188).
//
// Latin: Bebas Neue (Subtle) / system serif (Mid, Strong — Newsreader
// not bundled on Android, falls back to the system serif).
// ar / hi / ja: bundled Noto SemiBold families.

val NotoNaskhArabic = FontFamily(
    Font(R.font.noto_naskh_arabic_semibold, FontWeight.SemiBold)
)
val NotoSerifDevanagari = FontFamily(
    Font(R.font.noto_serif_devanagari_semibold, FontWeight.SemiBold)
)
val NotoSerifJP = FontFamily(
    Font(R.font.noto_serif_jp_semibold, FontWeight.SemiBold)
)

object WarmHeadingFont {

    @Composable
    @ReadOnlyComposable
    fun family(warmth: Warmth = Warmth.Subtle): FontFamily {
        val lang = LocalConfiguration.current.locales[0].language
        return when (lang) {
            "ar" -> NotoNaskhArabic
            "hi" -> NotoSerifDevanagari
            "ja" -> NotoSerifJP
            else -> when (warmth) {
                Warmth.Subtle -> BebasNeue
                Warmth.Mid, Warmth.Strong -> FontFamily.Serif
            }
        }
    }

    @Composable
    @ReadOnlyComposable
    fun weight(warmth: Warmth = Warmth.Subtle): FontWeight {
        val lang = LocalConfiguration.current.locales[0].language
        return when (lang) {
            "ar", "hi", "ja" -> FontWeight.SemiBold
            else -> when (warmth) {
                Warmth.Subtle -> FontWeight.Normal
                Warmth.Mid, Warmth.Strong -> FontWeight.Medium
            }
        }
    }

    @Composable
    @ReadOnlyComposable
    fun letterSpacing(warmth: Warmth = Warmth.Subtle): TextUnit {
        val lang = LocalConfiguration.current.locales[0].language
        return when (lang) {
            "ar", "hi", "ja" -> 0.sp
            else -> when (warmth) {
                Warmth.Subtle -> 0.01.sp
                Warmth.Mid -> (-0.01).sp
                Warmth.Strong -> (-0.015).sp
            }
        }
    }

    @Composable
    @ReadOnlyComposable
    fun style(size: TextUnit, warmth: Warmth = Warmth.Subtle): TextStyle =
        TextStyle(
            fontFamily = family(warmth),
            fontWeight = weight(warmth),
            fontSize = size,
            letterSpacing = letterSpacing(warmth)
        )
}
