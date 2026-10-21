package com.frameout.core.export

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream

object MotionPhotoWriter {
    private const val XMP_HEADER = "http://ns.adobe.com/xap/1.0/\u0000"
    private const val MAX_SEGMENT = 65533

    fun write(jpeg: ByteArray, mp4: File, presentationUs: Long, out: OutputStream) {
        val tagged = withXmp(jpeg, xmp(mp4.length(), presentationUs))
        out.write(tagged)
        mp4.inputStream().use { it.copyTo(out) }
        out.flush()
    }

    internal fun xmp(videoLength: Long, presentationUs: Long): ByteArray {
        val text = buildString {
            append("<?xpacket begin=\"﻿\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>")
            append("<x:xmpmeta xmlns:x=\"adobe:ns:meta/\"><rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">")
            append("<rdf:Description rdf:about=\"\" xmlns:GCamera=\"http://ns.google.com/photos/1.0/camera/\" ")
            append("xmlns:Container=\"http://ns.google.com/photos/1.0/container/\" ")
            append("xmlns:Item=\"http://ns.google.com/photos/1.0/container/item/\" ")
            append("GCamera:MotionPhoto=\"1\" GCamera:MotionPhotoVersion=\"1\" GCamera:MotionPhotoPresentationTimestampUs=\"$presentationUs\" ")
            append("GCamera:MicroVideo=\"1\" GCamera:MicroVideoVersion=\"1\" GCamera:MicroVideoOffset=\"$videoLength\" ")
            append("GCamera:MicroVideoPresentationTimestampUs=\"$presentationUs\">")
            append("<Container:Directory><rdf:Seq>")
            append("<rdf:li rdf:parseType=\"Resource\"><Container:Item Item:Mime=\"image/jpeg\" Item:Semantic=\"Primary\" Item:Length=\"0\" Item:Padding=\"0\"/></rdf:li>")
            append("<rdf:li rdf:parseType=\"Resource\"><Container:Item Item:Mime=\"video/mp4\" Item:Semantic=\"MotionPhoto\" Item:Length=\"$videoLength\" Item:Padding=\"0\"/></rdf:li>")
            append("</rdf:Seq></Container:Directory></rdf:Description></rdf:RDF></x:xmpmeta><?xpacket end=\"w\"?>")
        }
        return text.toByteArray(Charsets.UTF_8)
    }

    internal fun withXmp(jpeg: ByteArray, xmp: ByteArray): ByteArray {
        require(jpeg.size > 4 && jpeg[0] == 0xFF.toByte() && jpeg[1] == 0xD8.toByte()) { "Not a JPEG" }
        val payload = XMP_HEADER.toByteArray(Charsets.ISO_8859_1) + xmp
        require(payload.size <= MAX_SEGMENT) { "XMP too large" }
        val at = insertionPoint(jpeg)
        val out = ByteArrayOutputStream(jpeg.size + payload.size + 4)
        out.write(jpeg, 0, at)
        out.write(0xFF)
        out.write(0xE1)
        out.write((payload.size + 2) shr 8)
        out.write((payload.size + 2) and 0xFF)
        out.write(payload)
        out.write(jpeg, at, jpeg.size - at)
        return out.toByteArray()
    }

    private fun insertionPoint(jpeg: ByteArray): Int {
        val hasApp0 = jpeg[2] == 0xFF.toByte() && jpeg[3] == 0xE0.toByte()
        if (!hasApp0 || jpeg.size < 6) return 2
        val length = ((jpeg[4].toInt() and 0xFF) shl 8) or (jpeg[5].toInt() and 0xFF)
        return (4 + length).coerceAtMost(jpeg.size)
    }
}
