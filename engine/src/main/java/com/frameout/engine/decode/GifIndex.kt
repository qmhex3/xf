package com.frameout.engine.decode

internal class GifFrameInfo(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val interlaced: Boolean,
    val palette: ByteArray?,
    val transparent: Int,
    val dispose: Int,
    val dataOffset: Long,
    val dataLength: Int
)

internal class GifIndex(val width: Int, val height: Int, val globalPalette: ByteArray?, val frames: List<GifFrameInfo>) {
    fun resetPoints(): IntArray = frames.indices.filter { it == 0 || isOpaqueFull(frames[it]) }.toIntArray()

    private fun isOpaqueFull(frame: GifFrameInfo): Boolean =
        frame.x == 0 && frame.y == 0 && frame.width >= width && frame.height >= height &&
            frame.transparent < 0 && frame.dispose != DISPOSE_PREVIOUS

    companion object {
        const val DISPOSE_BACKGROUND = 2
        const val DISPOSE_PREVIOUS = 3
    }
}
