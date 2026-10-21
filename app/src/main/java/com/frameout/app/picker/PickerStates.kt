package com.frameout.app.picker

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import com.frameout.app.thumb.rememberThumbPulse
import com.frameout.designsystem.a11y.LocalSystemA11y
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.liveMessage
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill

@Composable
fun PickerMessage(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    @DrawableRes icon: Int = FrameIconsExtra.Album,
    onAction: (() -> Unit)? = null
) {
    val colors = FrameTheme.colors
    Column(
        modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(72.dp).background(colors.fieldFill, CircleShape), contentAlignment = Alignment.Center) {
            Image(painterResource(icon), null, Modifier.size(32.dp), colorFilter = ColorFilter.tint(colors.onSurfaceVariant))
        }
        FrameText(
            text,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp).liveMessage()
        )
        if (actionLabel != null && onAction != null) {
            FrameText(
                actionLabel,
                style = FrameTheme.typography.label,
                color = Color.White,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .frameClickable(onClick = onAction)
                    .background(FrameGradients.Brand, FrameShapes.pill)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
fun PickerSkeleton(modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val loading = FrameStrings.loading()
    val pulse = rememberThumbPulse(0.55f, 1f)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(PickerGrid.MinCell),
        modifier = modifier.semantics { contentDescription = loading },
        userScrollEnabled = false,
        contentPadding = PickerGrid.padding(bottom = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(PickerGrid.Gap),
        verticalArrangement = Arrangement.spacedBy(PickerGrid.Gap)
    ) {
        items(36) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer { alpha = pulse.value }
                    .background(colors.fieldFill, FrameShapes.sm)
            )
        }
    }
}

@Composable
fun PickerLoadingBar(modifier: Modifier = Modifier) {
    val color = FrameTheme.colors.primary
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val progress = if (reduceMotion) {
        0.5f
    } else {
        val value by rememberInfiniteTransition(label = "pickerBar").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
            label = "pickerBarX"
        )
        value
    }
    Canvas(modifier.fillMaxWidth().height(3.dp)) {
        drawRect(color.copy(alpha = 0.18f))
        val segment = size.width * 0.32f
        val start = (size.width + segment) * progress - segment
        val from = start.coerceAtLeast(0f)
        val to = (start + segment).coerceAtMost(size.width)
        if (to > from) drawRect(color, Offset(from, 0f), Size(to - from, size.height))
    }
}
