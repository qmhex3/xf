package com.frameout.app.library

import com.frameout.designsystem.theme.frameHairline
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.frameout.core.util.FrameA11y
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.frameout.designsystem.a11y.frameCombinedClickable
import com.frameout.designsystem.a11y.frameClickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.frameout.app.thumb.ThumbPlaceholder
import com.frameout.core.util.ThumbCache
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.core.db.SavedImageEntity
import com.frameout.core.db.SavedKind
import com.frameout.core.db.savedKindOf
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.app.CellOrigin
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

@Composable
fun SavedImageCard(
    item: SavedImageEntity,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onResave: () -> Unit,
    selectionMode: Boolean = false
) {
    val context = LocalContext.current
    val state = remember(item.uri) { mutableStateOf(SavedThumbs.peek(item.uri)) }
    LaunchedEffect(item.uri) {
        if (state.value == null) state.value = SavedThumbs.load(context, item.uri)
    }
    val loaded = state.value
    val bitmap = loaded?.bitmap
    val isMissing = loaded?.missing == true
    val colors = FrameTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val edge = when {
        selected -> Modifier.border(2.dp, colors.primary, shape)
        isMissing -> Modifier.border(1.dp, colors.error.copy(alpha = 0.35f), shape)
        else -> Modifier.border(1.dp, frameHairline(), shape)
    }
    val kind = remember(item.displayName) { savedKindOf(item.displayName) }
    val format = item.displayName.substringAfterLast('.', "").uppercase().takeIf { it.length in 2..4 }
    val coords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    Box(
        Modifier
            .padding(3.dp)
            .frameCombinedClickable({ if (!selectionMode) CellOrigin.mark(coords[0]); onClick() }, onLongClick, FrameA11y.select())
            .semantics { this.selected = selected }
            .aspectRatio(1f)
            .onGloballyPositioned { coords[0] = it }
            .clip(shape)
            .background(if (isMissing) colors.error.copy(alpha = 0.10f) else colors.container),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), item.displayName, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else if (!isMissing) {
            ThumbPlaceholder()
        }
        if (isMissing) MissingContent(onResave)
        if (kind != SavedKind.Photo && !isMissing) {
            KindBadge(kind, Modifier.align(Alignment.BottomEnd).padding(5.dp))
        } else if (format != null && !isMissing) {
            FrameText(
                format,
                style = FrameTheme.typography.monoSmall.copy(fontSize = 9.sp),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
        if (selectionMode) SelectionMark(selected, Modifier.align(Alignment.TopStart).padding(7.dp))
        Box(Modifier.fillMaxSize().then(edge))
    }
}

@Composable
private fun KindBadge(kind: SavedKind, modifier: Modifier) {
    Row(
        modifier.background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(5.dp)).padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(kind.icon()), null, Modifier.size(11.dp), colorFilter = ColorFilter.tint(Color.White))
        Spacer(Modifier.width(3.dp))
        FrameText(kind.label(), style = FrameTheme.typography.monoSmall.copy(fontSize = 9.sp), color = Color.White, maxLines = 1)
    }
}

@Composable
private fun MissingContent(onResave: () -> Unit) {
    val colors = FrameTheme.colors
    Column(
        Modifier.padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Image(
            painterResource(FrameIconsExtra.Warning),
            null,
            Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(colors.error)
        )
        FrameText(
            FrameStrings.savedMissing(),
            style = FrameTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        FrameText(
            FrameStrings.resave(),
            style = FrameTheme.typography.label,
            color = colors.background,
            modifier = Modifier
                .clip(FrameShapes.pill)
                .background(colors.error)
                .frameClickable(onClick = onResave)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SelectionMark(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    Box(
        modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) colors.primary else Color.Black.copy(alpha = 0.25f))
            .then(if (selected) Modifier else Modifier.border(1.8.dp, Color.White.copy(alpha = 0.85f), CircleShape)),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Image(
                painterResource(FrameIcons.Check),
                null,
                Modifier.size(14.dp),
                colorFilter = ColorFilter.tint(colors.onPrimary)
            )
        }
    }
}

internal data class ThumbResult(val bitmap: android.graphics.Bitmap?, val missing: Boolean)

internal object SavedThumbs {
    private fun key(uri: String) = "saved#$uri#512"

    fun peek(uri: String): ThumbResult? = ThumbCache.peekKey(key(uri))?.let { ThumbResult(it, false) }

    suspend fun load(context: android.content.Context, uri: String): ThumbResult {
        var missing = false
        val bitmap = ThumbCache.loadKeyed(key(uri)) {
            val decoded = decode(context, uri)
            missing = decoded.missing
            decoded.bitmap
        }
        return ThumbResult(bitmap, bitmap == null || missing)
    }

    private fun decode(context: android.content.Context, uri: String): ThumbResult {
        systemThumb(context, uri)?.let { return ThumbResult(it, false) }
        return decodeFile(context, uri)
    }

    private fun systemThumb(context: android.content.Context, uri: String): android.graphics.Bitmap? {
        if (android.os.Build.VERSION.SDK_INT < 29) return null
        return runCatching {
            context.contentResolver.loadThumbnail(android.net.Uri.parse(uri), android.util.Size(512, 512), null)
        }.getOrNull()
    }

    private fun decodeFile(context: android.content.Context, uri: String): ThumbResult = runCatching {
        val descriptor = context.contentResolver.openFileDescriptor(android.net.Uri.parse(uri), "r") ?: return@runCatching ThumbResult(null, true)
        descriptor.use {
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeFileDescriptor(it.fileDescriptor, null, bounds)
            val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight) }
            val bitmap = android.graphics.BitmapFactory.decodeFileDescriptor(it.fileDescriptor, null, options)
            ThumbResult(bitmap, bitmap == null)
        }
    }.getOrElse { ThumbResult(null, true) }

    private fun sampleSize(width: Int, height: Int): Int {
        var size = 1
        while (width / size > 512 || height / size > 512) size *= 2
        return size
    }
}
