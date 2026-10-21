package com.frameout.codecpack

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoTrackSelectorTest {
    @Test
    fun noVideoMeansMinusOne() {
        assertEquals(-1, VideoTrackSelector.choose(listOf(TrackInfo(0, "audio/mp4a-latm", 0, 0, 1000))))
        assertEquals(-1, VideoTrackSelector.choose(emptyList()))
    }

    @Test
    fun coverArtTrackIsSkipped() {
        val tracks = listOf(
            TrackInfo(0, "video/mjpeg", 3000, 3000, 0),
            TrackInfo(1, "audio/mp4a-latm", 0, 0, 5_000_000),
            TrackInfo(2, "video/avc", 1920, 1080, 5_000_000)
        )
        assertEquals(2, VideoTrackSelector.choose(tracks))
    }

    @Test
    fun longerThenLargerWins() {
        val tracks = listOf(
            TrackInfo(0, "video/avc", 640, 360, 10_000_000),
            TrackInfo(1, "video/avc", 1920, 1080, 10_000_000),
            TrackInfo(2, "video/avc", 3840, 2160, 1_000_000)
        )
        assertEquals(1, VideoTrackSelector.choose(tracks))
    }

    @Test
    fun onlyStillVideoIsStillChosen() {
        assertEquals(0, VideoTrackSelector.choose(listOf(TrackInfo(0, "video/mjpeg", 640, 480, 1_000_000))))
    }
}
