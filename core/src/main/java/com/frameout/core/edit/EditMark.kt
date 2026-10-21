package com.frameout.core.edit

enum class CoverStyle { Blur, Pixelate, Solid }

const val DEFAULT_COVER_STRENGTH = 0.8f

data class MarkPoint(val x: Float, val y: Float)

sealed interface EditMark

data class StrokeMark(val color: Int, val width: Float, val points: List<MarkPoint>) : EditMark

data class CoverMark(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val style: CoverStyle,
    val strength: Float = DEFAULT_COVER_STRENGTH
) : EditMark

data class BrushCoverMark(
    val style: CoverStyle,
    val strength: Float,
    val width: Float,
    val points: List<MarkPoint>
) : EditMark

enum class ShapeFill { None, Soft, Solid }

enum class ShapeKind {
    Arrow, TaperArrow, CurvedArrow, DotArrow, DoubleArrow, Line, Measure,
    Rect, RoundRect, Ellipse, Triangle, Diamond, Star, Bracket,
    Highlighter, Spotlight, SpotlightRect, Magnifier,
    Number, Callout, Check, Cross;

    val pointBased: Boolean
        get() = this == Arrow || this == TaperArrow || this == CurvedArrow || this == DotArrow ||
            this == DoubleArrow || this == Line || this == Measure

    val radial: Boolean
        get() = this == Number || this == Check || this == Cross || this == Magnifier || this == Spotlight

    val marker: Boolean
        get() = this == Number || this == Check || this == Cross

    val supportsFill: Boolean
        get() = this == Rect || this == RoundRect || this == Ellipse || this == Triangle ||
            this == Diamond || this == Star || this == Callout || this == Check || this == Cross

    val supportsDash: Boolean
        get() = !(this == TaperArrow || this == Highlighter || this == Number || this == Check || this == Cross)
}

data class ShapeMark(
    val kind: ShapeKind,
    val color: Int,
    val width: Float,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val fill: ShapeFill = ShapeFill.None,
    val dash: Boolean = false,
    val opacity: Float = 1f,
    val number: Int = 1
) : EditMark {
    companion object {
        const val MIN_WIDTH = 0.002f
        const val MAX_WIDTH = 0.03f
        const val DEFAULT_WIDTH = 0.007f
        const val MIN_OPACITY = 0.1f
        const val MARKER_SIZE = 0.07f
    }
}

data class TextMark(
    val text: String,
    val x: Float,
    val y: Float,
    val size: Float,
    val color: Int,
    val bold: Boolean = true,
    val italic: Boolean = false,
    val font: Int = FONT_SANS,
    val shadow: Float = 0f,
    val outline: Float = 0f,
    val outlineColor: Int = 0xFF000000.toInt(),
    val background: Int = 0,
    val opacity: Float = 1f,
    val rotation: Float = 0f
) : EditMark {
    val blank: Boolean get() = text.isBlank()

    companion object {
        const val FONT_SANS = 0
        const val FONT_SERIF = 1
        const val FONT_MONO = 2
        const val FONT_CURSIVE = 3
        const val MIN_SIZE = 0.015f
        const val MAX_SIZE = 0.4f

        val Default = TextMark(
            text = "", x = 0.5f, y = 0.5f, size = 0.07f,
            color = 0xFFFFFFFF.toInt(), bold = true, shadow = 0.5f
        )
    }
}
