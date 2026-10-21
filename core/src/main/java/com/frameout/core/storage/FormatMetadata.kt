package com.frameout.core.storage

internal object FormatMetadata {
    fun block(format: ImageFormat, stamp: ExifStamp): ByteArray? = when {
        format.modern -> ExifBlock.build(stamp)
        (format == ImageFormat.Png || format == ImageFormat.PngHdr16) && stamp.pngMetadata -> PngMetadata.chunks(stamp.takenAt, stamp.description, stamp.location)
        else -> null
    }
}
