package com.frameout.app.trim

object JumpSizes {
    val options: List<Int> = listOf(5, 10, 15, 30, 60)
    const val DEFAULT = 10

    fun next(current: Int): Int = options.firstOrNull { it > current } ?: options.first()

    fun normalize(value: Int): Int = if (value in options) value else DEFAULT
}
