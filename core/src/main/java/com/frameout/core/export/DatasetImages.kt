package com.frameout.core.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

object DatasetImages {
    private val NATIVE = setOf("jpg", "jpeg", "png")
    private const val JPEG_QUALITY = 95

    fun usable(frame: StagedFrame): StagedFrame {
        if (frame.file.extension.lowercase(Locale.ROOT) in NATIVE) return frame
        val bitmap = BitmapFactory.decodeFile(frame.file.path) ?: throw IOException("Unable to decode ${frame.file.name}")
        val target = File(frame.file.parentFile, frame.file.nameWithoutExtension + ".jpg")
        try {
            FileOutputStream(target).use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)) }
        } finally {
            bitmap.recycle()
        }
        frame.file.delete()
        return StagedFrame(frame.name, target, frame.frameId, frame.frameIndex, frame.sequence, frame.ptsUs, frame.width, frame.height)
    }
}
