package com.frameout.app.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameHairline

@Composable
internal fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier, hint: String = FrameStrings.searchSaved(), trailing: (@Composable () -> Unit)? = null) {
    val colors = FrameTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val edge by animateColorAsState(if (focused) colors.primary.copy(alpha = 0.65f) else frameHairline(), frameSpring(), label = "searchEdge")
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        interactionSource = interaction,
        textStyle = FrameTheme.typography.body.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        modifier = modifier.height(48.dp).semantics { contentDescription = hint },
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().height(48.dp).background(colors.fieldFill, FrameShapes.pill).border(1.dp, edge, FrameShapes.pill).padding(start = 16.dp, end = if (trailing != null) 4.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(painterResource(FrameIcons.Search), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(if (focused) colors.primary else colors.onSurfaceVariant))
                Box(Modifier.weight(1f), Alignment.CenterStart) {
                    if (value.isEmpty()) FrameText(hint, color = colors.onSurfaceVariant, maxLines = 1)
                    inner()
                }
                if (value.isNotEmpty()) ClearButton { onChange("") }
                trailing?.invoke()
            }
        }
    )
}

@Composable
private fun ClearButton(onClick: () -> Unit) {
    val clear = FrameStrings.clearSearch()
    Box(
        Modifier.size(48.dp).clip(FrameShapes.pill).frameClickable(onClick = onClick).semantics { contentDescription = clear },
        Alignment.Center
    ) {
        Image(painterResource(FrameIcons.Close), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(FrameTheme.colors.onSurfaceVariant))
    }
}

@Composable
internal fun IconButtonBox(icon: Int, label: String, onClick: () -> Unit, active: Boolean = false) {
    val colors = FrameTheme.colors
    val tint = if (active) colors.primary else colors.onSurfaceVariant
    Box(
        Modifier
            .frameClickable(onClick = onClick)
            .semantics { contentDescription = label }
            .size(48.dp)
            .clip(FrameShapes.lg)
            .background(if (active) colors.primary.copy(alpha = 0.14f) else colors.fieldFill)
            .border(1.dp, if (active) colors.primary.copy(alpha = 0.4f) else frameHairline(), FrameShapes.lg),
        Alignment.Center
    ) {
        Image(painterResource(icon), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(tint))
    }
}
