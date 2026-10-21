package com.frameout.app.trim

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.frameout.core.hdr.HdrSupport
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.storage.ImageFormat
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.engine.decode.HdrDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HdrNotice(uri: Uri, prefs: AppPrefs) {
    val context = LocalContext.current
    var hdr by remember(uri) { mutableStateOf(false) }
    val format by prefs.saveFormat.collectAsState()
    LaunchedEffect(uri) { hdr = withContext(Dispatchers.IO) { HdrDetector.isHdr(context, uri) } }
    if (!hdr) return
    val colors = FrameTheme.colors
    val message = when {
        !HdrSupport.supported() -> FrameStrings.hdrNoticeUnsupported()
        ImageFormat.fromKey(format).hdr -> FrameStrings.hdrNoticeOn()
        else -> FrameStrings.hdrNoticeOff()
    }
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).background(colors.fieldFill, FrameShapes.sm).padding(12.dp)) {
        FrameText(message, style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}
