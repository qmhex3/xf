package com.frameout.core.ads

class FakeAdStore : AdStore {
    private val longs = HashMap<String, Long>()
    private val ints = HashMap<String, Int>()

    override fun long(key: String, default: Long): Long = longs[key] ?: default
    override fun int(key: String, default: Int): Int = ints[key] ?: default
    override fun put(key: String, value: Long) { longs[key] = value }
    override fun put(key: String, value: Int) { ints[key] = value }
}
