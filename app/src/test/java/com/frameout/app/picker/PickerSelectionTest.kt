package com.frameout.app.picker

import com.frameout.core.media.MediaItem
import com.frameout.core.media.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PickerSelectionTest {
    private fun item(n: Int) = MediaItem(n.toLong(), "u$n", "n$n", "image/jpeg", 1, 0, 1, 1, 0, "b", "b", MediaKind.Image)

    private fun stateOf(vararg n: Int) = n.fold(PickerState()) { s, i -> s.toggleSelect(item(i)) }

    private fun PickerState.order() = selected.keys.toList()

    @Test
    fun moveForwardKeepsOtherOrder() {
        assertEquals(listOf("u2", "u3", "u1", "u4"), stateOf(1, 2, 3, 4).moveSelect(0, 2).order())
    }

    @Test
    fun moveBackward() {
        assertEquals(listOf("u4", "u1", "u2", "u3"), stateOf(1, 2, 3, 4).moveSelect(3, 0).order())
    }

    @Test
    fun invalidOrEqualMoveIsNoOp() {
        val s = stateOf(1, 2, 3)
        assertSame(s, s.moveSelect(1, 1))
        assertSame(s, s.moveSelect(-1, 2))
        assertSame(s, s.moveSelect(0, 3))
    }

    @Test
    fun restoreBringsBackOrder() {
        val items = listOf(item(3), item(1), item(2))
        assertEquals(listOf("u3", "u1", "u2"), PickerState().restoreSelect(items).order())
    }

    @Test
    fun restoreAtReinsertsAtOriginalIndex() {
        assertEquals(listOf("u1", "u2", "u3"), stateOf(1, 3).restoreAt(item(2), 1).order())
    }

    @Test
    fun restoreAtClampsIndexAndIgnoresDuplicates() {
        assertEquals(listOf("u1", "u2"), stateOf(1).restoreAt(item(2), 9).order())
        val s = stateOf(1, 2)
        assertSame(s, s.restoreAt(item(2), 0))
    }
}
