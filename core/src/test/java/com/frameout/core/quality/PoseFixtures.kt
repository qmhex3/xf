package com.frameout.core.quality

object PoseFixtures {
    private val STANDING = listOf(
        .50f to .12f, .52f to .10f, .53f to .10f, .54f to .10f, .48f to .10f, .47f to .10f, .46f to .10f, .56f to .12f, .44f to .12f,
        .52f to .16f, .48f to .16f, .60f to .28f, .40f to .28f, .64f to .42f, .36f to .42f, .66f to .55f, .34f to .55f, .67f to .58f,
        .33f to .58f, .66f to .58f, .34f to .58f, .65f to .57f, .35f to .57f, .56f to .55f, .44f to .55f, .57f to .75f, .43f to .75f,
        .57f to .92f, .43f to .92f, .58f to .95f, .42f to .95f, .60f to .97f, .40f to .97f
    )

    fun person(likelihood: Float = 0.95f, only: Set<Int>? = null): List<PoseLm> =
        STANDING.mapIndexed { type, (x, y) -> PoseLm(type, x, y, likelihood) }.filter { only == null || it.type in only }

    fun spreadOnACar(likelihood: Float = 0.95f): List<PoseLm> =
        List(33) { type -> PoseLm(type, 0.1f + 0.8f * type / 32f, 0.55f, likelihood) }
}
