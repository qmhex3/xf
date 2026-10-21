package com.frameout.engine.decode

import android.media.MediaFormat
import java.nio.ByteBuffer

internal object VuiColor {
    class Signal(val fullRange: Boolean, val hasDescription: Boolean, val primaries: Int, val transfer: Int, val matrix: Int)

    internal class Truncated : RuntimeException()

    fun read(format: MediaFormat): Signal? {
        val mime = if (format.containsKey(MediaFormat.KEY_MIME)) format.getString(MediaFormat.KEY_MIME) else null
        val hevc = mime == MediaFormat.MIMETYPE_VIDEO_HEVC
        if (!hevc && mime != MediaFormat.MIMETYPE_VIDEO_AVC) return null
        for (key in CSD_KEYS) {
            val buffer = if (format.containsKey(key)) format.getByteBuffer(key) else null
            val bytes = buffer?.let { copy(it) } ?: continue
            val signal = parse(bytes, hevc)
            if (signal != null) return signal
        }
        return null
    }

    fun parse(annexB: ByteArray, hevc: Boolean): Signal? {
        var i = 0
        while (i + 3 < annexB.size) {
            if (annexB[i].toInt() == 0 && annexB[i + 1].toInt() == 0 && annexB[i + 2].toInt() == 1) {
                val start = i + 3
                var end = start
                while (end + 2 < annexB.size && !(annexB[end].toInt() == 0 && annexB[end + 1].toInt() == 0 && (annexB[end + 2].toInt() == 1 || annexB[end + 2].toInt() == 0))) end++
                if (end + 2 >= annexB.size) end = annexB.size
                if (start < end) {
                    val header = annexB[start].toInt() and 0xFF
                    val isSps = if (hevc) ((header shr 1) and 0x3F) == 33 else (header and 0x1F) == 7
                    if (isSps) {
                        val payload = unescape(annexB, start + if (hevc) 2 else 1, end)
                        val signal = try {
                            if (hevc) hevcVui(Bits(payload)) else avcVui(Bits(payload))
                        } catch (_: Truncated) {
                            null
                        } catch (_: IndexOutOfBoundsException) {
                            null
                        }
                        if (signal != null) return signal
                    }
                }
                i = end
            } else {
                i++
            }
        }
        return null
    }

    internal fun copy(buffer: ByteBuffer): ByteArray {
        val view = buffer.duplicate()
        view.rewind()
        return ByteArray(view.remaining()).also { view.get(it) }
    }

    private fun unescape(data: ByteArray, from: Int, to: Int): ByteArray {
        if (to <= from) return ByteArray(0)
        val out = ByteArray(to - from)
        var n = 0
        var zeros = 0
        for (k in from until to) {
            val b = data[k].toInt() and 0xFF
            if (zeros >= 2 && b == 3) {
                zeros = 0
                continue
            }
            out[n++] = b.toByte()
            zeros = if (b == 0) zeros + 1 else 0
        }
        return out.copyOf(n)
    }

    internal class Bits(private val data: ByteArray) {
        private var pos = 0

        fun u(count: Int): Int {
            var value = 0
            repeat(count) { value = (value shl 1) or bit() }
            return value
        }

        fun bit(): Int {
            val byte = pos shr 3
            if (byte >= data.size) throw Truncated()
            val v = (data[byte].toInt() shr (7 - (pos and 7))) and 1
            pos++
            return v
        }

        fun flag(): Boolean = bit() == 1

        fun skip(count: Int) {
            pos += count
            if ((pos shr 3) > data.size) throw Truncated()
        }

        fun ue(): Int {
            var zeros = 0
            while (bit() == 0) {
                zeros++
                if (zeros > 31) throw Truncated()
            }
            return if (zeros == 0) 0 else ((1 shl zeros) - 1 + u(zeros))
        }

        fun se(): Int {
            val k = ue()
            return if (k and 1 == 1) (k + 1) / 2 else -(k / 2)
        }

        fun uvlc() {
            var zeros = 0
            while (bit() == 0) {
                zeros++
                if (zeros >= 32) return
            }
            skip(zeros)
        }
    }

    private fun avcVui(r: Bits): Signal? {
        val profile = r.u(8)
        r.skip(8)
        r.skip(8)
        r.ue()
        if (profile in HIGH_PROFILES) {
            val chroma = r.ue()
            if (chroma == 3) r.skip(1)
            r.ue()
            r.ue()
            r.skip(1)
            if (r.flag()) {
                val lists = if (chroma != 3) 8 else 12
                for (idx in 0 until lists) if (r.flag()) skipScalingList(r, if (idx < 6) 16 else 64)
            }
        }
        r.ue()
        val pocType = r.ue()
        if (pocType == 0) {
            r.ue()
        } else if (pocType == 1) {
            r.skip(1)
            r.se()
            r.se()
            val cycle = r.ue()
            repeat(cycle) { r.se() }
        }
        r.ue()
        r.skip(1)
        r.ue()
        r.ue()
        val frameMbsOnly = r.flag()
        if (!frameMbsOnly) r.skip(1)
        r.skip(1)
        if (r.flag()) repeat(4) { r.ue() }
        if (!r.flag()) return null
        return commonVui(r)
    }

    private fun skipScalingList(r: Bits, size: Int) {
        var last = 8
        var next = 8
        for (j in 0 until size) {
            if (next != 0) {
                next = (last + r.se() + 256) % 256
            }
            last = if (next == 0) last else next
        }
    }

    private fun commonVui(r: Bits): Signal? {
        if (r.flag()) {
            if (r.u(8) == 255) r.skip(32)
        }
        if (r.flag()) r.skip(1)
        if (!r.flag()) return null
        r.skip(3)
        val full = r.flag()
        if (!r.flag()) return Signal(full, false, 0, 0, 0)
        val primaries = r.u(8)
        val transfer = r.u(8)
        val matrix = r.u(8)
        return Signal(full, true, primaries, transfer, matrix)
    }

    private fun hevcVui(r: Bits): Signal? {
        r.skip(4)
        val maxSubLayersMinus1 = r.u(3)
        r.skip(1)
        profileTierLevel(r, maxSubLayersMinus1)
        r.ue()
        val chroma = r.ue()
        if (chroma == 3) r.skip(1)
        r.ue()
        r.ue()
        if (r.flag()) repeat(4) { r.ue() }
        r.ue()
        r.ue()
        val pocBits = r.ue() + 4
        val orderingInfo = r.flag()
        for (k in (if (orderingInfo) 0 else maxSubLayersMinus1)..maxSubLayersMinus1) repeat(3) { r.ue() }
        repeat(6) { r.ue() }
        if (r.flag() && r.flag()) skipHevcScalingLists(r)
        r.skip(2)
        if (r.flag()) {
            r.skip(8)
            r.ue()
            r.ue()
            r.skip(1)
        }
        val setCount = r.ue()
        val deltaPocs = IntArray(setCount.coerceAtLeast(1))
        for (idx in 0 until setCount) deltaPocs[idx] = shortTermRefPicSet(r, idx, deltaPocs)
        if (r.flag()) {
            val count = r.ue()
            repeat(count) {
                r.skip(pocBits)
                r.skip(1)
            }
        }
        r.skip(2)
        if (!r.flag()) return null
        return commonVui(r)
    }

    private fun profileTierLevel(r: Bits, maxSubLayersMinus1: Int) {
        r.skip(96)
        val profilePresent = BooleanArray(maxSubLayersMinus1)
        val levelPresent = BooleanArray(maxSubLayersMinus1)
        for (k in 0 until maxSubLayersMinus1) {
            profilePresent[k] = r.flag()
            levelPresent[k] = r.flag()
        }
        if (maxSubLayersMinus1 > 0) for (k in maxSubLayersMinus1 until 8) r.skip(2)
        for (k in 0 until maxSubLayersMinus1) {
            if (profilePresent[k]) r.skip(88)
            if (levelPresent[k]) r.skip(8)
        }
    }

    private fun skipHevcScalingLists(r: Bits) {
        for (sizeId in 0 until 4) {
            var matrixId = 0
            while (matrixId < 6) {
                if (!r.flag()) {
                    r.ue()
                } else {
                    val coefficients = minOf(64, 1 shl (4 + (sizeId shl 1)))
                    if (sizeId > 1) r.se()
                    repeat(coefficients) { r.se() }
                }
                matrixId += if (sizeId == 3) 3 else 1
            }
        }
    }

    private fun shortTermRefPicSet(r: Bits, idx: Int, deltaPocs: IntArray): Int {
        val predicted = idx != 0 && r.flag()
        if (predicted) {
            r.skip(1)
            r.ue()
            val reference = deltaPocs[idx - 1]
            var used = 0
            for (j in 0..reference) {
                val usedByCurrent = r.flag()
                val useDelta = if (!usedByCurrent) r.flag() else true
                if (usedByCurrent || useDelta) used++
            }
            return used
        }
        val negative = r.ue()
        val positive = r.ue()
        repeat(negative + positive) {
            r.ue()
            r.skip(1)
        }
        return negative + positive
    }

    private val CSD_KEYS = arrayOf("csd-0", "csd-1")
    private val HIGH_PROFILES = setOf(100, 110, 122, 244, 44, 83, 86, 118, 128, 138, 139, 134, 135)
}
