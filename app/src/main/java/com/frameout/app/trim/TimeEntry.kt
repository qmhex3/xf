package com.frameout.app.trim

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.input.TextFieldValue as TFV
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import com.frameout.engine.timeline.formatTimestamp
import com.frameout.engine.timeline.parseTimestamp

private const val MAX_CHARS = 12

private fun normalizeChar(c: Char): Char? = when (c) {
    in '0'..'9' -> c
    in '\u0660'..'\u0669' -> '0' + (c - '\u0660')
    in '\u06F0'..'\u06F9' -> '0' + (c - '\u06F0')
    ':', '\u0589', '\uFF1A' -> ':'
    '.', ',', '\u066B', '\u066C', '\u060C', '\u061B' -> '.'
    else -> null
}

private fun clean(text: String): String = buildString { text.forEach { c -> normalizeChar(c)?.let(::append) } }.take(MAX_CHARS)

private fun sanitized(value: TFV): TFV {
    val text = clean(value.text)
    if (text == value.text) return value
    val start = clean(value.text.take(value.selection.start)).length.coerceAtMost(text.length)
    val end = clean(value.text.take(value.selection.end)).length.coerceAtMost(text.length)
    return TFV(text, TextRange(start, end))
}

@Stable
private class EntryState(var valueMs: Long) {
    var focused by mutableStateOf(false)
    var dirty by mutableStateOf(false)
    var field by mutableStateOf(TFV(formatTimestamp(valueMs)))

    fun sync(ms: Long) {
        valueMs = ms
        if (!focused) reset()
    }

    fun reset() {
        field = TFV(formatTimestamp(valueMs))
        dirty = false
    }

    fun edit(raw: TFV) {
        val next = sanitized(raw)
        if (next.text != field.text) dirty = true
        field = next
    }

    fun finish(commit: (Long) -> Unit) {
        if (dirty) parseTimestamp(field.text)?.let(commit)
        reset()
    }
}

@Composable
internal fun TimeEntry(valueMs: Long, modifier: Modifier, flat: Boolean = false, emphasis: Boolean = false, onCommit: (Long) -> Unit) {
    val colors = FrameTheme.colors
    val focus = LocalFocusManager.current
    val commit by rememberUpdatedState(onCommit)
    val state = remember { EntryState(valueMs) }
    LaunchedEffect(valueMs, state.focused) { state.sync(valueMs) }
    val size = if (flat) (if (emphasis) 16.sp else 15.sp) else 13.sp
    val style = FrameTheme.typography.mono.copy(
        color = if (emphasis) colors.primary else colors.onSurface,
        fontSize = size,
        fontWeight = if (emphasis) FontWeight.SemiBold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        textDirection = TextDirection.Ltr
    )
    Box(modifier.then(entryFrame(flat, state.focused)), Alignment.Center) {
        BasicTextField(
            value = state.field,
            onValueChange = state::edit,
            textStyle = style,
            cursorBrush = SolidColor(colors.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                state.finish(commit)
                focus.clearFocus()
            }),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focusState ->
                if (!focusState.isFocused && state.focused) state.finish(commit)
                state.focused = focusState.isFocused
            }
        )
    }
}

@Composable
private fun entryFrame(flat: Boolean, focused: Boolean): Modifier {
    val colors = FrameTheme.colors
    val outline = if (focused) colors.primary else frameHairline()
    if (!flat) return Modifier.height(34.dp).clip(FrameShapes.sm).background(colors.surface).border(1.dp, outline, FrameShapes.sm)
    return Modifier.height(40.dp).drawBehind {
        val y = size.height - 7.dp.toPx()
        drawLine(outline, Offset(size.width * 0.12f, y), Offset(size.width * 0.88f, y), if (focused) 2.dp.toPx() else 1.dp.toPx())
    }
}
