package com.frameout.app.frames

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.QualityStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.motion.fxFloat
import com.frameout.designsystem.motion.fxRise
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun FramesEmptyState(filter: FrameFilter, extracting: Boolean, onClearFilter: () -> Unit) {
    val colors = FrameTheme.colors
    val progress = com.frameout.app.extract.LocalExtractProgress.current
    if (filter == FrameFilter.All && extracting && progress != null) {
        Box(Modifier.fillMaxSize().padding(bottom = 120.dp, start = 24.dp, end = 24.dp), contentAlignment = Alignment.Center) {
            com.frameout.app.extract.ExtractProgressPanel(progress)
        }
        return
    }
    val icon: Int
    val title: String
    val hint: String
    when {
        filter == FrameFilter.All && extracting -> { icon = FrameIcons.VideoAdd; title = QualityStrings.extractingTitle(); hint = QualityStrings.extractingHint() }
        filter == FrameFilter.All -> { icon = FrameIcons.Collage; title = FrameStrings.empty(); hint = QualityStrings.emptyAllHint() }
        filter == FrameFilter.Favorites -> { icon = FrameIcons.Star; title = FrameStrings.emptyFavorites(); hint = QualityStrings.emptyFavoritesHint() }
        filter == FrameFilter.Deleted -> { icon = FrameIcons.Trash; title = FrameStrings.emptyDeleted(); hint = QualityStrings.emptyDeletedHint() }
        filter == FrameFilter.Sharpest -> { icon = FrameIcons.Filter; title = QualityStrings.sharpestEmptyTitle(); hint = QualityStrings.sharpestEmptyHint() }
        else -> { icon = FrameIcons.Filter; title = FrameStrings.emptyTagged(); hint = QualityStrings.emptyTaggedHint() }
    }
    Box(Modifier.fillMaxSize().padding(bottom = 120.dp, start = 32.dp, end = 32.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.fxRise(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fxFloat().size(72.dp).clip(FrameShapes.pill).background(colors.primary.copy(alpha = 0.12f)), Alignment.Center) {
                Image(painterResource(icon), null, Modifier.size(32.dp), colorFilter = ColorFilter.tint(colors.primary))
            }
            FrameText(title, style = FrameTheme.typography.title, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            FrameText(hint, style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
            if (filter == FrameFilter.Tagged) {
                Box(Modifier.padding(top = 8.dp).clip(FrameShapes.pill).background(colors.primary.copy(alpha = 0.14f)).frameClickable(onClick = onClearFilter).padding(horizontal = 18.dp, vertical = 10.dp)) {
                    FrameText(QualityStrings.contentClear(), style = FrameTheme.typography.label, color = colors.primary)
                }
            }
        }
    }
}
