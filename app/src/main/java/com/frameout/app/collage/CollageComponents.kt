package com.frameout.app.collage

import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.a11y.frameClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frameout.core.collage.CollageTemplate
import com.frameout.core.collage.CollageTemplates
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill

@Composable
fun TemplatePicker(selected: CollageTemplate, onSelect: (CollageTemplate) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        items(CollageTemplates.all, key = { it.id }) { t ->
            val active = t.id == selected.id
            Box(
                modifier = Modifier
                    .background(
                        if (active) FrameTheme.colors.primary else FrameTheme.colors.container,
                        FrameShapes.pill
                    )
                    .frameSelectable(active, androidx.compose.ui.semantics.Role.RadioButton) { onSelect(t) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                FrameText(
                    t.label,
                    style = FrameTheme.typography.label,
                    color = if (active) FrameTheme.colors.onPrimary else FrameTheme.colors.onSurface
                )
            }
        }
    }
}

@Composable
fun PreviewGrid(template: CollageTemplate, filled: Int) {
    val colors = FrameTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(template.columns.toFloat() / template.rows)
            .background(colors.surface, FrameShapes.md)
            .padding(4.dp)
    ) {
        for (r in 0 until template.rows) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (c in 0 until template.columns) {
                    val i = r * template.columns + c
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(2.dp)
                            .background(colors.fieldFill, FrameShapes.xs)
                            .then(
                                if (i < filled) Modifier.border(1.dp, colors.primary, FrameShapes.xs)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (i < filled) {
                            FrameText("${i + 1}", style = FrameTheme.typography.label)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollageChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(FrameTheme.colors.fieldFill, FrameShapes.pill)
            .frameClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        FrameText(label, style = FrameTheme.typography.label)
    }
}
