package com.frameout.designsystem.dock

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.delay

private val CoverSize = 34.dp
private val CoverHit = 48.dp
private val CoverGap = 17.dp
private val DockVerticalPad = 12.dp
private const val ENTER_DELAY_MS = 600L
private const val DRIFT_MS = 10_000L
private const val DRIFT_DP = 5f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoxScope.FrameFabCover(
    cover: Bitmap,
    contentDescription: String,
    dismissLabel: String,
    visible: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FrameTheme.colors
    val haptics = rememberFrameHaptics()
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val image = remember(cover) { cover.asImageBitmap() }
    var armed by remember { mutableStateOf(false) }
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(ENTER_DELAY_MS)
        armed = true
        delay(DRIFT_MS)
        settled = true
    }
    val enter = remember { Animatable(0f) }
    val enterSpec = frameSpring<Float>()
    LaunchedEffect(armed, visible) {
        enter.animateTo(if (armed && visible) 1f else 0f, enterSpec)
    }
    val amplitude by animateFloatAsState(if (settled || reduceMotion) 0f else 1f, tween(900), label = "coverAmplitude")
    val drifting = amplitude > 0.001f
    val driftX: State<Float>?
    val driftY: State<Float>?
    val breathe: State<Float>?
    if (drifting) {
        val transition = rememberInfiniteTransition(label = "coverDrift")
        driftX = transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(6300, easing = LinearEasing), RepeatMode.Reverse), label = "coverDriftX")
        driftY = transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(4700, easing = LinearEasing), RepeatMode.Reverse), label = "coverDriftY")
        breathe = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(5300, easing = LinearEasing), RepeatMode.Reverse), label = "coverBreathe")
    } else {
        driftX = null
        driftY = null
        breathe = null
    }
    val interaction = remember { MutableInteractionSource() }
    val live = enter.value > 0.05f
    Box(
        modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = coverBottomPadding())
            .size(CoverHit)
            .graphicsLayer {
                val e = enter.value.coerceIn(0f, 1.2f)
                val s = (0.3f + 0.7f * e) * (1f + 0.03f * amplitude * (breathe?.value ?: 0f))
                scaleX = s
                scaleY = s
                alpha = e.coerceIn(0f, 1f)
                translationX = DRIFT_DP.dp.toPx() * amplitude * (driftX?.value ?: 0f)
                translationY = DRIFT_DP.dp.toPx() * amplitude * (driftY?.value ?: 0f) + (1f - e) * 64.dp.toPx()
            }
            .then(
                if (live) Modifier
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = contentDescription,
                        onLongClickLabel = dismissLabel,
                        onLongClick = {
                            haptics.perform(FrameHaptic.Select)
                            onDismiss()
                        },
                        onClick = {
                            haptics.perform(FrameHaptic.Tap)
                            onClick()
                        }
                    )
                    .semantics { this.contentDescription = contentDescription }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            image,
            null,
            Modifier
                .size(CoverSize)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .border(2.dp, colors.surface, CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

private fun coverBottomPadding(): Dp =
    DockVerticalPad + IslandBarHeight + FrameFabSize / 2 + FabRaise + CoverGap - (CoverHit - CoverSize) / 2
