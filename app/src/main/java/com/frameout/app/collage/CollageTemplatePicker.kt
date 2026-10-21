package com.frameout.app.collage

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.collage.CollageTemplate
import com.frameout.core.collage.CollageTemplates
import com.frameout.core.collage.toPx
import com.frameout.core.util.CollageStrings
import com.frameout.app.billing.ProBadge
import com.frameout.app.billing.rememberProGate
import com.frameout.core.pro.ProCatalog
import com.frameout.core.pro.ProFeature
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

@Composable
fun TemplatePicker(
    selected: CollageTemplate,
    frameCount: Int,
    listState: LazyListState,
    onSelect: (CollageTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    val gate = rememberProGate()
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
    ) {
        items(CollageTemplates.all, key = { it.id }) { t ->
            TemplateTile(t, t.id == selected.id, t.cells.size >= frameCount, gate.locked(ProCatalog.collageIsFree(t.id))) { gate.gated(ProFeature.Collage, ProCatalog.collageIsFree(t.id), t.id) { onSelect(t) } }
        }
    }
}

@Composable
private fun TemplateTile(template: CollageTemplate, active: Boolean, fits: Boolean, locked: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val border by animateColorAsState(if (active) colors.primary else frameHairline(), label = "tplBorder")
    val fill by animateColorAsState(if (active) colors.primaryContainer else colors.container, label = "tplFill")
    val name = CollageStrings.templateName(template.id)
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(FrameShapes.sm)
            .background(fill)
            .border(if (active) 1.5.dp else 1.dp, border, FrameShapes.sm)
            .frameSelectable(active, Role.RadioButton, onClick)
            .semantics { contentDescription = name }
            .padding(top = 10.dp, bottom = 8.dp, start = 6.dp, end = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TemplateGlyph(template, if (active) colors.primary else colors.onSurfaceVariant, Modifier.size(34.dp))
        FrameText(
            name,
            style = FrameTheme.typography.label.copy(fontSize = 11.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium),
            color = if (active) colors.primary else if (fits) colors.onSurface else colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
        if (locked) ProBadge()
    }
}

@Composable
private fun TemplateGlyph(template: CollageTemplate, tint: Color, modifier: Modifier) {
    Canvas(modifier) {
        val aspect = template.aspect
        val w = if (aspect >= 1f) size.minDimension else size.minDimension * aspect
        val h = if (aspect >= 1f) size.minDimension / aspect else size.minDimension
        val origin = Offset((size.width - w) / 2f, (size.height - h) / 2f)
        template.cells.forEach { c ->
            val r = c.toPx(w, h, 1.6.dp.toPx())
            val radius = minOf(3.dp.toPx(), r.width / 3f, r.height / 3f)
            drawRoundRect(tint.copy(alpha = 0.9f), Offset(origin.x + r.left, origin.y + r.top), Size(r.width, r.height), CornerRadius(radius))
        }
    }
}
