package com.frameout.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.delay

private const val MARQUEE_MS_PER_PX = 14
private const val MARQUEE_MIN_MS = 1200
private const val MARQUEE_PAUSE_MS = 1100L
private const val MARQUEE_MAX_CHARS = 400

@Composable
fun FrameMarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = FrameTheme.typography.body,
    color: Color = FrameTheme.colors.onSurface,
    textAlign: TextAlign? = null
) {
    val shown = if (text.length > MARQUEE_MAX_CHARS) text.take(MARQUEE_MAX_CHARS) else text
    if (LocalSystemA11y.current.reduceMotion) {
        FrameText(shown, style, color, modifier, textAlign, maxLines = 1, overflow = TextOverflow.Ellipsis)
        return
    }
    val scroll = rememberScrollState()
    LaunchedEffect(shown, scroll.maxValue) {
        val max = scroll.maxValue
        if (max <= 0) return@LaunchedEffect
        val duration = (max * MARQUEE_MS_PER_PX).coerceAtLeast(MARQUEE_MIN_MS)
        while (true) {
            delay(MARQUEE_PAUSE_MS)
            scroll.animateScrollTo(max, tween(duration, easing = LinearEasing))
            delay(MARQUEE_PAUSE_MS)
            scroll.animateScrollTo(0, tween(duration, easing = LinearEasing))
        }
    }
    Box(modifier.clipToBounds()) {
        FrameText(
            shown,
            style = style,
            color = color,
            textAlign = textAlign,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.horizontalScroll(scroll, enabled = false)
        )
    }
}
