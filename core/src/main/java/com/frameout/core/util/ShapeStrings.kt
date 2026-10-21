package com.frameout.core.util

import com.frameout.core.R
import com.frameout.core.edit.ShapeKind

object ShapeStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun title() = s(R.string.shapes_title)
    fun fillNone() = s(R.string.shape_fill_none)
    fun fillSoft() = s(R.string.shape_fill_soft)
    fun dashed() = s(R.string.shape_dashed)
    fun duplicate() = s(R.string.shape_duplicate)

    fun name(kind: ShapeKind): String = s(
        when (kind) {
            ShapeKind.Arrow -> R.string.shape_arrow
            ShapeKind.TaperArrow -> R.string.shape_taper
            ShapeKind.CurvedArrow -> R.string.shape_curved
            ShapeKind.DotArrow -> R.string.shape_dot
            ShapeKind.DoubleArrow -> R.string.shape_double
            ShapeKind.Line -> R.string.shape_line
            ShapeKind.Measure -> R.string.shape_measure
            ShapeKind.Rect -> R.string.shape_rect
            ShapeKind.RoundRect -> R.string.shape_round_rect
            ShapeKind.Ellipse -> R.string.shape_ellipse
            ShapeKind.Triangle -> R.string.shape_triangle
            ShapeKind.Diamond -> R.string.shape_diamond
            ShapeKind.Star -> R.string.shape_star
            ShapeKind.Bracket -> R.string.shape_bracket
            ShapeKind.Highlighter -> R.string.shape_highlighter
            ShapeKind.Spotlight -> R.string.shape_spotlight
            ShapeKind.SpotlightRect -> R.string.shape_spotlight_rect
            ShapeKind.Magnifier -> R.string.shape_magnifier
            ShapeKind.Number -> R.string.shape_number
            ShapeKind.Callout -> R.string.shape_callout
            ShapeKind.Check -> R.string.shape_check
            ShapeKind.Cross -> R.string.shape_cross
        }
    )
}
