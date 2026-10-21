package com.frameout.engine.decode

internal object ResetPoints {
    fun lastAtOrBefore(points: IntArray, index: Int): Int {
        var low = 0
        var high = points.size - 1
        var found = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (points[mid] <= index) {
                found = points[mid]
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return found
    }
}
