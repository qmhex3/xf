package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameImportanceTest {
    private fun frame(i: Int, score: Double = 100.0, sig: ByteArray? = null) = ScoredFrame(i.toLong(), i, score, sig = sig)

    private fun hit(tag: String, confidence: Float = 0.9f) = TagHit(tag, confidence)

    private fun frames(n: Int, score: (Int) -> Double = { 100.0 }) = (0 until n).map { frame(it, score(it)) }

    private fun tags(n: Int, extra: (Int) -> List<TagHit> = { emptyList() }): Map<Long, List<TagHit>> =
        (0 until n).associate { it.toLong() to (listOf(hit("l:Room")) + extra(it)) }

    private fun cake(i: Int) = if (i in 20..22) listOf(hit("l:Cake")) else emptyList()

    @Test
    fun shortVideoFallsBackToTheSharpestPicker() {
        val short = frames(5)
        assertEquals(SharpestPicker.pick(short), FrameImportance.pick(short, tags(5)))
        assertNull(FrameImportance.scores(short, tags(5)))
    }

    @Test
    fun unscannedVideoFallsBackToTheSharpestPicker() {
        val all = frames(40)
        assertEquals(SharpestPicker.pick(all), FrameImportance.pick(all, emptyMap()))
        assertNull(FrameImportance.scores(all, emptyMap()))
    }

    @Test
    fun aRareTagThatPersistsOutranksCommonFramesAndIsPicked() {
        val all = frames(40)
        val scores = FrameImportance.scores(all, tags(40, ::cake))
        assertNotNull(scores)
        assertTrue(scores!![21] > scores[5])
        val picked = FrameImportance.pick(all, tags(40, ::cake))
        assertTrue(picked.any { it in 20L..22L })
    }

    @Test
    fun aTagSeenInOneFrameOnlyIsTreatedAsFlicker() {
        val scores = FrameImportance.scores(frames(40), tags(40) { if (it == 10) listOf(hit("l:Ghost")) else emptyList() })!!
        assertEquals(scores[5], scores[10], 1e-9)
    }

    @Test
    fun aBlurredFrameLosesToASharpOneWithTheSameContent() {
        val all = frames(40) { if (it == 20) 1.0 else if (it == 21) 500.0 else 100.0 }
        val scores = FrameImportance.scores(all, tags(40, ::cake))!!
        assertTrue(scores[21] > scores[20])
    }

    @Test
    fun closedEyesLowerTheScoreUnlessTheFaceIsGood() {
        val scores = FrameImportance.scores(frames(40), tags(40) {
            when (it) {
                10 -> listOf(hit("p:face", 1f), hit("p:eyes_closed", 0.9f))
                11 -> listOf(hit("p:face", 1f))
                else -> emptyList()
            }
        })!!
        assertTrue(scores[11] > scores[10])
    }

    @Test
    fun aStaticVideoYieldsASinglePicture() {
        val same = ByteArray(FrameSignature.SIZE) { 100 }
        val all = (0 until 40).map { frame(it, sig = same) }
        assertEquals(1, FrameImportance.pick(all, tags(40)).size)
    }

    @Test
    fun aFlatVideoStillSpreadsItsPicksAcrossTheTimeline() {
        val picked = FrameImportance.pick(frames(60), tags(60))
        assertTrue(picked.size >= FrameImportance.MIN_PICKS)
        assertTrue(picked.last() - picked.first() >= 30)
    }

    @Test
    fun theCountStaysWithinTheCap() {
        val picked = FrameImportance.pick(frames(900), tags(900))
        assertTrue(picked.size <= FrameImportance.maxPicks(900))
        assertEquals(picked, picked.sorted())
    }

    @Test
    fun maxPicksGrowsSlowlyAndIsBounded() {
        assertEquals(FrameImportance.MIN_PICKS, FrameImportance.maxPicks(4))
        assertEquals(10, FrameImportance.maxPicks(100))
        assertEquals(FrameImportance.MAX_PICKS, FrameImportance.maxPicks(10_000))
    }
}
