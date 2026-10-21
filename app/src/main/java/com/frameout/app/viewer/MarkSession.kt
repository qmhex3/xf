package com.frameout.app.viewer

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.frameout.core.edit.CoverMark
import com.frameout.core.edit.BrushCoverMark
import com.frameout.core.edit.EditMark
import com.frameout.core.edit.MarkPoint
import com.frameout.core.edit.StrokeMark
import com.frameout.core.edit.Transform
import com.frameout.core.edit.ViewGeometry
import kotlin.math.max
import kotlin.math.min

private const val MIN_SPACING_PX = 2f
private const val MIN_COVER = 0.001f

internal class HeldMark(val bitmap: Bitmap, val mark: EditMark)

internal class MarkSession {
    val points = mutableStateListOf<Offset>()
    var cursor by mutableStateOf<Offset?>(null)
    var held by mutableStateOf<HeldMark?>(null)

    fun begin(start: Offset) {
        points.clear()
        points.add(start)
        cursor = start
    }

    fun move(position: Offset, rectangle: Boolean) {
        cursor = position
        if (rectangle) {
            if (points.size > 1) points[1] = position else points.add(position)
            return
        }
        val last = points.lastOrNull()
        if (last == null || (position - last).getDistance() >= MIN_SPACING_PX) points.add(position)
    }

    fun cancel() {
        points.clear()
        cursor = null
    }

    fun finish(mark: EditMark?, bitmap: Bitmap, onMark: (EditMark) -> Unit) {
        if (mark != null) {
            onMark(mark)
            held = HeldMark(bitmap, mark)
        }
        cancel()
    }
}

internal fun buildMark(points: List<Offset>, geo: ViewGeometry, t: Transform, tool: EditTool, draft: EditDraft): EditMark? {
    if (points.isEmpty()) return null
    val norm = points.map { toNorm(it, geo, t) }
    return when {
        tool == EditTool.Draw -> StrokeMark(draft.color, draft.size, norm)
        draft.shape == CoverShape.Pen -> BrushCoverMark(draft.style, draft.strength, draft.coverSize, norm)
        else -> rectangleMark(norm.first(), norm.last(), draft)
    }
}

private fun toNorm(p: Offset, geo: ViewGeometry, t: Transform) = MarkPoint(
    geo.toNormX(p.x - geo.viewW / 2f, t).coerceIn(0f, 1f),
    geo.toNormY(p.y - geo.viewH / 2f, t).coerceIn(0f, 1f)
)

private fun rectangleMark(a: MarkPoint, b: MarkPoint, draft: EditDraft): EditMark? {
    val cover = CoverMark(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y), draft.style, draft.strength)
    val wide = cover.right - cover.left >= MIN_COVER
    val tall = cover.bottom - cover.top >= MIN_COVER
    return cover.takeIf { wide && tall }
}
