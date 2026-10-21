package com.frameout.engine.decode

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri

internal class DecoderOrientation(val rotation: Int, val flipH: Boolean, val aspect: Float, val width: Int, val height: Int)

internal fun readOrientation(context: Context, uri: Uri, ex: MediaExtractor, track: Int, format: MediaFormat): DecoderOrientation {
    var rotation = VideoFormatInfo.rotation(format)
    var flipH = false
    val ordinal = videoOrdinal(ex, track)
    val container = Mp4Orientation.read(context, uri, ordinal)
    container?.takeIf { it.flipH }?.let {
        rotation = it.rotation
        flipH = true
    }
    val colr = container?.colr ?: if (container == null) MkvColour.read(context, uri, ordinal) else null
    colr?.let { ColorHints.apply(format, it) }
    runCatching { VuiColor.read(format) ?: Av1Color.read(format) }.getOrNull()?.let { ColorHints.applyVui(format, it) }
    val aspect = VideoFormatInfo.pixelAspect(format)
    val size = if (!format.containsKey(MediaFormat.KEY_WIDTH) || !format.containsKey(MediaFormat.KEY_HEIGHT)) 0 to 0
    else FrameOrientation.outputSize(format.getInteger(MediaFormat.KEY_WIDTH), format.getInteger(MediaFormat.KEY_HEIGHT), rotation, aspect)
    return DecoderOrientation(rotation, flipH, aspect, size.first, size.second)
}
