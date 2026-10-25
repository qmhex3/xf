package com.frameout.core.ads

interface AdStore {
    fun long(key: String, default: Long = 0L): Long
    fun int(key: String, default: Int = 0): Int
    fun put(key: String, value: Long)
    fun put(key: String, value: Int)
}
