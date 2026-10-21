package com.frameout.engine.decode

import android.content.Context
import android.net.Uri
import android.system.Os

internal object MkvColour {
    private const val HEAD = 1 shl 20
    private const val SEGMENT = 0x18538067
    private const val TRACKS = 0x1654AE6B
    private const val TRACK_ENTRY = 0xAE
    private const val TRACK_TYPE = 0x83
    private const val VIDEO = 0xE0
    private const val COLOUR = 0x55B0
    private const val MATRIX = 0x55B1
    private const val RANGE = 0x55B9
    private const val TRANSFER = 0x55BA
    private const val PRIMARIES = 0x55BB
    private const val UNSPECIFIED = 2
    private const val RANGE_LIMITED = 1L
    private const val RANGE_FULL = 2L

    private class Fields {
        var range = 0L
        var matrix = UNSPECIFIED
        var transfer = UNSPECIFIED
        var primaries = UNSPECIFIED
    }

    private class Track {
        var video = false
        var colour: Fields? = null
    }

    private class State {
        val colours = ArrayList<Mp4Orientation.Colr?>()
        var track: Track? = null
        var colour: Fields? = null
        var done = false
    }

    fun read(context: Context, uri: Uri, videoOrdinal: Int): Mp4Orientation.Colr? = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val size = minOf(Os.fstat(pfd.fileDescriptor).st_size, HEAD.toLong()).toInt()
            val head = Mp4Orientation.pread(pfd.fileDescriptor, 0L, size) ?: return@use null
            parse(head).getOrNull(videoOrdinal)
        }
    }.getOrNull()

    fun parse(data: ByteArray): List<Mp4Orientation.Colr?> {
        val state = State()
        walk(data, 0, data.size, state)
        return state.colours
    }

    private fun walk(d: ByteArray, start: Int, end: Int, s: State) {
        var p = start
        while (p < end && !s.done) {
            val idLength = vintLength(d[p].toInt() and 0xFF)
            if (idLength !in 1..4 || p + idLength > end) return
            var id = 0
            for (k in 0 until idLength) id = (id shl 8) or (d[p + k].toInt() and 0xFF)
            val sizeAt = p + idLength
            if (sizeAt >= end) return
            val sizeLength = vintLength(d[sizeAt].toInt() and 0xFF)
            if (sizeLength !in 1..8 || sizeAt + sizeLength > end) return
            val marker = 0xFF shr sizeLength
            var size = (d[sizeAt].toInt() and marker).toLong()
            var unknown = size == marker.toLong()
            for (k in 1 until sizeLength) {
                val b = d[sizeAt + k].toInt() and 0xFF
                size = (size shl 8) or b.toLong()
                unknown = unknown && b == 0xFF
            }
            val body = sizeAt + sizeLength
            val stop = if (unknown) end else minOf(end.toLong(), body + size).toInt()
            element(d, id, body, stop, s)
            p = stop
        }
    }

    private fun element(d: ByteArray, id: Int, body: Int, stop: Int, s: State) {
        when (id) {
            SEGMENT, VIDEO -> walk(d, body, stop, s)
            TRACKS -> {
                walk(d, body, stop, s)
                s.done = true
            }
            TRACK_ENTRY -> {
                s.track = Track()
                walk(d, body, stop, s)
                s.track?.takeIf { it.video }?.let { s.colours.add(it.colour?.let(::toColr)) }
                s.track = null
            }
            COLOUR -> {
                s.colour = Fields()
                walk(d, body, stop, s)
                s.track?.colour = s.colour
                s.colour = null
            }
            TRACK_TYPE -> if (number(d, body, stop) == 1L) s.track?.video = true
            RANGE -> s.colour?.range = number(d, body, stop)
            MATRIX -> s.colour?.matrix = number(d, body, stop).toInt()
            TRANSFER -> s.colour?.transfer = number(d, body, stop).toInt()
            PRIMARIES -> s.colour?.primaries = number(d, body, stop).toInt()
        }
    }

    private fun toColr(f: Fields): Mp4Orientation.Colr =
        Mp4Orientation.Colr(f.primaries, f.transfer, f.matrix, f.range == RANGE_FULL, f.range == RANGE_LIMITED || f.range == RANGE_FULL)

    private fun vintLength(first: Int): Int = if (first == 0) 0 else Integer.numberOfLeadingZeros(first) - 23

    private fun number(d: ByteArray, from: Int, to: Int): Long {
        var value = 0L
        for (k in from until minOf(to, from + 8)) value = (value shl 8) or (d[k].toLong() and 0xFF)
        return value
    }
}
