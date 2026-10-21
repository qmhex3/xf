package com.frameout.app

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VolumeKeysTest {
    @Test
    fun volumeUpMovesForwardAndDownMovesBack() {
        assertEquals(true, VolumeKeys.direction(KeyEvent.KEYCODE_VOLUME_UP))
        assertEquals(false, VolumeKeys.direction(KeyEvent.KEYCODE_VOLUME_DOWN))
    }

    @Test
    fun otherKeysAreIgnored() {
        assertNull(VolumeKeys.direction(KeyEvent.KEYCODE_BACK))
        assertNull(VolumeKeys.direction(KeyEvent.KEYCODE_VOLUME_MUTE))
    }
}
