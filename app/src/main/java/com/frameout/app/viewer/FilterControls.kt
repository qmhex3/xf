package com.frameout.app.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frameout.core.edit.FilterPreset
import com.frameout.core.edit.FilterPresets
import com.frameout.core.util.EditStrings
import com.frameout.app.billing.rememberProGate
import com.frameout.core.pro.ProCatalog
import com.frameout.core.pro.ProFeature
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.app.billing.ProCorner
import com.frameout.app.billing.proDim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val THUMB_PX = 160
private val THUMB_SIZE = 68.dp

@Composable
internal fun FilterControls(state: ViewerState, tools: ViewerToolEditor) {
    val draft = state.draft
    val thumb by rememberFilterThumb(state.bitmap)
    val gate = rememberProGate()
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
    ) {
        items(FilterPresets.All, key = { it.id }) { preset ->
            FilterTile(preset, thumb, preset.id == draft.filter, gate.locked(ProCatalog.filterIsFree(preset.id))) { gate.gated(ProFeature.Filters, ProCatalog.filterIsFree(preset.id), preset.id) { tools.setFilter(preset.id) } }
        }
    }
    PrecisionRow(
        enabled = draft.filtered,
        label = EditStrings.filterStrength(),
        valueText = "${(draft.filterStrength * 100f).roundToInt()}%",
        fraction = draft.filterStrength,
        onFraction = tools::setFilterStrength,
        onStep = { dir -> tools.setFilterStrength((((draft.filterStrength * 100f).roundToInt() + dir) / 100f).coerceIn(0f, 1f)) }
    ) { RowIcon(FrameIconsExtra.PhotoFilter) }
}

@Composable
private fun FilterTile(preset: FilterPreset, thumb: ImageBitmap?, selected: Boolean, locked: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val name = EditStrings.filterName(preset.id)
    val filter = remember(preset.id) { if (preset.isNone) null else ColorFilter.colorMatrix(ColorMatrix(preset.matrix)) }
    Column(
        Modifier.width(THUMB_SIZE).clip(FrameShapes.sm).frameSelectable(selected, Role.RadioButton, onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val frame = Modifier.size(THUMB_SIZE).clip(FrameShapes.sm)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outline, FrameShapes.sm)
        Box(Modifier.proDim(locked)) {
            if (thumb != null) {
                Image(thumb, name, frame, contentScale = ContentScale.Crop, colorFilter = filter)
            } else {
                Box(frame)
            }
            ProCorner(locked, kotlin.math.abs(preset.id.hashCode()) % 5, compact = true)
        }
        FrameText(
            name,
            style = FrameTheme.typography.label,
            color = if (selected) colors.primary else colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(THUMB_SIZE)
        )
    }
}

@Composable
private fun rememberFilterThumb(source: Bitmap?) = produceState<ImageBitmap?>(null, source) {
    val src = source
    value = if (src == null) null else withContext(Dispatchers.Default) { scaledThumb(src) }
}

private fun scaledThumb(src: Bitmap): ImageBitmap? = runCatching {
    if (src.isRecycled) return@runCatching null
    val scale = THUMB_PX.toFloat() / maxOf(src.width, src.height)
    if (scale >= 1f) return@runCatching src.copy(Bitmap.Config.ARGB_8888, false)?.asImageBitmap()
    val w = (src.width * scale).roundToInt().coerceAtLeast(1)
    val h = (src.height * scale).roundToInt().coerceAtLeast(1)
    Bitmap.createScaledBitmap(src, w, h, true).asImageBitmap()
}.getOrNull()
