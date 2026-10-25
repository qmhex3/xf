package com.frameout.app.trim

import com.frameout.app.asChooserWithoutSelf
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameIslandToast
import com.frameout.designsystem.components.IslandAction
import com.frameout.designsystem.components.IslandToastState
import com.frameout.designsystem.components.ToastKind
import com.frameout.designsystem.components.islandToastState
import com.frameout.designsystem.components.toastDurationMs
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.icons.SaveKind

data class SavedCapture(val uri: Uri, val mime: String)

data class CaptureStatusState(val message: String, val saved: SavedCapture? = null, val error: Boolean = false, val busy: Boolean = false, val kind: SaveKind? = null)

@Composable
fun CaptureStatus(state: CaptureStatusState?, modifier: Modifier = Modifier, onDismiss: (() -> Unit)? = null) {
    val context = LocalContext.current
    val island = remember(state, context) { state?.toIsland(context) }
    val duration = state?.takeIf { onDismiss != null && !it.busy }?.let {
        toastDurationMs(it.message, it.kind()) + if (it.saved != null) ACTION_EXTRA_MS else 0L
    }
    FrameIslandToast(island, modifier, durationMs = duration, onDismiss = onDismiss)
}

private fun CaptureStatusState.savedKind(): SaveKind? =
    if (error || busy) null else saved?.let { SaveKind.fromMime(it.mime) } ?: kind

private fun CaptureStatusState.kind() = if (error) ToastKind.Error else ToastKind.Success

internal fun CaptureStatusState.toIsland(context: Context): IslandToastState = islandToastState(
    message = message,
    kind = kind(),
    busy = busy,
    icon = savedKind()?.icon,
    saved = savedKind() != null,
    actions = saved?.takeUnless { error || busy }?.let { saved ->
        listOf(
            if (saved.opensInViewer()) IslandAction(FrameStrings.openSaved(), FrameIconsExtra.Album, primary = true) { saved.open(context) }
            else IslandAction(FrameStrings.showInFolder(), FrameIconsExtra.Folder, primary = true) { saved.showInFolder(context) },
            IslandAction(FrameStrings.share(), FrameIcons.Share) { saved.share(context) }
        )
    }.orEmpty()
)

internal const val ACTION_EXTRA_MS = 3000L

internal fun SavedCapture.open(context: Context) {
    val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    if (!tryLaunch(context, view)) showInFolder(context)
}

internal fun SavedCapture.share(context: Context) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    launch(context, send.asChooserWithoutSelf(context))
}

private fun launch(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}
