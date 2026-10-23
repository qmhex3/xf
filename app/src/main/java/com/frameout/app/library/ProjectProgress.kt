package com.frameout.app.library

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.frameout.core.db.ProjectEntity
import com.frameout.core.db.progressTotal
import com.frameout.engine.work.UnifiedExtractProgress
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

internal sealed interface CardProgress {
    data object None : CardProgress
    data object Indeterminate : CardProgress
    data class Value(val fraction: Float, val percent: Int) : CardProgress
}

internal fun percentOf(fraction: Float): Int = Math.round(fraction * 100f).coerceIn(0, 99)

internal fun cardProgress(p: ProjectEntity, live: Float? = null): CardProgress {
    val running = p.status == "Extracting" || p.status == "Indexing"
    val fraction = UnifiedExtractProgress.stored(p.status, p.extractedCount, p.progressTotal, live)
    return when {
        p.status == "Completed" -> CardProgress.None
        fraction != null && (running || p.status == "Paused") ->
            CardProgress.Value(fraction, percentOf(fraction))
        running -> CardProgress.Indeterminate
        else -> CardProgress.None
    }
}

@Composable
internal fun ProjectProgressBar(progress: CardProgress, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val track = Modifier.fillMaxWidth().height(6.dp).clip(FrameShapes.pill).background(colors.onSurface.copy(alpha = 0.10f))
    when (progress) {
        CardProgress.None -> Unit
        CardProgress.Indeterminate -> Box(modifier.then(track).then(sweep()))
        is CardProgress.Value -> {
            val animated by animateFloatAsState(progress.fraction, tween(450), label = "cardProgress")
            Box(modifier.then(track)) {
                Box(Modifier.fillMaxWidth(animated.coerceAtLeast(0.02f)).height(6.dp).background(FrameGradients.Progress, FrameShapes.pill))
            }
        }
    }
}

@Composable
private fun sweep(): Modifier {
    val reduce = LocalSystemA11y.current.reduceMotion
    val x = if (reduce) null else rememberInfiniteTransition(label = "sweep").animateFloat(
        initialValue = -0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "sweepX"
    )
    val sweepBrush = FrameGradients.Progress
    return Modifier.drawBehind {
        val start = (x?.value ?: 0.3f) * size.width
        drawRoundRect(
            brush = sweepBrush,
            topLeft = Offset(start, 0f),
            size = Size(size.width * 0.4f, size.height),
            cornerRadius = CornerRadius(size.height / 2f)
        )
    }
}
