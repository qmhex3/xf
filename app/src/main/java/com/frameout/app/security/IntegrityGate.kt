package com.frameout.app.security

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.frameout.core.util.DiagLog
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameConfirmDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun IntegrityGate(activity: ComponentActivity) {
    var verdict by remember { mutableStateOf(Verdict.Clean) }
    LaunchedEffect(Unit) {
        verdict = withContext(Dispatchers.IO) { Sentinel.verdict(activity) }
        if (verdict != Verdict.Clean) DiagLog.append("integrity", "verdict=$verdict")
    }
    if (verdict != Verdict.Tampered) return
    FrameConfirmDialog(
        title = FrameStrings.tamperTitle(),
        body = FrameStrings.tamperBody(),
        confirmLabel = FrameStrings.tamperExit(),
        dismissLabel = null,
        onConfirm = { activity.finishAffinity() },
        onDismiss = { activity.finishAffinity() },
        tone = com.frameout.designsystem.components.DialogTone.Danger,
        icon = com.frameout.designsystem.icons.FrameIconsExtra.Warning
    )
}
