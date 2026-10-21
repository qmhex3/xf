package com.frameout.engine.extract

internal object GateFlags {
    private const val BLURRY = 1
    private const val DUPLICATES = 2
    private const val PEOPLE = 4
    private const val BODY = 8
    private const val STORED = 16

    fun encode(blurry: Boolean, duplicates: Boolean, people: Boolean, body: Boolean): Int =
        STORED or (if (blurry) BLURRY else 0) or (if (duplicates) DUPLICATES else 0) or (if (people) PEOPLE else 0) or (if (body) BODY else 0)

    fun isStored(flags: Int): Boolean = flags and STORED != 0
    fun blurry(flags: Int): Boolean = flags and BLURRY != 0
    fun duplicates(flags: Int): Boolean = flags and DUPLICATES != 0
    fun people(flags: Int): Boolean = flags and PEOPLE != 0
    fun body(flags: Int): Boolean = flags and BODY != 0
}
