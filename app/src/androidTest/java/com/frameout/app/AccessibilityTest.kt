package com.frameout.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.a11y.frameToggle
import org.junit.Rule
import org.junit.Test

class AccessibilityTest {
    @get:Rule
    val rule = createComposeRule()

    private fun hasRole(role: Role) = SemanticsMatcher("role $role") { it.config.getOrNull(SemanticsProperties.Role) == role }

    @Test
    fun smallButtonGetsMinimumTouchTargetAndButtonRole() {
        rule.setContent { Box(Modifier.testTag("button").frameClickable { }) { Box(Modifier.size(12.dp)) } }
        rule.onNodeWithTag("button").assertHasClickAction().assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).assert(hasRole(Role.Button))
    }

    @Test
    fun selectableExposesTabRoleAndSelectedState() {
        rule.setContent { Box(Modifier.testTag("tab").frameSelectable(true) { }) { Box(Modifier.size(12.dp)) } }
        rule.onNodeWithTag("tab").assertIsSelected().assert(hasRole(Role.Tab)).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun toggleExposesSwitchRoleAndFlipsState() {
        rule.setContent {
            var on by remember { mutableStateOf(false) }
            Box(Modifier.testTag("switch").frameToggle(on) { on = it }) { Box(Modifier.size(12.dp)) }
        }
        rule.onNodeWithTag("switch").assertIsOff().assert(hasRole(Role.Switch)).performClick()
        rule.onNodeWithTag("switch").assertIsOn()
    }
}
