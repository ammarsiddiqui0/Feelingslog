package com.example.feelingslog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Core Log entity.
 */
@Entity(tableName = "logs")
data class FeelingLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val backgroundImageFileName: String? = null,
    @ColumnInfo(defaultValue = "0.3")
    val backgroundDim: Float = 0.3f,
    val coverImageFileName: String? = null,
    val themeAudioFileName: String? = null,
    @ColumnInfo(defaultValue = "0")
    val themeAudioAutoPlay: Boolean = false
)

/**
 * Legacy block definition retained for JSON decoding during Room MIGRATION_2_3.
 */
@Serializable
sealed class LogContentBlock {
    @Serializable
    data class Text(val text: String) : LogContentBlock()

    @Serializable
    data class Image(
        val fileName: String,
        val caption: String? = null
    ) : LogContentBlock()

    @Serializable
    data class Audio(
        val fileName: String,
        val type: String, // e.g., "voice", "music"
        val durationMs: Long
    ) : LogContentBlock()
}
