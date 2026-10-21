package com.frameout.engine.decode

import java.io.ByteArrayOutputStream

internal object GifLzw {
    private const val MAX_CODES = 4096

    private class State(val minCode: Int) {
        val clear = 1 shl minCode
        val end = clear + 1
        val prefix = ShortArray(MAX_CODES)
        val suffix = ByteArray(MAX_CODES)
        var codeSize = minCode + 1
        var mask = (1 shl codeSize) - 1
        var next = clear + 2
        var previous = -1
        var first = 0

        fun reset() {
            codeSize = minCode + 1
            mask = (1 shl codeSize) - 1
            next = clear + 2
            previous = -1
        }
    }

    fun decode(data: ByteArray, pixelCount: Int): ByteArray {
        val out = ByteArray(pixelCount.coerceAtLeast(0))
        if (data.isEmpty() || out.isEmpty()) return out
        val minCode = data[0].toInt() and 0xFF
        if (minCode !in 2..8) return out
        run(joinBlocks(data), State(minCode), out)
        return out
    }

    private fun run(stream: ByteArray, state: State, out: ByteArray) {
        val stack = ByteArray(MAX_CODES + 1)
        var written = 0
        var buffer = 0
        var bits = 0
        var position = 0
        while (written < out.size) {
            while (bits < state.codeSize) {
                if (position >= stream.size) return
                buffer = buffer or ((stream[position++].toInt() and 0xFF) shl bits)
                bits += 8
            }
            val code = buffer and state.mask
            buffer = buffer ushr state.codeSize
            bits -= state.codeSize
            if (code == state.clear) {
                state.reset()
                continue
            }
            if (code == state.end || code > state.next) return
            written = emit(state, code, stack, out, written)
        }
    }

    private fun emit(state: State, code: Int, stack: ByteArray, out: ByteArray, written: Int): Int {
        if (state.previous < 0) {
            if (code >= state.clear) return out.size
            out[written] = code.toByte()
            state.previous = code
            state.first = code
            return written + 1
        }
        var depth = 0
        var current = code
        if (code >= state.next) {
            stack[depth++] = state.first.toByte()
            current = state.previous
        }
        while (current >= state.clear) {
            stack[depth++] = state.suffix[current]
            current = state.prefix[current].toInt()
        }
        state.first = current
        stack[depth++] = current.toByte()
        addEntry(state)
        var index = written
        while (depth > 0 && index < out.size) out[index++] = stack[--depth]
        state.previous = code
        return index
    }

    private fun addEntry(state: State) {
        if (state.next >= MAX_CODES) return
        state.prefix[state.next] = state.previous.toShort()
        state.suffix[state.next] = state.first.toByte()
        state.next++
        if (state.next > state.mask && state.codeSize < 12) {
            state.codeSize++
            state.mask = (1 shl state.codeSize) - 1
        }
    }

    private fun joinBlocks(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(data.size)
        var position = 1
        while (position < data.size) {
            val size = data[position++].toInt() and 0xFF
            if (size == 0) break
            val end = minOf(position + size, data.size)
            out.write(data, position, end - position)
            position = end
        }
        return out.toByteArray()
    }
}
