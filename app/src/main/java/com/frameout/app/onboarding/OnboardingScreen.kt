package com.frameout.app.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.PreferredFrameTheme
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.motion.FlowEase
import com.frameout.designsystem.motion.LocalPageActive
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameAmbientGlow
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal enum class OnboardingPage { Hero, Formats, Pick, Ready }

internal val CopyReserve = 176.dp

@Composable
fun OnboardingScreen(onAllowNotifications: () -> Unit = {}, onStart: () -> Unit = {}, ready: Boolean = true, onDone: () -> Unit) {
    val pages = remember {
        listOf(
            OnboardingPage.Hero,
            OnboardingPage.Formats,
            OnboardingPage.Pick,
            OnboardingPage.Ready
        )
    }
    PreferredFrameTheme { OnboardingContent(pages, onAllowNotifications, onStart, ready, onDone) }
}

@Composable
private fun OnboardingContent(pages: List<OnboardingPage>, onAllowNotifications: () -> Unit, onStart: () -> Unit, ready: Boolean, onDone: () -> Unit) {
    val colors = FrameTheme.colors
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val current = pager.currentPage
    val last = current >= pages.lastIndex

    fun goTo(index: Int) {
        scope.launch { if (reduceMotion) pager.scrollToPage(index) else pager.animateScrollToPage(index) }
    }

    BackHandler(enabled = current > 0) { goTo(current - 1) }

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(ready) { if (ready) entered = true }
    val enter by animateFloatAsState(if (entered) 1f else 0f, frameSpring(), label = "enter")

    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val leave by animateFloatAsState(
        if (pending != null) 1f else 0f,
        if (reduceMotion) snap() else tween(300, easing = FlowEase.InOut),
        label = "leave"
    )
    LaunchedEffect(pending) {
        val action = pending ?: return@LaunchedEffect
        if (!reduceMotion) delay(280)
        action()
    }
    fun finish(action: () -> Unit) { if (pending == null) pending = action }

    Box(Modifier.fillMaxSize().background(colors.background).frameAmbientGlow()) {
        OnboardingColumn(pages, pager, enter, leave, ready, onAllowNotifications, { finish(onStart) }, { finish(onDone) }, ::goTo)
    }
}

@Composable
private fun OnboardingColumn(
    pages: List<OnboardingPage>,
    pager: PagerState,
    enter: Float,
    leave: Float,
    ready: Boolean,
    onAllowNotifications: () -> Unit,
    onStart: () -> Unit,
    onDone: () -> Unit,
    goTo: (Int) -> Unit
) {
    val current = pager.currentPage
    val last = current >= pages.lastIndex
    run {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .graphicsLayer {
                    alpha = enter * (1f - leave)
                    translationY = (1f - enter) * 24.dp.toPx()
                    val s = 1f + 0.06f * leave
                    scaleX = s
                    scaleY = s
                }
        ) {
            OnboardingTopBar(page = current, total = pages.size, showSkip = !last, onSkip = onDone)
            OnboardingBrand(Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp), showBadge = false)
            OnboardingPager(pager, pages, ready, onAllowNotifications, Modifier.weight(1f).fillMaxWidth())
            val label = when {
                last -> FrameStrings.onboardingReadyStart()
                else -> FrameStrings.onboardingNext()
            }
            Column(Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)) {
                OnboardingCta(label = label, last = last, onClick = { if (last) onStart() else goTo(current + 1) })
                if (last) {
                    FrameText(
                        FrameStrings.onboardingLater(),
                        style = FrameTheme.typography.titleSmall.copy(fontSize = 13.sp),
                        color = FrameTheme.colors.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally).frameClickable(onClick = onDone).padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PageBody(page: OnboardingPage, offset: () -> Float, onAllowNotifications: () -> Unit) {
    val dir = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val (factor, extra) = pageHeight(page)
        val wide = page == OnboardingPage.Formats || page == OnboardingPage.Pick || page == OnboardingPage.Ready
        val cap = if (wide) 340.dp else 320.dp
        if (maxWidth >= 560.dp && maxWidth > maxHeight) {
            val side = ((maxHeight - extra - 16.dp) / factor).coerceAtMost(cap).coerceAtLeast(120.dp)
            PageBodyWide(page, side, offset, dir, onAllowNotifications)
        } else {
            val floor = if (wide) 220.dp else 150.dp
            val side = (maxWidth - 48.dp).coerceAtMost(cap).coerceAtMost((maxHeight - pageReserve(page) - extra) / factor).coerceAtLeast(floor)
            PageBodyNarrow(page, side, maxHeight, offset, dir, onAllowNotifications)
        }
    }
}


@Composable
private fun OnboardingPager(pager: PagerState, pages: List<OnboardingPage>, ready: Boolean, onAllowNotifications: () -> Unit, modifier: Modifier) {
    HorizontalPager(state = pager, modifier = modifier, key = { pages[it].ordinal }) { index ->
        val offset = { (pager.currentPage - index) + pager.currentPageOffsetFraction }
        CompositionLocalProvider(LocalPageActive provides (ready && pager.currentPage == index)) {
            Box(Modifier.fillMaxSize()) { PageBody(pages[index], offset, onAllowNotifications) }
        }
    }
}

internal fun Modifier.stageParallax(offset: () -> Float, dir: Float): Modifier = graphicsLayer {
    val o = offset().coerceIn(-1f, 1f)
    val a = o.absoluteValue
    translationX = o * size.width * 0.40f * dir
    rotationY = -o * 22f * dir
    cameraDistance = 14f * density
    val s = 1f - 0.16f * a
    scaleX = s
    scaleY = s
    alpha = 1f - 0.85f * a
}

internal fun Modifier.copyParallax(offset: () -> Float, dir: Float): Modifier = graphicsLayer {
    val o = offset().coerceIn(-1f, 1f)
    val a = o.absoluteValue
    translationX = -o * size.width * 0.30f * dir
    alpha = (1f - 1.4f * a).coerceIn(0f, 1f)
}
