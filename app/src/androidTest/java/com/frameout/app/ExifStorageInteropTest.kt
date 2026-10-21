package com.frameout.app

import android.graphics.Bitmap
import android.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.frameout.core.storage.ExifBlock
import com.frameout.core.storage.ExifStamp
import com.frameout.core.storage.ExifWriter
import com.frameout.core.storage.GeoPoint
import com.frameout.core.storage.ExifText
import com.frameout.core.storage.ImageFormat
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExifStorageInteropTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun builtExifBlockIsReadableByExifInterface() {
        val stamp = ExifStamp(1709622489007L, "Trip · #1 · 00:01.000", GeoPoint(31.5, 35.0), true)
        val block = ExifBlock.build(stamp)
        assertNotNull(block)
        val file = File.createTempFile("frameout-exif", ".jpg", context.cacheDir)
        try {
            file.writeBytes(injectExif(jpegBytes(), block!!))
            val exif = ExifInterface(file.absolutePath)
            assertEquals(ExifText.dateTime(stamp.takenAt), exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
            assertEquals(ExifText.ascii(stamp.description), exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION))
            assertEquals("N", exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF))
            assertEquals("E", exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF))
        } finally {
            file.delete()
        }
    }

    @Test
    fun exifWriterRemovesStaleGpsWhenLocationIsOmitted() {
        val file = File.createTempFile("frameout-gps", ".jpg", context.cacheDir)
        try {
            file.outputStream().use { out ->
                val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
                try { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)) } finally { bitmap.recycle() }
            }
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_GPS_LATITUDE, "31/1,30/1,0/1")
                setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
                setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "35/1,0/1,0/1")
                setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
                saveAttributes()
            }
            val stamp = ExifStamp(1709622489000L, "frame", null, false)
            assertTrue(ExifWriter(context).writeFile(file, ImageFormat.Jpg, stamp))
            val exif = ExifInterface(file.absolutePath)
            assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
            assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF))
            assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE))
            assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF))
            assertEquals(ExifText.ascii(stamp.description), exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION))
        } finally {
            file.delete()
        }
    }

    private fun jpegBytes(): ByteArray {
        val file = File.createTempFile("frameout-seed", ".jpg", context.cacheDir)
        try {
            FileOutputStream(file).use { out ->
                val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
                try { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)) } finally { bitmap.recycle() }
            }
            return file.readBytes()
        } finally {
            file.delete()
        }
    }

    private fun injectExif(jpeg: ByteArray, payload: ByteArray): ByteArray {
        require(jpeg.size >= 2 && jpeg[0] == 0xFF.toByte() && jpeg[1] == 0xD8.toByte())
        val length = payload.size + 2
        require(length <= 0xFFFF)
        val segment = byteArrayOf(0xFF.toByte(), 0xE1.toByte(), (length ushr 8).toByte(), length.toByte()) + payload
        return byteArrayOf(0xFF.toByte(), 0xD8.toByte()) + segment + jpeg.copyOfRange(2, jpeg.size)
    }
}
