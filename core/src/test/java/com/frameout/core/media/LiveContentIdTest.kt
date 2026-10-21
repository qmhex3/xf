package com.frameout.core.media

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveContentIdTest {
    private val id = "F906FE4C-506A-4F85-B9BE-5F862ED06C90"

    private fun box(type: String, payload: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(ByteBuffer.allocate(4).putInt(payload.size + 8).array())
        out.write(type.toByteArray(Charsets.ISO_8859_1))
        out.write(payload)
        return out.toByteArray()
    }

    private fun int(v: Int) = ByteBuffer.allocate(4).putInt(v).array()

    private fun quickTime(value: String, otherKeys: List<String>): ByteArray {
        val keys = otherKeys + "com.apple.quicktime.content.identifier"
        val keyEntries = ByteArrayOutputStream()
        keys.forEach { keyEntries.write(box("mdta", it.toByteArray())) }
        val keysBox = box("keys", int(0) + int(keys.size) + keyEntries.toByteArray())
        val itemBytes = ByteArrayOutputStream()
        keys.forEachIndexed { i, key ->
            val text = if (i == keys.lastIndex) value else "x".repeat(key.length)
            val data = box("data", int(1) + int(0) + text.toByteArray())
            itemBytes.write(int(data.size + 8)); itemBytes.write(int(i + 1)); itemBytes.write(data)
        }
        val ilst = box("ilst", itemBytes.toByteArray())
        return ByteArray(40) + box("meta", keysBox + ilst)
    }

    @Test
    fun readsTheIdentifierFromTheQuickTimeKeysAndItemList() {
        val bytes = quickTime(id, listOf("com.apple.quicktime.still-image-time", "com.apple.quicktime.full-frame-rate-playback-intent"))
        assertEquals(id.lowercase(), LiveContentId.parse(bytes))
    }

    @Test
    fun readsTheIdentifierFromTheAppleMakerNote() {
        val body = ByteArrayOutputStream()
        val text = (id + "\u0000").toByteArray()
        val header = "Apple iOS\u0000".toByteArray() + byteArrayOf(0, 1) + "MM".toByteArray()
        val ifdSize = 2 + 12
        val valueOffset = header.size + ifdSize + 4
        val entry = ByteBuffer.allocate(14).putShort(1).putShort(MAKER_TAG.toShort()).putShort(2).putInt(text.size).putInt(valueOffset).array()
        body.write(header); body.write(entry); body.write(int(0)); body.write(text)
        val bytes = ByteArray(200) + body.toByteArray()
        assertEquals(id.lowercase(), LiveContentId.parse(bytes))
    }

    @Test
    fun returnsNothingWhenThereIsNoIdentifier() {
        assertNull(LiveContentId.parse(ByteArray(512)))
    }

    private companion object {
        const val MAKER_TAG = 0x0011
    }
}
