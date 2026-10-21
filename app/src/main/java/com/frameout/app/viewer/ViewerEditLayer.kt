package com.frameout.app.viewer

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.frameout.core.edit.ViewGeometry

private const val ZOOMED = 1.05f
private val HUD_TOP = 116.dp

@Composable
internal fun ViewerEditLayer(state: ViewerState, vm: ViewerViewModel) {
    val bitmap = state.bitmap ?: return
    if (state.tool == EditTool.Crop) {
        CropOverlayBox(state, vm)
        return
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    Box(Modifier.fillMaxSize().padding(state.panelPadding()).onSizeChanged { size = it }) {
        if (size.width > 0 && size.height > 0) {
            vm.tools.setViewport(size.width.toFloat(), size.height.toFloat())
            ZoomLayers(state, vm, bitmap, ViewGeometry(bitmap.width, bitmap.height, size.width.toFloat(), size.height.toFloat()))
        }
    }
}

@Composable
private fun BoxScope.ZoomLayers(state: ViewerState, vm: ViewerViewModel, bitmap: Bitmap, geo: ViewGeometry) {
    val t = state.transform()
    val drawing = state.tool == EditTool.Draw || state.tool == EditTool.Cover
    if (state.tool == EditTool.Text) {
        val input = TextOverlayInput(geo, bitmap, state.displayDraft(), vm::transformNow, { vm.setTransform(it) }, vm.tools)
        TextOverlay(input, t)
    }
    if (state.shapesLive()) {
        ShapeOverlay(ShapeOverlayInput(geo, bitmap, state.displayDraft(), vm::transformNow, vm.tools) { vm.setTransform(it) }, t)
    }
    if (drawing && !state.shapesLive()) {
        val input = OverlayInput(geo, bitmap, state.tool, state.draft, vm::transformNow, vm.tools::addMark) { vm.setTransform(it) }
        MarkOverlay(input, t)
    }
    if (state.playing) return
    if (state.showPixelGrid) PixelGrid(geo, t)
    val hud = state.controlsVisible && (state.tool != EditTool.None || state.scale > ZOOMED)
    AnimatedVisibility(
        visible = hud,
        modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = HUD_TOP, end = 12.dp),
        enter = fadeIn(tween(200)) + scaleIn(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium), initialScale = 0.85f),
        exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.9f)
    ) { ZoomHud(geo, t, state.showPixelGrid, vm::togglePixelGrid, { vm.setTransform(it) }) }
}
