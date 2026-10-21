package com.frameout.app.rating

import androidx.compose.ui.graphics.Color
import com.frameout.core.rating.RatingPolicy

internal val RatingGold = Color(0xFFFFC53D)
internal val RatingGoldDeep = Color(0xFFFF9F1C)
internal const val RATING_STARS = 5

internal enum class Bucket { None, Negative, Positive }

internal fun bucketOf(stars: Int) = when {
    stars <= 0 -> Bucket.None
    RatingPolicy.isPositive(stars) -> Bucket.Positive
    else -> Bucket.Negative
}
