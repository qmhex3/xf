package com.frameout.core.storage

import android.media.ExifInterface

internal fun applyExifTags(exif: ExifInterface, stamp: ExifStamp) {
    exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, ExifText.dateTime(stamp.takenAt))
    exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, ExifText.dateTime(stamp.takenAt))
    exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, ExifText.subSecond(stamp.takenAt))
    val text = ExifText.ascii(stamp.description)
    exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, text)
    exif.setAttribute(ExifInterface.TAG_USER_COMMENT, text)
    exif.setAttribute(ExifInterface.TAG_SOFTWARE, "Frame out")
    clearGps(exif)
    stamp.location?.takeIf { it.valid }?.let {
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, GeoPoint.exifRational(it.latitude))
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (it.latitude < 0) "S" else "N")
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, GeoPoint.exifRational(it.longitude))
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (it.longitude < 0) "W" else "E")
    }
    exif.saveAttributes()
}

private fun clearGps(exif: ExifInterface) {
    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null)
    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null)
    exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null)
    exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, null)
    exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, null)
    exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, null)
}
