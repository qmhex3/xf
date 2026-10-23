package com.frameout.core.util

import com.frameout.core.R
import com.frameout.core.collage.CollageAspect

object CollageStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)

    fun templateName(id: String): String = when (id) {
        "film_strip" -> s(R.string.collage_tpl_film_strip)
        "hero" -> s(R.string.collage_tpl_hero)
        "mosaic" -> s(R.string.collage_tpl_mosaic)
        "contact_sheet" -> s(R.string.collage_tpl_contact_sheet)
        else -> "\u202A" + id.replace('x', '\u00D7') + "\u202C"
    }

    fun layout() = s(R.string.collage_layout)
    fun spacing() = s(R.string.collage_spacing)
    fun background() = s(R.string.collage_background)
    fun corners() = s(R.string.collage_corners)
    fun time() = s(R.string.collage_time)
    fun on() = s(R.string.collage_on)
    fun off() = s(R.string.collage_off)
    fun saveCollage() = s(R.string.collage_save)
    fun preparing() = s(R.string.collage_preparing)
    fun hintSwap() = s(R.string.collage_hint_swap)
    fun hintSelected() = s(R.string.collage_hint_selected)
    fun overflow(fit: Int, total: Int) = s(R.string.collage_overflow, fit, total)
    fun emptySlot() = s(R.string.collage_empty_slot)
    fun frameN(n: Int) = s(R.string.collage_frame_n, n)
    fun skipped(missing: Int, total: Int) = s(R.string.collage_skipped, missing, total)
    fun noFrames() = s(R.string.collage_no_frames)
    fun pickFrame() = s(R.string.collage_pick_frame)
    fun hintAdjust() = s(R.string.collage_hint_adjust)
    fun aspect() = s(R.string.collage_aspect)
    fun resolution() = s(R.string.collage_resolution)
    fun tabFrames() = s(R.string.collage_tab_frames)
    fun tabStyle() = s(R.string.collage_tab_style)
    fun tabCanvas() = s(R.string.collage_tab_canvas)

    fun aspectName(key: String): String =
        if (key == CollageAspect.Template.key) s(R.string.collage_aspect_template) else "\u202A$key\u202C"

    fun resolutionSize(width: Int, height: Int): String = s(R.string.collage_resolution_size, "\u202A$width \u00D7 $height\u202C")

    fun resolutionName(width: Int): String = "\u202A$width\u202C"
}
