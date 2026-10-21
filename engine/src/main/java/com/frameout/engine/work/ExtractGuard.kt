package com.frameout.engine.work

import android.content.Context
import android.os.Build
import com.frameout.engine.extract.ExtractEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal fun CoroutineScope.launchFgsGuard(
    context: Context,
    engine: ExtractEngine,
    projectId: Long,
    pollMs: Long,
    onRotate: () -> Unit
): Job? {
    if (Build.VERSION.SDK_INT < 35) return null
    val parent = this
    return launch {
        while (isActive) {
            delay(pollMs)
            if (FgsBudget.shouldRotate(context, projectId)) {
                onRotate()
                ExtractControl.expectStop(projectId)
                engine.pause()
                parent.cancel(CancellationException("fgs_rotation"))
                return@launch
            }
        }
    }
}
