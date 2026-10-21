package com.frameout.engine.decode

object WebpFrameRiff {
    private const val VP8X_PAYLOAD = 10
    private const val ALPHA_FLAG = 0x10

    fun build(width: Int, height: Int, frameData: ByteArray): ByteArray {
        val padding = frameData.size and 1
        val body = 4 + 8 + VP8X_PAYLOAD + frameData.size + padding
        val out = ByteArray(8 + body)
        var at = 0
        at = put(out, at, "RIFF")
        at = putLe32(out, at, body)
        at = put(out, at, "WEBP")
        at = put(out, at, "VP8X")
        at = putLe32(out, at, VP8X_PAYLOAD)
        out[at] = ALPHA_FLAG.toByte()
        at += 4
        at = putLe24(out, at, width - 1)
        at = putLe24(out, at, height - 1)
        System.arraycopy(frameData, 0, out, at, frameData.size)
        return out
    }

    private fun put(out: ByteArray, at: Int, text: String): Int {
        text.forEachIndexed { i, c -> out[at + i] = c.code.toByte() }
        return at + text.length
    }

    private fun putLe24(out: ByteArray, at: Int, value: Int): Int {
        out[at] = value.toByte()
        out[at + 1] = (value ushr 8).toByte()
        out[at + 2] = (value ushr 16).toByte()
        return at + 3
    }

    private fun putLe32(out: ByteArray, at: Int, value: Int): Int {
        putLe24(out, at, value)
        out[at + 3] = (value ushr 24).toByte()
        return at + 4
    }
}
