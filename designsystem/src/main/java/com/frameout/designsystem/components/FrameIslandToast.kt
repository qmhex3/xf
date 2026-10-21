package com.frameout.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Immutable
class IslandAction(
    val label: String,
    @DrawableRes val icon: Int,
    val primary: Boolean = false,
    val onClick: () -> Unit
)

@Immutable
data class IslandToastState(
    val title: String,
    val detail: String? = null,
    val kind: ToastKind = ToastKind.Success,
    val busy: Boolean = false,
    val actions: List<IslandAction> = emptyList(),
    @DrawableRes val icon: Int? = null,
    val saved: Boolean = false
) {
    internal val expandable: Boolean
        get() = actions.isNotEmpty() || detail != null
}

fun islandToastState(
    message: String,
    kind: ToastKind = ToastKind.Success,
    busy: Boolean = false,
    actions: List<IslandAction> = emptyList(),
    @DrawableRes icon: Int? = null,
    saved: Boolean = false
): IslandToastState {
    val dot = message.indexOf(" · ")
    val dash = message.indexOf(" — ")
    val cut = listOf(dot, dash).filter { it > 0 }.minOrNull() ?: -1
    return if (cut > 0) IslandToastState(message.substring(0, cut), message.substring(cut + 3), kind, busy, actions, icon, saved)
    else IslandToastState(message, null, kind, busy, actions, icon, saved)
}


internal const val EXPAND_DELAY_MS = 700L
internal val COMPACT_H = 34.dp
internal val COMPACT_MAX_W = 176.dp
internal val EXPANDED_W = 300.dp
internal val EXPANDED_MAX_CORNER = 26.dp
internal val SIDE_MARGIN = 16.dp
internal val REST_GAP = 12.dp
internal val EASE_IN = CubicBezierEasing(0.5f, 0f, 0.85f, 0.35f)


@Composable
fun FrameIslandToast(
    state: IslandToastState?,
    modifier: Modifier = Modifier,
    durationMs: Long? = null,
    onDismiss: (() -> Unit)? = null
) {
    val reduce = LocalSystemA11y.current.reduceMotion
    var last by remember { mutableStateOf<IslandToastState?>(null) }
    SideEffect { if (state != null && state !== last) last = state }
    val shown = state ?: last
    val present = state != null
    val expandable = state?.expandable == true

    val appear = remember { Animatable(0f) }
    val expand = remember { Animatable(0f) }
    val ring = remember { Animatable(1f) }
    val timer = remember { Animatable(1f) }
    val checkP = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    val drag = remember { Animatable(0f) }
    val neck = remember { Animatable(0f) }

    LaunchedEffect(present) {
        if (present) {
            if (!reduce) {
                neck.snapTo(1f)
                launch { neck.animateTo(0f, tween(520, delayMillis = 200, easing = FastOutSlowInEasing)) }
                ring.snapTo(0f)
                launch { ring.animateTo(1f, tween(650, easing = LinearOutSlowInEasing)) }
                appear.animateTo(1f, spring(dampingRatio = 0.58f, stiffness = 360f))
            } else {
                neck.snapTo(0f)
                appear.animateTo(1f, tween(160))
            }
        } else if (last != null) {
            launch { drag.animateTo(0f, tween(240)) }
            if (reduce) {
                expand.snapTo(0f)
                appear.animateTo(0f, tween(140))
            } else {
                launch { neck.animateTo(1f, tween(300, delayMillis = 200, easing = LinearOutSlowInEasing)) }
                expand.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
                appear.animateTo(0f, tween(320, easing = EASE_IN))
                ring.snapTo(0f)
                ring.animateTo(1f, tween(520, easing = LinearOutSlowInEasing))
            }
            last = null
        }
    }

    LaunchedEffect(present, expandable) {
        if (!present) return@LaunchedEffect
        if (expandable) {
            if (reduce) expand.snapTo(1f) else {
                if (expand.value == 0f) delay(EXPAND_DELAY_MS)
                expand.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 320f))
            }
        } else {
            expand.animateTo(0f, spring(dampingRatio = 1f, stiffness = 420f))
        }
    }

    LaunchedEffect(state?.title, state?.detail, state?.kind, state?.busy, durationMs) {
        val s = state ?: return@LaunchedEffect
        timer.snapTo(1f)
        if (s.busy || durationMs == null) return@LaunchedEffect
        timer.animateTo(0f, tween(durationMs.toInt().coerceAtLeast(1), easing = LinearEasing))
        onDismiss?.invoke()
    }

    val done = shown?.let { !it.busy && it.kind == ToastKind.Success } == true
    LaunchedEffect(done) {
        if (!done) { checkP.snapTo(0f); return@LaunchedEffect }
        if (reduce) { checkP.snapTo(1f); return@LaunchedEffect }
        checkP.snapTo(0f)
        delay(if (appear.value < 0.9f) 180L else 40L)
        checkP.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
    }
    val kindKey = shown?.takeIf { !it.busy }?.kind
    LaunchedEffect(kindKey) {
        if (kindKey == null || reduce) return@LaunchedEffect
        pop.snapTo(0.55f)
        pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 520f))
    }

    val s = shown ?: return
    val accent = s.kind.islandAccent(FrameTheme.colors)
    val density = LocalDensity.current
    val view = LocalView.current
    val statusTop = WindowInsets.statusBars.getTop(density)
    val minD = with(density) { 10.dp.toPx() }
    val maxD = with(density) { 30.dp.toPx() }
    val rawSpot = rememberCameraSpot(view, minD, maxD)
    var rootPos by remember { mutableStateOf(Offset.Zero) }
    val spot = rawSpot?.takeIf {
        val x = it.cx - rootPos.x
        val y = it.cy - rootPos.y
        x.isFinite() && y.isFinite() && x >= 0f && y >= 0f && y <= view.height * 0.5f && x <= view.width.toFloat()
    }
    var geo by remember { mutableStateOf<NeckGeo?>(null) }
    val dismissNow by rememberUpdatedState(onDismiss)

    Layout(
        content = {
            IslandPill(
                state = s,
                accent = accent,
                reduce = reduce,
                dismissable = onDismiss != null && !s.busy,
                originD = spot?.d ?: minD,
                anchored = spot != null,
                appear = { appear.value },
                expand = { expand.value },
                timer = { timer.value },
                checkP = { checkP.value },
                pop = { pop.value },
                chrome = { if (spot == null) 1f else ramp(appear.value, 0.4f, 0.9f) * (1f - neck.value) },
                drag = drag,
                onDismiss = { dismissNow?.invoke() }
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { rootPos = it.positionInWindow() }
            .drawBehind {
                if (spot == null) return@drawBehind
                drawCameraRing(spot, rootPos, accent, ring.value)
                geo?.let { drawNeck(it, Color.Black) }
            }
    ) { measurables, constraints ->
        val pill = measurables[0].measure(Constraints(maxWidth = max(constraints.maxWidth - (SIDE_MARGIN * 2).roundToPx(), 0)))
        val p = appear.value
        val e = expand.value
        val d = spot?.d ?: minD
        val compactH = COMPACT_H.toPx()
        val rootW = constraints.maxWidth.toFloat()
        val ox = if (spot != null) spot.cx - rootPos.x else rootW / 2f
        val oy = if (spot != null) spot.cy - rootPos.y else (statusTop - rootPos.y) + 6.dp.toPx() + compactH / 2f
        val minTop = 3.dp.toPx()
        val statusRest = (statusTop - rootPos.y) + 6.dp.toPx()
        val compactTop = when {
            spot == null -> max(oy - compactH / 2f, minTop)
            spot.onTop -> max(oy + d / 2f + REST_GAP.toPx(), minTop)
            else -> max(statusRest, minTop)
        }
        val expandedTop = max(compactTop, (statusTop - rootPos.y) + 4.dp.toPx())
        val pc = p.coerceIn(0f, 1f)
        val cx = lerpF(ox, rootW / 2f, pc)
        val topAppear = lerpF(oy - d / 2f, compactTop, p)
        val top = lerpF(topAppear, expandedTop, e).let { if (it.isFinite()) it.coerceAtLeast(0f) else 0f }
        val height = (top + pill.height + 28.dp.toPx()).let { if (it.isFinite()) it else 0f }.roundToInt().coerceAtLeast(0)
        val left = cx - pill.width / 2f
        if (spot != null) {
            val corner = min(pill.height / 2f, EXPANDED_MAX_CORNER.toPx())
            geo = NeckGeo(ox, oy, d / 2f, left, top, pill.width.toFloat(), corner, neck.value)
        }
        layout(constraints.maxWidth, height) {
            pill.place(left.roundToInt(), top.roundToInt())
        }
    }
}
