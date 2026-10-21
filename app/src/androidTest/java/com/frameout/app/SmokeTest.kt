package com.frameout.app

import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test

class SmokeTest {
    @get:Rule
    val activity = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun launches() {
        activity.scenario.onActivity { check(!it.isFinishing) }
    }
}
