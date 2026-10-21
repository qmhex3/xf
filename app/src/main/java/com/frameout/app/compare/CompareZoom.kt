package com.frameout.app.compare

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.frameout.core.edit.Transform
import com.frameout.core.edit.ViewGeometry

private const val DOUBLE_TAP_ZOOM = 3f
private const val ZOOMED = 1.05f

@Stable
internal class CompareZoom {
    var t by mutableStateOf(Transform.Identity)
        private set
    var geo by mutableStateOf(ViewGeometry(1, 1, 1f, 1f))
        private set
    private var refSize = IntSize(1, 1)

    val zoomed: Boolean get() = t.scale > ZOOMED

    fun configure(imageW: Int, imageH: Int, size: IntSize) {
        if (size.width <= 0 || size.height <= 0) return
        val g = ViewGeometry(maxOf(1, imageW), maxOf(1, imageH), size.width.toFloat(), size.height.toFloat())
        if (g == geo) return
        geo = g
        refSize = size
        t = g.clamp(t)
    }

    fun local(size: IntSize): Transform = Transform(
        t.scale,
        t.x * size.width / refSize.width,
        t.y * size.height / refSize.height
    )

    fun gesture(centroid: Offset, pan: Offset, zoom: Float, size: IntSize) {
        if (size.width <= 0 || size.height <= 0) return
        val kx = refSize.width / size.width.toFloat()
        val ky = refSize.height / size.height.toFloat()
        t = geo.gesture(t, zoom, (centroid.x - size.width / 2f) * kx, (centroid.y - size.height / 2f) * ky, pan.x * kx, pan.y * ky)
    }

    fun set(next: Transform) {
        t = geo.clamp(next)
    }

    fun reset() {
        t = Transform.Identity
    }

    fun doubleTap(point: Offset, size: IntSize) {
        if (zoomed) reset() else gesture(point, Offset.Zero, DOUBLE_TAP_ZOOM, size)
    }
}

internal class PaneItem(
    val tag: String,
    val bitmap: android.graphics.Bitmap?,
    val title: String,
    val subtitle: String,
    val description: String
)

@Stable
internal class CompareZoomSet(val a: CompareZoom, private val b: CompareZoom, val linked: Boolean) {
    fun forB(): CompareZoom = if (linked) a else b
    val zoomed: Boolean get() = a.zoomed || (!linked && b.zoomed)
    fun reset() {
        a.reset()
        b.reset()
    }
}
