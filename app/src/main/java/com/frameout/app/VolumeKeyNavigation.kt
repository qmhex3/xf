package com.frameout.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun VolumeKeyNavigation(enabled: Boolean = true, onStep: suspend (Boolean) -> Unit) {
    val prefs = (LocalContext.current.applicationContext as FrameOutApp).container.prefs
    val setting by prefs.volumeNav.collectAsState()
    val active by rememberUpdatedState(enabled && setting)
    val step by rememberUpdatedState(onStep)
    val scope = rememberCoroutineScope()
    val running = remember { arrayOfNulls<Job>(1) }
    DisposableEffect(Unit) {
        val target = object : VolumeTarget {
            override fun isActive(): Boolean = active

            override fun onStep(forward: Boolean) {
                if (running[0]?.isActive != true) running[0] = scope.launch { step(forward) }
            }
        }
        VolumeKeys.register(target)
        onDispose { VolumeKeys.unregister(target) }
    }
}
