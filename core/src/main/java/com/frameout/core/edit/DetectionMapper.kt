package com.frameout.core.edit

import com.frameout.core.quality.NormBox
import kotlin.math.max
import kotlin.math.min

object DetectionMapper {
    private const val MIN_VISIBLE = 0.35f

    fun map(box: NormBox, edit: FrameEdit): NormBox? {
        val cropped = crop(box, edit) ?: return null
        return transform(cropped, edit)
    }

    private fun crop(box: NormBox, edit: FrameEdit): NormBox? {
        val cw = edit.cropR - edit.cropL
        val ch = edit.cropB - edit.cropT
        if (cw <= 0f || ch <= 0f) return null
        val l = max(box.l, edit.cropL)
        val t = max(box.t, edit.cropT)
        val r = min(box.r, edit.cropR)
        val b = min(box.b, edit.cropB)
        if (r <= l || b <= t) return null
        val kept = (r - l) * (b - t) / (box.width * box.height)
        if (kept < MIN_VISIBLE) return null
        return NormBox((l - edit.cropL) / cw, (t - edit.cropT) / ch, (r - edit.cropL) / cw, (b - edit.cropT) / ch)
    }

    private fun transform(box: NormBox, edit: FrameEdit): NormBox {
        if (edit.rotation == 0 && !edit.flipH && !edit.flipV) return box
        val corners = listOf(box.l to box.t, box.r to box.t, box.l to box.b, box.r to box.b).map { (u0, v0) ->
            val u1 = if (edit.flipH) 1f - u0 else u0
            val v1 = if (edit.flipV) 1f - v0 else v0
            when (((edit.rotation % 360) + 360) % 360) {
                90 -> (1f - v1) to u1
                180 -> (1f - u1) to (1f - v1)
                270 -> v1 to (1f - u1)
                else -> u1 to v1
            }
        }
        return NormBox(corners.minOf { it.first }, corners.minOf { it.second }, corners.maxOf { it.first }, corners.maxOf { it.second })
    }
}
