package com.example.feelingslog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class ContentType {
    TEXT, IMAGE, AUDIO, DOCUMENT
}

enum class ItemSize {
    SMALL, MEDIUM, BIG
}

enum class ItemAlignment {
    LEFT, CENTER, RIGHT
}

enum class FontStyleOption {
    DEFAULT, SERIF, MONOSPACE, CURSIVE
}

@Entity(
    tableName = "log_content_items",
    foreignKeys = [
        ForeignKey(
            entity = FeelingLog::class,
            parentColumns = ["id"],
            childColumns = ["logId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LogContainer::class,
            parentColumns = ["id"],
            childColumns = ["containerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("logId"), Index("containerId")]
)
data class LogContentItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val logId: String,
    val containerId: String? = null,
    val type: ContentType,
    val topLevelOrdinal: Int? = null,
    @ColumnInfo(defaultValue = "0")
    val itemOrdinal: Int = 0,
    val textContent: String? = null,
    @ColumnInfo(defaultValue = "DEFAULT")
    val fontStyle: FontStyleOption = FontStyleOption.DEFAULT,
    val fileName: String? = null,
    val mimeType: String? = null,
    val durationMs: Long? = null,
    val caption: String? = null,
    @ColumnInfo(defaultValue = "MEDIUM")
    val size: ItemSize = ItemSize.MEDIUM,
    @ColumnInfo(defaultValue = "LEFT")
    val alignment: ItemAlignment = ItemAlignment.LEFT,
    val aspectRatio: Float? = null,
    @ColumnInfo(defaultValue = "0")
    val isCropped: Boolean = false
)
