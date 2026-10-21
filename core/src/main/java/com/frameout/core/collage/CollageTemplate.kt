package com.frameout.core.collage

data class CellRect(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float
)

data class PxRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

data class CollageTemplate(
    val id: String,
    val label: String,
    val columns: Int,
    val rows: Int,
    val cells: List<CellRect>,
    val aspectOverride: Float? = null
) {
    val aspect: Float get() = aspectOverride ?: (columns.toFloat() / rows)

    fun withAspect(choice: CollageAspect): CollageTemplate = copy(aspectOverride = choice.ratio)
}

const val COLLAGE_GAP_MAX = 80
const val COLLAGE_GAP_DEFAULT = 12

const val COLLAGE_REFERENCE_WIDTH = 1536f
const val COLLAGE_CORNER_REFERENCE = 30f

fun CellRect.toPx(outW: Float, outH: Float, gap: Float): PxRect {
    val eps = 0.001f
    val half = gap / 2f
    val l = x * outW + if (x < eps) gap else half
    val t = y * outH + if (y < eps) gap else half
    val r = (x + w) * outW - if (x + w > 1f - eps) gap else half
    val b = (y + h) * outH - if (y + h > 1f - eps) gap else half
    return PxRect(l, t, maxOf(r, l + 1f), maxOf(b, t + 1f))
}

object CollageTemplates {
    val all: List<CollageTemplate> = listOf(
        grid("2x1", 2, 1),
        grid("1x2", 1, 2),
        grid("2x2", 2, 2),
        grid("2x3", 2, 3),
        grid("3x3", 3, 3),
        grid("4x4", 4, 4),
        hero(),
        mosaic(),
        grid("film_strip", 1, 5),
        grid("contact_sheet", 4, 5)
    )

    fun suggest(count: Int): CollageTemplate {
        val n = count.coerceAtLeast(1)
        val fitting = all.filter { it.cells.size >= n }
        return fitting.minWithOrNull(compareBy({ it.cells.size }, { all.indexOf(it) }))
            ?: all.maxByOrNull { it.cells.size }!!
    }

    private fun hero(): CollageTemplate = CollageTemplate(
        "hero", "Hero", 3, 3, listOf(
            CellRect(0f, 0f, 1f, 2f / 3f),
            CellRect(0f, 2f / 3f, 1f / 3f, 1f / 3f),
            CellRect(1f / 3f, 2f / 3f, 1f / 3f, 1f / 3f),
            CellRect(2f / 3f, 2f / 3f, 1f / 3f, 1f / 3f)
        )
    )

    private fun mosaic(): CollageTemplate = CollageTemplate(
        "mosaic", "Mosaic", 3, 3, listOf(
            CellRect(0f, 0f, 2f / 3f, 2f / 3f),
            CellRect(2f / 3f, 0f, 1f / 3f, 1f / 3f),
            CellRect(2f / 3f, 1f / 3f, 1f / 3f, 1f / 3f),
            CellRect(0f, 2f / 3f, 1f / 3f, 1f / 3f),
            CellRect(1f / 3f, 2f / 3f, 1f / 3f, 1f / 3f),
            CellRect(2f / 3f, 2f / 3f, 1f / 3f, 1f / 3f)
        )
    )

    private fun grid(id: String, cols: Int, rows: Int): CollageTemplate {
        val cells = mutableListOf<CellRect>()
        val cw = 1f / cols
        val rh = 1f / rows
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                cells += CellRect(c * cw, r * rh, cw, rh)
            }
        }
        return CollageTemplate(id, id, cols, rows, cells)
    }
}

data class CollageConfig(
    val template: CollageTemplate = CollageTemplates.all.first(),
    val spacingPx: Int = COLLAGE_GAP_DEFAULT,
    val backgroundColor: Long = 0xFF000000,
    val outputWidth: Int = COLLAGE_DEFAULT_WIDTH,
    val cornerRadiusPx: Float = 0f,
    val timestamps: List<String> = emptyList(),
    val crops: List<CellCrop> = emptyList()
)
