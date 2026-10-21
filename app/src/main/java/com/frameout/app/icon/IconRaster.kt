package com.frameout.app.icon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat

object IconRaster {
    fun iconRes(context: Context, key: String): Int =
        context.resources.getIdentifier("ic_launcher_$key", "mipmap", context.packageName)

    fun render(context: Context, @DrawableRes res: Int, sizePx: Int): Bitmap? {
        val drawable = load(context, res) ?: return null
        return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).also {
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(Canvas(it))
        }
    }

    private fun load(context: Context, @DrawableRes res: Int): Drawable? =
        if (res == 0) null else ResourcesCompat.getDrawable(context.resources, res, context.theme)?.mutate()
}
