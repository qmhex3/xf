package com.frameout.engine.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

class LevelAccumulator(private val windowMs: Long) {
    private val levels = ArrayList<Float>()
    private var channels = 0
    private var floatSamples = false
    private var windowFrames = 0
    private var stride = 1
    private var sumSquares = 0.0
    private var counted = 0

    fun configure(sampleRate: Int, channelCount: Int, float: Boolean) {
        channels = channelCount
        floatSamples = float
        stride = (sampleRate / TARGET_RATE).coerceAtLeast(1)
        windowFrames = (sampleRate.toLong() * windowMs / 1000 / stride).toInt().coerceAtLeast(1)
    }

    fun feed(buffer: ByteBuffer, offset: Int, size: Int) {
        if (channels <= 0 || windowFrames <= 0) return
        val view = buffer.duplicate().order(ByteOrder.nativeOrder())
        view.position(offset).limit(offset + size)
        val bytes = if (floatSamples) 4 else 2
        val frames = size / (bytes * channels)
        var frame = 0
        while (frame < frames) {
            add(frameEnergy(view, offset + frame * bytes * channels, bytes))
            frame += stride
        }
    }

    fun startAt(timeUs: Long) {
        if (counted > 0) flush()
        val target = (timeUs / 1000 / windowMs).toInt()
        while (levels.size < target) levels.add(0f)
    }

    fun finish(): FloatArray {
        if (counted > 0) flush()
        return levels.toFloatArray()
    }

    private fun frameEnergy(view: ByteBuffer, at: Int, bytes: Int): Double {
        var total = 0.0
        for (c in 0 until channels) {
            val v = if (floatSamples) view.getFloat(at + c * bytes) else view.getShort(at + c * bytes) / SHORT_SCALE
            total += v * v
        }
        return total / channels
    }

    private fun add(energy: Double) {
        sumSquares += energy
        counted++
        if (counted >= windowFrames) flush()
    }

    private fun flush() {
        levels.add(Math.sqrt(sumSquares / counted).toFloat().coerceIn(0f, 1f))
        sumSquares = 0.0
        counted = 0
    }

    companion object {
        private const val TARGET_RATE = 12_000
        private const val SHORT_SCALE = 32768f
    }
}
