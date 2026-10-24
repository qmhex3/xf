package com.frameout.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteCodecTest {
    @Test
    fun framesAndViewerRoundTrip() {
        val frames = AppRoute.Frames(7L)
        val viewer = AppRoute.Viewer(7L, 42L)
        assertEquals(frames, RouteCodec.decode(RouteCodec.encode(frames)!!))
        assertEquals(viewer, RouteCodec.decode(RouteCodec.encode(viewer)!!))
    }

    @Test
    fun viewerOpenedFromSavedKeepsSavedId() {
        val viewer = AppRoute.Viewer(7L, 42L, savedId = 5L)
        assertEquals(viewer, RouteCodec.decode(RouteCodec.encode(viewer)!!))
    }

    @Test
    fun savedViewerRoundTrip() {
        val route = AppRoute.SavedViewer(12L)
        assertEquals(route, RouteCodec.decode(RouteCodec.encode(route)!!))
    }

    @Test
    fun collageKeepsFrameIds() {
        val route = AppRoute.Collage(3L, listOf(1L, 2L, 9L))
        assertEquals(route, RouteCodec.decode(RouteCodec.encode(route)!!))
    }

    @Test
    fun oversizedSelectionFallsBackToFrames() {
        val ids = (1L..501L).toList()
        assertEquals(AppRoute.Frames(1L), RouteCodec.decode(RouteCodec.encode(AppRoute.Collage(1L, ids))!!))
        assertEquals(AppRoute.Frames(1L), RouteCodec.decode(RouteCodec.encode(AppRoute.Compare(1L, ids))!!))
    }

    @Test
    fun batchIsNotPersisted() {
        assertNull(RouteCodec.encode(AppRoute.Batch(listOf("content://a"))))
    }

    @Test
    fun malformedInputIsRejected() {
        assertNull(RouteCodec.decode("viewer|x"))
        assertNull(RouteCodec.decode("unknown"))
        assertNull(RouteCodec.decode(""))
    }

    @Test
    fun restoreAlwaysStartsWithLibrary() {
        val restored = RouteCodec.restore(listOf("frames|5", "viewer|5|8"))
        assertEquals(AppRoute.Library, restored.first())
        assertEquals(3, restored.size)
        assertEquals(listOf<AppRoute>(AppRoute.Library), RouteCodec.restore(emptyList()))
    }
}
