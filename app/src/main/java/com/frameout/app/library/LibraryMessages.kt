package com.frameout.app.library

internal fun LibraryState.withMessage(text: String, error: Boolean, @androidx.annotation.DrawableRes icon: Int? = null): LibraryState =
    copy(message = text, errorMessage = if (error) text else null, messageIcon = icon, messageSaved = null)

internal val LibraryState.messageIsError: Boolean
    get() = message != null && message == errorMessage
