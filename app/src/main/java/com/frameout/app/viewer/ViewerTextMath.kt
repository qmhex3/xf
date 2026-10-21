package com.frameout.app.viewer

import com.frameout.core.edit.EditMark

internal fun normalizeAngle(deg: Float): Float {
    var a = deg % 360f
    if (a > 180f) a -= 360f
    if (a < -180f) a += 360f
    return a
}

internal fun List<EditMark>.replaceAt(index: Int, mark: EditMark): List<EditMark> =
    mapIndexed { i, m -> if (i == index) mark else m }
