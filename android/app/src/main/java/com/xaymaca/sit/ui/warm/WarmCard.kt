package com.xaymaca.sit.ui.warm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xaymaca.sit.ui.theme.WarmCategory
import com.xaymaca.sit.ui.theme.WarmHeadingFont
import com.xaymaca.sit.ui.theme.WarmRadius
import com.xaymaca.sit.ui.theme.WarmSpacing
import com.xaymaca.sit.ui.theme.Warmth
import com.xaymaca.sit.ui.theme.WarmTheme

enum class WarmCardVariant { Hero, Compact, Row }

/**
 * Warm category card. Three variants matching iOS WarmCard:
 * Hero (16:9 illustration on top), Compact (88dp illustration left),
 * Row (100dp illustration left, used on the Groups list — slimmed in
 * the cross-platform "balance row heights" fix).
 */
@Composable
fun WarmCard(
    category: WarmCategory,
    modifier: Modifier = Modifier,
    variant: WarmCardVariant = WarmCardVariant.Hero,
    warmth: Warmth = Warmth.Subtle,
    showPrompt: Boolean = true,
    contactsCount: Int? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    when (variant) {
        WarmCardVariant.Hero -> HeroCard(category, modifier.then(clickModifier), warmth, showPrompt, contactsCount)
        WarmCardVariant.Compact -> CompactCard(category, modifier.then(clickModifier), warmth, contactsCount)
        WarmCardVariant.Row -> RowCard(category, modifier.then(clickModifier), warmth, showPrompt, contactsCount)
    }
}

@Composable
private fun HeroCard(
    category: WarmCategory,
    modifier: Modifier,
    warmth: Warmth,
    showPrompt: Boolean,
    contactsCount: Int?,
) {
    val palette = WarmTheme.palette(warmth)
    val shape = RoundedCornerShape(WarmRadius.CardHero)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.cardBg)
            .border(1.dp, palette.cardBorder, shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        ) {
            WarmIllustration(category = category, modifier = Modifier.fillMaxWidth())
        }
        Column(
            modifier = Modifier.padding(WarmSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryBadge(category = category, size = 44.dp)
            HeadlineBlock(category = category, sizeSp = 28.sp, warmth = warmth)
            Text(
                text = stringResource(category.bodyRes),
                style = TextStyle(fontSize = 14.sp, color = palette.ink2),
                maxLines = 3,
            )
            if (contactsCount != null) CountLine(contactsCount, palette.ink2)
            if (showPrompt) TicklePrompt(category = category, warmth = warmth)
        }
    }
}

@Composable
private fun CompactCard(
    category: WarmCategory,
    modifier: Modifier,
    warmth: Warmth,
    contactsCount: Int?,
) {
    val palette = WarmTheme.palette(warmth)
    val shape = RoundedCornerShape(WarmRadius.CardCompact)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.cardBg)
            .border(1.dp, palette.cardBorder, shape)
            .padding(WarmSpacing.Md),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(14.dp)),
        ) {
            WarmIllustration(category = category, modifier = Modifier.fillMaxWidth())
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            HeadlineBlock(category = category, sizeSp = 20.sp, warmth = warmth)
            if (contactsCount != null) {
                CountLine(contactsCount, palette.ink2)
            } else {
                Text(
                    text = stringResource(category.bodyRes),
                    style = TextStyle(fontSize = 13.sp, color = palette.ink2),
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun RowCard(
    category: WarmCategory,
    modifier: Modifier,
    warmth: Warmth,
    showPrompt: Boolean,
    contactsCount: Int?,
) {
    val palette = WarmTheme.palette(warmth)
    val shape = RoundedCornerShape(WarmRadius.Card)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.cardBg)
            .border(1.dp, palette.cardBorder, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Illustration on the leading edge — 100dp matching the iOS
        // "balance row heights" tweak so canonical and user groups read
        // with equal visual weight.
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = WarmRadius.Card,
                        bottomStart = WarmRadius.Card,
                        topEnd = 0.dp,
                        bottomEnd = 0.dp,
                    ),
                ),
        ) {
            WarmIllustration(category = category, modifier = Modifier.fillMaxWidth())
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = WarmSpacing.Lg, top = WarmSpacing.Md, bottom = WarmSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            HeadlineBlock(category = category, sizeSp = 20.sp, warmth = warmth)
            if (contactsCount != null) CountLine(contactsCount, palette.ink2)
            if (showPrompt) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(category.promptShortRes) + "  →",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = category.palette.accent,
                    ),
                )
            }
        }
    }
}

@Composable
private fun HeadlineBlock(category: WarmCategory, sizeSp: androidx.compose.ui.unit.TextUnit, warmth: Warmth) {
    val style = WarmHeadingFont.style(sizeSp, warmth)
    Column {
        Text(
            text = stringResource(category.headlineLine1Res),
            style = style.copy(color = category.palette.accent),
        )
        Text(
            text = stringResource(category.headlineLine2Res),
            style = style.copy(color = category.palette.accent),
        )
    }
}

@Composable
private fun CountLine(count: Int, color: androidx.compose.ui.graphics.Color) {
    // Plural-aware string lands in a later commit; for v1 use a literal.
    Text(
        text = "$count contacts",
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color),
    )
}
