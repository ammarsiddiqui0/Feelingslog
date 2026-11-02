package com.example.feelingslog

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.serialization.json.Json
import java.util.UUID

@Database(
    entities = [
        FeelingLog::class,
        LogContainer::class,
        LogContentItem::class,
        Tag::class,
        LogTagCrossRef::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(LogContentConverter::class)
abstract class LogDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: LogDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE logs ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val json = Json { ignoreUnknownKeys = true }

                // 1. Create new schema tables
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `logs_new` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER DEFAULT NULL,
                        `backgroundImageFileName` TEXT DEFAULT NULL,
                        `backgroundDim` REAL NOT NULL DEFAULT 0.3,
                        `coverImageFileName` TEXT DEFAULT NULL,
                        `themeAudioFileName` TEXT DEFAULT NULL,
                        `themeAudioAutoPlay` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `log_containers` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `logId` TEXT NOT NULL,
                        `topLevelOrdinal` INTEGER NOT NULL,
                        `layoutDirection` TEXT NOT NULL DEFAULT 'VERTICAL',
                        `size` TEXT NOT NULL DEFAULT 'MEDIUM',
                        FOREIGN KEY(`logId`) REFERENCES `logs`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_log_containers_logId` ON `log_containers` (`logId`)
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `log_content_items` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `logId` TEXT NOT NULL,
                        `containerId` TEXT DEFAULT NULL,
                        `type` TEXT NOT NULL,
                        `topLevelOrdinal` INTEGER DEFAULT NULL,
                        `itemOrdinal` INTEGER NOT NULL DEFAULT 0,
                        `textContent` TEXT,
                        `fontStyle` TEXT NOT NULL DEFAULT 'DEFAULT',
                        `fileName` TEXT,
                        `mimeType` TEXT,
                        `durationMs` INTEGER,
                        `caption` TEXT,
                        `size` TEXT NOT NULL DEFAULT 'MEDIUM',
                        `alignment` TEXT NOT NULL DEFAULT 'LEFT',
                        `aspectRatio` REAL DEFAULT NULL,
                        `isCropped` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`logId`) REFERENCES `logs`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`containerId`) REFERENCES `log_containers`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_log_content_items_logId` ON `log_content_items` (`logId`)
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_log_content_items_containerId` ON `log_content_items` (`containerId`)
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tags` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `colorHex` TEXT DEFAULT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `log_tag_cross_ref` (
                        `logId` TEXT NOT NULL,
                        `tagId` TEXT NOT NULL,
                        PRIMARY KEY(`logId`, `tagId`),
                        FOREIGN KEY(`logId`) REFERENCES `logs`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_log_tag_cross_ref_logId` ON `log_tag_cross_ref` (`logId`)
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_log_tag_cross_ref_tagId` ON `log_tag_cross_ref` (`tagId`)
                """.trimIndent())

                var expectedLogsCount = 0
                var expectedBlocksCount = 0
                val expectedUniqueTagNames = mutableMapOf<String, String>()
                val expectedUniqueCrossRefs = mutableSetOf<Pair<String, String>>()

                // 2. Read legacy data and migrate
                val cursor = db.query("SELECT id, title, blocks, tags, createdAt, updatedAt, deletedAt FROM logs")
                cursor.use { c ->
                    val idCol = c.getColumnIndexOrThrow("id")
                    val titleCol = c.getColumnIndexOrThrow("title")
                    val blocksCol = c.getColumnIndexOrThrow("blocks")
                    val tagsCol = c.getColumnIndexOrThrow("tags")
                    val createdCol = c.getColumnIndexOrThrow("createdAt")
                    val updatedCol = c.getColumnIndexOrThrow("updatedAt")
                    val deletedCol = c.getColumnIndexOrThrow("deletedAt")

                    while (c.moveToNext()) {
                        expectedLogsCount++
                        val logId = c.getString(idCol)
                        val title = c.getString(titleCol)
                        val blocksJson = c.getString(blocksCol)
                        val tagsJson = c.getString(tagsCol)
                        val createdAt = c.getLong(createdCol)
                        val updatedAt = c.getLong(updatedCol)
                        val deletedAt = if (c.isNull(deletedCol)) null else c.getLong(deletedCol)

                        db.execSQL(
                            "INSERT INTO logs_new (id, title, createdAt, updatedAt, deletedAt, backgroundDim, themeAudioAutoPlay) VALUES (?, ?, ?, ?, ?, 0.3, 0)",
                            arrayOf<Any?>(logId, title, createdAt, updatedAt, deletedAt)
                        )

                        if (!blocksJson.isNullOrEmpty()) {
                            val blocks: List<LogContentBlock> = try {
                                json.decodeFromString(blocksJson)
                            } catch (e: Exception) {
                                throw IllegalStateException("Migration 2->3 failed: Invalid blocks JSON in log $logId", e)
                            }

                            expectedBlocksCount += blocks.size
                            blocks.forEachIndexed { index, block ->
                                val itemId = UUID.randomUUID().toString()
                                when (block) {
                                    is LogContentBlock.Text -> {
                                        db.execSQL(
                                            "INSERT INTO log_content_items (id, logId, containerId, type, topLevelOrdinal, itemOrdinal, textContent, fontStyle, size, alignment, isCropped) VALUES (?, ?, NULL, 'TEXT', ?, 0, ?, 'DEFAULT', 'MEDIUM', 'LEFT', 0)",
                                            arrayOf<Any?>(itemId, logId, index, block.text)
                                        )
                                    }
                                    is LogContentBlock.Image -> {
                                        db.execSQL(
                                            "INSERT INTO log_content_items (id, logId, containerId, type, topLevelOrdinal, itemOrdinal, fileName, caption, size, alignment, isCropped) VALUES (?, ?, NULL, 'IMAGE', ?, 0, ?, ?, 'MEDIUM', 'LEFT', 0)",
                                            arrayOf<Any?>(itemId, logId, index, block.fileName, block.caption)
                                        )
                                    }
                                    is LogContentBlock.Audio -> {
                                        db.execSQL(
                                            "INSERT INTO log_content_items (id, logId, containerId, type, topLevelOrdinal, itemOrdinal, fileName, durationMs, size, alignment, isCropped) VALUES (?, ?, NULL, 'AUDIO', ?, 0, ?, ?, 'MEDIUM', 'LEFT', 0)",
                                            arrayOf<Any?>(itemId, logId, index, block.fileName, block.durationMs)
                                        )
                                    }
                                }
                            }
                        }

                        if (!tagsJson.isNullOrEmpty()) {
                            val rawTagNames: List<String> = try {
                                json.decodeFromString(tagsJson)
                            } catch (e: Exception) {
                                throw IllegalStateException("Migration 2->3 failed: Invalid tags JSON in log $logId", e)
                            }

                            rawTagNames.map { it.trim() }.filter { it.isNotEmpty() }.forEach { rawTag ->
                                val normalized = rawTag.lowercase()
                                if (!expectedUniqueTagNames.containsKey(normalized)) {
                                    expectedUniqueTagNames[normalized] = rawTag
                                }
                                val displayTag = expectedUniqueTagNames[normalized] ?: rawTag
                                val tagId = UUID.nameUUIDFromBytes(normalized.toByteArray()).toString()
                                expectedUniqueCrossRefs.add(Pair(logId, tagId))

                                db.execSQL("INSERT OR IGNORE INTO tags (id, name, colorHex) VALUES (?, ?, NULL)", arrayOf<Any?>(tagId, displayTag))
                                db.execSQL("INSERT OR IGNORE INTO log_tag_cross_ref (logId, tagId) VALUES (?, ?)", arrayOf<Any?>(logId, tagId))
                            }
                        }
                    }
                }

                // 3. Post-Insertion Verification using actual database COUNT(*) queries
                val actualLogsCount = db.compileStatement("SELECT COUNT(*) FROM logs_new").simpleQueryForLong().toInt()
                if (actualLogsCount != expectedLogsCount) {
                    throw IllegalStateException("Migration 2->3 verification failed: Expected $expectedLogsCount logs, actual DB count: $actualLogsCount")
                }

                val actualBlocksCount = db.compileStatement("SELECT COUNT(*) FROM log_content_items").simpleQueryForLong().toInt()
                if (actualBlocksCount != expectedBlocksCount) {
                    throw IllegalStateException("Migration 2->3 verification failed: Expected $expectedBlocksCount blocks, actual DB count: $actualBlocksCount")
                }

                val actualTagsCount = db.compileStatement("SELECT COUNT(*) FROM tags").simpleQueryForLong().toInt()
                if (actualTagsCount != expectedUniqueTagNames.size) {
                    throw IllegalStateException("Migration 2->3 verification failed: Expected ${expectedUniqueTagNames.size} unique tags, actual DB count: $actualTagsCount")
                }

                val actualCrossRefsCount = db.compileStatement("SELECT COUNT(*) FROM log_tag_cross_ref").simpleQueryForLong().toInt()
                if (actualCrossRefsCount != expectedUniqueCrossRefs.size) {
                    throw IllegalStateException("Migration 2->3 verification failed: Expected ${expectedUniqueCrossRefs.size} cross-references, actual DB count: $actualCrossRefsCount")
                }

                // Verify foreign key integrity (zero orphan references)
                val orphanRefsCursor = db.query("""
                    SELECT COUNT(*) FROM log_tag_cross_ref 
                    WHERE logId NOT IN (SELECT id FROM logs_new) 
                       OR tagId NOT IN (SELECT id FROM tags)
                """.trimIndent())
                orphanRefsCursor.use { orphanCursor ->
                    if (orphanCursor.moveToFirst() && orphanCursor.getInt(0) > 0) {
                        throw IllegalStateException("Migration 2->3 verification failed: Found orphan cross-reference entries pointing to non-existent logs or tags!")
                    }
                }

                // 4. Complete Table Swap
                db.execSQL("DROP TABLE logs")
                db.execSQL("ALTER TABLE logs_new RENAME TO logs")
            }
        }

        fun getDatabase(context: Context): LogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LogDatabase::class.java,
                    "feelings_log_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
