package com.frameout.app.billing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

private fun roll(): ContentTransform =
    (slideInVertically(tween(220)) { it / 2 } + fadeIn(tween(220))) togetherWith
        (slideOutVertically(tween(160)) { -it / 2 } + fadeOut(tween(120)))

internal fun ctaLabel(offer: PlanOffer?): String = when {
    offer == null -> ProStrings.loading()
    offer.plan == ProPlan.Lifetime -> ProStrings.ctaLifetime(offer.price)
    offer.freeTrial && offer.trialDays > 0 -> ProStrings.ctaTrial(offer.trialDays)
    offer.plan == ProPlan.Yearly -> ProStrings.ctaYear(offer.price)
    else -> ProStrings.ctaMonth(offer.price)
}

internal fun fineText(offer: PlanOffer?): String = when {
    offer == null -> ""
    offer.plan == ProPlan.Lifetime -> ProStrings.fineLifetime(offer.price)
    offer.freeTrial -> ProStrings.fineTrial(offer.price)
    offer.plan == ProPlan.Yearly -> ProStrings.fineYear(offer.price)
    else -> ProStrings.fineMonth(offer.price)
}

@Composable
fun PaywallCta(offer: PlanOffer?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shimmer = rememberShimmer(3200, 1200)
    val ink = proOnBrush()
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(FrameShapes.pill)
            .background(proBrush())
            .shimmerSweep(shimmer, 0.55f)
            .frameClickable(enabled = offer != null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        Alignment.Center
    ) {
        AnimatedContent(ctaLabel(offer), transitionSpec = { roll() }, label = "ctaLabel") { label ->
            FrameText(label, FrameTheme.typography.title.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), ink, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
fun PaywallFooter(offer: PlanOffer?, manager: BillingManager, onDismiss: () -> Unit) {
    val note by manager.note.collectAsState()
    val text = noteText(note) ?: fineText(offer)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (text.isNotEmpty()) {
            FrameText(text, FrameTheme.typography.bodySmall, FrameTheme.colors.onSurfaceVariant, Modifier.padding(top = 10.dp, start = 22.dp, end = 22.dp), TextAlign.Center)
        }
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            FooterLink(ProStrings.restore(), FrameTheme.colors.primary) { manager.restore() }
            FooterLink(ProStrings.notNow(), FrameTheme.colors.onSurfaceVariant, onDismiss)
        }
    }
}

@Composable
private fun FooterLink(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    FrameText(label, FrameTheme.typography.label, color, Modifier.frameClickable(role = Role.Button, onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp))
}

private fun noteText(note: BillingNote): String? = when (note) {
    BillingNote.Error -> ProStrings.billingError()
    BillingNote.Unavailable -> ProStrings.unavailable()
    BillingNote.Pending -> ProStrings.pending()
    BillingNote.Restored -> ProStrings.restored()
    BillingNote.NothingToRestore -> ProStrings.nothingToRestore()
    BillingNote.None -> null
}
