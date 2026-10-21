package com.frameout.core.edit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

object BitmapEditRenderer {
    fun render(source: Bitmap, edit: FrameEdit): Bitmap {
        if (edit.isIdentity) return source
        val space = MarkSpace.of(source.width, source.height, edit)
        return mark(fused(source, edit), edit, space)
    }

    fun renderCopy(source: Bitmap, edit: FrameEdit): Bitmap {
        val work = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(work).drawBitmap(source, 0f, 0f, null)
        return render(work, edit)
    }

    private fun mark(source: Bitmap, edit: FrameEdit, space: MarkSpace): Bitmap {
        if (edit.marks.isEmpty()) return source
        val out = MarkRenderer.draw(source, edit.marks.map { space.toOutput(it) }, space.sourceMin)
        GainmapOps.copy(source, out)
        source.recycle()
        return out
    }

    private fun fused(source: Bitmap, edit: FrameEdit): Bitmap {
        val rect = cropPixels(source.width, source.height, edit.cropL, edit.cropT, edit.cropR, edit.cropB)
        val cropped = rect.left != 0 || rect.top != 0 || rect.width != source.width || rect.height != source.height
        val turned = edit.rotation != 0 || edit.flipH || edit.flipV
        if (!cropped && !turned && !edit.hasAdjust) return source
        val swap = edit.rotation % 180 != 0
        val width = if (swap) rect.height else rect.width
        val height = if (swap) rect.width else rect.height
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (edit.hasAdjust) {
            paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(AdjustMatrix.values(edit.brightness, edit.contrast, edit.saturation, edit.filter, edit.filterStrength)))
        }
        val matrix = android.graphics.Matrix().apply {
            postTranslate(-(rect.left + rect.width / 2f), -(rect.top + rect.height / 2f))
            postScale(if (edit.flipH) -1f else 1f, if (edit.flipV) -1f else 1f)
            postRotate(edit.rotation.toFloat())
            postTranslate(width / 2f, height / 2f)
        }
        Canvas(out).drawBitmap(source, matrix, paint)
        carryGainmap(source, out, rect, edit)
        source.recycle()
        return out
    }

    private fun carryGainmap(source: Bitmap, out: Bitmap, rect: PixelRect, edit: FrameEdit) {
        if (!GainmapOps.has(source)) return
        val region = Bitmap.createBitmap(source, rect.left, rect.top, rect.width, rect.height)
        GainmapOps.crop(source, region, rect.left, rect.top, rect.right, rect.bottom)
        if (edit.rotation == 0 && !edit.flipH && !edit.flipV) GainmapOps.copy(region, out) else GainmapOps.transform(region, out, edit.rotation, edit.flipH, edit.flipV)
        if (region !== source) region.recycle()
    }
}
