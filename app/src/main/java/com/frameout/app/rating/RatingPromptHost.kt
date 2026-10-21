package com.frameout.app.rating

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.AppRoute
import com.frameout.app.FrameOutApp
import com.frameout.core.rating.RatingPolicy
import kotlinx.coroutines.delay

private const val SETTLE_MS = 1600L

@Composable
fun RatingPromptHost(app: FrameOutApp, route: AppRoute?, blocked: Boolean) {
    val tracker = app.container.rating
    val ready by tracker.ready.collectAsState()
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    var lifetime by remember { mutableStateOf(0) }
    val calm = route is AppRoute.Library || route is AppRoute.Frames

    LaunchedEffect(ready, calm, blocked) {
        if (!ready || !calm || blocked || visible) return@LaunchedEffect
        delay(SETTLE_MS)
        lifetime = tracker.state().lifetimeSuccesses
        visible = true
        tracker.markShown()
    }

    if (visible) {
        RatingDialog(
            lifetimeSuccesses = lifetime,
            onRate = { stars ->
                visible = false
                tracker.markFinished()
                if (RatingPolicy.isPositive(stars)) StoreLauncher.openStore(context) else StoreLauncher.sendFeedback(context, stars)
            },
            onLater = { visible = false },
            onNever = { visible = false; tracker.markFinished() }
        )
    }
}
