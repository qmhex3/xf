package com.frameout.app.rating

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.frameout.app.PreferredFrameTheme
import com.frameout.core.rating.RatingPolicy
import com.frameout.core.util.RatingStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameAmbientGlow
import com.frameout.designsystem.theme.frameCardBrush
import com.frameout.designsystem.theme.frameCardEdge

private val CardShape = RoundedCornerShape(28.dp)

@Composable
fun RatingDialog(
    lifetimeSuccesses: Int,
    onRate: (stars: Int) -> Unit,
    onLater: () -> Unit,
    onNever: () -> Unit
) {
    Dialog(onDismissRequest = onLater, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        PreferredFrameTheme {
            RatingCard(lifetimeSuccesses, onRate, onLater, onNever)
        }
    }
}

@Composable
private fun cardModifier(progress: () -> Float): Modifier = Modifier
    .padding(horizontal = 24.dp)
    .widthIn(max = 400.dp)
    .fillMaxWidth()
    .graphicsLayer {
        val p = progress()
        scaleX = 0.88f + 0.12f * p
        scaleY = 0.88f + 0.12f * p
        alpha = p.coerceIn(0f, 1f)
    }
    .clip(CardShape)
    .background(frameCardBrush())
    .frameAmbientGlow()
    .border(1.dp, frameCardEdge(), CardShape)
    .padding(horizontal = 24.dp, vertical = 28.dp)

@Composable
private fun RatingCard(lifetimeSuccesses: Int, onRate: (Int) -> Unit, onLater: () -> Unit, onNever: () -> Unit) {
    val reduce = LocalSystemA11y.current.reduceMotion
    var rating by remember { mutableIntStateOf(0) }
    val haptics = rememberFrameHaptics()
    val enter = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduce) enter.animateTo(1f, spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow))
    }
    Column(cardModifier { enter.value }, horizontalAlignment = Alignment.CenterHorizontally) {
        Hero(rating, reduce)
        RatingHeader(lifetimeSuccesses)
        StarPicker(
            rating = rating,
            reduce = reduce,
            onChange = { value ->
                if (value != rating) {
                    haptics.perform(FrameHaptic.Select)
                    rating = value
                }
            },
            modifier = Modifier.padding(top = 20.dp)
        )
        RatingFeedback(rating)
        RatingButton(rating, onClick = { if (rating > 0) onRate(rating) }, modifier = Modifier.padding(top = 10.dp))
        RatingFooter(onLater, onNever)
    }
}

@Composable
private fun RatingHeader(lifetimeSuccesses: Int) {
    FrameText(
        RatingStrings.title(),
        style = FrameTheme.typography.title,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 18.dp)
    )
    FrameText(
        RatingStrings.body(),
        style = FrameTheme.typography.bodySmall,
        color = FrameTheme.colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp)
    )
    if (lifetimeSuccesses >= RatingPolicy.MIN_SUCCESSES) {
        SuccessChip(RatingStrings.stat(lifetimeSuccesses), Modifier.padding(top = 14.dp))
    }
}

@Composable
private fun RatingFeedback(rating: Int) {
    val colors = FrameTheme.colors
    AnimatedContent(
        targetState = rating,
        transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(100)) },
        modifier = Modifier.padding(top = 10.dp),
        label = "rating-label"
    ) { stars ->
        FrameText(
            RatingStrings.label(stars),
            style = FrameTheme.typography.titleSmall,
            color = if (stars == 0) colors.onSurfaceVariant else colors.onSurface,
            textAlign = TextAlign.Center
        )
    }
    Box(Modifier.heightIn(min = 40.dp).padding(top = 6.dp), Alignment.Center) {
        AnimatedContent(
            targetState = bucketOf(rating),
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(100)) },
            label = "rating-note"
        ) { bucket ->
            FrameText(
                noteFor(bucket),
                style = FrameTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun noteFor(bucket: Bucket): String = when (bucket) {
    Bucket.Positive -> RatingStrings.notePositive()
    Bucket.Negative -> RatingStrings.noteNegative()
    Bucket.None -> ""
}

@Composable
private fun RatingFooter(onLater: () -> Unit, onNever: () -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextAction(RatingStrings.later(), colors.onSurface, onLater)
        TextAction(RatingStrings.never(), colors.onSurfaceVariant, onNever)
    }
}
