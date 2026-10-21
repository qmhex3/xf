package com.frameout.app.billing

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.frameout.core.edit.FilterPresets
import com.frameout.core.pro.ProShowcase
import com.frameout.core.util.EditStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.delay

private const val WIPE_MS = 3200
private const val STILL_WIPE = 0.55f
private val PhotoShape = RoundedCornerShape(18.dp)

@Composable
internal fun FilterShowcase() {
    val reduce = LocalSystemA11y.current.reduceMotion
    val ids = remember { ProShowcase.proFilterIds() }
    val scene = rememberShowcaseScene()
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(reduce) {
        if (reduce) return@LaunchedEffect
        while (true) {
            delay(WIPE_MS.toLong())
            index = (index + 1) % ids.size
        }
    }
    val wipe = rememberWipe(reduce)
    val id = ids[index]
    val filter = remember(id) { ColorFilter.colorMatrix(ColorMatrix(FilterPresets.matrix(id, 1f))) }
    Box(Modifier.size(SCENE_W_DP.dp, SCENE_H_DP.dp).clip(PhotoShape).border(1.dp, Color.White.copy(alpha = 0.2f), PhotoShape)) {
        Canvas(Modifier.size(SCENE_W_DP.dp, SCENE_H_DP.dp)) {
            val s = scene ?: return@Canvas
            val x = size.width * wipe.value
            drawImage(s.sharp)
            clipRect(right = x) { drawImage(s.sharp, colorFilter = filter) }
            drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
        }
        FrameText(
            EditStrings.filterName(id),
            FrameTheme.typography.label,
            Color.White,
            Modifier.align(Alignment.BottomStart).padding(8.dp).background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp)).padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun rememberWipe(reduce: Boolean): androidx.compose.runtime.State<Float> {
    if (reduce) return remember { androidx.compose.runtime.mutableFloatStateOf(STILL_WIPE) }
    val transition = rememberInfiniteTransition(label = "wipe")
    return transition.animateFloat(0f, 1f, infiniteRepeatable(tween(WIPE_MS, easing = LinearEasing)), label = "wipeValue")
}
