package com.frameout.core.backup

import com.frameout.core.backup.BackupText.TAB

internal object BackupRows {
    fun format(f: BackupFrame): String {
        val head = listOf(f.index, f.ptsUs, flag(f.isKeyframe), flag(f.isFavorite), flag(f.isDeleted), flag(f.edit != null))
        val e = f.edit ?: return head.joinToString(TAB.toString())
        val tail = listOf(e.rotation, flag(e.flipH), flag(e.flipV), e.cropL, e.cropT, e.cropR, e.cropB, e.brightness, e.contrast, e.saturation, flag(e.isDeleted), BackupText.escape(e.marks), BackupText.escape(e.filter), e.filterStrength)
        return (head + tail).joinToString(TAB.toString())
    }

    fun parse(columns: Map<String, Int>, cells: List<String>): BackupFrame {
        fun cell(name: String): String = columns[name]?.let { cells.getOrNull(it) }.orEmpty()
        fun on(name: String): Boolean = cell(name) == "1"
        fun number(name: String, fallback: Float, min: Float = -MAX_ABS, max: Float = MAX_ABS): Float =
            cell(name).toFloatOrNull()?.takeIf { it.isFinite() }?.coerceIn(min, max) ?: fallback
        fun crop(l: Float, t: Float, r: Float, b: Float): FloatArray =
            if (r - l >= MIN_CROP && b - t >= MIN_CROP) floatArrayOf(l, t, r, b) else floatArrayOf(0f, 0f, 1f, 1f)
        val edit = if (on("has_edit")) {
            val box = crop(number("crop_l", 0f, 0f, 1f), number("crop_t", 0f, 0f, 1f), number("crop_r", 1f, 0f, 1f), number("crop_b", 1f, 0f, 1f))
            BackupEdit(
                rotation = rotation(cell("rot")),
                flipH = on("flip_h"),
                flipV = on("flip_v"),
                cropL = box[0],
                cropT = box[1],
                cropR = box[2],
                cropB = box[3],
                brightness = number("bright", 0f, -0.5f, 0.5f),
                contrast = number("contrast", 1f, 0.5f, 1.5f),
                saturation = number("sat", 1f, 0f, 2f),
                isDeleted = on("edit_del"),
                marks = BackupText.unescape(cell("marks")).take(MAX_MARKS_CHARS),
                filter = BackupText.unescape(cell("filter")).take(MAX_FILTER_CHARS),
                filterStrength = number("filter_str", 1f, 0f, 1f)
            )
        } else null
        val pts = cell("pts_us").toLong()
        if (pts !in 0L..MAX_PTS_US) throw BackupException(BackupException.Reason.Invalid)
        val index = cell("index").toInt()
        if (index < 0) throw BackupException(BackupException.Reason.Invalid)
        return BackupFrame(index, pts, on("key"), on("fav"), on("del"), edit)
    }

    private fun rotation(raw: String): Int {
        val degrees = raw.toIntOrNull() ?: return 0
        val normalized = ((degrees % 360) + 360) % 360
        return if (normalized % 90 == 0) normalized else 0
    }

    private const val MAX_ABS = 1_000_000f
    private const val MIN_CROP = 0.01f
    private const val MAX_MARKS_CHARS = 256 * 1024
    private const val MAX_FILTER_CHARS = 64

    private const val MAX_PTS_US = 366L * 24 * 60 * 60 * 1_000_000L

    private fun flag(value: Boolean): Int = if (value) 1 else 0
}
