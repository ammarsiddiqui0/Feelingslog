package com.example.feelingslog

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Converts the complex List<LogContentBlock> into a String for Room storage.
 */
class LogContentConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromBlocks(blocks: List<LogContentBlock>): String {
        return json.encodeToString(blocks)
    }

    @TypeConverter
    fun toBlocks(data: String): List<LogContentBlock> {
        return json.decodeFromString(data)
    }

    @TypeConverter
    fun fromTags(tags: List<String>): String {
        return json.encodeToString(tags)
    }

    @TypeConverter
    fun toTags(data: String): List<String> {
        return json.decodeFromString(data)
    }
}
