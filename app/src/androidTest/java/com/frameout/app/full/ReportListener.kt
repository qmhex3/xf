package com.frameout.app.full

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject
import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener

class ReportListener : RunListener() {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dir = File(context.filesDir, "fullsuite").also { it.mkdirs() }
    private val results = File(dir, "results.jsonl")
    private val states = ConcurrentHashMap<String, JSONObject>()
    private val starts = ConcurrentHashMap<String, Long>()

    override fun testRunStarted(description: Description) {
        results.delete()
        File(dir, "capabilities.json").writeText(DeviceCapabilities.snapshot(context).toString(2))
    }

    override fun testStarted(description: Description) {
        starts[key(description)] = System.currentTimeMillis()
        states[key(description)] = entry(description, "passed", "")
    }

    override fun testFailure(failure: Failure) {
        states[key(failure.description)] = entry(failure.description, "failed", failure.message.orEmpty().take(MAX_REASON))
    }

    override fun testAssumptionFailure(failure: Failure) {
        states[key(failure.description)] = entry(failure.description, "skipped", failure.message.orEmpty().take(MAX_REASON))
    }

    override fun testIgnored(description: Description) {
        append(entry(description, "skipped", "ignored"))
    }

    override fun testFinished(description: Description) {
        val state = states.remove(key(description)) ?: entry(description, "passed", "")
        val started = starts.remove(key(description)) ?: System.currentTimeMillis()
        append(state.put("durationMs", System.currentTimeMillis() - started))
    }

    private fun key(description: Description): String = description.className + "#" + description.methodName

    private fun entry(description: Description, status: String, reason: String): JSONObject = JSONObject()
        .put("class", description.className.substringAfterLast('.'))
        .put("test", description.methodName.orEmpty())
        .put("heavy", description.getAnnotation(FullSuite::class.java) != null || description.testClass?.isAnnotationPresent(FullSuite::class.java) == true)
        .put("status", status)
        .put("reason", reason)

    private fun append(entry: JSONObject) {
        synchronized(results) { results.appendText(entry.toString() + "\n") }
    }

    private companion object {
        const val MAX_REASON = 600
    }
}
