package com.frameout.app.collage

import android.graphics.Bitmap
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.frameout.core.collage.CellCrop
import com.frameout.core.collage.CropGeometry

private class CropHolder {
    var crop = CellCrop()
}

@Composable
fun Modifier.cropGestures(bitmap: Bitmap?, cellWidth: Float, cellHeight: Float, crop: CellCrop, onCrop: (CellCrop) -> Unit): Modifier {
    val holder = remember { CropHolder() }
    holder.crop = crop
    val onChange by rememberUpdatedState(onCrop)
    if (bitmap == null) return this
    return pointerInput(bitmap, cellWidth, cellHeight) {
        val geometry = CropGeometry(bitmap.width.toFloat(), bitmap.height.toFloat(), cellWidth, cellHeight)
        detectTransformGestures { centroid, pan, zoom, _ ->
            val next = geometry.transform(holder.crop, pan.x, pan.y, zoom, centroid.x, centroid.y)
            holder.crop = next
            onChange(next)
        }
    }
}
