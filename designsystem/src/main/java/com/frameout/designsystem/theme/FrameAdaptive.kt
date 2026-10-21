package com.frameout.designsystem.theme

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

val FrameRailCompactWidth: Dp = 72.dp
val FrameRailExpandedWidth: Dp = 220.dp

private val RAIL_GUTTER = 24.dp
private val REFERENCE_WIDTH = 400.dp
private const val MAX_COLUMNS = 16

@Immutable
class FrameAdaptive(val width: Dp, val height: Dp, val isTv: Boolean) {
    val size: FrameWindowSize get() = frameWindowSize(width)

    val landscape: Boolean get() = width > height

    val sideEditor: Boolean get() = landscape && (height < 500.dp || width >= 840.dp)

    val railInset: Dp
        get() = when (size) {
            FrameWindowSize.Compact -> 0.dp
            FrameWindowSize.Medium -> FrameRailCompactWidth + RAIL_GUTTER
            FrameWindowSize.Expanded -> FrameRailExpandedWidth + RAIL_GUTTER
        }

    fun columns(base: Int): Int {
        val usable = (width - railInset).value
        val factor = usable / REFERENCE_WIDTH.value
        return (base * factor).roundToInt().coerceIn(base, MAX_COLUMNS.coerceAtLeast(base))
    }
}

@Composable
fun rememberFrameAdaptive(): FrameAdaptive {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val tv = remember(context) {
        (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
    return remember(configuration.screenWidthDp, configuration.screenHeightDp, tv) {
        FrameAdaptive(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp, tv)
    }
}
