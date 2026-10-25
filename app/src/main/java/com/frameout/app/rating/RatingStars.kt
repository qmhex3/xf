package com.frameout.app.rating

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.core.util.RatingStrings
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
internal fun StarPicker(rating: Int, reduce: Boolean, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val current by rememberUpdatedState(onChange)
    val description = if (rating > 0) RatingStrings.starsA11y(rating) else RatingStrings.hint()
    val accent = FrameTheme.colors.accentA
    val burst = remember { Animatable(0f) }
    LaunchedEffect(rating) {
        if (rating == RATING_STARS && !reduce) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier.starTouch(rtl) { current(it) }.starSemantics(rating, description) { current(it) }, Alignment.Center) {
            StarRow(rating, reduce, rtl)
            if (burst.value > 0f && burst.value < 1f) {
                Canvas(Modifier.matchParentSize()) { drawSparks(burst.value, accent) }
            }
        }
    }
}

private fun Modifier.starTouch(rtl: Boolean, onPick: (Int) -> Unit): Modifier = pointerInput(rtl) {
    awaitEachGesture {
        val down = awaitFirstDown()
        onPick(starAt(down.position.x, size.width, rtl))
        down.consume()
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull() ?: break
            if (!change.pressed) break
            onPick(starAt(change.position.x, size.width, rtl))
            change.consume()
        }
    }
}

private fun Modifier.starSemantics(rating: Int, description: String, onPick: (Int) -> Unit): Modifier = semantics {
    contentDescription = description
    progressBarRangeInfo = ProgressBarRangeInfo(rating.toFloat(), 1f..RATING_STARS.toFloat(), RATING_STARS - 2)
    setProgress { value -> onPick(value.roundToInt().coerceIn(1, RATING_STARS)); true }
}

@Composable
private fun StarRow(rating: Int, reduce: Boolean, rtl: Boolean) {
    val outline = FrameTheme.colors.onSurfaceVariant.copy(alpha = 0.78f)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (position in 0 until RATING_STARS) {
            val number = if (rtl) RATING_STARS - position else position + 1
            Star(selected = number <= rating, index = number - 1, reduce = reduce, size = 46.dp, outline = outline)
        }
    }
}

private fun starAt(x: Float, width: Int, rtl: Boolean): Int {
    if (width <= 0) return 1
    val position = ((x / width) * RATING_STARS).toInt().coerceIn(0, RATING_STARS - 1)
    return if (rtl) RATING_STARS - position else position + 1
}

@Composable
private fun Star(selected: Boolean, index: Int, reduce: Boolean, size: Dp, outline: Color) {
    val fill = remember { Animatable(0f) }
    LaunchedEffect(selected) {
        if (selected) {
            if (!reduce) delay(index * 30L)
            fill.animateTo(1f, if (reduce) tween(0) else spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        } else {
            fill.animateTo(0f, tween(if (reduce) 0 else 110))
        }
    }
    Box(Modifier.size(size)) {
        Image(painterResource(FrameIcons.Star), null, Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(outline))
        Image(
            painterResource(FrameIcons.StarFilled), null,
            Modifier.fillMaxSize().graphicsLayer {
                val v = fill.value
                scaleX = v
                scaleY = v
                alpha = v.coerceIn(0f, 1f)
                rotationZ = (1f - v.coerceIn(0f, 1f)) * -25f
            },
            colorFilter = ColorFilter.tint(RatingGold)
        )
    }
}

private fun DrawScope.drawSparks(progress: Float, accent: Color) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val reach = size.width * 0.55f
    val count = 16
    for (i in 0 until count) {
        val angle = (2.0 * PI * i / count).toFloat()
        val ring = if (i % 2 == 0) 1f else 0.72f
        val dist = reach * ring * (1f - (1f - progress) * (1f - progress))
        val alpha = (1f - progress).coerceIn(0f, 1f)
        val radius = (if (i % 2 == 0) 3.2.dp else 2.2.dp).toPx() * (1f - progress * 0.6f)
        drawCircle(
            color = (if (i % 3 == 0) accent else RatingGold).copy(alpha = alpha),
            radius = radius,
            center = Offset(c.x + cos(angle) * dist, c.y + sin(angle) * dist * 0.55f)
        )
    }
}
