package com.frameout.app.viewer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import com.frameout.designsystem.a11y.LocalSystemA11y

private val ToolEase = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private fun toolSwitch(from: EditTool, to: EditTool, reduce: Boolean): ContentTransform {
    if (reduce) return EnterTransition.None togetherWith ExitTransition.None
    val dir = if (to.ordinal >= from.ordinal) 1 else -1
    val enter = fadeIn(tween(220, 60)) + slideInHorizontally(tween(280, easing = ToolEase)) { it / 8 * dir }
    val exit = fadeOut(tween(120)) + slideOutHorizontally(tween(280, easing = ToolEase)) { -it / 10 * dir }
    return enter togetherWith exit
}

@Composable
internal fun AnimatedToolBody(state: ViewerState, content: @Composable (ViewerState) -> Unit) {
    val reduce = LocalSystemA11y.current.reduceMotion
    AnimatedContent(
        targetState = state.tool,
        transitionSpec = { toolSwitch(initialState, targetState, reduce) },
        label = "toolBody"
    ) { tool -> content(state.copy(tool = tool)) }
}
