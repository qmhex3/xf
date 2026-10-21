package com.frameout.app.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import com.frameout.app.billing.ProStatusBadge
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import com.frameout.designsystem.haptics.FrameHaptics
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.R
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.motion.frameFadeIn
import com.frameout.designsystem.motion.frameFadeOut
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun OnboardingTopBar(
    page: Int,
    total: Int,
    showSkip: Boolean,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FrameTheme.colors
    val skipAlpha by animateFloatAsState(if (showSkip) 1f else 0f, frameSpring(), label = "skipAlpha")
    Row(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f))
        ProgressSegments(page, total)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            FrameText(
                FrameStrings.onboardingSkip(),
                style = FrameTheme.typography.titleSmall.copy(fontSize = 14.sp),
                color = colors.onSurfaceVariant,
                modifier = Modifier
                    .graphicsLayer { this.alpha = skipAlpha }
                    .frameClickable(enabled = showSkip, onClick = onSkip)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun ProgressSegments(page: Int, total: Int) {
    val colors = FrameTheme.colors
    val description = FrameStrings.onboardingStep(page + 1, total)
    Row(
        Modifier.semantics(mergeDescendants = true) {
            stateDescription = description
            progressBarRangeInfo = ProgressBarRangeInfo((page + 1).toFloat(), 1f..total.toFloat(), (total - 2).coerceAtLeast(0))
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { i ->
            val active = i == page
            val segmentWidth by animateDpAsState(if (active) 28.dp else 8.dp, frameSpring(), label = "segment")
            val base = Modifier.width(segmentWidth).height(6.dp)
            Box(
                (if (active) base.frameGlow(colors.primary, { 0.4f }, 6.dp) else base)
                    .clip(FrameShapes.pill)
                    .then(
                        if (active) Modifier.background(FrameGradients.Progress)
                        else Modifier.background(if (i < page) colors.primary.copy(alpha = 0.5f) else colors.containerHigh)
                    )
            )
        }
    }
}

@Composable
internal fun OnboardingBrand(modifier: Modifier = Modifier, showBadge: Boolean = false) {
    val colors = FrameTheme.colors
    val badgeAlpha by animateFloatAsState(if (showBadge) 1f else 0f, label = "brandBadge")
    val style = FrameTheme.typography.title.copy(fontWeight = FontWeight.SemiBold)
    val label = FrameStrings.onboardingTitle()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier.semantics(mergeDescendants = true) { contentDescription = label },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(R.drawable.ic_brand_mark), null, Modifier.height(24.dp))
            Row {
                FrameText("Frame", style = style)
                FrameText(" out", style = style, color = colors.primary)
            }
            ProStatusBadge(Modifier.graphicsLayer { alpha = badgeAlpha }, interactive = false)
        }
    }
}

@Composable
internal fun OnboardingCta(label: String, last: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val haptics = rememberFrameHaptics()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.975f else 1f, frameSpring(), label = "ctaPress")
    Box(
        modifier
            .fillMaxWidth()
            .scale(press)
            .ctaButton(colors.accentA, colors.accentB, interaction, haptics, last, onClick),
        contentAlignment = Alignment.Center
    ) { CtaContent(label, last) }
}

private fun Modifier.ctaButton(
    from: Color,
    to: Color,
    interaction: MutableInteractionSource,
    haptics: FrameHaptics,
    last: Boolean,
    onClick: () -> Unit
): Modifier {
    val shape = FrameShapes.pill
    val fill = Brush.linearGradient(listOf(lerp(from, Color.Black, 0.26f), lerp(to, Color.Black, 0.36f)))
    val sheen = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent, Color.Black.copy(alpha = 0.14f)))
    val rim = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.06f)))
    return this
        .height(58.dp)
        .frameGlow(from, { 0.32f }, 18.dp, { 0.5f })
        .clip(shape)
        .background(fill)
        .background(sheen)
        .border(1.dp, rim, shape)
        .clickable(
            interactionSource = interaction,
            indication = null,
            role = Role.Button,
            onClick = {
                haptics.perform(if (last) FrameHaptic.Confirm else FrameHaptic.Tap)
                onClick()
            }
        )
}

@Composable
private fun BoxScope.CtaContent(label: String, last: Boolean) {
    val fadeIn = frameFadeIn()
    val fadeOut = frameFadeOut()
    AnimatedContent(
        targetState = label,
        modifier = Modifier.padding(horizontal = 56.dp),
        transitionSpec = { fadeIn togetherWith fadeOut },
        label = "ctaLabel"
    ) { text ->
        FrameText(
            text,
            style = FrameTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            color = Color.White,
            maxLines = 1
        )
    }
    Box(
        Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(42.dp).clip(FrameShapes.pill).background(Color.White.copy(alpha = 0.20f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(if (last) FrameIcons.VideoAdd else FrameIconsExtra.Chevron),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(Color.White)
        )
    }
}
