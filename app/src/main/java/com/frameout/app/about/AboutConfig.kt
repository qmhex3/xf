package com.frameout.app.about

object AboutConfig {
    const val YOUTUBE_URL: String = ""

    const val CONTACT_EMAIL: String = ""

    val hasYoutube: Boolean get() = YOUTUBE_URL.startsWith("https://")
    val hasEmail: Boolean get() = CONTACT_EMAIL.contains('@')
}
