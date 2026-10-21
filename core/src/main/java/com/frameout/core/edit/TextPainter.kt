package com.frameout.core.edit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

object TextPainter {
    private const val PAD = 0.28f

    data class Size(val width: Float, val height: Float)

    fun sizePx(mark: TextMark, w: Float, h: Float): Float = (mark.size * min(w, h)).coerceAtLeast(1f)

    fun boxSize(mark: TextMark, w: Float, h: Float): Size {
        val px = sizePx(mark, w, h)
        val pad = px * PAD
        if (mark.blank) return Size(px * 2f + pad * 2f, px * 1.2f + pad * 2f)
        val layout = layoutOf(mark.text, paint(mark, px))
        return Size(layout.width + pad * 2f, layout.height + pad * 2f)
    }

    fun draw(canvas: Canvas, mark: TextMark, w: Float, h: Float) {
        if (mark.blank) return
        val px = sizePx(mark, w, h)
        val pad = px * PAD
        val opacity = mark.opacity.coerceIn(0f, 1f)
        val fill = paint(mark, px)
        val layout = layoutOf(mark.text, fill)
        val lw = layout.width.toFloat()
        val lh = layout.height.toFloat()
        canvas.save()
        canvas.translate(mark.x * w, mark.y * h)
        canvas.rotate(mark.rotation)
        if (Color.alpha(mark.background) > 0) {
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withOpacity(mark.background, opacity) }
            canvas.drawRoundRect(-lw / 2f - pad, -lh / 2f - pad, lw / 2f + pad, lh / 2f + pad, pad, pad, bg)
        }
        canvas.translate(-lw / 2f, -lh / 2f)
        val shadowed = mark.shadow > 0f
        if (mark.outline > 0f) {
            val stroke = paint(mark, px).apply {
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeWidth = px * 0.14f * mark.outline.coerceIn(0f, 1f)
                color = withOpacity(mark.outlineColor, opacity)
            }
            if (shadowed) applyShadow(stroke, mark, px)
            layoutOf(mark.text, stroke).draw(canvas)
        } else if (shadowed) {
            applyShadow(fill, mark, px)
        }
        fill.color = withOpacity(mark.color, opacity)
        layoutOf(mark.text, fill).draw(canvas)
        canvas.restore()
    }

    private fun applyShadow(paint: TextPaint, mark: TextMark, px: Float) {
        val s = mark.shadow.coerceIn(0f, 1f)
        val alpha = ((0.35f + 0.55f * s) * 255f).roundToInt()
        paint.setShadowLayer(px * (0.02f + 0.14f * s), px * 0.06f * s, px * 0.06f * s, Color.argb(alpha, 0, 0, 0))
    }

    private fun withOpacity(color: Int, opacity: Float): Int =
        Color.argb((Color.alpha(color) * opacity).roundToInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    private fun paint(mark: TextMark, px: Float): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = px
        color = mark.color
        typeface = Typeface.create(family(mark.font), style(mark))
    }

    private fun family(font: Int): Typeface = when (font) {
        TextMark.FONT_SERIF -> Typeface.SERIF
        TextMark.FONT_MONO -> Typeface.MONOSPACE
        TextMark.FONT_CURSIVE -> Typeface.create("cursive", Typeface.NORMAL)
        else -> Typeface.DEFAULT
    }

    private fun style(mark: TextMark): Int = when {
        mark.bold && mark.italic -> Typeface.BOLD_ITALIC
        mark.bold -> Typeface.BOLD
        mark.italic -> Typeface.ITALIC
        else -> Typeface.NORMAL
    }

    private fun layoutOf(text: String, paint: TextPaint): StaticLayout {
        val widest = text.split('\n').maxOf { paint.measureText(it) }
        val width = ceil(widest).toInt().coerceAtLeast(1)
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .build()
    }
}
