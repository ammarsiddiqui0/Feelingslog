package com.example.feelingslog

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "log_tag_cross_ref",
    primaryKeys = ["logId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = FeelingLog::class,
            parentColumns = ["id"],
            childColumns = ["logId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("logId"), Index("tagId")]
)
data class LogTagCrossRef(
    val logId: String,
    val tagId: String
)
