package com.frameout.designsystem.dock

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

val LocalDockFabInset = compositionLocalOf { 0.dp }

internal val MergedFabInset: Dp = FrameFabSize + 2.dp

private val PillWidth = 252.dp
private val PillHeight = 44.dp

@Composable
fun FrameCompactStatus(
    primary: String,
    secondary: String,
    actionIcon: Int,
    actionDescription: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FrameTheme.colors
    val inset = LocalDockFabInset.current
    GlassSurface(shape = FrameShapes.pill, modifier = modifier.width(PillWidth).height(PillHeight)) {
        Row(
            Modifier.fillMaxSize().padding(start = 14.dp + inset, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                FrameText(primary, style = FrameTheme.typography.label, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (secondary.isNotEmpty()) {
                    FrameText(secondary, style = FrameTheme.typography.monoSmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(Modifier.size(44.dp).frameClickable(onClickLabel = actionDescription, onClick = onAction), Alignment.Center) {
                Box(Modifier.size(28.dp).background(colors.onSurface.copy(alpha = 0.08f), CircleShape), Alignment.Center) {
                    Image(
                        painter = painterResource(actionIcon),
                        contentDescription = actionDescription,
                        modifier = Modifier.size(14.dp),
                        colorFilter = ColorFilter.tint(colors.onSurfaceVariant)
                    )
                }
            }
        }
    }
}
