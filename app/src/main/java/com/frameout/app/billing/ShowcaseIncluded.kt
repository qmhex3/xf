package com.frameout.app.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.pro.ProShowcase
import com.frameout.core.pro.ShowcaseKind
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameAccents
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

private const val MAX_STAGGER = 5
private val AccentDots = FrameAccents.All.drop(1).take(5)

@Composable
fun IncludedWithThis(kind: ShowcaseKind, motion: SheetMotion, modifier: Modifier = Modifier) {
    val names = remember(kind) { ProShowcase.includedNames(kind) }
    val hidden = ProShowcase.hiddenCount(kind)
    Column(modifier.fillMaxWidth()) {
        FrameText(ProShowcase.title(), FrameTheme.typography.label.copy(fontSize = 11.sp), FrameTheme.colors.onSurfaceVariant, Modifier.padding(start = 20.dp, bottom = 7.dp))
        Row(
            Modifier.fillMaxWidth().fadedEnd().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            names.forEachIndexed { index, name -> IncludedChip(name, dotBrush(kind, index), Modifier.revealed(motion.reveal, 2 + minOf(index, MAX_STAGGER))) }
            if (hidden > 0) MoreChip(ProShowcase.more(hidden), Modifier.revealed(motion.reveal, 2 + MAX_STAGGER))
        }
    }
}

@Composable
private fun dotBrush(kind: ShowcaseKind, index: Int): Brush {
    val colors = FrameTheme.colors
    val accent = if (kind == ShowcaseKind.Accent) AccentDots.getOrNull(index) else null
    return if (accent != null) accentBrush(accent) else Brush.linearGradient(listOf(colors.accentC, colors.accentA, colors.accentB))
}

@Composable
private fun IncludedChip(text: String, dot: Brush, modifier: Modifier) {
    val colors = FrameTheme.colors
    Row(
        modifier
            .background(colors.onSurface.copy(alpha = 0.06f), FrameShapes.pill)
            .border(1.dp, frameHairline(), FrameShapes.pill)
            .padding(start = 8.dp, end = 11.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(14.dp).background(dot, FrameShapes.pill))
        FrameText(text, FrameTheme.typography.bodySmall, colors.onSurface, Modifier.padding(start = 7.dp), maxLines = 1, softWrap = false)
    }
}

@Composable
private fun MoreChip(text: String, modifier: Modifier) {
    FrameText(
        text,
        FrameTheme.typography.label,
        FrameTheme.colors.primary,
        modifier.padding(horizontal = 11.dp, vertical = 7.dp)
    )
}
