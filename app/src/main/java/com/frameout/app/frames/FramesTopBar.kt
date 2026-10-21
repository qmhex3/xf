package com.frameout.app.frames

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.QualityStrings
import com.frameout.core.util.ScrubStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.liveMessage
import com.frameout.designsystem.a11y.screenHeading
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameHairline

@Composable
fun FramesTopBar(
    name: String,
    fit: GridFit,
    count: Int,
    selected: Int,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onToggleFit: () -> Unit,
    onClearSelection: () -> Unit = {},
    onSelectAll: (() -> Unit)? = null,
    onTools: (() -> Unit)? = null,
    toolsAlert: Boolean = false,
    onTrash: (() -> Unit)? = null,
    inTrash: Boolean = false,
    trashCount: Int = 0
) {
    val colors = FrameTheme.colors
    var editing by remember(name) { mutableStateOf(false) }
    var text by remember(name) { mutableStateOf(name) }
    val selecting = selected > 0
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (editing) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { focus.requestFocus() }
            val commit = { if (text.isNotBlank()) onRename(text); editing = false }
            Box(
                Modifier.weight(1f).clip(FrameShapes.md).background(colors.fieldFill).border(1.dp, colors.primary.copy(alpha = 0.5f), FrameShapes.md).padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (text.isEmpty()) FrameText(QualityStrings.renameHint(), color = colors.onSurfaceVariant, maxLines = 1)
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = FrameTheme.typography.body.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commit() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus)
                )
            }
            HeaderButton(FrameIcons.Close, FrameStrings.cancel(), { text = name; editing = false })
            HeaderButton(FrameIcons.Check, FrameStrings.save(), { commit() }, active = true)
        } else {
            if (selecting) HeaderButton(FrameIcons.Close, FrameStrings.close(), onClearSelection)
            else HeaderButton(FrameIcons.Back, FrameStrings.back(), onBack)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (selecting) {
                    FrameText(
                        "$selected/$count",
                        style = FrameTheme.typography.title,
                        color = colors.primary,
                        modifier = Modifier.liveMessage().semantics { contentDescription = ScrubStrings.selectedOfTotal(selected, count) }
                    )
                } else {
                    FrameText(
                        name,
                        style = FrameTheme.typography.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.screenHeading().frameClickable { text = name; editing = true }
                    )
                    if (count > 0) FrameText(FrameStrings.frameCount(count), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1)
                }
            }
            if (selecting) onSelectAll?.let { HeaderButton(FrameIcons.SelectAll, FrameStrings.selectAll(), it) }
            if (!selecting) {
                onTrash?.takeIf { trashCount > 0 || inTrash }?.let { HeaderButton(FrameIcons.Trash, FrameStrings.deleted(), it, active = inTrash, count = trashCount) }
                onTools?.let { HeaderButton(FrameIcons.Tools, QualityStrings.toolsLabel(), it, badge = toolsAlert) }
                HeaderButton(FrameIcons.GridSize, if (fit == GridFit.Fill) FrameStrings.fill() else FrameStrings.fit(), onToggleFit)
            }
        }
    }
}

@Composable
internal fun HeaderButton(icon: Int, label: String, onClick: () -> Unit, active: Boolean = false, badge: Boolean = false, count: Int = 0) {
    val colors = FrameTheme.colors
    Box(
        Modifier
            .size(48.dp)
            .frameClickable(onClick = onClick)
            .semantics { contentDescription = if (count > 0) "$label $count" else label },
        Alignment.Center
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(FrameShapes.md)
                .background(if (active) colors.primary.copy(alpha = 0.16f) else colors.fieldFill)
                .border(1.dp, if (active) colors.primary.copy(alpha = 0.4f) else frameHairline(), FrameShapes.md),
            Alignment.Center
        ) {
            Image(painterResource(icon), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(if (active) colors.primary else colors.onSurface))
            if (badge && count <= 0) Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(8.dp).background(colors.error, FrameShapes.pill))
        }
        if (count > 0) CountBadge(count, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun CountBadge(count: Int, modifier: Modifier) {
    FrameText(
        if (count > 99) "99+" else count.toString(),
        style = FrameTheme.typography.label.copy(fontSize = 10.sp, textDirection = TextDirection.Ltr),
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .background(FrameTheme.colors.error, FrameShapes.pill)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}
