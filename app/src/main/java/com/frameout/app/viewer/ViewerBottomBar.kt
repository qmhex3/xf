package com.frameout.app.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.frameout.core.util.EditStrings
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.SearchStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.dock.FrameFab
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun ViewerBottomBar(
    playing: Boolean,
    onPlay: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onSearch: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    showPlay: Boolean = true,
    showGoTo: Boolean = false,
    onGoTo: () -> Unit = {}
) {
    val colors = FrameTheme.colors
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ViewerGlassSurface(modifier = Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (showPlay) BarButton(if (playing) FrameStrings.pause() else FrameStrings.play(), if (playing) FrameIcons.PauseFilled else FrameIcons.PlayFilled, ViewerInk.accent, tonal = true, onClick = onPlay)
                if (showGoTo) BarButton(FrameStrings.showInProject(), FrameIconsExtra.GoToFrame, ViewerInk.accent, tonal = true, onClick = onGoTo)
                BarButton(FrameStrings.save(), FrameIcons.Save, ViewerInk.text, onClick = onSave)
                BarButton(FrameStrings.share(), FrameIcons.Share, ViewerInk.text, onClick = onShare)
                BarButton(SearchStrings.actionShort(), FrameIcons.Search, ViewerInk.text, onClick = onSearch)
                BarButton(FrameStrings.delete(), FrameIcons.Trash, ViewerInk.danger, onClick = onDelete)
            }
        }
        FrameFab(FrameIconsExtra.Edit, EditStrings.tools(), onClick = onEdit)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BarButton(
    label: String,
    icon: Int,
    tint: Color,
    tonal: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .weight(1f, fill = false)
            .widthIn(min = 44.dp, max = 52.dp)
            .height(48.dp)
            .then(if (tonal) Modifier.viewerActiveGlow(tint) else Modifier)
            .clip(CircleShape)
            .then(if (tonal) Modifier.background(tint.copy(alpha = 0.16f)) else Modifier)
            .frameClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(tint)
        )
    }
}
