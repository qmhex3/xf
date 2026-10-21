package com.frameout.app.billing

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

private val PlanOrder = listOf(ProPlan.Monthly, ProPlan.Yearly, ProPlan.Lifetime)
private val CardCorner = 18.dp
private const val FEATURED_WEIGHT = 1.14f

@Composable
fun PlanList(offers: List<PlanOffer>, selected: ProPlan?, onPick: (ProPlan) -> Unit, modifier: Modifier = Modifier) {
    val saving = BillingCatalog.savingsPercent(
        offers.firstOrNull { it.plan == ProPlan.Monthly }?.micros ?: 0L,
        offers.firstOrNull { it.plan == ProPlan.Yearly }?.micros ?: 0L
    )
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (offers.isEmpty()) repeat(3) { PlanSkeleton(Modifier.weight(1f)) }
        offers.sortedBy { PlanOrder.indexOf(it.plan) }.forEach { offer ->
            val weight = if (offer.plan == ProPlan.Yearly) FEATURED_WEIGHT else 1f
            PlanCard(offer, offer.plan == selected, saving, Modifier.weight(weight).fillMaxHeight()) { onPick(offer.plan) }
        }
    }
}

@Composable
private fun PlanCard(offer: PlanOffer, selected: Boolean, saving: Int, modifier: Modifier, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val reduce = LocalSystemA11y.current.reduceMotion
    val springSpec: AnimationSpec<Float> = if (reduce) snap() else spring(0.55f, Spring.StiffnessMedium)
    val scale by animateFloatAsState(if (selected) 1.04f else 1f, springSpec, label = "planScale")
    val sweep = rememberSweep(selected && !reduce)
    val palette = listOf(colors.accentA, colors.accentC, colors.accentB)
    Box(modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
        Column(
            Modifier
                .fillMaxSize()
                .background(if (selected) colors.primary.copy(alpha = 0.12f) else colors.onSurface.copy(alpha = 0.05f), FrameShapes.xl)
                .border(1.dp, if (selected) Color.Transparent else frameHairline(), FrameShapes.xl)
                .sweepBorder(selected, sweep, palette)
                .clip(FrameShapes.xl)
                .frameSelectable(selected, Role.RadioButton, onClick)
                .padding(start = 6.dp, end = 6.dp, top = 15.dp, bottom = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PlanNames(offer, Modifier.weight(1f))
            PlanPrice(offer)
        }
        if (offer.plan == ProPlan.Yearly) Ribbon(saving, Modifier.align(Alignment.TopCenter).offset(y = (-9).dp))
    }
}

@Composable
private fun PlanNames(offer: PlanOffer, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FrameText(planName(offer.plan), FrameTheme.typography.body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), textAlign = TextAlign.Center)
        FrameText(
            planHint(offer),
            FrameTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 13.sp),
            FrameTheme.colors.onSurfaceVariant,
            Modifier.padding(top = 2.dp),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun PlanPrice(offer: PlanOffer) {
    val per = when (offer.plan) {
        ProPlan.Monthly -> ProStrings.perMonth()
        ProPlan.Yearly -> ProStrings.perYear()
        ProPlan.Lifetime -> ProStrings.perOnce()
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FrameText(offer.price, FrameTheme.typography.mono.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium), textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
        FrameText(per, FrameTheme.typography.bodySmall.copy(fontSize = 10.5.sp), FrameTheme.colors.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

private fun planName(plan: ProPlan) = when (plan) {
    ProPlan.Monthly -> ProStrings.monthly()
    ProPlan.Yearly -> ProStrings.yearly()
    ProPlan.Lifetime -> ProStrings.lifetime()
}

private fun planHint(offer: PlanOffer): String = when {
    offer.freeTrial && offer.trialDays > 0 -> ProStrings.planTrialHint(offer.trialDays)
    offer.plan == ProPlan.Lifetime -> ProStrings.oneTime()
    offer.plan == ProPlan.Yearly -> ProStrings.planYearlyHint()
    else -> ProStrings.planMonthlyHint()
}

@Composable
private fun Ribbon(saving: Int, modifier: Modifier) {
    val text = if (saving > 0) ProStrings.bestSave(saving) else ProStrings.bestValue()
    FrameText(
        text,
        FrameTheme.typography.label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
        proOnBrush(),
        modifier.background(proBrush(), FrameShapes.xs).padding(horizontal = 9.dp, vertical = 3.dp),
        maxLines = 1
    )
}

@Composable
private fun rememberSweep(active: Boolean): State<Float> {
    if (!active) return remember { mutableStateOf(0f) }
    return rememberInfiniteTransition(label = "sweep").animateFloat(0f, 1f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "sweepValue")
}

private fun Modifier.sweepBorder(visible: Boolean, phase: State<Float>, palette: List<Color>): Modifier = drawBehind {
    if (!visible) return@drawBehind
    val brush = Brush.sweepGradient(cycleColors(palette, phase.value), center)
    drawRoundRect(brush, cornerRadius = CornerRadius(CardCorner.toPx()), style = Stroke(1.6.dp.toPx()))
}

@Composable
private fun PlanSkeleton(modifier: Modifier) {
    val shimmer = rememberShimmer(1800)
    Box(
        modifier
            .height(112.dp)
            .clip(FrameShapes.xl)
            .background(FrameTheme.colors.onSurface.copy(alpha = 0.06f))
            .shimmerSweep(shimmer, 0.18f)
    )
}
