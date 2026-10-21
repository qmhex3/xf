package com.frameout.designsystem.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import com.frameout.designsystem.a11y.LocalSystemA11y

private fun rollSpec(): ContentTransform =
    (slideInVertically(tween(180)) { it / 2 } + fadeIn(tween(180))) togetherWith
        (slideOutVertically(tween(180)) { -it / 2 } + fadeOut(tween(120)))

@Composable
fun FrameRollingText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    if (LocalSystemA11y.current.reduceMotion) {
        FrameText(text, style, color, modifier, maxLines = 1)
        return
    }
    Row(modifier.clearAndSetSemantics { contentDescription = text }) {
        text.forEachIndexed { index, ch ->
            key(text.length - index) {
                AnimatedContent(targetState = ch, transitionSpec = { rollSpec() }, label = "roll") { c ->
                    FrameText(c.toString(), style, color, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
