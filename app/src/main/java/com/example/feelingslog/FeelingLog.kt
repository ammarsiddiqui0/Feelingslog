package com.example.feelingslog

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * The core Log entity.
 */
@Entity(tableName = "logs")
data class FeelingLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val blocks: List<LogContentBlock> = emptyList(),
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Represents a single piece of content within a log.
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
