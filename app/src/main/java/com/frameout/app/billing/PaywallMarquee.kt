package com.frameout.app.billing

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.a11y.LocalSystemA11y

private val Marquee = ProFeature.entries.filter { it != ProFeature.General }

@Composable
fun FeatureMarquee(modifier: Modifier = Modifier) {
    val names = Marquee.map { ProStrings.featureName(it) }
    val half = (names.size + 1) / 2
    Column(modifier.fillMaxWidth().fadedEdges(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MarqueeRow(names.take(half), reverse = false, periodMs = 38_000)
        MarqueeRow(names.drop(half), reverse = true, periodMs = 46_000)
    }
}

@Composable
private fun MarqueeRow(items: List<String>, reverse: Boolean, periodMs: Int) {
    val reduce = LocalSystemA11y.current.reduceMotion
    var setWidth by remember { mutableFloatStateOf(0f) }
    val phase = rememberInfiniteTransition(label = "marquee").animateFloat(
        0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing)), label = "marqueeValue"
    )
    Row(
        Modifier
            .clipToBounds()
            .wrapContentWidth(Alignment.Start, unbounded = true)
            .graphicsLayer { translationX = if (reduce) 0f else -setWidth * (if (reverse) 1f - phase.value else phase.value) }
    ) {
        repeat(4) { index ->
            Row(
                Modifier.onSizeChanged { if (index == 0) setWidth = it.width.toFloat() }.padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) { items.forEach { FeatureChip(it) } }
        }
    }
}

internal fun Modifier.fadedEdges(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val mask = Brush.horizontalGradient(0f to Color.Transparent, 0.12f to Color.Black, 0.88f to Color.Black, 1f to Color.Transparent)
        drawRect(mask, blendMode = BlendMode.DstIn)
    }
