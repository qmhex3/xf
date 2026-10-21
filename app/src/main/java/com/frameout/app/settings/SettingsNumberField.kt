package com.frameout.app.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameHoldRepeat
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

@Composable
fun SettingsNumberField(label: String, initial: Long, max: Long, step: Long = 1L, onCommit: (Long) -> Unit) {
    val colors = FrameTheme.colors
    var draft by remember { mutableStateOf(initial.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val apply = { raw: String ->
        val edit = evaluateNumber(raw, max)
        draft = edit.draft
        error = edit.error
        edit.commit?.let(onCommit)
    }
    val nudge = { direction: Int ->
        val current = draft.toLongOrNull() ?: 0L
        apply((current + direction * step).coerceIn(1L, max).toString())
    }
    val edge = if (error != null) colors.error.copy(alpha = 0.6f) else frameHairline()
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FrameText(label, style = FrameTheme.typography.label, color = colors.onSurfaceVariant)
        NumberStepper(label, draft, edge, { apply(it) }, { nudge(-1) }, { nudge(1) })
        error?.let {
            FrameText(it, style = FrameTheme.typography.bodySmall, color = colors.error)
        }
    }
}

@Composable
private fun NumberStepper(label: String, draft: String, edge: Color, onChange: (String) -> Unit, onMinus: () -> Unit, onPlus: () -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(FrameShapes.md)
            .background(colors.background)
            .border(1.dp, edge, FrameShapes.md)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton(FrameIconsExtra.Minus, FrameStrings.decrease(), false, onMinus)
        BasicTextField(
            value = draft,
            onValueChange = onChange,
            textStyle = FrameTheme.typography.mono.copy(
                color = colors.onSurface,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(colors.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).semantics { contentDescription = label }
        )
        StepButton(FrameIconsExtra.Plus, FrameStrings.increase(), true, onPlus)
    }
}

@Composable
private fun StepButton(icon: Int, description: String, filled: Boolean, onStep: () -> Unit) {
    val colors = FrameTheme.colors
    Box(
        Modifier
            .size(48.dp)
            .clip(FrameShapes.sm)
            .background(if (filled) FrameGradients.Brand else SolidColor(colors.containerHigh))
            .frameHoldRepeat(onStep)
            .semantics { contentDescription = description },
        Alignment.Center
    ) {
        Image(
            painterResource(icon),
            null,
            Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(if (filled) Color.White else colors.onSurface)
        )
    }
}

private class NumberEdit(val draft: String, val error: String?, val commit: Long?)

private fun evaluateNumber(raw: String, max: Long): NumberEdit {
    val digits = raw.filter(Char::isDigit).take(9)
    val value = digits.toLongOrNull() ?: 0L
    return when {
        value <= 0L -> NumberEdit(digits, FrameStrings.numberRange(max), null)
        value > max -> NumberEdit(max.toString(), FrameStrings.numberMax(max), max)
        else -> NumberEdit(digits, null, value)
    }
}
