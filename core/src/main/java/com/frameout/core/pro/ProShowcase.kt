package com.frameout.core.pro

import com.frameout.core.R
import com.frameout.core.edit.FilterPresets
import com.frameout.core.util.EditStrings
import com.frameout.core.util.StringSource

enum class ShowcaseKind { Accent, Filter, Shapes, Format, Smart, Export, Tool }

object ProShowcase {
    private const val SEPARATOR = "|"
    private const val HIDDEN_SHAPES = 14

    fun kindOf(feature: ProFeature): ShowcaseKind? = when (feature) {
        ProFeature.Accents -> ShowcaseKind.Accent
        ProFeature.Filters -> ShowcaseKind.Filter
        ProFeature.Shapes -> ShowcaseKind.Shapes
        ProFeature.ExportFormats -> ShowcaseKind.Format
        ProFeature.SmartExtract -> ShowcaseKind.Smart
        ProFeature.AdvancedExport -> ShowcaseKind.Export
        ProFeature.EditTools -> ShowcaseKind.Tool
        else -> null
    }

    fun hiddenCount(kind: ShowcaseKind): Int = if (kind == ShowcaseKind.Shapes) HIDDEN_SHAPES else 0

    fun parse(raw: String): List<String> = raw.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }

    fun proFilterIds(): List<String> = FilterPresets.All.map { it.id }.filter { !ProCatalog.filterIsFree(it) }

    fun includedNames(kind: ShowcaseKind): List<String> = when (kind) {
        ShowcaseKind.Filter -> proFilterIds().map { EditStrings.filterName(it) }
        ShowcaseKind.Accent -> raw(R.string.pro_inc_accents)
        ShowcaseKind.Shapes -> raw(R.string.pro_inc_shapes)
        ShowcaseKind.Format -> raw(R.string.pro_inc_formats)
        ShowcaseKind.Smart -> raw(R.string.pro_inc_smart)
        ShowcaseKind.Export -> raw(R.string.pro_inc_adv_export)
        ShowcaseKind.Tool -> raw(R.string.pro_inc_edit_tools)
    }

    fun title(): String = StringSource.get(R.string.pro_included)

    fun more(count: Int): String = StringSource.get(R.string.pro_included_more, count)

    fun caption(): String = StringSource.get(R.string.pro_demo_caption)

    fun tryHint(): String = StringSource.get(R.string.pro_try_hint)

    fun smartCount(): String = StringSource.get(R.string.pro_smart_count)

    fun saveSize(): String = StringSource.get(R.string.pro_save_size)

    private fun raw(id: Int): List<String> = parse(StringSource.get(id))
}
