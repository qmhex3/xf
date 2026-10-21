package com.frameout.core.edit

import com.frameout.core.util.logged
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Gainmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import kotlin.math.ceil

object GainmapOps {
    fun has(bitmap: Bitmap): Boolean = Build.VERSION.SDK_INT >= 34 && !bitmap.isRecycled && Api34.has(bitmap)

    fun copy(from: Bitmap, to: Bitmap) {
        if (Build.VERSION.SDK_INT >= 34 && from !== to) Api34.copy(from, to)
    }

    fun crop(from: Bitmap, to: Bitmap, left: Int, top: Int, right: Int, bottom: Int) {
        if (Build.VERSION.SDK_INT >= 34 && from !== to) Api34.crop(from, to, left, top, right, bottom)
    }

    fun transform(from: Bitmap, to: Bitmap, rotation: Int, flipH: Boolean, flipV: Boolean) {
        if (Build.VERSION.SDK_INT >= 34 && from !== to) Api34.transform(from, to, rotation, flipH, flipV)
    }

    fun <T> keeping(bitmap: Bitmap, keep: Boolean, block: () -> T): T {
        if (keep || Build.VERSION.SDK_INT < 34 || !has(bitmap)) return block()
        return Api34.without(bitmap, block)
    }

    fun decodeFile(path: String): Bitmap? {
        if (Build.VERSION.SDK_INT >= 34) Api34.decode(path)?.let { return it }
        return BitmapFactory.decodeFile(path)
    }

    @RequiresApi(34)
    private object Api34 {
        fun has(bitmap: Bitmap): Boolean = bitmap.hasGainmap()

        fun <T> without(bitmap: Bitmap, block: () -> T): T {
            val gainmap = bitmap.gainmap
            val cleared = runCatching { bitmap.setGainmap(null) }.isSuccess
            try {
                return block()
            } finally {
                if (cleared) bitmap.setGainmap(gainmap)
            }
        }

        fun copy(from: Bitmap, to: Bitmap) {
            val gainmap = from.gainmap ?: return
            to.setGainmap(Gainmap(gainmap, gainmap.gainmapContents))
        }

        fun crop(from: Bitmap, to: Bitmap, left: Int, top: Int, right: Int, bottom: Int) {
            val gainmap = from.gainmap ?: return
            val contents = gainmap.gainmapContents
            val sx = contents.width / from.width.toFloat()
            val sy = contents.height / from.height.toFloat()
            val gl = (left * sx).toInt().coerceIn(0, contents.width - 1)
            val gt = (top * sy).toInt().coerceIn(0, contents.height - 1)
            val gr = ceil(right * sx).toInt().coerceIn(gl + 1, contents.width)
            val gb = ceil(bottom * sy).toInt().coerceIn(gt + 1, contents.height)
            to.setGainmap(Gainmap(gainmap, Bitmap.createBitmap(contents, gl, gt, gr - gl, gb - gt)))
        }

        fun transform(from: Bitmap, to: Bitmap, rotation: Int, flipH: Boolean, flipV: Boolean) {
            val gainmap = from.gainmap ?: return
            val contents = gainmap.gainmapContents
            val matrix = Matrix().apply {
                postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f)
                postRotate(rotation.toFloat())
            }
            val moved = Bitmap.createBitmap(contents, 0, 0, contents.width, contents.height, matrix, false)
            to.setGainmap(Gainmap(gainmap, moved))
        }

        fun decode(path: String): Bitmap? = runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(File(path))) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }.logged("GainmapOps").getOrNull()
    }
}
