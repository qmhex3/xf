package com.frameout.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GeoPointTest {
    @Test
    fun parsesIso6709WithAltitude() {
        val point = GeoPoint.parseIso6709("+31.5000+035.0000+012.000/")!!
        assertEquals(31.5, point.latitude, 1e-9)
        assertEquals(35.0, point.longitude, 1e-9)
    }

    @Test
    fun parsesNegativeCoordinates() {
        val point = GeoPoint.parseIso6709("-33.8688+151.2093/")!!
        assertEquals(-33.8688, point.latitude, 1e-9)
        assertEquals(151.2093, point.longitude, 1e-9)
    }

    @Test
    fun rejectsMissingOrZeroLocation() {
        assertNull(GeoPoint.parseIso6709(null))
        assertNull(GeoPoint.parseIso6709("garbage"))
        assertNull(GeoPoint.parseIso6709("+0.0000+0.0000/"))
        assertNull(GeoPoint.parseIso6709("+95.0+10.0/"))
    }

    @Test
    fun parsesExifDegreesMinutesSeconds() {
        val point = GeoPoint.fromExif("31/1,30/1,0/1", "N", "35/1,0/1,0/1", "W")!!
        assertEquals(31.5, point.latitude, 1e-9)
        assertEquals(-35.0, point.longitude, 1e-9)
    }

    @Test
    fun exifRationalCarriesRoundedSecondsAcrossMinute() {
        val nearMinute = 1.0 + 59.99999995 / 3600.0
        assertEquals("1/1,1/1,0/10000", GeoPoint.exifRational(nearMinute))
    }

    @Test
    fun exifRationalRoundTrips() {
        val text = GeoPoint.exifRational(-33.8688)
        val back = GeoPoint.fromExif(text, "S", GeoPoint.exifRational(151.2093), "E")
        assertNotNull(back)
        assertEquals(-33.8688, back!!.latitude, 1e-5)
        assertEquals(151.2093, back.longitude, 1e-5)
    }
}
