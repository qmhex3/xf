package com.frameout.app

import com.frameout.designsystem.glass.glassDisabled
import com.frameout.designsystem.theme.GlassIntensity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassLevelTest {
    @Test
    fun glassStaysOnWhenNothingRestrictsIt() {
        assertFalse(glassDisabled(GlassIntensity.Full, powerSave = false, lowRam = false, reduceTransparency = false))
    }

    @Test
    fun reduceTransparencyDisablesGlass() {
        assertTrue(glassDisabled(GlassIntensity.Full, powerSave = false, lowRam = false, reduceTransparency = true))
    }

    @Test
    fun powerSaveLowRamAndOffDisableGlass() {
        assertTrue(glassDisabled(GlassIntensity.Full, powerSave = true, lowRam = false, reduceTransparency = false))
        assertTrue(glassDisabled(GlassIntensity.Light, powerSave = false, lowRam = true, reduceTransparency = false))
        assertTrue(glassDisabled(GlassIntensity.Off, powerSave = false, lowRam = false, reduceTransparency = false))
    }

    @Test
    fun thermalThrottlingDisablesGlass() {
        assertTrue(glassDisabled(GlassIntensity.Full, powerSave = false, lowRam = false, reduceTransparency = false, thermalThrottled = true))
        assertFalse(glassDisabled(GlassIntensity.Full, powerSave = false, lowRam = false, reduceTransparency = false, thermalThrottled = false))
    }
}
