package com.frameout.core.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedImageProbeTest {
    private fun header(flags: Int, fourcc: String = "VP8X"): ByteArray {
        val head = ByteArray(32)
        "RIFF".forEachIndexed { i, c -> head[i] = c.code.toByte() }
        "WEBP".forEachIndexed { i, c -> head[8 + i] = c.code.toByte() }
        fourcc.forEachIndexed { i, c -> head[12 + i] = c.code.toByte() }
        head[20] = flags.toByte()
        return head
    }

    @Test
    fun animationFlagMakesWebpAnimated() {
        assertTrue(AnimatedImageProbe.webpHeaderAnimated(header(0x02), 32))
        assertTrue(AnimatedImageProbe.webpHeaderAnimated(header(0x12), 32))
    }

    @Test
    fun staticAndSimpleWebpAreNotAnimated() {
        assertFalse(AnimatedImageProbe.webpHeaderAnimated(header(0x10), 32))
        assertFalse(AnimatedImageProbe.webpHeaderAnimated(header(0x02, "VP8 "), 32))
        assertFalse(AnimatedImageProbe.webpHeaderAnimated(header(0x02), 10))
    }

    @Test
    fun candidatesAreWebpAndApngOnly() {
        assertTrue(AnimatedImageProbe.isCandidate("image/webp", "a.bin"))
        assertTrue(AnimatedImageProbe.isCandidate("image/png", "clip.APNG"))
        assertFalse(AnimatedImageProbe.isCandidate("image/jpeg", "a.jpg"))
        assertFalse(AnimatedImageProbe.isCandidate("image/png", "a.png"))
    }

    @Test
    fun pngCandidatesAreSeparateFromTheDefaultCandidates() {
        assertTrue(AnimatedImageProbe.isPngCandidate("image/png", "a.bin"))
        assertTrue(AnimatedImageProbe.isPngCandidate("", "sticker.PNG"))
        assertFalse(AnimatedImageProbe.isPngCandidate("image/jpeg", "a.jpg"))
        assertFalse(AnimatedImageProbe.isCandidate("image/png", "a.png"))
    }
}
