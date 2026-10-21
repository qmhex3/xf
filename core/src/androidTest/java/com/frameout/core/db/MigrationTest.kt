package com.frameout.core.db

import android.content.Context
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MigrationTest {
    private val dbName = "migration-test"
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = context.getDatabasePath(dbName),
        driver = AndroidSQLiteDriver(),
        databaseClass = AppDatabase::class
    )

    private val migrations = AppDatabase.ALL_MIGRATIONS.toList()

    private fun exportedVersions(): List<Int> {
        val dir = "com.frameout.core.db.AppDatabase"
        return context.assets.list(dir).orEmpty().mapNotNull { it.removeSuffix(".json").toIntOrNull() }.sorted()
    }

    private fun countProjects(connection: SQLiteConnection): Long =
        connection.prepare("SELECT COUNT(*) FROM projects").use { stmt ->
            stmt.step()
            stmt.getLong(0)
        }

    @Test
    fun everyExportedVersionMigratesToLatest() = runBlocking {
        val versions = exportedVersions()
        assertTrue("no exported schemas", versions.contains(APP_DB_VERSION))
        versions.filter { it < APP_DB_VERSION }.forEach { from ->
            helper.createDatabase(from).close()
            helper.runMigrationsAndValidate(APP_DB_VERSION, migrations).close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun migrationsPreserveProjectRows() = runBlocking {
        val versions = exportedVersions().filter { it < APP_DB_VERSION }
        versions.forEach { from ->
            val old = helper.createDatabase(from)
            old.execSQL("INSERT INTO projects (name, sourceUri, sourceName, status, createdAt, updatedAt) VALUES ('p', 'content://x/1', 'p', 'Completed', 1, 1)")
            old.close()
            val db = helper.runMigrationsAndValidate(APP_DB_VERSION, migrations)
            assertEquals(1L, countProjects(db))
            db.close()
            context.deleteDatabase(dbName)
        }
    }
}
