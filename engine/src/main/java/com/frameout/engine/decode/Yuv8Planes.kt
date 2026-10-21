package com.frameout.engine.decode

import android.graphics.Rect

internal class Yuv8Planes(
    val y: ByteArray,
    val u: ByteArray,
    val v: ByteArray,
    val yRowStride: Int,
    val yPixelStride: Int,
    val uRowStride: Int,
    val uPixelStride: Int,
    val vRowStride: Int,
    val vPixelStride: Int,
    val crop: Rect,
    val chromaWidth: Int,
    val chromaHeight: Int,
    val uBase: Int = 0,
    val vBase: Int = 0
)
