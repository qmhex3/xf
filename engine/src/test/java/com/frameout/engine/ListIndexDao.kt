package com.frameout.engine

import com.frameout.core.db.VideoIndexDao
import com.frameout.core.db.VideoIndexEntity
import java.lang.reflect.Proxy

internal object ListIndexDao {
    fun entry(index: Int, ptsUs: Long, keyframe: Boolean = false) = VideoIndexEntity(0L, 1L, index, ptsUs, keyframe, 0, 0L)

    fun steady(frames: Int, stepUs: Long = 33_333L, keyEvery: Int = 30) = (0 until frames).map { entry(it, it * stepUs, it % keyEvery == 0) }

    fun of(entries: List<VideoIndexEntity>): VideoIndexDao = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(VideoIndexDao::class.java)) { _, method, args ->
        val a = args ?: emptyArray()
        fun range(start: Long, end: Long) = entries.filter { it.ptsUs >= start && it.ptsUs < end }.sortedBy { it.sampleIndex }
        when (method.name) {
            "bySampleIndex" -> entries.firstOrNull { it.sampleIndex == (a[1] as Int) }
            "countRange" -> range(a[1] as Long, a[2] as Long).size
            "rangeOffset" -> range(a[1] as Long, a[2] as Long).sortedBy { it.ptsUs }.getOrNull(a[3] as Int)
            "pageRange" -> range(a[1] as Long, a[2] as Long).drop(a[4] as Int).take(a[3] as Int)
            "pageRangeAfter" -> range(a[1] as Long, a[2] as Long).filter { it.sampleIndex > (a[3] as Int) }.take(a[4] as Int)
            "firstAtOrAfter" -> range(a[1] as Long, a[2] as Long).filter { it.ptsUs >= (a[3] as Long) }.minWithOrNull(compareBy({ it.ptsUs }, { it.sampleIndex }))
            "firstAtOrAfterAfter" -> range(a[1] as Long, a[2] as Long).filter { it.sampleIndex > (a[4] as Int) && it.ptsUs >= (a[3] as Long) }.minWithOrNull(compareBy({ it.ptsUs }, { it.sampleIndex }))
            else -> error("Unexpected query ${method.name}")
        }
    } as VideoIndexDao
}
