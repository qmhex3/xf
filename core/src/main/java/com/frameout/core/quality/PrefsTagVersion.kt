package com.frameout.core.quality

import com.frameout.core.prefs.SessionPrefs

class PrefsTagVersion(private val prefs: SessionPrefs) : TagVersionStore {
    override fun stored(): Int = prefs.taggerVersion()

    override fun save(version: Int) = prefs.setTaggerVersion(version)
}
