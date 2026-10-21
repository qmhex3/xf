package com.frameout.core.edit

class FilterPreset(val id: String, val matrix: FloatArray) {
    val isNone: Boolean get() = id == FilterPresets.NONE
}

object FilterPresets {
    const val NONE = ""
    const val DEFAULT_STRENGTH = 1f

    val All: List<FilterPreset> = listOf(
        FilterPreset(NONE, ColorOps.identity()),
        FilterPreset("mono", ColorOps.saturation(0f)),
        FilterPreset("noir", ColorOps.chain(ColorOps.saturation(0f), ColorOps.contrast(1.45f, -0.05f))),
        FilterPreset("silver", ColorOps.chain(ColorOps.saturation(0f), ColorOps.contrast(0.88f, 0.06f), ColorOps.channels(0.98f, 1f, 1.03f))),
        FilterPreset(
            "sepia",
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        ),
        FilterPreset("vintage", ColorOps.chain(ColorOps.saturation(0.7f), ColorOps.channels(1.08f, 1f, 0.86f, 12f, 6f, 0f), ColorOps.contrast(0.9f, 0.02f))),
        FilterPreset("fade", ColorOps.chain(ColorOps.saturation(0.8f), ColorOps.contrast(0.84f, 0.05f))),
        FilterPreset("warm", ColorOps.chain(ColorOps.channels(1.1f, 1.02f, 0.88f), ColorOps.saturation(1.1f))),
        FilterPreset("cool", ColorOps.chain(ColorOps.channels(0.9f, 1f, 1.12f), ColorOps.saturation(1.05f))),
        FilterPreset("vivid", ColorOps.chain(ColorOps.saturation(1.55f), ColorOps.contrast(1.12f))),
        FilterPreset("dramatic", ColorOps.chain(ColorOps.saturation(0.85f), ColorOps.contrast(1.38f, -0.04f))),
        FilterPreset("cinema", ColorOps.chain(ColorOps.channels(1.1f, 1f, 0.92f, -6f, 2f, 12f), ColorOps.saturation(1.15f), ColorOps.contrast(1.15f))),
        FilterPreset("sunset", ColorOps.chain(ColorOps.channels(1.16f, 0.98f, 0.8f, 8f, 0f, -4f), ColorOps.saturation(1.2f))),
        FilterPreset("chrome", ColorOps.chain(ColorOps.saturation(1.25f), ColorOps.contrast(1.2f), ColorOps.channels(1f, 1.02f, 1.06f)))
    )

    private val byId: Map<String, FilterPreset> = All.associateBy { it.id }

    fun find(id: String): FilterPreset = byId[id] ?: All.first()

    fun matrix(id: String, strength: Float): FloatArray {
        val preset = find(id)
        if (preset.isNone) return ColorOps.identity()
        return ColorOps.blend(preset.matrix, if (strength.isNaN()) DEFAULT_STRENGTH else strength)
    }
}
