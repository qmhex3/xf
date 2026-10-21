package com.frameout.core.db

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationChainTest {
    private val migrations = AppDatabase.ALL_MIGRATIONS.sortedBy { it.startVersion }

    @Test
    fun everyStepAdvancesByExactlyOneVersion() {
        migrations.forEach { assertEquals(it.startVersion + 1, it.endVersion) }
    }

    @Test
    fun chainIsContiguousFromFirstVersion() {
        assertEquals(1, migrations.first().startVersion)
        migrations.zipWithNext { a, b -> assertEquals(a.endVersion, b.startVersion) }
    }

    @Test
    fun chainReachesCurrentVersion() {
        assertEquals(APP_DB_VERSION, migrations.last().endVersion)
    }

    @Test
    fun noDuplicateSteps() {
        assertEquals(migrations.size, migrations.map { it.startVersion }.toSet().size)
    }

    @Test
    fun currentSchemaIsExported() {
        val file = File("schemas/com.frameout.core.db.AppDatabase/$APP_DB_VERSION.json")
        assertTrue("missing ${file.path}; build :core and commit core/schemas", file.isFile)
    }
}
