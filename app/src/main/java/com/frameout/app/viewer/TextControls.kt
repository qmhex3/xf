package com.frameout.app.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.frameout.core.edit.BrushScale
import com.frameout.core.edit.TextMark
import com.frameout.core.util.EditStrings
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import kotlin.math.abs
import kotlin.math.roundToInt

private val DARK_BACKGROUND = 0xCC000000.toInt()
private val LIGHT_BACKGROUND = 0xCCFFFFFF.toInt()
private const val MIN_OPACITY = 0.05f
private const val ROTATION_SNAP = 3f

internal fun pickerColorFor(draft: EditDraft): Int {
    val mark = draft.selectedText ?: draft.text
    return when (draft.pickerTarget) {
        ColorTarget.Brush -> draft.color
        ColorTarget.TextFill -> mark.color
        ColorTarget.TextOutline -> mark.outlineColor
        ColorTarget.TextBackground -> if (android.graphics.Color.alpha(mark.background) > 0) mark.background else 0xFFFFFFFF.toInt()
    }
}

@Composable
internal fun ColumnScope.TextControls(draft: EditDraft, dims: ImageDims, tools: ViewerToolEditor) {
    val mark = draft.selectedText ?: draft.text
    var tab by rememberSaveable { mutableStateOf(TextTab.Style) }
    TextInputRow(draft, tools)
    TextTabs(tab) { tab = it }
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (tab) {
            TextTab.Style -> StyleTab(mark, dims, tools)
            TextTab.Color -> ColorTab(mark, tools)
            TextTab.Effects -> EffectsTab(mark, tools)
        }
    }
}

@Composable
private fun TextInputRow(draft: EditDraft, tools: ViewerToolEditor) {
    val colors = FrameTheme.colors
    val focus = remember { FocusRequester() }
    val selectedText = draft.selectedText
    val content = selectedText?.text.orEmpty()
    var field by remember(draft.selected) { mutableStateOf(TextFieldValue(content, TextRange(content.length))) }
    val hint = EditStrings.textHint()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BasicTextField(
            value = field,
            onValueChange = { field = it; tools.setTextContent(it.text) },
            textStyle = FrameTheme.typography.body.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            maxLines = 3,
            modifier = Modifier.weight(1f).focusRequester(focus).semantics { contentDescription = hint },
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(FrameShapes.md).background(colors.fieldFill)
                        .border(1.dp, colors.outline, FrameShapes.md).padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (field.text.isEmpty()) FrameText(hint, style = FrameTheme.typography.body, color = colors.onSurfaceVariant)
                    inner()
                }
            }
        )
        PanelIconButton(FrameIconsExtra.Plus, EditStrings.textAdd()) {
            tools.addText()
            focus.requestFocus()
        }
        PanelIconButton(FrameIcons.Trash, EditStrings.textDelete(), enabled = selectedText != null) { tools.deleteSelectedText() }
    }
}

@Composable
private fun TextTabs(tab: TextTab, onTab: (TextTab) -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(FrameShapes.md).background(colors.fieldFill).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            TextTab.Style to EditStrings.tabStyle(),
            TextTab.Color to EditStrings.tabColor(),
            TextTab.Effects to EditStrings.tabEffects()
        ).forEach { (value, label) ->
            val selected = tab == value
            val tint = if (selected) colors.primary else colors.onSurfaceVariant
            Box(
                Modifier.weight(1f).clip(FrameShapes.sm)
                    .background(if (selected) colors.primary.copy(alpha = 0.18f) else Color.Transparent)
                    .frameSelectable(selected, Role.Tab) { onTab(value) }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) { FrameText(label, style = FrameTheme.typography.label, color = tint) }
        }
    }
}

@Composable
private fun StyleTab(mark: TextMark, dims: ImageDims, tools: ViewerToolEditor) {
    val lo = TextMark.MIN_SIZE
    val hi = TextMark.MAX_SIZE
    PrecisionRow(
        label = EditStrings.size(),
        valueText = "${BrushScale.pixels(mark.size, dims.w, dims.h)} px",
        fraction = BrushScale.toFraction(mark.size, lo, hi),
        onFraction = { f -> tools.editText { it.copy(size = BrushScale.fromFraction(f, lo, hi)) } },
        onStep = { dir -> tools.editText { it.copy(size = BrushScale.stepPixels(it.size, dir, dims.w, dims.h, lo, hi)) } }
    ) { TextGlyph() }
    PrecisionRow(
        label = EditStrings.rotation(),
        valueText = "${mark.rotation.roundToInt()}°",
        fraction = ((mark.rotation + 180f) / 360f).coerceIn(0f, 1f),
        onFraction = { f ->
            val degrees = f * 360f - 180f
            tools.editText { it.copy(rotation = if (abs(degrees) < ROTATION_SNAP) 0f else degrees) }
        },
        onStep = { dir -> tools.editText { it.copy(rotation = (it.rotation + dir).coerceIn(-180f, 180f)) } }
    ) { TextIcon(FrameIconsExtra.Rotate) }
    StyleChips(mark, tools)
}

@Composable
private fun StyleChips(mark: TextMark, tools: ViewerToolEditor) {
    val bold = EditStrings.bold()
    val italic = EditStrings.italic()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        StyleChip("B", bold, mark.bold, Modifier.weight(1f), FontFamily.SansSerif, FontWeight.Bold, FontStyle.Normal) {
            tools.editText { it.copy(bold = !it.bold) }
        }
        StyleChip("I", italic, mark.italic, Modifier.weight(1f), FontFamily.Serif, FontWeight.Normal, FontStyle.Italic) {
            tools.editText { it.copy(italic = !it.italic) }
        }
        listOf(
            Triple(TextMark.FONT_SANS, EditStrings.fontSans(), FontFamily.SansSerif),
            Triple(TextMark.FONT_SERIF, EditStrings.fontSerif(), FontFamily.Serif),
            Triple(TextMark.FONT_MONO, EditStrings.fontMono(), FontFamily.Monospace),
            Triple(TextMark.FONT_CURSIVE, EditStrings.fontCursive(), FontFamily.Cursive)
        ).forEach { (font, label, family) ->
            StyleChip("Aa", label, mark.font == font, Modifier.weight(1f), family, FontWeight.Normal, FontStyle.Normal) {
                tools.editText { it.copy(font = font) }
            }
        }
    }
}

@Composable
private fun StyleChip(
    glyph: String,
    label: String,
    selected: Boolean,
    modifier: Modifier,
    family: FontFamily,
    weight: FontWeight,
    fontStyle: FontStyle,
    onClick: () -> Unit
) {
    val colors = FrameTheme.colors
    val tint = if (selected) colors.primary else colors.onSurfaceVariant
    Box(
        modifier.heightIn(min = 40.dp).clip(FrameShapes.sm)
            .background(if (selected) colors.primary.copy(alpha = 0.18f) else colors.fieldFill)
            .border(1.dp, if (selected) colors.primary.copy(alpha = 0.45f) else Color.Transparent, FrameShapes.sm)
            .frameSelectable(selected, Role.Checkbox, onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        FrameText(glyph, style = FrameTheme.typography.body.copy(fontFamily = family, fontWeight = weight, fontStyle = fontStyle), color = tint)
    }
}

@Composable
private fun ColorTab(mark: TextMark, tools: ViewerToolEditor) {
    SwatchRow(mark.color, { c -> tools.editText { it.copy(color = c) } }) { tools.openPicker(ColorTarget.TextFill) }
    PrecisionRow(
        label = EditStrings.opacity(),
        valueText = "${(mark.opacity * 100f).roundToInt()}%",
        fraction = ((mark.opacity - MIN_OPACITY) / (1f - MIN_OPACITY)).coerceIn(0f, 1f),
        onFraction = { f -> tools.editText { it.copy(opacity = MIN_OPACITY + f * (1f - MIN_OPACITY)) } },
        onStep = { dir -> tools.editText { it.copy(opacity = (((it.opacity * 100f).roundToInt() + dir) / 100f).coerceIn(MIN_OPACITY, 1f)) } }
    ) { TextIcon(FrameIconsExtra.Contrast) }
}

@Composable
private fun EffectsTab(mark: TextMark, tools: ViewerToolEditor) {
    PrecisionRow(
        label = EditStrings.shadow(),
        valueText = "${(mark.shadow * 100f).roundToInt()}%",
        fraction = mark.shadow,
        onFraction = { f -> tools.editText { it.copy(shadow = f) } },
        onStep = { dir -> tools.editText { it.copy(shadow = (((it.shadow * 100f).roundToInt() + dir) / 100f).coerceIn(0f, 1f)) } }
    ) { TextIcon(FrameIconsExtra.Blur) }
    PrecisionRow(
        label = EditStrings.outline(),
        valueText = "${(mark.outline * 100f).roundToInt()}%",
        fraction = mark.outline,
        onFraction = { f -> tools.editText { it.copy(outline = f) } },
        onStep = { dir -> tools.editText { it.copy(outline = (((it.outline * 100f).roundToInt() + dir) / 100f).coerceIn(0f, 1f)) } }
    ) { TextIcon(FrameIconsExtra.RectSelect) }
    if (mark.outline > 0f) {
        FrameText(EditStrings.outlineColor(), style = FrameTheme.typography.label, color = FrameTheme.colors.onSurfaceVariant)
        SwatchRow(mark.outlineColor, { c -> tools.editText { it.copy(outlineColor = c) } }) { tools.openPicker(ColorTarget.TextOutline) }
    }
    FrameText(EditStrings.background(), style = FrameTheme.typography.label, color = FrameTheme.colors.onSurfaceVariant)
    val none = android.graphics.Color.alpha(mark.background) == 0
    val dark = mark.background == DARK_BACKGROUND
    val light = mark.background == LIGHT_BACKGROUND
    SegmentedChoice(
        listOf(
            ChoiceOption(FrameIconsExtra.Cancel, EditStrings.backgroundNone(), none) { tools.editText { it.copy(background = 0) } },
            ChoiceOption(FrameIconsExtra.Solid, EditStrings.backgroundDark(), dark) { tools.editText { it.copy(background = DARK_BACKGROUND) } },
            ChoiceOption(FrameIconsExtra.Contrast, EditStrings.backgroundLight(), light) { tools.editText { it.copy(background = LIGHT_BACKGROUND) } },
            ChoiceOption(FrameIconsExtra.Palette, EditStrings.backgroundCustom(), !none && !dark && !light) { tools.openPicker(ColorTarget.TextBackground) }
        )
    )
}

@Composable
private fun TextIcon(icon: Int) {
    Image(painterResource(icon), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(FrameTheme.colors.onSurfaceVariant))
}

@Composable
private fun TextGlyph() = TextIcon(FrameIconsExtra.Text)
