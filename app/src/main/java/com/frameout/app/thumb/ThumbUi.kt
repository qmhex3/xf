package com.frameout.app.thumb

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.graphicsLayer
import com.frameout.designsystem.a11y.LocalSystemA11y
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun rememberThumb(key: Any?, peek: () -> Bitmap?, load: suspend () -> Bitmap?): State<Bitmap?> {
    val state = remember(key) { mutableStateOf(peek()) }
    LaunchedEffect(key) {
        if (state.value == null) state.value = load()
    }
    return state
}

@Composable
fun rememberSharpThumb(key: Any?, peek: () -> Bitmap?, load: suspend () -> Bitmap?, waitIdle: suspend () -> Unit): State<Bitmap?> {
    val state = remember(key) { mutableStateOf(peek()) }
    LaunchedEffect(key) {
        if (state.value == null) {
            waitIdle()
            delay(SHARP_SETTLE_MS)
            waitIdle()
            state.value = load()
        }
    }
    return state
}

@Composable
fun rememberThumbPulse(low: Float = 0.3f, high: Float = 0.7f): State<Float> {
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val still = remember(low, high) { mutableFloatStateOf((low + high) / 2f) }
    val moving = if (reduceMotion) null else rememberInfiniteTransition(label = "thumbPulse").animateFloat(
        initialValue = low,
        targetValue = high,
        animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "thumbPulseAlpha"
    )
    return moving ?: still
}

@Composable
fun ThumbPlaceholder(modifier: Modifier = Modifier, iconSize: Dp = 28.dp, icon: Int = FrameIconsExtra.ImageFormat, pulsing: Boolean = false) {
    val pulse = if (pulsing) rememberThumbPulse() else null
    Box(modifier.fillMaxSize(), Alignment.Center) {
        Image(
            painterResource(icon),
            null,
            Modifier.size(iconSize).graphicsLayer { alpha = pulse?.value ?: 0.45f },
            colorFilter = ColorFilter.tint(FrameTheme.colors.onSurfaceVariant)
        )
    }
}

@Composable
fun PrefetchAhead(gridState: LazyGridState, ahead: Int, load: suspend (Int) -> Unit) {
    val current = rememberUpdatedState(load)
    LaunchedEffect(gridState, ahead) {
        snapshotFlow {
            if (gridState.isScrollInProgress) -1 else gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }
            .distinctUntilChanged()
            .collectLatest { last ->
                if (last >= 0) {
                    (last + 1..last + ahead).chunked(PREFETCH_PARALLEL).forEach { batch ->
                        coroutineScope { batch.forEach { index -> launch { current.value(index) } } }
                    }
                }
            }
    }
}

private const val PREFETCH_PARALLEL = 4
private const val SHARP_SETTLE_MS = 90L
