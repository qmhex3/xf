package com.frameout.app.rating

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.core.util.RatingStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun RatingButton(rating: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val enabled = rating > 0
    val shape = RoundedCornerShape(50)
    val idle = Brush.linearGradient(listOf(colors.accentA.copy(alpha = 0.16f), colors.accentB.copy(alpha = 0.10f)))
    val brush = if (enabled) FrameGradients.Fab else idle
    val edge = if (enabled) Color.White.copy(alpha = 0.22f) else colors.accentA.copy(alpha = 0.40f)
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(brush)
            .border(1.dp, edge, shape)
            .frameClickable(enabled = enabled, onClick = onClick),
        Alignment.Center
    ) {
        AnimatedContent(
            targetState = bucketOf(rating),
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(90)) },
            label = "rating-cta"
        ) { bucket ->
            FrameText(
                ctaFor(bucket),
                style = FrameTheme.typography.titleSmall,
                color = if (enabled) colors.onDarkAccent else colors.onSurface.copy(alpha = 0.88f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ctaFor(bucket: Bucket): String = when (bucket) {
    Bucket.Positive -> RatingStrings.ctaStore()
    Bucket.Negative -> RatingStrings.ctaFeedback()
    Bucket.None -> RatingStrings.ctaChoose()
}

@Composable
internal fun TextAction(label: String, color: Color, onClick: () -> Unit) {
    Box(Modifier.heightIn(min = 44.dp).frameClickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp), Alignment.Center) {
        FrameText(label, style = FrameTheme.typography.label, color = color)
    }
}
