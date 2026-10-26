package com.frameout.app.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.screenHeading
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.motion.LocalPageActive
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.motion.staggerIn
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun PageBodyWide(page: OnboardingPage, side: Dp, offset: () -> Float, dir: Float, onAllowNotifications: () -> Unit) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f).stageParallax(offset, dir), contentAlignment = Alignment.Center) { PageStage(page, side) }
        PageCopy(page, onAllowNotifications, Modifier.weight(1f).copyParallax(offset, dir).verticalScroll(rememberScrollState()))
    }
}

@Composable
internal fun PageBodyNarrow(page: OnboardingPage, side: Dp, maxHeight: Dp, offset: () -> Float, dir: Float, onAllowNotifications: () -> Unit) {
    val scroll = rememberScrollState()
    val hintAlpha by animateFloatAsState(if (scroll.canScrollForward) 1f else 0f, frameSpring(), label = "scrollHint")
    val fade = FrameTheme.colors.background
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .heightIn(min = maxHeight)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.stageParallax(offset, dir)) { PageStage(page, side) }
            Spacer(Modifier.height(24.dp))
            PageCopy(page, onAllowNotifications, Modifier.fillMaxWidth().copyParallax(offset, dir))
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(40.dp)
                .graphicsLayer { alpha = hintAlpha }
                .background(Brush.verticalGradient(listOf(Color.Transparent, fade)))
        )
    }
}

internal fun pageHeight(page: OnboardingPage): Pair<Float, Dp> = when (page) {
    OnboardingPage.Hero -> 1f to HeroExtraHeight
    OnboardingPage.Formats -> FormatsHeightFactor to FormatsExtraHeight
    OnboardingPage.Pick -> PickHeightFactor to PickExtraHeight
    OnboardingPage.Ready -> ReadyHeightFactor to ReadyExtraHeight
}

internal fun pageReserve(page: OnboardingPage): Dp = CopyReserve + when (page) {
    OnboardingPage.Ready -> 190.dp
    OnboardingPage.Formats, OnboardingPage.Pick -> 56.dp
    else -> 0.dp
}

@Composable
private fun PageStage(page: OnboardingPage, side: Dp) {
    when (page) {
        OnboardingPage.Hero -> HeroStage(side)
        OnboardingPage.Formats -> FormatsStage(side)
        OnboardingPage.Pick -> PickStage(side)
        OnboardingPage.Ready -> ReadyHero(side)
    }
}

@Composable
private fun PageCopy(page: OnboardingPage, onAllowNotifications: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val headline = FrameTheme.typography.display.copy(fontSize = 26.sp, lineHeight = 34.sp, color = colors.onSurface)
    val body = FrameTheme.typography.body.copy(fontSize = 16.sp, lineHeight = 24.sp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AccentCopy(page, headline, body, onAllowNotifications)
    }
}

@Composable
private fun AccentCopy(page: OnboardingPage, headline: TextStyle, body: TextStyle, onAllowNotifications: () -> Unit) {
    val colors = FrameTheme.colors
    val active = LocalPageActive.current
    val (first, second, sub) = when (page) {
        OnboardingPage.Hero -> Triple(FrameStrings.onboardingHeroA(), FrameStrings.onboardingHeroB(), FrameStrings.onboardingHeroSub())
        OnboardingPage.Formats -> Triple(FrameStrings.onboardingFormatsA(), FrameStrings.onboardingFormatsB(), FrameStrings.onboardingFormatsSub())
        OnboardingPage.Pick -> Triple(FrameStrings.onboardingPickA(), FrameStrings.onboardingPickB(), FrameStrings.onboardingPickSub())
        else -> Triple(FrameStrings.onboardingReadyA(), FrameStrings.onboardingReadyB(), FrameStrings.onboardingReadySub())
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val accent = if (rtl) {
        Brush.horizontalGradient(listOf(colors.success, colors.success, colors.primary))
    } else {
        Brush.horizontalGradient(listOf(colors.primary, colors.success, colors.success))
    }
    BasicText(
        text = buildAnnotatedString {
            append(first)
            append(if (page == OnboardingPage.Hero) "\n" else " ")
            withStyle(SpanStyle(brush = accent)) { append(second) }
        },
        style = headline,
        modifier = Modifier.screenHeading().staggerIn(0, active)
    )
    FrameText(sub, style = body, color = colors.onSurfaceVariant, modifier = Modifier.staggerIn(1, active))
    if (page == OnboardingPage.Formats) ModeChips(Modifier.padding(top = 8.dp).staggerIn(2, active))
    if (page == OnboardingPage.Pick) PickChips(Modifier.padding(top = 8.dp).staggerIn(2, active))
    if (page == OnboardingPage.Ready) PermissionPanel(rememberReadyPermissions(onAllowNotifications), Modifier.padding(top = 8.dp).staggerIn(2, active))
}
