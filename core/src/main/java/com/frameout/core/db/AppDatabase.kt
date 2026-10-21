package com.frameout.core.db

import android.content.Context
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

const val APP_DB_VERSION = 17

@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
@Database(
    entities = [
        ProjectEntity::class,
        FrameEntity::class,
        SavedImageEntity::class,
        FrameEditEntity::class,
        FrameEditHistoryEntity::class,
        VideoIndexEntity::class,
        FrameTagEntity::class,
        FrameScanEntity::class,
        SharpestFrameEntity::class
    ],
    version = APP_DB_VERSION,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun frameDao(): FrameDao
    abstract fun savedImageDao(): SavedImageDao
    abstract fun frameEditDao(): FrameEditDao
    abstract fun videoIndexDao(): VideoIndexDao
    abstract fun frameTagDao(): FrameTagDao
    abstract fun sharpestDao(): SharpestDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                ensureColumn(connection, "projects", "extractMode", "TEXT NOT NULL DEFAULT 'EveryFrame'")
                ensureColumn(connection, "projects", "autoSave", "INTEGER NOT NULL DEFAULT 0")
                ensureColumn(connection, "projects", "startMs", "INTEGER NOT NULL DEFAULT 0")
                ensureColumn(connection, "projects", "endMs", "INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("CREATE TABLE IF NOT EXISTS frame_edits (frameId INTEGER NOT NULL PRIMARY KEY, rotation INTEGER NOT NULL DEFAULT 0, flipH INTEGER NOT NULL DEFAULT 0, flipV INTEGER NOT NULL DEFAULT 0, cropL REAL NOT NULL DEFAULT 0, cropT REAL NOT NULL DEFAULT 0, cropR REAL NOT NULL DEFAULT 1, cropB REAL NOT NULL DEFAULT 1, brightness REAL NOT NULL DEFAULT 0, contrast REAL NOT NULL DEFAULT 1, saturation REAL NOT NULL DEFAULT 1, FOREIGN KEY(frameId) REFERENCES frames(id) ON DELETE CASCADE)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_frame_edits_frameId ON frame_edits(frameId)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                ensureColumn(connection, "projects", "extractMode", "TEXT NOT NULL DEFAULT 'EveryFrame'")
                ensureColumn(connection, "projects", "autoSave", "INTEGER NOT NULL DEFAULT 0")
                ensureColumn(connection, "projects", "startMs", "INTEGER NOT NULL DEFAULT 0")
                ensureColumn(connection, "projects", "endMs", "INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("CREATE TABLE IF NOT EXISTS video_index (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, projectId INTEGER NOT NULL, sampleIndex INTEGER NOT NULL, ptsUs INTEGER NOT NULL, isKeyframe INTEGER NOT NULL, nearestKeyframeIndex INTEGER NOT NULL)")
                connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_video_index_projectId_sampleIndex ON video_index(projectId, sampleIndex)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_frames_projectId_isDeleted_index ON frames(projectId, isDeleted, `index`)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_frames_projectId_isFavorite_isDeleted_index ON frames(projectId, isFavorite, isDeleted, `index`)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_video_index_projectId_ptsUs_sampleIndex ON video_index(projectId, ptsUs, sampleIndex)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_saved_images_album_createdAt ON saved_images(album, createdAt)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override suspend fun migrate(connection: SQLiteConnection) {
                ensureColumn(connection, "projects", "lastSampleIndex", "INTEGER NOT NULL DEFAULT -1")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override suspend fun migrate(connection: SQLiteConnection) {
                ensureColumn(connection, "video_index", "nearestKeyframePtsUs", "INTEGER NOT NULL DEFAULT -1")
            }
        }


        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override suspend fun migrate(connection: SQLiteConnection) {
                ensureColumn(connection, "frame_edits", "revision", "INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("CREATE TABLE IF NOT EXISTS frame_edit_history (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, frameId INTEGER NOT NULL, revision INTEGER NOT NULL, rotation INTEGER NOT NULL DEFAULT 0, flipH INTEGER NOT NULL DEFAULT 0, flipV INTEGER NOT NULL DEFAULT 0, cropL REAL NOT NULL DEFAULT 0, cropT REAL NOT NULL DEFAULT 0, cropR REAL NOT NULL DEFAULT 1, cropB REAL NOT NULL DEFAULT 1, brightness REAL NOT NULL DEFAULT 0, contrast REAL NOT NULL DEFAULT 1, saturation REAL NOT NULL DEFAULT 1, FOREIGN KEY(frameId) REFERENCES frames(id) ON DELETE CASCADE)")
                connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_frame_edit_history_frameId_revision ON frame_edit_history(frameId, revision)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override suspend fun migrate(connection: SQLiteConnection) {
                ensureColumn(connection, "frame_edits", "isDeleted", "INTEGER NOT NULL DEFAULT 0")
                ensureColumn(connection, "frame_edit_history", "isDeleted", "INTEGER NOT NULL DEFAULT 0")
            }
        }


        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override suspend fun migrate(connection: SQLiteConnection) {
                ensureColumn(connection, "projects", "saveFormat", "TEXT NOT NULL DEFAULT 'jpg'")
                ensureColumn(connection, "projects", "jpegQuality", "INTEGER NOT NULL DEFAULT 92")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override suspend fun migrate(connection: SQLiteConnection) {
                ensureColumn(connection, "projects", "saveSubfolder", "TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE IF NOT EXISTS frame_tags (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, frameId INTEGER NOT NULL, projectId INTEGER NOT NULL, tag TEXT NOT NULL, confidence REAL NOT NULL, FOREIGN KEY(frameId) REFERENCES frames(id) ON DELETE CASCADE)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_frame_tags_frameId ON frame_tags(frameId)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_frame_tags_projectId_tag ON frame_tags(projectId, tag)")
                connection.execSQL("CREATE TABLE IF NOT EXISTS frame_scans (frameId INTEGER PRIMARY KEY NOT NULL, projectId INTEGER NOT NULL, FOREIGN KEY(frameId) REFERENCES frames(id) ON DELETE CASCADE)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_frame_scans_projectId ON frame_scans(projectId)")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override suspend fun migrate(connection: SQLiteConnection) {
                ensureColumn(connection, "frame_edits", "marks", "TEXT NOT NULL DEFAULT ''")
                ensureColumn(connection, "frame_edit_history", "marks", "TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override suspend fun migrate(connection: SQLiteConnection) {
                ensureColumn(connection, "frame_tags", "boxes", "TEXT NOT NULL DEFAULT ''")
                connection.execSQL("DELETE FROM frame_scans")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override suspend fun migrate(connection: SQLiteConnection) {
                ensureColumn(connection, "projects", "targetCount", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE IF NOT EXISTS sharpest_frames (projectId INTEGER NOT NULL, frameId INTEGER NOT NULL, PRIMARY KEY(projectId, frameId), FOREIGN KEY(frameId) REFERENCES frames(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_sharpest_frames_frameId ON sharpest_frames(frameId)")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override suspend fun migrate(connection: SQLiteConnection) {
                ensureColumn(connection, "frame_edits", "filter", "TEXT NOT NULL DEFAULT ''")
                ensureColumn(connection, "frame_edits", "filterStrength", "REAL NOT NULL DEFAULT 1")
                ensureColumn(connection, "frame_edit_history", "filter", "TEXT NOT NULL DEFAULT ''")
                ensureColumn(connection, "frame_edit_history", "filterStrength", "REAL NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override suspend fun migrate(connection: SQLiteConnection) {
                ensureColumn(connection, "projects", "gateFlags", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17
        )

        private fun ensureColumn(connection: SQLiteConnection, table: String, column: String, definition: String) {
            var exists = false
            connection.prepare("PRAGMA table_info(`$table`)").use { stmt ->
                while (stmt.step()) {
                    if (stmt.getText(1) == column) { exists = true; break }
                }
            }
            if (!exists) connection.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "frameout.db"
                ).addMigrations(*ALL_MIGRATIONS).build().also { instance = it }
            }
        }
    }
}
