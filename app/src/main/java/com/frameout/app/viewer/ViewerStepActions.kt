package com.frameout.app.viewer

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.frameout.core.util.FrameStrings

internal fun Modifier.savedStepActions(enabled: Boolean, vm: ViewerViewModel): Modifier =
    if (!enabled) this else semantics {
        customActions = listOf(
            CustomAccessibilityAction(FrameStrings.previous()) { vm.goTo(-1); true },
            CustomAccessibilityAction(FrameStrings.next()) { vm.goTo(1); true }
        )
    }
