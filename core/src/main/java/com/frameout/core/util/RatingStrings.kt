package com.frameout.core.util

import com.frameout.core.R

object RatingStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun title() = s(R.string.rating_title)
    fun body() = s(R.string.rating_body)
    fun stat(count: Int) = StringSource.plural(R.plurals.rating_stat, count, count)
    fun hint() = s(R.string.rating_hint)
    fun label(stars: Int) = when (stars) {
        1 -> s(R.string.rating_label_1)
        2 -> s(R.string.rating_label_2)
        3 -> s(R.string.rating_label_3)
        4 -> s(R.string.rating_label_4)
        5 -> s(R.string.rating_label_5)
        else -> s(R.string.rating_hint)
    }
    fun ctaChoose() = s(R.string.rating_cta_choose)
    fun ctaStore() = s(R.string.rating_cta_store)
    fun ctaFeedback() = s(R.string.rating_cta_feedback)
    fun notePositive() = s(R.string.rating_note_positive)
    fun noteNegative() = s(R.string.rating_note_negative)
    fun later() = s(R.string.rating_later)
    fun never() = s(R.string.rating_never)
    fun starsA11y(stars: Int) = s(R.string.rating_stars_a11y, stars)
    fun feedbackSubject() = s(R.string.rating_feedback_subject)
    fun rowTitle() = s(R.string.rating_row_title)
    fun rowBody() = s(R.string.rating_row_body)
}
