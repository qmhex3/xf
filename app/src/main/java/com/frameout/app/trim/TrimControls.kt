package com.frameout.app.trim

import androidx.compose.ui.graphics.Color
import com.frameout.designsystem.theme.frameHairline
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.theme.frameControl
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.engine.timeline.TrimRange
import com.frameout.engine.timeline.formatTimestamp
import com.frameout.engine.timeline.parseTimestamp

@Composable
fun TimeFields(range: TrimRange, durationMs: Long, onRangeChange: (TrimRange) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
        TimeField(FrameStrings.from(), formatTimestamp(range.startMs), Modifier.weight(1f)) { text ->
            val ms = parseTimestamp(text) ?: return@TimeField false
            onRangeChange(range.copy(startMs = ms.coerceIn(0, range.endMs), useFull = false).clamped(durationMs))
            true
        }
        TimeField(FrameStrings.to(), formatTimestamp(range.endMs), Modifier.weight(1f)) { text ->
            val ms = parseTimestamp(text) ?: return@TimeField false
            onRangeChange(range.copy(endMs = ms.coerceIn(range.startMs, durationMs), useFull = false).clamped(durationMs))
            true
        }
    }
}

@Composable
private fun TimeField(label: String, value: String, modifier: Modifier = Modifier, onCommit: (String) -> Boolean) {
    val colors = FrameTheme.colors
    val focus = LocalFocusManager.current
    var draft by remember(value) { mutableStateOf(value) }
    var invalid by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        draft = value
        invalid = false
    }
    val borderColor = when {
        invalid -> colors.error
        focused -> colors.primary
        else -> colors.outline
    }
    Column(modifier.then(if (focused) Modifier.frameGlow(colors.primary, { 0.22f }, 8.dp, { 0.3f }) else Modifier).clip(FrameShapes.sm).background(colors.fieldFill).border(1.dp, borderColor, FrameShapes.sm).padding(horizontal = 12.dp, vertical = 8.dp)) {
        FrameText(label, style = FrameTheme.typography.label, color = colors.onSurfaceVariant)
        BasicTextField(
            value = draft,
            onValueChange = {
                draft = it
                invalid = false
            },
            textStyle = FrameTheme.typography.mono.copy(color = colors.onSurface, textDirection = TextDirection.Ltr, textAlign = TextAlign.Start),
            cursorBrush = SolidColor(colors.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                invalid = !onCommit(draft)
                if (!invalid) {
                    draft = value
                    focus.clearFocus()
                }
            }),
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp).onFocusChanged { focused = it.isFocused }
        )
        if (invalid) TimeFieldHint()
    }
}

@Composable
private fun TimeFieldHint() {
    FrameText(
        "mm:ss or h:mm:ss",
        style = FrameTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr),
        color = FrameTheme.colors.error,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
internal fun RangeSegmented(useFull: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(FrameShapes.pill).background(FrameTheme.colors.fieldFill).border(1.dp, frameHairline(), FrameShapes.pill).padding(4.dp),
        Arrangement.spacedBy(4.dp)
    ) {
        Segment(FrameStrings.fullVideo(), useFull, Modifier.weight(1f)) { onChange(true) }
        Segment(FrameStrings.selection(), !useFull, Modifier.weight(1f)) { onChange(false) }
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Box(
        modifier
            .frameSelectable(selected, Role.RadioButton, onClick)
            .height(36.dp)
            .then(if (selected) Modifier.frameControl(FrameShapes.pill, selected = true, solid = true, glowAlpha = 0.7f) else Modifier)
            .clip(FrameShapes.pill),
        Alignment.Center
    ) {
        FrameText(label, style = FrameTheme.typography.label, color = if (selected) Color.White else colors.onSurfaceVariant)
    }
}
