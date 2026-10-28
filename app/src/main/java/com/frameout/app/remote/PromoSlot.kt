package com.frameout.app.remote

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Drop this anywhere in a screen: shows the first active, verified promo (text, image or video). */
@Composable
fun PromoSlot(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var promo by remember { mutableStateOf<Promo?>(null) }
    var file by remember { mutableStateOf<File?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val p = RemoteStore(ctx).current()?.promos?.firstOrNull { it.isActive() } ?: return@withContext
            val f = if (p.media != null && p.sha != null) RemoteSync.media(ctx, p.media, p.sha) else null
            if (p.type == "text" || f != null) { file = f; promo = p }
        }
    }
    val p = promo ?: return
    fun open() = p.link?.let { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    Card(modifier.fillMaxWidth().clickable { open() }, shape = RoundedCornerShape(20.dp)) {
        Column {
            when (p.type) {
                "image" -> file?.let { f ->
                    val bmp = remember(f) { BitmapFactory.decodeFile(f.path)?.asImageBitmap() }
                    bmp?.let { Image(it, null, Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop) }
                }
                "video" -> file?.let { f ->
                    val player = remember(f) { ExoPlayer.Builder(ctx).build().apply { setMediaItem(MediaItem.fromUri(Uri.fromFile(f))); volume = 0f; repeatMode = 1; prepare(); playWhenReady = true } }
                    DisposableEffect(player) { onDispose { player.release() } }
                    AndroidView({ PlayerView(it).apply { this.player = player; useController = false } }, Modifier.fillMaxWidth().height(200.dp))
                }
            }
            if (p.title.isNotEmpty() || p.body.isNotEmpty()) Column(Modifier.padding(16.dp)) {
                if (p.title.isNotEmpty()) Text(p.title, style = MaterialTheme.typography.titleMedium)
                if (p.body.isNotEmpty()) Text(p.body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
