package com.frameout.core.storage

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

data class GeoPoint(val latitude: Double, val longitude: Double) {
    val valid: Boolean
        get() = latitude in -90.0..90.0 && longitude in -180.0..180.0 && !(latitude == 0.0 && longitude == 0.0)

    companion object {
        private val iso6709 = Regex("^\\s*([+-]\\d+(?:\\.\\d+)?)([+-]\\d+(?:\\.\\d+)?)")

        fun parseIso6709(text: String?): GeoPoint? {
            val match = iso6709.find(text.orEmpty()) ?: return null
            val lat = match.groupValues[1].toDoubleOrNull() ?: return null
            val lon = match.groupValues[2].toDoubleOrNull() ?: return null
            return GeoPoint(lat, lon).takeIf { it.valid }
        }

        fun fromExif(lat: String?, latRef: String?, lon: String?, lonRef: String?): GeoPoint? {
            val la = dms(lat) ?: return null
            val lo = dms(lon) ?: return null
            val signedLat = if (latRef.equals("S", true)) -la else la
            val signedLon = if (lonRef.equals("W", true)) -lo else lo
            return GeoPoint(signedLat, signedLon).takeIf { it.valid }
        }

        private fun dms(text: String?): Double? {
            val parts = text?.split(',')?.map { it.trim() } ?: return null
            if (parts.isEmpty() || parts.size > 3) return null
            var total = 0.0
            var divisor = 1.0
            for (part in parts) {
                val value = rational(part) ?: return null
                total += value / divisor
                divisor *= 60.0
            }
            return total
        }

        private fun rational(text: String): Double? {
            val pieces = text.split('/')
            val numerator = pieces[0].toDoubleOrNull() ?: return null
            if (pieces.size == 1) return numerator
            val denominator = pieces[1].toDoubleOrNull() ?: return null
            return if (denominator == 0.0) null else numerator / denominator
        }

        fun exifRational(value: Double): String {
            val parts = exifRationals(value)
            return String.format(Locale.US, "%d/1,%d/1,%d/10000", parts[0], parts[1], parts[2])
        }

        internal fun exifRationals(value: Double): LongArray {
            var scaledSeconds = (abs(value) * 3600.0 * 10_000.0).roundToLong()
            val degrees = scaledSeconds / 36_000_000L
            scaledSeconds %= 36_000_000L
            val minutes = scaledSeconds / 600_000L
            val seconds = scaledSeconds % 600_000L
            return longArrayOf(degrees, minutes, seconds)
        }
    }
}
