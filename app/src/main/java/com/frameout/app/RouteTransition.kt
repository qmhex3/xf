package com.frameout.app

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

private val EaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val EaseInOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

internal fun isTabSwitch(from: AppRoute?, to: AppRoute?): Boolean =
    (from is AppRoute.Library && to is AppRoute.Settings) || (from is AppRoute.Settings && to is AppRoute.Library)

private fun tabTransition(): ContentTransform {
    val enter = fadeIn(tween(260, easing = EaseInOut))
    val exit = fadeOut(tween(1, delayMillis = 260))
    return ContentTransform(enter, exit, 1f, SizeTransform(clip = false))
}

internal fun zoomKind(from: AppRoute, to: AppRoute): Int = when {
    isTabSwitch(from, to) -> 3
    from is AppRoute.Library && (to is AppRoute.Frames || to is AppRoute.Extracting) -> 1
    (from is AppRoute.Frames || from is AppRoute.Extracting) && to is AppRoute.Library -> 2
    else -> 0
}

private fun zoomTransition(opening: Boolean): ContentTransform {
    val enter = fadeIn(tween(280, delayMillis = 40, easing = EaseInOut)) + scaleIn(tween(380, easing = EaseOutQuint), initialScale = if (opening) 0.9f else 1.08f)
    val exit = fadeOut(tween(200, easing = EaseInOut)) + scaleOut(tween(300, easing = EaseInOut), targetScale = if (opening) 1.08f else 0.92f)
    return ContentTransform(enter, exit, 0f, SizeTransform(clip = false))
}

internal fun routeTransition(direction: Int, rtl: Boolean, reduceMotion: Boolean, zoom: Int = 0): ContentTransform {
    if (zoom == 3) return tabTransition()
    if (zoom != 0 && !reduceMotion) return zoomTransition(zoom == 1)
    if (reduceMotion) return ContentTransform(fadeIn(tween(120)), fadeOut(tween(90)), 0f, SizeTransform(clip = false))
    val sign = if (rtl) -1 else 1
    val spec = when (direction) {
        0 -> fadeIn(tween(260, delayMillis = 60, easing = EaseInOut)) + scaleIn(tween(320, easing = EaseOutQuint), initialScale = 0.96f) togetherWith
            fadeOut(tween(160, easing = EaseInOut)) + scaleOut(tween(200, easing = EaseInOut), targetScale = 1.02f)
        1 -> fadeIn(tween(300, delayMillis = 50, easing = EaseInOut)) +
            slideInHorizontally(tween(440, easing = EaseOutQuint)) { (it * 0.26f).toInt() * sign } +
            scaleIn(tween(440, easing = EaseOutQuint), initialScale = 0.96f) togetherWith
            fadeOut(tween(340, easing = EaseInOut), targetAlpha = 0.2f) +
            scaleOut(tween(440, easing = EaseOutQuint), targetScale = 0.92f) +
            slideOutHorizontally(tween(440, easing = EaseOutQuint)) { -(it * 0.16f).toInt() * sign }
        else -> fadeIn(tween(320, easing = EaseInOut), initialAlpha = 0.2f) +
            slideInHorizontally(tween(440, easing = EaseOutQuint)) { -(it * 0.16f).toInt() * sign } +
            scaleIn(tween(440, easing = EaseOutQuint), initialScale = 0.92f) togetherWith
            fadeOut(tween(260, delayMillis = 40, easing = EaseInOut)) +
            scaleOut(tween(440, easing = EaseOutQuint), targetScale = 0.96f) +
            slideOutHorizontally(tween(440, easing = EaseOutQuint)) { (it * 0.26f).toInt() * sign }
    }
    return ContentTransform(spec.targetContentEnter, spec.initialContentExit, if (direction < 0) -1f else 0f, SizeTransform(clip = false))
}
