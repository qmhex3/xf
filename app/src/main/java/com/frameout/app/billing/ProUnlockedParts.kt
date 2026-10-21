package com.frameout.app.billing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.R as DsR
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

private val ChipIcons = listOf(DsR.drawable.ic_filter, DsR.drawable.ic_shapes, DsR.drawable.ic_tools, DsR.drawable.ic_image_format, DsR.drawable.ic_sparkle, DsR.drawable.ic_export)

internal fun Modifier.revealAt(t: State<Float>, from: Float, to: Float): Modifier = graphicsLayer {
    val k = seg(t.value, from, to)
    alpha = k
    translationY = (1f - k) * 14.dp.toPx()
    scaleX = 0.94f + 0.06f * k
    scaleY = 0.94f + 0.06f * k
}

@Composable
internal fun DoneChips(t: State<Float>, colors: BoltColors) {
    val labels = ProStrings.doneChips()
    val tints = listOf(colors.first, colors.second, colors.third)
    labels.chunked(3).forEachIndexed { row, group ->
        Row(Modifier.padding(top = if (row == 0) 0.dp else 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            group.forEachIndexed { col, label ->
                val i = row * 3 + col
                DoneChip(ChipIcons[i % ChipIcons.size], label, tints[i % tints.size], Modifier.revealAt(t, 0.58f + 0.04f * i, 0.72f + 0.04f * i))
            }
        }
    }
}

@Composable
private fun DoneChip(icon: Int, label: String, tint: Color, modifier: Modifier) {
    val colors = FrameTheme.colors
    Row(
        modifier
            .background(colors.onSurface.copy(alpha = 0.08f), FrameShapes.pill)
            .border(1.dp, frameHairline(), FrameShapes.pill)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(icon), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(tint))
        FrameText(label, FrameTheme.typography.label, colors.onSurface, maxLines = 1)
    }
}

@Composable
internal fun DoneCta(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(proBrush(), FrameShapes.pill)
            .frameClickable(onClick = onClick),
        Alignment.Center
    ) {
        FrameText(ProStrings.doneCta(), FrameTheme.typography.title, proOnBrush(), textAlign = TextAlign.Center)
    }
}
