package com.frameout.app.library

import com.frameout.app.billing.ProStatusBadge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import com.frameout.designsystem.motion.frameSpring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.liveMessage
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameHairline

class HeaderActions(val onClear: () -> Unit, val onSelectAll: () -> Unit)

@Composable
fun LibraryHeader(selectedCount: Int, selectionMode: Boolean, liveCount: Int, exportProgress: ExportProgress?, actions: HeaderActions) {
    Box(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp).height(44.dp)) {
        Crossfade(selectionMode && selectedCount > 0, animationSpec = frameSpring(), label = "headerMode") { selecting ->
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selecting) {
                    val title = exportProgress?.let { FrameStrings.saving() + " " + FrameStrings.exportProgress(it.done, it.total) } ?: FrameStrings.selectedCount(selectedCount)
                    FrameText(title, style = FrameTheme.typography.title, maxLines = 1, modifier = Modifier.weight(1f).liveMessage())
                    SelectionActions(actions, exporting = exportProgress != null)
                } else {
                    LibraryBrand(Modifier.liveMessage())
                    ProStatusBadge()
                    if (liveCount > 0) LiveChip(liveCount)
                }
            }
        }
    }
}

@Composable
private fun SelectionActions(actions: HeaderActions, exporting: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!exporting) HeaderIconButton(FrameIcons.SelectAll, FrameStrings.selectAll(), onClick = actions.onSelectAll)
        HeaderIconButton(FrameIcons.Close, FrameStrings.cancel(), onClick = actions.onClear)
    }
}

@Composable
fun HeaderIconButton(icon: Int, label: String, accent: Boolean = false, tint: Color = Color.Unspecified, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val iconTint = if (tint != Color.Unspecified) tint else if (accent) colors.primary else colors.onSurfaceVariant
    val fill = if (accent) colors.primary.copy(alpha = 0.14f) else colors.fieldFill
    val edge = if (accent) colors.primary.copy(alpha = 0.40f) else frameHairline()
    Box(
        Modifier
            .frameClickable(onClick = onClick)
            .semantics { contentDescription = label }
            .size(44.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, edge, CircleShape),
        Alignment.Center
    ) {
        Image(painterResource(icon), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(iconTint))
    }
}

@Composable
private fun LiveChip(count: Int) {
    val colors = FrameTheme.colors
    val reduce = LocalSystemA11y.current.reduceMotion
    val dot = if (reduce) 1f else rememberInfiniteTransition(label = "chipDot").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "chipDotValue"
    ).value
    Row(
        Modifier
            .clip(FrameShapes.pill)
            .background(colors.primary.copy(alpha = 0.14f))
            .border(1.dp, colors.primary.copy(alpha = 0.35f), FrameShapes.pill)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(7.dp).alpha(dot).background(colors.success, CircleShape))
        FrameText("$count ${FrameStrings.extracting()}", style = FrameTheme.typography.monoSmall, color = colors.primary, maxLines = 1)
    }
}
