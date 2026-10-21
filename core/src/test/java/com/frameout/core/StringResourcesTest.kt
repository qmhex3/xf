package com.frameout.core

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StringResourcesTest {
    private fun load(dir: String): Map<String, String> {
        val file = File("src/main/res/$dir/strings.xml")
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
        return (0 until nodes.length).associate { nodes.item(it).attributes.getNamedItem("name").nodeValue to nodes.item(it).textContent }
    }

    private fun placeholders(value: String): List<String> =
        Regex("%\\d\\$[sd]").findAll(value).map { it.value }.sorted().toList()

    @Test
    fun everyLocaleHasSameKeys() {
        val base = load("values")
        assertTrue(base.isNotEmpty())
        assertEquals(base.keys, load("values-ar").keys)
    }

    @Test
    fun placeholdersMatchAcrossLocales() {
        val base = load("values")
        val arabic = load("values-ar")
        base.forEach { (key, value) -> assertEquals(key, placeholders(value), placeholders(arabic.getValue(key))) }
    }
}
