package com.frameout.app.billing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProShowcase
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameAccent
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import kotlinx.coroutines.launch

private val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
private const val REVEAL_MS = 1500f

@Stable
class SheetMotion(val enter: Animatable<Float, AnimationVector1D>, val reveal: Animatable<Float, AnimationVector1D>, val close: () -> Unit)

@Composable
fun rememberSheetMotion(onDismiss: () -> Unit): SheetMotion {
    val reduce = LocalSystemA11y.current.reduceMotion
    val enter = remember { Animatable(if (reduce) 1f else 0f) }
    val reveal = remember { Animatable(if (reduce) 1f else 0f) }
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        launch { enter.animateTo(1f, spring(0.82f, Spring.StiffnessLow)) }
        reveal.animateTo(1f, tween(REVEAL_MS.toInt(), easing = LinearEasing))
    }
    return remember { SheetMotion(enter, reveal) { scope.launch { if (!reduce) enter.animateTo(0f, tween(220)); dismiss() } } }
}

fun Modifier.revealed(reveal: Animatable<Float, AnimationVector1D>, index: Int): Modifier = graphicsLayer {
    val local = ((reveal.value * REVEAL_MS - (260f + 70f * index)) / 600f).coerceIn(0f, 1f)
    alpha = local
    translationY = (1f - local) * 16.dp.toPx()
}

@Composable
fun PaywallSheet(
    feature: ProFeature,
    trialsLeft: Int?,
    manager: BillingManager,
    onLaunch: (PlanOffer) -> Unit,
    onDismiss: () -> Unit
) {
    val offers by manager.offers.collectAsState()
    var chosen by remember { mutableStateOf(ProPlan.Yearly) }
    val selected = offers.firstOrNull { it.plan == chosen } ?: offers.firstOrNull()
    var picked by remember { mutableStateOf<FrameAccent?>(null) }
    val motion = rememberSheetMotion(onDismiss)
    Dialog(onDismissRequest = motion.close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        RetintedTheme(picked) {
            SheetFrame(motion) {
                PaywallBody(feature, trialsLeft, offers, selected, motion, Modifier.weight(1f), { picked = it }) { chosen = it }
                Column(Modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp)) {
                    PaywallCta(selected, { selected?.let(onLaunch) })
                    PaywallFooter(selected, manager, motion.close)
                }
            }
        }
    }
}

@Composable
private fun SheetFrame(motion: SheetMotion, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val colors = FrameTheme.colors
    val glow = if (colors.isDark) 0.34f else 0.18f
    Box(
        Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null, onClick = motion.close),
        Alignment.BottomCenter
    ) {
        Box(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.93f)
                .graphicsLayer { translationY = (1f - motion.enter.value) * size.height }
                .clip(SheetShape)
                .background(colors.surface)
                .drawBehind {
                    val stops = listOf(colors.accentA.copy(alpha = glow), Color.Transparent)
                    drawRect(Brush.radialGradient(stops, Offset(size.width / 2f, 0f), size.width * 0.8f))
                }
                .border(1.dp, frameHairline(), SheetShape)
                .clickable(remember { MutableInteractionSource() }, null) { }
        ) {
            Column(Modifier.fillMaxSize(), content = content)
            SheetChrome(motion.close)
        }
    }
}

@Composable
private fun BoxScope.SheetChrome(onClose: () -> Unit) {
    val colors = FrameTheme.colors
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .padding(top = 8.dp)
            .size(width = 38.dp, height = 4.dp)
            .background(colors.primary.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
    )
    Box(
        Modifier
            .align(Alignment.TopEnd)
            .padding(14.dp)
            .size(32.dp)
            .background(colors.onSurface.copy(alpha = 0.1f), RoundedCornerShape(50))
            .frameClickable(onClickLabel = ProStrings.notNow(), onClick = onClose),
        Alignment.Center
    ) { Image(painterResource(FrameIcons.Close), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(colors.onSurface)) }
}

@Composable
private fun PaywallBody(
    feature: ProFeature,
    trialsLeft: Int?,
    offers: List<PlanOffer>,
    selected: PlanOffer?,
    motion: SheetMotion,
    modifier: Modifier,
    onAccent: (FrameAccent) -> Unit,
    onPick: (ProPlan) -> Unit
) {
    val reveal = motion.reveal
    val kind = ProShowcase.kindOf(feature)
    Column(modifier.verticalScroll(rememberScrollState()).padding(bottom = 6.dp)) {
        if (kind == null) PaywallHero() else PaywallShowcase(kind, FrameTheme.accent, onAccent)
        PaywallHeader(feature, trialsLeft, Modifier.revealed(reveal, 0))
        if (kind == null) ProFeatureList(motion, null, Modifier.padding(top = 16.dp)) else IncludedWithThis(kind, motion, Modifier.padding(top = 12.dp))
        PlanList(offers, selected?.plan, onPick, Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp).revealed(reveal, 3))
        TrialTimeline(selected?.freeTrial == true, selected?.trialDays ?: 0, Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).revealed(reveal, 4))
        if (kind != null) ProFeatureList(motion, feature, Modifier.padding(top = 22.dp, bottom = 8.dp))
    }
}
