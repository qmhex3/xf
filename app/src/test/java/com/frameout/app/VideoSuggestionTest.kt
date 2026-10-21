package com.frameout.app

import com.frameout.app.library.SUGGEST_MAX_AGE_MS
import com.frameout.app.library.SUGGEST_RECENT_MS
import com.frameout.app.library.shouldSuggest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoSuggestionTest {
    private val now = 1_000_000_000_000L
    private val minute = 60_000L

    private fun ask(
        added: Long = now - 5 * minute,
        previous: Long = now - 10 * 60 * minute,
        id: Long = 7,
        handled: Long = -1,
        lastOpened: String = "",
        onboarded: Boolean = true
    ) = shouldSuggest(added, now, previous, id, handled, lastOpened, "content://media/external/video/media/$id", onboarded)

    @Test
    fun newVideoSinceLastSessionIsSuggested() = assertTrue(ask())

    @Test
    fun firstLaunchNeverSuggests() = assertFalse(ask(previous = 0))

    @Test
    fun beforeOnboardingNeverSuggests() = assertFalse(ask(onboarded = false))

    @Test
    fun handledVideoIsNotSuggestedAgain() = assertFalse(ask(handled = 7))

    @Test
    fun videoOpenedLastInTheAppIsSkipped() = assertFalse(ask(lastOpened = "content://media/external/video/media/7"))

    @Test
    fun oldVideoBeforeLastSessionIsSkipped() = assertFalse(ask(added = now - 5 * 60 * minute, previous = now - 60 * minute))

    @Test
    fun videoWithinTheLastHourIsSuggestedEvenIfSeenSession() = assertTrue(ask(added = now - SUGGEST_RECENT_MS + minute, previous = now - minute))

    @Test
    fun tooOldIsSkippedEvenIfNewerThanSession() = assertFalse(ask(added = now - SUGGEST_MAX_AGE_MS - minute, previous = now - SUGGEST_MAX_AGE_MS * 2))

    @Test
    fun futureTimestampIsSkipped() = assertFalse(ask(added = now + minute))
}
