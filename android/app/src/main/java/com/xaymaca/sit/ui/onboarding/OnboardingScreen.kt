package com.xaymaca.sit.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xaymaca.sit.R
import com.xaymaca.sit.ui.theme.Amber
import com.xaymaca.sit.ui.theme.BebasNeue
import com.xaymaca.sit.ui.theme.Navy
import com.xaymaca.sit.ui.theme.WarmCategory
import com.xaymaca.sit.ui.theme.WarmHeadingFont
import com.xaymaca.sit.ui.theme.WarmSpacing
import com.xaymaca.sit.ui.theme.WarmTheme
import com.xaymaca.sit.ui.theme.Warmth
import com.xaymaca.sit.ui.warm.WarmCard
import com.xaymaca.sit.ui.warm.WarmCardVariant

@Composable
fun OnboardingScreen(
    onImportContacts: () -> Unit,
    onAddContact: () -> Unit,
) {
    val warmth = Warmth.Subtle
    val palette = WarmTheme.palette(warmth)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.paper),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            BrandRow(palette = palette)

            TitleBlock(palette = palette, warmth = warmth)

            WarmCard(
                category = WarmCategory.Family,
                variant = WarmCardVariant.Hero,
                warmth = warmth,
                showPrompt = false,
                modifier = Modifier.padding(horizontal = WarmSpacing.Lg),
            )

            CtaStack(
                palette = palette,
                onImportContacts = onImportContacts,
                onAddContact = onAddContact,
            )

            PrivacyFooter(palette = palette)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BrandRow(palette: com.xaymaca.sit.ui.theme.WarmPalette) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = WarmSpacing.Lg),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Navy),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = "Ticklr",
            style = TextStyle(
                fontFamily = BebasNeue,
                fontSize = 22.sp,
                letterSpacing = (-0.4).sp,
                color = palette.ink,
            ),
        )
    }
}

@Composable
private fun TitleBlock(palette: com.xaymaca.sit.ui.theme.WarmPalette, warmth: Warmth) {
    Column(
        modifier = Modifier.padding(horizontal = WarmSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.warm_onboarding_title),
            style = WarmHeadingFont.style(44.sp, warmth)
                .copy(color = WarmCategory.Family.palette.accent),
        )
        Text(
            text = stringResource(R.string.warm_onboarding_subtitle),
            style = TextStyle(fontSize = 16.sp, color = palette.ink2, lineHeight = 22.sp),
        )
    }
}

@Composable
private fun CtaStack(
    palette: com.xaymaca.sit.ui.theme.WarmPalette,
    onImportContacts: () -> Unit,
    onAddContact: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = WarmSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onImportContacts,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Navy,
                contentColor = Color(0xFFFAF4E2),
            ),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                text = stringResource(R.string.warm_onboarding_cta_import),
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            )
        }
        TextButton(
            onClick = onAddContact,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.warm_onboarding_cta_add),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.ink2,
                ),
            )
        }
    }
}

@Composable
private fun PrivacyFooter(palette: com.xaymaca.sit.ui.theme.WarmPalette) {
    Text(
        text = stringResource(R.string.warm_onboarding_footer),
        style = TextStyle(fontSize = 11.5.sp, color = palette.ink3, lineHeight = 15.sp),
        modifier = Modifier.padding(horizontal = WarmSpacing.Lg),
    )
}
