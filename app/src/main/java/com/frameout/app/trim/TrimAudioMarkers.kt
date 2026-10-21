package com.frameout.app.trim

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.frameout.engine.audio.AudioMarkers

@Composable
fun rememberAudioMarkers(uri: Uri): List<Long> {
    val context = LocalContext.current.applicationContext
    var markers by remember(uri) { mutableStateOf<List<Long>>(emptyList()) }
    LaunchedEffect(uri) { markers = AudioMarkers.find(context, uri) }
    return markers
}
