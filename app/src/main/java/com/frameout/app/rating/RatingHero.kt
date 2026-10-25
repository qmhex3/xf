package com.frameout.app.rating

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun Hero(rating: Int, reduce: Boolean) {
    val colors = FrameTheme.colors
    val low = rating in 1..3
    val amount = remember { Animatable(0f) }
    LaunchedEffect(low) { amount.animateTo(if (low) 1f else 0f, tween(if (reduce) 0 else 320)) }
    val top = lerp(colors.accentC, RatingGoldDeep, amount.value)
    val mid = lerp(colors.accentA, RatingGold, amount.value)
    val bottom = lerp(colors.accentB, RatingGoldDeep, amount.value)
    val infinite = rememberInfiniteTransition(label = "hero")
    val float by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    val pulse by infinite.animateFloat(0.55f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    Box(Modifier.size(112.dp), Alignment.Center) {
        HeroGlow(mid, if (reduce) 0.8f else pulse)
        HeroDisc(Brush.linearGradient(listOf(top, mid, bottom)), rating, reduce, float)
    }
}

@Composable
private fun HeroGlow(color: Color, glow: Float) {
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = 0.42f * glow), Color.Transparent), center, size.minDimension / 2f),
            radius = size.minDimension / 2f
        )
    }
}

private fun faceFor(rating: Int) = when (rating) {
    1, 2 -> FrameIcons.FacePoor
    3 -> FrameIcons.FaceFair
    4 -> FrameIcons.FaceSmile
    5 -> FrameIcons.FaceGood
    else -> FrameIcons.StarFilled
}

private fun faceTransition(reduce: Boolean): ContentTransform =
    if (reduce) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
    else (fadeIn(tween(150)) + scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), initialScale = 0.55f)) togetherWith fadeOut(tween(80))

@Composable
private fun HeroDisc(brush: Brush, rating: Int, reduce: Boolean, float: Float) {
    Box(
        Modifier
            .size(78.dp)
            .graphicsLayer { translationY = if (reduce) 0f else (float - 0.5f) * 6.dp.toPx() }
            .border(1.5.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            .background(brush, CircleShape),
        Alignment.Center
    ) {
        AnimatedContent(targetState = faceFor(rating), transitionSpec = { faceTransition(reduce) }, label = "face") { icon ->
            Image(painterResource(icon), null, Modifier.size(44.dp), colorFilter = ColorFilter.tint(Color.White))
        }
    }
}

@Composable
internal fun SuccessChip(text: String, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val tint = colors.accentA
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.14f))
            .border(1.dp, tint.copy(alpha = 0.30f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(painterResource(FrameIcons.Check), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(tint))
        FrameText(text, style = FrameTheme.typography.label, color = colors.onSurface)
    }
}
