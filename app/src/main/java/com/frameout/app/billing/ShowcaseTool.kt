package com.frameout.app.billing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.frameout.core.pro.ProShowcase
import com.frameout.designsystem.R as DsR
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.theme.FrameTheme

private const val CYCLE_MS = 7200
private val Frame = RoundedCornerShape(18.dp)

@Composable
internal fun ToolShowcase() {
    val colors = FrameTheme.colors
    val style = FrameTheme.typography.titleSmall
    val scene = rememberShowcaseScene()
    val reveal by animateFloatAsState(if (scene != null) 1f else 0f, tween(300), label = "sceneReveal")
    val cycle = rememberCycle(LocalSystemA11y.current.reduceMotion, CYCLE_MS, 0.9f)
    val active by remember { derivedStateOf { activeTool(cycle.value) } }
    val measurer = rememberTextMeasurer()
    val caption = remember { ProShowcase.caption() }
    val shape = Modifier.size(SCENE_W_DP.dp, SCENE_H_DP.dp).clip(Frame).border(1.dp, Color.White.copy(alpha = 0.22f), Frame)
    Box(shape) {
        Canvas(Modifier.size(SCENE_W_DP.dp, SCENE_H_DP.dp).graphicsLayer { alpha = reveal }) {
            val s = scene ?: return@Canvas
            val p = cycle.value
            drawImage(s.sharp)
            drawBlurLens(s, phase(p, 0.06f, 0.30f), colors.accentA)
            drawPixelPlate(s, phase(p, 0.36f, 0.58f), colors.accentB)
            drawCaption(measurer, style, caption, phase(p, 0.64f, 0.86f), colors.accentA)
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolChip(DsR.drawable.ic_blur, active == 0, colors.accentA)
            ToolChip(DsR.drawable.ic_pixelate, active == 1, colors.accentB)
            ToolChip(DsR.drawable.ic_text, active == 2, colors.accentC)
        }
    }
}
