package com.frameout.core.edit

import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.math.roundToInt

object MarkCodec {
    fun encode(marks: List<EditMark>): String = marks.joinToString("\n") { encodeOne(it) }

    fun decode(text: String): List<EditMark> {
        if (text.isBlank()) return emptyList()
        return text.lines().mapNotNull { line -> runCatching { decodeOne(line) }.getOrNull() }
    }

    private fun q(value: Float): Float = (value * 100000f).roundToInt() / 100000f

    private fun encodeOne(mark: EditMark): String = when (mark) {
        is StrokeMark -> "S|${mark.color}|${q(mark.width)}|" + mark.points.joinToString(";") { "${q(it.x)},${q(it.y)}" }
        is CoverMark -> "C|${mark.style.name}|${q(mark.left)},${q(mark.top)},${q(mark.right)},${q(mark.bottom)}|${q(mark.strength)}"
        is TextMark -> encodeText(mark)
        is ShapeMark -> encodeShape(mark)
        is BrushCoverMark -> "B|${mark.style.name}|${q(mark.strength)}|${q(mark.width)}|" + mark.points.joinToString(";") { "${q(it.x)},${q(it.y)}" }
    }

    private fun decodeOne(line: String): EditMark? {
        val parts = line.split('|')
        return when (parts[0]) {
            "S" -> decodeStroke(parts)
            "C" -> decodeCover(parts)
            "B" -> decodeBrush(parts)
            "T" -> decodeText(parts)
            "H" -> decodeShape(parts)
            else -> null
        }
    }

    private fun decodeStroke(parts: List<String>): EditMark? {
        val points = parts[3].split(';').filter { it.isNotEmpty() }.map { pair ->
            val xy = pair.split(',')
            MarkPoint(xy[0].toFloat(), xy[1].toFloat())
        }
        if (points.isEmpty()) return null
        return StrokeMark(parts[1].toInt(), parts[2].toFloat(), points)
    }

    private fun decodeCover(parts: List<String>): EditMark? {
        val v = parts[2].split(',').map { it.toFloat() }
        if (v.size != 4) return null
        val strength = parts.getOrNull(3)?.toFloatOrNull() ?: DEFAULT_COVER_STRENGTH
        return CoverMark(v[0], v[1], v[2], v[3], CoverStyle.valueOf(parts[1]), strength)
    }

    private fun decodeBrush(parts: List<String>): EditMark? {
        val points = parts[4].split(';').filter { it.isNotEmpty() }.map { pair ->
            val xy = pair.split(',')
            MarkPoint(xy[0].toFloat(), xy[1].toFloat())
        }
        if (points.isEmpty()) return null
        return BrushCoverMark(CoverStyle.valueOf(parts[1]), parts[2].toFloat(), parts[3].toFloat(), points)
    }

    private fun encodeText(m: TextMark): String {
        val flags = (if (m.bold) 1 else 0) or (if (m.italic) 2 else 0)
        return listOf(
            "T", URLEncoder.encode(m.text, "UTF-8"), q(m.x), q(m.y), q(m.size), m.color, flags, m.font,
            q(m.shadow), q(m.outline), m.outlineColor, m.background, q(m.opacity), q(m.rotation)
        ).joinToString("|")
    }

    private fun decodeText(p: List<String>): EditMark? {
        if (p.size < 14) return null
        val flags = p[6].toInt()
        return TextMark(
            text = URLDecoder.decode(p[1], "UTF-8"),
            x = p[2].toFloat(), y = p[3].toFloat(), size = p[4].toFloat(),
            color = p[5].toInt(), bold = flags and 1 != 0, italic = flags and 2 != 0, font = p[7].toInt(),
            shadow = p[8].toFloat(), outline = p[9].toFloat(), outlineColor = p[10].toInt(),
            background = p[11].toInt(), opacity = p[12].toFloat(), rotation = p[13].toFloat()
        )
    }

    private fun encodeShape(m: ShapeMark): String = listOf(
        "H", m.kind.name, m.color, q(m.width),
        "${q(m.x1)},${q(m.y1)},${q(m.x2)},${q(m.y2)}",
        m.fill.name, if (m.dash) 1 else 0, q(m.opacity), m.number
    ).joinToString("|")

    private fun decodeShape(p: List<String>): EditMark? {
        if (p.size < 9) return null
        val v = p[4].split(',').map { it.toFloat() }
        if (v.size != 4) return null
        return ShapeMark(
            kind = ShapeKind.valueOf(p[1]), color = p[2].toInt(), width = p[3].toFloat(),
            x1 = v[0], y1 = v[1], x2 = v[2], y2 = v[3],
            fill = ShapeFill.valueOf(p[5]), dash = p[6] == "1", opacity = p[7].toFloat(), number = p[8].toInt()
        )
    }
}
